package com.fantasticterraform.intelligent.dungeon;

import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;

/**
 * Valida si la seleccion activa cumple el tamano minimo de un tier ANTES de generar.
 * Si no cumple, devuelve la cifra exacta que falta (nunca un mensaje generico).
 */
public final class DungeonSizeValidator {

    public static final class Result {
        public final boolean ok;
        public final String message;

        Result(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
    }

    private DungeonSizeValidator() {
    }

    public static Result validate(SelectionShape sel, DungeonSizeRequirement req) {
        if (sel == null) {
            return new Result(false, "No tienes una seleccion activa valida.");
        }
        BlockPos min = sel.getMin();
        BlockPos max = sel.getMax();
        int w = max.getX() - min.getX() + 1;
        int h = max.getY() - min.getY() + 1;
        int l = max.getZ() - min.getZ() + 1;
        long volume = sel.getVolume();

        StringBuilder faltan = new StringBuilder();
        if (w < req.minWidth) {
            faltan.append(" ancho +").append(req.minWidth - w);
        }
        if (h < req.minHeight) {
            faltan.append(" alto +").append(req.minHeight - h);
        }
        if (l < req.minLength) {
            faltan.append(" largo +").append(req.minLength - l);
        }
        if (volume < req.minVolume) {
            faltan.append(" volumen +").append(req.minVolume - volume);
        }

        if (faltan.length() == 0) {
            return new Result(true, "Seleccion " + w + "x" + h + "x" + l + " OK para tier " + req.tier.displayName() + ".");
        }
        String msg = "Tu seleccion es " + w + "x" + h + "x" + l + " (vol " + volume + "). "
                + "El tier " + req.tier.displayName() + " requiere minimo "
                + req.minWidth + "x" + req.minHeight + "x" + req.minLength + " y vol " + req.minVolume + "."
                + " Faltan:" + faltan + ". Agranda tu seleccion.";
        return new Result(false, msg);
    }
}
