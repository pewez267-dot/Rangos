package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.GlobalFlags;
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
import net.minecraft.util.Formatting;

/**
 * Entity-related flags:
 *   blockMobSpawn       - cancel hostile mob spawning inside protected claims
 *   blockPVP            - cancel PvP damage between players
 *   pvpAll              - allow PvP between everyone (lower priority than blockPVP)
 *   blockMobDamage      - cancel mob damage to players
 *   blockExplosions     - cancel explosion damage to entities
 *   blockEntityInteract - block right-clicking on entities (mounting, trading, ...)
 *   blockAnimalKilling  - block intruders from killing peaceful animals
 *   blockChestAccess    - chest minecarts / donkeys / chest boats
 *   publicMode          - umbrella deny-everything for visitors
 *   globalPVP           - server-wide pvp toggle when no claim is involved
 */
public final class EntityProtectionEvents {

    public static void register() {
        registerMobSpawnGuard();
        registerDamageGuards();
        registerInteractionGuard();
    }

    private static boolean isBypassing(PlayerEntity player) {
        return player.hasPermissionLevel(2)
            && ClaimManager.getInstance().isBypassing(player.getUuid());
    }

    private static void registerMobSpawnGuard() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof HostileEntity)) return;
            if (entity.age != 0) return;
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
            Entity attacker = source.getAttacker();
            Claim c = ClaimManager.getInstance().getClaimAt(entity.getWorld(), entity.getBlockPos());

            // Server-wide PvP toggle (only outside claims)
            if (c == null
                && entity instanceof PlayerEntity
                && attacker instanceof PlayerEntity
                && !GlobalFlags.getInstance().globalPVP) {
                if (attacker instanceof ServerPlayerEntity sp) {
                    sp.sendMessage(Text.literal("[!] El PVP está desactivado en este servidor.")
                        .formatted(Formatting.RED), true);
                }
                return false;
            }

            if (c == null) return true;

            // PvP gating inside claim
            if (entity instanceof PlayerEntity victim
                && attacker instanceof PlayerEntity aggressor) {
                if (isBypassing(aggressor)) return true;
                if (c.getFlags().blockPVP) {
                    if (!c.canModify(aggressor) || !c.canModify(victim) || c.getFlags().publicMode) {
                        if (aggressor instanceof ServerPlayerEntity sp) {
                            sp.sendMessage(Text.literal("[!] El PVP está desactivado en esta zona.")
                                .formatted(Formatting.RED), true);
                        }
                        return false;
                    }
                }
            }

            // Mob damage to player
            if (entity instanceof PlayerEntity
                && attacker instanceof LivingEntity
                && !(attacker instanceof PlayerEntity)) {
                if (c.getFlags().blockMobDamage || c.getFlags().publicMode) {
                    return false;
                }
            }

            // Block intruders killing peaceful animals (animals are LivingEntity targets)
            if (entity instanceof AnimalEntity
                && attacker instanceof PlayerEntity p
                && !c.canModify(p) && !isBypassing(p)
                && (c.getFlags().publicMode || c.getFlags().blockAnimalKilling)) {
                if (p instanceof ServerPlayerEntity sp) {
                    sp.sendMessage(Text.literal("[!] No puedes matar animales en esta zona.")
                        .formatted(Formatting.RED), true);
                }
                return false;
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
            if (isBypassing(player)) return ActionResult.PASS;
            Claim c = ClaimManager.getInstance().getClaimAt(world, target.getBlockPos());
            if (c == null) return ActionResult.PASS;
            if (target instanceof AnimalEntity || target instanceof MerchantEntity) {
                if (!c.canModify(player)
                    && (c.getFlags().publicMode
                        || c.getFlags().blockAnimalKilling
                        || c.getFlags().blockEntityInteract
                        || c.getFlags().blockBuilding)) {
                    if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(Text.literal("[!] No puedes dañar entidades aquí.")
                            .formatted(Formatting.RED), true);
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
            if (isBypassing(player)) return ActionResult.PASS;
            Claim c = ClaimManager.getInstance().getClaimAt(world, entity.getBlockPos());
            if (c == null) return ActionResult.PASS;
            if (c.canModify(player)) return ActionResult.PASS;

            // Container entities: chest minecarts, donkeys with chest, chest boats
            boolean isContainerEntity = entity instanceof StorageMinecartEntity
                || entity instanceof ChestBoatEntity
                || entity instanceof AbstractDonkeyEntity;
            if (isContainerEntity
                && (c.getFlags().publicMode || c.getFlags().blockChestAccess)) {
                if (player instanceof ServerPlayerEntity sp) {
                    sp.sendMessage(Text.literal("[!] No puedes abrir este contenedor aquí.")
                        .formatted(Formatting.RED), true);
                }
                return ActionResult.FAIL;
            }
            if (c.getFlags().publicMode || c.getFlags().blockEntityInteract) {
                if (player instanceof ServerPlayerEntity sp) {
                    sp.sendMessage(Text.literal("[!] No puedes interactuar con entidades aquí.")
                        .formatted(Formatting.RED), true);
                }
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }
}
