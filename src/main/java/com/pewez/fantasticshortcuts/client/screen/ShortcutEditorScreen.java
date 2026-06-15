package com.pewez.fantasticshortcuts.client.screen;

import com.pewez.fantasticshortcuts.FantasticShortcutsMod;
import com.pewez.fantasticshortcuts.client.widget.ScrollSelector;
import com.pewez.fantasticshortcuts.network.CreateShortcutPacket;
import com.pewez.fantasticshortcuts.network.DeleteShortcutPacket;
import com.pewez.fantasticshortcuts.network.FSShortcutsNetwork;
import com.pewez.fantasticshortcuts.network.SaveShortcutPacket;
import com.pewez.fantasticshortcuts.shortcuts.Shortcut;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * The Fantastic-style command shortcut editor screen.
 *
 * Layout (mirrors FantasticCrates / FantasticSpawners / FantasticKits):
 *  - Centered translucent panel (max 540x320), darkened backdrop.
 *  - Header row: title + tab buttons (LISTA, CREAR, AJUSTES).
 *  - Body: tab-specific content (split-pane editor for LISTA, form for CREAR, info for AJUSTES).
 *  - Footer: help line at the bottom; close button on the bottom-left, save on the bottom-right.
 */
@OnlyIn(Dist.CLIENT)
public class ShortcutEditorScreen extends Screen {

    private enum Tab {
        LISTA("Lista"), CREAR("Crear"), AJUSTES("Ajustes");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private final List<Shortcut> shortcuts;
    private Tab activeTab = Tab.LISTA;
    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;
    private String helpLine = "";

    // LISTA tab state
    private Shortcut selected;
    private EditBox aliasBox;
    private EditBox commandBox;
    private EditBox descriptionBox;
    private boolean editingAllowArguments = true;
    private boolean editingReplaceOriginal = false;

    // CREAR tab state
    private String createAlias = "";
    private String createCommand = "";

    public ShortcutEditorScreen(List<Shortcut> shortcuts, String openTab) {
        super(Component.literal("Fantastic Shortcuts"));
        this.shortcuts = new ArrayList<>(shortcuts);
        if ("crear".equalsIgnoreCase(openTab)) {
            this.activeTab = Tab.CREAR;
        } else if ("ajustes".equalsIgnoreCase(openTab)) {
            this.activeTab = Tab.AJUSTES;
        } else {
            this.activeTab = Tab.LISTA;
        }
        if (!this.shortcuts.isEmpty()) {
            this.selected = this.shortcuts.get(0);
            loadSelection();
        }
    }

    private void loadSelection() {
        if (selected == null) {
            editingAllowArguments = true;
            editingReplaceOriginal = false;
            return;
        }
        editingAllowArguments = selected.allowArguments;
        editingReplaceOriginal = selected.replaceOriginal;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(this.width - 16, 540);
        this.panelHeight = Math.min(this.height - 16, 320);
        this.leftPos = (this.width - panelWidth) / 2;
        this.topPos = (this.height - panelHeight) / 2;
        this.commandBox = null;
        this.descriptionBox = null;

        initHeader();
        initFooter();
        switch (activeTab) {
            case LISTA -> initListTab();
            case CREAR -> initCreateTab();
            case AJUSTES -> initSettingsTab();
        }
    }

    // ---------- Header / Footer ----------

    private void initHeader() {
        Tab[] tabs = Tab.values();
        int gap = 2;
        int tabW = (panelWidth - 16 - gap * (tabs.length - 1)) / tabs.length;
        int x = leftPos + 8;
        int y = topPos + 24;
        for (Tab tab : tabs) {
            boolean active = tab == activeTab;
            String text = (active ? "\u00a7f\u00a7l" : "\u00a77") + tab.label;
            this.addRenderableWidget(Button.builder(Component.literal(text), b -> {
                this.activeTab = tab;
                this.rebuildScreen();
            }).bounds(x, y, tabW, 18).build());
            x += tabW + gap;
        }
    }

    private void initFooter() {
        this.addRenderableWidget(Button.builder(Component.literal("\u00a77Cerrar"), b -> this.onClose())
                .bounds(leftPos + 8, topPos + panelHeight - 24, 80, 18).build());
        if (activeTab == Tab.LISTA && selected != null) {
            this.addRenderableWidget(Button.builder(Component.literal("\u00a7aGuardar cambios"), b -> saveSelected())
                    .bounds(leftPos + panelWidth - 150 - 8, topPos + panelHeight - 24, 150, 18).build());
        }
    }

    private int bodyX() {
        return leftPos + 8;
    }

    private int bodyY() {
        return topPos + 50;
    }

    private int bodyW() {
        return panelWidth - 16;
    }

    private int bodyH() {
        return panelHeight - 50 - 28;
    }

    // ---------- Tab: LISTA ----------

