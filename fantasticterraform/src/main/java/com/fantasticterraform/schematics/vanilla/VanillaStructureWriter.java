package com.fantasticterraform.schematics.vanilla;

import com.fantasticterraform.schematics.BlockStateCodec;
import com.fantasticterraform.schematics.SchematicData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Escritor del formato de estructura vanilla (.nbt), compatible con el bloque de
 * estructura nativo: paleta de estados, lista de bloques con pos/state/nbt y
 * DataVersion. NBT comprimido con gzip.
 */
public final class VanillaStructureWriter {

    private static final int DATA_VERSION_1_20_1 = 3465;

    private VanillaStructureWriter() {
    }

    public static void write(File file, SchematicData data) throws IOException {
        List<BlockState> palette = new ArrayList<>();
        Map<BlockState, Integer> paletteIndex = new HashMap<>();

        ListTag blocks = new ListTag();
        for (int y = 0; y < data.height; y++) {
            for (int z = 0; z < data.length; z++) {
                for (int x = 0; x < data.width; x++) {
                    BlockState state = data.getState(x, y, z);
                    Integer id = paletteIndex.get(state);
                    if (id == null) {
                        id = palette.size();
                        palette.add(state);
                        paletteIndex.put(state, id);
                    }
                    CompoundTag entry = new CompoundTag();
                    entry.put("pos", intList(x, y, z));
                    entry.putInt("state", id);
                    CompoundTag be = data.blockEntities.get(new BlockPos(x, y, z));
                    if (be != null) {
                        CompoundTag copy = be.copy();
                        copy.remove("x");
                        copy.remove("y");
                        copy.remove("z");
                        entry.put("nbt", copy);
                    }
                    blocks.add(entry);
                }
            }
        }

        ListTag paletteTag = new ListTag();
        for (BlockState state : palette) {
            paletteTag.add(BlockStateCodec.toCompound(state));
        }

        CompoundTag root = new CompoundTag();
        root.put("size", intList(data.width, data.height, data.length));
        root.put("palette", paletteTag);
        root.put("blocks", blocks);
        root.put("entities", new ListTag());
        root.putInt("DataVersion", DATA_VERSION_1_20_1);

        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        NbtIo.writeCompressed(root, file);
    }

    private static ListTag intList(int x, int y, int z) {
        ListTag list = new ListTag();
        list.add(IntTag.valueOf(x));
        list.add(IntTag.valueOf(y));
        list.add(IntTag.valueOf(z));
        return list;
    }
}
