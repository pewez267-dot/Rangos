/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
 *  net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
 *  net.fabricmc.fabric.api.event.player.AttackEntityCallback
 *  net.fabricmc.fabric.api.event.player.UseEntityCallback
 *  net.minecraft.class_124
 *  net.minecraft.class_1269
 *  net.minecraft.class_1297
 *  net.minecraft.class_1309
 *  net.minecraft.class_1429
 *  net.minecraft.class_1492
 *  net.minecraft.class_1588
 *  net.minecraft.class_1657
 *  net.minecraft.class_1693
 *  net.minecraft.class_1937
 *  net.minecraft.class_2561
 *  net.minecraft.class_3222
 *  net.minecraft.class_3988
 *  net.minecraft.class_7264
 *  net.minecraft.class_8111
 */
package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.GlobalFlags;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.class_124;
import net.minecraft.class_1269;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1429;
import net.minecraft.class_1492;
import net.minecraft.class_1588;
import net.minecraft.class_1657;
import net.minecraft.class_1693;
import net.minecraft.class_1937;
import net.minecraft.class_2561;
import net.minecraft.class_3222;
import net.minecraft.class_3988;
import net.minecraft.class_7264;
import net.minecraft.class_8111;

public final class EntityProtectionEvents {
    public static void register() {
        EntityProtectionEvents.registerMobSpawnGuard();
        EntityProtectionEvents.registerDamageGuards();
        EntityProtectionEvents.registerInteractionGuard();
    }

    private static boolean isBypassing(class_1657 player) {
        return player.method_5687(2) && ClaimManager.getInstance().isBypassing(player.method_5667());
    }

    private static void registerMobSpawnGuard() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof class_1588)) {
                return;
            }
            if (entity.field_6012 != 0) {
                return;
            }
            Claim c = ClaimManager.getInstance().getClaimAt((class_1937)world, entity.method_24515());
            if (c == null) {
                return;
            }
            if (c.getFlags().blockMobSpawn || c.getFlags().publicMode) {
                entity.method_31472();
            }
        });
    }

    private static void registerDamageGuards() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            class_1657 p;
            if (entity.method_37908().field_9236) {
                return true;
            }
            class_1297 attacker = source.method_5529();
            Claim c = ClaimManager.getInstance().getClaimAt(entity.method_37908(), entity.method_24515());
            if (c == null && entity instanceof class_1657 && attacker instanceof class_1657 && !GlobalFlags.getInstance().globalPVP) {
                if (attacker instanceof class_3222) {
                    class_3222 sp = (class_3222)attacker;
                    sp.method_7353((class_2561)class_2561.method_43470((String)"[!] El PVP est\u00e1 desactivado en este servidor.").method_27692(class_124.field_1061), true);
                }
                return false;
            }
            if (c == null) {
                return true;
            }
            if (entity instanceof class_1657) {
                class_1657 victim = (class_1657)entity;
                if (attacker instanceof class_1657) {
                    class_1657 aggressor = (class_1657)attacker;
                    if (EntityProtectionEvents.isBypassing(aggressor)) {
                        return true;
                    }
                    if (c.getFlags().blockPVP && (!c.canModify(aggressor) || !c.canModify(victim) || c.getFlags().publicMode)) {
                        if (aggressor instanceof class_3222) {
                            class_3222 sp = (class_3222)aggressor;
                            sp.method_7353((class_2561)class_2561.method_43470((String)"[!] El PVP est\u00e1 desactivado en esta zona.").method_27692(class_124.field_1061), true);
                        }
                        return false;
                    }
                }
            }
            if (entity instanceof class_1657 && attacker instanceof class_1309 && !(attacker instanceof class_1657) && (c.getFlags().blockMobDamage || c.getFlags().publicMode)) {
                return false;
            }
            if (entity instanceof class_1429 && attacker instanceof class_1657 && !c.canModify(p = (class_1657)attacker) && !EntityProtectionEvents.isBypassing(p) && (c.getFlags().publicMode || c.getFlags().blockAnimalKilling)) {
                if (p instanceof class_3222) {
                    class_3222 sp = (class_3222)p;
                    sp.method_7353((class_2561)class_2561.method_43470((String)"[!] No puedes matar animales en esta zona.").method_27692(class_124.field_1061), true);
                }
                return false;
            }
            return !c.getFlags().blockExplosions || !source.method_49708(class_8111.field_42331) && !source.method_49708(class_8111.field_42332);
        });
        AttackEntityCallback.EVENT.register((player, world, hand, target, hit) -> {
            if (world.field_9236) {
                return class_1269.field_5811;
            }
            if (EntityProtectionEvents.isBypassing(player)) {
                return class_1269.field_5811;
            }
            Claim c = ClaimManager.getInstance().getClaimAt(world, target.method_24515());
            if (c == null) {
                return class_1269.field_5811;
            }
            if ((target instanceof class_1429 || target instanceof class_3988) && !c.canModify(player) && (c.getFlags().publicMode || c.getFlags().blockAnimalKilling || c.getFlags().blockEntityInteract || c.getFlags().blockBuilding)) {
                if (player instanceof class_3222) {
                    class_3222 sp = (class_3222)player;
                    sp.method_7353((class_2561)class_2561.method_43470((String)"[!] No puedes da\u00f1ar entidades aqu\u00ed.").method_27692(class_124.field_1061), true);
                }
                return class_1269.field_5814;
            }
            return class_1269.field_5811;
        });
    }

    private static void registerInteractionGuard() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            boolean isContainerEntity;
            if (world.field_9236) {
                return class_1269.field_5811;
            }
            if (EntityProtectionEvents.isBypassing(player)) {
                return class_1269.field_5811;
            }
            Claim c = ClaimManager.getInstance().getClaimAt(world, entity.method_24515());
            if (c == null) {
                return class_1269.field_5811;
            }
            if (c.canModify(player)) {
                return class_1269.field_5811;
            }
            boolean bl = isContainerEntity = entity instanceof class_1693 || entity instanceof class_7264 || entity instanceof class_1492;
            if (isContainerEntity && (c.getFlags().publicMode || c.getFlags().blockChestAccess)) {
                if (player instanceof class_3222) {
                    class_3222 sp = (class_3222)player;
                    sp.method_7353((class_2561)class_2561.method_43470((String)"[!] No puedes abrir este contenedor aqu\u00ed.").method_27692(class_124.field_1061), true);
                }
                return class_1269.field_5814;
            }
            if (c.getFlags().publicMode || c.getFlags().blockEntityInteract) {
                if (player instanceof class_3222) {
                    class_3222 sp = (class_3222)player;
                    sp.method_7353((class_2561)class_2561.method_43470((String)"[!] No puedes interactuar con entidades aqu\u00ed.").method_27692(class_124.field_1061), true);
                }
                return class_1269.field_5814;
            }
            return class_1269.field_5811;
        });
    }
}

