package com.fantastickits.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

/**
 * Root command registration for /fkits.
 * Exactly 5 subcommands: create, edit, delete, get, test.
 * All registered via Brigadier CommandDispatcher.
 */
public class FKitsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("fkits")
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(CreateCommand::execute)
                                )
                        )
                        .then(Commands.literal("edit")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(EditCommand::execute)
                                )
                        )
                        .then(Commands.literal("delete")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(DeleteCommand::execute)
                                )
                        )
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("kit", StringArgumentType.word())
                                                .executes(GetCommand::execute)
                                        )
                                )
                        )
                        .then(Commands.literal("test")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(TestCommand::execute)
                                )
                        )
        );
    }
}
