package com.fantastic.kits;

/**
 * Centralised constants for Fantastic Kits.
 * <p>
 * The mod is intentionally focused on kits only and never overlaps with other
 * Fantastic family mods (FantasticSpawners, FantasticCrates).
 *
 * <p>Copyright (c) 2026 Pewez777. ALL RIGHTS RESERVED.
 */
public final class Reference {

    private Reference() {}

    public static final String MOD_ID = "fantastickits";
    public static final String MOD_NAME = "Fantastic Kits";
    public static final String AUTHOR = "Pewez777";
    public static final String COPYRIGHT = "Copyright (c) 2026 Pewez777. All Rights Reserved.";
    public static final String COMMAND_ROOT = "fkits";
    public static final String CHAT_PREFIX = "\u00A78[\u00A7bF-Kits\u00A78] \u00A7r";

    /** Required operator level to run any /fkits command. */
    public static final int OP_LEVEL = 3;

    /** Network channel protocol version. */
    public static final String NETWORK_PROTOCOL = "1";

    /** Storage subdirectories under {@code /config/fantastickits/}. */
    public static final String DIR_KITS = "kits";
    public static final String DIR_PLAYERS = "players";
    public static final String DIR_AUDIT = "audit";
    public static final String DIR_SECURITY = "security";

    /** Files. */
    public static final String FILE_CONFIG = "config.toml";
    public static final String FILE_AUDIT = "audit.log";
    public static final String FILE_SECURITY = "security.log";
}
