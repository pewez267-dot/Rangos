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

public final class ModBlocks {
    private static final MapColor[] MAP_COLORS = {
        MapColor.STONE_GRAY,
        MapColor.LIGHT_BLUE,    // tier 1 - azul claro
        MapColor.GREEN,         // tier 2 - verde
        MapColor.YELLOW,        // tier 3 - dorado
        MapColor.ORANGE,        // tier 4 - naranja
        MapColor.RED            // tier 5 - rojo
    };

    public static final Block CLAIM_BLOCK_TIER_1 = register("claim_block_tier_1", makeTier(1));
    public static final Block CLAIM_BLOCK_TIER_2 = register("claim_block_tier_2", makeTier(2));
    public static final Block CLAIM_BLOCK_TIER_3 = register("claim_block_tier_3", makeTier(3));
    public static final Block CLAIM_BLOCK_TIER_4 = register("claim_block_tier_4", makeTier(4));
    public static final Block CLAIM_BLOCK_TIER_5 = register("claim_block_tier_5", makeTier(5));

    private static Block makeTier(int tier) {
        AbstractBlock.Settings s = AbstractBlock.Settings.create()
            .strength(50.0f, 1200.0f)
            .mapColor(MAP_COLORS[tier])
            .luminance(state -> 8 + tier)
            .sounds(BlockSoundGroup.METAL)
            .requiresTool();
        return new ClaimBlock(s, tier);
    }

    private static Block register(String name, Block block) {
        Identifier id = Identifier.of(ClaimBlocksMod.MOD_ID, name);
        Block r = Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(r, new Item.Settings()));
        return r;
    }

    public static Block forTier(int tier) {
        return switch (tier) {
            case 1 -> CLAIM_BLOCK_TIER_1;
            case 2 -> CLAIM_BLOCK_TIER_2;
            case 3 -> CLAIM_BLOCK_TIER_3;
            case 4 -> CLAIM_BLOCK_TIER_4;
            case 5 -> CLAIM_BLOCK_TIER_5;
            default -> null;
        };
    }

    public static int tierForBlock(Block block) {
        if (block == CLAIM_BLOCK_TIER_1) return 1;
        if (block == CLAIM_BLOCK_TIER_2) return 2;
        if (block == CLAIM_BLOCK_TIER_3) return 3;
        if (block == CLAIM_BLOCK_TIER_4) return 4;
        if (block == CLAIM_BLOCK_TIER_5) return 5;
        return 0;
    }

    public static void register() {
        ClaimBlocksMod.LOGGER.info("Registrando bloques (5 tiers)");
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(g -> {
            g.add(CLAIM_BLOCK_TIER_1);
            g.add(CLAIM_BLOCK_TIER_2);
            g.add(CLAIM_BLOCK_TIER_3);
            g.add(CLAIM_BLOCK_TIER_4);
            g.add(CLAIM_BLOCK_TIER_5);
        });
    }
}
