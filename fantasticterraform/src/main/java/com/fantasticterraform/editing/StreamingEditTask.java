package com.fantasticterraform.editing;

import com.fantasticterraform.masks.Mask;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Iterator;
import java.util.UUID;

/**
 * Trabajo que recorre un flujo de posiciones (normalmente el bounding box de una
 * forma) y calcula el estado destino de cada una sobre la marcha. El calculo del
 * estado y la escritura se intercalan en lotes acotados por tick, evitando
 * materializar millones de bloques en memoria.
 */
public final class StreamingEditTask extends AbstractEditTask {

    /** Calcula el estado destino para una posicion; {@code null} = no tocar. */
    @FunctionalInterface
    public interface StateProvider {
        BlockState provide(ServerLevel level, BlockPos pos);
    }

    private final Iterator<BlockPos> positions;
    private final StateProvider provider;
    private final Runnable onFinish;

    public StreamingEditTask(ServerLevel level, UUID owner, String name, int total, Mask mask,
                             Iterator<BlockPos> positions, StateProvider provider) {
        this(level, owner, name, total, mask, positions, provider, null);
    }

    public StreamingEditTask(ServerLevel level, UUID owner, String name, int total, Mask mask,
                             Iterator<BlockPos> positions, StateProvider provider, Runnable onFinish) {
        super(level, owner, name, total, mask, true);
        this.positions = positions;
        this.provider = provider;
        this.onFinish = onFinish;
    }

    @Override
    public void finish() {
        super.finish();
        if (onFinish != null) {
            onFinish.run();
        }
    }

    @Override
    protected boolean hasNext() {
        return positions.hasNext();
    }

    @Override
    protected Placement next() {
        BlockPos pos = positions.next().immutable();
        BlockState state = provider.provide(level, pos);
        return state == null ? Placement.skip(pos) : Placement.of(pos, state);
    }
}
