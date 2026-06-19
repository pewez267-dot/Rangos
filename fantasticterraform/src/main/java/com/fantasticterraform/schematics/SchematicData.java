package com.fantasticterraform.schematics;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Representacion en memoria, independiente del formato, de una estructura: dimensiones,
 * matriz de estados de bloque y los datos NBT de los block entities (posicion relativa).
 * El orden de indexado es el de Sponge: {@code index = (y * length + z) * width + x}.
 */
public final class SchematicData {

    public static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public final int width;
    public final int height;
    public final int length;
    private final BlockState[] blocks;
    public final Map<BlockPos, CompoundTag> blockEntities = new HashMap<>();

    public SchematicData(int width, int height, int length) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.blocks = new BlockState[Math.max(0, width * height * length)];
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = AIR;
        }
    }

    public int index(int x, int y, int z) {
        return (y * length + z) * width + x;
    }

    public BlockState getState(int x, int y, int z) {
        return blocks[index(x, y, z)];
    }

    public void setState(int x, int y, int z, BlockState state) {
        blocks[index(x, y, z)] = state == null ? AIR : state;
    }

    public long volume() {
        return (long) width * height * length;
    }
}