    private void initListTab() {
        helpLine = "Selecciona un atajo a la izquierda y edita su comando, descripcion y opciones a la derecha.";
        int x = bodyX();
        int y = bodyY();
        int colW = (bodyW() - 8) / 2;
        int rightX = x + colW + 8;

        if (shortcuts.isEmpty()) {
            helpLine = "No hay atajos todavia. Ve a la pestaña 'Crear' para crear tu primer atajo.";
            addLabel("\u00a77No hay atajos creados.", x, y + 10);
            addLabel("\u00a78Usa la pestaña \u00a7fCrear\u00a78 para añadir uno.", x, y + 26);
            this.addRenderableWidget(Button.builder(Component.literal("\u00a7a+ Crear atajo"), b -> {
                this.activeTab = Tab.CREAR;
                this.rebuildScreen();
            }).bounds(x, y + 48, 150, 18).build());
            return;
        }

        // Search box
        EditBox search = new EditBox(this.font, x, y, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar atajo..."));
        search.setMaxLength(64);

        ScrollSelector<Shortcut> selector = new ScrollSelector<>(
                x, y + 20, colW, bodyH() - 22, 16,
                sc -> "\u00a7b/" + sc.alias + "  \u00a78-> \u00a7f/" + truncate(sc.command, 40),
                sc -> sc.alias + " " + sc.command);
        selector.setItems(shortcuts);
        if (selected != null) {
            selector.setSelected(selected);
        }
        selector.onSelect(sc -> {
            this.selected = sc;
            loadSelection();
            this.rebuildScreen();
        });
        search.setResponder(selector::setQuery);

        this.addRenderableWidget(search);
        this.addRenderableWidget(selector);

        // Right panel: editor for the selected shortcut
        if (selected == null) {
            return;
        }

        int rightY = y;
        int labelW = 110;
        int fieldW = colW - labelW - 4;

        // Alias (editable -> renames the shortcut)
        addLabel("\u00a78Alias:", rightX, rightY + 4);
        EditBox aliasBox = new EditBox(this.font, rightX + labelW, rightY, fieldW, 16, Component.empty());
        aliasBox.setMaxLength(32);
        aliasBox.setValue(selected.alias);
        this.aliasBox = aliasBox;
        this.addRenderableWidget(aliasBox);
        rightY += 22;

        addLabel("\u00a78Comando:", rightX, rightY + 4);
        EditBox cmd = new EditBox(this.font, rightX + labelW, rightY, fieldW, 16, Component.empty());
        cmd.setMaxLength(256);
        cmd.setValue(selected.command);
        this.commandBox = cmd;
        this.addRenderableWidget(cmd);
        rightY += 22;

        addLabel("\u00a78Descripcion:", rightX, rightY + 4);
        EditBox desc = new EditBox(this.font, rightX + labelW, rightY, fieldW, 16, Component.empty());
        desc.setMaxLength(120);
        desc.setValue(selected.description == null ? "" : selected.description);
        this.descriptionBox = desc;
        this.addRenderableWidget(desc);
        rightY += 22;

        // Toggle: Allow arguments
        addLabel("\u00a78Argumentos:", rightX, rightY + 4);
        Button allowBtn = Button.builder(toggleLabel(editingAllowArguments), b -> {
            editingAllowArguments = !editingAllowArguments;
            b.setMessage(toggleLabel(editingAllowArguments));
        }).bounds(rightX + labelW, rightY, fieldW, 18).build();
        this.addRenderableWidget(allowBtn);
        rightY += 22;

        // Toggle: Replace original
        addLabel("\u00a78Reemplazar:", rightX, rightY + 4);
        Button replaceBtn = Button.builder(toggleLabel(editingReplaceOriginal), b -> {
            editingReplaceOriginal = !editingReplaceOriginal;
            b.setMessage(toggleLabel(editingReplaceOriginal));
        }).bounds(rightX + labelW, rightY, fieldW, 18).build();
        this.addRenderableWidget(replaceBtn);
        rightY += 24;

        // Delete button
        this.addRenderableWidget(Button.builder(Component.literal("\u00a7cEliminar atajo"), b -> deleteSelected())
                .bounds(rightX, rightY, colW, 18).build());
    }

    private void saveSelected() {
        if (selected == null || commandBox == null) {
            return;
        }
        Shortcut copy = selected.copy();
        if (aliasBox != null) {
            copy.alias = aliasBox.getValue().trim().toLowerCase();
        }
        copy.command = commandBox.getValue().trim();
        copy.description = descriptionBox == null ? "" : descriptionBox.getValue().trim();
        copy.allowArguments = editingAllowArguments;
        copy.replaceOriginal = editingReplaceOriginal;
        FSShortcutsNetwork.sendToServer(new SaveShortcutPacket(selected.alias, copy));
        this.helpLine = "Cambios enviados al servidor...";
    }

    private void deleteSelected() {
        if (selected == null) {
            return;
        }
        FSShortcutsNetwork.sendToServer(new DeleteShortcutPacket(selected.alias));
        this.helpLine = "Solicitud de eliminacion enviada...";
    }

    // ---------- Tab: CREAR ----------

    private EditBox createAliasBox;
    private EditBox createCommandBox;

    private void initCreateTab() {
        helpLine = "Crea un nuevo atajo. Usa {args} en el comando para insertar lo que el jugador escriba despues.";
        int x = bodyX();
        int y = bodyY();
        int labelW = 110;
        int fieldW = bodyW() - labelW - 4;

        addLabel("\u00a78Alias (sin /):", x, y + 4);
        EditBox aliasBox = new EditBox(this.font, x + labelW, y, fieldW, 16, Component.empty());
        aliasBox.setMaxLength(32);
        aliasBox.setValue(createAlias);
        aliasBox.setResponder(s -> createAlias = s);
        this.createAliasBox = aliasBox;
        this.addRenderableWidget(aliasBox);
        y += 24;

        addLabel("\u00a78Comando real:", x, y + 4);
        EditBox cmdBox = new EditBox(this.font, x + labelW, y, fieldW, 16, Component.empty());
        cmdBox.setMaxLength(256);
        cmdBox.setValue(createCommand);
        cmdBox.setResponder(s -> createCommand = s);
        cmdBox.setHint(Component.literal("ej: gamemode creative {args}"));
        this.createCommandBox = cmdBox;
        this.addRenderableWidget(cmdBox);
        y += 28;

        this.addRenderableWidget(Button.builder(Component.literal("\u00a7aCrear atajo"), b -> {
            String alias = createAlias.trim();
            String command = createCommand.trim();
            if (alias.isEmpty() || command.isEmpty()) {
                helpLine = "\u00a7cAlias y comando son obligatorios.";
                return;
            }
            FSShortcutsNetwork.sendToServer(new CreateShortcutPacket(alias, command));
            createAlias = "";
            createCommand = "";
            helpLine = "Solicitud de creacion enviada...";
        }).bounds(x, y, 150, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("\u00a77Limpiar"), b -> {
            createAlias = "";
            createCommand = "";
            this.rebuildScreen();
        }).bounds(x + 158, y, 100, 18).build());
    }

    // ---------- Tab: AJUSTES ----------

    private void initSettingsTab() {
        helpLine = "Edita config/fantasticshortcuts/config.toml y usa /fshortcuts reload o vuelve a abrir esta pantalla.";
    }

    // ---------- Render ----------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Darker semi-opaque backdrop (premium feel)
        this.renderBackground(g);
        g.fill(0, 0, this.width, this.height, 0x90000000);

        // Panel
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + panelHeight, 0xF0151A1F);
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + 1, 0xFF2DD4FF);
        g.fill(leftPos, topPos + panelHeight - 1, leftPos + panelWidth, topPos + panelHeight, 0xFF2DD4FF);
        g.fill(leftPos, topPos, leftPos + 1, topPos + panelHeight, 0xFF2DD4FF);
        g.fill(leftPos + panelWidth - 1, topPos, leftPos + panelWidth, topPos + panelHeight, 0xFF2DD4FF);

