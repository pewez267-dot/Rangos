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
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.List;

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
            ctx.getSource().sendError(Text.literal("[x] ID no válido: " + id).formatted(Formatting.RED));
            return 0;
        }
        Block block = ModBlocks.byId(id);
        if (block == null) {
            ctx.getSource().sendError(Text.literal("[x] Bloque no registrado para: " + id)
                .formatted(Formatting.RED));
            return 0;
        }
        for (ServerPlayerEntity p : targets) {
            ItemStack stack = new ItemStack(block.asItem());
            if (!p.getInventory().insertStack(stack)) p.dropItem(stack, false);
            p.sendMessage(Text.literal("[+] ").formatted(Formatting.GREEN, Formatting.BOLD)
                .append(Text.literal("Recibiste Piedra de Claim ").formatted(Formatting.GREEN))
                .append(Text.literal(tier.label()).formatted(Formatting.YELLOW, Formatting.BOLD)),
                false);
            ctx.getSource().sendFeedback(() ->
                Text.literal("[OK] ").formatted(Formatting.GREEN, Formatting.BOLD)
                    .append(Text.literal("Le diste Piedra ").formatted(Formatting.GREEN))
                    .append(Text.literal(tier.label()).formatted(Formatting.YELLOW))
                    .append(Text.literal(" a ").formatted(Formatting.GREEN))
                    .append(Text.literal(p.getName().getString())
                        .formatted(Formatting.WHITE, Formatting.BOLD)),
                true);
        }
        return targets.size();
    }

    private static int clear(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
        int n = ClaimManager.getInstance().clearClaimsOf(target.getUuid());
        ctx.getSource().sendFeedback(() -> Text.literal("[OK] ")
            .formatted(Formatting.GREEN, Formatting.BOLD)
            .append(Text.literal("Eliminadas " + n + " zona(s) de ").formatted(Formatting.GREEN))
            .append(Text.literal(target.getName().getString())
                .formatted(Formatting.WHITE, Formatting.BOLD)),
            true);
        return n;
    }

    private static int remove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), p.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("[x] No estás en ninguna zona protegida.")
                .formatted(Formatting.RED));
            return 0;
        }
        if (!c.isOwner(p) && !p.hasPermissionLevel(2)) {
            ctx.getSource().sendError(Text.literal("[x] Solo el dueño puede eliminar esta zona.")
                .formatted(Formatting.RED));
            return 0;
        }
        BlockPos centre = c.getCenter();
        if (p.getWorld().getBlockState(centre).getBlock() instanceof ClaimStoneBlock) {
            p.getWorld().breakBlock(centre, false, p);
        }
        ClaimManager.getInstance().removeClaim(p.getWorld(), centre);

        Block b = c.getTierId() != null ? ModBlocks.byId(c.getTierId()) : null;
        if (b != null) {
            ItemStack stack = new ItemStack(b);
            if (!p.getInventory().insertStack(stack)) p.dropItem(stack, false);
        }
        ctx.getSource().sendFeedback(() ->
            Text.literal("[OK] Zona eliminada. Piedra devuelta a tu inventario.")
                .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int menu(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), p.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("[x] No estás en ninguna zona protegida.")
                .formatted(Formatting.RED));
            return 0;
        }
        if (!c.isOwner(p) && !p.hasPermissionLevel(2)) {
            ctx.getSource().sendError(Text.literal("[x] Solo el dueño puede administrar esta zona.")
                .formatted(Formatting.RED));
            return 0;
        }
        ClaimMenuHandler.open(p, c, 0);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        List<Claim> claims = ClaimManager.getInstance().getClaimsOf(p.getUuid());
        ctx.getSource().sendFeedback(() ->
            Text.literal("[Claim] ").formatted(Formatting.GRAY)
                .append(Text.literal("Tus zonas (" + claims.size() + "):")
                    .formatted(Formatting.AQUA)),
            false);
        for (Claim c : claims) {
            ctx.getSource().sendFeedback(() ->
                Text.literal("  >> ").formatted(Formatting.GRAY)
                    .append(Text.literal(c.sizeLabel()).formatted(Formatting.YELLOW))
                    .append(Text.literal(" en X=" + c.getX() + " Y=" + c.getY()
                        + " Z=" + c.getZ()).formatted(Formatting.WHITE))
                    .append(Text.literal(" - " + c.getWorld()).formatted(Formatting.DARK_GRAY)),
                false);
        }
        if (claims.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("  (no tienes ninguna)")
                .formatted(Formatting.DARK_GRAY), false);
        }
        return claims.size();
    }

    private static int info(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), p.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("[x] No estás en ninguna zona protegida.")
                .formatted(Formatting.RED));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("[Claim] ").formatted(Formatting.GRAY)
            .append(Text.literal("Información de la zona:").formatted(Formatting.AQUA, Formatting.BOLD)),
            false);
        ctx.getSource().sendFeedback(() -> labelLine("Dueño", c.getOwnerName(), Formatting.WHITE), false);
        ctx.getSource().sendFeedback(() -> labelLine("Zona",
            c.sizeLabel() + " bloques | Altura: +/-" + c.getHeight(), Formatting.YELLOW), false);
        ctx.getSource().sendFeedback(() -> labelLine("Coords",
            "X=" + c.getX() + " Y=" + c.getY() + " Z=" + c.getZ() + " - " + c.getWorld(),
            Formatting.WHITE), false);
        ctx.getSource().sendFeedback(() -> labelLine("Miembros",
            String.valueOf(c.getMembers().size()), Formatting.WHITE), false);
        ctx.getSource().sendFeedback(() -> labelLine("Baneados",
            String.valueOf(c.getBannedPlayers().size()), Formatting.WHITE), false);
        ClaimFlags f = c.getFlags();
        ctx.getSource().sendFeedback(() ->
            Text.literal("  Flags activas:").formatted(Formatting.GRAY), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Construir", f.blockBuilding), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Romper", f.blockBreaking), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Explosiones", f.blockExplosions), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Fuego", f.blockFire), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Mobs hostiles", f.blockMobSpawn), false);
        ctx.getSource().sendFeedback(() -> formatFlag("PVP", f.blockPVP), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Daño de mobs", f.blockMobDamage), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Alertas", f.trespasserAlerts), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Usar items", f.blockItemUse), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Entidades", f.blockEntityInteract), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Cultivos", f.blockTrampling), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Fluidos", f.blockFluids), false);
        ctx.getSource().sendFeedback(() -> formatFlag("PVP libre", f.pvpAll), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Árboles", f.blockTreeChopping), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Modo visita", f.publicMode), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Bienvenida", f.showWelcome), false);
        ClaimTier tier = c.getTier();
        if (tier != null && tier.isPaid()) {
            ctx.getSource().sendFeedback(() -> formatFlag("Regeneración", f.effectRegeneration), false);
            ctx.getSource().sendFeedback(() -> formatFlag("Resistencia", f.effectResistance), false);
            ctx.getSource().sendFeedback(() -> formatFlag("Velocidad", f.effectSpeed), false);
        }
        return 1;
    }

    private static Text labelLine(String key, String value, Formatting valueColor) {
        return Text.literal("  " + key + ": ").formatted(Formatting.GRAY)
            .append(Text.literal(value).formatted(valueColor));
    }

    private static Text formatFlag(String name, boolean on) {
        return Text.literal("    " + name + ": ").formatted(Formatting.GRAY)
            .append(Text.literal(on ? "[ON]" : "[OFF]")
                .formatted(on ? Formatting.GREEN : Formatting.RED, Formatting.BOLD));
    }

    private static int ban(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity exec = ctx.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
        Claim c = ClaimManager.getInstance().getClaimAt(exec.getWorld(), exec.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("[x] No estás en ninguna zona protegida.")
                .formatted(Formatting.RED));
            return 0;
        }
        if (!c.isOwner(exec) && !exec.hasPermissionLevel(2)) {
            ctx.getSource().sendError(Text.literal("[x] Solo el dueño puede banear de esta zona.")
                .formatted(Formatting.RED));
            return 0;
        }
        c.banPlayer(target.getUuid());
        ClaimManager.getInstance().save();
        ctx.getSource().sendFeedback(() ->
            Text.literal("[OK] ").formatted(Formatting.GREEN, Formatting.BOLD)
                .append(Text.literal(target.getName().getString())
                    .formatted(Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal(" baneado.").formatted(Formatting.GREEN)),
            true);
        return 1;
    }

    private static int unban(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity exec = ctx.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
        Claim c = ClaimManager.getInstance().getClaimAt(exec.getWorld(), exec.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("[x] No estás en ninguna zona protegida.")
                .formatted(Formatting.RED));
            return 0;
        }
        if (!c.isOwner(exec) && !exec.hasPermissionLevel(2)) {
            ctx.getSource().sendError(Text.literal("[x] Solo el dueño puede desbanear de esta zona.")
                .formatted(Formatting.RED));
            return 0;
        }
        c.unbanPlayer(target.getUuid());
        ClaimManager.getInstance().save();
        ctx.getSource().sendFeedback(() ->
            Text.literal("[OK] ").formatted(Formatting.GREEN, Formatting.BOLD)
                .append(Text.literal(target.getName().getString())
                    .formatted(Formatting.WHITE, Formatting.BOLD))
                .append(Text.literal(" desbaneado.").formatted(Formatting.GREEN)),
            true);
        return 1;
    }
}
