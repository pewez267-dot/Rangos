package com.claimblocks.command;

import com.claimblocks.block.ClaimBlock;
import com.claimblocks.block.ModBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimFlags;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.gui.ClaimMenuHandler;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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

/**
 * /claim ... — todos los comandos requieren OP (permission level 2) salvo
 * que se indique otra cosa.
 */
public final class ClaimCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            dispatcher.register(CommandManager.literal("claim")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("give")
                    .then(CommandManager.argument("jugador", EntityArgumentType.players())
                        .then(CommandManager.argument("tier", IntegerArgumentType.integer(1, 5))
                            .executes(ClaimCommands::give))))
                .then(CommandManager.literal("clear")
                    .then(CommandManager.argument("jugador", EntityArgumentType.player())
                        .executes(ClaimCommands::clear)))
                .then(CommandManager.literal("remove")
                    .executes(ClaimCommands::remove))
                .then(CommandManager.literal("menu")
                    .executes(ClaimCommands::menu))
                .then(CommandManager.literal("list")
                    .executes(ClaimCommands::list))
                .then(CommandManager.literal("info")
                    .executes(ClaimCommands::info))
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
        int tier = IntegerArgumentType.getInteger(ctx, "tier");
        Block block = ModBlocks.forTier(tier);
        if (block == null) {
            ctx.getSource().sendError(Text.literal("Tier inválido: " + tier));
            return 0;
        }
        for (ServerPlayerEntity p : targets) {
            ItemStack stack = new ItemStack(block.asItem());
            if (!p.getInventory().insertStack(stack)) p.dropItem(stack, false);
            p.sendMessage(Text.literal("§a✅ Has recibido un Claim Tier " + tier), false);
            ctx.getSource().sendFeedback(() -> Text.literal(
                "§a✅ Le diste el Claim Tier " + tier + " a " + p.getName().getString()), true);
        }
        return targets.size();
    }

    private static int clear(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
        int n = ClaimManager.getInstance().clearClaimsOf(target.getUuid());
        ctx.getSource().sendFeedback(() -> Text.literal(
            "§a✅ Se eliminaron todos los claims de " + target.getName().getString()
            + " §7(" + n + " zona" + (n == 1 ? "" : "s") + ")"), true);
        return n;
    }

    private static int remove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), p.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("§c❌ No estás dentro de ninguna zona protegida."));
            return 0;
        }
        if (!c.isOwner(p) && !p.hasPermissionLevel(2)) {
            ctx.getSource().sendError(Text.literal("§c❌ Solo el dueño puede eliminar esta zona."));
            return 0;
        }
        BlockPos centre = c.getCenter();
        if (p.getWorld().getBlockState(centre).getBlock() instanceof ClaimBlock) {
            p.getWorld().breakBlock(centre, false, p);
        }
        ClaimManager.getInstance().removeClaim(p.getWorld(), centre);
        // Refund block
        Block b = ModBlocks.forTier(c.getTier());
        if (b != null) {
            ItemStack stack = new ItemStack(b);
            if (!p.getInventory().insertStack(stack)) p.dropItem(stack, false);
        }
        ctx.getSource().sendFeedback(() -> Text.literal(
            "§a✅ Zona eliminada. El bloque fue devuelto a tu inventario."), false);
        return 1;
    }

    private static int menu(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), p.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("§c❌ No estás dentro de ninguna zona protegida."));
            return 0;
        }
        if (!c.isOwner(p) && !p.hasPermissionLevel(2)) {
            ctx.getSource().sendError(Text.literal("§c❌ Solo el dueño puede administrar esta zona."));
            return 0;
        }
        ClaimMenuHandler.open(p, c);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        List<Claim> claims = ClaimManager.getInstance().getClaimsOf(p.getUuid());
        ctx.getSource().sendFeedback(() -> Text.literal(
            "§e📋 Tus zonas (" + claims.size() + "):"), false);
        for (Claim c : claims) {
            ctx.getSource().sendFeedback(() -> Text.literal(
                "§7  - §f[Tier " + c.getTier() + "] en X="
                + c.getX() + " Y=" + c.getY() + " Z=" + c.getZ()
                + " §7— §a" + c.getWorld()), false);
        }
        if (claims.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("§7  (no tienes ninguna)"), false);
        }
        return claims.size();
    }

    private static int info(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), p.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("§c❌ No estás dentro de ninguna zona protegida."));
            return 0;
        }
        int side = c.getRadius() * 2 + 1;
        ctx.getSource().sendFeedback(() -> Text.literal("§e=== Información de la zona ==="), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Dueño: §a" + c.getOwnerName()), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Tier: §b" + c.getTier()
            + " §7(radio " + c.getRadius() + ", " + side + "×" + side + "×" + side + ")"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Coords: §a[" + c.getX() + ", "
            + c.getY() + ", " + c.getZ() + "] §7" + c.getWorld()), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Miembros: §f" + c.getMembers().size()), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Baneados: §f" + c.getBannedPlayers().size()), false);
        ClaimFlags f = c.getFlags();
        ctx.getSource().sendFeedback(() -> Text.literal("§7Flags:"), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Construcción", f.blockBuilding), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Destrucción", f.blockBreaking), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Explosiones", f.blockExplosions), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Fuego", f.blockFire), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Spawn de mobs", f.blockMobSpawn), false);
        ctx.getSource().sendFeedback(() -> formatFlag("PvP", f.blockPVP), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Daño de mobs", f.blockMobDamage), false);
        ctx.getSource().sendFeedback(() -> formatFlag("Alertas", f.trespasserAlerts), false);
        return 1;
    }

    private static Text formatFlag(String name, boolean on) {
        return Text.literal("§7  " + name + ": " + (on ? "§a✅ ON" : "§c❌ OFF"));
    }

    private static int ban(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity executor = ctx.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
        Claim c = ClaimManager.getInstance().getClaimAt(executor.getWorld(), executor.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("§c❌ No estás dentro de ninguna zona protegida."));
            return 0;
        }
        if (!c.isOwner(executor) && !executor.hasPermissionLevel(2)) {
            ctx.getSource().sendError(Text.literal("§c❌ Solo el dueño puede banear de esta zona."));
            return 0;
        }
        c.banPlayer(target.getUuid());
        ClaimManager.getInstance().save();
        ctx.getSource().sendFeedback(() -> Text.literal("§a✅ "
            + target.getName().getString() + " baneado de la zona."), true);
        return 1;
    }

    private static int unban(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity executor = ctx.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "jugador");
        Claim c = ClaimManager.getInstance().getClaimAt(executor.getWorld(), executor.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("§c❌ No estás dentro de ninguna zona protegida."));
            return 0;
        }
        if (!c.isOwner(executor) && !executor.hasPermissionLevel(2)) {
            ctx.getSource().sendError(Text.literal("§c❌ Solo el dueño puede desbanear de esta zona."));
            return 0;
        }
        c.unbanPlayer(target.getUuid());
        ClaimManager.getInstance().save();
        ctx.getSource().sendFeedback(() -> Text.literal("§a✅ "
            + target.getName().getString() + " desbaneado de la zona."), true);
        return 1;
    }
}
