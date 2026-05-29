package com.revivemod.event;

import com.revivemod.ReviveMod;
import com.revivemod.config.ReviveConfig;
import com.revivemod.state.DownManager;
import com.revivemod.state.DownState;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

/**
 * Handles right-clicks on a downed player. Right-click "arms" the revival
 * channel: from that moment on, the {@link DownTicker} will progress the
 * channel as long as the reviver stays within range and keeps looking at the
 * downed player. Looking away or walking out of range cancels the channel.
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

            // Anti-grief: if a different player is already actively reviving (in
            // range), don't let a third party hijack and reset the channel.
            if (state.reviverUuid != null && !state.reviverUuid.equals(reviver.getUuid())) {
                ServerPlayerEntity existing = reviver.getServer().getPlayerManager().getPlayer(state.reviverUuid);
                if (existing != null
                        && existing.isAlive()
                        && !existing.isSpectator()
                        && existing.getServerWorld() == downed.getServerWorld()
                        && existing.squaredDistanceTo(downed) <= maxSq) {
                    reviver.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                            Text.literal("Otro jugador ya esta reviviendo a " + downed.getGameProfile().getName())
                                    .formatted(Formatting.YELLOW)));
                    return ActionResult.FAIL;
                }
            }

            // Make sure the reviver is actually pointing at the body — otherwise
            // arming the channel here would just produce a "tink-tink" pair as
            // the next tick immediately cancels.
            if (!isLookingAt(reviver, downed)) {
                reviver.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                        Text.literal("Mira al jugador para revivirlo").formatted(Formatting.YELLOW)));
                return ActionResult.FAIL;
            }

            // Arm or refresh the channel.
            if (state.reviverUuid == null || !state.reviverUuid.equals(reviver.getUuid())) {
                state.reviverUuid = reviver.getUuid();
                state.reviveProgressTicks = 0;
                state.channelActive = true;

                // Soft "tink" on channel start.
                downed.getServerWorld().playSound(
                        null,
                        downed.getX(), downed.getY(), downed.getZ(),
                        SoundEvents.BLOCK_AMETHYST_BLOCK_HIT,
                        SoundCategory.PLAYERS,
                        0.5f, 1.0f
                );

                reviver.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
                        Text.literal("Reviviendo a " + downed.getGameProfile().getName() + "...")
                                .formatted(Formatting.GREEN)));
            }

            // Consume the click so it doesn't open chat windows / swap items / etc.
            return ActionResult.SUCCESS;
        });
    }

    /** Same look-cone test used by DownTicker (~45 degrees half-angle). */
    private static boolean isLookingAt(ServerPlayerEntity reviver, ServerPlayerEntity downed) {
        net.minecraft.util.math.Vec3d eye = reviver.getEyePos();
        net.minecraft.util.math.Vec3d look = reviver.getRotationVec(1.0f);
        // Aim near head height so standing right next to the body and looking
        // forward still hits the cone.
        net.minecraft.util.math.Vec3d aim = downed.getPos().add(0, downed.getHeight() * 0.85, 0);
        net.minecraft.util.math.Vec3d toAim = aim.subtract(eye);
        double dist = toAim.length();
        if (dist < 0.001) return true;
        double dot = look.dotProduct(toAim.multiply(1.0 / dist));
        return dot > 0.70;
    }
}
