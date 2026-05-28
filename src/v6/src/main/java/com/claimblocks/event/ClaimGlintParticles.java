/*
 * ClaimGlintParticles v6.0.1 - efecto visual encantado sobre el bloque-centro
 * de cada claim. Compensa que el ENCHANTMENT_GLINT_OVERRIDE solo aplica al
 * ItemStack pero no al bloque colocado.
 *
 * Spawn cada 8 ticks (~2.5 partículas por segundo) solo si hay un jugador a
 * <= 32 bloques. Usa class_2398.ENCHANT (símbolos mágicos) que es 100% vanilla.
 */
package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.minecraft.class_2338;
import net.minecraft.class_2398;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;

public final class ClaimGlintParticles {
    private static int counter = 0;
    private static final int RUN_EVERY_TICKS = 8;
    private static final double VIEW_DISTANCE_SQ = 32.0 * 32.0;

    private ClaimGlintParticles() {}

    public static void tick(MinecraftServer server) {
        if (++counter % RUN_EVERY_TICKS != 0) return;

        for (class_3218 world : server.method_3738()) {
            String dim = world.method_27983().method_29177().toString();
            for (Claim claim : ClaimManager.getInstance().getClaimsInWorld(dim)) {
                class_2338 c = claim.getCenter();
                // Solo si hay un jugador cerca (evita gasto innecesario).
                boolean playerNearby = false;
                for (class_3222 p : world.method_18456()) {
                    double dx = p.method_23317() - (c.method_10263() + 0.5);
                    double dy = p.method_23318() - (c.method_10264() + 0.5);
                    double dz = p.method_23321() - (c.method_10260() + 0.5);
                    if (dx * dx + dy * dy + dz * dz <= VIEW_DISTANCE_SQ) {
                        playerNearby = true;
                        break;
                    }
                }
                if (!playerNearby) continue;

                // Partículas de encantamiento alrededor del bloque (radio pequeño).
                double cx = c.method_10263() + 0.5;
                double cy = c.method_10264() + 1.1;
                double cz = c.method_10260() + 0.5;
                world.method_14199(class_2398.field_11215, cx, cy, cz, 6, 0.35, 0.35, 0.35, 0.6);
            }
        }
    }
}
