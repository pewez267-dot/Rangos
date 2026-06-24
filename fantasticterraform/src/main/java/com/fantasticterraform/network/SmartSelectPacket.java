package com.fantasticterraform.network;

import com.fantasticterraform.selection.PlayerSelection;
import com.fantasticterraform.selection.SelectionManager;
import com.fantasticterraform.selection.SelectionType;
import com.fantasticterraform.selection.shapes.SetSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * C-&gt;S: seleccion "smart" por relleno de contiguidad (flood-fill). Desde el bloque
 * apuntado expande a todos los bloques contiguos del MISMO tipo, hasta un limite de
 * bloques y un radio de busqueda. El resultado es una {@link SetSelection} explicita,
 * compatible con todas las operaciones del editor.
 */
public final class SmartSelectPacket {

    private static final int SEARCH_RADIUS = 64;

    private final BlockPos origin;
    private final int maxBlocks;
    private final boolean diagonal;

    public SmartSelectPacket(BlockPos origin, int maxBlocks, boolean diagonal) {
        this.origin = origin;
        this.maxBlocks = maxBlocks;
        this.diagonal = diagonal;
    }

    public static void encode(SmartSelectPacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.origin);
        buf.writeInt(m.maxBlocks);
        buf.writeBoolean(m.diagonal);
    }

    public static SmartSelectPacket decode(FriendlyByteBuf buf) {
        return new SmartSelectPacket(buf.readBlockPos(), buf.readInt(), buf.readBoolean());
    }

    public static void handle(SmartSelectPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            ServerLevel level = (ServerLevel) player.level();
            BlockState target = level.getBlockState(m.origin);
            if (target.isAir()) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7eApunta a un bloque solido para la seleccion inteligente."));
                return;
            }
            Block block = target.getBlock();
            int cap = Math.max(1, Math.min(500_000, m.maxBlocks));

            Set<BlockPos> result = new HashSet<>();
            Set<Long> visited = new HashSet<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            queue.add(m.origin);
            visited.add(m.origin.asLong());

            int[][] offs = m.diagonal ? NEIGHBORS_26 : NEIGHBORS_6;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            while (!queue.isEmpty() && result.size() < cap) {
                BlockPos p = queue.poll();
                result.add(p);
                for (int[] o : offs) {
                    int nx = p.getX() + o[0];
                    int ny = p.getY() + o[1];
                    int nz = p.getZ() + o[2];
                    if (Math.abs(nx - m.origin.getX()) > SEARCH_RADIUS
                            || Math.abs(ny - m.origin.getY()) > SEARCH_RADIUS
                            || Math.abs(nz - m.origin.getZ()) > SEARCH_RADIUS) {
                        continue;
                    }
                    long key = BlockPos.asLong(nx, ny, nz);
                    if (!visited.add(key)) {
                        continue;
                    }
                    if (level.getBlockState(cursor.set(nx, ny, nz)).is(block)) {
                        queue.add(new BlockPos(nx, ny, nz));
                    }
                }
            }

            PlayerSelection sel = SelectionManager.get(player);
            sel.setType(SelectionType.SMART);
            sel.setExplicitShape(new SetSelection(result));
            PacketHandler.sendToClient(player, SelectionUpdatePacket.fromSelection(sel));
            player.sendSystemMessage(Component.literal("\u00a7aSeleccion inteligente: \u00a7f"
                    + result.size() + "\u00a7a bloques de \u00a7f" + block.getName().getString()
                    + (result.size() >= cap ? " \u00a77(limite alcanzado)" : "")));
        });
        c.setPacketHandled(true);
    }

    private static final int[][] NEIGHBORS_6 = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private static final int[][] NEIGHBORS_26 = build26();

    private static int[][] build26() {
        int[][] a = new int[26][3];
        int k = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    a[k][0] = dx;
                    a[k][1] = dy;
                    a[k][2] = dz;
                    k++;
                }
            }
        }
        return a;
    }
}
