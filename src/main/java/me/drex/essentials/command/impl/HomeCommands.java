package me.drex.essentials.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.drex.essentials.command.CommandHelper;
import me.drex.essentials.config.Config;
import me.drex.essentials.storage.DataStorage;
import me.drex.essentials.storage.Location;
import me.drex.essentials.storage.PlayerData;
import me.drex.essentials.text.Messages;
import me.drex.essentials.util.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public final class HomeCommands {

    private static final SuggestionProvider<CommandSourceStack> HOME_SUGGESTIONS = (context, builder) -> {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            PlayerData data = DataStorage.playerData(player);
            return SharedSuggestionProvider.suggest(data.homes().keySet(), builder);
        } catch (Exception e) {
            return builder.buildFuture();
        }
    };

    private HomeCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /sethome <name>
        dispatcher.register(Commands.literal("sethome")
                .requires(Permissions.require("essentials.command.sethome", 0))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(HomeCommands::setHome))
                .executes(context -> setHome(context, "home")));

        // /delhome <name>
        dispatcher.register(Commands.literal("delhome")
                .requires(Permissions.require("essentials.command.delhome", 0))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(HOME_SUGGESTIONS)
                        .executes(HomeCommands::delHome)));

        // /home [name]
        dispatcher.register(Commands.literal("home")
                .requires(Permissions.require("essentials.command.home", 0))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(HOME_SUGGESTIONS)
                        .executes(HomeCommands::home))
                .executes(context -> home(context, "home")));

        // /homes
        dispatcher.register(Commands.literal("homes")
                .requires(Permissions.require("essentials.command.homes", 0))
                .executes(HomeCommands::homes));
    }

    private static int setHome(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return setHome(context, StringArgumentType.getString(context, "name"));
    }

    private static int setHome(CommandContext<CommandSourceStack> context, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerData data = DataStorage.playerData(player);
        boolean replacing = data.homes().containsKey(name);
        if (!replacing && data.homes().size() >= CommandHelper.homeLimit(player)) {
            player.sendSystemMessage(Messages.prefixed("home.limit",
                    "&cYou have reached your home limit of &e{limit}&c.",
                    Messages.of("limit", String.valueOf(Config.get().defaultHomeLimit))));
            return 0;
        }
        data.homes().put(name, Location.of(player));
        DataStorage.savePlayerData(player.getUUID());
        player.sendSystemMessage(Messages.prefixed("home.set",
                "&aHome &e{name}&a set.", Messages.of("name", name)));
        return 1;
    }

    private static int delHome(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        PlayerData data = DataStorage.playerData(player);
        if (data.homes().remove(name) == null) {
            player.sendSystemMessage(Messages.prefixed("home.unknown",
                    "&cYou don't have a home called &e{name}&c.", Messages.of("name", name)));
            return 0;
        }
        DataStorage.savePlayerData(player.getUUID());
        player.sendSystemMessage(Messages.prefixed("home.deleted",
                "&aHome &e{name}&a deleted.", Messages.of("name", name)));
        return 1;
    }

    private static int home(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return home(context, StringArgumentType.getString(context, "name"));
    }

    private static int home(CommandContext<CommandSourceStack> context, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerData data = DataStorage.playerData(player);
        Location location = data.homes().get(name);
        if (location == null) {
            if (data.homes().isEmpty()) {
                player.sendSystemMessage(Messages.prefixed("home.none",
                        "&cYou don't have any homes. Use &e/sethome&c first."));
            } else {
                player.sendSystemMessage(Messages.prefixed("home.unknown",
                        "&cYou don't have a home called &e{name}&c.", Messages.of("name", name)));
            }
            return 0;
        }
        if (CommandHelper.onCooldown(player, "home", Config.get().homeCooldownSeconds)) {
            return 0;
        }
        CommandHelper.teleport(player, location, Messages.prefixed("home.teleported",
                "&aTeleported to home &e{name}&a.", Messages.of("name", name)));
        return 1;
    }

    private static int homes(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerData data = DataStorage.playerData(player);
        if (data.homes().isEmpty()) {
            player.sendSystemMessage(Messages.prefixed("home.none",
                    "&cYou don't have any homes. Use &e/sethome&c first."));
            return 0;
        }
        MutableComponent message = Messages.prefix().append(
                Messages.get("home.list.header", "&6Homes &7({count}/{limit})&8: ",
                        Messages.of("count", String.valueOf(data.homes().size()),
                                "limit", limitDisplay(player))));
        boolean first = true;
        for (Map.Entry<String, Location> entry : data.homes().entrySet()) {
            if (!first) {
                message.append(Component.literal("\u00a77, "));
            }
            first = false;
            String name = entry.getKey();
            MutableComponent button = Component.literal(name).setStyle(Style.EMPTY
                    .withColor(net.minecraft.ChatFormatting.YELLOW)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home " + name))
                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                            net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Teleport to " + name))));
            message.append(button);
        }
        player.sendSystemMessage(message);
        return 1;
    }

    private static String limitDisplay(ServerPlayer player) {
        int limit = CommandHelper.homeLimit(player);
        return limit == Integer.MAX_VALUE ? "\u221e" : String.valueOf(limit);
    }
}
