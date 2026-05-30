package com.revivemod.event;

import com.revivemod.state.DownManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * While downed a player can do nothing: no breaking, attacking, using items/blocks or entities.
 * Replaces Fabric's RestrictionHandler (block-break / attack / use callbacks).
 */
public final class RestrictionEvents {

    private static boolean down(Player player) {
        return player instanceof ServerPlayer sp && DownManager.isDown(sp);
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent e) {
        if (down(e.getPlayer())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e) {
        if (down(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (down(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (down(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent e) {
        if (down(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (down(e.getEntity())) {
            e.setCanceled(true);
        }
    }
}
