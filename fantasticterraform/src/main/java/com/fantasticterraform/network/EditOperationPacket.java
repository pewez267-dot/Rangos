package com.fantasticterraform.network;

import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.editing.shapes.CylinderShape;
import com.fantasticterraform.editing.shapes.PyramidShape;
import com.fantasticterraform.editing.shapes.SphereShape;
import com.fantasticterraform.masks.Mask;
import com.fantasticterraform.masks.MaskManager;
import com.fantasticterraform.schematics.BlockStateCodec;
import com.fantasticterraform.selection.SelectionManager;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C->S: ejecuta una operacion basica de edicion sobre la seleccion activa. El servidor
 * valida OP, exige una seleccion valida y delega en {@link EditOperations}, que aplica
 * la contencion real y la cola por ticks.
 */
public final class EditOperationPacket {

    public enum Op {
        FILL, CLEAR, REPLACE, SHAPE_SPHERE, SHAPE_CYLINDER, SHAPE_PYRAMID, MOVE, COPY, PASTE,
        HOLLOW, WALLS, STACK, SMOOTH3D, FILL_PATTERN, REPLACE_PATTERN
    }

    private final Op op;
    private final String blockA;
    private final String blockB;
    private final int i1;
    private final int i2;
    private final int i3;
    private final int rotation;

    public EditOperationPacket(Op op, String blockA, String blockB, int i1, int i2, int i3, int rotation) {
        this.op = op;
        this.blockA = blockA == null ? "" : blockA;
        this.blockB = blockB == null ? "" : blockB;
        this.i1 = i1;
        this.i2 = i2;
        this.i3 = i3;
        this.rotation = rotation;
    }

