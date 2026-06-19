package com.fantasticterraform.masks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mantiene la configuracion de mascaras de cada jugador y construye la mascara
 * combinada (AND de todas las activas) que se aplica a Edicion, Brushes y Terreno.
 */
public final class MaskManager {

    private static final Map<UUID, MaskSettings> SETTINGS = new ConcurrentHashMap<>();

    private MaskManager() {
    }

    public static MaskSettings get(UUID id) {
        return SETTINGS.computeIfAbsent(id, k -> new MaskSettings());
    }

    public static void set(UUID id, MaskSettings settings) {
        SETTINGS.put(id, settings);
    }

    public static void remove(UUID id) {
        SETTINGS.remove(id);
    }

    /** Construye la mascara combinada para el jugador, o {@code null} si no hay ninguna activa. */
    public static Mask combinedFor(ServerPlayer player) {
        MaskSettings s = SETTINGS.get(player.getUUID());
        if (s == null) {
            return null;
        }
        return s.build();
    }

    /**
     * Configuracion serializable de mascaras de un jugador.
     */
    public static final class MaskSettings {
        public boolean blockActive;
        public ResourceLocation blockId;

        public boolean listActive;
        public final List<ResourceLocation> listIds = new ArrayList<>();

        public boolean exclusionActive;
        public final List<ResourceLocation> exclusionIds = new ArrayList<>();

        public boolean heightActive;
        public int heightMin = 0;
        public int heightMax = 255;

        public boolean airOnlyActive;
        public boolean skyExposedActive;

        public Mask build() {
            List<Mask> active = new ArrayList<>();

            if (blockActive && blockId != null) {
                Block b = ForgeRegistries.BLOCKS.getValue(blockId);
                if (b != null) {
                    active.add(new BlockMask(b));
                }
            }
            if (listActive && !listIds.isEmpty()) {
                active.add(new BlockListMask(resolve(listIds)));
            }
            if (exclusionActive && !exclusionIds.isEmpty()) {
                active.add(new ExclusionMask(resolve(exclusionIds)));
            }
            if (heightActive) {
                active.add(new HeightMask(heightMin, heightMax));
            }
            if (airOnlyActive) {
                active.add(new AirOnlyMask());
            }
            if (skyExposedActive) {
                active.add(new SkyExposedMask());
            }

            if (active.isEmpty()) {
                return null;
            }
            if (active.size() == 1) {
                return active.get(0);
            }
            Mask[] arr = active.toArray(new Mask[0]);
            return (level, pos) -> {
                for (Mask m : arr) {
                    if (!m.test(level, pos)) {
                        return false;
                    }
                }
                return true;
            };
        }

        private static Set<Block> resolve(List<ResourceLocation> ids) {
            Set<Block> out = new HashSet<>();
            for (ResourceLocation id : ids) {
                Block b = ForgeRegistries.BLOCKS.getValue(id);
                if (b != null) {
                    out.add(b);
                }
            }
            return out;
        }
    }
}
