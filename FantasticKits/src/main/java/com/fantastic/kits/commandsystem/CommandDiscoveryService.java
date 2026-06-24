package com.fantastic.kits.commandsystem;

import com.fantastic.kits.FantasticKits;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Walks the live Brigadier dispatcher tree at server start (and on demand) to
 * harvest every available top-level command. The result is a stable, sorted
 * snapshot used by the COMMANDS tab of {@link com.fantastic.kits.client.screen.KitEditorScreen}.
 *
 * <p>The service classifies commands as Vanilla, Forge or Modded based on a
 * conservative heuristic: the {@code source} of the registered command is
 * unfortunately not exposed by Brigadier, so we compare against well-known
 * vanilla and Forge command names instead. False positives are harmless: the
 * command is still listed, only the discovery toggles in the config decide
 * whether to include it.
 */
public final class CommandDiscoveryService {

    /** Hard-coded vanilla command names (1.20.1). Used for filtering, not for security. */
    private static final Set<String> VANILLA = Set.of(
            "advancement", "attribute", "ban", "ban-ip", "banlist", "bossbar", "clear", "clone",
            "data", "datapack", "debug", "defaultgamemode", "deop", "difficulty", "effect",
            "enchant", "execute", "experience", "fill", "fillbiome", "forceload", "function",
            "gamemode", "gamerule", "give", "help", "item", "jfr", "kick", "kill", "list",
            "locate", "loot", "me", "msg", "op", "pardon", "pardon-ip", "particle", "perf",
            "place", "playsound", "publish", "random", "recipe", "reload", "return", "ride",
            "save-all", "save-off", "save-on", "say", "schedule", "scoreboard", "seed",
            "setblock", "setidletimeout", "setworldspawn", "spawnpoint", "spectate",
            "spreadplayers", "stop", "stopsound", "summon", "tag", "team", "teammsg",
            "teleport", "tell", "tellraw", "tick", "time", "title", "tm", "tp", "trigger",
            "w", "weather", "whitelist", "worldborder", "xp"
    );

    private static final Set<String> FORGE = Set.of(
            "forge", "fml", "mods", "modlist", "track"
    );

    private final MinecraftServer server;
    private final AtomicReference<List<String>> cache = new AtomicReference<>(List.of());

    public CommandDiscoveryService(MinecraftServer server) {
        this.server = server;
        rebuild();
    }

    /** Force a full rescan; safe to call after data-pack reloads. */
    public void rebuild() {
        if (server == null) {
            cache.set(List.of());
            return;
        }
        try {
            RootCommandNode<CommandSourceStack> root = server.getCommands().getDispatcher().getRoot();
            Set<String> names = new LinkedHashSet<>();
            for (CommandNode<CommandSourceStack> node : root.getChildren()) {
                String n = node.getName();
                if (n == null || n.isBlank()) continue;
                if (n.startsWith("/")) n = n.substring(1);
                if (!keep(n)) continue;
                names.add(n.toLowerCase());
            }
            List<String> sorted = new ArrayList<>(names);
            Collections.sort(sorted);
            cache.set(Collections.unmodifiableList(sorted));
            FantasticKits.LOGGER.info("CommandDiscoveryService: {} command(s) cached.", sorted.size());
        } catch (Throwable t) {
            FantasticKits.LOGGER.error("CommandDiscoveryService rescan failed", t);
        }
    }

    public List<String> discoveredCommands() {
        return cache.get();
    }

    /** Apply config-driven include filters. */
    private boolean keep(String name) {
        if (name.equalsIgnoreCase("fkits")) return false; // never list our own
        if (VANILLA.contains(name)) return FantasticKits.config().discoverVanillaCommands;
        if (FORGE.contains(name)) return FantasticKits.config().discoverForgeCommands;
        return FantasticKits.config().discoverModCommands;
    }

    public boolean isVanilla(String name) { return VANILLA.contains(name.toLowerCase()); }
    public boolean isForge(String name) { return FORGE.contains(name.toLowerCase()); }
}
