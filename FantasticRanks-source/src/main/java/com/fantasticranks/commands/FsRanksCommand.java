package com.fantasticranks.commands;

import com.fantasticranks.capability.RanksCapability;
import com.fantasticranks.config.RanksConfig;
import com.fantasticranks.data.PlayerRanksData;
import com.fantasticranks.data.RankDefinition;
import com.fantasticranks.data.RanksPackage;
import com.fantasticranks.data.RanksSavedData;
import com.fantasticranks.nametag.NametagBuilder;
import com.fantasticranks.network.NametagSync;
import com.fantasticranks.network.OpenAdminScreenPacket;
import com.fantasticranks.network.PacketHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class FsRanksCommand {
    // Autocompletado: ids de todos los paquetes de rangos.
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_PACKAGE_IDS = (ctx, builder) -> {
        MinecraftServer server = ctx.getSource().getServer();
        return server != null ? SharedSuggestionProvider.suggest(RanksSavedData.get(server).getPackages().keySet(), builder) : builder.buildFuture();
    };
    // Autocompletado: nombres de rangos del paquete activo + palabras clave (all/clear).
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_RANK_NAMES = (ctx, builder) -> {
        List<String> out = new ArrayList<>();
        out.add("all");
        out.add("clear");
        MinecraftServer server = ctx.getSource().getServer();
        if (server != null) {
            RanksPackage pkg = RanksSavedData.get(server).getActivePackage();
            if (pkg != null) {
                for (RankDefinition r : pkg.getRanks()) {
                    if (r != null && r.getRankName() != null && !r.getRankName().isEmpty()) {
                        out.add(r.getRankName());
                    }
                }
            }
        }
        return SharedSuggestionProvider.suggest(out, builder);
    };

    private FsRanksCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fsranks").requires(source -> source.hasPermission(4))
                .then(Commands.literal("create").executes(FsRanksCommand::create))
                .then(Commands.literal("edit").then(Commands.argument("id", StringArgumentType.string()).suggests(SUGGEST_PACKAGE_IDS).executes(FsRanksCommand::edit)))
                .then(Commands.literal("delete").then(Commands.argument("id", StringArgumentType.string()).suggests(SUGGEST_PACKAGE_IDS).executes(FsRanksCommand::delete)))
                .then(Commands.literal("activate").then(Commands.argument("id", StringArgumentType.string()).suggests(SUGGEST_PACKAGE_IDS).executes(FsRanksCommand::activate)))
                .then(Commands.literal("test").then(Commands.argument("rank", StringArgumentType.greedyString()).suggests(SUGGEST_RANK_NAMES).executes(FsRanksCommand::test)))
                // Limpia TODO el progreso/rango guardado de TODOS los jugadores (online ahora, offline al reconectar).
                // Requiere confirmacion: /fsranks wipe confirmar
                .then(Commands.literal("wipe").executes(FsRanksCommand::wipeInfo)
                        .then(Commands.literal("confirmar").executes(FsRanksCommand::wipe))
                        .then(Commands.literal("confirm").executes(FsRanksCommand::wipe))));
    }

    private static int create(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        RanksPackage fresh = RanksPackage.createDefault("", "Paquete Nuevo");
        PacketHandler.sendToPlayer(player, new OpenAdminScreenPacket(fresh));
        return 1;
    }

    private static int edit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "id");
        RanksPackage pkg = RanksSavedData.get(ctx.getSource().getServer()).getPackage(id);
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
            if (data == null) {
                continue;
            }
            data.clearPreview();
            data.resetProgress(id);
            NametagSync.syncPlayer(player);
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
        RanksPackage pkg = RanksSavedData.get(ctx.getSource().getServer()).getActivePackage();
        if (pkg == null) {
            ctx.getSource().sendFailure(Component.translatable("fantasticranks.msg.no_active_package"));
            return 0;
        }
        // "test all" -> muestra TODOS los rangos con su estilo y el mensaje que sale al ganarlos.
        if (rankName.equalsIgnoreCase("all") || rankName.equalsIgnoreCase("todos")) {
            ctx.getSource().sendSuccess(() -> Component.translatable("fantasticranks.msg.test_all_header"), false);
            for (int i = 0; i < pkg.size(); ++i) {
                RankDefinition rank = pkg.get(i);
                MutableComponent styled = Component.literal("\u00a77Lvl " + rank.getRankNumber() + " ").append(NametagBuilder.buildStyledText(rank.getRankName(), rank.getStyle()));
                String upMsg = RanksConfig.RANK_UP_MESSAGE.get().replace("{rank}", rank.getRankName());
                MutableComponent full = styled.append(Component.literal("\u00a78  \u2192 ")).append(Component.literal(upMsg));
                ctx.getSource().sendSuccess(() -> full, false);
            }
            // Deja el primer rango como vista previa en el nametag para verlo puesto.
            if (pkg.size() > 0) {
                RankDefinition first = pkg.get(0);
                data.setPreview(first.getRankName(), first.getRankNumber(), first.getStyle());
                NametagSync.syncPlayer(player);
            }
            return 1;
        }
        RankDefinition rank = pkg.findByName(rankName);
        if (rank == null) {
            ctx.getSource().sendFailure(Component.translatable("fantasticranks.msg.rank_not_found", rankName));
            return 0;
        }
        data.setPreview(rank.getRankName(), rank.getRankNumber(), rank.getStyle());
        NametagSync.syncPlayer(player);
        String shown = rank.getRankName();
        ctx.getSource().sendSuccess(() -> Component.translatable("fantasticranks.msg.test_applied", shown), false);
        // Muestra tambien el mensaje que aparece al ganar ese rango.
        String upMsg = RanksConfig.RANK_UP_MESSAGE.get().replace("{rank}", rank.getRankName());
        ctx.getSource().sendSuccess(() -> Component.translatable("fantasticranks.msg.test_rankup").append(Component.literal(upMsg)), false);
        return 1;
    }

    // /fsranks wipe -> pide confirmacion antes de borrar todo.
    private static int wipeInfo(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendFailure(Component.literal("\u00a7c\u26a0 Esto BORRA el progreso y rango de tiempo de TODOS los jugadores (conectados y desconectados). Los rangos guardados por otros medios NO se tocan."));
        ctx.getSource().sendFailure(Component.literal("\u00a7eEscribe \u00a7f/fsranks wipe confirmar\u00a7e para confirmar."));
        return 0;
    }

    // /fsranks wipe confirmar -> limpia el progreso de todos. Los offline se limpian al reconectar
    // (via la "generacion de wipe" que se compara en el tick de progresion).
    private static int wipe(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        if (server == null) {
            return 0;
        }
        RanksSavedData saved = RanksSavedData.get(server);
        long gen = saved.bumpWipeGeneration();
        RanksPackage active = saved.getActivePackage();
        String activeId = active != null ? active.getId() : "";
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerRanksData data = RanksCapability.getData(player);
            if (data == null) {
                continue;
            }
            data.resetProgress(activeId);
            data.setWipeSeen(gen);
            data.clearPreview();
            NametagSync.syncPlayer(player);
            ++count;
        }
        int online = count;
        ctx.getSource().sendSuccess(() -> Component.literal("\u00a7a\u2714 Rangos de tiempo limpiados. \u00a7f" + online + "\u00a7a jugador(es) conectado(s) reiniciado(s); los desconectados se limpiaran al entrar."), true);
        return 1;
    }
}
