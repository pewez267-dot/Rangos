package com.fantasticterraform.schematics.vanilla;

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

/**
 * Lector del formato de estructura vanilla (.nbt), el mismo que usa el bloque de
 * estructura: {@code size}, {@code palette} (Name/Properties), {@code blocks}
 * (pos/state/nbt) y {@code DataVersion}. NBT comprimido con gzip.
 */
public final class VanillaStructureReader {

    private VanillaStructureReader() {
    }

    public static SchematicData read(File file, HolderLookup<Block> lookup) throws IOException {
        CompoundTag root = NbtIo.readCompressed(file);

        ListTag size = root.getList("size", Tag.TAG_INT);
        int w = size.getInt(0);
        int h = size.getInt(1);
        int l = size.getInt(2);

        ListTag paletteList = root.getList("palette", Tag.TAG_COMPOUND);
        BlockState[] palette = new BlockState[paletteList.size()];
        for (int i = 0; i < paletteList.size(); i++) {
            palette[i] = BlockStateCodec.fromCompound(paletteList.getCompound(i));
        }

        SchematicData data = new SchematicData(w, h, l);
        ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i);
            ListTag pos = entry.getList("pos", Tag.TAG_INT);
            int x = pos.getInt(0);
            int y = pos.getInt(1);
            int z = pos.getInt(2);
            if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= l) {
                continue;
            }
            int stateId = entry.getInt("state");
            if (stateId >= 0 && stateId < palette.length) {
                data.setState(x, y, z, palette[stateId]);
            }
            if (entry.contains("nbt", Tag.TAG_COMPOUND)) {
                data.blockEntities.put(new BlockPos(x, y, z), entry.getCompound("nbt").copy());
            }
        }
        return data;
    }
}
