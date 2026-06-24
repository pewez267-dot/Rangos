package com.fantasticshortcuts.network;

import com.fantasticshortcuts.FantasticShortcuts;
import com.fantasticshortcuts.audit.AuditLogger;
import com.fantasticshortcuts.brigadier.ClientTreeModifier;
import com.fantasticshortcuts.data.Shortcut;
import com.fantasticshortcuts.data.ShortcutManager;
import com.fantasticshortcuts.gui.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client &rarr; server: delete a shortcut by id. Server-validated and audited. */
public final class DeleteShortcutPacket {

    private final String shortcutId;

    public DeleteShortcutPacket(final String shortcutId) {
        this.shortcutId = shortcutId == null ? "" : shortcutId;
    }

    public static void encode(final DeleteShortcutPacket msg, final FriendlyByteBuf buf) {
        buf.writeUtf(msg.shortcutId);
    }

    public static DeleteShortcutPacket decode(final FriendlyByteBuf buf) {
        return new DeleteShortcutPacket(buf.readUtf());
    }

    public static void handle(final DeleteShortcutPacket msg, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(FantasticShortcuts.ADMIN_PERMISSION_LEVEL)) {
                return;
            }
            final Shortcut existing = ShortcutManager.get().byId(msg.shortcutId);
            if (existing == null) {
                player.sendSystemMessage(Component.literal("§cEse shortcut ya no existe."));
                ModMenus.openMain(player);
                return;
            }
            ShortcutManager.get().remove(existing.getId());
            AuditLogger.shortcutDeleted(existing.getName(), "/" + existing.aliasKey(),
                    player.getGameProfile().getName(), player.getUUID());
            if (player.getServer() != null) {
                ClientTreeModifier.resendToAll(player.getServer());
            }
            player.sendSystemMessage(Component.literal("§aShortcut §e/" + existing.aliasKey() + " §aeliminado."));
            ModMenus.openMain(player);
        });
        context.setPacketHandled(true);
    }
}
