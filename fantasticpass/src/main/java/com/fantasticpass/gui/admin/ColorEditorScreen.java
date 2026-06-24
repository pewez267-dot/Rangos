package com.fantasticpass.gui.admin;

import com.fantasticpass.data.NametagStyle;
import com.fantasticpass.gui.GuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;

/**
 * Full-screen nametag color editor: HSB wheel, RGB sliders, hex input, the 16 vanilla
 * colors, format toggles, gradient mode with start/end targets, a rank-text field, a
 * "copy code" action, and a real-time preview. Returns the edited style and text to the
 * caller on confirm. Compact, non-overlapping layout.
 */
public class ColorEditorScreen extends Screen {

    @Nullable
    private final Screen parent;
    private final BiConsumer<NametagStyle, String> onDone;
    private final NametagStyle initialStyle;
    private final String initialText;

    private ColorEditorWidget editor;
    private EditBox textField;

    public ColorEditorScreen(@Nullable Screen parent, NametagStyle style, String text,
                             BiConsumer<NametagStyle, String> onDone) {
        super(Component.literal("Nametag Style Editor"));
        this.parent = parent;
        this.initialStyle = style == null ? new NametagStyle() : style.copy();
        this.initialText = text == null ? "" : text;
        this.onDone = onDone;
    }

    private int leftX() {
        return this.width / 2 - 120;
    }

    @Override
    protected void init() {
        int leftX = leftX();

        textField = addRenderableWidget(new EditBox(this.font, leftX, 42, 240, 16,
                Component.translatable("fantasticpass.gui.rank_text")));
        textField.setMaxLength(64);
        textField.setValue(initialText);

        editor = new ColorEditorWidget(initialStyle, initialText);
        editor.setPreviewContext(Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getGameProfile().getName() : "Player", 100);
        textField.setResponder(editor::setRankText);
        editor.build(this::addRenderableWidget, this.font, leftX, 64);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> confirm())
                .bounds(this.width - 174, 8, 80, 18).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(this.width - 90, 8, 80, 18).build());
    }

    private void confirm() {
        if (onDone != null) {
            onDone.accept(editor.getStyle(), textField.getValue());
        }
        onClose();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.drawBackground(graphics, this.width, this.height);
        graphics.drawString(this.font, this.title, leftX(), 14, 0xFF00E5FF, false);
        graphics.drawString(this.font, Component.translatable("fantasticpass.gui.rank_text"),
                leftX(), 32, 0xFFAAAAAA, false);

        super.render(graphics, mouseX, mouseY, partialTick);

        editor.renderPalette(graphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && editor.handlePaletteClick(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
