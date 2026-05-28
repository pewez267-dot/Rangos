/*
 * PlayerTracker v6.0.0
 * - FIX: pushOutOfClaim recalcula Y segura (no atasca dentro de bloques).
 * - FIX: limpieza de lastClaim/lastAlert al desconectar.
 * - FIX: ConcurrentHashMap para multithreading.
 */
package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.class_124;
import net.minecraft.class_1657;
import net.minecraft.class_1923;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_2902;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3419;
import net.minecraft.class_5250;
import net.minecraft.server.MinecraftServer;

public final class PlayerTracker {
    private static final Map<UUID, UUID> lastClaim = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastAlert = new ConcurrentHashMap<>();
    private static final long ALERT_COOLDOWN_TICKS = 600L;
    private static int bypassReminderCounter = 0;

    public static void register() {
        // FIX v6: limpieza al desconectar
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.field_14140.method_5667();
            lastClaim.remove(id);
            lastAlert.remove(id);
            ClaimManager.getInstance().onPlayerDisconnect(id);
        });
    }

    public static void tick(MinecraftServer server) {
        boolean showBypassReminder = ++bypassReminderCounter % 60 == 0;
        for (class_3218 world : server.method_3738()) {
            for (class_3222 player : world.method_18456()) {
                handle(world, player);
                if (!showBypassReminder || !player.method_5687(2) || !ClaimManager.getInstance().isBypassing(player.method_5667())) continue;
                player.method_7353(class_2561.method_43470("[!] BYPASS ACTIVO").method_27695(new class_124[]{class_124.field_1061, class_124.field_1067}), true);
            }
        }
    }

    private static void handle(class_3218 world, class_3222 player) {
        Claim now = ClaimManager.getInstance().getClaimAt(world, player.method_24515());
        UUID prev = lastClaim.get(player.method_5667());
        UUID nowId = now == null ? null : now.getClaimId();
        if (Objects.equals(prev, nowId)) {
            if (now != null && now.isBanned(player.method_5667()) && !player.method_5687(2)) {
                pushOutOfClaim(world, player, now);
            }
            return;
        }
        if (prev != null) {
            Claim left = findClaimById(prev);
            if (left != null) {
                class_5250 msg = class_2561.method_43470("[Claim] ").method_27692(class_124.field_1080)
                        .method_10852(class_2561.method_43470("Saliendo de la zona de ").method_27692(class_124.field_1061))
                        .method_10852(class_2561.method_43470(truncate(left.getOwnerName(), 20)).method_27695(new class_124[]{class_124.field_1068, class_124.field_1067}));
                player.method_7353(msg, true);
                player.method_17356(class_3417.field_14624.comp_349(), class_3419.field_15248, 0.5f, 1.0f);
            }
        }
        if (now != null) {
            if (now.isBanned(player.method_5667()) && !player.method_5687(2)) {
                player.method_7353(class_2561.method_43470("[!] Estás baneado de esta zona.").method_27695(new class_124[]{class_124.field_1061, class_124.field_1067}), false);
                pushOutOfClaim(world, player, now);
                lastClaim.remove(player.method_5667());
                return;
            }
            class_5250 entryMsg = (now.getFlags().showWelcome && now.getFlags().welcomeMessage != null && !now.getFlags().welcomeMessage.isBlank())
                    ? class_2561.method_43470("[Claim] ").method_27692(class_124.field_1080)
                            .method_10852(class_2561.method_43470(truncate(now.getFlags().welcomeMessage, 50)).method_27692(class_124.field_1060))
                    : class_2561.method_43470("[Claim] ").method_27692(class_124.field_1080)
                            .method_10852(class_2561.method_43470("Entrando a la zona de ").method_27692(class_124.field_1060))
                            .method_10852(class_2561.method_43470(truncate(now.getOwnerName(), 16)).method_27695(new class_124[]{class_124.field_1068, class_124.field_1067}))
                            .method_10852(class_2561.method_43470(" (" + now.sizeLabel() + ")").method_27692(class_124.field_1080));
            player.method_7353(entryMsg, true);
            player.method_17356(class_3417.field_14725.comp_349(), class_3419.field_15248, 0.5f, 1.0f);
            if (now.getFlags().trespasserAlerts && !now.canModify(player)) {
                long t = world.method_8503().method_3780();
                Long last = lastAlert.get(player.method_5667());
                if (last == null || t - last > ALERT_COOLDOWN_TICKS) {
                    lastAlert.put(player.method_5667(), t);
                    class_3222 owner = world.method_8503().method_3760().method_14602(now.getOwnerUUID());
                    if (owner != null) {
                        class_5250 alert = class_2561.method_43470("[!] ").method_27695(new class_124[]{class_124.field_1061, class_124.field_1067})
                                .method_10852(class_2561.method_43470(truncate(player.method_5477().getString(), 16)).method_27695(new class_124[]{class_124.field_1068, class_124.field_1067}))
                                .method_10852(class_2561.method_43470(" entró a tu zona en X=" + now.getX() + " Z=" + now.getZ()).method_27692(class_124.field_1054));
                        owner.method_7353(alert, false);
                    }
                }
            }
        }
        lastClaim.put(player.method_5667(), nowId);
    }

    private static Claim findClaimById(UUID id) {
        for (Claim c : ClaimManager.getInstance().getAllClaims()) {
            if (c.getClaimId().equals(id)) return c;
        }
        return null;
    }

    /**
     * FIX v6: empuja al jugador hacia afuera del claim Y recalcula Y segura
     * usando el heightmap MOTION_BLOCKING_NO_LEAVES, evitando atasco en bloques sólidos.
     */
    private static void pushOutOfClaim(class_3218 world, class_3222 player, Claim claim) {
        int r = claim.getRadius() + 2;
        class_243 cur = player.method_19538();
        double dx = cur.field_1352 - claim.getX();
        double dz = cur.field_1350 - claim.getZ();
        double mag = Math.max(1.0E-4, Math.sqrt(dx * dx + dz * dz));
        double tx = claim.getX() + 0.5 + dx / mag * r;
        double tz = claim.getZ() + 0.5 + dz / mag * r;
        int ix = (int) Math.floor(tx);
        int iz = (int) Math.floor(tz);
        // Asegurar chunk cargado antes de pedir la heightmap
        world.method_8497(ix >> 4, iz >> 4);
        int safeY = world.method_8624(class_2902.class_2903.field_13202, ix, iz);
        int minY = world.method_31607();
        int maxY = world.method_31600();
        if (safeY <= minY) safeY = (int) player.method_23318();
        if (safeY >= maxY) safeY = maxY - 2;
        player.method_5859(tx, safeY, tz);
        player.method_18800(0, 0, 0); // detener velocidad
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }
}
