package com.fsmobs.stats;

import com.fsmobs.MobControl;
import com.fsmobs.network.Net;
import com.fsmobs.network.StatsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recolecta y envia las estadisticas del servidor a los jugadores que las estan viendo (panel en
 * pantalla o pestana de estadisticas). Solo calcula cuando hay alguien mirando y como maximo una vez
 * por segundo => impacto de CPU despreciable.
 */
public final class StatsManager {

    private StatsManager() {}

    private static final int MB = 1024 * 1024;

    /** Jugadores con el panel en pantalla activado (por comando). */
    private static final Set<UUID> HUD_WATCHERS = ConcurrentHashMap.newKeySet();
    /** Jugadores con la pestana de estadisticas abierta en la GUI. */
    private static final Set<UUID> GUI_WATCHERS = ConcurrentHashMap.newKeySet();

    public static boolean toggleHud(UUID id) {
        if (HUD_WATCHERS.contains(id)) {
            HUD_WATCHERS.remove(id);
            return false;
        }
        HUD_WATCHERS.add(id);
        return true;
    }

    public static boolean isHudOn(UUID id) {
        return HUD_WATCHERS.contains(id);
    }

    public static void setGuiWatching(UUID id, boolean watching) {
        if (watching) {
            GUI_WATCHERS.add(id);
        } else {
            GUI_WATCHERS.remove(id);
        }
    }

    public static void clear(UUID id) {
        HUD_WATCHERS.remove(id);
        GUI_WATCHERS.remove(id);
    }

    private static boolean isWatcher(UUID id) {
        return HUD_WATCHERS.contains(id) || GUI_WATCHERS.contains(id);
    }

    /** Llamar en cada tick del servidor; internamente se limita a 1 vez/segundo. */
    public static void serverTick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        if (HUD_WATCHERS.isEmpty() && GUI_WATCHERS.isEmpty()) {
            return;
        }

        float mspt = server.getAverageTickTime();
        float tps = (float) Math.min(20.0, 1000.0 / Math.max(0.01, mspt));
        Runtime rt = Runtime.getRuntime();
        int memUsed = (int) ((rt.totalMemory() - rt.freeMemory()) / MB);
        int memMax = (int) (rt.maxMemory() / MB);
        int radius = MobControl.getRadius();

        // Cache de estadisticas globales por dimension (una sola pasada por dimension).
        Map<ServerLevel, int[]> globalCache = new HashMap<>();
        Map<ServerLevel, int[]> totalsCache = new HashMap<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isWatcher(player.getUUID())) {
                continue;
            }
            ServerLevel level = player.serverLevel();
            int[] global = globalCache.get(level);
            if (global == null) {
                global = new int[6];
                int[] totals = new int[1];
                for (Entity e : level.getAllEntities()) {
                    totals[0]++;
                    if (e instanceof Mob mob) {
                        global[ServerStats.group(mob.getType().getCategory())]++;
                    }
                }
                globalCache.put(level, global);
                totalsCache.put(level, totals);
            }

            ServerStats stats = new ServerStats();
            stats.tps = tps;
            stats.mspt = mspt;
            stats.memUsed = memUsed;
            stats.memMax = memMax;
            stats.loadedChunks = level.getChunkSource().getLoadedChunksCount();
            stats.dim = level.dimension().location().toString();
            stats.radius = radius;
            int zoneR = Math.max(96, radius);
            stats.zoneRadius = zoneR;
            stats.totalEntities = totalsCache.get(level)[0];
            System.arraycopy(global, 0, stats.global, 0, 6);

            // Una sola busqueda al radio amplio; lo que caiga dentro del radio del tope cuenta en "near".
            double px = player.getX();
            double py = player.getY();
            double pz = player.getZ();
            AABB box = player.getBoundingBox().inflate(zoneR);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) {
                int gi = ServerStats.group(mob.getType().getCategory());
                stats.zone[gi]++;
                if (Math.abs(mob.getX() - px) <= radius
                        && Math.abs(mob.getY() - py) <= radius
                        && Math.abs(mob.getZ() - pz) <= radius) {
                    stats.near[gi]++;
                }
            }

            Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StatsPacket(stats));
        }
    }
}
