package com.claimblocks.event;

import com.claimblocks.block.ClaimStoneBlock;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
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
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Block / item / tile related flag enforcement.
 *
 * Flags handled here:
 *   blockBuilding      - placement of blocks
 *   blockBreaking      - breaking blocks (incl. attack-block)
 *   blockTreeChopping  - breaking minecraft:logs
 *   blockFire          - fire / soul-fire extinction sweep
 *   blockFluids        - placing water / lava buckets
 *   blockItemUse       - right-clicking items in air
 *   publicMode         - blanket "no modifications by visitors"
 *
 * Container blocks (chest / ender / furnace / any inventory tile) are always
 * protected from non-members.
 */
public final class BlockProtectionEvents {

    private static int fireSweepCounter = 0;

    public static void register() {
        registerBreakEvents();
        registerPlaceAndUseEvents();
        registerItemUseEvent();
    }

    /* -------- helpers ----------------------------------------------------- */

    /** True when the action should be cancelled because the player is a "visitor" in the claim. */
    private static boolean denyForVisitor(Claim claim, PlayerEntity player, boolean specificFlag) {
        if (claim.canModify(player)) return false;
        if (claim.getFlags().publicMode) return true;
        return specificFlag;
    }

    /* -------- break events ----------------------------------------------- */

    private static void registerBreakEvents() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
            if (world.isClient) return true;
            if (state.getBlock() instanceof ClaimStoneBlock) return true;

            Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
            if (claim == null) return true;
            if (claim.canModify(player)) return true;

            // Independent tree-chopping flag
            if (state.isIn(BlockTags.LOGS)) {
                if (claim.getFlags().publicMode || claim.getFlags().blockTreeChopping) {
                    if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("[!] No puedes talar arboles en esta zona."), true);
                    }
                    return false;
                }
            }

            if (denyForVisitor(claim, player, claim.getFlags().blockBreaking)) {
                if (player instanceof ServerPlayerEntity sp) {
                    sp.sendMessage(Text.literal("[!] No puedes romper bloques aqui."), true);
                }
                return false;
            }
            return true;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> {
            if (world.isClient) return ActionResult.PASS;
            if (world.getBlockState(pos).getBlock() instanceof ClaimStoneBlock) return ActionResult.PASS;
            Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
            if (claim == null) return ActionResult.PASS;
            if (claim.canModify(player)) return ActionResult.PASS;
            BlockState state = world.getBlockState(pos);
            if (state.isIn(BlockTags.LOGS)
                && (claim.getFlags().publicMode || claim.getFlags().blockTreeChopping)) {
                return ActionResult.FAIL;
            }
            if (denyForVisitor(claim, player, claim.getFlags().blockBreaking)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }

    /* -------- place + use ------------------------------------------------- */

    private static void registerPlaceAndUseEvents() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient) return ActionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            BlockPos placeAt = pos.offset(hit.getSide());
            ItemStack stack = player.getStackInHand(hand);
            BlockState clickedState = world.getBlockState(pos);
            Block clickedBlock = clickedState.getBlock();
            boolean clickingClaimStone = clickedBlock instanceof ClaimStoneBlock;

            // Container access: always protected
            if (isProtectedContainer(world, pos)) {
                Claim cc = ClaimManager.getInstance().getClaimAt(world, pos);
                if (cc != null && !cc.canModify(player)) {
                    if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("[!] No puedes usar este bloque aqui."), true);
                    }
                    return ActionResult.FAIL;
                }
            }

            // Bucket placement (water/lava)
            if (stack.getItem() instanceof BucketItem) {
                Claim claim = ClaimManager.getInstance().getClaimAt(world, placeAt);
                if (claim != null && !claim.canModify(player)) {
                    if (claim.getFlags().publicMode || claim.getFlags().blockFluids
                        || claim.getFlags().blockBuilding) {
                        if (player instanceof ServerPlayerEntity sp) {
                            sp.sendMessage(Text.literal("[!] No puedes colocar fluidos aqui."), true);
                        }
                        return ActionResult.FAIL;
                    }
                }
            }

            // Block item placement (any block being placed)
            if (stack.getItem() instanceof BlockItem && !clickingClaimStone) {
                BlockPos finalPos = clickedState.canReplace(
                    new ItemPlacementContext(player, hand, stack, hit)) ? pos : placeAt;
                Claim claim = ClaimManager.getInstance().getClaimAt(world, finalPos);
                if (claim != null
                    && denyForVisitor(claim, player, claim.getFlags().blockBuilding)) {
                    if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("[!] No puedes construir aqui."), true);
                    }
                    return ActionResult.FAIL;
                }
            }

            // Interactive blocks (lever, button, door, ...) - blocked by building flag
            if (!clickingClaimStone && isInteractiveBlock(clickedBlock)) {
                Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
                if (claim != null
                    && denyForVisitor(claim, player, claim.getFlags().blockBuilding)) {
                    if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("[!] No puedes interactuar aqui."), true);
                    }
                    return ActionResult.FAIL;
                }
            }

            return ActionResult.PASS;
        });
    }

    /* -------- item use (in air) ----------------------------------------- */

    private static void registerItemUseEvent() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient) return TypedActionResult.pass(stack);
            Claim claim = ClaimManager.getInstance().getClaimAt(world, player.getBlockPos());
            if (claim == null) return TypedActionResult.pass(stack);
            if (claim.canModify(player)) return TypedActionResult.pass(stack);
            // Allow "harmless" items (placing one of our own claim stones,
            // empty hand, food in some cases).  We block when blockItemUse or publicMode.
            if (claim.getFlags().publicMode || claim.getFlags().blockItemUse) {
                // Allow placement of claim stones still (they're harmless preview-only)
                if (stack.getItem() instanceof BlockItem bi
                    && bi.getBlock() instanceof ClaimStoneBlock) {
                    return TypedActionResult.pass(stack);
                }
                if (player instanceof ServerPlayerEntity sp) {
                    sp.sendMessage(Text.literal("[!] No puedes usar items en esta zona."), true);
                }
                return TypedActionResult.fail(stack);
            }
            return TypedActionResult.pass(stack);
        });
    }

    /* -------- helpers ---------------------------------------------------- */

    private static boolean isProtectedContainer(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block b = state.getBlock();
        if (b instanceof ChestBlock || b instanceof EnderChestBlock || b instanceof AbstractFurnaceBlock) {
            return true;
        }
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof Inventory;
    }

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

    /* -------- per-tick fire sweep --------------------------------------- */

    public static void tickFireSweep(MinecraftServer server) {
        fireSweepCounter++;
        if (fireSweepCounter % 40 != 0) return;
        for (ServerWorld world : server.getWorlds()) {
            for (Claim claim : ClaimManager.getInstance().getClaimsInWorld(
                    world.getRegistryKey().getValue().toString())) {
                if (!claim.getFlags().blockFire && !claim.getFlags().publicMode) continue;
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
