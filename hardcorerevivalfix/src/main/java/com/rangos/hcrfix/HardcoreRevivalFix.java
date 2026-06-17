package com.rangos.hcrfix;

import net.blay09.mods.hardcorerevival.PlayerHardcoreRevivalManager;
import net.blay09.mods.hardcorerevival.handler.KnockoutSyncHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side entrypoint for hardcorerevivalfix.
 *
 * Two responsibilities:
 *   1) Fix the rescue bug: when a knocked-out player teleports (e.g. via /tpa)
 *      into another player's view, that other player never receives the
 *      HardcoreRevivalDataMessage, so on his client the target is not flagged
 *      as knocked out and right-click revive does nothing. We patch this by
 *      re-syncing whenever an entity tracking session begins.
 *
 *   2) Enforce a mandatory client version check, modeled after BlockPops:
 *      register an S2C payload type whose mere presence on the client (i.e.
 *      a registered receiver) proves the client has this companion mod
 *      installed. Players without it get kicked at join time.
 */
public class HardcoreRevivalFix implements ModInitializer {

    public static final String MOD_ID = "hardcorerevivalfix";
    public static final String REQUIRED_VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Register the version-check payload as an S2C type. The codec is
        // unit-empty - we never actually send the packet; we only probe
        // whether the client has a registered receiver for this ID.
        try {
            PayloadTypeRegistry.playS2C().register(
                    VersionCheckPayload.TYPE, VersionCheckPayload.CODEC);
        } catch (Throwable t) {
            LOGGER.warn("[hardcorerevivalfix] could not register version-check S2C type: {}", t.toString());
        }

        // ---- Bug fix: resync revival data on entity tracking start. ----
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, trackingPlayer) -> {
            if (!(trackedEntity instanceof Player target)) {
                return;
            }
            if (target == trackingPlayer) {
                return;
            }
            try {
                if (PlayerHardcoreRevivalManager.isKnockedOut(target)) {
                    KnockoutSyncHandler.sendHardcoreRevivalData(trackingPlayer, target);
                }
            } catch (Throwable t) {
                LOGGER.warn("[hardcorerevivalfix] failed to resync revival data on START_TRACKING: {}", t.toString());
            }
        });

        // Also resync to any tracking player whenever a knocked-out player
        // is (re)loaded into a world - covers dimension changes / respawn.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof ServerPlayer loaded)) {
                return;
            }
            try {
                if (!PlayerHardcoreRevivalManager.isKnockedOut(loaded)) {
                    return;
                }
                // Send to anyone who is already tracking this player (rare
                // but possible for cross-dimension teleports).
                for (ServerPlayer other : world.getServer().getPlayerList().getPlayers()) {
                    if (other != loaded && other.level() == loaded.level()
                            && other.distanceTo(loaded) < 256.0f) {
                        KnockoutSyncHandler.sendHardcoreRevivalData(other, loaded);
                    }
                }
            } catch (Throwable t) {
                LOGGER.warn("[hardcorerevivalfix] failed to resync revival data on ENTITY_LOAD: {}", t.toString());
            }
        });

        // ---- Mandatory version check (kicks outdated/missing clients). ----
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            try {
                if (!ServerPlayNetworking.canSend(player, VersionCheckPayload.ID)) {
                    String name = "?";
                    try {
                        name = player.getName().getString();
                    } catch (Throwable ignored) { }
                    LOGGER.warn("[hardcorerevivalfix] Kicking {} (outdated or missing hardcorerevivalfix client; please update to {})",
                            name, REQUIRED_VERSION);
                    player.connection.disconnect(Component.literal(
                            "Tu mod Hardcore Revival Fix esta desactualizado o no esta instalado.\n\n" +
                            "Descarga la version " + REQUIRED_VERSION + " de hardcorerevivalfix\n" +
                            "y colocala en tu carpeta mods/ junto al mod hardcorerevival.\n\n" +
                            "Tu cliente actual no es compatible con este servidor."));
                }
            } catch (Throwable t) {
                LOGGER.warn("[hardcorerevivalfix] version-check skipped (no kick): {}", t.toString());
            }
        });

        // Friendly server log line so the operator can see the mod loaded.
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                LOGGER.info("[hardcorerevivalfix] v{} loaded - rescue bug patched, mandatory client check enabled.",
                        REQUIRED_VERSION));
    }
}
