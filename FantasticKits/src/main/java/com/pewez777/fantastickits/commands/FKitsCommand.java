/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.commands;

import java.util.List;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.pewez777.fantastickits.Reference;
import com.pewez777.fantastickits.commandsystem.CommandDiscovery;
import com.pewez777.fantastickits.kits.Kit;
import com.pewez777.fantastickits.kits.KitManager;
import com.pewez777.fantastickits.kits.KitService;
import com.pewez777.fantastickits.luckperms.GroupInfo;
import com.pewez777.fantastickits.luckperms.LuckPermsHook;
import com.pewez777.fantastickits.network.NetworkHandler;
import com.pewez777.fantastickits.network.packets.OpenDeleteConfirmPacket;
import com.pewez777.fantastickits.network.packets.OpenEditorPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers the single root command {@code /fkits} with EXACTLY five
 * subcommands and no aliases or hidden/extra commands:
 *
 * <ul>
 *   <li>{@code /fkits create <name>} - operators; opens the editor for a new kit</li>
 *   <li>{@code /fkits edit <name>}   - operators; opens the editor for an existing kit</li>
 *   <li>{@code /fkits delete <name>} - operators; opens the deletion confirmation</li>
 *   <li>{@code /fkits get <name>}    - players; claims the kit (single permanent claim)</li>
 *   <li>{@code /fkits test <name>}   - operators; temporary delivery, no claim recorded</li>
 * </ul>
 */
public final class FKitsCommand {

    private static final SuggestionProvider<CommandSourceStack> KIT_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(KitManager.get().getAllNames(), builder);

    private FKitsCommand() {
    }