    public static void encode(EditOperationPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.op);
        buf.writeUtf(msg.blockA);
        buf.writeUtf(msg.blockB);
        buf.writeInt(msg.i1);
        buf.writeInt(msg.i2);
        buf.writeInt(msg.i3);
        buf.writeInt(msg.rotation);
    }

    public static EditOperationPacket decode(FriendlyByteBuf buf) {
        return new EditOperationPacket(buf.readEnum(Op.class), buf.readUtf(), buf.readUtf(),
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(EditOperationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            ServerLevel level = (ServerLevel) player.level();
            SelectionShape sel = SelectionManager.get(player).getShape();
            if (sel == null && msg.op != Op.PASTE) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7cNecesitas una seleccion activa valida para editar."));
                return;
            }
            Mask mask = MaskManager.combinedFor(player);
            HolderLookup<Block> lookup = player.server.registryAccess().lookupOrThrow(Registries.BLOCK);

            switch (msg.op) {
                case FILL:
                    EditOperations.fill(player, level, sel, BlockStateCodec.parse(lookup, msg.blockA), mask);
                    break;
                case CLEAR:
                    EditOperations.clear(player, level, sel, mask);
                    break;
                case REPLACE:
                    EditOperations.replace(player, level, sel,
                            BlockStateCodec.parse(lookup, msg.blockA),
                            BlockStateCodec.parse(lookup, msg.blockB), mask);
                    break;
                case SHAPE_SPHERE: {
                    BlockPos center = center(sel);
                    EditOperations.fillShape(player, level, sel,
                            new SphereShape(center, msg.i1), BlockStateCodec.parse(lookup, msg.blockA), mask);
                    break;
                }
                case SHAPE_CYLINDER: {
                    BlockPos base = new BlockPos(center(sel).getX(), sel.getMin().getY(), center(sel).getZ());
                    EditOperations.fillShape(player, level, sel,
                            new CylinderShape(base, msg.i1, msg.i2), BlockStateCodec.parse(lookup, msg.blockA), mask);
                    break;
                }
                case SHAPE_PYRAMID: {
                    BlockPos base = new BlockPos(center(sel).getX(), sel.getMin().getY(), center(sel).getZ());
                    EditOperations.fillShape(player, level, sel,
                            new PyramidShape(base, msg.i1, msg.i2, msg.i3 != 0),
                            BlockStateCodec.parse(lookup, msg.blockA), mask);
                    break;
                }
                case MOVE:
                    EditOperations.move(player, level, sel, new BlockPos(msg.i1, msg.i2, msg.i3), mask);
                    break;
                case COPY: {
                    int copied = EditOperations.copy(player, level, sel);
                    player.sendSystemMessage(Component.literal("\u00a7aCopiados " + copied + " bloques al portapapeles."));
                    com.fantasticterraform.editing.ClipboardManager.Clipboard clip =
                            com.fantasticterraform.editing.ClipboardManager.get(player.getUUID());
                    if (clip != null) {
                        PacketHandler.sendToClient(player,
                                ClipboardPreviewPacket.fromClipboard(clip, level));
                    }
                    break;
                }
                case PASTE: {
                    int packed = msg.rotation;
                    int rot = packed & 0x3;
                    boolean mx = (packed & 0x4) != 0;
                    boolean myF = (packed & 0x8) != 0;
                    boolean mz = (packed & 0x10) != 0;
                    int sc = Math.max(1, (packed >> 8) & 0xF);
                    EditOperations.paste(player, level, new BlockPos(msg.i1, msg.i2, msg.i3),
                            rotationFromIndex(rot), mx, myF, mz, sc, mask);
                    break;
                }
                case HOLLOW:
                    EditOperations.hollow(player, level, sel, mask);
                    break;
                case WALLS: {
                    com.fantasticterraform.editing.BlockPattern pat =
                            com.fantasticterraform.editing.BlockPattern.parse(lookup, msg.blockA);
                    if (pat == null) {
                        player.sendSystemMessage(Component.literal("\u00a7cPatron de muro invalido."));
                        break;
                    }
                    EditOperations.walls(player, level, sel, pat, seed(player), mask);
                    break;
                }
                case STACK:
                    EditOperations.stack(player, level, sel, msg.i1, msg.i2 == 0 ? 1 : -1, msg.i3, mask);
                    break;
                case SMOOTH3D:
                    EditOperations.smooth3D(player, level, sel, msg.i1, mask);
                    break;
                case FILL_PATTERN: {
                    com.fantasticterraform.editing.BlockPattern pat =
                            com.fantasticterraform.editing.BlockPattern.parse(lookup, msg.blockA);
                    if (pat == null) {
                        player.sendSystemMessage(Component.literal("\u00a7cPatron invalido. Ej: 50%stone,50%cobblestone"));
                        break;
                    }
                    EditOperations.fillPattern(player, level, sel, pat, seed(player), mask);
                    break;
                }
                case REPLACE_PATTERN: {
                    com.fantasticterraform.editing.BlockPattern pat =
                            com.fantasticterraform.editing.BlockPattern.parse(lookup, msg.blockB);
                    if (pat == null) {
                        player.sendSystemMessage(Component.literal("\u00a7cPatron invalido. Ej: 50%stone,50%cobblestone"));
                        break;
                    }
                    BlockState fromState = (msg.blockA == null || msg.blockA.isEmpty())
                            ? null : BlockStateCodec.parse(lookup, msg.blockA);
                    EditOperations.replacePattern(player, level, sel, fromState, pat, seed(player), mask);
                    break;
                }
                default:
                    break;
            }
        });
        c.setPacketHandled(true);
    }

    private static BlockPos center(SelectionShape sel) {
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        return new BlockPos((min.getX() + max.getX()) / 2, (min.getY() + max.getY()) / 2, (min.getZ() + max.getZ()) / 2);
    }

    private static long seed(ServerPlayer player) {
        return player.getUUID().hashCode() * 0x9E3779B97F4A7C15L ^ System.nanoTime();
    }

    private static Rotation rotationFromIndex(int index) {
        switch (((index % 4) + 4) % 4) {
            case 1:
                return Rotation.CLOCKWISE_90;
            case 2:
                return Rotation.CLOCKWISE_180;
            case 3:
                return Rotation.COUNTERCLOCKWISE_90;
            default:
                return Rotation.NONE;
        }
    }
}
