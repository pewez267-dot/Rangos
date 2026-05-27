package com.claimblocks;

import com.claimblocks.client.ClaimVisualization;
import com.claimblocks.network.ClaimNetworking;
import net.fabricmc.api.ClientModInitializer;

public class ClaimBlocksModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClaimNetworking.registerClient();
        ClaimVisualization.register();
        ClaimBlocksMod.LOGGER.info("[ClaimBlocks] Cliente inicializado.");
    }
}
