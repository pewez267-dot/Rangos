package com.fsholo.util;

/**
 * Animaciones del texto del holograma. Hay dos familias:
 *  - TEXTO COMPLETO (1..26): mueven/escalan toda la linea (compute()).
 *  - POR LETRA (27..50): desplazan cada caracter por separado (charOffset()).
 * La intensidad y velocidad son configurables. Algunas usan el indice de linea/caracter.
 */
public final class HoloAnimations {
    private static final int PERLETTER_START = 27;

    public static final String[] ANIM_NAMES = new String[]{
        "Ninguna",
        // Texto completo
        "Flotar", "Balanceo", "Pulso", "Rebote", "Latido", "Temblor", "C\u00edrculo", "Zoom", "Deslizar", "P\u00e9ndulo",
        "Figura 8", "Vaiv\u00e9n", "Salto", "Ca\u00edda Suave", "Sacudida", "Respirar", "Zigzag", "\u00d3rbita Amplia", "Tambaleo", "El\u00e1stico",
        "Deriva", "Nervioso", "Cabeceo", "Vibrar", "Rebote Doble", "Espiral",
        // Por letra
        "Ola", "Ola Inversa", "Ola Horizontal", "Rebote Letras", "Ondular", "Cascada", "M\u00e1quina Escribir", "Temblor Letras", "Salto Secuencial", "Serpiente",
        "Marea", "Latido Letras", "Resorte", "Zigzag Letras", "Aleteo", "Tornado", "Goteo", "Pulso Letras", "Vaiv\u00e9n Letras", "Onda Doble",
        "Rizado", "Sismo Letras", "Deriva Letras", "Flotar Letras",
        // Por letra (nuevas)
        "Ola Suave", "Ola Fuerte", "Ola Lenta", "Ola Doble Inversa", "Ondas Cruzadas", "Espiga", "L\u00e1tigo", "L\u00e1tigo Inverso", "Rebote Alterno", "Salto Alterno",
        "Ca\u00edda Letras", "Ca\u00edda Escalonada", "Subida Escalonada", "P\u00e9ndulo Letras", "C\u00edrculo Letras", "Elipse Letras", "Remolino", "Espiral Letras", "Vibraci\u00f3n Fina", "Vibraci\u00f3n Fuerte",
        "Nervio", "Tic", "Rebote Suave", "Flotaci\u00f3n Lenta", "Deriva Vertical", "Deriva Diagonal", "Balanceo Letras", "Mecer", "Empuje", "Tir\u00f3n",
        "Ondulaci\u00f3n R\u00e1pida", "Serpiente R\u00e1pida", "Cola de Sirena", "Bandera", "Bandera Vertical", "Latido Doble", "Coraz\u00f3n Letras", "P\u00e1lpito", "Escalera", "Cascada Inversa",
        "Goteo R\u00e1pido", "Rebote Cascada", "Zigzag Doble", "Cruz", "Diagonal", "Vaiv\u00e9n Diagonal", "Temblor Suave", "Sacudida Letras", "Rizo Fino", "Onda Triple"
    };
    public static final String[] SPEED_NAMES = new String[]{"Lenta", "Normal", "R\u00e1pida", "Muy R\u00e1pida"};
    public static final String[] INTENSITY_NAMES = new String[]{"Sutil", "Normal", "Fuerte"};
    private static final float[] SPEED_MULT = new float[]{0.5f, 1.0f, 1.8f, 2.8f};
    private static final float[] AMP = new float[]{0.12f, 0.28f, 0.55f};

    private HoloAnimations() {
    }

    public static int count() {
        return ANIM_NAMES.length;
    }

    public static String name(int i) {
        return ANIM_NAMES[Math.floorMod(i, ANIM_NAMES.length)];
    }

    public static boolean isPerLetter(int anim) {
        return anim >= PERLETTER_START && anim < ANIM_NAMES.length;
    }

    public static int speedCount() {
        return SPEED_NAMES.length;
    }

    public static String speedName(int i) {
        return SPEED_NAMES[Math.floorMod(i, SPEED_NAMES.length)];
    }

