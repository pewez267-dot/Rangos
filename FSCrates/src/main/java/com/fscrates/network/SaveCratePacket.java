package com.fscrates.network;

import com.fscrates.config.CrateConfig;
import com.fscrates.crate.CrateRegistry;
import com.fscrates.item.CrateItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: the admin saved a crate in the editor. Server re-validates
 * OP level 4, stores the definition and gives the admin the physical crate.
 */
public class SaveCratePacket {

    private final CompoundTag configNbt;

    public SaveCratePacket(CompoundTag configNbt) {
        this.configNbt = configNbt;
    }

    public static void encode(SaveCratePacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.configNbt);
    }

    public static SaveCratePacket decode(FriendlyByteBuf buf) {
        return new SaveCratePacket(buf.readNbt());
    }

    public static void handle(SaveCratePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(4) || msg.configNbt == null) {
                return;
            }
            CrateConfig crate = CrateConfig.load(msg.configNbt);
            if (crate.id == null || crate.id.isBlank()) {
                crate.id = "crate_" + System.currentTimeMillis();
            }
            CrateRegistry.get(player.serverLevel()).put(crate);

            ItemStack crateItem = CrateItems.buildCrate(crate);
            if (!player.getInventory().add(crateItem)) {
                player.drop(crateItem, false);
            }
            player.sendSystemMessage(Component.literal("\u00A7aCrate '" + crate.id
                    + "' guardada y entregada. Usa \u00A7e/fscrate key give\u00A7a para dar llaves."));
        });
        context.setPacketHandled(true);
    }
}
