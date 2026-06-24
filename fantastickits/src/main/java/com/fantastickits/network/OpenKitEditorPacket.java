package com.fantastickits.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server &rarr; client: open the kit editor.
 *
 * <p>Carries the kit (as NBT), the live list of LuckPerms group names (read on the
 * server at open time, never hard-coded or cached) and the commands currently assigned
 * to the kit's group. The full list of available server commands is read on the client
 * from the synced command tree, so it is not transmitted here.</p>
 */
public final class OpenKitEditorPacket {

    private final CompoundTag kitNbt;
    private final List<String> groups;
    private final List<String> assignedCommands;

    public OpenKitEditorPacket(final CompoundTag kitNbt, final List<String> groups, final List<String> assignedCommands) {
        this.kitNbt = kitNbt;
        this.groups = groups;
        this.assignedCommands = assignedCommands;
    }

    public static void encode(final OpenKitEditorPacket msg, final FriendlyByteBuf buf) {
        buf.writeNbt(msg.kitNbt);
        writeStrings(buf, msg.groups);
        writeStrings(buf, msg.assignedCommands);
    }

    public static OpenKitEditorPacket decode(final FriendlyByteBuf buf) {
        final CompoundTag nbt = buf.readNbt();
        final List<String> groups = readStrings(buf);
        final List<String> commands = readStrings(buf);
        return new OpenKitEditorPacket(nbt, groups, commands);
    }

    public static void handle(final OpenKitEditorPacket msg, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.fantastickits.gui.ClientHooks.openEditor(msg.kitNbt, msg.groups, msg.assignedCommands)));
        context.setPacketHandled(true);
    }

    static void writeStrings(final FriendlyByteBuf buf, final List<String> values) {
        final List<String> list = values == null ? List.of() : values;
        buf.writeVarInt(list.size());
        for (final String value : list) {
            buf.writeUtf(value == null ? "" : value);
        }
    }

    static List<String> readStrings(final FriendlyByteBuf buf) {
        final int size = buf.readVarInt();
        final List<String> list = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            list.add(buf.readUtf());
        }
        return list;
    }
}
