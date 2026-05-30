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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
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

    // ====================== MOB SPAWN ======================
    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (monster.tickCount != 0) return;
        Claim c = ClaimManager.getInstance().getClaimAt(event.getLevel(), monster.blockPosition());
        if (c == null) return;
        if (c.getFlags().blockMobSpawn || c.getFlags().publicMode) {
            event.setCanceled(true);
        }
    }

    // ====================== DAMAGE ======================
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Level world = victim.level();
        if (world.isClientSide) return;
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        Claim c = ClaimManager.getInstance().getClaimAt(world, victim.blockPosition());

        // PVP global desactivado (sin importar zona)
        if (victim instanceof Player && attacker instanceof Player && !GlobalFlags.getInstance().globalPVP) {
            deny((Player) attacker, "[!] El PVP est\u00e1 desactivado en este servidor.");
            event.setCanceled(true);
            return;
        }
        if (c == null) return;

        if (victim instanceof Player && attacker instanceof Player aggressor) {
            if (isBypassing(aggressor)) return;
            if (c.getFlags().blockPVP && (!c.canModify(aggressor) || !c.canModify((Player) victim) || c.getFlags().publicMode)) {
                deny(aggressor, "[!] El PVP est\u00e1 desactivado en esta zona.");
                event.setCanceled(true);
                return;
            }
        }
        if (victim instanceof Player && attacker instanceof LivingEntity && !(attacker instanceof Player)
                && (c.getFlags().blockMobDamage || c.getFlags().publicMode)) {
            event.setCanceled(true);
            return;
        }
        if (victim instanceof Animal && attacker instanceof Player p && !c.canModify(p) && !isBypassing(p)
                && (c.getFlags().publicMode || c.getFlags().blockAnimalKilling)) {
            deny(p, "[!] No puedes matar animales en esta zona.");
            event.setCanceled(true);
            return;
        }
        if (c.getFlags().blockExplosions && source.is(DamageTypeTags.IS_EXPLOSION)) {
            event.setCanceled(true);
        }
    }

    // ====================== ATTACK ENTITY ======================
    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Level world = player.level();
        if (world.isClientSide) return;
        if (isBypassing(player)) return;
        Entity target = event.getTarget();
        Claim c = ClaimManager.getInstance().getClaimAt(world, target.blockPosition());
        if (c == null) return;
        if ((target instanceof Animal || target instanceof AbstractVillager) && !c.canModify(player)
                && (c.getFlags().publicMode || c.getFlags().blockAnimalKilling || c.getFlags().blockEntityInteract || c.getFlags().blockBuilding)) {
            deny(player, "[!] No puedes da\u00f1ar entidades aqu\u00ed.");
            event.setCanceled(true);
        }
    }

    // ====================== ENTITY INTERACT ======================
    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Level world = event.getLevel();
        if (world.isClientSide) return;
        Player player = event.getEntity();
        if (isBypassing(player)) return;
        Entity target = event.getTarget();
        Claim c = ClaimManager.getInstance().getClaimAt(world, target.blockPosition());
        if (c == null || c.canModify(player)) return;
        boolean isContainerEntity = target instanceof Container;
        if (isContainerEntity && (c.getFlags().publicMode || c.getFlags().blockChestAccess)) {
            deny(player, "[!] No puedes abrir este contenedor aqu\u00ed.");
            event.setCanceled(true);
            return;
        }
        if (c.getFlags().publicMode || c.getFlags().blockEntityInteract) {
            deny(player, "[!] No puedes interactuar con entidades aqu\u00ed.");
            event.setCanceled(true);
        }
    }
}
