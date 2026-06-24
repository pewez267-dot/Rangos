package com.fantasticshortcuts.brigadier;

import com.fantasticshortcuts.data.Shortcut;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.SharedSuggestionProvider;

/**
 * Builds the client-facing Brigadier node for a shortcut alias and adds it to the
 * per-player command tree assembled by {@link ClientTreeModifier}.
 *
 * <p>Aliases are intentionally added only to the <em>client</em> tree (for tab-completion
 * and visibility); execution is handled server-side by interception, so the server's own
 * dispatcher is never mutated. When the original is exactly {@code "<root> {args}"} the
 * alias is a {@code redirect} to the original's client node, which makes the alias inherit
 * the original command's argument structure and suggestions in tab-completion.</p>
 */
public final class ShortcutRegistrar {

    private ShortcutRegistrar() {
    }

    /**
     * @param root           the player's client command-tree root being built
     * @param shortcut       the shortcut to expose
     * @param originalClient the converted client node of the original command's root
     *                       literal (possibly detached when the original is hidden by
     *                       Replace), or {@code null} if unavailable
     */
    public static void addAliasNode(final RootCommandNode<SharedSuggestionProvider> root,
                                    final Shortcut shortcut,
                                    final CommandNode<SharedSuggestionProvider> originalClient) {
        final String alias = shortcut.aliasKey();
        if (alias.isEmpty() || root.getChild(alias) != null) {
            return;
        }

        if (shortcut.isSingleRootWithArgs() && originalClient != null) {
            // Mirror the original command's args/suggestions under the alias.
            final LiteralCommandNode<SharedSuggestionProvider> node =
                    LiteralArgumentBuilder.<SharedSuggestionProvider>literal(alias)
                            .requires(s -> true)
                            .redirect(originalClient)
                            .build();
            root.addChild(node);
            return;
        }

        // Fixed-argument (or no-arg) alias: a simple runnable literal, with a free-form
        // greedy argument when the original uses {args} so the client lets the player type
        // arguments after the alias.
        final LiteralArgumentBuilder<SharedSuggestionProvider> builder =
                LiteralArgumentBuilder.<SharedSuggestionProvider>literal(alias)
                        .requires(s -> true)
                        .executes(c -> 0);
        if (shortcut.usesArgs()) {
            builder.then(RequiredArgumentBuilder
                    .<SharedSuggestionProvider, String>argument("args", StringArgumentType.greedyString())
                    .executes(c -> 0));
        }
        root.addChild(builder.build());
    }
}
