package me.drex.essentials.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.essentials.command.CommandHelper;
import me.drex.essentials.config.Config;
import me.drex.essentials.storage.DataStorage;
import me.drex.essentials.storage.Location;
import me.drex.essentials.storage.PlayerData;
import me.drex.essentials.text.Messages;
import me.drex.essentials.util.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class BackCommand {

    private BackCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("back")
                .requires(Permissions.require("essentials.command.back", 0))
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
