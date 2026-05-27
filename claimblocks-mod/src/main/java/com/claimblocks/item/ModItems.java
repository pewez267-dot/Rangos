package com.claimblocks.item;

import com.claimblocks.ClaimBlocksMod;

/**
 * Items in this mod are all BlockItems registered alongside their blocks
 * inside {@link com.claimblocks.block.ModBlocks}. This class exists for
 * forward-compatibility (extra items can be registered here later).
 */
public class ModItems {
    public static void registerModItems() {
        ClaimBlocksMod.LOGGER.info("Registering items for {}", ClaimBlocksMod.MOD_ID);
    }
}
