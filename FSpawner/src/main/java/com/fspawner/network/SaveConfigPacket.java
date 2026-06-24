package com.fspawner.network;

import com.fspawner.config.SpawnerConfig;
import com.fspawner.item.SpawnerItemBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server. Carries the configuration the admin built in the GUI. The
 * server re-validates OP level 4 (never trust the client) and grants the item.
 */
public class SaveConfigPacket {

    private final CompoundTag configNbt;

    public SaveConfigPacket(CompoundTag configNbt) {
        this.configNbt = configNbt;
    }

    public static void encode(SaveConfigPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.configNbt);
    }

    public static SaveConfigPacket decode(FriendlyByteBuf buf) {
        return new SaveConfigPacket(buf.readNbt());
    }

    public static void handle(SaveConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            // Security: this is an OP-only feature. Re-check on the server.
            if (!player.hasPermissions(4)) {
                player.sendSystemMessage(Component.translatable("fspawner.command.no_permission"));
                return;
            }
            if (msg.configNbt == null) {
                return;
            }
            SpawnerConfig cfg = SpawnerConfig.load(msg.configNbt);
            ItemStack stack = SpawnerItemBuilder.build(cfg);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            player.sendSystemMessage(Component.translatable("fspawner.command.given"));
        });
        context.setPacketHandled(true);
    }
}
