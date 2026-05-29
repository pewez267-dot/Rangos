package com.revivemod.event;

import com.revivemod.ReviveMod;
import com.revivemod.config.ReviveConfig;
import com.revivemod.state.DownManager;
import com.revivemod.state.DownState;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

/**
 * Right-click on a downed player to "arm" the revive channel. Multiple players
 * can each arm the same downed player; DownTicker then advances the channel
 * faster the more of them stay in range and keep looking. Looking away or
 * walking off just pauses your contribution (you stay armed and resume
 * automatically when you look back).
 */
public final class InteractionHandler {

    private InteractionHandler() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity reviver)) return ActionResult.PASS;
            if (!(entity instanceof ServerPlayerEntity downed)) return ActionResult.PASS;
            if (!DownManager.isDown(downed)) return ActionResult.PASS;
            if (DownManager.isDown(reviver)) return ActionResult.PASS;
            if (reviver.isSpectator()) return ActionResult.PASS;
            if (reviver.getUuid().equals(downed.getUuid())) return ActionResult.PASS;

            DownState state = DownManager.get(downed);
            if (state == null) return ActionResult.PASS;

            ReviveConfig cfg = ReviveMod.getConfig();
            double maxSq = cfg.reviveDistance * cfg.reviveDistance;
            if (reviver.squaredDistanceTo(downed) > maxSq) {
                reviver.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                        Text.literal("Demasiado lejos para revivir").formatted(Formatting.RED)));
                return ActionResult.FAIL;
            }
            if (!DownTicker.isLookingAt(reviver, downed)) {
                reviver.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                        Text.literal("Mira al jugador para revivirlo").formatted(Formatting.YELLOW)));
                return ActionResult.FAIL;
            }

            // Arm the channel for this reviver (no progress reset — additive).
            if (state.armedRevivers.add(reviver.getUuid())) {
                reviver.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                        Text.literal("Reviviendo a " + downed.getGameProfile().getName() + "...")
                                .formatted(Formatting.GREEN)));
            }

            // Consume the click so it doesn't swap items / open screens / etc.
            return ActionResult.SUCCESS;
        });
    }
}
