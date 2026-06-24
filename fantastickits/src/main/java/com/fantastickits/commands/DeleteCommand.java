package com.fantastickits.commands;

import com.fantastickits.FantasticKits;
import com.fantastickits.data.KitData;
import com.fantastickits.data.KitDefinition;
import com.fantastickits.security.SecurityManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /fkits delete <name>
 * Deletes an existing kit permanently.
 * Requires admin permission. Kit must exist.
 * Also removes the group command mappings if applicable.
 */
public class DeleteCommand {

    public static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cThis command can only be used by a player."));
            return 0;
        }

        // Check admin permission
        if (!SecurityManager.hasAdminPermission(player)) {
            source.sendFailure(Component.literal("§cYou do not have permission to delete kits."));
            return 0;
        }

        String kitName = StringArgumentType.getString(context, "name");
        KitData kitData = FantasticKits.getInstance().getKitData();

        // Check kit exists
        KitDefinition kit = kitData.getKit(kitName);
        if (kit == null) {
            source.sendFailure(Component.literal("§cKit '" + kitName + "' does not exist."));
            return 0;
        }

        // Remove the kit
        String actualName = kit.getName();
        String assignedGroup = kit.getAssignedGroup();
        kitData.removeKit(kitName);

        // Remove associated group commands if any
        if (assignedGroup != null && !assignedGroup.isEmpty()) {
            FantasticKits.getInstance().getGroupCommandData().removeGroup(assignedGroup);
        }

        // Log the deletion
        FantasticKits.getInstance().getAuditLog().logKitDeleted(
                player.getUUID(), player.getName().getString(), actualName);

        source.sendSuccess(() -> Component.literal("§aKit '" + actualName + "' has been deleted."), true);
        return Command.SINGLE_SUCCESS;
    }
}
