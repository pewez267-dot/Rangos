package com.pewez.fantasticessentials.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pewez.fantasticessentials.text.Messages;
import com.pewez.fantasticessentials.util.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;

public final class MiscCommands {

    private MiscCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /feed [player]
        dispatcher.register(Commands.literal("feed")
                .requires(Permissions.require("fantasticessentials.command.feed", 2))
                .executes(context -> feed(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(Permissions.require("fantasticessentials.command.feed.others", 2))
                        .executes(context -> feed(context, EntityArgument.getPlayer(context, "player")))));

        // /heal [player]
        dispatcher.register(Commands.literal("heal")
                .requires(Permissions.require("fantasticessentials.command.heal", 2))
                .executes(context -> heal(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(Permissions.require("fantasticessentials.command.heal.others", 2))
                        .executes(context -> heal(context, EntityArgument.getPlayer(context, "player")))));

        // /fly [player]
        dispatcher.register(Commands.literal("fly")
                .requires(Permissions.require("fantasticessentials.command.fly", 2))
                .executes(context -> fly(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(Permissions.require("fantasticessentials.command.fly.others", 2))
                        .executes(context -> fly(context, EntityArgument.getPlayer(context, "player")))));

        // /flyspeed <speed> [player]
        dispatcher.register(Commands.literal("flyspeed")
                .requires(Permissions.require("fantasticessentials.command.flyspeed", 2))
                .then(Commands.argument("speed", FloatArgumentType.floatArg(0.0f, 10.0f))
                        .executes(context -> flySpeed(context, context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> flySpeed(context, EntityArgument.getPlayer(context, "player"))))));

        // /walkspeed <speed> [player]
        dispatcher.register(Commands.literal("walkspeed")
                .requires(Permissions.require("fantasticessentials.command.walkspeed", 2))
                .then(Commands.argument("speed", FloatArgumentType.floatArg(0.0f, 10.0f))
                        .executes(context -> walkSpeed(context, context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> walkSpeed(context, EntityArgument.getPlayer(context, "player"))))));

        // /glow [player]
        dispatcher.register(Commands.literal("glow")
                .requires(Permissions.require("fantasticessentials.command.glow", 2))
                .executes(context -> glow(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> glow(context, EntityArgument.getPlayer(context, "player")))));

        // /invulnerable [player]
        dispatcher.register(Commands.literal("invulnerable")
                .requires(Permissions.require("fantasticessentials.command.invulnerable", 2))
                .executes(context -> invulnerable(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> invulnerable(context, EntityArgument.getPlayer(context, "player")))));

        // /hat
        dispatcher.register(Commands.literal("hat")
                .requires(Permissions.require("fantasticessentials.command.hat", 2))
                .executes(MiscCommands::hat));

        // /repair
        dispatcher.register(Commands.literal("repair")
                .requires(Permissions.require("fantasticessentials.command.repair", 2))
                .executes(MiscCommands::repair));

        // /ping [player]
        dispatcher.register(Commands.literal("ping")
                .requires(Permissions.require("fantasticessentials.command.ping", 0))
                .executes(context -> ping(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> ping(context, EntityArgument.getPlayer(context, "player")))));

        // /whois [player]
        dispatcher.register(Commands.literal("whois")
                .requires(Permissions.require("fantasticessentials.command.whois", 0))
                .executes(context -> whois(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> whois(context, EntityArgument.getPlayer(context, "player")))));
    }

    private static int feed(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0f);
        player.getFoodData().setExhaustion(0.0f);
        player.sendSystemMessage(Messages.prefixed("feed.self", "&aYour hunger has been satisfied."));
        notifyOther(context, player, "feed.other", "&aFed &e{player}&a.");
        return 1;
    }

    private static int heal(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0f);
        player.removeAllEffects();
        player.clearFire();
        player.sendSystemMessage(Messages.prefixed("heal.self", "&aYou have been healed."));
        notifyOther(context, player, "heal.other", "&aHealed &e{player}&a.");
        return 1;
    }

    private static int fly(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        Abilities abilities = player.getAbilities();
        abilities.mayfly = !abilities.mayfly;
        if (!abilities.mayfly) {
            abilities.flying = false;
        }
        player.onUpdateAbilities();
        String state = abilities.mayfly ? "enabled" : "disabled";
        player.sendSystemMessage(Messages.prefixed("fly.self",
                "&aFlight {state}.", Messages.of("state", state)));
        notifyOther(context, player, "fly.other", "&aFlight {state} for &e{player}&a.",
                Messages.of("state", state, "player", player.getGameProfile().getName()));
        return 1;
    }

    private static int flySpeed(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        float speed = FloatArgumentType.getFloat(context, "speed");
        player.getAbilities().setFlyingSpeed(0.05f * speed);
        player.onUpdateAbilities();
        player.sendSystemMessage(Messages.prefixed("flyspeed.set",
                "&aFlight speed set to &e{speed}&a.", Messages.of("speed", String.valueOf(speed))));
        return 1;
    }

