// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.network;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.fscrates.item.CrateItems;
import net.minecraft.network.chat.Component;
import com.fscrates.block.CrateBlockEntity;
import com.fscrates.crate.CrateRegistry;
import com.fscrates.config.CrateConfig;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class SaveCratePacket
{
    private final CompoundTag configNbt;
    private final BlockPos pos;
    
    public SaveCratePacket(final CompoundTag configNbt) {
        this(configNbt, null);
    }
    
    public SaveCratePacket(final CompoundTag configNbt, final BlockPos pos) {
        this.configNbt = configNbt;
        this.pos = pos;
    }
    
    public static void encode(final SaveCratePacket msg, final FriendlyByteBuf buf) {
        buf.writeNbt(msg.configNbt);
        final boolean hasPos = msg.pos != null;
        buf.writeBoolean(hasPos);
        if (hasPos) {
            buf.writeBlockPos(msg.pos);
        }
    }
    
    public static SaveCratePacket decode(final FriendlyByteBuf buf) {
        final CompoundTag nbt = buf.readNbt();
        final BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
        return new SaveCratePacket(nbt, pos);
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
                if (msg.pos != null) {
                    final ServerLevel level = player.serverLevel();
                    final BlockEntity be = level.getBlockEntity(msg.pos);
                    if (be instanceof final CrateBlockEntity crateBe) {
                        crateBe.setConfig(crate);
                        player.sendSystemMessage((Component)Component.literal("§aCofre '" + crate.id + "' actualizado en el sitio."));
                        return;
                    }
                    else {
                        player.sendSystemMessage((Component)Component.literal("§eEl cofre ya no esta ahi; se guardo '" + crate.id + "' en el registro."));
                        return;
                    }
                }
                else {
                    final ItemStack crateItem = CrateItems.buildCrate(crate);
                    if (!player.getInventory().add(crateItem)) {
                        player.drop(crateItem, false);
                    }
                    player.sendSystemMessage((Component)Component.literal("§aCrate '" + crate.id + "' guardada y entregada. Usa §e/fscrate key give§a para dar llaves."));
                    return;
                }
            }
        });
        context.setPacketHandled(true);
    }
}
