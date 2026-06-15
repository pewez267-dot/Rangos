package com.pewez.fantasticshortcuts.network;

import com.pewez.fantasticshortcuts.client.ClientPacketHandler;
import com.pewez.fantasticshortcuts.shortcuts.Shortcut;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server -> Client. Open the shortcut editor screen on the player's client, with the current list
 * of shortcuts as a snapshot.
 */
public class OpenEditorPacket {

    private final List<Shortcut> shortcuts;

    public OpenEditorPacket(List<Shortcut> shortcuts) {
        this.shortcuts = shortcuts;
    }

    public List<Shortcut> getShortcuts() {
        return shortcuts;
    }

    public static void encode(OpenEditorPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.shortcuts.size());
        for (Shortcut shortcut : packet.shortcuts) {
            shortcut.encode(buf);
        }
    }

    public static OpenEditorPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Shortcut> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(Shortcut.decode(buf));
        }
        return new OpenEditorPacket(list);
    }

    public static void handle(OpenEditorPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.openEditor(packet)));
        ctx.setPacketHandled(true);
    }
}
