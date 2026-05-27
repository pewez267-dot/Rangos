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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * v4.0 visualisation: ALWAYS-ON PLAYER PREVIEW.
 *
 * The outline is drawn whenever the player holds a claim-stone item in their
 * main hand, regardless of where they are looking. The cube is centred on the
 * player's current block position - if they walked there and placed the stone
 * "right now", the drawn cube is exactly the area they would protect.
 *
 * No crosshair-target logic, no in-claim outline, no server sync.
 */
@Environment(EnvType.CLIENT)
public final class ClaimVisualization {

    private static final float ALPHA = 0.85f;

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ClaimVisualization::onRender);
    }

    private static void onRender(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        // Only when holding one of the 10 claim stones in the main hand
        ItemStack held = player.getMainHandStack();
        ClaimTier tier = tierOfHeldClaimBlock(held);
        if (tier == null) return;

        // Centre is the player's CURRENT block position - follows them every frame
        BlockPos centre = player.getBlockPos();
        int r = tier.radius;
        int h = tier.height;
        double minX = centre.getX() - r;
        double maxX = centre.getX() + r + 1;
        double minY = centre.getY() - h;
        double maxY = centre.getY() + h + 1;
        double minZ = centre.getZ() - r;
        double maxZ = centre.getZ() + r + 1;

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
