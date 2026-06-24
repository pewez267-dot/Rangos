package com.fantasticpass.gui.widgets;

import com.fantasticpass.data.PassSerializer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/**
 * Hex color input ({@code #RRGGBB}). Accepts pasted codes, validates as the user types,
 * and reports parsed colors. {@link #setColorSilently(int)} syncs from the wheel/sliders
 * without re-triggering the listener.
 */
public class HexInputWidget extends EditBox {

    private final IntConsumer onColorChanged;
    private boolean suppress;

    public HexInputWidget(Font font, int x, int y, int width, int height, IntConsumer onColorChanged) {
        super(font, x, y, width, height, Component.literal("HEX"));
        this.onColorChanged = onColorChanged;
        setMaxLength(7);
        setFilter(s -> s.matches("#?[0-9a-fA-F]{0,6}"));
        setResponder(this::onChanged);
        // No initial value is set here on purpose: the owning editor pushes the real
        // color via setColorSilently() after layout, avoiding a spurious listener fire.
    }

    private void onChanged(String value) {
        if (suppress) {
            return;
        }
        int parsed = PassSerializer.parseHex(value, Integer.MIN_VALUE);
        if (parsed != Integer.MIN_VALUE && onColorChanged != null) {
            onColorChanged.accept(parsed);
        }
    }

    public void setColorSilently(int rgb) {
        suppress = true;
        setValue(PassSerializer.toHex(rgb));
        suppress = false;
    }
}
