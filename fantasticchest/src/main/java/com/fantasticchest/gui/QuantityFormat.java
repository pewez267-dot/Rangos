package com.fantasticchest.gui;

import com.fantasticchest.config.ChestConfig;

import java.util.Locale;

/**
 * Formats {@code Long} quantities: thousands separators below {@code compact_threshold}
 * (e.g. 1,000,000) and a compact form above it (e.g. 1.0M, 2.5B).
 */
public final class QuantityFormat {

    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp"};

    private QuantityFormat() {
    }

    public static String format(final long quantity) {
        if (quantity >= ChestConfig.compactThreshold()) {
            return compact(quantity);
        }
        return withSeparators(quantity);
    }

    public static String withSeparators(final long n) {
        return String.format(Locale.US, "%,d", n);
    }

    public static String compact(final long n) {
        double v = n;
        int i = 0;
        while (v >= 1000.0 && i < SUFFIXES.length - 1) {
            v /= 1000.0;
            i++;
        }
        if (i == 0) {
            return Long.toString(n);
        }
        return String.format(Locale.US, "%.1f%s", v, SUFFIXES[i]);
    }
}
