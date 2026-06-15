/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * This file is part of the proprietary "Fantastic Kits" software. Unauthorized
 * copying, distribution, modification, reverse engineering, modpack inclusion,
 * or use for training artificial intelligence systems is strictly prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits;

/**
 * Central, immutable reference constants for Fantastic Kits.
 *
 * <p>This class is intentionally free of any heavy imports so it can be safely
 * referenced from both the dedicated server and the physical client.</p>
 */
public final class Reference {

    private Reference() {
        // Constants holder - not instantiable.
    }

    /** The mod identifier. Must match the value declared in {@code mods.toml}. */
    public static final String MOD_ID = "fantastickits";

    /** Human-readable mod name. */
    public static final String MOD_NAME = "Fantastic Kits";

    /** Mod version. */
    public static final String VERSION = "1.0.0";

    /** The owner / author of this proprietary software. */
    public static final String AUTHOR = "Pewez777";

    /** Strong proprietary copyright notice. */
    public static final String COPYRIGHT =
            "Copyright (c) 2026 Pewez777. All Rights Reserved. "
                    + "Proprietary software - unauthorized copying, distribution, "
                    + "reverse engineering, modpack inclusion or AI training is prohibited.";

    /** Chat prefix used for all player-facing messages. */
    public static final String CHAT_PREFIX = "[F-Kits] ";

    /** Root command literal. */
    public static final String COMMAND_ROOT = "fkits";

    /** Network protocol version for the mod's {@code SimpleChannel}. */
    public static final String NETWORK_PROTOCOL = "fkits-1";

    /** Permission level required for administrative actions (operator). */
    public static final int ADMIN_PERMISSION_LEVEL = 2;
}
