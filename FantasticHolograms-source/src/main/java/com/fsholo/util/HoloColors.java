/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 */
package com.fsholo.util;

import java.awt.Color;
import java.util.Locale;
import net.minecraft.ChatFormatting;

public final class HoloColors {
    private static final int[][] RAINBOW_PALETTES = new int[][]{{0xFF2200, 0xFF6600, 0xFFAA00, 0xFFEE00}, {17663, 39423, 58879, 65484}, {0xFF00FF, 65535, 65382, 0xFFFF00}, {0xFF3366, 0xFF66AA, 0xFFAA55, 0xFFDD66}, {3066944, 0x88DD22, 0xDDEE33, 0xFFFFAA}, {0xFF66CC, 0xCC66FF, 0xFF99DD, 0xEE55AA}, {0xFFFFFF, 0xAADDFF, 0x66CCFF, 0x99FFFF}, {0x6600FF, 0xCC00FF, 0x3366FF, 0xEE99FF}, {0x330000, 0x990000, 0xFF3300, 0xFF9900, 0xFFDD00}, {65433, 0x66FFCC, 0xCCFFEE, 0x33DDAA}, {0x330066, 0x6600CC, 0x9933FF, 0xCC99FF}, {6044193, 10511646, 14721088, 16769184}, {65416, 0xFFEE00, 0xFF3399, 52479}, {0xFFFFFF, 0xBBCCDD, 0x8899AA, 0xCCDDEE}, {16766700, 0xFF99CC, 0xFF66AA, 0xFFFFFF}, {0xFF00AA, 65518, 0xFFEE00, 0xAA00FF}, {9118976, 0xCC5500, 0xFF9933, 0xFFCC33}, {65450, 0x33CCFF, 0x9966FF, 65382}, {0x660011, 0xCC0033, 0xFF3366, 0xFF99AA}, {17442, 43605, 0x33DD88, 0xAAFFCC}, {6758, 17612, 0x3388FF, 0x99CCFF}, {0x330044, 0x8800CC, 0xCC66FF, 0xEEBBFF}, {16740814, 118270, 12150783, 393121}, {0x1A0000, 0x660000, 0xCC0000, 0xFF3333, 0xFF8888}, {0xFF0000, 0xFFFF00, 0x00FF00, 0x00FFFF, 0x0000FF, 0xFF00FF}, {0x8B6914, 0xD4AF37, 0xFFD700, 0xFFF8DC}, {0x707070, 0xA0A0A0, 0xD0D0D0, 0xFFFFFF}, {0x000066, 0x0044FF, 0x00CCFF, 0xCCFFFF}, {0x003300, 0x33CC00, 0x88FF00, 0xCCFF66}, {0xFF66CC, 0xFF99DD, 0xFFCCEE, 0xFFFFFF}, {0x0066FF, 0x66AAFF, 0xAADDFF, 0xFFFFFF}, {0x330000, 0x991100, 0xFF4400, 0xFFAA00, 0xFFEE66}, {0x0B3D0B, 0x2E7D32, 0x66BB6A, 0xC8E6C9}, {0xFF6F61, 0xFF9478, 0xFFB199, 0xFFD9CC}, {0x006666, 0x00AAAA, 0x33DDDD, 0xAAFFFF}, {0x6A4C93, 0x9B72CF, 0xC3A6E8, 0xE8D9FF}, {0xFF9966, 0xFFB380, 0xFFCCA3, 0xFFE6CC}, {0x1A0033, 0x4B0082, 0x7B2FBF, 0xB266FF}, {0x999900, 0xCCCC00, 0xFFFF33, 0xFFFF99}, {0x4D0011, 0x990022, 0xE60039, 0xFF6688}, {0x006644, 0x00AA77, 0x55DDAA, 0xAAFFDD}, {0x5C2E00, 0xB87333, 0xE39A5C, 0xFFCC99}, {0x000080, 0x0033CC, 0x3366FF, 0x99BBFF}, {0x800040, 0xCC0066, 0xFF3399, 0xFF99CC}, {0x336600, 0x66CC00, 0xAAFF00, 0xDDFF88}, {0x993300, 0xFF6600, 0xFF9933, 0xFFCC66}, {0x2E0854, 0x6A0DAD, 0x9D4EDD, 0xD0A6FF}, {0xFF0080, 0xFF33AA, 0xFF66CC, 0xFF99DD}, {0x1B4D3E, 0x2E8B57, 0x66CDAA, 0xB0E0D0}, {0x663300, 0xCC6600, 0xFFAA00, 0xFFDD66}, {0x003366, 0x0088CC, 0x66CCFF, 0xCCF2FF}, {0x006644, 0x33AA55, 0xFF3366, 0xFF88AA}, {0x1A1A40, 0x3D2C8D, 0x916BBF, 0xD4A5FF}, {0x660033, 0xCC0055, 0xFF3388, 0xFFAACC}, {0x4D6600, 0x99CC00, 0xCCFF33, 0xEEFF99}, {0x000033, 0x003366, 0x0066AA, 0x3399CC, 0x88CCEE}};
    private static final String[] RAINBOW_NAMES = new String[]{"Cl\u00e1sico", "Fuego", "Oc\u00e9ano", "Ne\u00f3n", "Atardecer", "Bosque", "Chicle", "Hielo", "Galaxia", "Lava", "Menta", "Uva", "Caramelo", "Tropical", "Fantasma", "Sakura", "Cyberpunk", "Oto\u00f1o", "Aurora", "Rub\u00ed", "Esmeralda", "Zafiro", "Amatista", "Vaporwave", "Sangre", "Arco\u00edris Ne\u00f3n", "Dorado", "Plata", "Fuego Azul", "Veneno", "Rosa Chicle", "Cielo", "Magma", "Selva", "Coral", "Turquesa", "Lavanda", "Melocot\u00f3n", "\u00cdndigo", "Lim\u00f3n", "Cereza", "Menta Fresca", "Cobre", "Azul Real", "Fucsia", "Verde \u00c1cido", "Naranja Sol", "P\u00farpura Real", "Rosa Ne\u00f3n", "Verde Bosque", "\u00c1mbar", "Hielo Azul", "Sand\u00eda", "Crep\u00fasculo", "Rub\u00ed Rosa", "Verde Lima", "Oc\u00e9ano Profundo"};

