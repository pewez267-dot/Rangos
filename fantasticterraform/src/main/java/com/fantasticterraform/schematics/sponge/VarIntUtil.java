package com.fantasticterraform.schematics.sponge;

import java.io.ByteArrayOutputStream;

/**
 * Codificacion/decodificacion VarInt (LEB128 sin signo) tal como la usa el campo
 * {@code BlockData} de la especificacion Sponge Schematic.
 */
public final class VarIntUtil {

    private VarIntUtil() {
    }

    /** Decodifica {@code count} varints del array a partir del offset 0. */
    public static int[] readAll(byte[] data, int count) {
        int[] out = new int[count];
        int index = 0;
        int pos = 0;
        while (index < count && pos < data.length) {
            int value = 0;
            int shift = 0;
            byte b;
            do {
                b = data[pos++];
                value |= (b & 0x7F) << shift;
                shift += 7;
                if (shift > 35) {
                    throw new IllegalArgumentException("VarInt demasiado largo en BlockData");
                }
            } while ((b & 0x80) != 0 && pos < data.length);
            out[index++] = value;
        }
        return out;
    }

    /** Codifica un solo valor en el stream. */
    public static void write(ByteArrayOutputStream out, int value) {
        int v = value;
        while ((v & 0xFFFFFF80) != 0) {
            out.write((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        out.write(v & 0x7F);
    }
}
