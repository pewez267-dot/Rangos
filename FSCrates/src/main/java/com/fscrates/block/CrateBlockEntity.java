package com.fscrates.block;

import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import com.fscrates.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores a placed crate's full {@link CrateConfig} in NBT so the crate persists
 * across restarts and can be opened/edited/recovered. Syncs to the client so
 * the renderer can show per-rarity visuals and the floating name.
 */
public class CrateBlockEntity extends BlockEntity {

    private CrateConfig config = new CrateConfig();
    /** Client-side spin angle for the rendered crate animation. */
    public float clientSpin = 0f;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.CRATE_BE.get(), pos, state);
    }

    public CrateConfig getConfig() {
        return config;
    }

    public void setConfig(CrateConfig config) {
        this.config = config == null ? new CrateConfig() : config;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public Rarity getRarity() {
        return config.rarity;
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("config", config.save());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("config")) {
            config = CrateConfig.load(tag.getCompound("config"));
        }
    }

    // ------------------------------------------------------------------
    // Client sync
    // ------------------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("config", config.save());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains("config")) {
            config = CrateConfig.load(tag.getCompound("config"));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null && tag.contains("config")) {
            config = CrateConfig.load(tag.getCompound("config"));
        }
    }
}
