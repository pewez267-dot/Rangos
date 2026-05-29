package com.revivemod.event;

import com.revivemod.state.DownManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Hooks into Fabric's ALLOW_DAMAGE / ALLOW_DEATH events.
 *
 * - When a player would die from any source other than the void or our forced
 *   kill, we cancel the death and put them in the downed state instead.
 * - While downed, the player is immune to all damage sources except the void
 *   and our forced kill so their HP can't be drained while waiting to be revived.
 */
public final class DamageHandler {

    private DamageHandler() {}

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!DownManager.isDown(player)) return true;

            // While downed: only let void / generic-kill / our internal forced-kill through.
            if (isLethalAllowed(source)) {
                return true;
            }
            return false; // cancel damage
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;

            // Always allow death when we are force-killing the player (timer expired
            // / /revive kill / etc.). This is the safety guard against the
            // re-knock-down loop.
            if (DownManager.isForceKilling(player.getUuid())) return true;

            // If already downed, only let the "real" death go through (timer / void / forced).
            if (DownManager.isDown(player)) {
                return isLethalAllowed(source);
            }

            // Don't intercept creative / spectator deaths (creative shouldn't die anyway,
            // but be safe).
            if (player.isCreative() || player.isSpectator()) return true;

            // Skip outside-of-world (the void): we let them die for real.
            if (isLethalAllowed(source)) return true;

            // Otherwise: knock them down instead of dying.
            DownManager.knockDown(player, source);
            return false; // cancel real death
        });
    }

    private static boolean isLethalAllowed(DamageSource source) {
        if (source == null) return false;
        return source.isOf(DamageTypes.OUT_OF_WORLD)
                || source.isOf(DamageTypes.GENERIC_KILL);
    }
}
