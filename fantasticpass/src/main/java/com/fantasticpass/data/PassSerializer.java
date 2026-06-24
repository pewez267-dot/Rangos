package com.fantasticpass.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Shared (de)serialization helpers: pass byte serialization, hex parsing/formatting,
 * HSB/RGB conversion used by the color editor, and conversion of a {@link NametagStyle}
 * into a legacy {@code §}-code string (used by the admin "Copy Code" feature and as a
 * plain-text fallback).
 */
public final class PassSerializer {

    private PassSerializer() {
    }

    // ---- Pass byte (de)serialization ----

    public static void writePass(FriendlyByteBuf buf, PassDefinition pass) {
        pass.toBuf(buf);
    }

    public static PassDefinition readPass(FriendlyByteBuf buf) {
        return PassDefinition.fromBuf(buf);
    }

    // ---- Hex helpers ----

    /** Formats a packed 0xRRGGBB color as {@code #RRGGBB}. */
    public static String toHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    /**
     * Parses {@code #RRGGBB} / {@code RRGGBB} / {@code #RGB} into a packed color.
     *
     * @return the packed color, or {@code fallback} if the input is not a valid hex color
     */
    public static int parseHex(String input, int fallback) {
        if (input == null) {
            return fallback;
        }
        String s = input.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        try {
            if (s.length() == 3) {
                int r = Integer.parseInt(s.substring(0, 1), 16);
                int g = Integer.parseInt(s.substring(1, 2), 16);
                int b = Integer.parseInt(s.substring(2, 3), 16);
                r = r * 17;
                g = g * 17;
                b = b * 17;
                return (r << 16) | (g << 8) | b;
            }
            if (s.length() == 6) {
                return Integer.parseInt(s, 16) & 0xFFFFFF;
            }
        } catch (NumberFormatException ignored) {
            // fall through to fallback
        }
        return fallback;
    }

    // ---- RGB / HSB conversion (no java.awt dependency in client runtime) ----

    /** @return {h, s, b} all in 0..1 */
    public static float[] rgbToHsb(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float hue = 0.0F;
        if (delta > 1.0E-5F) {
            if (max == r) {
                hue = ((g - b) / delta) % 6.0F;
            } else if (max == g) {
                hue = (b - r) / delta + 2.0F;
            } else {
                hue = (r - g) / delta + 4.0F;
            }
            hue /= 6.0F;
            if (hue < 0.0F) {
                hue += 1.0F;
            }
        }
        float saturation = max <= 0.0F ? 0.0F : delta / max;
        return new float[]{hue, saturation, max};
    }

    /** @param h hue 0..1, @param s saturation 0..1, @param v brightness 0..1 */
    public static int hsbToRgb(float h, float s, float v) {
        h = clamp01(h);
        s = clamp01(s);
        v = clamp01(v);

        float r;
        float g;
        float b;
        if (s <= 0.0F) {
            r = g = b = v;
        } else {
            float hScaled = h * 6.0F;
            if (hScaled >= 6.0F) {
                hScaled = 0.0F;
            }
            int i = (int) hScaled;
            float f = hScaled - i;
            float p = v * (1.0F - s);
            float q = v * (1.0F - s * f);
            float t = v * (1.0F - s * (1.0F - f));
            switch (i) {
                case 0: r = v; g = t; b = p; break;
                case 1: r = q; g = v; b = p; break;
                case 2: r = p; g = v; b = t; break;
                case 3: r = p; g = q; b = v; break;
                case 4: r = t; g = p; b = v; break;
                default: r = v; g = p; b = q; break;
            }
        }
        int ri = Math.round(r * 255.0F);
        int gi = Math.round(g * 255.0F);
        int bi = Math.round(b * 255.0F);
        return (ri << 16) | (gi << 8) | bi;
    }

    private static float clamp01(float value) {
        if (value < 0.0F) {
            return 0.0F;
        }
        return Math.min(value, 1.0F);
    }

    // ---- Legacy format-code string ----

    /**
     * Builds a legacy {@code §}-prefixed string approximating the configured style,
     * suitable for copying into other plugins/mods. Gradients are flattened to their
     * start color since legacy codes cannot express per-character gradients.
     */
    public static String toFormatCodeString(NametagStyle style, String text) {
        StringBuilder sb = new StringBuilder();
        int color = style.isGradient() ? style.getGradientStart() : style.getColor();
        ChatFormatting nearest = nearestLegacyColor(color);
        sb.append('\u00A7').append(nearest.getChar());
        if (style.isBold()) {
            sb.append('\u00A7').append('l');
        }
        if (style.isItalic()) {
            sb.append('\u00A7').append('o');
        }
        if (style.isUnderline()) {
            sb.append('\u00A7').append('n');
        }
        if (style.isStrikethrough()) {
            sb.append('\u00A7').append('m');
        }
        sb.append(text == null ? "" : text);
        return sb.toString();
    }

    /** Finds the nearest vanilla color {@link ChatFormatting} to a packed RGB color. */
    public static ChatFormatting nearestLegacyColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        ChatFormatting best = ChatFormatting.WHITE;
        long bestDistance = Long.MAX_VALUE;
        for (ChatFormatting formatting : ChatFormatting.values()) {
            if (!formatting.isColor()) {
                continue;
            }
            Integer value = formatting.getColor();
            if (value == null) {
                continue;
            }
            int cr = (value >> 16) & 0xFF;
            int cg = (value >> 8) & 0xFF;
            int cb = value & 0xFF;
            long dr = r - cr;
            long dg = g - cg;
            long db = b - cb;
            long distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = formatting;
            }
        }
        return best;
    }
}
