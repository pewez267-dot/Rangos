package com.fsholo.util;

/**
 * Animaciones del texto del holograma. Cada animacion produce un desplazamiento horizontal
 * (en el plano del texto), vertical y/o un multiplicador de escala, en funcion del tiempo,
 * la velocidad y la intensidad. Algunas (Ola, Saltos) usan el indice de linea para efectos en cascada.
 */
public final class HoloAnimations {
    public static final String[] ANIM_NAMES = new String[]{
        "Ninguna", "Flotar", "Balanceo", "Pulso", "Rebote", "Latido", "Temblor", "Ola", "C\u00edrculo", "Zoom", "Deslizar", "Saltos"
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

    private static int clamp(int v, int hi) {
        return v < 0 ? 0 : (v > hi ? hi : v);
    }

    /**
     * @param out out[0]=offset horizontal (plano texto), out[1]=offset vertical, out[2]=multiplicador de escala
     */
    public static void compute(int anim, float time, int speedIdx, int intensityIdx, int lineIndex, int lineCount, float[] out) {
        out[0] = 0.0f;
        out[1] = 0.0f;
        out[2] = 1.0f;
        if (anim <= 0) {
            return;
        }
        float speed = SPEED_MULT[clamp(speedIdx, SPEED_MULT.length - 1)];
        float amp = AMP[clamp(intensityIdx, AMP.length - 1)];
        float t = time * speed;
        switch (anim) {
            case 1:
                out[1] = (float) Math.sin(t * 2.0) * amp;
                break;
            case 2:
                out[0] = (float) Math.sin(t * 2.0) * amp;
                break;
            case 3:
                out[2] = 1.0f + (float) Math.sin(t * 3.0) * amp * 0.35f;
                break;
            case 4:
                out[1] = Math.abs((float) Math.sin(t * 2.2)) * amp * 1.4f;
                break;
            case 5: {
                float base = (float) Math.max(0.0, Math.sin(t * 2.0));
                float hb = base * base * base * base;
                out[2] = 1.0f + hb * amp * 0.7f;
                break;
            }
            case 6:
                out[0] = (float) Math.sin(t * 40.0) * amp * 0.4f;
                out[1] = (float) Math.sin(t * 47.0 + 1.3) * amp * 0.4f;
                break;
            case 7:
                out[1] = (float) Math.sin(t * 2.0 + (double) lineIndex * 0.7) * amp;
                break;
            case 8:
                out[0] = (float) Math.cos(t * 2.0) * amp;
                out[1] = (float) Math.sin(t * 2.0) * amp;
                break;
            case 9:
                out[2] = 1.0f + (float) Math.sin(t * 2.5) * amp * 0.6f;
                break;
            case 10:
                out[0] = (float) Math.sin(t * 1.6) * amp * 1.6f;
                break;
            case 11:
                out[1] = Math.abs((float) Math.sin(t * 1.8 + (double) lineIndex * 0.6)) * amp * 1.3f;
                break;
            default:
                break;
        }
        if (out[2] < 0.1f) {
            out[2] = 0.1f;
        }
    }
}
