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
 * Full-screen, professional nametag color editor: HSB wheel, RGB sliders, hex input,
 * the 16 vanilla colors, §-format toggles, a gradient mode with start/end targets, a
 * rank-text field, a "copy code" action, and a real-time preview. Returns the edited
 * style and text to the caller on confirm.
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

    @Override
    protected void init() {
        int leftX = this.width / 2 - 130;
        int topY = 44;

        textField = addRenderableWidget(new EditBox(this.font, leftX, topY, 240, 18,
                Component.translatable("fantasticpass.gui.rank_text")));
        textField.setMaxLength(64);
        textField.setValue(initialText);

        editor = new ColorEditorWidget(initialStyle, initialText);
        editor.setPreviewContext(Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getGameProfile().getName() : "Player", 100);
        textField.setResponder(editor::setRankText);
        editor.build(this::addRenderableWidget, this.font, leftX, topY + 26);

        int buttonY = this.height - 28;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> confirm())
                .bounds(this.width / 2 - 104, buttonY, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(this.width / 2 + 4, buttonY, 100, 20).build());
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
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFF00E5FF);

        int leftX = this.width / 2 - 130;
        graphics.drawString(this.font, Component.translatable("fantasticpass.gui.rank_text"),
                leftX, 34, 0xFFAAAAAA, false);

        super.render(graphics, mouseX, mouseY, partialTick);

        // Palette swatches (rendered on top of the background, in their own region).
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
