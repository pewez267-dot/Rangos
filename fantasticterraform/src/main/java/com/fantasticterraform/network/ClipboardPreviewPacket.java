package com.fantasticterraform.network;

import com.fantasticterraform.editing.ClipboardManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * S-&gt;C: vista previa compacta del portapapeles para el "fantasma" del pegado. Envia
 * las dimensiones y solo los vEoxeles de la CASCARA (las celdas con alguna cara
 * expuesta), con su color de mapa, limitados/submuestreados para no saturar la red ni
 * el render. El cliente lo dibuja como cubos translucidos en el destino del pegado.
 */
public final class ClipboardPreviewPacket {

    private static final int MAX_VOXELS = 4000;

    public final int width;
    public final int height;
    public final int length;
    public final int[] xs;
    public final int[] ys;
    public final int[] zs;
    public final int[] colors;

    public ClipboardPreviewPacket(int width, int height, int length, int[] xs, int[] ys, int[] zs, int[] colors) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.xs = xs;
        this.ys = ys;
        this.zs = zs;
        this.colors = colors;
    }

    /** Construye la vista previa (cascara) desde un portapapeles. {@code level} puede ser null. */
    public static ClipboardPreviewPacket fromClipboard(ClipboardManager.Clipboard clip, BlockGetter level) {
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;
        Set<Long> occupied = new HashSet<>(clip.size() * 2);
        for (ClipboardManager.Entry e : clip.entries) {
            if (e.state.isAir()) {
                continue;
            }
            occupied.add(e.rel.asLong());
            maxX = Math.max(maxX, e.rel.getX());
            maxY = Math.max(maxY, e.rel.getY());
            maxZ = Math.max(maxZ, e.rel.getZ());
        }

        // Cascara: celdas con al menos una de las 6 caras vacia.
        java.util.List<ClipboardManager.Entry> shell = new java.util.ArrayList<>();
        for (ClipboardManager.Entry e : clip.entries) {
            if (e.state.isAir()) {
                continue;
            }
            BlockPos p = e.rel;
            boolean exposed = !occupied.contains(p.east().asLong()) || !occupied.contains(p.west().asLong())
                    || !occupied.contains(p.above().asLong()) || !occupied.contains(p.below().asLong())
                    || !occupied.contains(p.south().asLong()) || !occupied.contains(p.north().asLong());
            if (exposed) {
                shell.add(e);
            }
        }

        int stride = Math.max(1, (shell.size() + MAX_VOXELS - 1) / MAX_VOXELS);
        int n = (shell.size() + stride - 1) / stride;
        int[] xs = new int[n];
        int[] ys = new int[n];
        int[] zs = new int[n];
        int[] colors = new int[n];
        int k = 0;
        for (int i = 0; i < shell.size() && k < n; i += stride) {
            ClipboardManager.Entry e = shell.get(i);
            xs[k] = e.rel.getX();
            ys[k] = e.rel.getY();
            zs[k] = e.rel.getZ();
            colors[k] = colorOf(e.state, level, e.rel);
            k++;
        }
        return new ClipboardPreviewPacket(maxX + 1, maxY + 1, maxZ + 1, trim(xs, k), trim(ys, k), trim(zs, k), trim(colors, k));
    }

    private static int[] trim(int[] a, int len) {
        if (len == a.length) {
            return a;
        }
        int[] out = new int[len];
        System.arraycopy(a, 0, out, 0, len);
        return out;
    }

    private static int colorOf(BlockState state, BlockGetter level, BlockPos pos) {
        try {
            return state.getMapColor(level, pos).col;
        } catch (Exception ignored) {
            return 0x8C8C8C;
        }
    }

    public static void encode(ClipboardPreviewPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.width);
        buf.writeVarInt(m.height);
        buf.writeVarInt(m.length);
        buf.writeVarInt(m.xs.length);
        for (int i = 0; i < m.xs.length; i++) {
            buf.writeVarInt(m.xs[i]);
            buf.writeVarInt(m.ys[i]);
            buf.writeVarInt(m.zs[i]);
            buf.writeInt(m.colors[i]);
        }
    }

    public static ClipboardPreviewPacket decode(FriendlyByteBuf buf) {
        int w = buf.readVarInt();
        int h = buf.readVarInt();
        int l = buf.readVarInt();
        int n = buf.readVarInt();
        int[] xs = new int[n];
        int[] ys = new int[n];
        int[] zs = new int[n];
        int[] colors = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = buf.readVarInt();
            ys[i] = buf.readVarInt();
            zs[i] = buf.readVarInt();
            colors[i] = buf.readInt();
        }
        return new ClipboardPreviewPacket(w, h, l, xs, ys, zs, colors);
    }

    public static void handle(ClipboardPreviewPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.fantasticterraform.client.ClientGhostState.update(m)));
        c.setPacketHandled(true);
    }
}
