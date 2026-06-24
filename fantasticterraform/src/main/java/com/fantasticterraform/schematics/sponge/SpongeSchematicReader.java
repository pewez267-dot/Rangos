package com.fantasticterraform.schematics.sponge;

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
import java.util.HashMap;
import java.util.Map;

/**
 * Lector del formato Sponge Schematic v2/v3 (.schem). NBT comprimido con gzip.
 * Respeta los campos reales: Width/Height/Length, Palette, BlockData en VarInt,
 * BlockEntities. Soporta tanto el layout plano de v2 como el anidado bajo
 * {@code Schematic}/{@code Blocks} de v3.
 */
public final class SpongeSchematicReader {

    private SpongeSchematicReader() {
    }

    public static SchematicData read(File file, HolderLookup<Block> lookup) throws IOException {
        CompoundTag root = NbtIo.readCompressed(file);
        CompoundTag schem = root.contains("Schematic", Tag.TAG_COMPOUND) ? root.getCompound("Schematic") : root;

        int width = schem.getShort("Width") & 0xFFFF;
        int height = schem.getShort("Height") & 0xFFFF;
        int length = schem.getShort("Length") & 0xFFFF;

        // v3 anida Palette/Data/BlockEntities bajo "Blocks".
        CompoundTag blocks = schem.contains("Blocks", Tag.TAG_COMPOUND) ? schem.getCompound("Blocks") : schem;

        CompoundTag paletteTag = blocks.getCompound("Palette");
        int maxId = 0;
        for (String key : paletteTag.getAllKeys()) {
            maxId = Math.max(maxId, paletteTag.getInt(key));
        }
        String[] byId = new String[maxId + 1];
        for (String key : paletteTag.getAllKeys()) {
            byId[paletteTag.getInt(key)] = key;
        }

        byte[] blockData = blocks.contains("BlockData") ? blocks.getByteArray("BlockData") : blocks.getByteArray("Data");
        int count = width * height * length;
        int[] indices = VarIntUtil.readAll(blockData, count);

        Map<Integer, BlockState> stateCache = new HashMap<>();
        SchematicData data = new SchematicData(width, height, length);
        for (int i = 0; i < count; i++) {
            int id = indices[i];
            BlockState state = stateCache.computeIfAbsent(id, k -> {
                String name = (k >= 0 && k < byId.length && byId[k] != null) ? byId[k] : "minecraft:air";
                return BlockStateCodec.parse(lookup, name);
            });
            int x = i % width;
            int z = (i / width) % length;
            int y = i / (width * length);
            data.setState(x, y, z, state);
        }

        readBlockEntities(blocks, data);
        return data;
    }

    private static void readBlockEntities(CompoundTag blocks, SchematicData data) {
        ListTag list = null;
        if (blocks.contains("BlockEntities", Tag.TAG_LIST)) {
            list = blocks.getList("BlockEntities", Tag.TAG_COMPOUND);
        } else if (blocks.contains("TileEntities", Tag.TAG_LIST)) {
            list = blocks.getList("TileEntities", Tag.TAG_COMPOUND);
        }
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int[] pos = entry.getIntArray("Pos");
            if (pos.length != 3) {
                continue;
            }
            CompoundTag be = entry.copy();
            be.remove("Pos");
            if (be.contains("Id")) {
                be.putString("id", be.getString("Id"));
                be.remove("Id");
            }
            data.blockEntities.put(new BlockPos(pos[0], pos[1], pos[2]), be);
        }
    }
}
