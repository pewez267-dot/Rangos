package com.fantasticshortcuts.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reads the live command tree the client received from the server (the real
 * {@code CommandDispatcher}, already filtered to the player's permissions) when the
 * selector screen opens — never a hard-coded list.
 *
 * <p>Brigadier root literals carry no namespace or owning-mod metadata, so command source
 * is reported honestly as Vanilla vs Mod using the known set of 1.20.1 vanilla roots,
 * rather than inventing attribution that does not exist at runtime.</p>
 */
public final class CommandDiscovery {

    /** A discovered command: its root literal and whether it is a vanilla command. */
    public record CommandInfo(String name, boolean vanilla) {
        public String sourceLabel() {
            return vanilla ? "Minecraft (vanilla)" : "Mod / otro";
        }

        public String namespace() {
            return vanilla ? "minecraft" : "—";
        }
    }

    private static final Set<String> VANILLA = buildVanilla();

    private CommandDiscovery() {
    }

    /** All root commands currently available to the client, sorted alphabetically. */
    public static List<CommandInfo> discover() {
        final List<CommandInfo> out = new ArrayList<>();
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return out;
        }
        final CommandDispatcher<SharedSuggestionProvider> dispatcher = connection.getCommands();
        final Set<String> names = new TreeSet<>();
        for (final CommandNode<SharedSuggestionProvider> node : dispatcher.getRoot().getChildren()) {
            if (node.getName() != null && !node.getName().isBlank()) {
                names.add(node.getName().toLowerCase(Locale.ROOT));
            }
        }
        for (final String name : names) {
            out.add(new CommandInfo(name, VANILLA.contains(name)));
        }
        return out;
    }

    public static boolean isVanilla(final String command) {
        return VANILLA.contains(command == null ? "" : command.toLowerCase(Locale.ROOT));
    }

    private static Set<String> buildVanilla() {
        final Set<String> s = new HashSet<>(Arrays.asList(
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
        return Collections.unmodifiableSet(s);
    }
}
