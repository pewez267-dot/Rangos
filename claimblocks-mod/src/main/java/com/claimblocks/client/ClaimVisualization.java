package com.claimblocks.client;

import com.claimblocks.block.ClaimStoneBlock;
import com.claimblocks.data.ClaimTier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * v3.0 visualisation: PREVIEW ONLY.
 *
 * The outline is drawn only when ALL of the following are true:
 *   - The player holds a claim-stone block in main hand.
 *   - The crosshair is targeting a block ({@link BlockHitResult}).
 *
 * In every other case nothing is drawn (no in-claim outlines).
 */
@Environment(EnvType.CLIENT)
public final class ClaimVisualization {

    private static final float ALPHA = 0.7f;

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ClaimVisualization::onRender);
    }

    private static void onRender(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        // 1) Item in main hand must be one of our claim stones
        ItemStack held = player.getMainHandStack();
        ClaimTier tier = tierOfHeldClaimBlock(held);
        if (tier == null) return;

        // 2) Crosshair must be on a block
        HitResult hit = mc.crosshairTarget;
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos targetPos = bhr.getBlockPos();

        // 3) Compute prism corners
        int r = tier.radius;
        int h = tier.height;
        double minX = targetPos.getX() - r;
        double maxX = targetPos.getX() + r + 1;
        double minY = targetPos.getY() - h;
        double maxY = targetPos.getY() + h + 1;
        double minZ = targetPos.getZ() - r;
        double maxZ = targetPos.getZ() + r + 1;

        // 4) Translate by the negative camera position so vertices are in view space
        Camera camera = ctx.camera();
        Vec3d cam = camera.getPos();
        VertexConsumerProvider consumers = ctx.consumers();
        if (consumers == null) return;
        MatrixStack matrices = ctx.matrixStack();
        if (matrices == null) return;

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumer cons = consumers.getBuffer(RenderLayer.getLines());
        drawCubeEdges(cons, matrices,
            (float) minX, (float) minY, (float) minZ,
            (float) maxX, (float) maxY, (float) maxZ,
            tier.r, tier.g, tier.b, ALPHA);

        matrices.pop();

        // Force flush so the lines render this frame
        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayer.getLines());
        }
    }

    private static ClaimTier tierOfHeldClaimBlock(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof BlockItem bi) {
            Block b = bi.getBlock();
            if (b instanceof ClaimStoneBlock cs) return cs.getTier();
        }
        return null;
    }

    /** Draws the 12 edges of an axis-aligned box as world-space lines. */
    private static void drawCubeEdges(VertexConsumer cons, MatrixStack matrices,
                                      float x1, float y1, float z1,
                                      float x2, float y2, float z2,
                                      float r, float g, float b, float a) {
        // bottom
        line(cons, matrices, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(cons, matrices, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(cons, matrices, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(cons, matrices, x1, y1, z2, x1, y1, z1, r, g, b, a);
        // top
        line(cons, matrices, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(cons, matrices, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(cons, matrices, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(cons, matrices, x1, y2, z2, x1, y2, z1, r, g, b, a);
        // verticals
        line(cons, matrices, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(cons, matrices, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(cons, matrices, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(cons, matrices, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void line(VertexConsumer cons, MatrixStack matrices,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        Matrix4f mat = matrices.peek().getPositionMatrix();
        float nx = x2 - x1, ny = y2 - y1, nz = z2 - z1;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-5f) return;
        nx /= len; ny /= len; nz /= len;
        cons.vertex(mat, x1, y1, z1).color(r, g, b, a).normal(matrices.peek(), nx, ny, nz);
        cons.vertex(mat, x2, y2, z2).color(r, g, b, a).normal(matrices.peek(), nx, ny, nz);
    }
}
