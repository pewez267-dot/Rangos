package com.fantastic.kits.network;

import com.fantastic.kits.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client. Opens {@link com.fantastic.kits.client.screen.KitEditorScreen}
 * with one specific kit's NBT plus the same auxiliary data carried by
 * {@link OpenKitListPacket} (groups, command catalogue) so the editor never
 * has to ping the server during editing.
 */
public class OpenKitEditorPacket {

    private final CompoundTag kit;
    private final ListTag groups;
    private final ListTag commands;

    public OpenKitEditorPacket(CompoundTag kit, ListTag groups, ListTag commands) {
        this.kit = kit == null ? new CompoundTag() : kit;
        this.groups = groups == null ? new ListTag() : groups;
        this.commands = commands == null ? new ListTag() : commands;
    }

    public static void encode(OpenKitEditorPacket msg, FriendlyByteBuf buf) {
        CompoundTag wrap = new CompoundTag();
        wrap.put("kit", msg.kit);
        wrap.put("groups", msg.groups);
        wrap.put("commands", msg.commands);
        buf.writeNbt(wrap);
    }

    public static OpenKitEditorPacket decode(FriendlyByteBuf buf) {
        CompoundTag wrap = buf.readNbt();
        if (wrap == null) wrap = new CompoundTag();
        return new OpenKitEditorPacket(
                wrap.getCompound("kit"),
                wrap.getList("groups", Tag.TAG_COMPOUND),
                wrap.getList("commands", Tag.TAG_STRING));
    }

    public static void handle(OpenKitEditorPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.openKitEditor(msg.kit, msg.groups, msg.commands)));
        context.setPacketHandled(true);
    }
}
