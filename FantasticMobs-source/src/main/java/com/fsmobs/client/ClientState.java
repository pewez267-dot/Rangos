package com.fsmobs.client;

import com.fsmobs.MobControl;
import com.fsmobs.client.screen.MobControlScreen;
import com.fsmobs.stats.ServerStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.LinkedHashMap;
import java.util.Map;

/** Estado cacheado en el cliente (config para la GUI, ultimas estadisticas, panel on/off). */
public final class ClientState {

    private ClientState() {}

    // Copia editable de la config (para UI optimista; el servidor confirma con SyncConfigPacket).
    public static int radius = 32;
    public static double multiplier = 1.0;
    public static final Map<String, Integer> categoryCaps = new LinkedHashMap<>();
    public static final Map<String, Integer> typeCaps = new LinkedHashMap<>();

    private static ServerStats stats;
    private static boolean overlayOn;

    public static ServerStats stats() {
        return stats;
    }

    public static boolean overlayOn() {
        return overlayOn;
    }

    private static void copyFrom(MobControl.Snapshot snap) {
        radius = snap.radius();
        multiplier = snap.multiplier();
        categoryCaps.clear();
        categoryCaps.putAll(snap.categoryCaps());
        typeCaps.clear();
        typeCaps.putAll(snap.typeCaps());
    }

    public static void openConfig(MobControl.Snapshot snap) {
        copyFrom(snap);
        Minecraft.getInstance().setScreen(new MobControlScreen());
    }

    public static void updateConfig(MobControl.Snapshot snap) {
        copyFrom(snap);
        Screen s = Minecraft.getInstance().screen;
        if (s instanceof MobControlScreen screen) {
            screen.onConfigSynced();
        }
    }

    public static void setStats(ServerStats s) {
        stats = s;
    }

    public static void setOverlay(boolean on) {
        overlayOn = on;
    }
}
