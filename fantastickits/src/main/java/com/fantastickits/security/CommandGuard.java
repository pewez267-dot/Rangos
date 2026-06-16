package com.fantastickits.security;

import com.fantastickits.config.FKConfig;
import com.fantastickits.data.GroupCommandStore;
import com.fantastickits.integration.LuckPermsIntegration;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;

import java.util.Set;
import java.util.UUID;

/**
 * Server-side command gating, enforced on every command execution via Forge's
 * {@link CommandEvent} (fired just before a parsed command runs, and cancellable).
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>A command is "gated" only if it appears in some group's allow-list in
 *       {@code group_commands.json}. Non-gated commands are never touched.</li>
 *   <li>For a gated command, a player may run it only if they belong (per LuckPerms,
 *       resolving inheritance) to at least one group that lists it. This is what stops a
 *       lower rank from using a higher rank's commands.</li>
 *   <li>Non-player sources (console, command blocks) are never gated.</li>
 *   <li>Optionally, players at or above the admin permission level bypass gating.</li>
 * </ul>
 *
 * <p>Both allowed and blocked attempts are written to the audit log.</p>
 */
public final class CommandGuard {

    private CommandGuard() {
    }

    public static void onCommand(final CommandEvent event) {
        if (!FKConfig.commandGatingEnabled()) {
            return;
        }

        final ParseResults<CommandSourceStack> results = event.getParseResults();
        final CommandSourceStack source = results.getContext().getSource();
        final ServerPlayer player = source.getPlayer();
        if (player == null) {
            return; // Console and command blocks are not subject to group gating.
        }
        if (FKConfig.opsBypassCommandGating() && source.hasPermission(FKConfig.adminPermissionLevel())) {
            return;
        }

        final String command = GroupCommandStore.normalizeCommand(results.getReader().getString());
        if (command.isEmpty()) {
            return;
        }

        final GroupCommandStore store = GroupCommandStore.get();
        if (!store.allGatedCommands().contains(command)) {
            return; // Not a restricted command.
        }

        final UUID uuid = player.getUUID();
        final String name = player.getGameProfile().getName();

        // Authoritative decision via LuckPerms: the command's permission node
        // (fantastickits.command.<cmd>) is registered ONLY on the assigned rank's group, so only
        // that rank (and groups inheriting it) hold it. Everyone else is blocked.
        if (LuckPermsIntegration.isAvailable()) {
            if (LuckPermsIntegration.hasCommandPermission(uuid, command, FKConfig.commandPermissionPrefixes())) {
                AuditLog.commandAllowed(uuid, name, command, "luckperms");
                return;
            }
            event.setCanceled(true);
            AuditLog.commandBlocked(uuid, name, command);
            source.sendFailure(Component.literal("§cTu rango no tiene permiso para usar §f/" + command + "§c."));
            return;
        }

        // Fallback when LuckPerms is unavailable: decide by group membership recorded in
        // group_commands.json (keeps the gate working on a server without LuckPerms).
        final Set<String> allowingGroups = store.groupsAllowing(command);
        for (final String group : allowingGroups) {
            if (LuckPermsIntegration.isMemberOf(uuid, group)) {
                AuditLog.commandAllowed(uuid, name, command, group);
                return; // Player is in a group that grants this command.
            }
        }

        // No qualifying group: block the command.
        event.setCanceled(true);
        AuditLog.commandBlocked(uuid, name, command);
        source.sendFailure(Component.literal("§cTu rango no tiene permiso para usar §f/" + command + "§c."));
    }
}
