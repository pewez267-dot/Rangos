package com.fantasticterraform.particles;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.data.ParticlePersistence;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.ParticleEmitterDefinitionPacket;
import com.fantasticterraform.network.RemoveParticleEmitterPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona los emisores de particulas del servidor. Envia la definicion de cada
 * emisor a un jugador solo una vez, cuando entra en su radio de visibilidad
 * (rastreando que emisores ya conoce cada jugador), y nunca particula por particula.
 */
public final class ParticleEmitterManager {

    private static final ParticleEmitterManager INSTANCE = new ParticleEmitterManager();

    private final Map<String, ParticleEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> known = new ConcurrentHashMap<>();
    private net.minecraft.server.MinecraftServer server;

    private ParticleEmitterManager() {
    }

    public static ParticleEmitterManager get() {
        return INSTANCE;
    }

    public void loadAll(net.minecraft.server.MinecraftServer server) {
        this.server = server;
        emitters.clear();
        known.clear();
        for (ParticleEmitter e : ParticlePersistence.load(server)) {
            emitters.put(e.id, e);
        }
    }

    private void persist() {
        if (server != null) {
            ParticlePersistence.save(server, new ArrayList<>(emitters.values()));
        }
    }

    public boolean add(ServerPlayer creator, ParticleEmitter emitter) {
        long inDim = emitters.values().stream().filter(e -> e.dimension.equals(emitter.dimension)).count();
        if (inDim >= TerraformConfig.GENERAL.maxActiveEmitters.get()) {
            creator.sendSystemMessage(Component.literal("\u00a7cLimite de emisores de particulas alcanzado en esta dimension."));
            return false;
        }
        emitter.createdGameTime = creator.level().getGameTime();
        emitters.put(emitter.id, emitter);
        persist();
        creator.sendSystemMessage(Component.literal("\u00a7aEmisor de particulas creado (" + emitter.particleType + ")."));
        return true;
    }

    public void remove(String id) {
        ParticleEmitter removed = emitters.remove(id);
        if (removed == null) {
            return;
        }
        persist();
        for (Map.Entry<UUID, Set<String>> e : known.entrySet()) {
            e.getValue().remove(id);
        }
    }

    public List<ParticleEmitter> inDimension(String dimension) {
        List<ParticleEmitter> out = new ArrayList<>();
        for (ParticleEmitter e : emitters.values()) {
            if (e.dimension.equals(dimension)) {
                out.add(e);
            }
        }
        return out;
    }

    /** Recalcula que emisores debe conocer el jugador segun su posicion y rango. */
    public void updateForPlayer(ServerPlayer player) {
        String dim = player.level().dimension().location().toString();
        Set<String> playerKnown = known.computeIfAbsent(player.getUUID(), k -> new HashSet<>());

        Set<String> inRange = new HashSet<>();
        for (ParticleEmitter e : emitters.values()) {
            if (!e.dimension.equals(dim)) {
                continue;
            }
            double dx = player.getX() - e.x;
            double dy = player.getY() - e.y;
            double dz = player.getZ() - e.z;
            double r = e.visibilityRadius;
            if (dx * dx + dy * dy + dz * dz <= r * r) {
                inRange.add(e.id);
                if (!playerKnown.contains(e.id)) {
                    PacketHandler.sendToClient(player, ParticleEmitterDefinitionPacket.of(e));
                    playerKnown.add(e.id);
                }
            }
        }

        // Emisores que salieron de rango o ya no existen: avisar al cliente que pare.
        playerKnown.removeIf(id -> {
            if (!inRange.contains(id)) {
                PacketHandler.sendToClient(player, new RemoveParticleEmitterPacket(id));
                return true;
            }
            return false;
        });
    }

    public void forgetPlayer(UUID id) {
        known.remove(id);
    }
}