    private static boolean isOperator(CommandSourceStack source) {
        return source.hasPermission(Reference.ADMIN_PERMISSION_LEVEL);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(Reference.COMMAND_ROOT)
                // Root executor: typing just "/fkits" shows the usage help.
                .executes(FKitsCommand::runHelp)
                .then(Commands.literal("create")
                        .requires(FKitsCommand::isOperator)
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(FKitsCommand::runCreate)))
                .then(Commands.literal("edit")
                        .requires(FKitsCommand::isOperator)
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(KIT_SUGGESTIONS)
                                .executes(FKitsCommand::runEdit)))
                .then(Commands.literal("delete")
                        .requires(FKitsCommand::isOperator)
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(KIT_SUGGESTIONS)
                                .executes(FKitsCommand::runDelete)))
                .then(Commands.literal("get")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(KIT_SUGGESTIONS)
                                .executes(FKitsCommand::runGet)))
                .then(Commands.literal("test")
                        .requires(FKitsCommand::isOperator)
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(KIT_SUGGESTIONS)
                                .executes(FKitsCommand::runTest))));
    }

    // ---- Subcommand implementations ----------------------------------------

    private static int runHelp(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("").append(
                Component.literal("=== Fantastic Kits v" + Reference.VERSION + " ===")
                        .withStyle(ChatFormatting.GOLD)));
        ctx.getSource().sendSystemMessage(Component.literal(
                Reference.CHAT_PREFIX + "/fkits create <name>").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" - Create a new kit").withStyle(ChatFormatting.GRAY)));
        ctx.getSource().sendSystemMessage(Component.literal(
                Reference.CHAT_PREFIX + "/fkits edit <name>").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" - Edit an existing kit").withStyle(ChatFormatting.GRAY)));
        ctx.getSource().sendSystemMessage(Component.literal(
                Reference.CHAT_PREFIX + "/fkits delete <name>").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" - Delete a kit").withStyle(ChatFormatting.GRAY)));
        ctx.getSource().sendSystemMessage(Component.literal(
                Reference.CHAT_PREFIX + "/fkits get <name>").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" - Claim a kit").withStyle(ChatFormatting.GRAY)));
        ctx.getSource().sendSystemMessage(Component.literal(
                Reference.CHAT_PREFIX + "/fkits test <name>").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Test a kit (admin, no claim)").withStyle(ChatFormatting.GRAY)));
        return 1;
    }

    private static int runCreate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name").trim();

        if (name.isEmpty()) {
            ctx.getSource().sendFailure(prefixed("You must specify a kit name."));
            return 0;
        }

        if (KitManager.get().exists(name)) {
            ctx.getSource().sendFailure(prefixed("A kit named '" + name + "' already exists."));
            return 0;
        }

        Kit kit = new Kit();
        kit.setName(name);
        openEditor(player, kit, false);
        ctx.getSource().sendSuccess(() -> prefixed("Opening the kit creator...", ChatFormatting.AQUA), false);
        return 1;
    }

    private static int runEdit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name").trim();

        if (name.isEmpty()) {
            ctx.getSource().sendFailure(prefixed("You must specify a kit name."));
            return 0;
        }

        Optional<Kit> kit = KitManager.get().getByName(name);
        if (kit.isEmpty()) {
            ctx.getSource().sendFailure(prefixed("Kit '" + name + "' does not exist."));
            return 0;
        }
        openEditor(player, kit.get(), true);
        ctx.getSource().sendSuccess(() -> prefixed("Opening the kit editor...", ChatFormatting.AQUA), false);
        return 1;
    }

    private static int runDelete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name").trim();

        if (name.isEmpty()) {
            ctx.getSource().sendFailure(prefixed("You must specify a kit name."));
            return 0;
        }

        Optional<Kit> kit = KitManager.get().getByName(name);
        if (kit.isEmpty()) {
            ctx.getSource().sendFailure(prefixed("Kit '" + name + "' does not exist."));
            return 0;
        }
        NetworkHandler.sendToPlayer(
                new OpenDeleteConfirmPacket(kit.get().getName(), kit.get().getOwnerGroup()), player);
        return 1;
    }

    private static int runGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name").trim();

        if (name.isEmpty()) {
            ctx.getSource().sendFailure(prefixed("You must specify a kit name."));
            return 0;
        }

        Optional<Kit> kit = KitManager.get().getByName(name);
        if (kit.isEmpty()) {
            ctx.getSource().sendFailure(prefixed("Kit '" + name + "' does not exist."));
            return 0;
        }

        KitService.ClaimResult result = KitService.claim(player, kit.get());
        switch (result) {
            case SUCCESS -> ctx.getSource().sendSuccess(
                    () -> prefixed("You have claimed the kit '" + kit.get().getName() + "'.",
                            ChatFormatting.GREEN), false);
            case ALREADY_CLAIMED -> ctx.getSource().sendFailure(
                    prefixed("You have already claimed this kit before."));
            case WRONG_GROUP -> ctx.getSource().sendFailure(
                    prefixed("This kit is reserved for the rank '" + kit.get().getOwnerGroup() + "'."));
            case NO_LUCKPERMS -> ctx.getSource().sendFailure(
                    prefixed("Rank verification is unavailable right now. Try again later."));
            case COOLDOWN -> ctx.getSource().sendFailure(
                    prefixed("Please wait a moment before trying again."));
            default -> ctx.getSource().sendFailure(
                    prefixed("The kit could not be delivered."));
        }
        return result == KitService.ClaimResult.SUCCESS ? 1 : 0;
    }

    private static int runTest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name").trim();

        if (name.isEmpty()) {
            ctx.getSource().sendFailure(prefixed("You must specify a kit name."));
            return 0;
        }

        Optional<Kit> kit = KitManager.get().getByName(name);
        if (kit.isEmpty()) {
            ctx.getSource().sendFailure(prefixed("Kit '" + name + "' does not exist."));
            return 0;
        }
        KitService.test(player, kit.get());
        ctx.getSource().sendSuccess(
                () -> prefixed("Test delivery of '" + kit.get().getName()
                        + "' completed (no claim recorded).", ChatFormatting.GREEN), false);
        return 1;
    }

    // ---- Helpers -----------------------------------------------------------

    private static void openEditor(ServerPlayer player, Kit kit, boolean editMode) {
        List<String> groups = LuckPermsHook.getAllGroups().stream()
                .map(GroupInfo::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<String> commands = CommandDiscovery.discoverRootCommands(player.getServer());
        OpenEditorPacket packet = new OpenEditorPacket(
                editMode, LuckPermsHook.isAvailable(), kit.toNbt(), groups, commands);
        NetworkHandler.sendToPlayer(packet, player);
    }

    private static Component prefixed(String text) {
        return prefixed(text, ChatFormatting.RED);
    }

    private static Component prefixed(String text, ChatFormatting color) {
        return Component.literal(Reference.CHAT_PREFIX + text).withStyle(color);
    }
}
