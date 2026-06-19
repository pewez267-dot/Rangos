package com.fantasticterraform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * Dibuja el "fantasma" del pegado: cubos translucidos en el destino, con la misma
 * transformacion (rotacion en Y, espejo X/Y/Z y escala) que aplicara el servidor, para
 * ver EXACTAMENTE donde y como caera el portapapeles antes de pegarlo. Es puramente
 * client-side: lee {@link ClientGhostState} (que solo cambia al recibir un packet).
 */
public final class ClientGhostRenderer {

    private ClientGhostRenderer() {
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (!ClientToolState.ghostEnabled || !ClientGhostState.hasPreview()) {
            return;
        }
        if (!ClientWand.hudAvailable()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        BlockPos origin = mc.player.blockPosition();
        int s = Math.max(1, Math.min(8, ClientToolState.pasteScale));
        int rot = ((ClientToolState.pasteRotation % 4) + 4) % 4;
        boolean mx = ClientToolState.mirrorX;
        boolean my = ClientToolState.mirrorY;
        boolean mz = ClientToolState.mirrorZ;
        int maxX = ClientGhostState.width() - 1;
        int maxY = ClientGhostState.height() - 1;
        int maxZ = ClientGhostState.length() - 1;

        Camera camera = event.getCamera();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.debugFilledBox());

        int count = ClientGhostState.count();
        for (int i = 0; i < count; i++) {
            int rx = mx ? maxX - ClientGhostState.x(i) : ClientGhostState.x(i);
            int ry = my ? maxY - ClientGhostState.y(i) : ClientGhostState.y(i);
            int rz = mz ? maxZ - ClientGhostState.z(i) : ClientGhostState.z(i);
            int[] r = rotateY(rx, ry, rz, rot);

            double wx = origin.getX() + r[0] * s;
            double wy = origin.getY() + r[1] * s;
            double wz = origin.getZ() + r[2] * s;

            int col = ClientGhostState.color(i);
            float cr = ((col >> 16) & 0xFF) / 255.0F;
            float cg = ((col >> 8) & 0xFF) / 255.0F;
            float cb = (col & 0xFF) / 255.0F;

            LevelRenderer.addChainedFilledBoxVertices(pose, consumer,
                    wx, wy, wz, wx + s, wy + s, wz + s, cr, cg, cb, 0.45F);
        }

        buffers.endBatch(RenderType.debugFilledBox());
        pose.popPose();
    }

    private static int[] rotateY(int x, int y, int z, int rot) {
        switch (rot) {
            case 1:  // CW 90
                return new int[] {-z, y, x};
            case 2:  // 180
                return new int[] {-x, y, -z};
            case 3:  // CCW 90
                return new int[] {z, y, -x};
            default:
                return new int[] {x, y, z};
        }
    }
}
