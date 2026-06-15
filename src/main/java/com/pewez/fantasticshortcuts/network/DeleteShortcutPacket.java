package com.pewez.fantasticshortcuts.network;

import com.pewez.fantasticshortcuts.FantasticShortcutsMod;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import com.pewez.fantasticshortcuts.util.ChatPrefix;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server. Request the deletion of a shortcut by alias.
 */
public class DeleteShortcutPacket {

    public final String alias;

    public DeleteShortcutPacket(String alias) {
        this.alias = alias;
    }

    public static void encode(DeleteShortcutPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.alias == null ? "" : packet.alias, 64);
    }

    public static DeleteShortcutPacket decode(FriendlyByteBuf buf) {
        return new DeleteShortcutPacket(buf.readUtf(64));
    }

    public static void handle(DeleteShortcutPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null || !sender.hasPermissions(4)) {
                return;
            }
            ShortcutManager.Result result = ShortcutManager.get()
                    .delete(packet.alias, sender.getGameProfile().getName());
            sender.sendSystemMessage(result.success()
                    ? ChatPrefix.success(result.message())
                    : ChatPrefix.error(result.message()));
            FantasticShortcutsMod.liveSync(sender.getServer());
            FSShortcutsNetwork.sendToClient(sender,
                    new OpenEditorPacket(new java.util.ArrayList<>(ShortcutManager.get().all())));
        });
        ctx.setPacketHandled(true);
    }
}
