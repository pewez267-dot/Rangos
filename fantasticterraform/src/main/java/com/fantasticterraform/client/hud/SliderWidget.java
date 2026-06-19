package com.fantasticterraform.client.hud;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Slider generico para valores double/int con etiqueta, rango y callback al cambiar.
 * Usado por los paneles del HUD para radios, intensidades, alturas, etc.
 */
public final class SliderWidget extends AbstractSliderButton {

    private final String label;
    private final double min;
    private final double max;
    private final boolean integer;
    private final Consumer<Double> onChange;

    public SliderWidget(int x, int y, int width, int height, String label, double min, double max,
                        double initial, boolean integer, Consumer<Double> onChange) {
        super(x, y, width, height, Component.empty(), clamp01((initial - min) / (max - min)));
        this.label = label;
        this.min = min;
        this.max = max;
        this.integer = integer;
        this.onChange = onChange;
        updateMessage();
    }

    private static double clamp01(double v) {
        return Math.max(0.0D, Math.min(1.0D, v));
    }

    public double currentValue() {
        double v = min + value * (max - min);
        return integer ? Math.round(v) : v;
    }

    @Override
    protected void updateMessage() {
        double v = currentValue();
        String txt = integer ? String.valueOf((int) v) : String.format("%.2f", v);
        setMessage(Component.literal(label + ": " + txt));
    }

    @Override
    protected void applyValue() {
        if (onChange != null) {
            onChange.accept(currentValue());
        }
    }
}
