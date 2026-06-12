// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.network;

import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.fscrates.item.CrateItems;
import com.fscrates.crate.CrateRegistry;
import com.fscrates.config.CrateConfig;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;

public class SaveCratePacket
{
    private final CompoundTag configNbt;
    
    public SaveCratePacket(final CompoundTag configNbt) {
        this.configNbt = configNbt;
    }
    
    public static void encode(final SaveCratePacket msg, final FriendlyByteBuf buf) {
        buf.writeNbt(msg.configNbt);
    }
    
    public static SaveCratePacket decode(final FriendlyByteBuf buf) {
        return new SaveCratePacket(buf.readNbt());
    }
    
    public static void handle(final SaveCratePacket msg, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(4) || msg.configNbt == null) {
                return;
            }
            else {
                final CrateConfig crate = CrateConfig.load(msg.configNbt);
                if (crate.id == null || crate.id.isBlank()) {
                    crate.id = "crate_" + System.currentTimeMillis();
                }
                CrateRegistry.get(player.serverLevel()).put(crate);
                final ItemStack crateItem = CrateItems.buildCrate(crate);
                if (!player.getInventory().add(crateItem)) {
                    player.drop(crateItem, false);
                }
                player.sendSystemMessage((Component)Component.literal("§aCrate '" + crate.id + "' guardada y entregada. Usa §e/fscrate key give§a para dar llaves."));
                return;
            }
        });
        context.setPacketHandled(true);
    }
}
