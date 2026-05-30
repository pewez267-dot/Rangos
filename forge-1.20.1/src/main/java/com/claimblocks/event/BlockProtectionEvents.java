package com.claimblocks.event;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.gui.ClaimMenuHandler;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BlockProtectionEvents {
    private static int fireSweepCounter = 0;

    private static final Set<String> CONTAINER_KEYWORDS = Set.of(
        "backpack", "bag", "satchel", "pouch", "drawer", "crate", "container", "trunk",
        "shulker", "barrel", "chest", "vault", "hopper", "dispenser", "dropper",
        "furnace", "smoker", "blast", "storage", "tank", "silo", "lootr"
    );
    private static final Set<String> CONTAINER_NAMESPACES = Set.of(
        "sophisticatedbackpacks", "sophisticatedstorage", "sophisticatedcore",
        "travelersbackpack", "simplybackpacks", "iron_backpacks", "ironchests",
        "expandedstorage", "functionalstorage", "storagedrawers", "lootr",
        "metalbarrels", "tieredshulkers", "tiered_shulkers"
    );

    private static boolean isBypassing(Player player) {
        return player.hasPermissions(2) && ClaimManager.getInstance().isBypassing(player.getUUID());
    }

    private static boolean denyForVisitor(Claim claim, Player player, boolean specificFlag) {
        if (claim.canModify(player)) return false;
        if (isBypassing(player)) return false;
        if (claim.getFlags().publicMode) return true;
        return specificFlag;
    }

    private static void deny(Player player, String msg) {
        if (player instanceof ServerPlayer sp && !msg.isEmpty()) {
            sp.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.RED), true);
        }
    }

    // ====================== BREAK ======================
    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level world) || world.isClientSide) return;
        Player player = event.getPlayer();
        if (player == null) return;
        if (isBypassing(player)) return;
        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        Claim centerClaim = ClaimManager.getInstance().getClaimByCenter(world, pos);
        if (centerClaim != null) {
            ClaimTier tier = centerClaim.getTier();
            if (tier != null && ClaimBlocks.isClaimConcreteForTier(state.getBlock(), tier)) {
                if (centerClaim.isOwner(player) || player.hasPermissions(2)) {
                    ClaimManager.getInstance().removeClaim(world, pos);
                    if (!player.getAbilities().instabuild) {
                        ItemStack drop = ClaimBlocks.createTierItem(tier, 1);
                        if (!player.getInventory().add(drop)) player.drop(drop, false);
                    }
                    world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (player instanceof ServerPlayer sp) {
                        sp.displayClientMessage(Component.literal("\u2714 Zona eliminada. Piedra devuelta a tu inventario.").withStyle(ChatFormatting.GREEN), false);
                    }
                    return;
                }
                deny(player, "[!] Solo el due\u00f1o puede romper esta piedra.");
                event.setCanceled(true);
                return;
            }
        }

        Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
        if (claim == null || claim.canModify(player)) return;
        if (state.is(BlockTags.LOGS) && (claim.getFlags().publicMode || claim.getFlags().blockTreeChopping)) {
            deny(player, "[!] No puedes talar \u00e1rboles en esta zona.");
            event.setCanceled(true);
            return;
        }
        if (isMatureCrop(state) && (claim.getFlags().publicMode || claim.getFlags().blockCropHarvest)) {
            deny(player, "[!] No puedes cosechar cultivos aqu\u00ed.");
            event.setCanceled(true);
            return;
        }
        if (denyForVisitor(claim, player, claim.getFlags().blockBreaking)) {
            deny(player, "[!] No puedes romper bloques aqu\u00ed.");
            event.setCanceled(true);
        }
    }

    // ====================== PLACE (building) ======================
    @SubscribeEvent
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level world) || world.isClientSide) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (isBypassing(player)) return;
        Claim claim = ClaimManager.getInstance().getClaimAt(world, event.getPos());
        if (claim == null) return;
        if (denyForVisitor(claim, player, claim.getFlags().blockBuilding)) {
            deny(player, "[!] No puedes construir aqu\u00ed.");
            event.setCanceled(true);
        }
    }

    // ====================== RIGHT CLICK BLOCK ======================
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level world = event.getLevel();
        if (world.isClientSide) return;
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        ItemStack stack = event.getItemStack();

        // 1. Click sobre el bloque-centro de un claim
        Claim centerClaim = ClaimManager.getInstance().getClaimByCenter(world, pos);
        if (centerClaim != null) {
            ClaimTier tier = centerClaim.getTier();
            BlockState clickedState = world.getBlockState(pos);
            if (tier != null && ClaimBlocks.isClaimConcreteForTier(clickedState.getBlock(), tier) && !player.isShiftKeyDown()) {
                if (centerClaim.isOwner(player) || player.hasPermissions(2)) {
                    if (player instanceof ServerPlayer sp) ClaimMenuHandler.open(sp, centerClaim, 0);
                } else {
                    deny(player, "[x] Solo el due\u00f1o puede administrar esta zona.");
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }

        // 2. Item con NBT de claim -> colocar zona
        ClaimTier itemTier = ClaimBlocks.readTier(stack);
        if (itemTier != null && !isBypassing(player)) {
            InteractionResult r = tryPlaceClaim(player, world, event.getHand(), event.getFace(), pos, stack, itemTier);
            event.setCanceled(true);
            event.setCancellationResult(r);
            return;
        }

        // 3. Protecciones normales
        InteractionResult r = regularChecks(player, world, pos, event.getFace(), stack);
        if (r != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(r);
        }
    }

    private InteractionResult tryPlaceClaim(Player player, Level world, InteractionHand hand, Direction face, BlockPos clicked, ItemStack stack, ClaimTier tier) {
        BlockState clickedState = world.getBlockState(clicked);
        BlockPos placeAt = clickedState.canBeReplaced() ? clicked : clicked.relative(face);
        BlockState atState = world.getBlockState(placeAt);
        if (!atState.isAir() && !atState.canBeReplaced()) {
            return InteractionResult.PASS;
        }
        ClaimManager mgr = ClaimManager.getInstance();
        Claim ownerOfPlace = mgr.getClaimAt(world, placeAt);
        if (ownerOfPlace != null && !ownerOfPlace.canModify(player) && !player.hasPermissions(2)) {
            deny(player, "[x] No puedes construir en esta zona.");
            return InteractionResult.SUCCESS;
        }
        if (mgr.wouldOverlap(world, placeAt, tier.radius, tier.height)) {
            deny(player, "[x] Esta zona se solapar\u00eda con otra existente.");
            return InteractionResult.SUCCESS;
        }
        int max = ClaimManager.getMaxClaimsPerPlayer();
        if (max > 0 && !player.hasPermissions(2)) {
            int owned = mgr.getClaimsOf(player.getUUID()).size();
            if (owned >= max) {
                deny(player, "[x] Has alcanzado el l\u00edmite de zonas (" + max + ").");
                return InteractionResult.SUCCESS;
            }
        }
        Block block = ClaimBlocks.blockForTier(tier);
        world.setBlockAndUpdate(placeAt, block.defaultBlockState());
        world.playSound(null, placeAt, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
        mgr.createClaim(world, placeAt, player, tier);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.swing(hand);
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal("\u2714 Zona creada: " + tier.label() + " bloques | Altura: +/-" + tier.height).withStyle(ChatFormatting.GREEN), false);
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult regularChecks(Player player, Level world, BlockPos pos, Direction face, ItemStack stack) {
        if (isBypassing(player)) return InteractionResult.PASS;
        BlockPos placeAt = pos.relative(face);
        BlockState clickedState = world.getBlockState(pos);
        Block clickedBlock = clickedState.getBlock();

        Claim cc;
        if (isContainer(world, pos) && (cc = ClaimManager.getInstance().getClaimAt(world, pos)) != null
                && !cc.canModify(player) && (cc.getFlags().publicMode || cc.getFlags().blockChestAccess)) {
            deny(player, "[!] No puedes abrir contenedores aqu\u00ed.");
            return InteractionResult.FAIL;
        }
        Claim claim;
        if (clickedBlock instanceof AnvilBlock && (claim = ClaimManager.getInstance().getClaimAt(world, pos)) != null
                && !claim.canModify(player) && (claim.getFlags().publicMode || claim.getFlags().blockAnvilUse)) {
            deny(player, "[!] No puedes usar yunques aqu\u00ed.");
            return InteractionResult.FAIL;
        }
        if (clickedBlock instanceof SignBlock && (claim = ClaimManager.getInstance().getClaimAt(world, pos)) != null
                && !claim.canModify(player) && (claim.getFlags().publicMode || claim.getFlags().blockSignEditing)) {
            deny(player, "[!] No puedes editar letreros aqu\u00ed.");
            return InteractionResult.FAIL;
        }
        if (stack.getItem() instanceof BucketItem && (claim = ClaimManager.getInstance().getClaimAt(world, placeAt)) != null
                && !claim.canModify(player) && (claim.getFlags().publicMode || claim.getFlags().blockFluids || claim.getFlags().blockBuilding)) {
            deny(player, "[!] No puedes colocar fluidos aqu\u00ed.");
            return InteractionResult.FAIL;
        }
        if (isDoorLike(clickedState) && (claim = ClaimManager.getInstance().getClaimAt(world, pos)) != null
                && denyForVisitor(claim, player, claim.getFlags().blockDoorsAccess)) {
            deny(player, "[!] No puedes usar puertas, botones ni placas aqu\u00ed.");
            return InteractionResult.FAIL;
        }
        if (isInteractiveBlock(clickedState) && (claim = ClaimManager.getInstance().getClaimAt(world, pos)) != null
                && denyForVisitor(claim, player, claim.getFlags().blockBuilding)) {
            deny(player, "[!] No puedes interactuar aqu\u00ed.");
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    // ====================== RIGHT CLICK ITEM ======================
    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Level world = event.getLevel();
        if (world.isClientSide) return;
        Player player = event.getEntity();
        if (isBypassing(player)) return;
        ItemStack stack = event.getItemStack();
        if (ClaimBlocks.readTierId(stack) != null) return;
        Claim claim = ClaimManager.getInstance().getClaimAt(world, player.blockPosition());
        if (claim == null || claim.canModify(player)) return;
        if (claim.getFlags().publicMode || claim.getFlags().blockItemUse) {
            deny(player, "[!] No puedes usar items en esta zona.");
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    // ====================== FARMLAND TRAMPLE ======================
    @SubscribeEvent
    public void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getLevel() instanceof Level world) || world.isClientSide) return;
        Claim claim = ClaimManager.getInstance().getClaimAt(world, event.getPos());
        if (claim == null) return;
        if (claim.getFlags().blockTrampling || claim.getFlags().publicMode) {
            event.setCanceled(true);
        }
    }

    // ====================== EXPLOSION ======================
    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        Level world = event.getLevel();
        if (world.isClientSide) return;
        event.getAffectedBlocks().removeIf(pos -> {
            Claim c = ClaimManager.getInstance().getClaimAt(world, pos);
            return c != null && (c.getFlags().blockExplosions || c.getFlags().publicMode);
        });
    }

    // ====================== PISTON ======================
    @SubscribeEvent
    public void onPiston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof Level world) || world.isClientSide) return;
        BlockPos pistonPos = event.getPos();
        Direction dir = event.getDirection();
        Claim pistonClaim = ClaimManager.getInstance().getClaimAt(world, pistonPos);

        PistonStructureResolver resolver = event.getStructureHelper();
        if (resolver != null && resolver.resolve()) {
            for (BlockPos origin : resolver.getToPush()) {
                BlockPos dest = origin.relative(dir);
                if (crossClaimBlocked(world, pistonClaim, origin, dest)) {
                    event.setCanceled(true);
                    return;
                }
            }
            for (BlockPos origin : resolver.getToDestroy()) {
                if (crossClaimBlocked(world, pistonClaim, origin, origin)) {
                    event.setCanceled(true);
                    return;
                }
            }
        } else {
            BlockPos front = pistonPos.relative(dir);
            if (crossClaimBlocked(world, pistonClaim, front, front.relative(dir))) {
                event.setCanceled(true);
            }
        }
    }

    private static boolean crossClaimBlocked(Level world, Claim pistonClaim, BlockPos origin, BlockPos dest) {
        Claim originClaim = ClaimManager.getInstance().getClaimAt(world, origin);
        Claim destClaim = ClaimManager.getInstance().getClaimAt(world, dest);
        if (sameClaim(originClaim, destClaim) && sameClaim(pistonClaim, originClaim)) return false;
        return protectsBuilding(originClaim) || protectsBuilding(destClaim) || protectsBuilding(pistonClaim);
    }

    private static boolean sameClaim(Claim a, Claim b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.getClaimId().equals(b.getClaimId());
    }

    private static boolean protectsBuilding(Claim c) {
        if (c == null) return false;
        return c.getFlags().publicMode || c.getFlags().blockBuilding;
    }

    // ====================== ENDER PEARL ======================
    @SubscribeEvent
    public void onEnderPearl(EntityTeleportEvent.EnderPearl event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) return;
        Level world = player.level();
        BlockPos dest = BlockPos.containing(event.getTargetX(), event.getTargetY(), event.getTargetZ());
        Claim c = ClaimManager.getInstance().getClaimAt(world, dest);
        if (c == null) return;
        if (c.canModify(player) || isBypassing(player)) return;
        if (c.getFlags().blockEnderPearl || c.getFlags().publicMode) {
            event.setCanceled(true);
            deny(player, "[!] No puedes teletransportarte a esta zona.");
        }
    }

    // ====================== HELPERS ======================
    public static boolean isContainer(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block b = state.getBlock();
        if (b instanceof ChestBlock || b instanceof BarrelBlock || b instanceof ShulkerBoxBlock
                || b instanceof DispenserBlock || b instanceof HopperBlock) return true;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof Container) return true;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
        if (id != null) {
            String ns = id.getNamespace().toLowerCase(java.util.Locale.ROOT);
            String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
            if (CONTAINER_NAMESPACES.contains(ns)) return true;
            for (String kw : CONTAINER_KEYWORDS) {
                if (path.contains(kw) || ns.contains(kw)) return true;
            }
        }
        return false;
    }

    private static boolean isMatureCrop(BlockState state) {
        Block b = state.getBlock();
        if (b instanceof CropBlock) {
            if (state.hasProperty(BlockStateProperties.AGE_7)) return state.getValue(BlockStateProperties.AGE_7) >= 7;
            if (state.hasProperty(BlockStateProperties.AGE_3)) return state.getValue(BlockStateProperties.AGE_3) >= 3;
        }
        if (b instanceof NetherWartBlock) return state.getValue(BlockStateProperties.AGE_3) >= 3;
        if (state.hasProperty(BlockStateProperties.AGE_7) && state.getValue(BlockStateProperties.AGE_7) >= 7) return true;
        if (state.hasProperty(BlockStateProperties.AGE_3) && state.getValue(BlockStateProperties.AGE_3) >= 3) return true;
        return false;
    }

    private static boolean isDoorLike(BlockState state) {
        if (state.is(BlockTags.DOORS)) return true;
        if (state.is(BlockTags.TRAPDOORS)) return true;
        if (state.is(BlockTags.FENCE_GATES)) return true;
        if (state.is(BlockTags.BUTTONS)) return true;
        if (state.getBlock() == Blocks.LEVER) return true;
        return false;
    }

    private static boolean isInteractiveBlock(BlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.CRAFTING_TABLE) return true;
        if (b == Blocks.ENCHANTING_TABLE) return true;
        if (b == Blocks.GRINDSTONE) return true;
        if (b == Blocks.BREWING_STAND) return true;
        return false;
    }

    public static void tickFireSweep(MinecraftServer server) {
        if (++fireSweepCounter % 40 != 0) return;
        for (ServerLevel world : server.getAllLevels()) {
            for (Claim claim : ClaimManager.getInstance().getClaimsInWorld(world.dimension().location().toString())) {
                if (!claim.getFlags().blockFire && !claim.getFlags().publicMode) continue;
                for (ServerPlayer p : world.players()) {
                    if (!claim.contains(p.blockPosition())) continue;
                    extinguishAround(world, p.blockPosition(), claim);
                }
            }
        }
    }

    private static void extinguishAround(ServerLevel world, BlockPos centre, Claim claim) {
        int r = 6;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; ++dx) {
            for (int dy = -r; dy <= r; ++dy) {
                for (int dz = -r; dz <= r; ++dz) {
                    m.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (!claim.contains(m)) continue;
                    Block bb = world.getBlockState(m).getBlock();
                    if (bb == Blocks.FIRE || bb == Blocks.SOUL_FIRE) {
                        world.setBlock(m.immutable(), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
}
