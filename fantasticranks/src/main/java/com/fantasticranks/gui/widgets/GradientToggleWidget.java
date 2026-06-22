package com.fantasticranks.gui.widgets;

import com.fantasticranks.gui.GuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * A compact on/off toggle used for the gradient switch and the §-format toggles (bold /
 * italic / underline / strikethrough). Styled with the mod's dark theme.
 */
public class GradientToggleWidget extends AbstractWidget {

    private boolean state;
    private final Consumer<Boolean> onToggle;

    public GradientToggleWidget(int x, int y, int width, int height, Component label,
                                boolean initialState, Consumer<Boolean> onToggle) {
        super(x, y, width, height, label);
        this.state = initialState;
        this.onToggle = onToggle;
    }

    public boolean getState() {
        return state;
    }

    public void setStateSilently(boolean state) {
        this.state = state;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = mouseX >= getX() && mouseX < getX() + this.width
                && mouseY >= getY() && mouseY < getY() + this.height;
        int background = state ? GuiTheme.ACCENT_CYAN_DIM : GuiTheme.PANEL;
        graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, 0xFF000000 | background);
        int border = state ? GuiTheme.ACCENT_GOLD : (hovered ? GuiTheme.ACCENT_CYAN : GuiTheme.BORDER);
        graphics.renderOutline(getX(), getY(), this.width, this.height, 0xFF000000 | border);

        int textColor = state ? 0xFFFFFFFF : 0xFFAAAAAA;
        int textWidth = Minecraft.getInstance().font.width(getMessage());
        graphics.drawString(Minecraft.getInstance().font, getMessage(),
                getX() + (this.width - textWidth) / 2, getY() + (this.height - 8) / 2, textColor, false);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        state = !state;
        if (onToggle != null) {
            onToggle.accept(state);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
