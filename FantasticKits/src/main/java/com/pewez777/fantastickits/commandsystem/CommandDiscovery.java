/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.commandsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

/**
 * Automatic command discovery.
 *
 * <p>Walks the live Brigadier command tree at runtime to enumerate every
 * registered top-level command - vanilla, Forge, other mods and any command
 * registered dynamically. The resulting catalogue powers the Command Manager
 * GUI (search, filter and multi-select).</p>
 */
public final class CommandDiscovery {

    private CommandDiscovery() {
    }

    /** Discovers all registered root command literals from a running server. */
    public static List<String> discoverRootCommands(MinecraftServer server) {
        if (server == null) {
            return new ArrayList<>();
        }
        return discoverRootCommands(server.getCommands().getDispatcher());
    }

    /** Discovers all registered root command literals from a dispatcher. */
    public static List<String> discoverRootCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TreeSet keeps the catalogue sorted and de-duplicated.
        TreeSet<String> commands = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (dispatcher == null) {
            return new ArrayList<>(commands);
        }
        for (CommandNode<CommandSourceStack> child : dispatcher.getRoot().getChildren()) {
            if (child instanceof LiteralCommandNode) {
                String name = child.getName();
                if (name != null && !name.isBlank()) {
                    commands.add(name);
                }
            }
        }
        return new ArrayList<>(commands);
    }
}
