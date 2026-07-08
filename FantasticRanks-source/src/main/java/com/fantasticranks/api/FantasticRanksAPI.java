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

    private static String descriptor(RankDefinition rank) {
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

    /** Descriptor del rango de tiempo ACTUAL del jugador, o null si no tiene. */
    public static String getCurrentRankDescriptor(Player player) {
        RanksPackage pkg = activePackage(player);
        if (pkg == null || pkg.size() == 0) {
            return null;
        }
        int idx = currentIndex(player);
        // El rango base (indice 0) se trata como "sin rango / nivel 0": NO se muestra tag, solo el
        // nivel del pase (igual que si no hubiera paquete de rangos). El tag de tiempo aparece a
        // partir del primer rango ganado (indice 1). Asi, tras /fsranks wipe, todos quedan sin rango.
        if (idx < 1) {
            return null;
        }
        idx = Math.min(pkg.size() - 1, idx);
        RankDefinition rank = pkg.get(idx);
        return rank == null ? null : descriptor(rank);
    }

    /** Lista de "id\u0000etiqueta" de los rangos de tiempo GANADOS (0..indiceActual). */
    public static List<String> getEarnedRankEntries(Player player) {
        List<String> out = new ArrayList<>();
        RanksPackage pkg = activePackage(player);
        if (pkg == null || pkg.size() == 0) {
            return out;
        }
        int idx = currentIndex(player);
        if (idx < 1) {
            return out;
        }
        idx = Math.min(pkg.size() - 1, idx);
        for (int i = 1; i <= idx; ++i) {
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
        if (idx < 1) {
            return null;
        }
        idx = Math.min(pkg.size() - 1, idx);
        for (int i = 1; i <= idx; ++i) {
            RankDefinition rank = pkg.get(i);
            if (rank != null && sanitizeId(rank.getRankName()).equals(id)) {
                return descriptor(rank);
            }
        }
        return null;
    }
}
