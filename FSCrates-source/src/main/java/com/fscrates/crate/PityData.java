package com.fscrates.crate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

// Contador de aperturas por jugador y por crate, persistente. Alimenta el sistema de
// "pity" (rareza asegurada cada N aperturas): cada apertura incrementa el contador y, si
// count % N == 0, la apertura FUERZA la rareza asegurada configurada en la crate.
public class PityData
extends SavedData {
    public static final String NAME = "fscrates_pity";
    private final Map<String, Integer> counts = new HashMap<String, Integer>();

    public static PityData get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return (PityData)overworld.getDataStorage().computeIfAbsent(PityData::load, PityData::new, NAME);
    }

    private static String key(UUID player, String crateId) {
        return player.toString() + "|" + crateId;
    }

    // Incrementa y devuelve el nuevo numero de aperturas de este jugador en esta crate.
    public int incrementAndGet(UUID player, String crateId) {
        String k = PityData.key(player, crateId);
        int n = this.counts.getOrDefault(k, 0) + 1;
        this.counts.put(k, n);
        this.setDirty();
        return n;
    }

    public int getCount(UUID player, String crateId) {
        return this.counts.getOrDefault(PityData.key(player, crateId), 0);
    }

    public static PityData load(CompoundTag tag) {
        PityData data = new PityData();
        CompoundTag stored = tag.getCompound("counts");
        for (String k : stored.getAllKeys()) {
            data.counts.put(k, stored.getInt(k));
        }
        return data;
    }

    public CompoundTag save(CompoundTag tag) {
        CompoundTag stored = new CompoundTag();
        for (Map.Entry<String, Integer> e : this.counts.entrySet()) {
            stored.putInt(e.getKey(), e.getValue());
        }
        tag.put("counts", (Tag)stored);
        return tag;
    }
}
