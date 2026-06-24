package com.fantastickits.network;

import com.fantastickits.config.FKConfig;
import com.fantastickits.data.GroupCommandStore;
import com.fantastickits.data.Kit;
import com.fantastickits.data.KitRegistry;
import com.fantastickits.integration.LuckPermsIntegration;
import com.fantastickits.security.AuditLog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client &rarr; server: persist an edited kit.
 *
 * <p>The handler is the security boundary for editing: it re-checks the sender's
 * permission level (the client cannot be trusted), clamps the item count to the
 * configured maximum, stores the kit, mirrors the selected commands into
 * {@code group_commands.json} for the kit's group, and audits the result.</p>
 */
public final class SaveKitPacket {

    private final CompoundTag kitNbt;
    private final List<String> commands;

    public SaveKitPacket(final CompoundTag kitNbt, final List<String> commands) {
        this.kitNbt = kitNbt;
        this.commands = commands;
    }

    public static void encode(final SaveKitPacket msg, final FriendlyByteBuf buf) {
        buf.writeNbt(msg.kitNbt);
        OpenKitEditorPacket.writeStrings(buf, msg.commands);
    }

    public static SaveKitPacket decode(final FriendlyByteBuf buf) {
        final CompoundTag nbt = buf.readNbt();
        final List<String> commands = OpenKitEditorPacket.readStrings(buf);
        return new SaveKitPacket(nbt, commands);
    }

    public static void handle(final SaveKitPacket msg, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null || msg.kitNbt == null) {
                return;
            }
            if (!player.hasPermissions(FKConfig.adminPermissionLevel())) {
                player.sendSystemMessage(Component.literal("§cNo tienes permiso para guardar kits."));
                return;
            }

            final Kit kit = Kit.fromNbt(msg.kitNbt);
            kit.id = Kit.normalizeId(kit.id);
            if (kit.id.isBlank()) {
                kit.id = "kit_" + (System.currentTimeMillis() % 100000L);
            }
            // Clamp item count to the configured maximum (server-side safety).
            final int max = FKConfig.maxItemsPerKit();
            while (kit.items.size() > max) {
                kit.items.remove(kit.items.size() - 1);
            }

            final Kit previous = KitRegistry.get().get(kit.id);
            final boolean existed = previous != null;
            final String oldGroup = (previous != null && previous.hasGroup()) ? previous.group : null;

            KitRegistry.get().put(kit);

            if (kit.hasGroup()) {
                final java.util.Set<String> before = GroupCommandStore.get().commandsFor(kit.group);
                GroupCommandStore.get().setCommands(kit.group, msg.commands);
                final java.util.Set<String> after = GroupCommandStore.get().commandsFor(kit.group);
                // Grant the newly-added command nodes and revoke the removed ones on the rank.
                if (FKConfig.manageLuckPermsPermissions()) {
                    final java.util.Set<String> toAdd = new java.util.LinkedHashSet<>(after);
                    toAdd.removeAll(before);
                    final java.util.Set<String> toRemove = new java.util.LinkedHashSet<>(before);
                    toRemove.removeAll(after);
                    LuckPermsIntegration.updateGroupCommandNodes(kit.group, toAdd, toRemove,
                            FKConfig.commandPermissionPrefixes());
                }
            }

            // If the kit was moved to a different rank, clean the previous group's gating/nodes
            // when no remaining kit still uses it.
            if (oldGroup != null && !oldGroup.equalsIgnoreCase(kit.group)) {
                boolean stillUsed = false;
                for (final Kit other : KitRegistry.get().all()) {
                    if (other.hasGroup() && oldGroup.equalsIgnoreCase(other.group)) {
                        stillUsed = true;
                        break;
                    }
                }
                if (!stillUsed) {
                    final java.util.Set<String> oldCommands = GroupCommandStore.get().commandsFor(oldGroup);
                    GroupCommandStore.get().removeGroup(oldGroup);
                    if (FKConfig.manageLuckPermsPermissions()) {
                        LuckPermsIntegration.updateGroupCommandNodes(oldGroup,
                                java.util.Collections.emptySet(), oldCommands, FKConfig.commandPermissionPrefixes());
                    }
                }
            }

            final UUID uuid = player.getUUID();
            final String name = player.getGameProfile().getName();
            if (existed) {
                AuditLog.kitEdited(uuid, name, kit.id);
            } else {
                AuditLog.kitCreated(uuid, name, kit.id);
            }

            final String groupInfo = kit.hasGroup()
                    ? ("§7grupo §e" + kit.group + "§7, comandos: §f" + msg.commands.size())
                    : "§7sin grupo asignado";
            player.sendSystemMessage(Component.literal(
                    "§aKit §e" + kit.id + " §aguardado (" + kit.items.size() + " items, " + groupInfo + "§a)."));
        });
        context.setPacketHandled(true);
    }
}
