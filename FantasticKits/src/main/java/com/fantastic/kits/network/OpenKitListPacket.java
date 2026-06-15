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
 * Server -> Client. Tells the client to open {@link com.fantastic.kits.client.screen.KitListScreen}
 * pre-populated with every kit, every LuckPerms group and the discovered
 * command catalogue, in one shot, so the screen has no further round-trips.
 *
 * <p>Modes (see {@link com.fantastic.kits.client.screen.KitListScreen.Mode}):
 * 0 = CREATE, 1 = EDIT, 2 = DELETE, 3 = TEST.
 */
public class OpenKitListPacket {

    private final int mode;
    private final ListTag kits;       // list of full Kit NBT
    private final ListTag groups;     // list of CompoundTag {name, displayName, weight, primary}
    private final ListTag commands;   // list of String tags

    public OpenKitListPacket(int mode, ListTag kits, ListTag groups, ListTag commands) {
        this.mode = mode;
        this.kits = kits == null ? new ListTag() : kits;
        this.groups = groups == null ? new ListTag() : groups;
        this.commands = commands == null ? new ListTag() : commands;
    }

    public static void encode(OpenKitListPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.mode);
        CompoundTag wrap = new CompoundTag();
        wrap.put("kits", msg.kits);
        wrap.put("groups", msg.groups);
        wrap.put("commands", msg.commands);
        buf.writeNbt(wrap);
    }

    public static OpenKitListPacket decode(FriendlyByteBuf buf) {
        int mode = buf.readVarInt();
        CompoundTag wrap = buf.readNbt();
        ListTag kits = wrap == null ? new ListTag() : wrap.getList("kits", Tag.TAG_COMPOUND);
        ListTag groups = wrap == null ? new ListTag() : wrap.getList("groups", Tag.TAG_COMPOUND);
        ListTag commands = wrap == null ? new ListTag() : wrap.getList("commands", Tag.TAG_STRING);
        return new OpenKitListPacket(mode, kits, groups, commands);
    }

    public static void handle(OpenKitListPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.openKitList(msg.mode, msg.kits, msg.groups, msg.commands)));
        context.setPacketHandled(true);
    }
}
