package com.pewez.fantasticessentials.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pewez.fantasticessentials.util.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;

public final class MenuCommands {

    private MenuCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        register(dispatcher, "anvil", "fantasticessentials.command.anvil", "container.repair",
                (id, inv, player) -> new AnvilMenu(id, inv, ContainerLevelAccess.NULL));
        register(dispatcher, "cartographytable", "fantasticessentials.command.cartographytable", "container.cartography_table",
                (id, inv, player) -> new CartographyTableMenu(id, inv, ContainerLevelAccess.NULL));
        register(dispatcher, "craft", "fantasticessentials.command.craft", "container.crafting",
                (id, inv, player) -> new CraftingMenu(id, inv, ContainerLevelAccess.NULL));
        register(dispatcher, "enchanting", "fantasticessentials.command.enchanting", "container.enchant",
                (id, inv, player) -> new EnchantmentMenu(id, inv, ContainerLevelAccess.NULL));
        register(dispatcher, "grindstone", "fantasticessentials.command.grindstone", "container.grindstone_title",
                (id, inv, player) -> new GrindstoneMenu(id, inv, ContainerLevelAccess.NULL));
        register(dispatcher, "loom", "fantasticessentials.command.loom", "container.loom",
                (id, inv, player) -> new LoomMenu(id, inv, ContainerLevelAccess.NULL));
        register(dispatcher, "smithing", "fantasticessentials.command.smithing", "container.upgrade",
                (id, inv, player) -> new SmithingMenu(id, inv, ContainerLevelAccess.NULL));
        register(dispatcher, "stonecutter", "fantasticessentials.command.stonecutter", "container.stonecutter",
                (id, inv, player) -> new StonecutterMenu(id, inv, ContainerLevelAccess.NULL));

        // Workbench alias
        dispatcher.register(Commands.literal("workbench")
                .requires(Permissions.require("fantasticessentials.command.craft", 0))
                .executes(context -> open(context, "container.crafting",
                        (id, inv, player) -> new CraftingMenu(id, inv, ContainerLevelAccess.NULL))));

        // Ender chest opens the player's own ender chest inventory
        dispatcher.register(Commands.literal("enderchest")
                .requires(Permissions.require("fantasticessentials.command.enderchest", 0))
                .executes(MenuCommands::openEnderChest));
        dispatcher.register(Commands.literal("ec")
                .requires(Permissions.require("fantasticessentials.command.enderchest", 0))
                .executes(MenuCommands::openEnderChest));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, String name,
                                 String node, String titleKey, MenuConstructor constructor) {
        dispatcher.register(Commands.literal(name)
                .requires(Permissions.require(node, 0))
                .executes(context -> open(context, titleKey, constructor)));
    }

    private static int open(CommandContext<CommandSourceStack> context, String titleKey, MenuConstructor constructor) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MenuProvider provider = new SimpleMenuProvider(constructor, Component.translatable(titleKey));
        player.openMenu(provider);
        return 1;
    }

    private static int openEnderChest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MenuProvider provider = new SimpleMenuProvider(
                (id, inv, p) -> ChestMenu.threeRows(id, inv, player.getEnderChestInventory()),
                Component.translatable("container.enderchest"));
        player.openMenu(provider);
        return 1;
    }
}
