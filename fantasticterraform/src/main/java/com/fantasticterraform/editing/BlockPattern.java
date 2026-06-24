package com.fantasticterraform.editing;

import net.minecraft.core.HolderLookup;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.fantasticterraform.schematics.BlockStateCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Patron de bloques ponderado, estilo WorldEdit: {@code "50%stone,50%cobblestone"} o
 * {@code "3 oak_log, dirt"}. Permite rellenar/reemplazar con mezclas aleatorias
 * coherentes. Si no se indican pesos, se reparten por igual. Acepta tanto pesos en
 * porcentaje ({@code 50%}) como enteros ({@code 3}).
 */
public final class BlockPattern {

    private final List<BlockState> states = new ArrayList<>();
    private final List<Double> cumulative = new ArrayList<>();
    private double totalWeight;

    private BlockPattern() {
    }

    /** Parsea una cadena de patron. Devuelve {@code null} si no hay ningun bloque valido. */
    public static BlockPattern parse(HolderLookup<Block> lookup, String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        BlockPattern pattern = new BlockPattern();
        for (String part : input.split(",")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            double weight = 1.0D;
            String id = token;
            int sep = indexOfWeightSeparator(token);
            if (sep > 0) {
                String w = token.substring(0, sep).trim().replace("%", "");
                try {
                    weight = Double.parseDouble(w);
                } catch (NumberFormatException ignored) {
                    weight = 1.0D;
                }
                id = token.substring(sep + 1).trim();
            }
            if (weight <= 0.0D) {
                continue;
            }
            BlockState state = BlockStateCodec.parse(lookup, id);
            if (state == null) {
                continue;
            }
            pattern.totalWeight += weight;
            pattern.states.add(state);
            pattern.cumulative.add(pattern.totalWeight);
        }
        return pattern.states.isEmpty() ? null : pattern;
    }

    private static int indexOfWeightSeparator(String token) {
        // Separador entre peso y bloque: '%' o el primer espacio si empieza por digito.
        int pct = token.indexOf('%');
        if (pct >= 0) {
            return pct;
        }
        if (Character.isDigit(token.charAt(0))) {
            int sp = token.indexOf(' ');
            if (sp > 0) {
                return sp - 1 >= 0 ? sp : -1;
            }
        }
        return -1;
    }

    /** Elige un estado segun los pesos. */
    public BlockState pick(RandomSource rng) {
        if (states.size() == 1) {
            return states.get(0);
        }
        double r = rng.nextDouble() * totalWeight;
        for (int i = 0; i < cumulative.size(); i++) {
            if (r < cumulative.get(i)) {
                return states.get(i);
            }
        }
        return states.get(states.size() - 1);
    }

    public int size() {
        return states.size();
    }
}
