package com.fantasticchest.item;

import com.fantasticchest.block.ModBlocks;
import com.fantasticchest.inventory.CompressedInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The placeable Fantastic Chest item. Carries the full chest configuration in its NBT
 * under {@code BlockEntityTag}, which vanilla applies to the {@link com.fantasticchest.block.ChestBlockEntity}
 * on placement — no server round-trip when placing.
 */
public final class ChestItem extends BlockItem {

    public ChestItem(final Properties properties) {
        super(ModBlocks.CHEST_BLOCK.get(), properties);
    }

    /** Builds a fully-configured chest item stack from a finished configuration. */
    public static ItemStack buildStack(final String id, final String name, final UUID owner,
                                       final Set<UUID> permitted,
                                       final CompressedInventory inventory,
                                       final CompressedInventory original) {
        final ItemStack stack = new ItemStack(ModBlocks.CHEST_BLOCK.get());
        final CompoundTag be = new CompoundTag();
        be.putString("fc_id", id == null ? "" : id);
        be.putString("fc_name", name == null ? "" : name);
        be.putString("fc_owner", owner == null ? "" : owner.toString());
        final ListTag permittedTag = new ListTag();
        if (permitted != null) {
            for (final UUID uuid : permitted) {
                permittedTag.add(StringTag.valueOf(uuid.toString()));
            }
        }
        be.put("fc_permitted", permittedTag);
        be.put("fc_inventory", inventory == null ? new CompoundTag() : inventory.toNbt());
        be.put("fc_original", original == null ? new CompoundTag() : original.toNbt());
        stack.getOrCreateTag().put("BlockEntityTag", be);
        if (name != null && !name.isBlank()) {
            stack.setHoverName(Component.literal(name));
        }
        return stack;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Level level, final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        final CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("BlockEntityTag")) {
            final CompoundTag be = tag.getCompound("BlockEntityTag");
            final String id = be.getString("fc_id");
            if (!id.isBlank()) {
                tooltip.add(Component.literal("§7ID: §f" + id));
            }
            if (be.contains("fc_inventory")) {
                tooltip.add(Component.literal("§7Tipos de items: §f" + be.getCompound("fc_inventory").getAllKeys().size()));
            }
        }
    }
}
