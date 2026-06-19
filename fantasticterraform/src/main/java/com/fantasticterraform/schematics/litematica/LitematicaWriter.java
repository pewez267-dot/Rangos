package com.fantasticterraform.schematics.litematica;

import com.fantasticterraform.schematics.BlockStateCodec;
import com.fantasticterraform.schematics.SchematicData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
 * Escritor del formato Litematica (.litematic). Genera una sola region con la paleta
 * de estados y los BlockStates empaquetados mediante {@link LitematicaBitArray}
 * ({@code bits = max(2, ceil(log2(paletteSize)))}).
 */
public final class LitematicaWriter {

    private static final int DATA_VERSION_1_20_1 = 3465;

    private LitematicaWriter() {
    }

    public static void write(File file, SchematicData data, String name) throws IOException {
        // Paleta con aire en el indice 0 (convencion Litematica).
        List<BlockState> palette = new ArrayList<>();
        Map<BlockState, Integer> paletteIndex = new HashMap<>();
        palette.add(SchematicData.AIR);
        paletteIndex.put(SchematicData.AIR, 0);

        long volume = data.volume();
        int[] ids = new int[(int) volume];
        long totalBlocks = 0;
        long index = 0;
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
                    if (!state.isAir()) {
                        totalBlocks++;
                    }
                    ids[(int) index++] = id;
                }
            }
        }

        int bits = LitematicaBitArray.bitsForPaletteSize(palette.size());
        LitematicaBitArray bitArray = new LitematicaBitArray(bits, volume);
        for (long i = 0; i < volume; i++) {
            bitArray.setAt(i, ids[(int) i]);
        }

        CompoundTag region = new CompoundTag();
        region.put("Position", vec(0, 0, 0));
        region.put("Size", vec(data.width, data.height, data.length));

        ListTag paletteTag = new ListTag();
        for (BlockState state : palette) {
            paletteTag.add(BlockStateCodec.toCompound(state));
        }
        region.put("BlockStatePalette", paletteTag);
        region.putLongArray("BlockStates", bitArray.getBackingArray());

        ListTag tileEntities = new ListTag();
        for (Map.Entry<BlockPos, CompoundTag> e : data.blockEntities.entrySet()) {
            CompoundTag be = e.getValue().copy();
            BlockPos pos = e.getKey();
            be.putInt("x", pos.getX());
            be.putInt("y", pos.getY());
            be.putInt("z", pos.getZ());
            tileEntities.add(be);
        }
        region.put("TileEntities", tileEntities);
        region.put("Entities", new ListTag());
        region.put("PendingBlockTicks", new ListTag());
        region.put("PendingFluidTicks", new ListTag());

        CompoundTag regions = new CompoundTag();
        regions.put(name, region);

        CompoundTag metadata = new CompoundTag();
        metadata.putString("Name", name);
        metadata.putString("Author", "Fantastic Terraform");
        metadata.putString("Description", "Exportado por Fantastic Terraform");
        long now = System.currentTimeMillis();
        metadata.putLong("TimeCreated", now);
        metadata.putLong("TimeModified", now);
        metadata.put("EnclosingSize", vec(data.width, data.height, data.length));
        metadata.putInt("RegionCount", 1);
        metadata.putLong("TotalBlocks", totalBlocks);
        metadata.putLong("TotalVolume", volume);

        CompoundTag root = new CompoundTag();
        root.putInt("Version", 6);
        root.putInt("MinecraftDataVersion", DATA_VERSION_1_20_1);
        root.put("Metadata", metadata);
        root.put("Regions", regions);

        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        NbtIo.writeCompressed(root, file);
    }

    private static CompoundTag vec(int x, int y, int z) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);
        return tag;
    }
}
