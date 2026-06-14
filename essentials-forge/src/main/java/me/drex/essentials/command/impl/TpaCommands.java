package me.drex.essentials.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.essentials.command.CommandHelper;
import me.drex.essentials.config.Config;
import me.drex.essentials.storage.Location;
import me.drex.essentials.text.Messages;
import me.drex.essentials.util.Permissions;
import me.drex.essentials.util.TpaManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

public final class TpaCommands {

    private TpaCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpa")
                .requires(Permissions.require("essentials.command.tpa", 0))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> request(context, TpaManager.Direction.TO_TARGET))));

        dispatcher.register(Commands.literal("tpahere")
                .requires(Permissions.require("essentials.command.tpa", 0))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> request(context, TpaManager.Direction.FROM_TARGET))));

        dispatcher.register(Commands.literal("tpaccept")
                .requires(Permissions.require("essentials.command.tpaccept", 0))
                .executes(context -> accept(context, null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> accept(context, EntityArgument.getPlayer(context, "player")))));

        dispatcher.register(Commands.literal("tpdeny")
                .requires(Permissions.require("essentials.command.tpdeny", 0))
                .executes(context -> deny(context, null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> deny(context, EntityArgument.getPlayer(context, "player")))));

        dispatcher.register(Commands.literal("tpall")
                .requires(Permissions.require("essentials.command.tpall", 2))
                .executes(TpaCommands::tpAll));
    }

    private static int request(CommandContext<CommandSourceStack> context, TpaManager.Direction direction) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        if (sender == target) {
            sender.sendSystemMessage(Messages.prefixed("tpa.self", "&cYou cannot send a request to yourself."));
            return 0;
        }
        if (CommandHelper.onCooldown(sender, "tpa", Config.get().tpaCooldownSeconds)) {
            return 0;
        }
        TpaManager.add(sender, target, direction);
        sender.sendSystemMessage(Messages.prefixed("tpa.sent",
                "&aTeleport request sent to &e{player}&a.",
                Messages.of("player", target.getGameProfile().getName())));

        String descKey = direction == TpaManager.Direction.TO_TARGET ? "tpa.received.to" : "tpa.received.here";
        String descDef = direction == TpaManager.Direction.TO_TARGET
                ? "&e{player}&7 wants to teleport to you. "
                : "&e{player}&7 wants you to teleport to them. ";
        MutableComponent message = Messages.prefix()
                .append(Messages.get(descKey, descDef, Messages.of("player", sender.getGameProfile().getName())))
                .append(button("[Accept]", ChatFormatting.GREEN, "/tpaccept " + sender.getGameProfile().getName()))
                .append(Component.literal(" "))
                .append(button("[Deny]", ChatFormatting.RED, "/tpdeny " + sender.getGameProfile().getName()));
        target.sendSystemMessage(message);
        return 1;
    }

    private static MutableComponent button(String text, ChatFormatting color, String command) {
        return Component.literal(text).setStyle(Style.EMPTY
                .withColor(color)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
    }

    private static int accept(CommandContext<CommandSourceStack> context, ServerPlayer fromPlayer) throws CommandSyntaxException {
        ServerPlayer target = context.getSource().getPlayerOrException();
        TpaManager.Request request = TpaManager.take(target.getUUID(), fromPlayer == null ? null : fromPlayer.getUUID());
        if (request == null) {
            target.sendSystemMessage(Messages.prefixed("tpa.norequest", "&cYou have no pending teleport requests."));
            return 0;
        }
        ServerPlayer sender = target.server.getPlayerList().getPlayer(request.sender);
        if (sender == null) {
            target.sendSystemMessage(Messages.prefixed("tpa.offline", "&cThat player is no longer online."));
            return 0;
        }
        ServerPlayer moving = request.direction == TpaManager.Direction.TO_TARGET ? sender : target;
        ServerPlayer destination = request.direction == TpaManager.Direction.TO_TARGET ? target : sender;
        Location location = Location.of(destination);
        CommandHelper.teleport(moving, location, Messages.prefixed("tpa.teleporting", "&aTeleporting..."));
        sender.sendSystemMessage(Messages.prefixed("tpa.accepted.sender",
                "&e{player}&a accepted your teleport request.",
                Messages.of("player", target.getGameProfile().getName())));
        target.sendSystemMessage(Messages.prefixed("tpa.accepted.target",
                "&aYou accepted &e{player}&a's teleport request.",
                Messages.of("player", sender.getGameProfile().getName())));
        return 1;
    }

    private static int deny(CommandContext<CommandSourceStack> context, ServerPlayer fromPlayer) throws CommandSyntaxException {
        ServerPlayer target = context.getSource().getPlayerOrException();
        TpaManager.Request request = TpaManager.take(target.getUUID(), fromPlayer == null ? null : fromPlayer.getUUID());
        if (request == null) {
            target.sendSystemMessage(Messages.prefixed("tpa.norequest", "&cYou have no pending teleport requests."));
            return 0;
        }
        target.sendSystemMessage(Messages.prefixed("tpa.denied.target", "&cTeleport request denied."));
        ServerPlayer sender = target.server.getPlayerList().getPlayer(request.sender);
        if (sender != null) {
            sender.sendSystemMessage(Messages.prefixed("tpa.denied.sender",
                    "&e{player}&c denied your teleport request.",
                    Messages.of("player", target.getGameProfile().getName())));
        }
        return 1;
    }

    private static int tpAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        int count = 0;
        for (ServerPlayer target : sender.server.getPlayerList().getPlayers()) {
            if (target == sender) {
                continue;
            }
            TpaManager.add(sender, target, TpaManager.Direction.FROM_TARGET);
            MutableComponent message = Messages.prefix()
                    .append(Messages.get("tpa.received.here", "&e{player}&7 wants you to teleport to them. ",
                            Messages.of("player", sender.getGameProfile().getName())))
                    .append(button("[Accept]", ChatFormatting.GREEN, "/tpaccept " + sender.getGameProfile().getName()))
                    .append(Component.literal(" "))
                    .append(button("[Deny]", ChatFormatting.RED, "/tpdeny " + sender.getGameProfile().getName()));
            target.sendSystemMessage(message);
            count++;
        }
        final int sent = count;
        sender.sendSystemMessage(Messages.prefixed("tpa.all.sent",
                "&aSent a teleport request to &e{count}&a players.",
                Messages.of("count", String.valueOf(sent))));
        return 1;
    }
}
