package com.fantasticranks.nametag;

import com.fantasticranks.data.NametagStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Builds {@link Component}s from a {@link NametagStyle} / {@link NametagData}, including
 * per-character gradient interpolation. Pure common code (no rendering), reusable by the
 * admin GUI preview and the client nametag renderer alike.
 */
public final class NametagBuilder {

    private NametagBuilder() {
    }

    /** Builds the full extra line, e.g. {@code "Lvl 7 Veteran"}. */
    public static Component buildLine(NametagData data) {
        MutableComponent line = Component.literal("Lvl " + data.getLevel() + " ")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA)));
        line.append(buildStyledText(data.getText(), data.getStyle()));
        return line;
    }

    /** Builds just the styled rank text, applying solid color or per-character gradient. */
    public static MutableComponent buildStyledText(String text, NametagStyle style) {
        if (text == null) {
            text = "";
        }

        Style baseFormat = Style.EMPTY
                .withBold(style.isBold())
                .withItalic(style.isItalic())
                .withUnderlined(style.isUnderline())
                .withStrikethrough(style.isStrikethrough());

        if (!style.isGradient()) {
            return Component.literal(text)
                    .withStyle(baseFormat.withColor(TextColor.fromRgb(style.getColor())));
        }

        MutableComponent result = Component.empty();
        int length = text.length();
        for (int i = 0; i < length; i++) {
            float t = length <= 1 ? 0.0F : (float) i / (float) (length - 1);
            int color = lerpColor(style.getGradientStart(), style.getGradientEnd(), t);
            result.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(baseFormat.withColor(TextColor.fromRgb(color))));
        }
        return result;
    }

    /** Linear RGB interpolation between two packed 0xRRGGBB colors. */
    public static int lerpColor(int start, int end, float t) {
        if (t <= 0.0F) {
            return start & 0xFFFFFF;
        }
        if (t >= 1.0F) {
            return end & 0xFFFFFF;
        }
        int sr = (start >> 16) & 0xFF;
        int sg = (start >> 8) & 0xFF;
        int sb = start & 0xFF;
        int er = (end >> 16) & 0xFF;
        int eg = (end >> 8) & 0xFF;
        int eb = end & 0xFF;
        int r = Math.round(sr + (er - sr) * t);
        int g = Math.round(sg + (eg - sg) * t);
        int b = Math.round(sb + (eb - sb) * t);
        return (r << 16) | (g << 8) | b;
    }
}
