package com.pewez.fantasticessentials.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pewez.fantasticessentials.text.Messages;
import com.pewez.fantasticessentials.util.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ItemEditCommand {

    private static final String DISPLAY = "display";
    private static final String LORE = "Lore";

    private ItemEditCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("itemedit")
                .requires(Permissions.require("fantasticessentials.command.itemedit", 2))
                .then(Commands.literal("name")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ItemEditCommand::setName))
                        .then(Commands.literal("clear").executes(ItemEditCommand::clearName)))
                .then(Commands.literal("lore")
                        .then(Commands.literal("add")
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(ItemEditCommand::loreAdd)))
                        .then(Commands.literal("set")
                                .then(Commands.argument("line", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(ItemEditCommand::loreSet))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("line", IntegerArgumentType.integer(1))
                                        .executes(ItemEditCommand::loreRemove)))
                        .then(Commands.literal("clear").executes(ItemEditCommand::loreClear))));
    }

    private static ItemStack held(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(
                    Messages.get("itemedit.empty", "&cYou must hold an item.")).create();
        }
        return stack;
    }

    private static int setName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = held(context);
        String text = StringArgumentType.getString(context, "text");
        stack.setHoverName(Messages.LegacyText.parse(text));
        context.getSource().getPlayerOrException().sendSystemMessage(
                Messages.prefixed("itemedit.name.set", "&aItem name updated."));
        return 1;
    }

    private static int clearName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = held(context);
        stack.resetHoverName();
        context.getSource().getPlayerOrException().sendSystemMessage(
                Messages.prefixed("itemedit.name.clear", "&aItem name cleared."));
        return 1;
    }

    private static ListTag loreTag(ItemStack stack, boolean create) {
        CompoundTag display = create ? stack.getOrCreateTagElement(DISPLAY) : stack.getTagElement(DISPLAY);
        if (display == null) {
            return null;
        }
        if (!display.contains(LORE, Tag.TAG_LIST)) {
            if (!create) {
                return null;
            }
            display.put(LORE, new ListTag());
        }
        return display.getList(LORE, Tag.TAG_STRING);
    }

    private static int loreAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = held(context);
        String text = StringArgumentType.getString(context, "text");
        ListTag lore = loreTag(stack, true);
        lore.add(StringTag.valueOf(toJson(text)));
        context.getSource().getPlayerOrException().sendSystemMessage(
                Messages.prefixed("itemedit.lore.add", "&aLore line added."));
        return 1;
    }

    private static int loreSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = held(context);
        int line = IntegerArgumentType.getInteger(context, "line");
        String text = StringArgumentType.getString(context, "text");
        ListTag lore = loreTag(stack, true);
        while (lore.size() < line) {
            lore.add(StringTag.valueOf(toJson("")));
        }
        lore.set(line - 1, StringTag.valueOf(toJson(text)));
        context.getSource().getPlayerOrException().sendSystemMessage(
                Messages.prefixed("itemedit.lore.set", "&aLore line {line} set.",
                        Messages.of("line", String.valueOf(line))));
        return 1;
    }

    private static int loreRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = held(context);
        int line = IntegerArgumentType.getInteger(context, "line");
        ListTag lore = loreTag(stack, false);
        if (lore == null || line > lore.size()) {
            context.getSource().getPlayerOrException().sendSystemMessage(
                    Messages.prefixed("itemedit.lore.none", "&cThat lore line does not exist."));
            return 0;
        }
        lore.remove(line - 1);
        context.getSource().getPlayerOrException().sendSystemMessage(
                Messages.prefixed("itemedit.lore.remove", "&aLore line {line} removed.",
                        Messages.of("line", String.valueOf(line))));
        return 1;
    }

    private static int loreClear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = held(context);
        CompoundTag display = stack.getTagElement(DISPLAY);
        if (display != null) {
            display.remove(LORE);
        }
        context.getSource().getPlayerOrException().sendSystemMessage(
                Messages.prefixed("itemedit.lore.clear", "&aLore cleared."));
        return 1;
    }

    private static String toJson(String legacy) {
        Component component = Messages.LegacyText.parse(legacy);
        return Component.Serializer.toJson(component);
    }
}
