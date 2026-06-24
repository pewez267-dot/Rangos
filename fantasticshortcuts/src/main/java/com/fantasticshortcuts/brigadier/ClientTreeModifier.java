package com.fantasticshortcuts.brigadier;

import com.fantasticshortcuts.FantasticShortcuts;
import com.fantasticshortcuts.data.Shortcut;
import com.fantasticshortcuts.data.ShortcutManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The "Replace Original Command" system. Builds a per-player copy of the server command
 * tree — filtered to the player's permissions, with replaced originals removed and
 * shortcut aliases added — and sends it to the client via {@link ClientboundCommandsPacket}.
 *
 * <p>This only changes what the <em>client</em> sees for tab-completion/visibility. The
 * server keeps its full dispatcher intact, so security is never affected. The tree-copy
 * mirrors vanilla {@code Commands#fillUsableCommands}, evaluating each node's requirement
 * against the player so a player who cannot use a command sees neither it nor its alias.</p>
 */
public final class ClientTreeModifier {

    private ClientTreeModifier() {
    }

    public static void resendToAll(final MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendModifiedTree(player);
        }
    }

    public static void sendModifiedTree(final ServerPlayer player) {
        final MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        try {
            final CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
            final CommandSourceStack source = player.createCommandSourceStack();
            final RootCommandNode<SharedSuggestionProvider> root = new RootCommandNode<>();
            final Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> map = new HashMap<>();
            final Set<String> hidden = ShortcutManager.get().replacedOriginalRootLiterals();

            // Copy the usable server tree, skipping replaced original root commands.
            for (final CommandNode<CommandSourceStack> child : dispatcher.getRoot().getChildren()) {
                if (!child.canUse(source)) {
                    continue;
                }
                if (hidden.contains(child.getName().toLowerCase(Locale.ROOT))) {
                    continue;
                }
                final CommandNode<SharedSuggestionProvider> converted = convert(child, map);
                root.addChild(converted);
                fillChildren(child, converted, source, map);
            }

            // Add an alias node for every shortcut the player is allowed to use.
            for (final Shortcut shortcut : ShortcutManager.get().all()) {
                final String rootLiteral = shortcut.originalRootLiteral();
                if (rootLiteral.isEmpty()) {
                    continue;
                }
                final CommandNode<CommandSourceStack> originalServer = dispatcher.getRoot().getChild(rootLiteral);
                if (originalServer == null || !originalServer.canUse(source)) {
                    continue; // No permission for the original -> no alias shown.
                }
                CommandNode<SharedSuggestionProvider> originalClient = map.get(originalServer);
                if (originalClient == null) {
                    // The original is hidden by Replace (or absent from the visible root):
                    // build a detached copy so the alias can still mirror its structure.
                    originalClient = convert(originalServer, map);
                    fillChildren(originalServer, originalClient, source, map);
                }
                ShortcutRegistrar.addAliasNode(root, shortcut, originalClient);
            }

            player.connection.send(new ClientboundCommandsPacket(root));
        } catch (final Exception e) {
            FantasticShortcuts.LOGGER.error("[FantasticShortcuts] No se pudo enviar el arbol de comandos modificado a {}: {}",
                    player.getGameProfile().getName(), e.toString());
        }
    }

    private static void fillChildren(final CommandNode<CommandSourceStack> from,
                                     final CommandNode<SharedSuggestionProvider> to,
                                     final CommandSourceStack source,
                                     final Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> map) {
        for (final CommandNode<CommandSourceStack> child : from.getChildren()) {
            if (!child.canUse(source)) {
                continue;
            }
            final CommandNode<SharedSuggestionProvider> converted = convert(child, map);
            to.addChild(converted);
            if (!child.getChildren().isEmpty()) {
                fillChildren(child, converted, source, map);
            }
        }
    }

    /** Converts a {@code CommandSourceStack} node into a client {@code SharedSuggestionProvider}
     *  node, mirroring vanilla's conversion (requirement stripped, suggestions swapped,
     *  redirects remapped through {@code map}). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CommandNode<SharedSuggestionProvider> convert(final CommandNode<CommandSourceStack> node,
                                                                 final Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> map) {
        final ArgumentBuilder<SharedSuggestionProvider, ?> builder = (ArgumentBuilder) node.createBuilder();
        builder.requires(s -> true);
        if (builder.getCommand() != null) {
            builder.executes(c -> 0);
        }
        if (builder instanceof RequiredArgumentBuilder) {
            final RequiredArgumentBuilder<SharedSuggestionProvider, ?> required = (RequiredArgumentBuilder) builder;
            if (required.getSuggestionsProvider() != null) {
                required.suggests(SuggestionProviders.safelySwap(required.getSuggestionsProvider()));
            }
        }
        if (builder.getRedirect() != null) {
            builder.redirect(map.get(builder.getRedirect()));
        }
        final CommandNode<SharedSuggestionProvider> result = builder.build();
        map.put(node, result);
        return result;
    }
}
