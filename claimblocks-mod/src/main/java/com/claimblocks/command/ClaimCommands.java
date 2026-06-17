package com.claimblocks.command;

import com.claimblocks.block.ClaimBlock;
import com.claimblocks.block.ModBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.gui.ClaimMenuScreen;
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
 * Commands: /claim give|clear|remove|menu|list|info  (all OP-only)
 */
public class ClaimCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("claim")
                .requires(src -> src.hasPermissionLevel(2))
                // /claim give <player> <tier>
                .then(CommandManager.literal("give")
                    .then(CommandManager.argument("player", EntityArgumentType.players())
                        .then(CommandManager.argument("tier", IntegerArgumentType.integer(1, 5))
                            .executes(ClaimCommands::give))))
                // /claim clear <player>
                .then(CommandManager.literal("clear")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ClaimCommands::clear)))
                // /claim remove
                .then(CommandManager.literal("remove")
                    .executes(ClaimCommands::remove))
                // /claim menu
                .then(CommandManager.literal("menu")
                    .executes(ClaimCommands::menu))
                // /claim list
                .then(CommandManager.literal("list")
                    .executes(ClaimCommands::list))
                // /claim info
                .then(CommandManager.literal("info")
                    .executes(ClaimCommands::info))
            );
        });
    }

    private static int give(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "player");
        int tier = IntegerArgumentType.getInteger(ctx, "tier");
        Block block = ModBlocks.getBlockForTier(tier);
        if (block == null) {
            ctx.getSource().sendError(Text.literal("Invalid tier: " + tier));
            return 0;
        }
        ItemStack stack = new ItemStack(block.asItem());
        for (ServerPlayerEntity p : targets) {
            ItemStack copy = stack.copy();
            if (!p.getInventory().insertStack(copy)) {
                p.dropItem(copy, false);
            }
            p.sendMessage(Text.literal("§a[Claim] §fYou received a Tier " + tier + " Claim Block."), false);
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§aGave Tier " + tier + " Claim Block to " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static int clear(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        int removed = ClaimManager.getInstance().clearClaimsOfPlayer(ctx.getSource().getServer(), target.getUuid());
        ClaimManager.getInstance().saveClaims(ctx.getSource().getServer());
        ctx.getSource().sendFeedback(() -> Text.literal("§aRemoved " + removed + " claim(s) of " + target.getName().getString()), true);
        return removed;
    }

    private static int remove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        BlockPos pos = p.getBlockPos();
        Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), pos);
        if (c == null) {
            ctx.getSource().sendError(Text.literal("You are not standing inside any claim."));
            return 0;
        }
        if (!c.isOwner(p) && !p.hasPermissionLevel(2)) {
            ctx.getSource().sendError(Text.literal("You do not own this claim."));
            return 0;
        }
        BlockPos center = c.getCenter();
        // Break the actual claim block so the owner gets the item back
        if (p.getWorld().getBlockState(center).getBlock() instanceof ClaimBlock) {
            p.getWorld().breakBlock(center, true, p);
        } else {
            // No block? Just remove the data
            ClaimManager.getInstance().removeClaim(p.getWorld(), center);
            ClaimManager.getInstance().saveClaims(ctx.getSource().getServer());
        }
        ctx.getSource().sendFeedback(() -> Text.literal("§aClaim removed."), true);
        return 1;
    }

    private static int menu(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), p.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("You are not standing inside any claim."));
            return 0;
        }
        if (!c.canModify(p)) {
            ctx.getSource().sendError(Text.literal("You don't have permission to manage this claim."));
            return 0;
        }
        ClaimMenuScreen.open(p, c);
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        List<Claim> claims;
        if (p.hasPermissionLevel(2)) {
            claims = ClaimManager.getInstance().getAllClaims();
            ctx.getSource().sendFeedback(() -> Text.literal("§e[Claims - all] §fTotal: " + claims.size()), false);
        } else {
            claims = ClaimManager.getInstance().getClaimsOfPlayer(p.getUuid());
            ctx.getSource().sendFeedback(() -> Text.literal("§e[Your claims] §fTotal: " + claims.size()), false);
        }
        for (Claim c : claims) {
            BlockPos pos = c.getCenter();
            ctx.getSource().sendFeedback(() -> Text.literal("  §7- §fT" + c.getTier()
                + " §a[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "] §7" + c.getDimension()
                + " §fowner: §b" + c.getOwnerName()), false);
        }
        return claims.size();
    }

    private static int info(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
        Claim c = ClaimManager.getInstance().getClaimAt(p.getWorld(), p.getBlockPos());
        if (c == null) {
            ctx.getSource().sendError(Text.literal("You are not standing inside any claim."));
            return 0;
        }
        BlockPos pos = c.getCenter();
        int side = c.getRadius() * 2 + 1;
        ctx.getSource().sendFeedback(() -> Text.literal("§e=== Claim Info ==="), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Owner: §a" + c.getOwnerName()), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Tier: §b" + c.getTier()), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Center: §a[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Area: §d" + side + "x" + side + "x" + side), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Members: §f" + c.getMembers().size()), false);
        return 1;
    }
}
