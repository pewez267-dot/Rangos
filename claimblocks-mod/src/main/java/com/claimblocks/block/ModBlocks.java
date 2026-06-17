package com.claimblocks.block;

import com.claimblocks.ClaimBlocksMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {
    /** radius (in blocks) for each tier */
    public static final int[] TIER_RADIUS = {0, 10, 20, 30, 40, 50};
    /** map colors for visual distinction */
    private static final MapColor[] TIER_COLORS = {
        MapColor.STONE_GRAY,        // unused index 0
        MapColor.STONE_GRAY,        // tier 1 - grey
        MapColor.GOLD,              // tier 2 - gold
        MapColor.DIAMOND_BLUE,      // tier 3 - diamond
        MapColor.EMERALD_GREEN,     // tier 4 - emerald
        MapColor.PURPLE             // tier 5 - netherite/purple
    };
    private static final int[] TIER_LIGHT = {0, 5, 7, 10, 12, 15};

    public static final Block CLAIM_BLOCK_TIER_1 = registerBlock("claim_block_tier_1", makeClaimBlock(1));
    public static final Block CLAIM_BLOCK_TIER_2 = registerBlock("claim_block_tier_2", makeClaimBlock(2));
    public static final Block CLAIM_BLOCK_TIER_3 = registerBlock("claim_block_tier_3", makeClaimBlock(3));
    public static final Block CLAIM_BLOCK_TIER_4 = registerBlock("claim_block_tier_4", makeClaimBlock(4));
    public static final Block CLAIM_BLOCK_TIER_5 = registerBlock("claim_block_tier_5", makeClaimBlock(5));

    private static Block makeClaimBlock(int tier) {
        AbstractBlock.Settings settings = AbstractBlock.Settings.create()
            .strength(50.0f, 1200.0f)
            .mapColor(TIER_COLORS[tier])
            .luminance(state -> TIER_LIGHT[tier])
            .sounds(BlockSoundGroup.METAL)
            .requiresTool();
        return new ClaimBlock(settings, tier, TIER_RADIUS[tier]);
    }

    private static Block registerBlock(String name, Block block) {
        Identifier id = Identifier.of(ClaimBlocksMod.MOD_ID, name);
        Block registered = Registry.register(Registries.BLOCK, id, block);
        registerBlockItem(name, registered);
        return registered;
    }

    private static void registerBlockItem(String name, Block block) {
        Identifier id = Identifier.of(ClaimBlocksMod.MOD_ID, name);
        Registry.register(
            Registries.ITEM,
            id,
            new BlockItem(block, new Item.Settings())
        );
    }

    public static void registerModBlocks() {
        ClaimBlocksMod.LOGGER.info("Registering claim blocks for {}", ClaimBlocksMod.MOD_ID);
        // Add to creative inventory under Functional tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(group -> {
            group.add(CLAIM_BLOCK_TIER_1);
            group.add(CLAIM_BLOCK_TIER_2);
            group.add(CLAIM_BLOCK_TIER_3);
            group.add(CLAIM_BLOCK_TIER_4);
            group.add(CLAIM_BLOCK_TIER_5);
        });
    }

    public static Block getBlockForTier(int tier) {
        return switch (tier) {
            case 1 -> CLAIM_BLOCK_TIER_1;
            case 2 -> CLAIM_BLOCK_TIER_2;
            case 3 -> CLAIM_BLOCK_TIER_3;
            case 4 -> CLAIM_BLOCK_TIER_4;
            case 5 -> CLAIM_BLOCK_TIER_5;
            default -> null;
        };
    }
}
