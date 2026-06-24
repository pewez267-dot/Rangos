package com.fantasticshortcuts.commands;

import com.fantasticshortcuts.audit.AuditLogger;
import com.fantasticshortcuts.config.ShortcutsConfig;
import com.fantasticshortcuts.data.Shortcut;
import com.fantasticshortcuts.data.ShortcutManager;
import com.fantasticshortcuts.integration.LuckPermsIntegration;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;

import java.util.Locale;
import java.util.UUID;

/**
 * Intercepts and executes shortcut aliases.
 *
 * <p>Aliases are NOT registered in the server dispatcher; instead this listener watches
 * every command via Forge's {@link CommandEvent}. When the first token of a command is a
 * known alias, the event is cancelled and the alias is handled here: the original command
 * is resolved (with {@code {args}} substitution), the player's permission for it is
 * verified, and — only if permitted — it is executed in the player's <em>own</em>
 * {@link CommandSourceStack}. The command therefore runs with the player's exact
 * permissions; it is never run as console or as a temporary OP.</p>
 */
public final class ShortcutExecutor {

    /** Guards against alias-resolving-to-alias recursion (prevents infinite loops / crashes). */
    private static final ThreadLocal<Boolean> EXECUTING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private ShortcutExecutor() {
    }

    public static void onCommand(final CommandEvent event) {
        if (EXECUTING.get()) {
            return; // We are already running a shortcut's resolved command; never re-intercept.
        }
        final ParseResults<CommandSourceStack> parse = event.getParseResults();
        final CommandSourceStack source = parse.getContext().getSource();
        final ServerPlayer player = source.getPlayer();
        if (player == null) {
            return; // Only player-issued commands are turned into shortcuts.
        }
        final String input = parse.getReader().getString();
        if (input == null || input.isBlank()) {
            return;
        }
        final String trimmed = input.trim();
        final int space = trimmed.indexOf(' ');
        final String first = (space >= 0 ? trimmed.substring(0, space) : trimmed).toLowerCase(Locale.ROOT);

        final Shortcut shortcut = ShortcutManager.get().byAlias(first);
        if (shortcut == null) {
            return; // Not an alias: let normal command handling proceed.
        }

        // It is an alias: take over completely.
        event.setCanceled(true);
        final String args = space >= 0 ? trimmed.substring(space + 1).trim() : "";
        execute(player, shortcut, args);
    }

    /** Resolves, permission-checks and runs the shortcut in the player's own context. */
    public static void execute(final ServerPlayer player, final Shortcut shortcut, final String rawArgs) {
        final String aliasLabel = "/" + shortcut.aliasKey();
        final String name = player.getGameProfile().getName();
        final UUID uuid = player.getUUID();
        final MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        final CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();

        if (!shortcut.usesArgs() && !rawArgs.isEmpty() && ShortcutsConfig.denyExtraArgs()) {
            AuditLogger.shortcutDenied(aliasLabel, name, uuid, "argumentos extra no permitidos");
            player.sendSystemMessage(Component.literal("§cEse shortcut no acepta argumentos adicionales."));
            return;
        }

        final String resolved = shortcut.resolve(rawArgs);
        if (resolved.isEmpty()) {
            AuditLogger.shortcutDenied(aliasLabel, name, uuid, "comando original vacio");
            player.sendSystemMessage(Component.literal("§cEse shortcut no tiene un comando original valido."));
            return;
        }

        // Faithful permission check — identical to the player typing the original command.
        if (!LuckPermsIntegration.canRunOriginal(player, resolved, dispatcher)) {
            AuditLogger.shortcutDenied(aliasLabel, name, uuid, "sin permiso (" + LuckPermsIntegration.providerName() + ")");
            player.sendSystemMessage(Component.literal("§cNo tienes permiso para usar ese comando."));
            return;
        }

        final BlockPos pos = player.blockPosition();
        final String posStr = pos.getX() + "," + pos.getY() + "," + pos.getZ();
        final String dim = player.level().dimension().location().toString();
        AuditLogger.shortcutUsed(aliasLabel, "/" + resolved, name, uuid, posStr, dim);

        // Execute in the player's OWN command source — no elevation, no console.
        EXECUTING.set(Boolean.TRUE);
        try {
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), resolved);
        } finally {
            EXECUTING.set(Boolean.FALSE);
        }
    }
}
