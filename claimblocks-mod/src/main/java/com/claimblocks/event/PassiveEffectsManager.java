package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Applies passive potion effects (Regeneration, Resistance, Speed) to owners
 * and members of paid-tier claims (250x250 / 300x300 / 500x500) when those
 * effect flags are turned on. Intruders never get the effects.
 *
 * Driven by {@code ServerTickEvents.END_SERVER_TICK} from
 * {@link com.claimblocks.ClaimBlocksMod}; the actual work runs every 40 ticks
 * (2 seconds) and applies effects with a 60-tick (3-second) duration so they
 * always overlap. The {@code showParticles=false} argument keeps the effect
 * subtle - no constant particle storm around members.
 */
public final class PassiveEffectsManager {

    private static final int RUN_EVERY_TICKS = 40;
    private static int counter = 0;

    private PassiveEffectsManager() {}

    public static void tick(MinecraftServer server) {
        counter++;
        if (counter % RUN_EVERY_TICKS != 0) return;

        for (ServerWorld world : server.getWorlds()) {
            String dim = world.getRegistryKey().getValue().toString();
            for (Claim claim : ClaimManager.getInstance().getClaimsInWorld(dim)) {
                ClaimTier tier = claim.getTier();
                if (tier == null || !tier.isPaid()) continue;
                boolean regen  = claim.getFlags().effectRegeneration;
                boolean resist = claim.getFlags().effectResistance;
                boolean speed  = claim.getFlags().effectSpeed;
                if (!regen && !resist && !speed) continue;

                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (!claim.contains(player.getBlockPos())) continue;
                    if (!claim.canModify(player)) continue; // only owner/member

                    if (regen) {
                        player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.REGENERATION, 60, 0, true, false, true));
                    }
                    if (resist) {
                        player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.RESISTANCE, 60, 0, true, false, true));
                    }
                    if (speed) {
                        player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.SPEED, 60, 0, true, false, true));
                    }
                }
            }
        }
    }
}
