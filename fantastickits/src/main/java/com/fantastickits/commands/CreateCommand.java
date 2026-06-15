package com.fantastickits.commands;

import com.fantastickits.FantasticKits;
import com.fantastickits.data.KitData;
import com.fantastickits.data.KitDefinition;
import com.fantastickits.gui.KitEditMenu;
import com.fantastickits.security.SecurityManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;

/**
 * /fkits create <name>
 * Creates a new empty kit and opens the editor GUI.
 * Requires admin permission. Kit name must not already exist.
 */
public class CreateCommand {

    public static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        // Must be executed by a player
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cThis command can only be used by a player."));
            return 0;
        }

        // Check admin permission
        if (!SecurityManager.hasAdminPermission(player)) {
            source.sendFailure(Component.literal("§cYou do not have permission to create kits."));
            return 0;
        }

        String kitName = StringArgumentType.getString(context, "name");

        // Validate kit name
        if (kitName.isEmpty() || kitName.length() > 32) {
            source.sendFailure(Component.literal("§cKit name must be between 1 and 32 characters."));
            return 0;
        }

        if (!kitName.matches("[a-zA-Z0-9_-]+")) {
            source.sendFailure(Component.literal("§cKit name can only contain letters, numbers, underscores, and hyphens."));
            return 0;
        }

        KitData kitData = FantasticKits.getInstance().getKitData();

        // Check if kit already exists
        if (kitData.kitExists(kitName)) {
            source.sendFailure(Component.literal("§cA kit with name '" + kitName + "' already exists."));
            return 0;
        }

        // Create the new kit with empty items and no assigned group
        KitDefinition newKit = new KitDefinition(kitName, "");
        kitData.addKit(newKit);

        // Log the creation
        FantasticKits.getInstance().getAuditLog().logKitCreated(
                player.getUUID(), player.getName().getString(), kitName);

        // Open the kit editor GUI
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Edit Kit: " + kitName);
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player p) {
                return new KitEditMenu(containerId, playerInventory, kitName);
            }
        }, buf -> buf.writeUtf(kitName));

        source.sendSuccess(() -> Component.literal("§aKit '" + kitName + "' created. Opening editor..."), true);
        return Command.SINGLE_SUCCESS;
    }
}
