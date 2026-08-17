package com.revivemod.event;

import com.revivemod.state.DownManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class RestrictionEvents {
    private static boolean down(Player player) {
        ServerPlayer sp;
        return player instanceof ServerPlayer && DownManager.isDown(sp = (ServerPlayer)player);
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent e) {
        if (RestrictionEvents.down(e.getPlayer())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e) {
        if (RestrictionEvents.down(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (RestrictionEvents.down(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (RestrictionEvents.down(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent e) {
        if (RestrictionEvents.down(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (RestrictionEvents.down(e.getEntity())) {
            e.setCanceled(true);
        }
    }
}