        // Title
        g.drawString(this.font, Component.literal("\u00a7b\u00a7lFantastic Shortcuts"), leftPos + 8, topPos + 8, 0xFFFFFF, false);
        g.drawString(this.font, Component.literal("\u00a78v1.0  by Pewez777"), leftPos + panelWidth - 100, topPos + 9, 0x9AAAB8, false);

        // Body separator
        g.fill(leftPos + 4, topPos + 46, leftPos + panelWidth - 4, topPos + 47, 0xFF243038);

        super.render(g, mouseX, mouseY, partialTick);

        // Help line (footer top)
        g.drawString(this.font, Component.literal(helpLine), leftPos + 8, topPos + panelHeight - 38, 0x9AAAB8, false);
    }

    private void rebuildScreen() {
        this.clearWidgets();
        this.init();
    }

    private void addLabel(String text, int x, int y) {
        Component component = Component.literal(text);
        this.addRenderableWidget(new RenderOnlyWidget((g, mx, my, pt) ->
                g.drawString(this.font, component, x, y, 0xC0CDD7, false)));
    }

    private static Component toggleLabel(boolean value) {
        return Component.literal(value ? "\u00a7aActivado" : "\u00a77Desactivado");
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "\u2026";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** A tiny no-input widget that just renders text. */
    private static class RenderOnlyWidget extends net.minecraft.client.gui.components.AbstractWidget {
        private final net.minecraft.client.gui.components.Renderable inner;

        RenderOnlyWidget(net.minecraft.client.gui.components.Renderable inner) {
            super(0, 0, 0, 0, Component.empty());
            this.inner = inner;
            this.active = false;
            this.visible = true;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            inner.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput out) {
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return false;
        }
    }

    /** Avoid unused-import warning for the Minecraft instance type. */
    @SuppressWarnings("unused")
    private static Minecraft mcRef() {
        return Minecraft.getInstance();
    }

    /** Hint to keep the FantasticShortcutsMod import grouped with the rest. */
    @SuppressWarnings("unused")
    private static String modIdRef() {
        return FantasticShortcutsMod.MOD_ID;
    }
}
