package com.claimblocks.block;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.gui.ClaimMenuHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The 5 tier blocks all share this implementation, parameterised by tier. */
public class ClaimBlock extends Block {
    private final int tier;

    public ClaimBlock(Settings settings, int tier) {
        super(settings);
        this.tier = tier;
    }

    public int getTier() { return tier; }
    public int getRadius() { return Claim.tierRadius(tier); }

    /** Right-click: open the menu (owner only). */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        Claim claim = ClaimManager.getInstance().getClaimByCenter(world, pos);
        if (claim == null) {
            player.sendMessage(Text.literal("§c❌ Esta piedra no tiene datos de zona asociados."), false);
            return ActionResult.CONSUME;
        }
        if (!claim.isOwner(player) && !player.hasPermissionLevel(2)) {
            player.sendMessage(Text.literal("§c❌ Solo el dueño puede administrar esta zona."), false);
            return ActionResult.CONSUME;
        }
        if (player instanceof ServerPlayerEntity sp) {
            ClaimMenuHandler.open(sp, claim);
        }
        return ActionResult.CONSUME;
    }

    /** Place: register the claim. Reverts and refunds if it would overlap. */
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state,
                         LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (world.isClient) return;
        if (!(placer instanceof PlayerEntity player)) return;

        ClaimManager mgr = ClaimManager.getInstance();
        if (mgr.wouldOverlap(world, pos, getRadius())) {
            world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState());
            if (!player.getAbilities().creativeMode) {
                player.giveItemStack(new ItemStack(this));
            }
            player.sendMessage(Text.literal("§c❌ Esta zona se solaparia con una existente."), true);
            return;
        }
        Claim claim = mgr.createClaim(world, pos, player, tier);
        int side = claim.getRadius() * 2 + 1;
        player.sendMessage(Text.literal("§a✅ Zona creada: §bTier " + tier
            + "§f, area de §d" + side + "x" + side + "x" + side + "§f bloques."), false);
    }

    /** Break: only owner / OP can break; otherwise the break is reverted. */
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            Claim claim = ClaimManager.getInstance().getClaimByCenter(world, pos);
            if (claim != null) {
                if (!claim.isOwner(player) && !player.hasPermissionLevel(2)) {
                    world.setBlockState(pos, state);
                    player.sendMessage(Text.literal("§c❌ Solo el dueño puede romper esta piedra."), true);
                    return state;
                }
                ClaimManager.getInstance().removeClaim(world, pos);
                player.sendMessage(Text.literal("§a✅ Zona eliminada."), false);
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}
