package com.arthurleray.invhistory.command;

import com.arthurleray.invhistory.InvHistory;
import com.arthurleray.invhistory.data.InventorySnapshot;
import com.arthurleray.invhistory.gui.SnapshotViewerGui;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/** {@code /invhistory see <player>} - opens the snapshot viewer GUI for an admin. */
public class InvHistoryCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("invhistory")
                .requires(source -> source.hasPermission(InvHistory.config().getPermissionLevel()))
                .then(Commands.literal("see")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(InvHistoryCommand::executeSee))));
    }

    private static int executeSee(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "player");
        if (profiles.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Player not found."));
            return 0;
        }
        GameProfile target = profiles.iterator().next();
        List<InventorySnapshot> snapshots = InvHistory.storage().loadSnapshots(target.getId());
        if (snapshots.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No snapshots found for " + target.getName() + "."));
            return 0;
        }
        SnapshotViewerGui.open(admin, target, snapshots, snapshots.size() - 1);
        return 1;
    }
}
