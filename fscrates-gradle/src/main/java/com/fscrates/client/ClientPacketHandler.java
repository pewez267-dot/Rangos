// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.client;

import net.minecraft.world.item.ItemStack;
import java.util.List;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import com.fscrates.block.CrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.screens.Screen;
import com.fscrates.client.screen.CrateEditorScreen;
import net.minecraft.client.Minecraft;
import com.fscrates.config.CrateConfig;
import net.minecraft.nbt.CompoundTag;

public final class ClientPacketHandler
{
    private ClientPacketHandler() {
    }
    
    public static void openEditor(final CompoundTag configNbt) {
        final CrateConfig cfg = (configNbt == null) ? new CrateConfig() : CrateConfig.load(configNbt);
        Minecraft.m_91087_().m_91152_((Screen)new CrateEditorScreen(cfg));
    }
    
    public static void playAnimation(final BlockPos pos, final String animationId, final int rarityColor, final int winnerIndex, final CompoundTag candidates) {
        final Level level = (Level)Minecraft.m_91087_().f_91073_;
        if (level == null) {
            return;
        }
        final BlockEntity 7702_ = level.m_7702_(pos);
        if (7702_ instanceof final CrateBlockEntity be) {
            final List<ItemStack> cands = CrateBlockEntity.decodeItems(candidates);
            be.startAnimation(animationId, rarityColor, winnerIndex, cands);
        }
    }
}