    private HoloColors() {
    }

    public static int parse(String s, int def) {
        if (s == null || s.isEmpty()) {
            return def;
        }
        String v = s.trim();
        try {
            if (v.startsWith("#")) {
                return 0xFFFFFF & Integer.parseInt(v.substring(1), 16);
            }
            ChatFormatting fmt = ChatFormatting.getByName((String)v.toLowerCase(Locale.ROOT));
            if (fmt != null && fmt.getColor() != null) {
                return fmt.getColor();
            }
            return 0xFFFFFF & Integer.parseInt(v, 16);
        }
        catch (NumberFormatException e) {
            return def;
        }
    }

    public static int lerp(int from, int to, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int fr = from >> 16 & 0xFF;
        int fg = from >> 8 & 0xFF;
        int fb = from & 0xFF;
        int tr = to >> 16 & 0xFF;
        int tg = to >> 8 & 0xFF;
        int tb = to & 0xFF;
        int r = Math.round((float)fr + (float)(tr - fr) * t);
        int g = Math.round((float)fg + (float)(tg - fg) * t);
        int b = Math.round((float)fb + (float)(tb - fb) * t);
        return r << 16 | g << 8 | b;
    }

    public static int rainbow(float hue) {
        return 0xFFFFFF & Color.HSBtoRGB(hue - (float)Math.floor(hue), 0.9f, 1.0f);
    }

    public static int rainbowStyleCount() {
        return RAINBOW_NAMES.length;
    }

    public static String rainbowStyleName(int style) {
        style = (style % RAINBOW_NAMES.length + RAINBOW_NAMES.length) % RAINBOW_NAMES.length;
        return RAINBOW_NAMES[style];
    }

    public static int rainbowColor(int style, float pos, float time) {
        if (style <= 0) {
            return HoloColors.rainbow(pos + time);
        }
        int[] pal = RAINBOW_PALETTES[(style - 1) % RAINBOW_PALETTES.length];
        float x = pos + time;
        x -= (float)Math.floor(x);
        float scaled = x * (float)pal.length;
        int i = (int)Math.floor(scaled) % pal.length;
        int j = (i + 1) % pal.length;
        float f = scaled - (float)Math.floor(scaled);
        return HoloColors.lerp(pal[i], pal[j], f);
    }

    public static int hsbToRgb(float h, float s, float b) {
        return 0xFFFFFF & Color.HSBtoRGB(h, s, b);
    }

    public static float[] rgbToHsb(int rgb) {
        return Color.RGBtoHSB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, null);
    }

    public static String toHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    public static String amp(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; ++i) {
            if (chars[i] == '&' && i + 1 < chars.length && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(chars[i + 1]) >= 0) {
                sb.append('\u00a7');
                continue;
            }
            sb.append(chars[i]);
        }
        return sb.toString();
    }

    public static String strip(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("[&\u00a7][0-9A-Fa-fK-Ok-or]", "");
    }
}

