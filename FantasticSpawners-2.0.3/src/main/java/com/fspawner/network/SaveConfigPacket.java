// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.network;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.fspawner.item.SpawnerItemBuilder;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import com.fspawner.config.SpawnerConfig;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;

public class SaveConfigPacket
{
    private final CompoundTag configNbt;
    private final EditContext context;
    
    public SaveConfigPacket(final CompoundTag configNbt, final EditContext context) {
        this.configNbt = configNbt;
        this.context = context;
    }
    
    public SaveConfigPacket(final CompoundTag configNbt) {
        this(configNbt, EditContext.newSession());
    }
    
    public static void encode(final SaveConfigPacket msg, final FriendlyByteBuf buf) {
        buf.writeNbt(msg.configNbt);
        msg.context.encode(buf);
    }
    
    public static SaveConfigPacket decode(final FriendlyByteBuf buf) {
        final CompoundTag tag = buf.readNbt();
        final EditContext ctx = EditContext.decode(buf);
        return new SaveConfigPacket(tag, ctx);
    }
    
    public static void handle(final SaveConfigPacket msg, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null || msg.configNbt == null) {
                return;
            }
            else if (!player.hasPermissions(4)) {
                player.sendSystemMessage((Component)Component.translatable("fspawner.command.no_permission"));
                return;
            }
            else {
                final SpawnerConfig cfg = SpawnerConfig.load(msg.configNbt);
                applyTo(player, cfg, msg.context);
                return;
            }
        });
        context.setPacketHandled(true);
    }
    
    private static void applyTo(final ServerPlayer player, final SpawnerConfig cfg, final EditContext src) {
        switch (src.source) {
            case BLOCK: {
                applyToBlock(player, cfg, src.pos);
                break;
            }
            case MAIN_HAND: {
                replaceHand(player, cfg, InteractionHand.MAIN_HAND, src.slot);
                break;
            }
            case OFF_HAND: {
                replaceHand(player, cfg, InteractionHand.OFF_HAND, -1);
                break;
            }
            default: {
                giveNew(player, cfg);
                break;
            }
        }
    }
    
    private static void giveNew(final ServerPlayer player, final SpawnerConfig cfg) {
        final ItemStack stack = SpawnerItemBuilder.build(cfg);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.sendSystemMessage((Component)Component.translatable("fspawner.command.given"));
    }
    
    private static void replaceHand(final ServerPlayer player, final SpawnerConfig cfg, final InteractionHand hand, final int hintSlot) {
        final Inventory inv = player.getInventory();
        final ItemStack stack = SpawnerItemBuilder.build(cfg);
        if (hand == InteractionHand.MAIN_HAND && hintSlot >= 0 && hintSlot < inv.items.size() && SpawnerItemBuilder.isFantasticSpawner(inv.getItem(hintSlot))) {
            inv.setItem(hintSlot, stack);
        }
        else {
            player.setItemInHand(hand, stack);
        }
        player.sendSystemMessage((Component)Component.translatable("fspawner.command.updated"));
    }
    
    private static void applyToBlock(final ServerPlayer player, final SpawnerConfig cfg, final BlockPos pos) {
        if (pos == null) {
            giveNew(player, cfg);
            return;
        }
        final ServerLevel level = player.serverLevel();
        final BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SpawnerBlockEntity)) {
            player.sendSystemMessage((Component)Component.literal("§cEl bloque ya no existe."));
            return;
        }
        final CompoundTag beTag = SpawnerItemBuilder.buildBlockEntityTag(cfg);
        beTag.putString("id", "minecraft:mob_spawner");
        beTag.putInt("x", pos.getX());
        beTag.putInt("y", pos.getY());
        beTag.putInt("z", pos.getZ());
        be.load(beTag);
        be.setChanged();
        final BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 10);
        player.sendSystemMessage((Component)Component.translatable("fspawner.command.block_updated"));
    }
}
