package com.claimblocks;

import com.claimblocks.data.ClaimTier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class ClaimBlocks {
    public static final String NBT_KEY = "claimblocks";
    public static final String NBT_TIER_FIELD = "tier";

    private ClaimBlocks() {}

    /** Bloque vanilla (concreto de color) que representa cada tier. */
    public static Block blockForTier(ClaimTier tier) {
        if (tier == null) return Blocks.WHITE_CONCRETE;
        return switch (tier.id) {
            case "claimstone_10x10"   -> Blocks.WHITE_CONCRETE;
            case "claimstone_25x25"   -> Blocks.LIGHT_GRAY_CONCRETE;
            case "claimstone_40x40"   -> Blocks.CYAN_CONCRETE;
            case "claimstone_64x64"   -> Blocks.LIGHT_BLUE_CONCRETE;
            case "claimstone_80x80"   -> Blocks.LIME_CONCRETE;
            case "claimstone_100x100" -> Blocks.YELLOW_CONCRETE;
            case "claimstone_150x150" -> Blocks.ORANGE_CONCRETE;
            case "claimstone_250x250" -> Blocks.PINK_CONCRETE;
            case "claimstone_300x300" -> Blocks.MAGENTA_CONCRETE;
            case "claimstone_500x500" -> Blocks.PURPLE_CONCRETE;
            default -> Blocks.WHITE_CONCRETE;
        };
    }

    public static Item itemForTier(ClaimTier tier) {
        return blockForTier(tier).asItem();
    }

    public static boolean isClaimConcreteForTier(Block block, ClaimTier tier) {
        return block == blockForTier(tier);
    }

    public static boolean isAnyClaimConcrete(Block block) {
        for (ClaimTier t : ClaimTier.VALUES) {
            if (block == blockForTier(t)) return true;
        }
        return false;
    }

    /** Color de texto del nombre del item, acorde al concreto del tier. */
    public static ChatFormatting colorForTier(ClaimTier tier) {
        if (tier == null) return ChatFormatting.WHITE;
        return switch (tier.id) {
            case "claimstone_10x10"   -> ChatFormatting.WHITE;
            case "claimstone_25x25"   -> ChatFormatting.GRAY;
            case "claimstone_40x40"   -> ChatFormatting.AQUA;
            case "claimstone_64x64"   -> ChatFormatting.BLUE;
            case "claimstone_80x80"   -> ChatFormatting.GREEN;
            case "claimstone_100x100" -> ChatFormatting.YELLOW;
            case "claimstone_150x150" -> ChatFormatting.GOLD;
            case "claimstone_250x250" -> ChatFormatting.LIGHT_PURPLE;
            case "claimstone_300x300" -> ChatFormatting.LIGHT_PURPLE;
            case "claimstone_500x500" -> ChatFormatting.DARK_PURPLE;
            default -> ChatFormatting.WHITE;
        };
    }

    /** Crea el ItemStack de la piedra de claim: concreto vanilla + NBT marker + glint + nombre/lore. */
    public static ItemStack createTierItem(ClaimTier tier, int amount) {
        ItemStack stack = new ItemStack(itemForTier(tier), amount);
        CompoundTag tag = stack.getOrCreateTag();

        // 1. Marcador NBT para identificar la piedra al colocarla.
        CompoundTag root = new CompoundTag();
        root.putString(NBT_TIER_FIELD, tier.id);
        tag.put(NBT_KEY, root);

        // 2. Efecto de encantamiento (glint) sin mostrar el encantamiento en el tooltip.
        ListTag ench = new ListTag();
        CompoundTag e = new CompoundTag();
        e.putString("id", "minecraft:unbreaking");
        e.putInt("lvl", 1);
        ench.add(e);
        tag.put("Enchantments", ench);
        tag.putInt("HideFlags", 1); // oculta la linea del encantamiento

        // 3. Nombre con color del tier (sin cursiva).
        ChatFormatting color = colorForTier(tier);
        MutableComponent name = Component.literal("Piedra de Claim " + tier.label())
                .setStyle(Style.EMPTY.withColor(color).withBold(true).withItalic(false));
        stack.setHoverName(name);

        // 4. Lore informativo (display.Lore).
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("Tier: " + tier.id).withStyle(ChatFormatting.GRAY));
        lore.add(Component.literal("Radio: " + tier.radius + " | Altura: +/-" + tier.height).withStyle(ChatFormatting.DARK_GRAY));
        lore.add(Component.literal("Coloca para crear una zona").withStyle(color));
        setLore(stack, lore);

        return stack;
    }

    public static void setLore(ItemStack stack, List<Component> lore) {
        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag loreList = new ListTag();
        for (Component line : lore) {
            loreList.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }
        display.put("Lore", loreList);
    }

    public static String readTierId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_KEY, Tag.TAG_COMPOUND)) return null;
        CompoundTag root = tag.getCompound(NBT_KEY);
        if (!root.contains(NBT_TIER_FIELD, Tag.TAG_STRING)) return null;
        return root.getString(NBT_TIER_FIELD);
    }

    public static ClaimTier readTier(ItemStack stack) {
        String id = readTierId(stack);
        if (id == null) return null;
        return ClaimTier.byId(id);
    }
}
