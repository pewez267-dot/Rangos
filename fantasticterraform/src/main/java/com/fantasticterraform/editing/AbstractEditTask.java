package com.fantasticterraform.editing;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.history.BlockChange;
import com.fantasticterraform.history.EditOperation;
import com.fantasticterraform.history.HistoryManager;
import com.fantasticterraform.masks.Mask;
import com.fantasticterraform.network.EditProgressPacket;
import com.fantasticterraform.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Base de los trabajos de edicion. Aplica cada {@link Placement} respetando la
 * mascara activa, captura el estado anterior (incluido el NBT de block entities)
 * para el historial y emite progreso al HUD. Subclases solo proveen el flujo de
 * {@link Placement}s ({@link #hasNext()} / {@link #next()}).
 */
public abstract class AbstractEditTask implements EditTask {

    /** Banderas de colocacion: avisa a clientes, sin updates de vecinos ni drops (estilo editor). */
    protected static final int SET_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    protected final ServerLevel level;
    private final UUID owner;
    private final String name;
    private final int total;
    private final Mask mask;
    private final boolean recordHistory;
    private final int historyCap;
    private final EditOperation operation;

    private int processed;
    private boolean complete;
    private int ticksSinceProgress;

    protected AbstractEditTask(ServerLevel level, UUID owner, String name, int total, Mask mask, boolean recordHistory) {
        this.level = level;
        this.owner = owner;
        this.name = name;
        this.total = total;
        this.mask = mask;
        this.recordHistory = recordHistory;
        this.historyCap = TerraformConfig.GENERAL.maxUndoBlocksPerOperation.get();
        this.operation = new EditOperation(owner, level.dimension(), name);
    }

    protected abstract boolean hasNext();

    protected abstract Placement next();

    @Override
    public int tick(int budget) {
        int used = 0;
        while (used < budget && hasNext()) {
            Placement pl = next();
            used++;
            processed++;
            if (pl == null || pl.state == null) {
                continue;
            }
            if (mask != null && !mask.test(level, pl.pos)) {
                continue;
            }
            applyPlacement(pl);
        }
        if (!hasNext()) {
            complete = true;
        }
        emitProgress(false);
        return used;
    }

    private void applyPlacement(Placement pl) {
        BlockPos pos = pl.pos.immutable();
        BlockState prev = level.getBlockState(pos);

        CompoundTag prevBeData = null;
        BlockEntity prevBe = level.getBlockEntity(pos);
        if (prevBe != null) {
            prevBeData = prevBe.saveWithFullMetadata();
        }

        boolean sameState = prev == pl.state;
        if (sameState && pl.blockEntityData == null) {
            return;
        }

        level.setBlock(pos, pl.state, SET_FLAGS);

        if (pl.blockEntityData != null) {
            BlockEntity newBe = level.getBlockEntity(pos);
            if (newBe != null) {
                CompoundTag data = pl.blockEntityData.copy();
                data.putInt("x", pos.getX());
                data.putInt("y", pos.getY());
                data.putInt("z", pos.getZ());
                newBe.load(data);
                newBe.setChanged();
            }
        }

        if (recordHistory) {
            if (operation.size() < historyCap) {
                operation.add(new BlockChange(pos, prev, pl.state, prevBeData));
            } else {
                operation.markTruncated();
            }
        }
    }

    private void emitProgress(boolean done) {
        ticksSinceProgress++;
        if (!done && ticksSinceProgress < 2) {
            return;
        }
        ticksSinceProgress = 0;
        ServerPlayer player = resolvePlayer();
        if (player != null) {
            PacketHandler.sendToClient(player, new EditProgressPacket(name, processed, total, done));
        }
    }

    protected ServerPlayer resolvePlayer() {
        return level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(owner);
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public void finish() {
        if (recordHistory && !operation.isEmpty()) {
            HistoryManager.get().pushDone(operation);
        }
        emitProgress(true);
    }

    @Override
    public UUID owner() {
        return owner;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int processed() {
        return processed;
    }

    @Override
    public int total() {
        return total;
    }
}
