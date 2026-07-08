package com.fantasticranks.api;

import com.fantasticranks.capability.RanksCapability;
import com.fantasticranks.data.NametagStyle;
import com.fantasticranks.data.PlayerRanksData;
import com.fantasticranks.data.RankDefinition;
import com.fantasticranks.data.RanksPackage;
import com.fantasticranks.data.RanksSavedData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

/**
 * API publica para que otros mods (p. ej. FantasticPass) consulten los rangos de tiempo del jugador.
 * Se accede por reflexion, asi que las firmas deben mantenerse estables.
 *
 * Los "descriptores" de estilo se serializan como un String con campos separados por '\u0000':
 *   texto, color, negrita, cursiva, subrayado, tachado, gradiente, gradStart, gradEnd, arcoiris, estiloArcoiris
 * (los booleanos como "1"/"0", los enteros en decimal).
 */
public final class FantasticRanksAPI {
    private static final char SEP = '\u0000';

    private FantasticRanksAPI() {
    }

    private static RanksPackage activePackage(Player player) {
        if (player == null) {
            return null;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }
        return RanksSavedData.get(server).getActivePackage();
    }

    private static int currentIndex(Player player) {
        PlayerRanksData data = RanksCapability.getData(player);
        return data == null ? -1 : data.getCurrentRankIndex();
    }

    /** Convierte un nombre de rango en un id manejable por comando (minusculas, alfanumerico, espacios->_). */
    public static String sanitizeId(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : name.toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            } else if (c == ' ' || c == '_' || c == '-') {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    public static String descriptor(RankDefinition rank) {
        NametagStyle s = rank.getStyle();
        return new StringBuilder()
                .append(rank.getRankName()).append(SEP)
                .append(s.getColor()).append(SEP)
                .append(s.isBold() ? 1 : 0).append(SEP)
                .append(s.isItalic() ? 1 : 0).append(SEP)
                .append(s.isUnderline() ? 1 : 0).append(SEP)
                .append(s.isStrikethrough() ? 1 : 0).append(SEP)
                .append(s.isGradient() ? 1 : 0).append(SEP)
                .append(s.getGradientStart()).append(SEP)
                .append(s.getGradientEnd()).append(SEP)
                .append(s.isRainbow() ? 1 : 0).append(SEP)
                .append(s.getRainbowStyle())
                .toString();
    }

    /**
     * Descriptor del rango de tiempo GANADO del jugador (instantanea), o null si no tiene (wipeado).
     * Se lee de la instantanea guardada en el jugador, NO del paquete: asi el rango persiste SIEMPRE,
     * incluso si el paquete de rangos se borra o se cambia. Solo /fsranks wipe lo elimina.
     * El progreso (RankProgressionManager) mantiene esta instantanea actualizada mientras el paquete exista.
     */
    public static String getCurrentRankDescriptor(Player player) {
        PlayerRanksData data = RanksCapability.getData(player);
        if (data == null) {
            return null;
        }
        String d = data.getEarnedDescriptor();
        return d == null || d.isEmpty() ? null : d;
    }

    /** Lista de "id\u0000etiqueta" de los rangos de tiempo GANADOS (0..indiceActual). */
    public static List<String> getEarnedRankEntries(Player player) {
        List<String> out = new ArrayList<>();
        RanksPackage pkg = activePackage(player);
        if (pkg == null || pkg.size() == 0) {
            return out;
        }
        int idx = currentIndex(player);
        if (idx < 0) {
            return out;
        }
        idx = Math.min(pkg.size() - 1, idx);
        for (int i = 0; i <= idx; ++i) {
            RankDefinition rank = pkg.get(i);
            if (rank == null) {
                continue;
            }
            out.add(sanitizeId(rank.getRankName()) + SEP + rank.getRankName());
        }
        return out;
    }

    /** Devuelve solo los IDs de los rangos de tiempo ganados (para autocompletar comandos). */
    public static List<String> getEarnedRankIds(Player player) {
        List<String> out = new ArrayList<>();
        for (String entry : getEarnedRankEntries(player)) {
            int i = entry.indexOf(SEP);
            out.add(i >= 0 ? entry.substring(0, i) : entry);
        }
        return out;
    }

    /** Descriptor de un rango de tiempo GANADO por id (nombre saneado), o null si no lo tiene. */
    public static String getRankDescriptor(Player player, String id) {
        if (id == null) {
            return null;
        }
        RanksPackage pkg = activePackage(player);
        if (pkg == null || pkg.size() == 0) {
            return null;
        }
        int idx = currentIndex(player);
        if (idx < 0) {
            return null;
        }
        idx = Math.min(pkg.size() - 1, idx);
        for (int i = 0; i <= idx; ++i) {
            RankDefinition rank = pkg.get(i);
            if (rank != null && sanitizeId(rank.getRankName()).equals(id)) {
                return descriptor(rank);
            }
        }
        return null;
    }
}
