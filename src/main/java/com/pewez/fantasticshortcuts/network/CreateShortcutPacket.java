package com.pewez.fantasticshortcuts.network;

import com.pewez.fantasticshortcuts.audit.AuditEvent;
import com.pewez.fantasticshortcuts.gui.GuiTab;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C -> S: crear un atajo nuevo desde la GUI.
 *
 * <p>El servidor valida {@code hasPermissions(4)}, valida la entrada (alias/comando), audita,
 * sincroniza el árbol de comandos y responde con un {@link OpenEditorPacket} para refrescar la
 * pantalla en la pestaña "Lista".
 */
public class CreateShortcutPacket {

    private final String alias;
    private final String command;

    public CreateShortcutPacket(String alias, String command) {
        this.alias = alias;
        this.command = command;
    }

    public static void encode(CreateShortcutPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.alias);
        buf.writeUtf(msg.command);
    }

    public static CreateShortcutPacket decode(FriendlyByteBuf buf) {
        return new CreateShortcutPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(CreateShortcutPacket msg, Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (!player.hasPermissions(4)) {
                ShortcutManager.get().auditWithActor(AuditEvent.PERMISSION_DENIED, player.createCommandSourceStack(),
                        "CREATE alias='/" + msg.alias + "'");
                deny(player);
                return;
            }
            final CommandSourceStack source = player.createCommandSourceStack();
            final ShortcutManager.Result result = ShortcutManager.get().create(source, msg.alias, msg.command);
            feedback(player, result);
            OpenEditorPacket.open(player, GuiTab.LIST.ordinal());
        });
        context.setPacketHandled(true);
    }

    static void deny(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§c[F-Shortcuts] No tienes permiso para gestionar atajos."));
    }

    static void feedback(ServerPlayer player, ShortcutManager.Result result) {
        final String color = result.success() ? "§a" : "§c";
        player.sendSystemMessage(Component.literal("§7[§bF-Shortcuts§7] " + color + result.message()));
    }
}
