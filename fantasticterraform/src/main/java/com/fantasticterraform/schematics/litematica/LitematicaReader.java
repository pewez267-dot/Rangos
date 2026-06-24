package com.fantasticterraform.schematics.litematica;

import com.fantasticterraform.schematics.BlockStateCodec;
import com.fantasticterraform.schematics.SchematicData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Lector del formato Litematica (.litematic). NBT comprimido con gzip que contiene
 * Metadata y Regions; cada region tiene Position, Size, BlockStatePalette y
 * BlockStates empaquetados con {@link LitematicaBitArray}. Se lee la primera region.
 */
public final class LitematicaReader {

    private LitematicaReader() {
    }

    public static SchematicData read(File file, HolderLookup<Block> lookup) throws IOException {
        CompoundTag root = NbtIo.readCompressed(file);
        CompoundTag regions = root.getCompound("Regions");
        Iterator<String> it = regions.getAllKeys().iterator();
        if (!it.hasNext()) {
            throw new IOException("El archivo .litematic no contiene regiones.");
        }
        CompoundTag region = regions.getCompound(it.next());

        CompoundTag size = region.getCompound("Size");
        int sx = Math.abs(size.getInt("x"));
        int sy = Math.abs(size.getInt("y"));
        int sz = Math.abs(size.getInt("z"));

        ListTag paletteList = region.getList("BlockStatePalette", Tag.TAG_COMPOUND);
        BlockState[] palette = new BlockState[paletteList.size()];
        for (int i = 0; i < paletteList.size(); i++) {
            palette[i] = BlockStateCodec.fromCompound(paletteList.getCompound(i));
        }

        long[] blockStates = region.getLongArray("BlockStates");
        int bits = LitematicaBitArray.bitsForPaletteSize(palette.length);
        long volume = (long) sx * sy * sz;
        LitematicaBitArray bitArray = new LitematicaBitArray(bits, volume, blockStates);

        SchematicData data = new SchematicData(sx, sy, sz);
        long index = 0;
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++) {
                    int id = bitArray.getAt(index++);
                    if (id >= 0 && id < palette.length) {
                        data.setState(x, y, z, palette[id]);
                    }
                }
            }
        }

        if (region.contains("TileEntities", Tag.TAG_LIST)) {
            ListTag tiles = region.getList("TileEntities", Tag.TAG_COMPOUND);
            for (int i = 0; i < tiles.size(); i++) {
                CompoundTag entry = tiles.getCompound(i).copy();
                int x = entry.getInt("x");
                int y = entry.getInt("y");
                int z = entry.getInt("z");
                data.blockEntities.put(new BlockPos(x, y, z), entry);
            }
        }
        return data;
    }
}
