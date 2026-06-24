package com.fantasticshortcuts.integration;

import com.fantasticshortcuts.FantasticShortcuts;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.util.Locale;

/**
 * Permission verification for shortcut execution.
 *
 * <p><b>Core guarantee:</b> a shortcut never elevates, replaces or bypasses the original
 * command's permissions. The check {@link #canRunOriginal} evaluates the original
 * command's <em>own</em> Brigadier requirement against the player's own
 * {@link CommandSourceStack} — exactly what the game does when the player types the
 * command. On a LuckPerms-managed server the player's command access is governed by
 * LuckPerms, so this check inherently respects LuckPerms; it can never grant more than
 * the player already has.</p>
 *
 * <p>The actual {@code net.luckperms.*} API is confined to the nested {@link Hooks} class
 * and only touched when LuckPerms is installed, so this facade always loads safely.</p>
 */
public final class LuckPermsIntegration {

    public static final String MOD_ID = "luckperms";

    private LuckPermsIntegration() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /** True if LuckPerms is installed and its API is currently available. */
    public static boolean isAvailable() {
        if (!isLoaded()) {
            return false;
        }
        try {
            return Hooks.ping();
        } catch (final Throwable t) {
            return false;
        }
    }

    /** Human-readable name of the active permission provider, for audit messages. */
    public static String providerName() {
        return isAvailable() ? "LuckPerms" : "vanilla";
    }

    /**
     * Whether {@code player} may run {@code resolvedCommand} (without leading slash),
     * evaluated identically to the player typing it: the root command node's requirement
     * is checked against the player's command source. Returns {@code false} if the command
     * does not exist or the player lacks permission.
     */
    public static boolean canRunOriginal(final ServerPlayer player, final String resolvedCommand,
                                         final CommandDispatcher<CommandSourceStack> dispatcher) {
        if (player == null || dispatcher == null || resolvedCommand == null || resolvedCommand.isBlank()) {
            return false;
        }
        final String trimmed = resolvedCommand.trim();
        final int space = trimmed.indexOf(' ');
        final String root = (space >= 0 ? trimmed.substring(0, space) : trimmed).toLowerCase(Locale.ROOT);
        final CommandNode<CommandSourceStack> node = dispatcher.getRoot().getChild(root);
        if (node == null) {
            return false;
        }
        try {
            return node.canUse(player.createCommandSourceStack());
        } catch (final Exception e) {
            FantasticShortcuts.LOGGER.warn("[FantasticShortcuts] Error verificando permiso de '{}': {}", root, e.toString());
            return false;
        }
    }

    /**
     * Isolated holder for the actual LuckPerms API. Loaded lazily and only from guarded
     * call sites, so {@code net.luckperms.*} is never resolved when LuckPerms is absent.
     */
    private static final class Hooks {
        static boolean ping() {
            net.luckperms.api.LuckPermsProvider.get();
            return true;
        }
    }
}
