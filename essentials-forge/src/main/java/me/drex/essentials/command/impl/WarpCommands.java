package me.drex.essentials.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.drex.essentials.command.CommandHelper;
import me.drex.essentials.config.Config;
import me.drex.essentials.storage.DataStorage;
import me.drex.essentials.storage.Location;
import me.drex.essentials.text.Messages;
import me.drex.essentials.util.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public final class WarpCommands {

    private static final SuggestionProvider<CommandSourceStack> WARP_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(DataStorage.serverData().warps().keySet(), builder);

    private WarpCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setwarp")
                .requires(Permissions.require("essentials.command.setwarp", 2))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(WarpCommands::setWarp)));

        dispatcher.register(Commands.literal("delwarp")
                .requires(Permissions.require("essentials.command.delwarp", 2))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(WARP_SUGGESTIONS)
                        .executes(WarpCommands::delWarp)));

        dispatcher.register(Commands.literal("warp")
                .requires(Permissions.require("essentials.command.warp", 0))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(WARP_SUGGESTIONS)
                        .executes(WarpCommands::warp)));

        dispatcher.register(Commands.literal("warps")
                .requires(Permissions.require("essentials.command.warps", 0))
                .executes(WarpCommands::warps));
    }

    private static int setWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        DataStorage.serverData().warps().put(name, Location.of(player));
        DataStorage.saveServerData();
        player.sendSystemMessage(Messages.prefixed("warp.set",
                "&aWarp &e{name}&a set.", Messages.of("name", name)));
        return 1;
    }

    private static int delWarp(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();
        if (DataStorage.serverData().warps().remove(name) == null) {
            source.sendFailure(Messages.get("warp.unknown",
                    "&cThere is no warp called &e{name}&c.", Messages.of("name", name)));
            return 0;
        }
        DataStorage.saveServerData();
        source.sendSuccess(() -> Messages.prefixed("warp.deleted",
                "&aWarp &e{name}&a deleted.", Messages.of("name", name)), true);
        return 1;
    }

    private static int warp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        Location location = DataStorage.serverData().warps().get(name);
        if (location == null) {
            player.sendSystemMessage(Messages.prefixed("warp.unknown",
                    "&cThere is no warp called &e{name}&c.", Messages.of("name", name)));
            return 0;
        }
        if (CommandHelper.onCooldown(player, "warp", Config.get().warpCooldownSeconds)) {
            return 0;
        }
        CommandHelper.teleport(player, location, Messages.prefixed("warp.teleported",
                "&aTeleported to warp &e{name}&a.", Messages.of("name", name)));
        return 1;
    }

    private static int warps(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Map<String, Location> warps = DataStorage.serverData().warps();
        if (warps.isEmpty()) {
            source.sendSuccess(() -> Messages.prefixed("warp.none", "&cThere are no warps."), false);
            return 0;
        }
        MutableComponent message = Messages.prefix().append(
                Messages.get("warp.list.header", "&6Warps &7({count})&8: ",
                        Messages.of("count", String.valueOf(warps.size()))));
        boolean first = true;
        for (String name : warps.keySet()) {
            if (!first) {
                message.append(Component.literal("\u00a77, "));
            }
            first = false;
            message.append(Component.literal(name).setStyle(Style.EMPTY
                    .withColor(net.minecraft.ChatFormatting.YELLOW)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp " + name))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Teleport to " + name)))));
        }
        source.sendSuccess(() -> message, false);
        return 1;
    }
}
