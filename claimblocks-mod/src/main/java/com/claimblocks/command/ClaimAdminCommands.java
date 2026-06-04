package com.claimblocks.command;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.data.GlobalFlags;
import com.claimblocks.gui.AdminPanelHandler;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * /claimadmin ... - all admin operations. Every subcommand requires
 * permission level 2 (op).
 *
 * Subcommands:
 *   /claimadmin                        - opens the admin panel GUI
 *   /claimadmin list                   - chat list of every claim
 *   /claimadmin bypass                 - toggle bypass mode
 *   /claimadmin stats                  - print server-wide statistics
 *   /claimadmin globalflag <name> <on|off>
 */
public final class ClaimAdminCommands {

    private static final SuggestionProvider<ServerCommandSource> GLOBAL_FLAG_NAMES =
        (ctx, builder) -> {
            String s = builder.getRemaining().toLowerCase();
            for (String n : new String[]{"globalPVP", "globalMobGriefing", "globalFireSpread"}) {
                if (n.toLowerCase().startsWith(s)) builder.suggest(n);
            }
            return builder.buildFuture();
        };

    private static final SuggestionProvider<ServerCommandSource> ON_OFF =
        (ctx, builder) -> {
            String s = builder.getRemaining().toLowerCase();
            for (String n : new String[]{"on", "off"}) {
                if (n.startsWith(s)) builder.suggest(n);
            }
            return builder.buildFuture();
        };

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            dispatcher.register(CommandManager.literal("claimadmin")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(ClaimAdminCommands::openPanel)
                .then(CommandManager.literal("list").executes(ClaimAdminCommands::list))
                .then(CommandManager.literal("bypass").executes(ClaimAdminCommands::toggleBypass))
                .then(CommandManager.literal("stats").executes(ClaimAdminCommands::stats))
                .then(CommandManager.literal("globalflag")
                    .then(CommandManager.argument("flag", StringArgumentType.word())
                        .suggests(GLOBAL_FLAG_NAMES)
                        .then(CommandManager.argument("value", StringArgumentType.word())
                            .suggests(ON_OFF)
                            .executes(ClaimAdminCommands::globalFlag))))
            );
        });
    }

    /* ---- /claimadmin (no args) - open GUI panel ---- */
    private static int openPanel(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        AdminPanelHandler.open(p, 0);
        return 1;
    }

    /* ---- /claimadmin list ---- */
    private static int list(CommandContext<ServerCommandSource> ctx) {
        List<Claim> all = ClaimManager.getInstance().getAllClaims();
        if (all.isEmpty()) {
            ctx.getSource().sendFeedback(() ->
                Text.literal("[i] No hay zonas activas en el servidor.").formatted(Formatting.AQUA),
                false);
            return 0;
        }
        for (Claim c : all) {
            Text line = Text.literal("✔ ").formatted(Formatting.AQUA, Formatting.BOLD)
                .append(Text.literal(c.getOwnerName()).formatted(Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal(" | ").formatted(Formatting.GRAY))
                .append(Text.literal(c.sizeLabel()).formatted(Formatting.YELLOW))
                .append(Text.literal(" | ").formatted(Formatting.GRAY))
                .append(Text.literal("X:" + c.getX() + " Z:" + c.getZ()).formatted(Formatting.WHITE))
                .append(Text.literal(" | ").formatted(Formatting.GRAY))
                .append(Text.literal("Dim:" + c.getWorld()).formatted(Formatting.DARK_AQUA));
            ctx.getSource().sendFeedback(() -> line, false);
        }
        return all.size();
    }

    /* ---- /claimadmin bypass ---- */
    private static int toggleBypass(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        boolean now = ClaimManager.getInstance().toggleBypass(p.getUuid());
        if (now) {
            p.sendMessage(Text.literal("✔ Modo bypass activado. Las zonas no te afectan.")
                .formatted(Formatting.GOLD, Formatting.BOLD), false);
        } else {
            p.sendMessage(Text.literal("✔ Modo bypass desactivado.")
                .formatted(Formatting.GREEN), false);
        }
        return 1;
    }

    /* ---- /claimadmin stats ---- */
    private static int stats(CommandContext<ServerCommandSource> ctx) {
        List<Claim> all = ClaimManager.getInstance().getAllClaims();
        Set<UUID> uniqueOwners = new HashSet<>();
        Claim biggest = null;
        Claim oldest = null;
        int paid = 0;
        for (Claim c : all) {
            uniqueOwners.add(c.getOwnerUUID());
            if (biggest == null || c.getRadius() > biggest.getRadius()) biggest = c;
            if (oldest == null || c.getCreatedAt() < oldest.getCreatedAt()) oldest = c;
            ClaimTier t = c.getTier();
            if (t != null && t.isPaid()) paid++;
        }

        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("--- Estadísticas de ClaimBlocks ---").formatted(Formatting.GOLD), false);
        src.sendFeedback(() -> infoLine("Total de zonas activas: " + all.size()), false);
        src.sendFeedback(() -> infoLine("Jugadores con zona: " + uniqueOwners.size()), false);
        if (biggest != null) {
            Claim b = biggest;
            src.sendFeedback(() -> infoLine("Zona más grande: " + b.sizeLabel() + " de " + b.getOwnerName()), false);
        }
        if (oldest != null) {
            Claim o = oldest;
            String when = o.getCreatedAt() == 0 ? "(legacy)" : new java.util.Date(o.getCreatedAt()).toString();
            src.sendFeedback(() -> Text.literal("✔ Zona más antigua: ")
                .formatted(Formatting.AQUA, Formatting.BOLD)
                .append(Text.literal(o.sizeLabel() + " de " + o.getOwnerName()).formatted(Formatting.WHITE))
                .append(Text.literal(" (" + when + ")").formatted(Formatting.DARK_GRAY)), false);
        }
        final int paidCount = paid;
        src.sendFeedback(() -> Text.literal("✔ Zonas de pago activas: " + paidCount).formatted(Formatting.GOLD, Formatting.BOLD), false);
        src.sendFeedback(() -> Text.literal("-----------------------------------").formatted(Formatting.GOLD), false);
        return 1;
    }

    private static Text infoLine(String text) {
        return Text.literal("✔ ").formatted(Formatting.AQUA, Formatting.BOLD)
            .append(Text.literal(text).formatted(Formatting.AQUA));
    }

    /* ---- /claimadmin globalflag <name> <on|off> ---- */
    private static int globalFlag(CommandContext<ServerCommandSource> ctx) {
        String flag = StringArgumentType.getString(ctx, "flag");
        String value = StringArgumentType.getString(ctx, "value").toLowerCase();
        if (!flag.equals("globalPVP") && !flag.equals("globalMobGriefing")
            && !flag.equals("globalFireSpread")) {
            ctx.getSource().sendError(Text.literal("[x] Flag global desconocida: " + flag)
                .formatted(Formatting.RED));
            return 0;
        }
        boolean on = value.equals("on") || value.equals("true");
        GlobalFlags.getInstance().set(flag, on, ctx.getSource().getServer());
        ctx.getSource().sendFeedback(() ->
            Text.literal("✔ Flag global ").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal(flag).formatted(Formatting.YELLOW))
                .append(Text.literal(" establecida a ").formatted(Formatting.GOLD))
                .append(Text.literal(on ? "[ON]" : "[OFF]").formatted(on ? Formatting.GREEN : Formatting.RED, Formatting.BOLD))
                .append(Text.literal(".").formatted(Formatting.GOLD)),
            true);
        // broadcast
        Text bcast = Text.literal("[!] Un administrador cambió una configuración global del servidor.")
            .formatted(Formatting.YELLOW);
        ctx.getSource().getServer().getPlayerManager().getPlayerList().forEach(p -> p.sendMessage(bcast, false));
        return 1;
    }
}
