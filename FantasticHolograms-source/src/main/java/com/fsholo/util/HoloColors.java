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
    private static final int[][] RAINBOW_PALETTES = new int[][]{{0xFF2200, 0xFF6600, 0xFFAA00, 0xFFEE00}, {17663, 39423, 58879, 65484}, {0xFF00FF, 65535, 65382, 0xFFFF00}, {0xFF3366, 0xFF66AA, 0xFFAA55, 0xFFDD66}, {3066944, 0x88DD22, 0xDDEE33, 0xFFFFAA}, {0xFF66CC, 0xCC66FF, 0xFF99DD, 0xEE55AA}, {0xFFFFFF, 0xAADDFF, 0x66CCFF, 0x99FFFF}, {0x6600FF, 0xCC00FF, 0x3366FF, 0xEE99FF}, {0x330000, 0x990000, 0xFF3300, 0xFF9900, 0xFFDD00}, {65433, 0x66FFCC, 0xCCFFEE, 0x33DDAA}, {0x330066, 0x6600CC, 0x9933FF, 0xCC99FF}, {6044193, 10511646, 14721088, 16769184}, {65416, 0xFFEE00, 0xFF3399, 52479}, {0xFFFFFF, 0xBBCCDD, 0x8899AA, 0xCCDDEE}, {16766700, 0xFF99CC, 0xFF66AA, 0xFFFFFF}, {0xFF00AA, 65518, 0xFFEE00, 0xAA00FF}, {9118976, 0xCC5500, 0xFF9933, 0xFFCC33}, {65450, 0x33CCFF, 0x9966FF, 65382}, {0x660011, 0xCC0033, 0xFF3366, 0xFF99AA}, {17442, 43605, 0x33DD88, 0xAAFFCC}, {6758, 17612, 0x3388FF, 0x99CCFF}, {0x330044, 0x8800CC, 0xCC66FF, 0xEEBBFF}, {16740814, 118270, 12150783, 393121}, {0x1A0000, 0x660000, 0xCC0000, 0xFF3333, 0xFF8888}, {0xFF0000, 0xFFFF00, 0x00FF00, 0x00FFFF, 0x0000FF, 0xFF00FF}, {0x8B6914, 0xD4AF37, 0xFFD700, 0xFFF8DC}, {0x707070, 0xA0A0A0, 0xD0D0D0, 0xFFFFFF}, {0x000066, 0x0044FF, 0x00CCFF, 0xCCFFFF}, {0x003300, 0x33CC00, 0x88FF00, 0xCCFF66}, {0xFF66CC, 0xFF99DD, 0xFFCCEE, 0xFFFFFF}, {0x0066FF, 0x66AAFF, 0xAADDFF, 0xFFFFFF}, {0x330000, 0x991100, 0xFF4400, 0xFFAA00, 0xFFEE66}, {0x0B3D0B, 0x2E7D32, 0x66BB6A, 0xC8E6C9}, {0xFF6F61, 0xFF9478, 0xFFB199, 0xFFD9CC}, {0x006666, 0x00AAAA, 0x33DDDD, 0xAAFFFF}, {0x6A4C93, 0x9B72CF, 0xC3A6E8, 0xE8D9FF}, {0xFF9966, 0xFFB380, 0xFFCCA3, 0xFFE6CC}, {0x1A0033, 0x4B0082, 0x7B2FBF, 0xB266FF}, {0x999900, 0xCCCC00, 0xFFFF33, 0xFFFF99}, {0x4D0011, 0x990022, 0xE60039, 0xFF6688}, {0x006644, 0x00AA77, 0x55DDAA, 0xAAFFDD}, {0x5C2E00, 0xB87333, 0xE39A5C, 0xFFCC99}, {0x000080, 0x0033CC, 0x3366FF, 0x99BBFF}, {0x800040, 0xCC0066, 0xFF3399, 0xFF99CC}, {0x336600, 0x66CC00, 0xAAFF00, 0xDDFF88}, {0x993300, 0xFF6600, 0xFF9933, 0xFFCC66}, {0x2E0854, 0x6A0DAD, 0x9D4EDD, 0xD0A6FF}, {0xFF0080, 0xFF33AA, 0xFF66CC, 0xFF99DD}, {0x1B4D3E, 0x2E8B57, 0x66CDAA, 0xB0E0D0}, {0x663300, 0xCC6600, 0xFFAA00, 0xFFDD66}, {0x003366, 0x0088CC, 0x66CCFF, 0xCCF2FF}, {0x006644, 0x33AA55, 0xFF3366, 0xFF88AA}, {0x1A1A40, 0x3D2C8D, 0x916BBF, 0xD4A5FF}, {0x660033, 0xCC0055, 0xFF3388, 0xFFAACC}, {0x4D6600, 0x99CC00, 0xCCFF33, 0xEEFF99}, {0x000033, 0x003366, 0x0066AA, 0x3399CC, 0x88CCEE}, {0x39FF14, 0x7CFF50, 0xB6FF9E}, {0xFF10F0, 0xFF5FF5, 0xFFA6FA}, {0xFF5F6D, 0xFFC371, 0xFFF17A}, {0x00FFB3, 0x00B3FF, 0x8A2BE2}, {0x00E5FF, 0x2979FF, 0x651FFF}, {0x004D40, 0x00897B, 0x4DB6AC, 0xB2DFDB}, {0xFFB6E1, 0xB6E0FF, 0xFFF7B6}, {0x000000, 0x4D0000, 0xB30000, 0xFF6600, 0xFFCC00}, {0xE0FFFF, 0xAFEEEE, 0x7FFFD4, 0xE0FFFF}, {0x2A0845, 0x6441A5, 0xB24592, 0xF15F79}, {0x7A3B00, 0xC46210, 0xE8A020, 0xF5D76E}, {0xFFDE00, 0xFF9A00, 0xFF5252}, {0xE0F7FF, 0x9BD3E8, 0x4A90C2, 0x2C5F8A}, {0xF9F871, 0xB6E388, 0x6FCF97, 0xF7A6C4}, {0x0FF0FC, 0xFF00E5, 0xFFE600}, {0x3E2723, 0x795548, 0xA1887F, 0xD7CCC8}, {0x004D25, 0x00A651, 0x39E97C, 0xB6FFD6}, {0x001A4D, 0x0047AB, 0x3A7BD5, 0xA6D0FF}, {0x141E30, 0x243B55, 0xF6A192, 0xFFE0C7}, {0xB76E79, 0xE0A899, 0xF3D5C0, 0xFFF0E6}, {0x3E2723, 0x6D4C41, 0x66CDAA, 0xC8F7DC}, {0x6A00FF, 0x9D50FF, 0xC792FF}, {0x001A00, 0x006600, 0x00CC33, 0x66FF66}, {0xFFB3BA, 0xFFDFBA, 0xFFFFBA, 0xBAFFC9, 0xBAE1FF, 0xE0BAFF}, {0x00F0FF, 0x0080FF, 0x0040FF}, {0xFFFF00, 0xFFCC00, 0xFF9900}, {0xFF0066, 0xFF3399, 0xFF99CC}, {0xFFD6EC, 0xE0AEDC, 0xB088C9}, {0x0A1F0A, 0x1E4620, 0x3A7D44}, {0x001830, 0x00476B, 0x0193C4}, {0xFF4E00, 0xFF9500, 0xFFD000}, {0xFF9A8B, 0xFF6A88, 0xFF99AC}, {0x00FF87, 0x60EFFF, 0x00FF87}, {0x1A0022, 0x4A0E5C, 0x8E24AA}, {0x89F7FE, 0x66A6FF, 0x89F7FE}, {0xC1FFD7, 0x7BE0AD, 0x4CB8A9}, {0xFF8C69, 0xFFB37B, 0xFFE0A3}, {0x0F2027, 0x203A43, 0x2C5364}, {0x3E2723, 0x5D4037, 0x8D6E63}, {0xB06500, 0xE8A00D, 0xFFD34E}, {0x00FFE0, 0x00C9B7, 0x008C8C}, {0xFF1361, 0xFF5A5F, 0xFFB86C}, {0x39FF14, 0xCCFF00, 0x9EFF00}, {0xB026FF, 0xE100FF, 0x7F00FF}, {0x0575E6, 0x00F260, 0x0575E6}, {0x41295A, 0x2F0743, 0x662D8C}, {0xF12711, 0xF5AF19, 0xF12711}, {0xFFC3A0, 0xFFAFBD, 0xFFC3A0}, {0x002B36, 0x0891A2, 0x00E5D0}, {0xA8FF78, 0x78FFD6, 0xA8FF78}, {0x2B0000, 0x800000, 0xE60000, 0xFF4D4D}, {0xF7C6C7, 0xE0A899, 0xC98B7A}, {0x1A2980, 0x26D0CE, 0x1A2980}, {0xFF2E63, 0xFF7597, 0xFFC1CC}, {0xE1FF00, 0x9BFF00, 0x56FF00}, {0xFBD3E9, 0xBB377D, 0xFBD3E9}, {0x7303C0, 0xEC38BC, 0xFDEFF9}, {0x11998E, 0x38EF7D, 0x11998E}, {0x360033, 0x0B8793, 0x360033}, {0xFF61D2, 0xFE9090, 0xFF61D2}, {0x2980B9, 0x6DD5FA, 0xFFFFFF}, {0x9D50BB, 0x6E48AA, 0x9D50BB}, {0xFF7E5F, 0xFEB47B, 0xFF7E5F}, {0x004D40, 0x1DE9B6, 0x004D40}, {0xFF512F, 0xF09819, 0xFF512F}, {0xDA22FF, 0x9733EE, 0xDA22FF}, {0x1CD8D2, 0x93EDC7, 0x1CD8D2}, {0x8A5A00, 0xCD9B1D, 0xFFE066}, {0xFF00CC, 0x333399, 0xFF00CC}, {0x00B09B, 0x96C93D, 0x00B09B}, {0xFF057E, 0x8D0B93, 0xFF057E}, {0x4776E6, 0x8E54E9, 0x4776E6}, {0xFF6A00, 0xEE0979, 0xFF6A00}, {0x00DBDE, 0xFC00FF, 0x00DBDE}, {0x0F3443, 0x34E89E, 0x0F3443}, {0xFF0000, 0xFF7F00, 0xFFFF00, 0x00FF00, 0x0000FF, 0x4B0082, 0x9400D3}};
    private static final String[] RAINBOW_NAMES = new String[]{"Cl\u00e1sico", "Fuego", "Oc\u00e9ano", "Ne\u00f3n", "Atardecer", "Bosque", "Chicle", "Hielo", "Galaxia", "Lava", "Menta", "Uva", "Caramelo", "Tropical", "Fantasma", "Sakura", "Cyberpunk", "Oto\u00f1o", "Aurora", "Rub\u00ed", "Esmeralda", "Zafiro", "Amatista", "Vaporwave", "Sangre", "Arco\u00edris Ne\u00f3n", "Dorado", "Plata", "Fuego Azul", "Veneno", "Rosa Chicle", "Cielo", "Magma", "Selva", "Coral", "Turquesa", "Lavanda", "Melocot\u00f3n", "\u00cdndigo", "Lim\u00f3n", "Cereza", "Menta Fresca", "Cobre", "Azul Real", "Fucsia", "Verde \u00c1cido", "Naranja Sol", "P\u00farpura Real", "Rosa Ne\u00f3n", "Verde Bosque", "\u00c1mbar", "Hielo Azul", "Sand\u00eda", "Crep\u00fasculo", "Rub\u00ed Rosa", "Verde Lima", "Oc\u00e9ano Profundo", "Ne\u00f3n Verde", "Ne\u00f3n Rosa", "Atardecer Tropical", "Aurora Boreal", "Fuego Fr\u00edo", "Bosque Encantado", "Algod\u00f3n de Az\u00facar", "Volc\u00e1n", "Cristal", "N\u00e9bula", "Oto\u00f1o Dorado", "Verano", "Invierno", "Primavera", "Cyber Ne\u00f3n", "Metal Oxidado", "Esmeralda Brillante", "Zafiro Profundo", "Amanecer", "Rosa Dorado", "Menta Chocolate", "Uva Ne\u00f3n", "Fuego Verde", "Arco\u00edris Pastel", "Ne\u00f3n Azul", "Ne\u00f3n Amarillo", "Fuego Rosa", "Hielo Rosa", "Bosque Oscuro", "Mar Profundo", "Puesta Solar", "Amanecer Rosa", "Selva Ne\u00f3n", "Uva Oscura", "Chicle Azul", "Menta Fr\u00eda", "Mel\u00f3n", "Cielo Nocturno", "Caf\u00e9", "Miel Dorada", "Turquesa Ne\u00f3n", "Rosa Ardiente", "Verde T\u00f3xico", "P\u00farpura Ne\u00f3n", "Azul El\u00e9ctrico", "Atardecer P\u00farpura", "Naranja Fuego", "Rosa Pastel", "Cian Profundo", "Lima Ne\u00f3n", "Rojo Sangre", "Oro Rosado", "Oc\u00e9ano Turquesa", "Fresa", "Lim\u00f3n Lima", "Cielo Rosado", "Violeta", "Verde Agua", "Fuego P\u00farpura", "Rosa Dorado Ne\u00f3n", "Azul Cielo", "Amatista Ne\u00f3n", "Coral Suave", "Verde Esmeralda", "Rojo Naranja", "Rosa P\u00farpura", "Azul Verde", "Dorado Bronce", "Magenta Ne\u00f3n", "Verde Menta Ne\u00f3n", "Rosa Rojo", "Azul P\u00farpura", "Naranja Rosa", "Cian Magenta", "Verde Azulado", "Arco\u00edris Vivo"};

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

