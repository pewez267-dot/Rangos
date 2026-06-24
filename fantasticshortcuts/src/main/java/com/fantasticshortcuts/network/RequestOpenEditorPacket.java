package com.fantasticshortcuts.network;

import com.fantasticshortcuts.FantasticShortcuts;
import com.fantasticshortcuts.data.Shortcut;
import com.fantasticshortcuts.data.ShortcutManager;
import com.fantasticshortcuts.gui.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client &rarr; server: open the editor. An empty id opens a fresh shortcut (create);
 * otherwise the existing shortcut is loaded for editing.
 */
public final class RequestOpenEditorPacket {

    private final String shortcutId;

    public RequestOpenEditorPacket(final String shortcutId) {
        this.shortcutId = shortcutId == null ? "" : shortcutId;
    }

    public static void encode(final RequestOpenEditorPacket msg, final FriendlyByteBuf buf) {
        buf.writeUtf(msg.shortcutId);
    }

    public static RequestOpenEditorPacket decode(final FriendlyByteBuf buf) {
        return new RequestOpenEditorPacket(buf.readUtf());
    }

    public static void handle(final RequestOpenEditorPacket msg, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(FantasticShortcuts.ADMIN_PERMISSION_LEVEL)) {
                return;
            }
            Shortcut target = null;
            if (!msg.shortcutId.isBlank()) {
                target = ShortcutManager.get().byId(msg.shortcutId);
            }
            if (target == null) {
                target = new Shortcut();
            }
            ModMenus.openEditor(player, target);
        });
        context.setPacketHandled(true);
    }
}
