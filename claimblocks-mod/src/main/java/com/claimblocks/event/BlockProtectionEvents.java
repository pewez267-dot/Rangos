package com.claimblocks.event;

import com.claimblocks.block.ClaimBlock;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric event handlers for the block-related flags:
 *   - blockBuilding   (place)
 *   - blockBreaking   (break + attack-block)
 *   - blockFire       (extinction sweep)
 *   - container access (always protected)
 */
public final class BlockProtectionEvents {

    private static int fireSweepCounter = 0;

    public static void register() {
        registerBreakEvents();
        registerPlaceAndUseEvents();
    }

    private static void registerBreakEvents() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
            if (world.isClient) return true;
            // Always allow the owner to break their own claim block (handled in ClaimBlock#onBreak)
            if (state.getBlock() instanceof ClaimBlock) return true;

            Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
            if (claim == null) return true;
            if (claim.canModify(player)) return true;
            if (!claim.getFlags().blockBreaking) return true;
            if (player instanceof ServerPlayerEntity sp) {
                sp.sendMessage(Text.literal("§c❌ No tienes permiso para romper bloques aquí."), true);
            }
            return false;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> {
            if (world.isClient) return ActionResult.PASS;
            if (world.getBlockState(pos).getBlock() instanceof ClaimBlock) return ActionResult.PASS;
            Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
            if (claim == null) return ActionResult.PASS;
            if (claim.canModify(player)) return ActionResult.PASS;
            if (!claim.getFlags().blockBreaking) return ActionResult.PASS;
            return ActionResult.FAIL;
        });
    }

    private static void registerPlaceAndUseEvents() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient) return ActionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            BlockPos clickedAdjacent = pos.offset(hit.getSide()); // where a block would be placed
            ItemStack stack = player.getStackInHand(hand);

            // Determine what action this is:
            BlockState clickedState = world.getBlockState(pos);
            Block clickedBlock = clickedState.getBlock();
            boolean clickingClaimBlock = clickedBlock instanceof ClaimBlock;

            // --- Always-protected interaction: containers ---
            if (isProtectedContainer(world, pos)) {
                Claim cc = ClaimManager.getInstance().getClaimAt(world, pos);
                if (cc != null && !cc.canModify(player)) {
                    if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("§c❌ No tienes permiso para usar este bloque aquí."), true);
                    }
                    return ActionResult.FAIL;
                }
            }

            // --- Building flag: prevent placement of new blocks by non-members ---
            if (stack.getItem() instanceof BlockItem bi && !clickingClaimBlock) {
                // The block would land on `clickedAdjacent` (or `pos` if replaceable)
                BlockPos placedAt = clickedState.canReplace(
                    new net.minecraft.item.ItemPlacementContext(player, hand, stack, hit)
                ) ? pos : clickedAdjacent;
                Claim claim = ClaimManager.getInstance().getClaimAt(world, placedAt);
                if (claim != null && !claim.canModify(player) && claim.getFlags().blockBuilding) {
                    if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("§c❌ No tienes permiso para construir aquí."), true);
                    }
                    return ActionResult.FAIL;
                }
            }

            // --- Generic interaction (right-click on funny blocks) blocked by building flag ---
            // Some blocks (buttons, levers, doors, etc.) are interactions, not placements.
            // We don't want non-members toggling redstone in someone's claim.
            if (!clickingClaimBlock && isInteractiveBlock(clickedBlock)) {
                Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
                if (claim != null && !claim.canModify(player) && claim.getFlags().blockBuilding) {
                    if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("§c❌ No tienes permiso para interactuar aquí."), true);
                    }
                    return ActionResult.FAIL;
                }
            }

            return ActionResult.PASS;
        });
    }

    private static boolean isProtectedContainer(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block b = state.getBlock();
        if (b instanceof ChestBlock || b instanceof EnderChestBlock || b instanceof AbstractFurnaceBlock) {
            return true;
        }
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof Inventory;
    }

    /** True for blocks where a right-click is an "interaction" (lever, button, door, ...). */
    private static boolean isInteractiveBlock(Block b) {
        if (b == Blocks.LEVER) return true;
        String n = b.getTranslationKey();
        return n.contains(".button")
            || n.contains(".door")
            || n.contains(".trapdoor")
            || n.contains(".pressure_plate")
            || b == Blocks.NOTE_BLOCK
            || b == Blocks.JUKEBOX
            || b == Blocks.LECTERN
            || b == Blocks.CAKE;
    }

    /**
     * Periodic sweep that extinguishes fire and lava inside any claim that has
     * the {@code blockFire} flag enabled. Cheap because we only scan the
     * immediate neighbourhood of each player who is currently inside a
     * fire-protected claim.
     */
    public static void tickFireSweep(MinecraftServer server) {
        fireSweepCounter++;
        if (fireSweepCounter % 40 != 0) return; // every 2s

        for (ServerWorld world : server.getWorlds()) {
            for (Claim claim : ClaimManager.getInstance().getClaimsInWorld(
                    world.getRegistryKey().getValue().toString())) {
                if (!claim.getFlags().blockFire) continue;
                // For each online player inside this claim, scan a 9-cube around them
                for (ServerPlayerEntity p : world.getPlayers()) {
                    if (!claim.contains(p.getBlockPos())) continue;
                    extinguishAround(world, p.getBlockPos(), claim);
                }
            }
        }
    }

    private static void extinguishAround(ServerWorld world, BlockPos centre, Claim claim) {
        int r = 6;
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    m.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (!claim.contains(m)) continue;
                    BlockState bs = world.getBlockState(m);
                    if (bs.getBlock() == Blocks.FIRE || bs.getBlock() == Blocks.SOUL_FIRE) {
                        world.setBlockState(m.toImmutable(), Blocks.AIR.getDefaultState(), 3);
                    }
                }
            }
        }
    }
}
