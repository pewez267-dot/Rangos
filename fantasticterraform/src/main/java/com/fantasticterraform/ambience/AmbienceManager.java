package com.fantasticterraform.ambience;

import com.fantasticterraform.data.AmbiencePersistence;
import com.fantasticterraform.network.AmbienceTriggerPacket;
import com.fantasticterraform.network.PacketHandler;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona las zonas de ambiente. El chequeo de en que zona esta cada jugador se hace
 * solo al cambiar de chunk (no cada tick), cumpliendo la regla de rendimiento. Cuando
 * el jugador cambia de zona se envia un {@link AmbienceTriggerPacket} para iniciar o
 * detener el sonido del lado cliente.
 */
public final class AmbienceManager {

    private static final AmbienceManager INSTANCE = new AmbienceManager();

    private final Map<String, AmbienceZone> zones = new ConcurrentHashMap<>();
    private final Map<UUID, String> currentZone = new ConcurrentHashMap<>();

    private AmbienceManager() {
    }

    public static AmbienceManager get() {
        return INSTANCE;
    }

    public void loadAll() {
        zones.clear();
        for (AmbienceZone z : AmbiencePersistence.load()) {
            zones.put(z.id, z);
        }
    }

    private void persist() {
        AmbiencePersistence.save(new ArrayList<>(zones.values()));
    }

    public void add(AmbienceZone zone) {
        zones.put(zone.id, zone);
        persist();
    }

    public void remove(String id) {
        if (zones.remove(id) != null) {
            persist();
        }
    }

    public List<AmbienceZone> all() {
        return new ArrayList<>(zones.values());
    }

    /** Llamado al cambiar de chunk. Determina la zona actual y notifica cambios. */
    public void updateForPlayer(ServerPlayer player) {
        String dim = player.level().dimension().location().toString();
        AmbienceZone found = null;
        for (AmbienceZone z : zones.values()) {
            if (z.dimension.equals(dim) && z.contains(player.getX(), player.getY(), player.getZ())) {
                found = z;
                break;
            }
        }

        String previous = currentZone.get(player.getUUID());
        String newId = found == null ? null : found.id;

        if (previous != null && !previous.equals(newId)) {
            PacketHandler.sendToClient(player, AmbienceTriggerPacket.stop(previous));
        }
        if (newId != null && !newId.equals(previous)) {
            PacketHandler.sendToClient(player, AmbienceTriggerPacket.start(found));
        }

        if (newId == null) {
            currentZone.remove(player.getUUID());
        } else {
            currentZone.put(player.getUUID(), newId);
        }
    }

    public void forgetPlayer(UUID id) {
        currentZone.remove(id);
    }
}
