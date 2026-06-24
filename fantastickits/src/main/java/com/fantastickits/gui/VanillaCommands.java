package com.fantastickits.gui;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Best-effort classification of vanilla Minecraft 1.20.1 command roots, used only to
 * drive the "Vanilla / Mods" filter in the command-selector GUI. The synced command
 * tree carries no namespace for literals, so any root not in this set is shown as a mod
 * command. This is a display aid only and never affects gating decisions.
 */
final class VanillaCommands {

    static final Set<String> NAMES;

    static {
        final Set<String> names = new HashSet<>(Arrays.asList(
                "advancement", "attribute", "ban", "ban-ip", "banlist", "bossbar", "clear", "clone",
                "damage", "data", "datapack", "debug", "defaultgamemode", "deop", "difficulty", "effect",
                "enchant", "execute", "experience", "fill", "fillbiome", "forceload", "function", "gamemode",
                "gamerule", "give", "help", "item", "jfr", "kick", "kill", "list", "locate", "loot", "me",
                "msg", "op", "pardon", "pardon-ip", "particle", "perf", "place", "playsound", "publish",
                "random", "recipe", "reload", "return", "ride", "save-all", "save-off", "save-on", "say",
                "schedule", "scoreboard", "seed", "setblock", "setidletimeout", "setworldspawn", "spawnpoint",
                "spectate", "spreadplayers", "stop", "stopsound", "summon", "tag", "team", "teammsg",
                "teleport", "tell", "tellraw", "time", "title", "tm", "tp", "trigger", "w", "weather",
                "whitelist", "worldborder", "xp"
        ));
        NAMES = Collections.unmodifiableSet(names);
    }

    private VanillaCommands() {
    }
}
