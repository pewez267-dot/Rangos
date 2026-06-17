package com.claimblocks.block;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.gui.ClaimMenuScreen;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * The protection block. Five tier subclass instances are created in
 * {@link ModBlocks}; each tier has a different protection radius.
 */
public class ClaimBlock extends Block {
    private final int tier;
    private final int protectionRadius;

    public ClaimBlock(Settings settings, int tier, int protectionRadius) {
        super(settings);
        this.tier = tier;
        this.protectionRadius = protectionRadius;
    }

    public int getTier() { return tier; }
    public int getProtectionRadius() { return protectionRadius; }

    /** Right-click: open menu if owner / member / OP, otherwise show info. */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        Claim claim = ClaimManager.getInstance().getClaimByCenter(world, pos);
        if (claim == null) {
            // Orphan block - happens after upgrade or data loss
            player.sendMessage(Text.literal("§cThis block has no associated claim data."), false);
            return ActionResult.CONSUME;
        }
        if (claim.canModify(player) && player instanceof ServerPlayerEntity sp) {
            ClaimMenuScreen.open(sp, claim);
        } else {
            player.sendMessage(Text.literal("§e[Claim] §fOwner: §a" + claim.getOwnerName()
                + "§f, Tier: §b" + claim.getTier()
                + "§f, Area: §d" + (claim.getRadius() * 2 + 1) + "x"
                + (claim.getRadius() * 2 + 1) + "x" + (claim.getRadius() * 2 + 1)), false);
        }
        return ActionResult.CONSUME;
    }

    /** Place: register the claim in the manager. */
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (world.isClient) return;
        if (!(placer instanceof PlayerEntity player)) return;

        ClaimManager mgr = ClaimManager.getInstance();
        if (mgr.wouldOverlap(world, pos, protectionRadius)) {
            // Refuse: cancel placement and refund the item
            world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState());
            world.emitGameEvent(player, GameEvent.BLOCK_DESTROY, pos);
            if (!player.getAbilities().creativeMode) {
                player.giveItemStack(new ItemStack(this));
            }
            player.sendMessage(Text.literal("§cThis claim would overlap with an existing one!"), true);
            return;
        }
        mgr.createClaim(world, pos, player, tier, protectionRadius);
        if (world instanceof ServerWorld) {
            mgr.saveClaims(((ServerWorld) world).getServer());
        }
        player.sendMessage(Text.literal("§a[Claim] §fCreated Tier §b" + tier
            + "§f claim with area §d" + (protectionRadius * 2 + 1) + "x"
            + (protectionRadius * 2 + 1) + "x" + (protectionRadius * 2 + 1)), false);
    }

    /** Break: only the owner / OPs may break, otherwise the break is reverted. */
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            Claim claim = ClaimManager.getInstance().getClaimByCenter(world, pos);
            if (claim != null) {
                if (!claim.isOwner(player) && !player.hasPermissionLevel(2)) {
                    // Cancel break
                    world.setBlockState(pos, state);
                    if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("§cYou are not the owner of this claim!"), true);
                    }
                    return state;
                }
                ClaimManager.getInstance().removeClaim(world, pos);
                if (world instanceof ServerWorld) {
                    ClaimManager.getInstance().saveClaims(((ServerWorld) world).getServer());
                }
                player.sendMessage(Text.literal("§a[Claim] §fClaim removed."), false);
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}
