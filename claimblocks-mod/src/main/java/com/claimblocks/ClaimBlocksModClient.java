package com.claimblocks;

import com.claimblocks.client.ClaimVisualization;
import net.fabricmc.api.ClientModInitializer;

public class ClaimBlocksModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClaimVisualization.register();
        ClaimBlocksMod.LOGGER.info("Claim Blocks Client initialized");
    }
}
