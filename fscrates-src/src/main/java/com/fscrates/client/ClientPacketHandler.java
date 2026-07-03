package com.fscrates.client;

import com.fscrates.block.CrateBlockEntity;
import com.fscrates.client.screen.CrateCinematicScreen;
import com.fscrates.client.screen.CrateEditorScreen;
import com.fscrates.config.CrateConfig;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void openEditor(CompoundTag configNbt) {
        ClientPacketHandler.openEditor(configNbt, null);
    }

    public static void openEditor(CompoundTag configNbt, BlockPos pos) {
        CrateConfig cfg = configNbt == null ? new CrateConfig() : CrateConfig.load(configNbt);
        Minecraft.getInstance().setScreen((Screen)new CrateEditorScreen(cfg, pos));
    }

    public static void playAnimation(BlockPos pos, String animationId, int rarityColor, int winnerIndex, int winnerRarity, CompoundTag candidates, UUID opener) {
        BlockEntity blockEntity;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != null && (blockEntity = level.getBlockEntity(pos)) instanceof CrateBlockEntity) {
            boolean cinematic;
            CrateBlockEntity be = (CrateBlockEntity)blockEntity;
            List<ItemStack> cands = CrateBlockEntity.decodeItems(candidates);
            int[] candRarities = CrateBlockEntity.decodeRarities(candidates);
            boolean isOpener = mc.player != null && opener != null && mc.player.getUUID().equals(opener);
            // TODAS las crates del que abre usan la MISMA cinematica (comun, rara, etc.),
            // sin excepcion. Antes, si el animationId era "instant" (o distinto), caia en la
            // animacion in-world (haz+ruleta+hologramas) = pesada y "distinta" -> lag horrible
            // en las clasicas. Ahora el opener SIEMPRE ve la cinematica si hay premios.
            boolean bl = cinematic = isOpener && cands != null && !cands.isEmpty();
            if (cinematic) {
                try {
                    mc.setScreen((Screen)new CrateCinematicScreen(be.getConfig(), rarityColor, winnerRarity, winnerIndex, cands, candRarities));
                    return;
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
            be.startAnimation(animationId, rarityColor, winnerIndex, winnerRarity, candRarities, cands);
        }
    }
}

