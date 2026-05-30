package com.revivemod.event;

import com.revivemod.RevivemodForge;
import com.revivemod.config.ReviveConfig;
import com.revivemod.state.DownManager;
import com.revivemod.state.DownState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Grants a short "revive window" to nearby players who right-click a downed player (directly on
 * the entity, or by looking at them and right-clicking a block / using their hand).
 * Replaces Fabric's UseEntityCallback / UseBlockCallback / UseItemCallback revive logic.
 */
public final class InteractionEvents {
    private static final int CLICK_WINDOW = 12;

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer reviver)) {
            return;
        }
        if (event.getTarget() instanceof ServerPlayer downed && DownManager.isDown(downed) && canRevive(reviver)) {
            tryGrant(reviver, downed);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer reviver && canRevive(reviver) && grantNearestTarget(reviver)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer reviver && canRevive(reviver) && grantNearestTarget(reviver)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static boolean canRevive(ServerPlayer reviver) {
        return !reviver.isSpectator() && !DownManager.isDown(reviver);
    }

    private static boolean tryGrant(ServerPlayer reviver, ServerPlayer downed) {
        ReviveConfig cfg = RevivemodForge.getConfig();
        double maxSq = cfg.reviveDistance * cfg.reviveDistance;
        if (reviver.distanceToSqr(downed) > maxSq) {
            reviver.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.literal("Demasiado lejos").withStyle(ChatFormatting.RED)));
            return false;
        }
        DownState st = DownManager.get(downed);
        if (st == null) {
            return false;
        }
        st.reviverWindow.put(reviver.getUUID(), CLICK_WINDOW);
        return true;
    }

    private static boolean grantNearestTarget(ServerPlayer reviver) {
        ReviveConfig cfg = RevivemodForge.getConfig();
        double maxSq = cfg.reviveDistance * cfg.reviveDistance;
        ServerPlayer best = null;
        double bestDot = 0.5;
        Vec3 eye = reviver.getEyePosition();
        Vec3 look = reviver.getViewVector(1.0f);
        for (ServerPlayer p : reviver.serverLevel().players()) {
            if (!DownManager.isDown(p) || p.getUUID().equals(reviver.getUUID()) || reviver.distanceToSqr(p) > maxSq) {
                continue;
            }
            Vec3 aim = p.position().add(0.0, p.getBbHeight() * 0.5, 0.0).subtract(eye);
            double dist = aim.length();
            if (dist < 0.001) {
                best = p;
                break;
            }
            double dot = look.dot(aim.scale(1.0 / dist));
            if (!(dot > bestDot)) {
                continue;
            }
            bestDot = dot;
            best = p;
        }
        if (best == null) {
            return false;
        }
        return tryGrant(reviver, best);
    }
}
