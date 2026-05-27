package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

/**
 * Entity-related flags:
 *   - blockMobSpawn   (cancel hostile mob spawning inside claims)
 *   - blockPVP        (cancel PvP damage)
 *   - blockMobDamage  (cancel mob damage to players)
 *   - container interaction with chest minecarts / donkeys
 */
public final class EntityProtectionEvents {

    public static void register() {
        registerMobSpawnGuard();
        registerDamageGuards();
        registerInteractionGuard();
    }

    /**
     * When a hostile entity is just loaded into a server world, check if it is
     * inside a claim with {@code blockMobSpawn} enabled and discard it.
     * This catches natural spawns since we discard before they fully integrate.
     * Loaded chunk entities have {@code age > 0} so they're left alone.
     */
    private static void registerMobSpawnGuard() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof HostileEntity)) return;
            // Heuristic: only freshly-spawned (age 0) entities count as "natural spawns"
            // for our cancel-on-load.
            if (entity.age != 0) return;
            Claim c = ClaimManager.getInstance().getClaimAt(world, entity.getBlockPos());
            if (c != null && c.getFlags().blockMobSpawn) {
                entity.discard();
            }
        });
    }

    private static void registerDamageGuards() {
        // Cancel damage that's denied by flags
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity.getWorld().isClient) return true;
            Claim c = ClaimManager.getInstance().getClaimAt(entity.getWorld(), entity.getBlockPos());
            if (c == null) return true;

            Entity attacker = source.getAttacker();

            // Block PvP: attacker is player, victim is player, both inside same claim, flag set
            if (entity instanceof PlayerEntity victim
                && attacker instanceof PlayerEntity aggressor
                && c.getFlags().blockPVP) {
                if (!c.canModify(aggressor) || !c.canModify(victim)) {
                    if (aggressor instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("§c❌ El PvP está desactivado en esta zona."), true);
                    }
                    return false;
                }
            }

            // Block mob damage: attacker is mob, victim is player, flag set
            if (entity instanceof PlayerEntity
                && attacker instanceof LivingEntity la
                && !(attacker instanceof PlayerEntity)
                && c.getFlags().blockMobDamage) {
                return false;
            }

            // Block explosion damage to entities inside protected claims
            if (c.getFlags().blockExplosions
                && (source.isOf(DamageTypes.EXPLOSION) || source.isOf(DamageTypes.PLAYER_EXPLOSION))) {
                return false;
            }

            return true;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, target, hit) -> {
            if (world.isClient) return ActionResult.PASS;
            Claim c = ClaimManager.getInstance().getClaimAt(world, target.getBlockPos());
            if (c == null) return ActionResult.PASS;
            // Also protect peaceful entities (animals/villagers) from non-members
            if (target instanceof AnimalEntity || target instanceof MerchantEntity) {
                if (!c.canModify(player) && c.getFlags().blockBuilding) {
                    if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("§c❌ No tienes permiso para dañar entidades aquí."), true);
                    }
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });
    }

    private static void registerInteractionGuard() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient) return ActionResult.PASS;
            Claim c = ClaimManager.getInstance().getClaimAt(world, entity.getBlockPos());
            if (c == null) return ActionResult.PASS;
            if (c.canModify(player)) return ActionResult.PASS;

            // Always-protected: chest minecarts, donkeys with chest, chest boats, etc.
            boolean isContainerEntity = entity instanceof StorageMinecartEntity
                || entity instanceof ChestBoatEntity
                || entity instanceof AbstractDonkeyEntity;
            if (isContainerEntity) {
                if (player instanceof ServerPlayerEntity sp) {
                    sp.sendMessage(Text.literal("§c❌ No tienes permiso para usar este bloque aquí."), true);
                }
                return ActionResult.FAIL;
            }
            // Other entity right-click protected by building flag
            if (c.getFlags().blockBuilding) {
                if (player instanceof ServerPlayerEntity sp) {
                    sp.sendMessage(Text.literal("§c❌ No tienes permiso para interactuar con entidades aquí."), true);
                }
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }
}
