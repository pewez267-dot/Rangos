package com.claimblocks.command;

import com.claimblocks.block.ClaimStoneBlock;
import com.claimblocks.block.ModBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimFlags;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.gui.ClaimMenuHandler;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * /claim ... - todos los comandos requieren OP nivel 2.
 *
 * /claim give <jugador> <id>     - id es uno de los 10 claimstone_NxN
 * /claim clear <jugador>
 * /claim remove
 * /claim menu
 * /claim list
 * /claim info
 * /claim ban <jugador>
 * /claim unban <jugador>
 */
public final class ClaimCommands {

    private static final SuggestionProvider<ServerCommandSource> CLAIMSTONE_IDS =
        (context, builder) -> {
            String start = builder.getRemaining().toLowerCase();
            for (ClaimTier t : ClaimTier.VALUES) {
                if (t.id.startsWith(start)) builder.suggest(t.id);
            }
            return builder.buildFuture();
        };

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            dispatcher.register(CommandManager.literal("claim")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("give")
                    .then(CommandManager.argument("jugador", EntityArgumentType.players())
                        .then(CommandManager.argument("id", StringArgumentType.word())
                            .suggests(CLAIMSTONE_IDS)
                            .executes(ClaimCommands::give))))
                .then(CommandManager.literal("clear")
                    .then(CommandManager.argument("jugador", EntityArgumentType.player())
                        .executes(ClaimCommands::clear)))
                .then(CommandManager.literal("remove").executes(ClaimCommands::remove))
                .then(CommandManager.literal("menu").executes(ClaimCommands::menu))
                .then(CommandManager.literal("list").executes(ClaimCommands::list))
                .then(CommandManager.literal("info").executes(ClaimCommands::info))
                .then(CommandManager.literal("ban")
                    .then(CommandManager.argument("jugador", EntityArgumentType.player())
                        .executes(ClaimCommands::ban)))
                .then(CommandManager.literal("unban")
                    .then(CommandManager.argument("jugador", EntityArgumentType.player())
                        .executes(ClaimCommands::unban)))
            );
        });
    }

    private static int give(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "jugador");
        String id = StringArgumentType.getString(ctx, "id");
        ClaimTier tier = ClaimTier.byId(id);
        if (tier == null) {
            ctx.getSource().sendError(Text.literal("[x] ID no valido: " + id));
            return 0;
        }
        Block block = ModBlocks.byId(id);
        if (block == null) {
            ctx.getSource().sendError(Text.literal("[x] Bloque no registrado para: " + id));
            return 0;
        }
        for (ServerPlayerEntity p : targets) {
            ItemStack stack = new ItemStack(block.asItem());
            if (!p.getInventory().insertStack(stack)) p.dropItem(stack, false);
            p.sendMessage(Text.literal("[OK] Recibiste Piedra de Claim " + tier.label()), false);
            ctx.getSource().sendFeedback(() -> Text.literal(
                "[OK] Le diste Piedra " + tier.label() + " a " + p.getName().getString()), true);
        }
        return targets.size();
    }

    private static int clear(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
        int n = ClaimManager.getInstance().clearClaimsOf(target.getUuid());
        ctx.getSource().sendFeedback(() -> Text.literal(
            "[OK] Eliminadas " + n + " zona(s) de " + target.getName().getString()), true);
        return n;
    }

    private static int remove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), p.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("[x] No estas en ninguna zona protegida."));
            return 0;
        }
        if (!c.isOwner(p) && !p.hasPermissionLevel(2)) {
            ctx.getSource().sendError(Text.literal("[x] Solo el dueno puede eliminar esta zona."));
            return 0;
        }
        BlockPos centre = c.getCenter();
        if (p.getWorld().getBlockState(centre).getBlock() instanceof ClaimStoneBlock) {
            p.getWorld().breakBlock(centre, false, p);
        }
        ClaimManager.getInstance().removeClaim(p.getWorld(), centre);

        // Refund
        Block b = c.getTierId() != null ? ModBlocks.byId(c.getTierId()) : null;
        if (b != null) {
            ItemStack stack = new ItemStack(b);
            if (!p.getInventory().insertStack(stack)) p.dropItem(stack, false);
        }
        ctx.getSource().sendFeedback(() -> Text.literal(
            "[OK] Zona eliminada. Item devuelto."), false);
        return 1;
    }

    private static int menu(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), p.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("[x] No estas en ninguna zona protegida."));
            return 0;
        }
        if (!c.isOwner(p) && !p.hasPermissionLevel(2)) {
            ctx.getSource().sendError(Text.literal("[x] Solo el dueno puede administrar esta zona."));
            return 0;
        }
        ClaimMenuHandler.open(p, c, 0);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        List<Claim> claims = ClaimManager.getInstance().getClaimsOf(p.getUuid());
        ctx.getSource().sendFeedback(() -> Text.literal(
            "[Claim] Tus zonas (" + claims.size() + "):"), false);
        for (Claim c : claims) {
            ctx.getSource().sendFeedback(() -> Text.literal(
                "  - " + c.sizeLabel() + " en X="
                + c.getX() + " Y=" + c.getY() + " Z=" + c.getZ()
                + " - " + c.getWorld()), false);
        }
        if (claims.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("  (no tienes ninguna)"), false);
        }
        return claims.size();
    }

    private static int info(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), p.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("[x] No estas en ninguna zona protegida."));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("[Claim] Info de la zona:"), false);
        ctx.getSource().sendFeedback(() -> Text.literal(
            "  Dueno: " + c.getOwnerName()), false);
        ctx.getSource().sendFeedback(() -> Text.literal(
            "  Zona: " + c.sizeLabel() + " bloques | Altura: +/-" + c.getHeight()), false);
        ctx.getSource().sendFeedback(() -> Text.literal(
            "  Coords: X=" + c.getX() + " Y=" + c.getY() + " Z=" + c.getZ()
            + " - " + c.getWorld()), false);
        ctx.getSource().sendFeedback(() -> Text.literal(
            "  Miembros: " + c.getMembers().size()
            + " | Baneados: " + c.getBannedPlayers().size()), false);
        ClaimFlags f = c.getFlags();
        ctx.getSource().sendFeedback(() -> Text.literal("  Flags activas:"), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Construccion", f.blockBuilding), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Destruccion", f.blockBreaking), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Explosiones", f.blockExplosions), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Fuego", f.blockFire), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Spawn mobs", f.blockMobSpawn), false);
        ctx.getSource().sendFeedback(() -> formatFlag("PVP", f.blockPVP), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Dano de mobs", f.blockMobDamage), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Alertas", f.trespasserAlerts), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Uso de items", f.blockItemUse), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Interac. entidades", f.blockEntityInteract), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Pisar cultivos", f.blockTrampling), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Fluidos", f.blockFluids), false);
        ctx.getSource().sendFeedback(() -> formatFlag("PVP contra todos", f.pvpAll), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Talar arboles", f.blockTreeChopping), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Modo publico", f.publicMode), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Bienvenida", f.showWelcome), false);
        return 1;
    }

    private static Text formatFlag(String name, boolean on) {
        return Text.literal("    " + name + ": " + (on ? "[ON]" : "[OFF]"));
    }

    private static int ban(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity exec = ctx.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
        Claim c = ClaimManager.getInstance().getClaimAt(exec.getWorld(), exec.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("[x] No estas en ninguna zona protegida."));
            return 0;
        }
        if (!c.isOwner(exec) && !exec.hasPermissionLevel(2)) {
            ctx.getSource().sendError(Text.literal("[x] Solo el dueno puede banear de esta zona."));
            return 0;
        }
        c.banPlayer(target.getUuid());
        ClaimManager.getInstance().save();
        ctx.getSource().sendFeedback(() -> Text.literal("[OK] " + target.getName().getString() + " baneado."), true);
        return 1;
    }

    private static int unban(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity exec = ctx.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
        Claim c = ClaimManager.getInstance().getClaimAt(exec.getWorld(), exec.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("[x] No estas en ninguna zona protegida."));
            return 0;
        }
        if (!c.isOwner(exec) && !exec.hasPermissionLevel(2)) {
            ctx.getSource().sendError(Text.literal("[x] Solo el dueno puede desbanear de esta zona."));
            return 0;
        }
        c.unbanPlayer(target.getUuid());
        ClaimManager.getInstance().save();
        ctx.getSource().sendFeedback(() -> Text.literal("[OK] " + target.getName().getString() + " desbaneado."), true);
        return 1;
    }
}
