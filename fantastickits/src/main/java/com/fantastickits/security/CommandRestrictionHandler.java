package com.fantastickits.security;

import com.fantastickits.FantasticKits;
import com.fantastickits.data.AuditLog;
import com.fantastickits.data.ConfigHandler;
import com.fantastickits.data.GroupCommandData;
import com.fantastickits.integration.LuckPermsIntegration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Forge event handler that intercepts command execution and validates
 * whether the player's LuckPerms group is allowed to execute the command.
 *
 * Logic:
 * - If command restrictions are disabled in config, allow everything.
 * - If the player is OP (admin), allow everything (they manage the system).
 * - If the command is NOT in ANY group's allowed list, allow it (unmanaged command).
 * - If the command IS in a group's allowed list, check if the player belongs to
 *   that group or a group that also has the command assigned.
 * - If the player's groups do not include any group that allows the command, BLOCK it.
 */
public class CommandRestrictionHandler {

    private final GroupCommandData groupCommandData;
    private final LuckPermsIntegration luckPerms;
    private final AuditLog auditLog;

    public CommandRestrictionHandler(GroupCommandData groupCommandData,
                                     LuckPermsIntegration luckPerms,
                                     AuditLog auditLog) {
        this.groupCommandData = groupCommandData;
        this.luckPerms = luckPerms;
        this.auditLog = auditLog;
    }

    @SubscribeEvent
    public void onCommandExecute(CommandEvent event) {
        // Check if command restrictions are enabled
        if (!ConfigHandler.ENABLE_COMMAND_RESTRICTIONS.get()) {
            return;
        }

        CommandSourceStack source = event.getParseResults().getContext().getSource();

        // Only restrict player-executed commands
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return; // Console commands are always allowed
        }

        // Admins bypass restrictions
        if (SecurityManager.hasAdminPermission(player)) {
            return;
        }

        // LuckPerms must be available for restrictions to work
        if (!luckPerms.isAvailable()) {
            return; // If LP isn't available, don't restrict
        }

        // Extract the root command name from the parse results
        String fullCommand = event.getParseResults().getReader().getString();
        String rootCommand = extractRootCommand(fullCommand);

        if (rootCommand.isEmpty()) {
            return;
        }

        // Check if this command is managed by ANY group
        boolean isManagedCommand = isCommandManagedByAnyGroup(rootCommand);
        if (!isManagedCommand) {
            // Unmanaged commands are not restricted by this system
            return;
        }

        // Command is managed - check if player's groups allow it
        UUID playerUUID = player.getUUID();
        List<String> playerGroups = luckPerms.getPlayerGroups(playerUUID);

        boolean allowed = false;
        for (String group : playerGroups) {
            if (groupCommandData.isCommandAllowed(group, rootCommand)) {
                allowed = true;
                break;
            }
        }

        if (allowed) {
            // Log allowed command usage
            if (ConfigHandler.LOG_COMMAND_USAGE.get()) {
                auditLog.logCommandAllowed(playerUUID, player.getName().getString(), rootCommand);
            }
        } else {
            // Block the command
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal(
                    "§cYou do not have permission to use /" + rootCommand + ". " +
                            "Your group does not include this command."));

            // Log blocked command
            if (ConfigHandler.LOG_COMMAND_USAGE.get()) {
                auditLog.logCommandBlocked(playerUUID, player.getName().getString(), rootCommand);
            }
        }
    }

    /**
     * Check if a command is listed in any group's allowed commands.
     * If it's not in any group, it's considered "unmanaged" and won't be restricted.
     */
    private boolean isCommandManagedByAnyGroup(String command) {
        Set<String> allGroups = groupCommandData.getAllGroups();
        for (String group : allGroups) {
            if (groupCommandData.isCommandAllowed(group, command)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract the root command name from a full command string.
     * E.g., "/fly speed 2" -> "fly", "gamemode creative" -> "gamemode"
     */
    private String extractRootCommand(String fullCommand) {
        String trimmed = fullCommand.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex > 0) {
            return trimmed.substring(0, spaceIndex).toLowerCase();
        }
        return trimmed.toLowerCase();
    }
}
