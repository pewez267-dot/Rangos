package com.fantasticterraform.particles.client;

import com.fantasticterraform.particles.ParticleEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renderizador client-side de emisores de particulas. Recibe la definicion una vez y
 * genera las particulas localmente cada tick segun la tasa de emision; el servidor
 * nunca envia particulas individuales.
 */
public final class ClientParticleRenderer {

    private static final Map<String, ParticleEmitter> EMITTERS = new ConcurrentHashMap<>();
    private static final Map<String, Double> ACCUMULATORS = new ConcurrentHashMap<>();

    private ClientParticleRenderer() {
    }

    public static void addEmitter(ParticleEmitter emitter) {
        EMITTERS.put(emitter.id, emitter);
        ACCUMULATORS.put(emitter.id, 0.0D);
    }

    public static void removeEmitter(String id) {
        EMITTERS.remove(id);
        ACCUMULATORS.remove(id);
    }

    public static void clear() {
        EMITTERS.clear();
        ACCUMULATORS.clear();
    }

    /** Devuelve el id del emisor conocido mas cercano a una posicion, o null. */
    public static String nearestId(double x, double y, double z) {
        String best = null;
        double bestDist = Double.MAX_VALUE;
        for (ParticleEmitter e : EMITTERS.values()) {
            double dx = x - e.x;
            double dy = y - e.y;
            double dz = z - e.z;
            double d = dx * dx + dy * dy + dz * dz;
            if (d < bestDist) {
                bestDist = d;
                best = e.id;
            }
        }
        return best;
    }

    /** Llamado cada client tick. Genera particulas para los emisores en rango. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || EMITTERS.isEmpty()) {
            return;
        }
        String dim = level.dimension().location().toString();

        for (ParticleEmitter e : EMITTERS.values()) {
            if (!e.dimension.equals(dim)) {
                continue;
            }
            double dx = player.getX() - e.x;
            double dy = player.getY() - e.y;
            double dz = player.getZ() - e.z;
            if (dx * dx + dy * dy + dz * dz > e.visibilityRadius * e.visibilityRadius) {
                continue;
            }
            ParticleOptions options = optionsFor(e);
            if (options == null) {
                continue;
            }
            double perTick = e.emissionRate / 20.0D;
            double acc = ACCUMULATORS.getOrDefault(e.id, 0.0D) + perTick;
            int count = (int) acc;
            ACCUMULATORS.put(e.id, acc - count);
            for (int i = 0; i < count; i++) {
                if (e.hasRegion) {
                    // Reparte las particulas por TODA el area seleccionada.
                    double rx = e.minX + level.random.nextDouble() * (e.maxX - e.minX + 1);
                    double ry = e.minY + level.random.nextDouble() * (e.maxY - e.minY + 1);
                    double rz = e.minZ + level.random.nextDouble() * (e.maxZ - e.minZ + 1);
                    level.addParticle(options, rx, ry, rz, e.vx, e.vy, e.vz);
                } else {
                    double jx = (level.random.nextDouble() - 0.5D) * 0.4D;
                    double jy = (level.random.nextDouble() - 0.5D) * 0.4D;
                    double jz = (level.random.nextDouble() - 0.5D) * 0.4D;
                    level.addParticle(options, e.x + jx, e.y + jy, e.z + jz, e.vx, e.vy, e.vz);
                }
            }
        }
    }

    private static ParticleOptions optionsFor(ParticleEmitter e) {
        ResourceLocation id = ResourceLocation.tryParse(e.particleType);
        if (id == null) {
            return null;
        }
        ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(id);
        if (type == null) {
            return null;
        }
        if (type instanceof SimpleParticleType simple) {
            return simple;
        }
        // Particulas tintables tipo "dust": construir con color y tamano.
        if (type == net.minecraft.core.particles.ParticleTypes.DUST) {
            return new DustParticleOptions(new Vector3f(e.red, e.green, e.blue), Math.max(0.01F, e.size));
        }
        return null;
    }
}
