package com.pewez.fantasticshortcuts.network;

import com.pewez.fantasticshortcuts.audit.AuditEvent;
import com.pewez.fantasticshortcuts.gui.GuiTab;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C -> S: eliminar un atajo completo. Valida permiso 4, audita, sincroniza y refresca la GUI.
 */
public class DeleteShortcutPacket {

    private final String alias;

    public DeleteShortcutPacket(String alias) {
        this.alias = alias;
    }

    public static void encode(DeleteShortcutPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.alias);
    }

    public static DeleteShortcutPacket decode(FriendlyByteBuf buf) {
        return new DeleteShortcutPacket(buf.readUtf());
    }

    public static void handle(DeleteShortcutPacket msg, Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (!player.hasPermissions(4)) {
                ShortcutManager.get().auditWithActor(AuditEvent.PERMISSION_DENIED, player.createCommandSourceStack(),
                        "DELETE alias='/" + msg.alias + "'");
                CreateShortcutPacket.deny(player);
                return;
            }
            final CommandSourceStack source = player.createCommandSourceStack();
            final ShortcutManager.Result result = ShortcutManager.get().delete(source, msg.alias);
            CreateShortcutPacket.feedback(player, result);
            OpenEditorPacket.open(player, GuiTab.LIST.ordinal());
        });
        context.setPacketHandled(true);
    }
}