    public static int intensityCount() {
        return INTENSITY_NAMES.length;
    }

    public static String intensityName(int i) {
        return INTENSITY_NAMES[Math.floorMod(i, INTENSITY_NAMES.length)];
    }

    private static int clampIdx(int v, int hi) {
        return v < 0 ? 0 : (v > hi ? hi : v);
    }

    private static float speed(int i) {
        return SPEED_MULT[clampIdx(i, SPEED_MULT.length - 1)];
    }

    private static float amp(int i) {
        return AMP[clampIdx(i, AMP.length - 1)];
    }

    /** Transformacion de TODA la linea: out[0]=offset horizontal, out[1]=offset vertical (bloques), out[2]=escala. */
    public static void compute(int anim, float time, int speedIdx, int intensityIdx, int lineIndex, int lineCount, float[] out) {
        out[0] = 0.0f;
        out[1] = 0.0f;
        out[2] = 1.0f;
        if (anim <= 0 || anim >= PERLETTER_START) {
            return;
        }
        float t = time * speed(speedIdx);
        float a = amp(intensityIdx);
        switch (anim) {
            case 1: out[1] = (float) Math.sin(t * 2.0) * a; break;
            case 2: out[0] = (float) Math.sin(t * 2.0) * a; break;
            case 3: out[2] = 1.0f + (float) Math.sin(t * 3.0) * a * 0.35f; break;
            case 4: out[1] = Math.abs((float) Math.sin(t * 2.2)) * a * 1.4f; break;
            case 5: { float base = (float) Math.max(0.0, Math.sin(t * 2.0)); float hb = base * base * base * base; out[2] = 1.0f + hb * a * 0.7f; break; }
            case 6: out[0] = (float) Math.sin(t * 40.0) * a * 0.4f; out[1] = (float) Math.sin(t * 47.0 + 1.3) * a * 0.4f; break;
            case 7: out[0] = (float) Math.cos(t * 2.0) * a; out[1] = (float) Math.sin(t * 2.0) * a; break;
            case 8: out[2] = 1.0f + (float) Math.sin(t * 2.5) * a * 0.6f; break;
            case 9: out[0] = (float) Math.sin(t * 1.6) * a * 1.6f; break;
            case 10: out[0] = (float) Math.sin(t * 1.5) * a * 1.3f; break;
            case 11: out[0] = (float) Math.sin(t * 2.0) * a; out[1] = (float) Math.sin(t * 4.0) * a * 0.5f; break;
            case 12: out[0] = (float) Math.sin(t * 3.0) * a; break;
            case 13: out[1] = Math.abs((float) Math.sin(t * 1.8)) * a * 1.8f; break;
            case 14: out[1] = -Math.abs((float) Math.sin(t * 1.5)) * a; break;
            case 15: out[0] = (float) Math.sin(t * 30.0) * a * 0.6f; break;
            case 16: out[2] = 1.0f + (float) Math.sin(t * 1.2) * a * 0.4f; break;
            case 17: out[0] = (float) Math.asin(Math.sin(t * 2.5)) * a * 0.9f; break;
            case 18: out[0] = (float) Math.cos(t * 1.5) * a * 1.5f; out[1] = (float) Math.sin(t * 1.5) * a * 1.5f; break;
            case 19: out[0] = (float) Math.sin(t * 2.0) * a; out[2] = 1.0f + (float) Math.sin(t * 2.0) * a * 0.15f; break;
            case 20: out[2] = 1.0f + Math.abs((float) Math.sin(t * 2.0)) * a * 0.5f; break;
            case 21: out[0] = (float) Math.sin(t * 0.8) * a * 1.4f; break;
            case 22: out[0] = (float) Math.sin(t * 50.0) * a * 0.3f; out[1] = (float) Math.cos(t * 55.0) * a * 0.3f; break;
            case 23: out[1] = (float) Math.sin(t * 1.2) * a; break;
            case 24: out[1] = (float) Math.sin(t * 35.0) * a * 0.4f; break;
            case 25: out[1] = Math.abs((float) Math.sin(t * 3.0)) * a * 1.2f; break;
            case 26: out[0] = (float) Math.cos(t * 3.0) * a; out[1] = (float) Math.sin(t * 3.0) * a; out[2] = 1.0f + (float) Math.sin(t) * a * 0.2f; break;
            default: break;
        }
        if (out[2] < 0.1f) {
            out[2] = 0.1f;
        }
    }

