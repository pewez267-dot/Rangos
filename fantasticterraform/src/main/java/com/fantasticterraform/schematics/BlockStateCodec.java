package com.fantasticterraform.schematics;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Optional;

/**
 * Conversion entre {@link BlockState} y las dos representaciones usadas por los
 * formatos de schematic:
 * <ul>
 *   <li>cadena estilo Sponge: {@code minecraft:oak_log[axis=x]}</li>
 *   <li>compound estilo vanilla/litematica: {@code {Name, Properties}}</li>
 * </ul>
 */
public final class BlockStateCodec {

    private BlockStateCodec() {
    }

    // ----- cadena estilo Sponge -----

    public static String serialize(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        StringBuilder sb = new StringBuilder(id == null ? "minecraft:air" : id.toString());
        Map<Property<?>, Comparable<?>> values = state.getValues();
        if (!values.isEmpty()) {
            sb.append('[');
            boolean first = true;
            for (Map.Entry<Property<?>, Comparable<?>> e : values.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(e.getKey().getName()).append('=').append(propertyName(e.getKey(), e.getValue()));
            }
            sb.append(']');
        }
        return sb.toString();
    }

    public static BlockState parse(HolderLookup<Block> lookup, String str) {
        try {
            return BlockStateParser.parseForBlock(lookup, str, false).blockState();
        } catch (Exception e) {
            return Blocks.AIR.defaultBlockState();
        }
    }

    // ----- compound estilo vanilla/litematica -----

    public static CompoundTag toCompound(BlockState state) {
        CompoundTag tag = new CompoundTag();
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        tag.putString("Name", id == null ? "minecraft:air" : id.toString());
        Map<Property<?>, Comparable<?>> values = state.getValues();
        if (!values.isEmpty()) {
            CompoundTag props = new CompoundTag();
            for (Map.Entry<Property<?>, Comparable<?>> e : values.entrySet()) {
                props.putString(e.getKey().getName(), propertyName(e.getKey(), e.getValue()));
            }
            tag.put("Properties", props);
        }
        return tag;
    }

    public static BlockState fromCompound(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Name"));
        Block block = id == null ? Blocks.AIR : ForgeRegistries.BLOCKS.getValue(id);
        if (block == null) {
            block = Blocks.AIR;
        }
        BlockState state = block.defaultBlockState();
        if (tag.contains("Properties", 10)) {
            CompoundTag props = tag.getCompound("Properties");
            StateDefinition<Block, BlockState> def = block.getStateDefinition();
            for (String key : props.getAllKeys()) {
                Property<?> property = def.getProperty(key);
                if (property != null) {
                    state = applyProperty(state, property, props.getString(key));
                }
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.getValue(value);
        return parsed.map(t -> state.setValue(property, t)).orElse(state);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String propertyName(Property<T> property, Comparable<?> value) {
        return property.getName((T) value);
    }
}
