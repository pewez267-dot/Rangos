package me.drex.essentials.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.essentials.EssentialsMod;
import me.drex.essentials.text.Messages;
import me.drex.essentials.util.CommandSpy;
import me.drex.essentials.util.MessageState;
import me.drex.essentials.util.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

public final class AdminCommands {

    private AdminCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /broadcast <message>
        dispatcher.register(Commands.literal("broadcast")
                .requires(Permissions.require("essentials.command.broadcast", 2))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(AdminCommands::broadcast)));
        dispatcher.register(Commands.literal("bc")
                .requires(Permissions.require("essentials.command.broadcast", 2))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(AdminCommands::broadcast)));

        // /commandspy [on|off]
        dispatcher.register(Commands.literal("commandspy")
                .requires(Permissions.require("essentials.command.commandspy", 3))
                .executes(AdminCommands::commandSpyToggle)
                .then(Commands.literal("on").executes(context -> commandSpySet(context, true)))
                .then(Commands.literal("off").executes(context -> commandSpySet(context, false))));

        // /essentials [reload|version]
        dispatcher.register(Commands.literal("essentials")
                .requires(Permissions.require("essentials.command.essentials", 4))
                .executes(AdminCommands::version)
                .then(Commands.literal("reload").executes(AdminCommands::reload))
                .then(Commands.literal("version").executes(AdminCommands::version)));

        // /mods
        dispatcher.register(Commands.literal("mods")
                .requires(Permissions.require("essentials.command.mods", 2))
                .executes(AdminCommands::mods));

        // /msg <player> <message> + aliases
        for (String alias : new String[]{"msg", "tell", "w", "whisper"}) {
            dispatcher.register(Commands.literal(alias)
                    .requires(Permissions.require("essentials.command.msg", 0))
                    .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("message", StringArgumentType.greedyString())
                                    .executes(AdminCommands::msg))));
        }

        // /reply <message> + alias /r
        for (String alias : new String[]{"reply", "r"}) {
            dispatcher.register(Commands.literal(alias)
                    .requires(Permissions.require("essentials.command.reply", 0))
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(AdminCommands::reply)));
        }
    }

    private static int broadcast(CommandContext<CommandSourceStack> context) {
        String message = StringArgumentType.getString(context, "message");
        Component component = Messages.prefixed("broadcast.format", "&6[Broadcast] &r{message}",
                Messages.of("message", message));
        context.getSource().getServer().getPlayerList().broadcastSystemMessage(component, false);
        return 1;
    }

    private static int commandSpyToggle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean enabled = CommandSpy.toggle(player.getUUID());
        player.sendSystemMessage(Messages.prefixed("commandspy.toggle",
                "&aCommandSpy {state}.", Messages.of("state", enabled ? "enabled" : "disabled")));
        return 1;
    }

    private static int commandSpySet(CommandContext<CommandSourceStack> context, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CommandSpy.set(player.getUUID(), enabled);
        player.sendSystemMessage(Messages.prefixed("commandspy.toggle",
                "&aCommandSpy {state}.", Messages.of("state", enabled ? "enabled" : "disabled")));
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        EssentialsMod.reload();
        context.getSource().sendSuccess(() -> Messages.prefixed("essentials.reloaded",
                "&aEssentials configuration reloaded."), true);
        return 1;
    }

    private static int version(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Messages.prefix()
                .append(Component.literal("Essentials (Forge port) - based on Fabric Essentials by Drex (MIT)")
                        .withStyle(ChatFormatting.GRAY)), false);
        return 1;
    }

    private static int mods(CommandContext<CommandSourceStack> context) {
        var modList = ModList.get().getMods();
        MutableComponent message = Messages.prefix()
                .append(Component.literal("Loaded mods (" + modList.size() + "):").withStyle(ChatFormatting.GOLD));
        for (var mod : modList) {
            message.append(Component.literal("\n")
                    .append(Component.literal(mod.getDisplayName()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" " + mod.getVersion()).withStyle(ChatFormatting.GRAY)));
        }
        context.getSource().sendSuccess(() -> message, false);
        return modList.size();
    }

    private static int msg(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        String message = StringArgumentType.getString(context, "message");
        String senderName = source.getTextName();
        deliver(source, target, senderName, message);
        if (source.getEntity() instanceof ServerPlayer sender) {
            MessageState.setLastContact(sender.getUUID(), target.getUUID());
            MessageState.setLastContact(target.getUUID(), sender.getUUID());
        }
        return 1;
    }

    private static int reply(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        String message = StringArgumentType.getString(context, "message");
        java.util.UUID targetUuid = MessageState.getLastContact(sender.getUUID());
        if (targetUuid == null) {
            sender.sendSystemMessage(Messages.prefixed("reply.none", "&cYou have nobody to reply to."));
            return 0;
        }
        ServerPlayer target = sender.server.getPlayerList().getPlayer(targetUuid);
        if (target == null) {
            sender.sendSystemMessage(Messages.prefixed("reply.offline", "&cThat player is no longer online."));
            return 0;
        }
        deliver(context.getSource(), target, sender.getGameProfile().getName(), message);
        MessageState.setLastContact(target.getUUID(), sender.getUUID());
        return 1;
    }

    private static void deliver(CommandSourceStack source, ServerPlayer target, String senderName, String message) {
        Component toTarget = Messages.get("msg.in", "&7[&e{sender}&7 -> me] &f{message}",
                Messages.of("sender", senderName, "message", message));
        Component toSender = Messages.get("msg.out", "&7[me -> &e{target}&7] &f{message}",
                Messages.of("target", target.getGameProfile().getName(), "message", message));
        target.sendSystemMessage(toTarget);
        source.sendSuccess(() -> toSender, false);
    }
}
