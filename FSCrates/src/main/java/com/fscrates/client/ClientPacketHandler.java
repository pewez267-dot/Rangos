package com.fscrates.client;

import com.fscrates.client.screen.CrateEditorScreen;
import com.fscrates.config.CrateConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Client-only entry points invoked from network packet handlers. */
public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void openEditor(CompoundTag configNbt) {
        CrateConfig cfg = configNbt == null ? new CrateConfig() : CrateConfig.load(configNbt);
        Minecraft.getInstance().setScreen(new CrateEditorScreen(cfg));
    }

    public static void playAnimation(String animationId, int rarityColor, CompoundTag rewardItem,
                                     CompoundTag candidates, boolean allowSkip) {
        Minecraft.getInstance().setScreen(new com.fscrates.client.screen.AnimationScreen(
                animationId, rarityColor, rewardItem, candidates, allowSkip));
    }
}
