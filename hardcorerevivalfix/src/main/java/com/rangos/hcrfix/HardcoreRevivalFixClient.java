package com.rangos.hcrfix;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side entrypoint. Its only job is to register a receiver for the
 * version-check payload. The presence of that receiver is what proves to the
 * server that this client has the companion mod installed at the right
 * version. The handler itself is a no-op - we never actually send the packet.
 */
public class HardcoreRevivalFixClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("hardcorerevivalfix/client");

    @Override
    public void onInitializeClient() {
        try {
            ClientPlayNetworking.registerGlobalReceiver(
                    VersionCheckPayload.TYPE,
                    (payload, context) -> { /* no-op: presence is the signal */ });
        } catch (Throwable t) {
            LOGGER.warn("[hardcorerevivalfix] could not register version-check receiver: {}", t.toString());
        }
    }
}
