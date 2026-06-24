package com.fantastickits.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for parsing Minecraft color codes (&amp;) and converting them
 * to Component objects with proper formatting.
 */
public final class ColorUtil {

    private ColorUtil() {}

    private static final Map<Character, ChatFormatting> COLOR_MAP = new HashMap<>();

    static {
        COLOR_MAP.put('0', ChatFormatting.BLACK);
        COLOR_MAP.put('1', ChatFormatting.DARK_BLUE);
        COLOR_MAP.put('2', ChatFormatting.DARK_GREEN);
        COLOR_MAP.put('3', ChatFormatting.DARK_AQUA);
        COLOR_MAP.put('4', ChatFormatting.DARK_RED);
        COLOR_MAP.put('5', ChatFormatting.DARK_PURPLE);
        COLOR_MAP.put('6', ChatFormatting.GOLD);
        COLOR_MAP.put('7', ChatFormatting.GRAY);
        COLOR_MAP.put('8', ChatFormatting.DARK_GRAY);
        COLOR_MAP.put('9', ChatFormatting.BLUE);
        COLOR_MAP.put('a', ChatFormatting.GREEN);
        COLOR_MAP.put('b', ChatFormatting.AQUA);
        COLOR_MAP.put('c', ChatFormatting.RED);
        COLOR_MAP.put('d', ChatFormatting.LIGHT_PURPLE);
        COLOR_MAP.put('e', ChatFormatting.YELLOW);
        COLOR_MAP.put('f', ChatFormatting.WHITE);
        COLOR_MAP.put('k', ChatFormatting.OBFUSCATED);
        COLOR_MAP.put('l', ChatFormatting.BOLD);
        COLOR_MAP.put('m', ChatFormatting.STRIKETHROUGH);
        COLOR_MAP.put('n', ChatFormatting.UNDERLINE);
        COLOR_MAP.put('o', ChatFormatting.ITALIC);
        COLOR_MAP.put('r', ChatFormatting.RESET);
    }

    /**
     * Parse a string with &amp; color codes into a styled Component.
     * Supports &amp;0-9, &amp;a-f, &amp;k, &amp;l, &amp;m, &amp;n, &amp;o, &amp;r
     * Also supports &amp;#RRGGBB for hex colors.
     *
     * @param input The raw string with color codes
     * @return A Component with proper formatting applied
     */
    public static Component parseColorCodes(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        MutableComponent result = Component.empty();
        StringBuilder currentText = new StringBuilder();
        Style currentStyle = Style.EMPTY;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '&' && i + 1 < input.length()) {
                char next = Character.toLowerCase(input.charAt(i + 1));

                // Check for hex color: &#RRGGBB
                if (next == '#' && i + 8 <= input.length()) {
                    String hex = input.substring(i + 2, i + 8);
                    try {
                        int color = Integer.parseInt(hex, 16);
                        // Flush current text with current style
                        if (currentText.length() > 0) {
                            result = result.append(Component.literal(currentText.toString()).withStyle(currentStyle));
                            currentText.setLength(0);
                        }
                        currentStyle = Style.EMPTY.withColor(TextColor.fromRgb(color));
                        i += 7; // Skip &#RRGGBB
                        continue;
                    } catch (NumberFormatException e) {
                        // Not a valid hex, treat as normal text
                    }
                }

                // Check for standard color code
                ChatFormatting formatting = COLOR_MAP.get(next);
                if (formatting != null) {
                    // Flush current text with current style
                    if (currentText.length() > 0) {
                        result = result.append(Component.literal(currentText.toString()).withStyle(currentStyle));
                        currentText.setLength(0);
                    }

                    if (formatting == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else if (formatting.isColor()) {
                        currentStyle = Style.EMPTY.withColor(formatting);
                    } else {
                        // Formatting codes (bold, italic, etc.) stack on current style
                        currentStyle = applyFormatting(currentStyle, formatting);
                    }
                    i++; // Skip the code character
                    continue;
                }
            }

            currentText.append(c);
        }

        // Flush remaining text
        if (currentText.length() > 0) {
            result = result.append(Component.literal(currentText.toString()).withStyle(currentStyle));
        }

        return result;
    }

    /**
     * Strip all color codes from a string, returning plain text.
     */
    public static String stripColorCodes(String input) {
        if (input == null) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '&' && i + 1 < input.length()) {
                char next = Character.toLowerCase(input.charAt(i + 1));
                if (next == '#' && i + 8 <= input.length()) {
                    i += 7;
                    continue;
                }
                if (COLOR_MAP.containsKey(next)) {
                    i++;
                    continue;
                }
            }
            result.append(c);
        }
        return result.toString();
    }

    /**
     * Convert a Component back to a string with &amp; codes (best effort).
     */
    public static String toColorCodeString(Component component) {
        // For simplicity, use the getString which strips formatting
        // Full round-trip would require visiting the component tree
        return component.getString();
    }

    private static Style applyFormatting(Style style, ChatFormatting formatting) {
        switch (formatting) {
            case BOLD:
                return style.withBold(true);
            case ITALIC:
                return style.withItalic(true);
            case UNDERLINE:
                return style.withUnderlined(true);
            case STRIKETHROUGH:
                return style.withStrikethrough(true);
            case OBFUSCATED:
                return style.withObfuscated(true);
            default:
                return style;
        }
    }
}
