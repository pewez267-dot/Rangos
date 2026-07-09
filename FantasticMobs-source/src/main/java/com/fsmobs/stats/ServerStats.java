package com.fsmobs.stats;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.MobCategory;

/** Instantanea de estadisticas del servidor que se envia al cliente para mostrarlas. */
public final class ServerStats {

    /** Grupos de mobs que se muestran (indices 0..5). */
    public static final String[] GROUPS = {"Monstruos", "Animales", "Ambiente", "Agua", "Ajolotes", "Otros"};

    public float tps;
    public float mspt;
    public int memUsed;
    public int memMax;
    public int loadedChunks;
    public String dim = "";
    public int radius;      // radio del tope (para el conteo "en tu radio")
    public int zoneRadius;  // radio amplio (para el conteo "a tu alrededor")
    public int totalEntities;
    public final int[] global = new int[6];
    public final int[] near = new int[6];   // dentro del radio del tope
    public final int[] zone = new int[6];   // a tu alrededor (radio amplio)

    /** Indice de grupo para una categoria de mob. */
    public static int group(MobCategory cat) {
        return switch (cat) {
            case MONSTER -> 0;
            case CREATURE -> 1;
            case AMBIENT -> 2;
            case WATER_CREATURE, WATER_AMBIENT, UNDERGROUND_WATER_CREATURE -> 3;
            case AXOLOTLS -> 4;
            default -> 5;
        };
    }

    public int sum(int[] arr) {
        int s = 0;
        for (int v : arr) {
            s += v;
        }
        return s;
    }

    public int totalMobsGlobal() {
        return sum(global);
    }

    public int totalMobsNear() {
        return sum(near);
    }

    public int totalMobsZone() {
        return sum(zone);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(tps);
        buf.writeFloat(mspt);
        buf.writeVarInt(memUsed);
        buf.writeVarInt(memMax);
        buf.writeVarInt(loadedChunks);
        buf.writeUtf(dim);
        buf.writeVarInt(radius);
        buf.writeVarInt(zoneRadius);
        buf.writeVarInt(totalEntities);
        for (int v : global) {
            buf.writeVarInt(v);
        }
        for (int v : near) {
            buf.writeVarInt(v);
        }
        for (int v : zone) {
            buf.writeVarInt(v);
        }
    }

    public static ServerStats read(FriendlyByteBuf buf) {
        ServerStats s = new ServerStats();
        s.tps = buf.readFloat();
        s.mspt = buf.readFloat();
        s.memUsed = buf.readVarInt();
        s.memMax = buf.readVarInt();
        s.loadedChunks = buf.readVarInt();
        s.dim = buf.readUtf();
        s.radius = buf.readVarInt();
        s.zoneRadius = buf.readVarInt();
        s.totalEntities = buf.readVarInt();
        for (int i = 0; i < 6; i++) {
            s.global[i] = buf.readVarInt();
        }
        for (int i = 0; i < 6; i++) {
            s.near[i] = buf.readVarInt();
        }
        for (int i = 0; i < 6; i++) {
            s.zone[i] = buf.readVarInt();
        }
        return s;
    }
}
