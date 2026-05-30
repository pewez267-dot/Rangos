package com.claimblocks.client;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.ClaimBlocksMod;
import com.claimblocks.data.ClaimTier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renderizado CLIENT-SIDE: dibuja el contorno del area que protegeria la piedra
 * mientras el jugador la tiene en la mano. El color del contorno coincide con el
 * color del concreto del tier. Al colocar la piedra (sale de la mano) el contorno
 * desaparece automaticamente.
 */
@Mod.EventBusSubscriber(modid = ClaimBlocksMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClaimPreviewRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        // ¿Tiene una piedra de claim en alguna mano?
        ClaimTier tier = ClaimBlocks.readTier(player.getMainHandItem());
        if (tier == null) tier = ClaimBlocks.readTier(player.getOffhandItem());
        if (tier == null) return;

        // Centro = donde se colocaria la piedra (bloque mirado o su cara); si no mira a nada, su posicion.
        BlockPos center;
        HitResult hr = mc.hitResult;
        if (hr instanceof BlockHitResult bhr && hr.getType() == HitResult.Type.BLOCK) {
            BlockPos clicked = bhr.getBlockPos();
            BlockState cs = mc.level.getBlockState(clicked);
            center = cs.canBeReplaced() ? clicked : clicked.relative(bhr.getDirection());
        } else {
            center = player.blockPosition();
        }

        int r = tier.radius;
        int h = tier.height;
        AABB box = new AABB(
            center.getX() - r, center.getY() - h, center.getZ() - r,
            center.getX() + r + 1, center.getY() + h + 1, center.getZ() + r + 1
        );

        PoseStack pose = event.getPoseStack();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(RenderType.lines());
        // Color del contorno = color del concreto del tier.
        LevelRenderer.renderLineBox(pose, vc, box, tier.r, tier.g, tier.b, 1.0f);
        buf.endBatch(RenderType.lines());
        pose.popPose();
    }
}
