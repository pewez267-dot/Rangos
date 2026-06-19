package com.fantasticterraform.selection.client;

import com.fantasticterraform.client.ClientEditorState;
import com.fantasticterraform.client.ClientSelectionState;
import com.fantasticterraform.selection.ConvexHull3D;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Renderiza el wireframe de la seleccion del lado cliente, en tiempo real y por frame,
 * con una geometria distinta para cada uno de los seis modos. Es puramente client-side:
 * lee {@link ClientSelectionState} (que solo cambia al llegar un packet), por lo que no
 * genera trafico de red por frame.
 */
public final class WireframeRenderer {

    private WireframeRenderer() {
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (!ClientEditorState.isActive()) {
            return;
        }
        List<BlockPos> points = ClientSelectionState.points();
        if (points.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Camera camera = event.getCamera();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();

        float[] color = ClientSelectionState.valid()
                ? new float[] {0.2F, 1.0F, 0.4F, 0.9F}
                : new float[] {0.2F, 0.8F, 1.0F, 0.9F};

        switch (ClientSelectionState.type()) {
            case CUBOID:
                renderCuboid(consumer, matrix, normal, points, color);
                break;
            case SPHERE:
                renderSphere(consumer, matrix, normal, points, color, false);
                break;
            case CYLINDER:
                renderCylinder(consumer, matrix, normal, points, color);
                break;
            case ELLIPSOID:
                renderSphere(consumer, matrix, normal, points, color, true);
                break;
            case POLYGON:
                renderPolygon(consumer, matrix, normal, points, color);
                break;
            case CONVEX_HULL:
                renderHull(consumer, matrix, normal, points, color);
                break;
            default:
                break;
        }

        // Marcadores de los puntos marcados.
        for (BlockPos p : points) {
            renderMarker(consumer, matrix, normal, p.getX() + 0.5F, p.getY() + 0.5F, p.getZ() + 0.5F, color);
        }

        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    // ----- por modo -----

    private static void renderCuboid(VertexConsumer c, Matrix4f m, Matrix3f n, List<BlockPos> pts, float[] col) {
        if (pts.size() < 2) {
            return;
        }
        BlockPos a = pts.get(0);
        BlockPos b = pts.get(1);
        float x0 = Math.min(a.getX(), b.getX());
        float y0 = Math.min(a.getY(), b.getY());
        float z0 = Math.min(a.getZ(), b.getZ());
        float x1 = Math.max(a.getX(), b.getX()) + 1;
        float y1 = Math.max(a.getY(), b.getY()) + 1;
        float z1 = Math.max(a.getZ(), b.getZ()) + 1;
        box(c, m, n, x0, y0, z0, x1, y1, z1, col);
    }

    private static void renderSphere(VertexConsumer c, Matrix4f m, Matrix3f n, List<BlockPos> pts, float[] col, boolean ellipsoid) {
        if (pts.size() < 2) {
            return;
        }
        BlockPos center = pts.get(0);
        BlockPos other = pts.get(1);
        double cx = center.getX() + 0.5;
        double cy = center.getY() + 0.5;
        double cz = center.getZ() + 0.5;
        double rx;
        double ry;
        double rz;
        if (ellipsoid) {
            rx = Math.max(0.5, Math.abs(other.getX() - center.getX()));
            ry = Math.max(0.5, Math.abs(other.getY() - center.getY()));
            rz = Math.max(0.5, Math.abs(other.getZ() - center.getZ()));
        } else {
            double dx = other.getX() - center.getX();
            double dy = other.getY() - center.getY();
            double dz = other.getZ() - center.getZ();
            double r = Math.sqrt(dx * dx + dy * dy + dz * dz);
            rx = ry = rz = r;
        }
        int segs = 24;
        // Anillos de latitud (Y constante).
        for (int lat = -2; lat <= 2; lat++) {
            double phi = lat * (Math.PI / 6.0);
            double y = cy + ry * Math.sin(phi);
            double ringX = rx * Math.cos(phi);
            double ringZ = rz * Math.cos(phi);
            ringXZ(c, m, n, cx, y, cz, ringX, ringZ, segs, col);
        }
        // Anillos de longitud (verticales).
        for (int lon = 0; lon < 4; lon++) {
            double theta = lon * (Math.PI / 4.0);
            ringVertical(c, m, n, cx, cy, cz, rx, ry, rz, theta, segs, col);
        }
    }

    private static void renderCylinder(VertexConsumer c, Matrix4f m, Matrix3f n, List<BlockPos> pts, float[] col) {
        if (pts.size() < 2) {
            return;
        }
        BlockPos base = pts.get(0);
        BlockPos edge = pts.get(1);
        double cx = base.getX() + 0.5;
        double cz = base.getZ() + 0.5;
        double dx = edge.getX() - base.getX();
        double dz = edge.getZ() - base.getZ();
        double r = Math.sqrt(dx * dx + dz * dz);
        double yBottom = base.getY();
        double yTop = base.getY() + ClientSelectionState.cylinderHeight();
        int segs = 24;
        ringXZ(c, m, n, cx, yBottom, cz, r, r, segs, col);
        ringXZ(c, m, n, cx, yTop, cz, r, r, segs, col);
        for (int i = 0; i < 4; i++) {
            double theta = i * (Math.PI / 2.0);
            float vx = (float) (cx + r * Math.cos(theta));
            float vz = (float) (cz + r * Math.sin(theta));
            line(c, m, n, vx, (float) yBottom, vz, vx, (float) yTop, vz, col);
        }
    }

    private static void renderPolygon(VertexConsumer c, Matrix4f m, Matrix3f n, List<BlockPos> pts, float[] col) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (BlockPos p : pts) {
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
        }
        float yb = minY;
        float yt = maxY + 1;
        int count = pts.size();
        boolean closed = ClientSelectionState.closed();
        int edges = closed ? count : count - 1;
        for (int i = 0; i < edges; i++) {
            BlockPos a = pts.get(i);
            BlockPos b = pts.get((i + 1) % count);
            float ax = a.getX() + 0.5F;
            float az = a.getZ() + 0.5F;
            float bx = b.getX() + 0.5F;
            float bz = b.getZ() + 0.5F;
            line(c, m, n, ax, yb, az, bx, yb, bz, col);
            line(c, m, n, ax, yt, az, bx, yt, bz, col);
        }
        for (BlockPos p : pts) {
            float px = p.getX() + 0.5F;
            float pz = p.getZ() + 0.5F;
            line(c, m, n, px, yb, pz, px, yt, pz, col);
        }
    }

    private static void renderHull(VertexConsumer c, Matrix4f m, Matrix3f n, List<BlockPos> pts, float[] col) {
        if (pts.size() < 4) {
            return;
        }
        double[][] arr = new double[pts.size()][3];
        for (int i = 0; i < pts.size(); i++) {
            arr[i][0] = pts.get(i).getX() + 0.5;
            arr[i][1] = pts.get(i).getY() + 0.5;
            arr[i][2] = pts.get(i).getZ() + 0.5;
        }
        ConvexHull3D hull = new ConvexHull3D(arr);
        if (!hull.isValid()) {
            return;
        }
        for (int[] face : hull.faces()) {
            tri(c, m, n, arr[face[0]], arr[face[1]], arr[face[2]], col);
        }
    }

    // ----- primitivas -----

    private static void ringXZ(VertexConsumer c, Matrix4f m, Matrix3f n, double cx, double y, double cz,
                               double rX, double rZ, int segs, float[] col) {
        float prevX = (float) (cx + rX);
        float prevZ = (float) cz;
        for (int i = 1; i <= segs; i++) {
            double t = (i / (double) segs) * Math.PI * 2.0;
            float x = (float) (cx + rX * Math.cos(t));
            float z = (float) (cz + rZ * Math.sin(t));
            line(c, m, n, prevX, (float) y, prevZ, x, (float) y, z, col);
            prevX = x;
            prevZ = z;
        }
    }

    private static void ringVertical(VertexConsumer c, Matrix4f m, Matrix3f n, double cx, double cy, double cz,
                                     double rx, double ry, double rz, double theta, int segs, float[] col) {
        double dirX = Math.cos(theta);
        double dirZ = Math.sin(theta);
        float prevX = (float) (cx + rx * dirX);
        float prevY = (float) cy;
        float prevZ = (float) (cz + rz * dirZ);
        for (int i = 1; i <= segs; i++) {
            double t = (i / (double) segs) * Math.PI * 2.0;
            float x = (float) (cx + rx * dirX * Math.cos(t));
            float y = (float) (cy + ry * Math.sin(t));
            float z = (float) (cz + rz * dirZ * Math.cos(t));
            line(c, m, n, prevX, prevY, prevZ, x, y, z, col);
            prevX = x;
            prevY = y;
            prevZ = z;
        }
    }

    private static void box(VertexConsumer c, Matrix4f m, Matrix3f n, float x0, float y0, float z0,
                            float x1, float y1, float z1, float[] col) {
        line(c, m, n, x0, y0, z0, x1, y0, z0, col);
        line(c, m, n, x1, y0, z0, x1, y0, z1, col);
        line(c, m, n, x1, y0, z1, x0, y0, z1, col);
        line(c, m, n, x0, y0, z1, x0, y0, z0, col);
        line(c, m, n, x0, y1, z0, x1, y1, z0, col);
        line(c, m, n, x1, y1, z0, x1, y1, z1, col);
        line(c, m, n, x1, y1, z1, x0, y1, z1, col);
        line(c, m, n, x0, y1, z1, x0, y1, z0, col);
        line(c, m, n, x0, y0, z0, x0, y1, z0, col);
        line(c, m, n, x1, y0, z0, x1, y1, z0, col);
        line(c, m, n, x1, y0, z1, x1, y1, z1, col);
        line(c, m, n, x0, y0, z1, x0, y1, z1, col);
    }

    private static void tri(VertexConsumer c, Matrix4f m, Matrix3f n, double[] a, double[] b, double[] d, float[] col) {
        line(c, m, n, (float) a[0], (float) a[1], (float) a[2], (float) b[0], (float) b[1], (float) b[2], col);
        line(c, m, n, (float) b[0], (float) b[1], (float) b[2], (float) d[0], (float) d[1], (float) d[2], col);
        line(c, m, n, (float) d[0], (float) d[1], (float) d[2], (float) a[0], (float) a[1], (float) a[2], col);
    }

    private static void renderMarker(VertexConsumer c, Matrix4f m, Matrix3f n, float x, float y, float z, float[] col) {
        float s = 0.15F;
        line(c, m, n, x - s, y, z, x + s, y, z, col);
        line(c, m, n, x, y - s, z, x, y + s, z, col);
        line(c, m, n, x, y, z - s, x, y, z + s, col);
    }

    private static void line(VertexConsumer c, Matrix4f m, Matrix3f n,
                             float x1, float y1, float z1, float x2, float y2, float z2, float[] col) {
        float nx = x2 - x1;
        float ny = y2 - y1;
        float nz = z2 - z1;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-5F) {
            nx = 0;
            ny = 1;
            nz = 0;
        } else {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        c.vertex(m, x1, y1, z1).color(col[0], col[1], col[2], col[3]).normal(n, nx, ny, nz).endVertex();
        c.vertex(m, x2, y2, z2).color(col[0], col[1], col[2], col[3]).normal(n, nx, ny, nz).endVertex();
    }
}
