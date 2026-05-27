package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Applies passive perks to owners and members of paid-tier claims:
 *   - effectRegeneration / effectResistance / effectSpeed (every 40 ticks)
 *   - allowFlight (every 20 ticks; toggles player.getAbilities().allowFlying)
 *
 * Intruders never get any effect. Players in creative or spectator are
 * never touched (they manage their own flight via gamemode), so we don't
 * stomp on their abilities.
 *
 * Bypass-mode OPs ARE granted flight inside claims with allowFlight = true,
 * because they're effectively "above" the claim system and the perk would
 * still make sense; if you don't want this, add an isBypassing check.
 */
public final class PassiveEffectsManager {

    private static final int RUN_EVERY_TICKS = 20;
    private static int counter = 0;

    /** Players whose flight WE granted; used to know who to revoke from. */
    private static final Set<UUID> grantedFlight = new HashSet<>();

    private PassiveEffectsManager() {}

    public static void tick(MinecraftServer server) {
        counter++;
        if (counter % RUN_EVERY_TICKS != 0) return;
        boolean runEffects = (counter % 40 == 0); // potion effects every 2s

        for (ServerWorld world : server.getWorlds()) {
            String dim = world.getRegistryKey().getValue().toString();
            for (ServerPlayerEntity player : world.getPlayers()) {
                Claim claim = ClaimManager.getInstance().getClaimAt(world, player.getBlockPos());
                handleFlight(player, claim);
                if (!runEffects) continue;
                applyEffects(player, claim);
            }
        }
    }

    /* -------- flight management -------------------------------------- */

    private static void handleFlight(ServerPlayerEntity player, Claim claim) {
        boolean inClaimWithFlight = claim != null
            && claim.canModify(player)
            && claim.getTier() != null
            && claim.getTier().isPaid()
            && claim.getFlags().allowFlight;
        boolean creativeOrSpectator = player.interactionManager.getGameMode() == GameMode.CREATIVE
            || player.interactionManager.getGameMode() == GameMode.SPECTATOR;

        UUID id = player.getUuid();
        if (inClaimWithFlight) {
            if (!player.getAbilities().allowFlying && !creativeOrSpectator) {
                player.getAbilities().allowFlying = true;
                grantedFlight.add(id);
                player.sendAbilitiesUpdate();
                player.sendMessage(Text.literal("✔ Vuelo activado en esta zona.")
                    .formatted(Formatting.GREEN), true);
            } else if (creativeOrSpectator) {
                grantedFlight.remove(id); // gamemode handles it
            }
        } else if (grantedFlight.contains(id)) {
            // Player left the flight-claim; revoke ours unless gamemode covers it
            if (!creativeOrSpectator) {
                player.getAbilities().allowFlying = false;
                player.getAbilities().flying = false;
                player.sendAbilitiesUpdate();
                player.sendMessage(Text.literal("[i] Saliste de la zona de vuelo.")
                    .formatted(Formatting.AQUA), true);
            }
            grantedFlight.remove(id);
        }
    }

    /* -------- potion effects ----------------------------------------- */

    private static void applyEffects(ServerPlayerEntity player, Claim claim) {
        if (claim == null) return;
        ClaimTier tier = claim.getTier();
        if (tier == null || !tier.isPaid()) return;
        if (!claim.canModify(player)) return;
        if (claim.getFlags().effectRegeneration) {
            player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.REGENERATION, 60, 0, true, false, true));
        }
        if (claim.getFlags().effectResistance) {
            player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE, 60, 0, true, false, true));
        }
        if (claim.getFlags().effectSpeed) {
            player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SPEED, 60, 0, true, false, true));
        }
    }
}
