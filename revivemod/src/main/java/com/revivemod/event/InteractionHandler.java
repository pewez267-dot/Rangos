package com.revivemod.event;

import com.revivemod.ReviveMod;
import com.revivemod.config.ReviveConfig;
import com.revivemod.state.DownManager;
import com.revivemod.state.DownState;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;

/**
 * Reviving requires the ally to actively RIGHT-CLICK (not just look). Each
 * accepted right-click grants a short "click window"; DownTicker only advances
 * the revive while that window is alive, so the ally must keep clicking.
 *
 * To make it forgiving (the lying body has a small visual hitbox), we accept
 * the right-click three ways: directly on the downed player (UseEntity), or on
 * any block / with any item while standing within range and looking toward a
 * downed teammate. Effectively the "hitbox" becomes a generous look-cone within
 * reviveDistance.
 */
public final class InteractionHandler {

    /** Ticks a single right-click keeps the reviver "active". */
    private static final int CLICK_WINDOW = 12;

    private InteractionHandler() {}

    public static void register() {
        // Direct right-click on the downed player.
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity reviver)) return ActionResult.PASS;
            if (entity instanceof ServerPlayerEntity downed && DownManager.isDown(downed)
                    && canRevive(reviver)) {
                tryGrant(reviver, downed);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        // Right-click on a block near the downed teammate.
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (player instanceof ServerPlayerEntity reviver && canRevive(reviver)
                    && grantNearestTarget(reviver)) {
                return ActionResult.FAIL; // consume the click (no block placement)
            }
            return ActionResult.PASS;
        });

        // Right-click with an item near the downed teammate.
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient || hand != Hand.MAIN_HAND)
                return TypedActionResult.pass(player.getStackInHand(hand));
            if (player instanceof ServerPlayerEntity reviver && canRevive(reviver)
                    && grantNearestTarget(reviver)) {
                return TypedActionResult.fail(player.getStackInHand(hand));
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
    }

    private static boolean canRevive(ServerPlayerEntity reviver) {
        return !reviver.isSpectator() && !DownManager.isDown(reviver);
    }

    /** Grant a click window for a specific downed target if in range. */
    private static boolean tryGrant(ServerPlayerEntity reviver, ServerPlayerEntity downed) {
        ReviveConfig cfg = ReviveMod.getConfig();
        double maxSq = cfg.reviveDistance * cfg.reviveDistance;
        if (reviver.squaredDistanceTo(downed) > maxSq) {
            reviver.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                    Text.literal("Demasiado lejos").formatted(Formatting.RED)));
            return false;
        }
        DownState st = DownManager.get(downed);
        if (st == null) return false;
        st.reviverWindow.put(reviver.getUuid(), CLICK_WINDOW);
        return true;
    }

    /** Find the best downed teammate the reviver is looking toward, and grant. */
    private static boolean grantNearestTarget(ServerPlayerEntity reviver) {
        ReviveConfig cfg = ReviveMod.getConfig();
        double maxSq = cfg.reviveDistance * cfg.reviveDistance;
        ServerPlayerEntity best = null;
        double bestDot = 0.5; // minimum alignment
        Vec3d eye = reviver.getEyePos();
        Vec3d look = reviver.getRotationVec(1.0f);

        for (ServerPlayerEntity p : reviver.getServerWorld().getPlayers()) {
            if (!DownManager.isDown(p)) continue;
            if (p.getUuid().equals(reviver.getUuid())) continue;
            if (reviver.squaredDistanceTo(p) > maxSq) continue;
            Vec3d aim = p.getPos().add(0, p.getHeight() * 0.5, 0).subtract(eye);
            double dist = aim.length();
            if (dist < 0.001) { best = p; break; }
            double dot = look.dotProduct(aim.multiply(1.0 / dist));
            if (dot > bestDot) { bestDot = dot; best = p; }
        }
        if (best == null) return false;
        return tryGrant(reviver, best);
    }
}
