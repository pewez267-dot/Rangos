package com.fantasticterraform.schematics.sponge;

import com.fantasticterraform.schematics.BlockStateCodec;
import com.fantasticterraform.schematics.SchematicData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.state.BlockState;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Escritor del formato Sponge Schematic v2 (.schem). Genera NBT comprimido con gzip
 * siguiendo la especificacion: paleta de estados, BlockData en VarInt y BlockEntities.
 */
public final class SpongeSchematicWriter {

    /** Data version de Minecraft 1.20.1. */
    private static final int DATA_VERSION_1_20_1 = 3465;

    private SpongeSchematicWriter() {
    }

    public static void write(File file, SchematicData data) throws IOException {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", 2);
        root.putInt("DataVersion", DATA_VERSION_1_20_1);
        root.putShort("Width", (short) data.width);
        root.putShort("Height", (short) data.height);
        root.putShort("Length", (short) data.length);
        root.putIntArray("Offset", new int[] {0, 0, 0});

        Map<String, Integer> palette = new HashMap<>();
        CompoundTag paletteTag = new CompoundTag();
        ByteArrayOutputStream blockData = new ByteArrayOutputStream();

        // Orden Sponge: index = (y * length + z) * width + x  ->  recorremos en ese orden.
        for (int y = 0; y < data.height; y++) {
            for (int z = 0; z < data.length; z++) {
                for (int x = 0; x < data.width; x++) {
                    BlockState state = data.getState(x, y, z);
                    String key = BlockStateCodec.serialize(state);
                    Integer id = palette.get(key);
                    if (id == null) {
                        id = palette.size();
                        palette.put(key, id);
                        paletteTag.putInt(key, id);
                    }
                    VarIntUtil.write(blockData, id);
                }
            }
        }

        root.putInt("PaletteMax", palette.size());
        root.put("Palette", paletteTag);
        root.putByteArray("BlockData", blockData.toByteArray());

        ListTag blockEntities = new ListTag();
        for (Map.Entry<BlockPos, CompoundTag> e : data.blockEntities.entrySet()) {
            CompoundTag be = e.getKey() == null ? new CompoundTag() : new CompoundTag();
            CompoundTag source = e.getValue().copy();
            BlockPos pos = e.getKey();
            be.putIntArray("Pos", new int[] {pos.getX(), pos.getY(), pos.getZ()});
            String id = source.getString("id");
            be.putString("Id", id);
            source.remove("id");
            source.remove("x");
            source.remove("y");
            source.remove("z");
            for (String key : source.getAllKeys()) {
                be.put(key, source.get(key));
            }
            blockEntities.add(be);
        }
        root.put("BlockEntities", blockEntities);

        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        NbtIo.writeCompressed(root, file);
    }
}
