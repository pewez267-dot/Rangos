package com.fantasticpass.network;

import com.fantasticpass.capability.PassCapability;
import com.fantasticpass.data.NametagStyle;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PassRankReward;
import com.fantasticpass.data.PassSavedData;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.interop.FantasticRanksInterop;
import com.fantasticpass.nametag.NametagData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class NametagSync {
    private NametagSync() {
    }

    /** Estilo del label "Nivel: N" segun el pase activo y si el jugador es premium. */
    private static NametagStyle resolveLevelStyle(ServerPlayer player, PlayerPassData data) {
        try {
            MinecraftServer server = player.getServer();
            if (server != null) {
                PassDefinition pass = PassSavedData.get(server).getActivePass();
                if (pass != null) {
                    boolean premium = data != null && data.isPremium();
                    NametagStyle s = premium ? pass.getLevelStylePremium() : pass.getLevelStyleFree();
                    if (s != null) {
                        return s.copy();
                    }
                }
            }
        } catch (Throwable ignored) {
            // fail-safe: si algo falla, usamos el gris por defecto
        }
        return NametagData.defaultLevelStyle();
    }

    public static NametagData compute(ServerPlayer player) {
        PlayerPassData data = PassCapability.getData((Player) player);
        int level = data != null ? data.getCurrentTier() : 0;
        NametagStyle levelStyle = resolveLevelStyle(player, data);
        if (data != null && data.getDisplayedRankId() != null) {
            String sel = data.getDisplayedRankId();
            // \u00bfEs un rango del pase ganado?
            PassRankReward reward = data.getEarnedRank(sel);
            if (reward != null) {
                return new NametagData(level, true, true, reward.getRankDisplayText(), reward.getStyle(), "", levelStyle);
            }
            // \u00bfEs un rango de tiempo (mod Ranks) ganado?
            NametagData fromRanks = fromDescriptor(FantasticRanksInterop.getRankDescriptor(player, sel), level, levelStyle);
            if (fromRanks != null) {
                return fromRanks;
            }
            // id invalido -> cae al rango de tiempo actual
        }
        // Por defecto: rango de tiempo actual (mod Ranks)
        NametagData current = fromDescriptor(FantasticRanksInterop.getCurrentRankDescriptor(player), level, levelStyle);
        if (current != null) {
            return current;
        }
        // Sin descriptor de rango (estado transitorio del interop con Ranks): NO dejamos caer la linea.
        // Como Ranks SIEMPRE cede el dibujado al pase, si aqui devolvieramos hasLine=false el jugador
        // se quedaria sin ninguna linea (rango "desaparecido"). Mostramos al menos el "Nivel N".
        return new NametagData(level, true, true, "", new NametagStyle(), "", levelStyle);
    }

    private static NametagData fromDescriptor(String desc, int level, NametagStyle levelStyle) {
        if (desc == null || desc.isEmpty()) {
            return null;
        }
        String[] p = desc.split("\u0000", -1);
        if (p.length < 11) {
            return null;
        }
        try {
            String text = p[0];
            int color = Integer.parseInt(p[1]);
            boolean bold = "1".equals(p[2]);
            boolean italic = "1".equals(p[3]);
            boolean underline = "1".equals(p[4]);
            boolean strike = "1".equals(p[5]);
            boolean gradient = "1".equals(p[6]);
            int gStart = Integer.parseInt(p[7]);
            int gEnd = Integer.parseInt(p[8]);
            boolean rainbow = "1".equals(p[9]);
            int rainbowStyle = Integer.parseInt(p[10]);
            NametagStyle style = new NametagStyle(color, bold, italic, underline, strike, gradient, gStart, gEnd, rainbow, rainbowStyle);
            return new NametagData(level, true, true, text, style, "", levelStyle);
        } catch (Exception e) {
            return null;
        }
    }

    public static void syncPlayer(ServerPlayer player) {
        NametagData data = NametagSync.compute(player);
        PacketHandler.sendToTrackingAndSelf(player, new NametagUpdatePacket(player.getUUID(), data));
    }
}
