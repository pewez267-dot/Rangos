package com.fscrates.client.color;

import java.awt.Color;
import java.util.Locale;
import net.minecraft.ChatFormatting;

public final class FSColors {
    private FSColors() {
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

    public static int hsbToRgb(float h, float s, float b) {
        return 0xFFFFFF & Color.HSBtoRGB(h, s, b);
    }

    public static float[] rgbToHsb(int rgb) {
        return Color.RGBtoHSB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, null);
    }

    public static String toHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }
}

