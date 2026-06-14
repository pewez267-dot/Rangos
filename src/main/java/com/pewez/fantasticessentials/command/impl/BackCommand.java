package com.pewez.fantasticessentials.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pewez.fantasticessentials.command.CommandHelper;
import com.pewez.fantasticessentials.config.Config;
import com.pewez.fantasticessentials.storage.DataStorage;
import com.pewez.fantasticessentials.storage.Location;
import com.pewez.fantasticessentials.storage.PlayerData;
import com.pewez.fantasticessentials.text.Messages;
import com.pewez.fantasticessentials.util.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class BackCommand {

    private BackCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("back")
                .requires(Permissions.require("fantasticessentials.command.back", 0))
                .executes(BackCommand::back));
    }

    private static int back(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerData data = DataStorage.playerData(player);
        Location location = data.lastLocation;
        if (location == null) {
            player.sendSystemMessage(Messages.prefixed("back.none",
                    "&cYou have no previous location to return to."));
            return 0;
        }
        if (CommandHelper.onCooldown(player, "back", Config.get().backCooldownSeconds)) {
            return 0;
        }
        CommandHelper.teleport(player, location, Messages.prefixed("back.teleported",
                "&aReturned to your previous location."));
        return 1;
    }
}
