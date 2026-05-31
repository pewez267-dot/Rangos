package com.revivemod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Pantalla de configuracion accesible desde Mods > Revive Mod > Configuracion.
 * Permite re-asignar las teclas de "Rendirse" y "Auto-revivir" sin salir del
 * juego ni tocar archivos de configuracion.
 */
public class ReviveConfigScreen extends Screen {
    private final Screen parent;
    private KeyMapping selecting;
    private Button surrenderBtn;
    private Button selfBtn;

    public ReviveConfigScreen(Screen parent) {
        super(Component.literal("Configuraci\u00f3n de Revive Mod"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 60;

        this.surrenderBtn = Button.builder(Component.empty(), b -> {
            this.selecting = RevivemodKeybinds.SURRENDER;
            updateButtons();
        }).bounds(cx - 120, y, 240, 20).build();
        addRenderableWidget(this.surrenderBtn);

        this.selfBtn = Button.builder(Component.empty(), b -> {
            this.selecting = RevivemodKeybinds.SELF_REVIVE;
            updateButtons();
        }).bounds(cx - 120, y + 28, 240, 20).build();
        addRenderableWidget(this.selfBtn);

        addRenderableWidget(Button.builder(Component.literal("Restablecer teclas por defecto"), b -> {
            setKey(RevivemodKeybinds.SURRENDER, InputConstants.Type.KEYSYM.getOrCreate(RevivemodKeybinds.DEFAULT_SURRENDER_KEY));
            setKey(RevivemodKeybinds.SELF_REVIVE, InputConstants.Type.KEYSYM.getOrCreate(RevivemodKeybinds.DEFAULT_SELF_REVIVE_KEY));
            this.selecting = null;
            updateButtons();
        }).bounds(cx - 120, y + 60, 240, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Todos los controles\u2026"), b ->
                this.minecraft.setScreen(new KeyBindsScreen(this, this.minecraft.options)))
                .bounds(cx - 120, y + 88, 240, 20).build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(cx - 120, y + 124, 240, 20).build());

        updateButtons();
    }

    private void updateButtons() {
        this.surrenderBtn.setMessage(label("Rendirse / Morir", RevivemodKeybinds.SURRENDER));
        this.selfBtn.setMessage(label("Auto-revivir", RevivemodKeybinds.SELF_REVIVE));
    }

    private Component label(String name, KeyMapping km) {
        Component keyName = km.getTranslatedKeyMessage();
        if (this.selecting == km) {
            return Component.literal(name + ": ")
                    .append(Component.literal("> ").withStyle(ChatFormatting.YELLOW))
                    .append(keyName.copy().withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" <").withStyle(ChatFormatting.YELLOW));
        }
        return Component.literal(name + ": ").append(keyName.copy().withStyle(ChatFormatting.GREEN));
    }

    private void setKey(KeyMapping km, InputConstants.Key key) {
        this.minecraft.options.setKey(km, key);
        KeyMapping.resetMapping();
        this.minecraft.options.save();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.selecting != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                setKey(this.selecting, InputConstants.UNKNOWN);
            } else {
                setKey(this.selecting, InputConstants.getKey(keyCode, scanCode));
            }
            this.selecting = null;
            updateButtons();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (this.selecting != null) {
            setKey(this.selecting, InputConstants.Type.MOUSE.getOrCreate(button));
            this.selecting = null;
            updateButtons();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFF);
        g.drawCenteredString(this.font,
                Component.literal("Haz clic en una tecla y luego pulsa la nueva tecla.").withStyle(ChatFormatting.GRAY),
                this.width / 2, 38, 0xA0A0A0);
        super.render(g, mx, my, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.options.save();
        this.minecraft.setScreen(this.parent);
    }
}
