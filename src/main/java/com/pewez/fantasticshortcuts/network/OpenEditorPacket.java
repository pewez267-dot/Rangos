package com.pewez.fantasticshortcuts.network;

import com.pewez.fantasticshortcuts.config.FSConfig;
import com.pewez.fantasticshortcuts.shortcuts.Shortcut;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S -> C: abre (o refresca) el editor de atajos en el cliente, con la lista completa de atajos y un
 * snapshot de la configuración para la pestaña "Ajustes".
 */
public class OpenEditorPacket {

    private final List<Shortcut> shortcuts;
    private final int tab;
    private final boolean enableReplaceMode;
    private final String shortcutPriority;
    private final boolean auditEnabled;

    public OpenEditorPacket(List<Shortcut> shortcuts, int tab,
                            boolean enableReplaceMode, String shortcutPriority, boolean auditEnabled) {
        this.shortcuts = shortcuts;
        this.tab = tab;
        this.enableReplaceMode = enableReplaceMode;
        this.shortcutPriority = shortcutPriority;
        this.auditEnabled = auditEnabled;
    }

    /** Construye el paquete a partir del estado actual del servidor, para la pestaña indicada. */
    public static OpenEditorPacket snapshot(int tab) {
        return new OpenEditorPacket(
                ShortcutManager.get().list(),
                tab,
                FSConfig.enableReplaceMode(),
                FSConfig.shortcutPriority().name(),
                FSConfig.auditEnabled());
    }

    /** Envía el snapshot actual al jugador, abriendo o refrescando la pestaña indicada. */
    public static void open(ServerPlayer player, int tab) {
        FSNetwork.sendToClient(player, snapshot(tab));
    }

    public List<Shortcut> shortcuts() {
        return shortcuts;
    }

    public int tab() {
        return tab;
    }

    public boolean enableReplaceMode() {
        return enableReplaceMode;
    }

    public String shortcutPriority() {
        return shortcutPriority;
    }

    public boolean auditEnabled() {
        return auditEnabled;
    }

    public static void encode(OpenEditorPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.shortcuts.size());
        for (Shortcut s : msg.shortcuts) {
            s.encode(buf);
        }
        buf.writeVarInt(msg.tab);
        buf.writeBoolean(msg.enableReplaceMode);
        buf.writeUtf(msg.shortcutPriority);
        buf.writeBoolean(msg.auditEnabled);
    }

    public static OpenEditorPacket decode(FriendlyByteBuf buf) {
        final int count = buf.readVarInt();
        final List<Shortcut> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(Shortcut.decode(buf));
        }
        final int tab = buf.readVarInt();
        final boolean replace = buf.readBoolean();
        final String priority = buf.readUtf();
        final boolean audit = buf.readBoolean();
        return new OpenEditorPacket(list, tab, replace, priority, audit);
    }

    public static void handle(OpenEditorPacket msg, Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.pewez.fantasticshortcuts.client.ClientPacketHandler.openEditor(msg)));
        context.setPacketHandled(true);
    }
}
