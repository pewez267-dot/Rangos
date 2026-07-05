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
            CrateBlockEntity be = (CrateBlockEntity)blockEntity;
            List<ItemStack> cands = CrateBlockEntity.decodeItems(candidates);
            int[] candRarities = CrateBlockEntity.decodeRarities(candidates);
            boolean isOpener = mc.player != null && opener != null && mc.player.getUUID().equals(opener);
            boolean isInstant = "instant".equals(animationId);
            // El opener ve la cinematica fullscreen SIEMPRE que haya premios (salvo skip
            // instant). Ademas, la crate FISICA del suelo abre su tapa sincronizada con la
            // escena y la cierra al terminar (modo sceneLid), reactivando sus particulas por
            // defecto: asi los que estan detras del jugador VEN abrir/cerrar la cajita.
            boolean cinematic = isOpener && cands != null && !cands.isEmpty() && !isInstant;
            if (cinematic) {
                try {
                    mc.setScreen((Screen)new CrateCinematicScreen(be.getConfig(), rarityColor, winnerRarity, winnerIndex, cands, candRarities));
                }
                catch (Throwable throwable) {
                    be.startAnimation(animationId, rarityColor, winnerIndex, winnerRarity, candRarities, cands);
                    return;
                }
                be.startSceneLid(rarityColor, winnerRarity);
                return;
            }
            if (!isInstant) {
                // Bystanders (y opener sin premios): misma tapa sincronizada + particulas,
                // sin haz/ruleta (eso solo sale en la pantalla del opener).
                be.startSceneLid(rarityColor, winnerRarity);
            } else {
                be.startAnimation(animationId, rarityColor, winnerIndex, winnerRarity, candRarities, cands);
            }
        }
    }
}

