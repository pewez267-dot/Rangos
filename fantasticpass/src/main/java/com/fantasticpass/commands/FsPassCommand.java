package com.fantasticpass.commands;

import com.fantasticpass.capability.PassCapability;
import com.fantasticpass.config.PassConfig;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PassSavedData;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.network.NametagSync;
import com.fantasticpass.network.OpenAdminScreenPacket;
import com.fantasticpass.network.OpenViewScreenPacket;
import com.fantasticpass.network.PacketHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The {@code /fspass} command tree, registered through Forge's command dispatcher.
 * Admin subcommands require permission level 4; {@code view} and {@code rango} are open
 * to everyone.
 */
public final class FsPassCommand {

    private FsPassCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fspass")
                .then(Commands.literal("create")
                        .requires(source -> source.hasPermission(4))
                        .executes(FsPassCommand::create))
                .then(Commands.literal("edit")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(FsPassCommand::edit)))
                .then(Commands.literal("delete")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(FsPassCommand::delete)))
                .then(Commands.literal("setpremium")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(FsPassCommand::setPremium)))
                .then(Commands.literal("activate")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(FsPassCommand::activate)))
                .then(Commands.literal("view")
                        .executes(FsPassCommand::view))
                .then(Commands.literal("rango")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(FsPassCommand::rango))));
    }

    private static int create(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PassDefinition fresh = new PassDefinition("", "New Pass");
        PacketHandler.sendToPlayer(player, new OpenAdminScreenPacket(fresh));
        return 1;
    }

    private static int edit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "id");
        PassSavedData saved = PassSavedData.get(player.getServer());
        PassDefinition pass = saved.getPass(id);
        if (pass == null) {
            ctx.getSource().sendFailure(Component.translatable("fantasticpass.msg.pass_not_found", id));
            return 0;
        }
        PacketHandler.sendToPlayer(player, new OpenAdminScreenPacket(pass.copy()));
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        String id = StringArgumentType.getString(ctx, "id");
        PassSavedData saved = PassSavedData.get(server);
        if (saved.deletePass(id)) {
            ctx.getSource().sendSuccess(() -> Component.translatable("fantasticpass.msg.pass_deleted", id), true);
            return 1;
        }
        ctx.getSource().sendFailure(Component.translatable("fantasticpass.msg.pass_not_found", id));
        return 0;
    }

    private static int setPremium(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        PlayerPassData data = PassCapability.getData(target);
        if (data == null) {
            return 0;
        }
        data.setPremium(true);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("fantasticpass.msg.premium_set", target.getGameProfile().getName()), true);
        return 1;
    }

    private static int activate(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        String id = StringArgumentType.getString(ctx, "id");
        PassSavedData saved = PassSavedData.get(server);
        if (!saved.hasPass(id)) {
            ctx.getSource().sendFailure(Component.translatable("fantasticpass.msg.pass_not_found", id));
            return 0;
        }
        saved.setActivePassId(id);

        // Reset seasonal progress for everyone online (offline players reset on next login).
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerPassData data = PassCapability.getData(player);
            if (data != null) {
                data.resetForNewSeason(id);
                NametagSync.syncPlayer(player);
            }
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("fantasticpass.msg.pass_activated", id), true);
        return 1;
    }

    private static int view(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PassSavedData saved = PassSavedData.get(player.getServer());
        PassDefinition pass = saved.getActivePass();
        if (pass == null) {
            ctx.getSource().sendFailure(Component.translatable("fantasticpass.msg.no_active_pass"));
            return 0;
        }
        PlayerPassData data = PassCapability.getData(player);
        if (data == null) {
            return 0;
        }
        int minutesPerTier = pass.getMinutesPerTierOverride() > 0
                ? pass.getMinutesPerTierOverride()
                : PassConfig.MINUTES_PER_TIER.get();
        PacketHandler.sendToPlayer(player, new OpenViewScreenPacket(pass, data, minutesPerTier));
        return 1;
    }

    private static int rango(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "id");
        PlayerPassData data = PassCapability.getData(player);
        if (data == null) {
            return 0;
        }

        if (id.equalsIgnoreCase("none") || id.equalsIgnoreCase("clear")) {
            data.setDisplayedRankId(null);
            NametagSync.syncPlayer(player);
            ctx.getSource().sendSuccess(() -> Component.translatable("fantasticpass.msg.rank_set", "none"), false);
            return 1;
        }

        if (!data.hasEarnedRank(id)) {
            ctx.getSource().sendFailure(Component.translatable("fantasticpass.msg.rank_not_owned", id));
            return 0;
        }
        data.setDisplayedRankId(id);
        NametagSync.syncPlayer(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("fantasticpass.msg.rank_set", id), false);
        return 1;
    }
}
