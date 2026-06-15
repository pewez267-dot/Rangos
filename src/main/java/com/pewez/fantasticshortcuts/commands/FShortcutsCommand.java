package com.pewez.fantasticshortcuts.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.pewez.fantasticshortcuts.FantasticShortcutsMod;
import com.pewez.fantasticshortcuts.shortcuts.Shortcut;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import com.pewez.fantasticshortcuts.util.ChatPrefix;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * The management command: {@code /fshortcuts}. Requires operator level 4 by default.
 */
public final class FShortcutsCommand {

    private static final SuggestionProvider<CommandSourceStack> ALIAS_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(ShortcutManager.get().all().stream().map(s -> s.alias), builder);

    private FShortcutsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fshortcuts")
                .requires(source -> source.hasPermission(4))
                .executes(FShortcutsCommand::openGuiOrHelp)
                .then(Commands.literal("gui").executes(FShortcutsCommand::openGui))
                .then(Commands.literal("list").executes(FShortcutsCommand::list))
                .then(Commands.literal("reload").executes(FShortcutsCommand::reload))
                .then(Commands.literal("create")
                        .then(Commands.argument("alias", StringArgumentType.word())
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .executes(FShortcutsCommand::create))))
                .then(Commands.literal("edit")
                        .then(Commands.argument("alias", StringArgumentType.word())
                                .suggests(ALIAS_SUGGESTIONS)
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .executes(FShortcutsCommand::edit))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("alias", StringArgumentType.word())
                                .suggests(ALIAS_SUGGESTIONS)
                                .executes(FShortcutsCommand::delete)))
                .then(Commands.literal("replace")
                        .then(Commands.argument("alias", StringArgumentType.word())
                                .suggests(ALIAS_SUGGESTIONS)
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(FShortcutsCommand::replace))))
                .then(Commands.literal("info")
                        .then(Commands.argument("alias", StringArgumentType.word())
                                .suggests(ALIAS_SUGGESTIONS)
                                .executes(FShortcutsCommand::info))));
    }

    private static int openGuiOrHelp(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer) {
            return openGui(context);
        }
        return list(context);
    }

    private static int openGui(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(ChatPrefix.error("Only players can open the GUI. Use /fshortcuts list."));
            return 0;
        }
        com.pewez.fantasticshortcuts.network.FSShortcutsNetwork.sendToClient(player,
                new com.pewez.fantasticshortcuts.network.OpenEditorPacket(
                        new java.util.ArrayList<>(ShortcutManager.get().all())));
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MutableComponent message = ChatPrefix.prefix()
                .append(Component.literal("Shortcuts (" + ShortcutManager.get().all().size() + "):")
                        .withStyle(ChatFormatting.GOLD));
        for (Shortcut shortcut : ShortcutManager.get().all()) {
            message.append(Component.literal("\n")
                    .append(Component.literal("/" + shortcut.alias).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" -> ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("/" + shortcut.command).withStyle(ChatFormatting.WHITE))
                    .append(shortcut.replaceOriginal
                            ? Component.literal(" [replace]").withStyle(ChatFormatting.LIGHT_PURPLE)
                            : Component.empty()));
        }
        source.sendSuccess(() -> message, false);
        return ShortcutManager.get().all().size();
    }

    private static int create(CommandContext<CommandSourceStack> context) {
        String alias = StringArgumentType.getString(context, "alias");
        String command = StringArgumentType.getString(context, "command");
        ShortcutManager.Result result = ShortcutManager.get().create(alias, command, actor(context));
        if (result.success()) {
            liveRegister(context);
            context.getSource().sendSuccess(() -> ChatPrefix.success(result.message()), true);
            return 1;
        }
        context.getSource().sendFailure(ChatPrefix.error(result.message()));
        return 0;
    }

    private static int edit(CommandContext<CommandSourceStack> context) {
        String alias = StringArgumentType.getString(context, "alias");
        String command = StringArgumentType.getString(context, "command");
        ShortcutManager.Result result = ShortcutManager.get().edit(alias, command, actor(context));
        reportAndMaybeNote(context, result, true);
        return result.success() ? 1 : 0;
    }

    private static int delete(CommandContext<CommandSourceStack> context) {
        String alias = StringArgumentType.getString(context, "alias");
        ShortcutManager.Result result = ShortcutManager.get().delete(alias, actor(context));
        reportAndMaybeNote(context, result, true);
        return result.success() ? 1 : 0;
    }

    private static int replace(CommandContext<CommandSourceStack> context) {
        String alias = StringArgumentType.getString(context, "alias");
        boolean value = BoolArgumentType.getBool(context, "value");
        ShortcutManager.Result result = ShortcutManager.get().setReplaceOriginal(alias, value, actor(context));
        reportAndMaybeNote(context, result, true);
        return result.success() ? 1 : 0;
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        String alias = StringArgumentType.getString(context, "alias");
        Shortcut shortcut = ShortcutManager.get().get(alias);
        if (shortcut == null) {
            context.getSource().sendFailure(ChatPrefix.error("No shortcut named '" + alias + "'."));
            return 0;
        }
        MutableComponent message = ChatPrefix.prefix()
                .append(Component.literal("/" + shortcut.alias).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("\n  command: /").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(shortcut.command).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("\n  allowArguments: " + shortcut.allowArguments).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n  replaceOriginal: " + shortcut.replaceOriginal).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n  description: " + shortcut.description).withStyle(ChatFormatting.DARK_GRAY));
        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        ShortcutManager.get().reload();
        liveRegister(context);
        context.getSource().sendSuccess(() -> ChatPrefix.success(
                "Reloaded. " + ShortcutManager.get().all().size() + " shortcuts loaded."), true);
        context.getSource().sendSuccess(() -> ChatPrefix.info(
                "Renamed/removed shortcuts fully apply after a vanilla /reload or restart."), false);
        return 1;
    }

    private static void reportAndMaybeNote(CommandContext<CommandSourceStack> context,
                                           ShortcutManager.Result result, boolean addNote) {
        if (result.success()) {
            liveRegister(context);
            context.getSource().sendSuccess(() -> ChatPrefix.success(result.message()), true);
            if (addNote) {
                context.getSource().sendSuccess(() -> ChatPrefix.info(
                        "Use /fshortcuts reload (or vanilla /reload) to fully apply changes."), false);
            }
        } else {
            context.getSource().sendFailure(ChatPrefix.error(result.message()));
        }
    }

    /** Register newly added shortcuts into the live dispatcher and resync to players. */
    private static void liveRegister(CommandContext<CommandSourceStack> context) {
        FantasticShortcutsMod.liveSync(context.getSource().getServer());
    }

    private static String actor(CommandContext<CommandSourceStack> context) {
        return context.getSource().getTextName();
    }
}
