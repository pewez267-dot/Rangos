package com.fantasticranks.commands;

import com.fantasticranks.capability.RanksCapability;
import com.fantasticranks.data.PlayerRanksData;
import com.fantasticranks.data.RankDefinition;
import com.fantasticranks.data.RanksPackage;
import com.fantasticranks.data.RanksSavedData;
import com.fantasticranks.network.NametagSync;
import com.fantasticranks.network.OpenAdminScreenPacket;
import com.fantasticranks.network.PacketHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code /fsranks} command tree. All subcommands require permission level 4 (OP).
 */
public final class FsRanksCommand {

    /** Suggests the ids of saved packages for edit/delete/activate. */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_PACKAGE_IDS = (ctx, builder) -> {
        MinecraftServer server = ctx.getSource().getServer();
        if (server != null) {
            return SharedSuggestionProvider.suggest(RanksSavedData.get(server).getPackages().keySet(), builder);
        }
        return builder.buildFuture();
    };

    /** Suggests the rank names of the active package for /fsranks test. */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_RANK_NAMES = (ctx, builder) -> {
        MinecraftServer server = ctx.getSource().getServer();
        List<String> names = new ArrayList<>();
        if (server != null) {
            RanksPackage pkg = RanksSavedData.get(server).getActivePackage();
            if (pkg != null) {
                for (RankDefinition rank : pkg.getRanks()) {
                    names.add(rank.getRankName());
                }
            }
        }
        names.add("clear");
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private FsRanksCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fsranks")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("create")
                        .executes(FsRanksCommand::create))
                .then(Commands.literal("edit")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests(SUGGEST_PACKAGE_IDS)
                                .executes(FsRanksCommand::edit)))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests(SUGGEST_PACKAGE_IDS)
                                .executes(FsRanksCommand::delete)))
                .then(Commands.literal("activate")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests(SUGGEST_PACKAGE_IDS)
                                .executes(FsRanksCommand::activate)))
                .then(Commands.literal("test")
                        .then(Commands.argument("rank", StringArgumentType.greedyString())
                                .suggests(SUGGEST_RANK_NAMES)
                                .executes(FsRanksCommand::test))));
    }

    private static int create(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        RanksPackage fresh = RanksPackage.createDefault("", "New Package");
        PacketHandler.sendToPlayer(player, new OpenAdminScreenPacket(fresh));
        return 1;
    }

    private static int edit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "id");
        RanksPackage pkg = RanksSavedData.get(player.getServer()).getPackage(id);
        if (pkg == null) {
            ctx.getSource().sendFailure(Component.translatable("fantasticranks.msg.package_not_found", id));
            return 0;
        }
        PacketHandler.sendToPlayer(player, new OpenAdminScreenPacket(pkg.copy()));
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        String id = StringArgumentType.getString(ctx, "id");
        RanksSavedData saved = RanksSavedData.get(server);
        if (saved.deletePackage(id)) {
            ctx.getSource().sendSuccess(() -> Component.translatable("fantasticranks.msg.package_deleted", id), true);
            return 1;
        }
        ctx.getSource().sendFailure(Component.translatable("fantasticranks.msg.package_not_found", id));
        return 0;
    }

    private static int activate(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        String id = StringArgumentType.getString(ctx, "id");
        RanksSavedData saved = RanksSavedData.get(server);
        if (!saved.hasPackage(id)) {
            ctx.getSource().sendFailure(Component.translatable("fantasticranks.msg.package_not_found", id));
            return 0;
        }
        saved.setActivePackageId(id);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerRanksData data = RanksCapability.getData(player);
            if (data != null) {
                data.clearPreview();
                data.resetProgress(id);
                NametagSync.syncPlayer(player);
            }
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("fantasticranks.msg.package_activated", id), true);
        return 1;
    }

    private static int test(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String rankName = StringArgumentType.getString(ctx, "rank").trim();
        PlayerRanksData data = RanksCapability.getData(player);
        if (data == null) {
            return 0;
        }

        if (rankName.equalsIgnoreCase("clear") || rankName.equalsIgnoreCase("none")) {
            data.clearPreview();
            NametagSync.syncPlayer(player);
            ctx.getSource().sendSuccess(() -> Component.translatable("fantasticranks.msg.test_cleared"), false);
            return 1;
        }

        RanksPackage pkg = RanksSavedData.get(player.getServer()).getActivePackage();
        if (pkg == null) {
            ctx.getSource().sendFailure(Component.translatable("fantasticranks.msg.no_active_package"));
            return 0;
        }
        RankDefinition rank = pkg.findByName(rankName);
        if (rank == null) {
            ctx.getSource().sendFailure(Component.translatable("fantasticranks.msg.rank_not_found", rankName));
            return 0;
        }

        data.setPreview(rank.getRankName(), rank.getRankNumber(), rank.getStyle());
        NametagSync.syncPlayer(player);
        final String shown = rank.getRankName();
        ctx.getSource().sendSuccess(() -> Component.translatable("fantasticranks.msg.test_applied", shown), false);
        return 1;
    }
}
