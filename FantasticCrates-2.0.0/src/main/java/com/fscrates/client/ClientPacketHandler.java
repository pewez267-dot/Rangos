// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.client;

import net.minecraft.world.item.ItemStack;
import java.util.List;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import com.fscrates.block.CrateBlockEntity;
import net.minecraft.client.gui.screens.Screen;
import com.fscrates.client.screen.CrateEditorScreen;
import net.minecraft.client.Minecraft;
import com.fscrates.config.CrateConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public final class ClientPacketHandler
{
    private ClientPacketHandler() {
    }
    
    public static void openEditor(final CompoundTag configNbt) {
        openEditor(configNbt, null);
    }
    
    public static void openEditor(final CompoundTag configNbt, final BlockPos pos) {
        final CrateConfig cfg = (configNbt == null) ? new CrateConfig() : CrateConfig.load(configNbt);
        Minecraft.getInstance().setScreen((Screen)new CrateEditorScreen(cfg, pos));
    }
    
    public static void playAnimation(final BlockPos pos, final String animationId, final int rarityColor, final int winnerIndex, final int winnerRarity, final CompoundTag candidates) {
        final Level level = (Level)Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof final CrateBlockEntity be) {
            final List<ItemStack> cands = CrateBlockEntity.decodeItems(candidates);
            final int[] candRarities = CrateBlockEntity.decodeRarities(candidates);
            be.startAnimation(animationId, rarityColor, winnerIndex, winnerRarity, candRarities, cands);
        }
    }
}