    /** Desplazamiento por CARACTER en pixeles de fuente: out[0]=dx, out[1]=dy. out[1]=NaN significa ocultar. */
    public static void charOffset(int anim, float time, int speedIdx, int intensityIdx, int charIndex, int charCount, float[] out) {
        out[0] = 0.0f;
        out[1] = 0.0f;
        if (!isPerLetter(anim)) {
            return;
        }
        float t = time * speed(speedIdx);
        float A = amp(intensityIdx) * 30.0f;
        float ci = (float) charIndex;
        float cc = (float) Math.max(1, charCount);
        switch (anim) {
            case 27: out[1] = (float) Math.sin(t * 2.0 + ci * 0.5) * A; break;
            case 28: out[1] = (float) Math.sin(t * 2.0 - ci * 0.5) * A; break;
            case 29: out[0] = (float) Math.sin(t * 2.0 + ci * 0.5) * A * 0.5f; break;
            case 30: out[1] = -Math.abs((float) Math.sin(t * 2.0 + ci * 0.6)) * A * 1.3f; break;
            case 31: out[1] = (float) Math.sin(t * 3.0 + ci * 0.9) * A; break;
            case 32: out[1] = (float) Math.sin(t * 2.0 - ci * 0.4) * A; break;
            case 33: {
                int period = charCount + 8;
                int revealed = (int) (t * 4.0) % period;
                if (charIndex >= revealed) {
                    out[1] = Float.NaN;
                }
                break;
            }
            case 34: out[0] = (float) Math.sin(t * 30.0 + ci * 13.0) * A * 0.5f; out[1] = (float) Math.sin(t * 34.0 + ci * 7.0) * A * 0.5f; break;
            case 35: {
                float local = (float) (t * 2.0 % (double) Math.max(1, charCount));
                float d = Math.abs(local - ci);
                out[1] = d < 1.0f ? -(1.0f - d) * A * 1.6f : 0.0f;
                break;
            }
            case 36: out[1] = (float) Math.sin(t * 3.0 + ci * 0.6) * A; out[0] = (float) Math.cos(t * 3.0 + ci * 0.6) * A * 0.4f; break;
            case 37: out[1] = (float) Math.sin(t * 1.5 + ci * 0.3) * A; break;
            case 38: out[1] = -Math.abs((float) Math.sin(t * 3.0 + ci * 0.2)) * A * 0.6f; break;
            case 39: out[1] = (float) Math.sin(t * 4.0 + ci * 0.5) * A; break;
            case 40: out[1] = (charIndex % 2 == 0 ? 1.0f : -1.0f) * (float) Math.sin(t * 2.0) * A; break;
            case 41: out[0] = (float) Math.sin(t * 8.0 + ci) * A * 0.3f; out[1] = (float) Math.cos(t * 8.0 + ci) * A * 0.3f; break;
            case 42: out[0] = (float) Math.sin(t * 3.0 + ci * 0.5) * A; out[1] = (float) Math.sin(t * 3.0 + ci * 0.5 + 1.5) * A * 0.5f; break;
            case 43: { float f = (float) ((t + ci * 0.5) % 2.0); out[1] = f < 1.0f ? f * A : (2.0f - f) * A; break; }
            case 44: out[1] = (float) Math.sin(t * 3.0 + ci * 0.4) * A * 0.5f; break;
            case 45: out[0] = (float) Math.sin(t * 2.0 + ci * 0.3) * A; break;
            case 46: out[1] = ((float) Math.sin(t * 2.0 + ci * 0.5) + (float) Math.sin(t * 3.0 + ci * 0.3)) * A * 0.5f; break;
            case 47: out[1] = (float) Math.sin(t * 4.0 + ci * 1.2) * A * 0.7f; break;
            case 48: out[0] = (float) Math.sin(t * 25.0 + ci * 7.0) * A * 0.5f; out[1] = (float) Math.sin(t * 29.0 + ci * 5.0) * A * 0.5f; break;
            case 49: out[0] = (float) Math.sin(t * 0.8 + ci * 0.2) * A * 1.2f; break;
            case 50: out[1] = (float) Math.sin(t * 1.2 + ci * 0.4) * A; break;
            case 51: out[1] = (float) Math.sin(t * 1.5 + ci * 0.4) * A * 0.7f; break;
            case 52: out[1] = (float) Math.sin(t * 2.5 + ci * 0.6) * A * 1.3f; break;
            case 53: out[1] = (float) Math.sin(t * 0.8 + ci * 0.3) * A; break;
            case 54: out[1] = ((float) Math.sin(t * 2.0 + ci * 0.5) - (float) Math.sin(t * 3.0 - ci * 0.4)) * A * 0.5f; break;
            case 55: out[1] = (float) Math.sin(t * 2.0 + ci * 0.5) * A * 0.6f; out[0] = (float) Math.cos(t * 2.0 + ci * 0.5) * A * 0.6f; break;
            case 56: out[1] = -Math.abs((float) Math.sin(t * 2.0 + ci * 0.7)) * A; break;
            case 57: out[1] = (float) Math.sin(t * 3.0 + ci) * A * ((ci + 1.0f) / cc); break;
            case 58: out[1] = (float) Math.sin(t * 3.0 + ci) * A * ((cc - ci) / cc); break;
            case 59: out[1] = (charIndex % 2 == 0 ? 1.0f : -1.0f) * Math.abs((float) Math.sin(t * 2.0)) * A; break;
            case 60: out[1] = (charIndex % 2 == 0 ? -1.0f : 0.0f) * Math.abs((float) Math.sin(t * 2.0 + ci)) * A * 1.4f; break;
            case 61: out[1] = Math.abs((float) Math.sin(t * 1.5 + ci * 0.5)) * A; break;
            case 62: out[1] = (float) Math.sin(t * 2.0 - ci * 0.5) * A; break;
            case 63: out[1] = -(float) Math.sin(t * 2.0 - ci * 0.5) * A; break;
            case 64: out[0] = (float) Math.sin(t * 1.5 + ci * 0.2) * A; break;
            case 65: out[0] = (float) Math.cos(t * 2.0 + ci * 0.4) * A * 0.6f; out[1] = (float) Math.sin(t * 2.0 + ci * 0.4) * A * 0.6f; break;
            case 66: out[0] = (float) Math.cos(t * 2.0 + ci * 0.4) * A; out[1] = (float) Math.sin(t * 2.0 + ci * 0.4) * A * 0.5f; break;
            case 67: out[0] = (float) Math.cos(t * 3.0 + ci * 0.8) * A * 0.5f; out[1] = (float) Math.sin(t * 3.0 + ci * 0.8) * A * 0.5f; break;
            case 68: { float r = A * (0.4f + 0.6f * ((float) (charIndex % 5) / 5.0f)); out[0] = (float) Math.cos(t * 3.0) * r; out[1] = (float) Math.sin(t * 3.0) * r; break; }
            case 69: out[1] = (float) Math.sin(t * 40.0 + ci * 5.0) * A * 0.25f; break;
            case 70: out[1] = (float) Math.sin(t * 45.0 + ci * 7.0) * A * 0.5f; out[0] = (float) Math.cos(t * 43.0 + ci * 3.0) * A * 0.3f; break;
            case 71: out[0] = (float) Math.sin(t * 35.0 + ci * 11.0) * A * 0.3f; break;
            case 72: out[1] = ((int) (t * 4.0 + ci) % 5 == 0) ? -A * 0.6f : 0.0f; break;
            case 73: out[1] = -Math.abs((float) Math.sin(t * 1.6 + ci * 0.3)) * A * 0.8f; break;
            case 74: out[1] = (float) Math.sin(t * 1.0 + ci * 0.25) * A * 0.8f; break;
            case 75: out[1] = (float) Math.sin(t * 0.7 + ci * 0.15) * A * 1.1f; break;
            case 76: out[0] = (float) Math.sin(t * 0.8 + ci * 0.2) * A * 0.8f; out[1] = (float) Math.sin(t * 0.8 + ci * 0.2) * A * 0.8f; break;
            case 77: out[0] = (float) Math.sin(t * 1.8 + ci * 0.35) * A; break;
            case 78: out[0] = (float) Math.sin(t * 1.2 + ci * 0.4) * A * 1.2f; out[1] = (float) Math.cos(t * 1.2 + ci * 0.4) * A * 0.3f; break;
            case 79: out[0] = (float) Math.sin(t * 2.2 + ci * 0.5) * A; break;
            case 80: out[0] = (Math.abs((float) Math.sin(t * 2.0)) - 0.5f) * A * ((ci + 1.0f) / cc) * 2.0f; break;
            case 81: out[1] = (float) Math.sin(t * 4.0 + ci * 0.7) * A * 0.8f; break;
            case 82: out[1] = (float) Math.sin(t * 4.0 + ci * 0.7) * A; out[0] = (float) Math.cos(t * 4.0 + ci * 0.7) * A * 0.4f; break;
            case 83: out[1] = (float) Math.sin(t * 2.0 + ci * 0.5) * A * (ci / cc); break;
            case 84: out[1] = (float) Math.sin(t * 3.0 + ci * 0.9) * A * Math.min(1.0f, ci * 0.3f); break;
            case 85: out[0] = (float) Math.sin(t * 3.0 + ci * 0.9) * A * Math.min(1.0f, ci * 0.3f); break;
            case 86: { float p = Math.abs((float) Math.sin(t * 3.0)); out[1] = -(p * p) * A * 0.9f; break; }
            case 87: out[1] = -Math.abs((float) Math.sin(t * 2.5 + ci * 0.3)) * A * 0.7f; break;
            case 88: out[1] = ((float) Math.sin(t * 5.0) > 0.6f) ? -A * 0.7f : 0.0f; break;
            case 89: out[1] = (float) ((charIndex % 3) - 1) * (float) Math.sin(t * 2.0) * A; break;
            case 90: out[1] = (float) Math.sin(t * 2.0 + ci * 0.4) * A; break;
            case 91: { float f = (float) ((t * 2.0 + ci * 0.6) % 2.0); out[1] = f < 1.0f ? f * A : (2.0f - f) * A; break; }
            case 92: out[1] = -Math.abs((float) Math.sin(t * 3.0 - ci * 0.5)) * A; break;
            case 93: out[1] = (charIndex % 2 == 0 ? 1.0f : -1.0f) * (float) Math.sin(t * 3.0) * A; out[0] = (charIndex % 2 == 0 ? -1.0f : 1.0f) * (float) Math.cos(t * 3.0) * A * 0.4f; break;
            case 94: out[0] = (float) Math.sin(t * 2.0 + ci) * A * 0.5f; out[1] = (float) Math.cos(t * 2.0 + ci) * A * 0.5f; break;
            case 95: out[0] = (float) Math.sin(t * 2.0 + ci * 0.4) * A * 0.7f; out[1] = (float) Math.sin(t * 2.0 + ci * 0.4) * A * 0.7f; break;
            case 96: out[0] = (float) Math.sin(t * 1.6 + ci * 0.3) * A; out[1] = -(float) Math.sin(t * 1.6 + ci * 0.3) * A * 0.6f; break;
            case 97: out[0] = (float) Math.sin(t * 20.0 + ci * 9.0) * A * 0.25f; out[1] = (float) Math.cos(t * 22.0 + ci * 6.0) * A * 0.25f; break;
            case 98: out[0] = (float) Math.sin(t * 28.0 + ci * 13.0) * A * 0.6f; break;
            case 99: out[1] = (float) Math.sin(t * 5.0 + ci * 1.5) * A * 0.5f; break;
            case 100: out[1] = ((float) Math.sin(t * 2.0 + ci * 0.5) + (float) Math.sin(t * 3.0 + ci * 0.3) + (float) Math.sin(t * 4.0 + ci * 0.7)) * A * 0.33f; break;
            default: break;
        }
    }
}
