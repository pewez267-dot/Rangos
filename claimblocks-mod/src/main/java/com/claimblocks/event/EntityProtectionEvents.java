package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

/**
 * Entity-related flags:
 *   blockMobSpawn       - cancel hostile mob spawning inside protected claims
 *   blockPVP            - cancel PvP damage between players
 *   pvpAll              - allow PvP between everyone (lower priority than blockPVP)
 *   blockMobDamage      - cancel mob damage to players
 *   blockExplosions     - cancel explosion damage to entities
 *   blockEntityInteract - block right-clicking on entities (mounting, trading, ...)
 *   publicMode          - umbrella deny-everything for visitors
 */
public final class EntityProtectionEvents {

    public static void register() {
        registerMobSpawnGuard();
        registerDamageGuards();
        registerInteractionGuard();
    }

    private static void registerMobSpawnGuard() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof HostileEntity)) return;
            if (entity.age != 0) return; // only freshly-spawned natural spawns
            Claim c = ClaimManager.getInstance().getClaimAt(world, entity.getBlockPos());
            if (c == null) return;
            if (c.getFlags().blockMobSpawn || c.getFlags().publicMode) {
                entity.discard();
            }
        });
    }

    private static void registerDamageGuards() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity.getWorld().isClient) return true;
            Claim c = ClaimManager.getInstance().getClaimAt(entity.getWorld(), entity.getBlockPos());
            if (c == null) return true;

            Entity attacker = source.getAttacker();

            // PvP gating: blockPVP has priority over pvpAll
            if (entity instanceof PlayerEntity victim
                && attacker instanceof PlayerEntity aggressor) {
                if (c.getFlags().blockPVP) {
                    if (!c.canModify(aggressor) || !c.canModify(victim) || c.getFlags().publicMode) {
                        if (aggressor instanceof ServerPlayerEntity sp) {
                            sp.sendMessage(Text.literal("[!] El PVP esta desactivado en esta zona."), true);
                        }
                        return false;
                    }
                }
                // pvpAll has no effect here unless we also enabled it; default behaviour
                // is to allow PvP if blockPVP is OFF, so pvpAll just acts as a marker.
                // (If a third-party mod blocks PvP elsewhere, pvpAll cannot override it.)
            }

            // Mob damage to player
            if (entity instanceof PlayerEntity
                && attacker instanceof LivingEntity la
                && !(attacker instanceof PlayerEntity)) {
                if (c.getFlags().blockMobDamage || c.getFlags().publicMode) {
                    return false;
                }
            }

            // Explosion damage
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
            // Protect peaceful entities from non-members (covered by building flag too)
            if (target instanceof AnimalEntity || target instanceof MerchantEntity) {
                if (!c.canModify(player)
                    && (c.getFlags().publicMode || c.getFlags().blockEntityInteract
                        || c.getFlags().blockBuilding)) {
                    if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("[!] No puedes danar entidades aqui."), true);
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

            // Always-protected: chest minecarts, donkeys with chest, chest boats
            boolean isContainerEntity = entity instanceof StorageMinecartEntity
                || entity instanceof ChestBoatEntity
                || entity instanceof AbstractDonkeyEntity;
            if (isContainerEntity) {
                if (player instanceof ServerPlayerEntity sp) {
                    sp.sendMessage(Text.literal("[!] No puedes usar este bloque aqui."), true);
                }
                return ActionResult.FAIL;
            }
            if (c.getFlags().publicMode || c.getFlags().blockEntityInteract) {
                if (player instanceof ServerPlayerEntity sp) {
                    sp.sendMessage(Text.literal("[!] No puedes interactuar con entidades aqui."), true);
                }
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }
}
