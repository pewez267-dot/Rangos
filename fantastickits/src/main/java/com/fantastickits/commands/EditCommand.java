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
 * /fkits edit <name>
 * Opens the editor GUI for an existing kit.
 * Requires admin permission. Kit must exist.
 */
public class EditCommand {

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
            source.sendFailure(Component.literal("§cYou do not have permission to edit kits."));
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

        // Open the kit editor GUI
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Edit Kit: " + kit.getName());
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player p) {
                return new KitEditMenu(containerId, playerInventory, kit.getName());
            }
        }, buf -> buf.writeUtf(kit.getName()));

        source.sendSuccess(() -> Component.literal("§aOpening editor for kit '" + kit.getName() + "'..."), true);
        return Command.SINGLE_SUCCESS;
    }
}
