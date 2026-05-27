package com.claimblocks.client;

import com.claimblocks.block.ClaimBlock;
import com.claimblocks.block.ModBlocks;
import com.claimblocks.network.ClaimNetworking;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Client-side rendering of cube outlines:
 *
 *   1. Preview: when the player holds a claim block in main-hand and is
 *      looking at a block, draw the outline of the cube that would be
 *      protected if the block were placed there.
 *   2. Active: when the server has notified us we are inside a claim
 *      (via {@link ClaimNetworking}), draw that claim's outline.
 *
 * Uses Fabric's {@link WorldRenderEvents#AFTER_TRANSLUCENT}.
 */
@Environment(EnvType.CLIENT)
public final class ClaimVisualization {
    /** Color per tier: 1=lightblue, 2=green, 3=gold, 4=orange, 5=red. */
    private static final float[][] COLORS = {
        {1f, 1f, 1f, 0.6f},
        {0.36f, 0.68f, 0.93f, 0.6f},  // tier 1
        {0.34f, 0.84f, 0.55f, 0.6f},  // tier 2
        {0.96f, 0.77f, 0.26f, 0.6f},  // tier 3
        {0.94f, 0.50f, 0.19f, 0.6f},  // tier 4
        {0.90f, 0.19f, 0.19f, 0.6f},  // tier 5
    };

    /** Last known claim from server sync, or null if outside. */
    private static volatile ClaimSnapshot active = null;

    public record ClaimSnapshot(int x, int y, int z, int radius, int tier, String ownerName) {
        public Box box() {
            return new Box(
                x - radius, y - radius, z - radius,
                x + radius + 1, y + radius + 1, z + radius + 1);
        }
    }

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ClaimVisualization::onRender);
    }

    public static void handleSync(ClaimNetworking.ClaimSyncPayload payload) {
        if (payload.radius() < 0) {
            active = null;
        } else {
            active = new ClaimSnapshot(payload.x(), payload.y(), payload.z(),
                payload.radius(), payload.tier(), payload.ownerName());
        }
    }

    private static void onRender(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        Camera camera = ctx.camera();
        Vec3d cam = camera.getPos();
        VertexConsumerProvider consumers = ctx.consumers();
        if (consumers == null) return;

        MatrixStack matrices = ctx.matrixStack();
        if (matrices == null) return;
        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumer cons = consumers.getBuffer(RenderLayer.getLines());

        // 1) preview cube if main hand holds a claim block
        ItemStack held = player.getMainHandStack();
        int previewTier = tierOfHeldClaimBlock(held);
        if (previewTier > 0) {
            HitResult hit = mc.crosshairTarget;
            if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos placeAt = bhr.getBlockPos().offset(bhr.getSide());
                int r = radiusForTier(previewTier);
                Box box = new Box(
                    placeAt.getX() - r, placeAt.getY() - r, placeAt.getZ() - r,
                    placeAt.getX() + r + 1, placeAt.getY() + r + 1, placeAt.getZ() + r + 1);
                drawBoxLines(cons, matrices, box, COLORS[previewTier]);
            }
        }

        // 2) active claim outline (set via server sync)
        ClaimSnapshot snap = active;
        if (snap != null) {
            float[] c = COLORS[Math.max(1, Math.min(5, snap.tier()))];
            drawBoxLines(cons, matrices, snap.box(), c);
        }

        matrices.pop();

        // Force flush so the lines are rendered immediately
        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayer.getLines());
        }
    }

    private static int tierOfHeldClaimBlock(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem bi) {
            Block b = bi.getBlock();
            if (b instanceof ClaimBlock cb) return cb.getTier();
            return ModBlocks.tierForBlock(b);
        }
        return 0;
    }

    private static int radiusForTier(int tier) {
        return switch (tier) {
            case 1 -> 10;
            case 2 -> 20;
            case 3 -> 30;
            case 4 -> 40;
            case 5 -> 50;
            default -> 0;
        };
    }

    /** Draw the 12 edges of an axis-aligned box as world-space lines. */
    private static void drawBoxLines(VertexConsumer cons, MatrixStack matrices,
                                     Box box, float[] color) {
        float r = color[0], g = color[1], b = color[2], a = color[3];
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;
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
                             float x1, float y1, float z1, float x2, float y2, float z2,
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
