package com.fsmobs.client;

import com.fsmobs.FSMobs;
import com.fsmobs.stats.ServerStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Panel de estadisticas en pantalla (esquina superior izquierda). Se muestra/oculta con /fsmobs stats. */
@Mod.EventBusSubscriber(modid = FSMobs.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StatsOverlay {

    private StatsOverlay() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ClientState.overlayOn()) {
            return;
        }
        ServerStats s = ClientState.stats();
        if (s == null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;

        List<String> lines = new ArrayList<>();
        lines.add("\u00a76\u2726 Fantastic Mobs");
        lines.add(tpsColor(s.tps) + "TPS " + fmt(s.tps) + " \u00a77| \u00a7fMSPT " + fmt(s.mspt));
        lines.add("\u00a77RAM: \u00a7f" + s.memUsed + "\u00a77/\u00a7f" + s.memMax + " MB \u00a77| Chunks: \u00a7f" + s.loadedChunks);
        lines.add("\u00a77Dim: \u00a7f" + shortDim(s.dim));
        lines.add("\u00a7eEn tu radio de tope (" + s.radius + " bl): \u00a7f" + s.totalMobsNear() + " mobs");
        lines.add("  " + groupLine(s.near));
        lines.add("\u00a76A tu alrededor (" + s.zoneRadius + " bl): \u00a7f" + s.totalMobsZone() + " mobs");
        lines.add("  " + groupLine(s.zone));
        lines.add("\u00a7bEn la dimension: \u00a7f" + s.totalMobsGlobal() + " mobs \u00a77(" + s.totalEntities + " entidades)");
        lines.add("  " + groupLine(s.global));

        int pad = 4;
        int lineH = 10;
        int maxW = 0;
        for (String l : lines) {
            maxW = Math.max(maxW, font.width(l));
        }
        int x = 4;
        int y = 4;
        int boxW = maxW + pad * 2;
        int boxH = lines.size() * lineH + pad * 2;
        g.fill(x, y, x + boxW, y + boxH, 0xB0000000);
        g.fill(x, y, x + boxW, y + 1, 0xFFFFB300);
        int ty = y + pad;
        for (String l : lines) {
            g.drawString(font, l, x + pad, ty, 0xFFFFFF, false);
            ty += lineH;
        }
    }

    private static String groupLine(int[] counts) {
        String[] ab = {"\u00a7cMon", "\u00a7aAni", "\u00a7eAmb", "\u00a79Agua", "\u00a7dAjo", "\u00a77Otr"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i > 0) {
                sb.append("\u00a77 ");
            }
            sb.append(ab[i]).append(" \u00a7f").append(counts[i]);
        }
        return sb.toString();
    }

    private static String tpsColor(float tps) {
        if (tps >= 19.0f) {
            return "\u00a7a";
        }
        if (tps >= 15.0f) {
            return "\u00a7e";
        }
        return "\u00a7c";
    }

    private static String fmt(float v) {
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    private static String shortDim(String dim) {
        int i = dim.indexOf(':');
        return i >= 0 ? dim.substring(i + 1) : dim;
    }
}
