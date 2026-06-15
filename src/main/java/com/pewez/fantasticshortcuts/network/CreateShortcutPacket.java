package com.pewez.fantasticshortcuts.network;

import com.pewez.fantasticshortcuts.FantasticShortcutsMod;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import com.pewez.fantasticshortcuts.util.ChatPrefix;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server. Request the creation of a new shortcut.
 */
public class CreateShortcutPacket {

    public final String alias;
    public final String command;

    public CreateShortcutPacket(String alias, String command) {
        this.alias = alias;
        this.command = command;
    }

    public static void encode(CreateShortcutPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.alias == null ? "" : packet.alias, 64);
        buf.writeUtf(packet.command == null ? "" : packet.command, 512);
    }

    public static CreateShortcutPacket decode(FriendlyByteBuf buf) {
        return new CreateShortcutPacket(buf.readUtf(64), buf.readUtf(512));
    }

    public static void handle(CreateShortcutPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null || !sender.hasPermissions(4)) {
                return;
            }
            ShortcutManager.Result result = ShortcutManager.get()
                    .create(packet.alias, packet.command, sender.getGameProfile().getName());
            sender.sendSystemMessage(result.success()
                    ? ChatPrefix.success(result.message())
                    : ChatPrefix.error(result.message()));
            FantasticShortcutsMod.liveSync(sender.getServer());
            // Refresh the editor on the client with the new state.
            FSShortcutsNetwork.sendToClient(sender,
                    new OpenEditorPacket(new java.util.ArrayList<>(ShortcutManager.get().all()), "lista"));
        });
        ctx.setPacketHandled(true);
    }
}
