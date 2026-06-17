package com.fantasticaudit.events;

import com.fantasticaudit.FantasticAudit;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * Optional integration that captures block breaks routed through <b>Architectury</b>'s
 * {@code BlockEvent.BREAK} rather than Forge's {@code BlockEvent.BreakEvent}.
 *
 * <p>Area mining tools such as <i>JustHammers</i> break the directly-hit block normally (which
 * fires the Forge event), but break the surrounding blocks by invoking Architectury's event and
 * removing them directly — so those extra blocks never reach Forge's event and would otherwise be
 * invisible to the audit. When the Architectury API is installed, this hook listens to that event
 * and forwards each break to {@link BlockEventHandler#logBlockBreak}, which de-duplicates the
 * directly-hit block so it is logged exactly once.</p>
 *
 * <p>All references to Architectury live inside the {@link Hooks} inner class, which is only loaded
 * after confirming the {@code architectury} mod is present. Without it, this class is inert and the
 * mod runs normally.</p>
 */
public final class ArchitecturyAuditHook {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile boolean registered;

    private ArchitecturyAuditHook() {
    }

    /** Registers the Architectury break listener when the Architectury API is available. Idempotent. */
    public static void init() {
        if (registered) {
            return;
        }
        if (!ModList.get().isLoaded("architectury")) {
            LOGGER.info("[FantasticAudit] Architectury not detected; area-tool break capture is disabled.");
            return;
        }
        try {
            Hooks.register();
            registered = true;
            LOGGER.info("[FantasticAudit] Architectury detected; capturing area-tool block breaks (e.g. JustHammers).");
        } catch (final Throwable t) {
            LOGGER.warn("[FantasticAudit] Could not register the Architectury break hook: {}", t.toString());
        }
    }

    /** Isolated holder for all Architectury API references (loaded only when the mod is present). */
    private static final class Hooks {
        static void register() {
            dev.architectury.event.events.common.BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
                try {
                    if (player != null && level instanceof ServerLevel serverLevel) {
                        // pos may be a mutable cursor; copy it for safe keying/logging.
                        BlockEventHandler.logBlockBreak(player, serverLevel, pos.immutable(), state,
                                player.getMainHandItem());
                    }
                } catch (final Throwable t) {
                    LOGGER.warn("[FantasticAudit] Error handling Architectury block break: {}", t.toString());
                }
                // Observe only; never cancel the break.
                return dev.architectury.event.EventResult.pass();
            });
        }
    }
}
