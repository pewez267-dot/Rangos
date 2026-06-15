package com.pewez.fantasticshortcuts.network;

import com.pewez.fantasticshortcuts.audit.AuditEvent;
import com.pewez.fantasticshortcuts.gui.GuiTab;
import com.pewez.fantasticshortcuts.shortcuts.Shortcut;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C -> S: guardar cambios de un atajo existente (incluye renombrado).
 *
 * <p>Lleva el {@code originalAlias} para poder renombrar: si el alias cambió, el servidor elimina la
 * entrada antigua y crea la nueva. Valida permiso 4, audita, sincroniza y refresca la GUI.
 */
public class SaveShortcutPacket {

    private final String originalAlias;
    private final String alias;
    private final String command;
    private final String description;
    private final boolean useArgs;
    private final boolean replaceOriginal;

    public SaveShortcutPacket(String originalAlias, String alias, String command,
                              String description, boolean useArgs, boolean replaceOriginal) {
        this.originalAlias = originalAlias;
        this.alias = alias;
        this.command = command;
        this.description = description;
        this.useArgs = useArgs;
        this.replaceOriginal = replaceOriginal;
    }

    public static void encode(SaveShortcutPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.originalAlias);
        buf.writeUtf(msg.alias);
        buf.writeUtf(msg.command);
        buf.writeUtf(msg.description);
        buf.writeBoolean(msg.useArgs);
        buf.writeBoolean(msg.replaceOriginal);
    }

    public static SaveShortcutPacket decode(FriendlyByteBuf buf) {
        return new SaveShortcutPacket(
                buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(SaveShortcutPacket msg, Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (!player.hasPermissions(4)) {
                ShortcutManager.get().auditWithActor(AuditEvent.PERMISSION_DENIED, player.createCommandSourceStack(),
                        "SAVE alias='/" + msg.alias + "'");
                CreateShortcutPacket.deny(player);
                return;
            }
            final CommandSourceStack source = player.createCommandSourceStack();
            final Shortcut edited = new Shortcut(msg.alias, msg.command, msg.description, msg.useArgs, msg.replaceOriginal);
            final ShortcutManager.Result result = ShortcutManager.get().update(source, msg.originalAlias, edited);
            CreateShortcutPacket.feedback(player, result);
            OpenEditorPacket.open(player, GuiTab.LIST.ordinal());
        });
        context.setPacketHandled(true);
    }
}
