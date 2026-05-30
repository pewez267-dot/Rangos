/*
 * PassiveEffectsManager v6.0.1 - efectos escalonados por tier de paga.
 *
 * Reglas:
 *   - 250x250 (paid pequeño): solo Regeneración
 *   - 300x300 (paid mediano): Regeneración + Resistencia + Velocidad
 *   - 500x500 (paid grande):  Regeneración + Resistencia + Velocidad + Vuelo
 *
 * El vuelo en 500x500 SOLO se concede al OWNER, no a miembros.
 * Los efectos se siguen condicionando por las flags del claim
 * (effectRegeneration / effectResistance / effectSpeed / allowFlight).
 */
package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.class_124;
import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_1657;
import net.minecraft.class_1934;
import net.minecraft.class_1937;
import net.minecraft.class_2561;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;

public final class PassiveEffectsManager {
    private static int counter = 0;
    // Thread-safe + acepta nulls? No, ConcurrentHashMap.newKeySet sí. Lo usamos.
    private static final Set<UUID> grantedFlight = ConcurrentHashMap.newKeySet();

    private PassiveEffectsManager() {}

    public static void tick(MinecraftServer server) {
        if (++counter % 20 != 0) return;
        boolean runEffects = counter % 40 == 0;
        for (class_3218 world : server.method_3738()) {
            for (class_3222 player : world.method_18456()) {
                Claim claim = ClaimManager.getInstance().getClaimAt(world, player.method_24515());
                handleFlight(player, claim);
                if (runEffects) applyEffects(player, claim);
            }
        }
    }

    /** ¿Qué tier "nivel" tiene una piedra paga? 1=250x250, 2=300x300, 3=500x500, 0=ninguno. */
    private static int paidLevel(ClaimTier t) {
        if (t == null) return 0;
        return switch (t.id) {
            case "claimstone_250x250" -> 1;
            case "claimstone_300x300" -> 2;
            case "claimstone_500x500" -> 3;
            default -> 0;
        };
    }

    /**
     * Vuelo: solo en 500x500 (level 3). Solo para el OWNER, no miembros.
     * Visitantes nunca tienen vuelo aunque la flag esté ON.
     */
    private static void handleFlight(class_3222 player, Claim claim) {
        UUID id = player.method_5667();

        // FIX v6.1.2: en creativo/espectador el JUEGO maneja el vuelo.
        // Nunca tocamos sus abilities; solo limpiamos nuestro registro para
        // no pelear con el modo de juego (causaba spam en consola).
        class_1934 mode = player.field_13974.method_14257();
        if (mode == class_1934.field_9220 || mode == class_1934.field_9219) {
            grantedFlight.remove(id);
            return;
        }

        boolean shouldHaveClaimFlight = false;
        if (claim != null) {
            int level = paidLevel(claim.getTier());
            // Solo el owner (no miembros) en una zona 500x500 con flag de vuelo activa.
            if (level >= 3 && claim.isOwner((class_1657) player) && claim.getFlags().allowFlight) {
                shouldHaveClaimFlight = true;
            }
        }

        boolean weGranted = grantedFlight.contains(id);
        boolean alreadyCanFly = player.method_31549().field_7478;

        if (shouldHaveClaimFlight) {
            // Solo concedemos si NOSOTROS no lo hemos hecho y el jugador NO tiene
            // vuelo ya por otra fuente (rango, /fly, otro mod). Así no lo reclamamos
            // ni entramos en conflicto con permisos externos.
            if (!weGranted && !alreadyCanFly) {
                player.method_31549().field_7478 = true;
                player.method_7355();
                grantedFlight.add(id);
                player.method_7353(
                    class_2561.method_43470("\u2714 Vuelo activado (Owner 500x500).")
                        .method_27692(class_124.field_1060), true);
            }
            // Si ya volaba por otra fuente, no hacemos nada (respetamos su vuelo).
        } else {
            // No debe tener vuelo POR EL CLAIM. Solo revocamos si NOSOTROS lo dimos.
            if (weGranted) {
                player.method_31549().field_7478 = false;
                player.method_31549().field_7479 = false;
                player.method_7355();
                grantedFlight.remove(id);
                player.method_7353(
                    class_2561.method_43470("[i] Saliste de la zona de vuelo.")
                        .method_27692(class_124.field_1075), true);
            }
        }
    }

    /**
     * Efectos escalonados por tier paid (toggleables via flags del menu):
     *   level 1 (250x250): regen
     *   level 2 (300x300): regen + resistance + speed
     *   level 3 (500x500): regen + resistance + speed (vuelo se maneja en handleFlight)
     *
     * Cada efecto requiere ADEMAS la flag correspondiente activa en el menu GUI.
     * Las flags vienen ON por default al crear el claim (ver ClaimManager.createClaim).
     */
    private static void applyEffects(class_3222 player, Claim claim) {
        if (claim == null) return;
        int level = paidLevel(claim.getTier());
        if (level == 0) return;
        // Owner SIEMPRE recibe efectos en su zona; miembros también si tienen canModify.
        // Visitantes no reciben nada.
        if (!claim.canModify((class_1657) player)) return;

        boolean canRegen   = level >= 1;
        boolean canResist  = level >= 2;
        boolean canSpeed   = level >= 2;

        if (canRegen && claim.getFlags().effectRegeneration) {
            player.method_6092(new class_1293(class_1294.field_5924, 60, 0, true, false, true));
        }
        if (canResist && claim.getFlags().effectResistance) {
            player.method_6092(new class_1293(class_1294.field_5907, 60, 0, true, false, true));
        }
        if (canSpeed && claim.getFlags().effectSpeed) {
            player.method_6092(new class_1293(class_1294.field_5904, 60, 0, true, false, true));
        }
    }

    /** Limpieza al desconectar (llamado desde PlayerTracker). */
    public static void onPlayerDisconnect(UUID id) {
        grantedFlight.remove(id);
    }
}
