package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

/**
 * All world-protection events live here.  Each handler quickly checks if the
 * acted-on position is inside a claim and either allows / cancels the action.
 */
public class BlockProtectionEvents {
    public static void register() {
        // Block break
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient) return true;
            // ClaimBlock itself is handled in onBreak (so the owner can break it)
            if (state.getBlock() instanceof com.claimblocks.block.ClaimBlock) return true;

            Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
            if (claim == null) return true;
            if (claim.canModify(player)) return true;
            if (claim.getFlags().isBreaking()) return true;
            if (player instanceof ServerPlayerEntity sp) {
                sp.sendMessage(Text.literal("§c[Claim] You cannot break blocks here."), true);
            }
            return false;
        });

        // Block place / right-click on block
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient) return ActionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
            if (claim == null) return ActionResult.PASS;
            if (claim.canModify(player)) return ActionResult.PASS;

            // Allow placement if creeping flag is on
            if (claim.getFlags().isCreeping()) return ActionResult.PASS;

            if (player instanceof ServerPlayerEntity sp) {
                sp.sendMessage(Text.literal("§c[Claim] You cannot interact here."), true);
            }
            return ActionResult.FAIL;
        });

        // Attack block (left click - canceling here prevents griefing chests etc)
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient) return ActionResult.PASS;
            Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
            if (claim == null) return ActionResult.PASS;
            if (world.getBlockState(pos).getBlock() instanceof com.claimblocks.block.ClaimBlock) {
                return ActionResult.PASS;
            }
            if (claim.canModify(player)) return ActionResult.PASS;
            if (claim.getFlags().isBreaking()) return ActionResult.PASS;
            return ActionResult.FAIL;
        });

        // Attack entity - PVP and mob attacking by non-members
        AttackEntityCallback.EVENT.register((player, world, hand, target, hit) -> {
            if (world.isClient) return ActionResult.PASS;
            Claim claim = ClaimManager.getInstance().getClaimAt(world, target.getBlockPos());
            if (claim == null) return ActionResult.PASS;

            // PvP gating: if attacker is a player and target is a player
            if (target instanceof PlayerEntity) {
                if (claim.canModify(player)) return ActionResult.PASS;
                if (!claim.getFlags().isPvp()) {
                    if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("§c[Claim] PvP is disabled here."), true);
                    }
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        // Right-click entities (item frames, armour stands etc)
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient) return ActionResult.PASS;
            Claim claim = ClaimManager.getInstance().getClaimAt(world, entity.getBlockPos());
            if (claim == null) return ActionResult.PASS;
            if (claim.canModify(player)) return ActionResult.PASS;
            // Animals & villagers should also be protected from random non-member interaction
            if (entity instanceof LivingEntity && !(entity instanceof HostileEntity)) {
                if (player instanceof ServerPlayerEntity sp) {
                    sp.sendMessage(Text.literal("§c[Claim] You cannot interact with entities here."), true);
                }
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }

    /**
     * Static check used by other systems (mob-spawning lookups etc.).
     * Returns true if mobs should be allowed to spawn at the given pos.
     */
    public static boolean isMobSpawnAllowed(net.minecraft.world.World world, BlockPos pos) {
        Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
        if (claim == null) return true;
        return claim.getFlags().isMobs();
    }

    public static boolean isExplosionAllowed(net.minecraft.world.World world, BlockPos pos) {
        Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
        if (claim == null) return true;
        return claim.getFlags().isExplosions();
    }
}
