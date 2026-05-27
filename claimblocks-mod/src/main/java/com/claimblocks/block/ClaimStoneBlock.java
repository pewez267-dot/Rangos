package com.claimblocks.block;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.gui.ClaimMenuHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Base block class for every claim stone. Each placed instance owns a
 * horizontal {@code radius} (X/Z) and a vertical {@code height} (Y, applied
 * symmetrically up and down). Tier metadata is held by {@link ClaimTier};
 * an instance of this block is created once per tier in {@link ModBlocks}.
 *
 * Behaviour:
 *   - place: register a new claim (refunds and cancels if it would overlap)
 *   - right-click: open the GUI menu (owner only)
 *   - break:  remove the claim (owner only)
 */
public class ClaimStoneBlock extends Block {
    private final ClaimTier tier;
    private final int radius;
    private final int height;

    public ClaimStoneBlock(Settings settings, ClaimTier tier) {
        super(settings);
        this.tier = tier;
        this.radius = tier.radius;
        this.height = tier.height;
    }

    public ClaimTier getTier() { return tier; }
    public int getRadius() { return radius; }
    public int getHeight() { return height; }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        Claim claim = ClaimManager.getInstance().getClaimByCenter(world, pos);
        if (claim == null) {
            player.sendMessage(Text.literal("[x] Esta piedra no tiene zona registrada.")
                .formatted(Formatting.RED), false);
            return ActionResult.CONSUME;
        }
        if (!claim.isOwner(player) && !player.hasPermissionLevel(2)) {
            player.sendMessage(Text.literal("[x] Solo el dueño puede administrar esta zona.")
                .formatted(Formatting.RED), false);
            return ActionResult.CONSUME;
        }
        if (player instanceof ServerPlayerEntity sp) {
            ClaimMenuHandler.open(sp, claim, 0);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state,
                         LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (world.isClient) return;
        if (!(placer instanceof PlayerEntity player)) return;

        ClaimManager mgr = ClaimManager.getInstance();
        if (mgr.wouldOverlap(world, pos, radius, height)) {
            world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState());
            if (!player.getAbilities().creativeMode) {
                player.giveItemStack(new ItemStack(this));
            }
            player.sendMessage(Text.literal("[x] Esta zona se solaparía con otra existente.")
                .formatted(Formatting.RED, Formatting.BOLD), true);
            return;
        }
        mgr.createClaim(world, pos, player, tier);
        Text msg = Text.literal("✔ ").formatted(Formatting.GREEN, Formatting.BOLD)
            .append(Text.literal("Zona creada: ").formatted(Formatting.GREEN))
            .append(Text.literal(tier.label()).formatted(Formatting.YELLOW, Formatting.BOLD))
            .append(Text.literal(" bloques | Altura: +/-" + height).formatted(Formatting.GRAY));
        player.sendMessage(msg, false);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            Claim claim = ClaimManager.getInstance().getClaimByCenter(world, pos);
            if (claim != null) {
                if (!claim.isOwner(player) && !player.hasPermissionLevel(2)) {
                    world.setBlockState(pos, state);
                    player.sendMessage(Text.literal("[x] Solo el dueño puede romper esta piedra.")
                        .formatted(Formatting.RED), true);
                    return state;
                }
                ClaimManager.getInstance().removeClaim(world, pos);
                player.sendMessage(Text.literal("✔ Zona eliminada.").formatted(Formatting.GREEN), false);
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}