    private static int walkSpeed(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        float speed = FloatArgumentType.getFloat(context, "speed");
        player.getAbilities().setWalkingSpeed(0.1f * speed);
        player.onUpdateAbilities();
        player.sendSystemMessage(Messages.prefixed("walkspeed.set",
                "&aWalk speed set to &e{speed}&a.", Messages.of("speed", String.valueOf(speed))));
        return 1;
    }

    private static int glow(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        boolean enabled;
        if (player.hasEffect(MobEffects.GLOWING)) {
            player.removeEffect(MobEffects.GLOWING);
            enabled = false;
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, 0, false, false, false));
            enabled = true;
        }
        String state = enabled ? "enabled" : "disabled";
        player.sendSystemMessage(Messages.prefixed("glow.self", "&aGlowing {state}.", Messages.of("state", state)));
        notifyOther(context, player, "glow.other", "&aGlowing {state} for &e{player}&a.",
                Messages.of("state", state, "player", player.getGameProfile().getName()));
        return 1;
    }

    private static int invulnerable(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        Abilities abilities = player.getAbilities();
        abilities.invulnerable = !abilities.invulnerable;
        player.setInvulnerable(abilities.invulnerable);
        player.onUpdateAbilities();
        String state = abilities.invulnerable ? "enabled" : "disabled";
        player.sendSystemMessage(Messages.prefixed("invulnerable.self",
                "&aInvulnerability {state}.", Messages.of("state", state)));
        notifyOther(context, player, "invulnerable.other", "&aInvulnerability {state} for &e{player}&a.",
                Messages.of("state", state, "player", player.getGameProfile().getName()));
        return 1;
    }

    private static int hat(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack hand = player.getMainHandItem();
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (hand.isEmpty()) {
            player.sendSystemMessage(Messages.prefixed("hat.empty", "&cYou must hold an item to wear it as a hat."));
            return 0;
        }
        player.setItemSlot(EquipmentSlot.HEAD, hand.copy());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, head);
        player.sendSystemMessage(Messages.prefixed("hat.set", "&aEnjoy your new hat!"));
        return 1;
    }

    private static int repair(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            player.sendSystemMessage(Messages.prefixed("repair.fail", "&cThat item cannot be repaired."));
            return 0;
        }
        stack.setDamageValue(0);
        player.sendSystemMessage(Messages.prefixed("repair.success", "&aItem repaired."));
        return 1;
    }

    private static int ping(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        int latency = player.latency;
        context.getSource().sendSuccess(() -> Messages.prefixed("ping",
                "&e{player}&7's ping is &a{ping}ms&7.",
                Messages.of("player", player.getGameProfile().getName(), "ping", String.valueOf(latency))), false);
        return latency;
    }

    private static int whois(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CommandSourceStack source = context.getSource();
        MutableComponent message = Messages.prefix()
                .append(Component.literal("WhoIs " + player.getGameProfile().getName()).withStyle(ChatFormatting.GOLD));
        message.append(line("UUID", player.getStringUUID()));
        message.append(line("Game mode", player.gameMode.getGameModeForPlayer().getName()));
        message.append(line("Health", String.format("%.1f / %.1f", player.getHealth(), player.getMaxHealth())));
        message.append(line("Food", String.valueOf(player.getFoodData().getFoodLevel())));
        message.append(line("Level", String.valueOf(player.experienceLevel)));
        message.append(line("Ping", player.latency + "ms"));
        message.append(line("Dimension", player.level().dimension().location().toString()));
        message.append(line("Position", String.format("%.1f, %.1f, %.1f",
                player.getX(), player.getY(), player.getZ())));
        message.append(line("Op level", String.valueOf(opLevel(player))));
        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static int opLevel(ServerPlayer player) {
        for (int i = 4; i >= 1; i--) {
            if (player.hasPermissions(i)) {
                return i;
            }
        }
        return 0;
    }

    private static MutableComponent line(String key, String value) {
        return Component.literal("\n")
                .append(Component.literal(key + ": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static void notifyOther(CommandContext<CommandSourceStack> context, ServerPlayer target,
                                    String key, String def) {
        notifyOther(context, target, key, def, Messages.of("player", target.getGameProfile().getName()));
    }

    private static void notifyOther(CommandContext<CommandSourceStack> context, ServerPlayer target,
                                    String key, String def, java.util.Map<String, String> placeholders) {
        try {
            ServerPlayer executor = context.getSource().getPlayerOrException();
            if (executor != target) {
                executor.sendSystemMessage(Messages.prefixed(key, def, placeholders));
            }
        } catch (CommandSyntaxException ignored) {
            context.getSource().sendSuccess(() -> Messages.prefixed(key, def, placeholders), true);
        }
    }
}
