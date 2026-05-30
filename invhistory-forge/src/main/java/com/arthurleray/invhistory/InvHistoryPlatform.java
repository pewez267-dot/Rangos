package com.arthurleray.invhistory;

import java.nio.file.Path;

/**
 * Platform abstraction (kept from the original cross-loader architecture).
 * The Forge implementation lives in {@link com.arthurleray.invhistory.forge.InvHistoryForge}.
 */
public interface InvHistoryPlatform {
    InvHistoryPlatform[] INSTANCE = new InvHistoryPlatform[1];

    static InvHistoryPlatform get() {
        return INSTANCE[0];
    }

    Path getConfigDir();
}
