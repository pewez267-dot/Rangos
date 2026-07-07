package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.GlobalFlags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class EntityProtectionEvents {
    private static boolean isBypassing(Player player) {
        return player.hasPermissions(2) && ClaimManager.getInstance().isBypassing(player.getUUID());
    }

    private static void deny(Player player, String msg) {
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.RED), true);
        }
    }

    // ------------------------------------------------------------------
    // Hostiles dentro de una proteccion: en vez de quemarlos hasta MATARLOS
    // (lo que hacia que soltaran drops -> granjas), se les da el mismo trato
    // que a un jugador baneado: se les EXPULSA del borde mas cercano, se les
    // quema brevemente y se les hace un daño leve. No se busca matarlos dentro.
    // Ademas, si aun asi mueren por el fuego/entorno dentro de la zona, se
    // cancelan sus drops y su XP (ver onHostileDrops / onHostileXp) para que
    // sea IMPOSIBLE usarlo como granja.
    // ------------------------------------------------------------------
    @SubscribeEvent
    public void onHostileTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide() || !(entity instanceof Enemy)) {
            return;
        }
        if (entity.tickCount % 5 != 0) {
            return;
        }
        Claim claim = ClaimManager.getInstance().getClaimAt(level, entity.blockPosition());
        if (claim != null && claim.getFlags().burnHostiles) {
            EntityProtectionEvents.repelHostile(claim, entity);
        }
    }

    private static void repelHostile(Claim claim, LivingEntity mob) {
        double ex = mob.getX();
        double ez = mob.getZ();
        int r = claim.getRadius();
        double cx = (double) claim.getX() + 0.5;
        double cz = (double) claim.getZ() + 0.5;
        // Distancia a cada borde exterior de la caja de la proteccion.
        double toWest = ex - (cx - (double) r);   // empujar -X
        double toEast = cx + (double) r - ex;      // empujar +X
        double toNorth = ez - (cz - (double) r);   // empujar -Z
        double toSouth = cz + (double) r - ez;     // empujar +Z
        double dirX = 0.0;
        double dirZ = 0.0;
        double min = Math.min(Math.min(toWest, toEast), Math.min(toNorth, toSouth));
        if (min == toWest) {
            dirX = -1.0;
        } else if (min == toEast) {
            dirX = 1.0;
        } else if (min == toNorth) {
            dirZ = -1.0;
        } else {
            dirZ = 1.0;
        }
        // Empujon hacia afuera (~3 bloques) igual que el repel de baneados.
        mob.setDeltaMovement(dirX * 1.1, 0.42, dirZ * 1.1);
        mob.hasImpulse = true;
        mob.hurtMarked = true;
        // Daño/quema "al contacto" como disuasion, NO como ejecucion.
        mob.setSecondsOnFire(3);
        mob.invulnerableTime = 0;
        mob.hurt(mob.damageSources().generic(), 3.0f);
        // MARCA al mob: "tocado por la barrera en este tick". Asi, aunque muera
        // FUERA de la zona (expulsado + quemado) o en un borde vertical, sabemos
        // que su muerte la causo la proteccion y cancelamos sus drops/XP.
        mob.getPersistentData().putLong(BARRIER_TAG, mob.level().getGameTime());
    }

    private static final String BARRIER_TAG = "claimblocks_barrier_tick";
    // Ventana tras el ultimo contacto con la barrera en la que una muerte NO
    // provocada por un jugador se considera "muerte por proteccion" -> sin loot.
    private static final long BARRIER_WINDOW = 200L; // 10 s

    // Una muerte cuenta como "por barrera" si el mob murio dentro de una
    // proteccion con burnHostiles, O si toco la barrera hace poco (fue expulsado
    // y murio del fuego justo despues, posiblemente ya fuera de la zona).
    private static boolean killedByBarrier(LivingEntity entity, Level level) {
        long tick = entity.getPersistentData().getLong(BARRIER_TAG);
        if (tick > 0L && level.getGameTime() - tick <= BARRIER_WINDOW) {
            return true;
        }
        Claim claim = ClaimManager.getInstance().getClaimAt(level, entity.blockPosition());
        return claim != null && claim.getFlags().burnHostiles;
    }

    // Un hostil que murio por la proteccion (fuego/daño de la barrera) y NO por
    // un jugador no suelta NADA de loot -> imposible farmear.
    @SubscribeEvent
    public void onHostileDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide() || !(entity instanceof Enemy)) {
            return;
        }
        DamageSource src = event.getSource();
        if (src != null && src.getEntity() instanceof Player) {
            return; // kill legitimo de un jugador -> conserva drops
        }
        if (EntityProtectionEvents.killedByBarrier(entity, level)) {
            event.setCanceled(true);
        }
    }

    // Igual para la XP: sin XP en muertes por proteccion -> nada de granjas de XP.
    @SubscribeEvent
    public void onHostileXp(LivingExperienceDropEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Enemy)) {
            return;
        }
        if (event.getAttackingPlayer() != null) {
            return; // lo mato un jugador -> conserva XP
        }
        if (EntityProtectionEvents.killedByBarrier(entity, entity.level())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Monster monster) {
            if (monster.tickCount == 0) {
                Claim claim = ClaimManager.getInstance().getClaimAt(event.getLevel(), monster.blockPosition());
                if (claim != null && (claim.getFlags().blockMobSpawn || claim.getFlags().publicMode)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        Claim claim = ClaimManager.getInstance().getClaimAt(level, victim.blockPosition());
        if (victim instanceof Player && attacker instanceof Player && !GlobalFlags.getInstance().globalPVP) {
            EntityProtectionEvents.deny((Player) attacker, "[!] El PVP est\u00e1 desactivado en este servidor.");
            event.setCanceled(true);
            return;
        }
        if (claim == null) {
            return;
        }
        if (victim instanceof Player && attacker instanceof Player) {
            Player pAttacker = (Player) attacker;
            if (EntityProtectionEvents.isBypassing(pAttacker)) {
                return;
            }
            if (claim.getFlags().blockPVP && (!claim.canModify(pAttacker) || !claim.canModify((Player) victim) || claim.getFlags().publicMode)) {
                EntityProtectionEvents.deny(pAttacker, "[!] El PVP est\u00e1 desactivado en esta zona.");
                event.setCanceled(true);
                return;
            }
        }
        if (victim instanceof Player && attacker instanceof LivingEntity && !(attacker instanceof Player) && (claim.getFlags().blockMobDamage || claim.getFlags().publicMode)) {
            event.setCanceled(true);
            return;
        }
        if (victim instanceof Animal && attacker instanceof Player) {
            Player pAttacker = (Player) attacker;
            if (!claim.canModify(pAttacker) && !EntityProtectionEvents.isBypassing(pAttacker) && (claim.getFlags().publicMode || claim.getFlags().blockAnimalKilling)) {
                EntityProtectionEvents.deny(pAttacker, "[!] No puedes matar animales en esta zona.");
                event.setCanceled(true);
                return;
            }
        }
        if (claim.getFlags().blockExplosions && source.is(DamageTypeTags.IS_EXPLOSION)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide() || EntityProtectionEvents.isBypassing(player)) {
            return;
        }
        Entity target = event.getTarget();
        Claim claim = ClaimManager.getInstance().getClaimAt(level, target.blockPosition());
        if (claim != null && !claim.canModify(player) && (claim.getFlags().publicMode || claim.getFlags().blockAnimalKilling || claim.getFlags().blockEntityInteract || claim.getFlags().blockBuilding)) {
            EntityProtectionEvents.deny(player, "[!] No puedes da\u00f1ar entidades aqu\u00ed.");
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        if (level.isClientSide() || EntityProtectionEvents.isBypassing(player)) {
            return;
        }
        Entity target = event.getTarget();
        Claim claim = ClaimManager.getInstance().getClaimAt(level, target.blockPosition());
        if (claim != null && !claim.canModify(player)) {
            if (claim.getFlags().blockAllInteractions) {
                EntityProtectionEvents.deny(player, "[!] No tienes ning\u00fan permiso de interacci\u00f3n en esta zona.");
                event.setCanceled(true);
            } else if (claim.getFlags().blockEntityInteract && target instanceof ItemFrame || !(target instanceof Container)) {
                EntityProtectionEvents.deny(player, "[!] No puedes interactuar con entidades aqu\u00ed.");
                event.setCanceled(true);
            } else {
                EntityProtectionEvents.deny(player, "[!] No puedes abrir este contenedor aqu\u00ed.");
                event.setCanceled(true);
            }
        }
    }
}
