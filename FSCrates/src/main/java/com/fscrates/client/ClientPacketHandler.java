package com.fscrates.client;

import com.fscrates.block.CrateBlockEntity;
import com.fscrates.client.screen.CrateEditorScreen;
import com.fscrates.config.CrateConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** Client-only entry points invoked from network packet handlers. */
public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void openEditor(CompoundTag configNbt) {
        CrateConfig cfg = configNbt == null ? new CrateConfig() : CrateConfig.load(configNbt);
        Minecraft.getInstance().setScreen(new CrateEditorScreen(cfg));
    }

    public static void playAnimation(BlockPos pos, String animationId, int rarityColor,
                                     CompoundTag rewardItem, CompoundTag candidates) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity be)) {
            return;
        }
        ItemStack reward = ItemStack.EMPTY;
        if (rewardItem != null) {
            CompoundTag copy = rewardItem.copy();
            copy.remove("label");
            if (!copy.isEmpty()) {
                reward = ItemStack.of(copy);
            }
        }
        List<ItemStack> cands = CrateBlockEntity.decodeItems(candidates);
        be.startAnimation(animationId, rarityColor, reward, cands);
    }
}
