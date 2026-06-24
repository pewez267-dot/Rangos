package com.fantasticshortcuts.network;

import com.fantasticshortcuts.FantasticShortcuts;
import com.fantasticshortcuts.audit.AuditLogger;
import com.fantasticshortcuts.brigadier.ClientTreeModifier;
import com.fantasticshortcuts.data.Shortcut;
import com.fantasticshortcuts.data.ShortcutManager;
import com.fantasticshortcuts.gui.ModMenus;
import com.fantasticshortcuts.util.ConflictChecker;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Client &rarr; server: create or update a shortcut. The server is the security boundary:
 * it re-checks the sender's permission, re-runs the conflict checker authoritatively
 * (blocking errors abort the save), preserves server-managed metadata, persists, audits
 * and refreshes every player's command tree.
 */
public final class SaveShortcutPacket {

    private final Shortcut shortcut;

    public SaveShortcutPacket(final Shortcut shortcut) {
        this.shortcut = shortcut;
    }

    public static void encode(final SaveShortcutPacket msg, final FriendlyByteBuf buf) {
        ShortcutCodec.write(buf, msg.shortcut);
    }

    public static SaveShortcutPacket decode(final FriendlyByteBuf buf) {
        return new SaveShortcutPacket(ShortcutCodec.read(buf));
    }

    public static void handle(final SaveShortcutPacket msg, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null || player.getServer() == null
                    || !player.hasPermissions(FantasticShortcuts.ADMIN_PERMISSION_LEVEL)) {
                return;
            }
            final Shortcut incoming = msg.shortcut;
            if (incoming.getOriginalCommand().isBlank() || incoming.getAlias().isBlank()) {
                player.sendSystemMessage(Component.literal("§cEl alias y el comando original son obligatorios."));
                ModMenus.openEditor(player, incoming);
                return;
            }

            final CommandDispatcher<CommandSourceStack> dispatcher = player.getServer().getCommands().getDispatcher();
            final Map<String, String> aliasToName = new HashMap<>();
            for (final Shortcut s : ShortcutManager.get().all()) {
                if (!s.getId().equals(incoming.getId())) {
                    aliasToName.put(s.aliasKey(), s.getName().isBlank() ? ("/" + s.aliasKey()) : s.getName());
                }
            }
            final ConflictChecker.Result result = ConflictChecker.check(incoming.getAlias(), aliasToName,
                    name -> dispatcher.getRoot().getChild(name) != null);
            if (result.blocking()) {
                AuditLogger.conflictDetected("/" + incoming.aliasKey(), "-", "bloqueado: " + result.message());
                player.sendSystemMessage(Component.literal("§c" + result.message()));
                ModMenus.openEditor(player, incoming);
                return;
            }
            if (result.severity() == ConflictChecker.Severity.WARNING) {
                AuditLogger.conflictDetected("/" + incoming.aliasKey(), "-", "advertencia aceptada");
            }

            final Shortcut existing = ShortcutManager.get().byId(incoming.getId());
            final boolean creating = existing == null;
            final Shortcut toSave = incoming.copy();
            if (creating) {
                toSave.setCreatedBy(player.getUUID().toString());
                toSave.setCreatedAt(Shortcut.nowIso());
            } else {
                toSave.setCreatedBy(existing.getCreatedBy());
                toSave.setCreatedAt(existing.getCreatedAt());
            }
            toSave.touchModified();

            ShortcutManager.get().put(toSave);

            if (creating) {
                AuditLogger.shortcutCreated(toSave.getName(), "/" + toSave.aliasKey(), toSave.getOriginalCommand(),
                        player.getGameProfile().getName(), player.getUUID());
            } else {
                AuditLogger.shortcutEdited(toSave.getName(), diff(existing, toSave),
                        player.getGameProfile().getName(), player.getUUID());
            }

            ClientTreeModifier.resendToAll(player.getServer());
            player.sendSystemMessage(Component.literal("§aShortcut §e/" + toSave.aliasKey() + " §aguardado."));
            ModMenus.openMain(player);
        });
        context.setPacketHandled(true);
    }

    private static String diff(final Shortcut before, final Shortcut after) {
        final StringBuilder sb = new StringBuilder();
        appendChange(sb, "alias", "/" + before.aliasKey(), "/" + after.aliasKey());
        appendChange(sb, "original", before.getOriginalCommand(), after.getOriginalCommand());
        appendChange(sb, "nombre", before.getName(), after.getName());
        appendChange(sb, "descripcion", before.getDescription(), after.getDescription());
        appendChange(sb, "replace", Boolean.toString(before.isReplaceOriginal()), Boolean.toString(after.isReplaceOriginal()));
        return sb.length() == 0 ? "sin cambios" : sb.toString();
    }

    private static void appendChange(final StringBuilder sb, final String field, final String a, final String b) {
        if (!a.equals(b)) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(field).append(": '").append(a).append("' -> '").append(b).append('\'');
        }
    }
}
