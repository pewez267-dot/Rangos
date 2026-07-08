package com.fantasticpass.gui.admin;

import com.fantasticpass.data.NametagStyle;
import com.fantasticpass.gui.GuiTheme;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ColorEditorScreen extends Screen {
    @Nullable
    private final Screen parent;
    private final BiConsumer<NametagStyle, String> onDone;
    // Estado de trabajo persistente: sobrevive a las re-inicializaciones de la pantalla
    // (p. ej. al volver del selector de arcoiris), asi no se pierden los cambios.
    private final NametagStyle workingStyle;
    private String workingText;
    private final int previewLevel;
    private final String label;
    private ColorEditorWidget editor;
    private EditBox textField;
    private int leftX;

    // Constructor compatible con el editor de rangos existente (nivel de vista previa = 100).
    public ColorEditorScreen(@Nullable Screen parent, NametagStyle style, String text, BiConsumer<NametagStyle, String> onDone) {
        this(parent, style, text, 100, "Texto", onDone);
    }

    public ColorEditorScreen(@Nullable Screen parent, NametagStyle style, String text, int previewLevel, BiConsumer<NametagStyle, String> onDone) {
        this(parent, style, text, previewLevel, "Texto", onDone);
    }

    public ColorEditorScreen(@Nullable Screen parent, NametagStyle style, String text, int previewLevel, String label, BiConsumer<NametagStyle, String> onDone) {
        super(Component.literal("Editor de Estilo"));
        this.parent = parent;
        this.workingStyle = style == null ? new NametagStyle() : style.copy();
        this.workingText = text == null ? "" : text;
        this.previewLevel = previewLevel;
        this.label = label == null ? "Texto" : label;
        this.onDone = onDone;
    }

    @Override
    protected void init() {
        this.leftX = this.width / 2 - 130;
        int nameY = 28;
        int editorY = 50;
        this.textField = this.addRenderableWidget(new EditBox(this.font, this.leftX, nameY, 258, 16, Component.literal(this.label)));
        this.textField.setMaxLength(64);
        this.textField.setValue(this.workingText);
        this.editor = new ColorEditorWidget(this.workingStyle, this.workingText);
        this.editor.setParentScreen(this);
        this.editor.setPreviewContext(Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getGameProfile().getName() : "Jugador", this.previewLevel);
        this.textField.setResponder(v -> {
            this.workingText = v;
            this.editor.setRankText(v);
        });
        this.editor.build(this::addRenderableWidget, this.font, this.leftX, editorY);
        // Preview compacto anclado justo arriba de los botones (evita solapamientos).
        int buttonY = this.height - 26;
        int previewY = buttonY - 30;
        this.editor.attachPreview(this::addRenderableWidget, this.leftX, previewY, 258, 26);
        this.addRenderableWidget(Button.builder(Component.literal("Listo"), b -> this.confirm()).bounds(this.width / 2 - 104, buttonY, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> this.onClose()).bounds(this.width / 2 + 4, buttonY, 100, 20).build());
    }

    private void confirm() {
        if (this.onDone != null) {
            this.onDone.accept(this.workingStyle, this.workingText);
        }
        this.onClose();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.drawBackground(graphics, this.width, this.height);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, -16718337);
        graphics.drawString(this.font, Component.literal(this.label), this.leftX, 18, -5592406, false);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.editor.renderPalette(graphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.editor.handlePaletteClick(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
