package com.claimblocks.block;

import com.claimblocks.ClaimBlocksMod;
import com.claimblocks.data.ClaimTier;
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registers all 10 claim-stone blocks. Each block has its own item, model,
 * blockstate and texture (matched on id "claimstone_NxN").
 */
public final class ModBlocks {
    /** Map color used per tier for the in-world block (purely cosmetic). */
    private static final Map<String, MapColor> MAP_COLORS = new LinkedHashMap<>();
    static {
        MAP_COLORS.put("claimstone_10x10",   MapColor.STONE_GRAY);
        MAP_COLORS.put("claimstone_25x25",   MapColor.LIGHT_BLUE);
        MAP_COLORS.put("claimstone_40x40",   MapColor.CYAN);
        MAP_COLORS.put("claimstone_64x64",   MapColor.LIME);
        MAP_COLORS.put("claimstone_80x80",   MapColor.GREEN);
        MAP_COLORS.put("claimstone_100x100", MapColor.YELLOW);
        MAP_COLORS.put("claimstone_150x150", MapColor.ORANGE);
        MAP_COLORS.put("claimstone_250x250", MapColor.RED);
        MAP_COLORS.put("claimstone_300x300", MapColor.DARK_RED);
        MAP_COLORS.put("claimstone_500x500", MapColor.PURPLE);
    }

    private static final Map<String, Block> BY_ID = new LinkedHashMap<>();

    public static void register() {
        ClaimBlocksMod.LOGGER.info("Registrando 10 bloques claimstone");
        for (ClaimTier t : ClaimTier.VALUES) {
            Block block = registerBlock(t);
            BY_ID.put(t.id, block);
        }
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(g -> {
            for (Block b : BY_ID.values()) g.add(b);
        });
    }

    private static Block registerBlock(ClaimTier tier) {
        AbstractBlock.Settings s = AbstractBlock.Settings.create()
            .strength(50.0f, 1200.0f)
            .mapColor(MAP_COLORS.getOrDefault(tier.id, MapColor.STONE_GRAY))
            .luminance(state -> 8)
            .sounds(BlockSoundGroup.METAL)
            .requiresTool();
        Block block = new ClaimStoneBlock(s, tier);
        Identifier id = Identifier.of(ClaimBlocksMod.MOD_ID, tier.id);
        Block reg = Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(reg, new Item.Settings()));
        return reg;
    }

    public static Block byId(String id) { return BY_ID.get(id); }

    public static Map<String, Block> all() { return BY_ID; }

    /** Returns the {@link ClaimTier} for the given block, or null if not one of ours. */
    public static ClaimTier tierForBlock(Block block) {
        if (block instanceof ClaimStoneBlock cb) return cb.getTier();
        return null;
    }
}
