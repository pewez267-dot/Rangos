package com.fantasticaudit.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Helpers that turn Minecraft game objects (items, blocks, positions, dimensions) into
 * mod-agnostic, namespace-complete strings for the audit log.
 *
 * <p>Every registry id is resolved through {@link ForgeRegistries}, so blocks, items and
 * containers from any installed mod are captured with their full {@code namespace:path}
 * identifier. Nothing here ever assumes a {@code minecraft:} namespace.</p>
 */
public final class ItemSerializer {

    private ItemSerializer() {
    }

    /** @return the full registry id of an item, e.g. {@code minecraft:iron_pickaxe} or {@code modid:custom_tool}. */
    public static String itemId(Item item) {
        if (item == null) {
            return "minecraft:air";
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key != null ? key.toString() : "unknown:unregistered_item";
    }

    /** @return the full registry id of the stack's item, or {@code minecraft:air} when empty. */
    public static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "minecraft:air";
        }
        return itemId(stack.getItem());
    }

    /** @return the full registry id of a block, e.g. {@code minecraft:diamond_ore}. */
    public static String blockId(Block block) {
        if (block == null) {
            return "minecraft:air";
        }
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        return key != null ? key.toString() : "unknown:unregistered_block";
    }

    /** @return the full registry id of a block state's block. */
    public static String blockId(BlockState state) {
        if (state == null) {
            return "minecraft:air";
        }
        return blockId(state.getBlock());
    }

    /** @return {@code namespace:path} of the dimension, e.g. {@code minecraft:overworld}. */
    public static String dimension(Level level) {
        if (level == null) {
            return "unknown:unknown";
        }
        return level.dimension().location().toString();
    }

    /**
     * Compact dimension label: the path only for vanilla ({@code overworld}), full namespaced id
     * for modded dimensions so they remain unambiguous.
     */
    public static String dimShort(Level level) {
        if (level == null) {
            return "unknown";
        }
        ResourceLocation rl = level.dimension().location();
        return "minecraft".equals(rl.getNamespace()) ? rl.getPath() : rl.toString();
    }

    /** @return a {@code x,y,z} block-position string. */
    public static String pos(BlockPos pos) {
        if (pos == null) {
            return "0,0,0";
        }
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    /** @return a {@code x,y,z} string for a precise entity position (rounded to whole blocks). */
    public static String pos(Vec3 vec) {
        if (vec == null) {
            return "0,0,0";
        }
        return Math.round(vec.x) + "," + Math.round(vec.y) + "," + Math.round(vec.z);
    }

    /** @return the block-position string of an entity, or {@code 0,0,0} when {@code null}. */
    public static String pos(Entity entity) {
        if (entity == null) {
            return "0,0,0";
        }
        return pos(entity.blockPosition());
    }

    /** @return a compact {@code id xN} description of a single stack. */
    public static String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "minecraft:air x0";
        }
        return itemId(stack) + " x" + stack.getCount();
    }

    /**
     * Renders a list of dropped stacks as {@code [id xN, id xM]}. Empty lists render as {@code []}.
     *
     * @param drops the dropped stacks (may be empty, never expected {@code null})
     * @return a compact bracketed list
     */
    public static String describeDrops(List<ItemStack> drops) {
        if (drops == null || drops.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < drops.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(describeStack(drops.get(i)));
        }
        return sb.append("]").toString();
    }

    /**
     * @param drops the dropped stacks
     * @return the total number of items (summed stack counts) that dropped
     */
    public static int totalDropCount(List<ItemStack> drops) {
        if (drops == null || drops.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : drops) {
            if (stack != null) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
