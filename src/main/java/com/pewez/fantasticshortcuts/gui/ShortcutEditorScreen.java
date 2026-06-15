package com.pewez.fantasticshortcuts.gui;

import com.pewez.fantasticshortcuts.network.CreateShortcutPacket;
import com.pewez.fantasticshortcuts.network.DeleteShortcutPacket;
import com.pewez.fantasticshortcuts.network.FSNetwork;
import com.pewez.fantasticshortcuts.network.SaveShortcutPacket;
import com.pewez.fantasticshortcuts.shortcuts.Shortcut;
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
 * Editor de atajos de Fantastic Shortcuts.
 *
 * <p>Réplica fiel de la arquitectura de GUI de FantasticCrates / FantasticSpawners: una
 * {@link Screen} de cliente dibujada A MANO (sin inventarios ni contenedores), con panel oscuro,
 * borde de acento aqua, cabecera con pestañas tipo botón, cuerpo según la pestaña activa y pie con
 * "Cerrar" y la acción principal.
 *
 * <p>Pestañas:
 * <ul>
 *     <li><b>Lista</b>: panel partido. Izquierda búsqueda + {@link ScrollSelector} de atajos.
 *     Derecha editor del seleccionado (renombrar alias, comando, descripción, toggles Argumentos y
 *     Reemplazar, y botón Eliminar). Estado vacío con acceso directo a "Crear".</li>
 *     <li><b>Crear</b>: alias + comando ({@code {args}} soportado) y botones Crear/Limpiar.</li>
 *     <li><b>Ajustes</b>: información de {@code config.toml}.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class ShortcutEditorScreen extends Screen {

    // Paleta de la suite Fantastic.
    private static final int PANEL_BG = 0xF0151A1F;
    private static final int HEADER_BG = 0xFF10151B;
    private static final int ACCENT = 0xFF2DD4FF;
    private static final int SEPARATOR = 0xFF243038;
    private static final int HELP_COLOR = 0x9AA9B0;

    private final List<Shortcut> shortcuts;
    private final boolean cfgReplaceMode;
    private final String cfgPriority;
    private final boolean cfgAudit;

    private GuiTab activeTab;
    private String helpLine = "";

    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;

    // Selección y buffers de edición de la pestaña "Lista".
    private String selectedAlias = null;
    private String editAlias = "";
    private String editCommand = "";
    private String editDescription = "";
    private boolean editUseArgs = false;
    private boolean editReplace = false;

    // Buffers de la pestaña "Crear".
    private String createAlias = "";
    private String createCommand = "";

    private ScrollSelector<Shortcut> listSelector;
    private final List<Label> labels = new ArrayList<>();

    public ShortcutEditorScreen(List<Shortcut> shortcuts, GuiTab tab,
                                boolean cfgReplaceMode, String cfgPriority, boolean cfgAudit) {
        super(Component.literal("Fantastic Shortcuts"));
        this.shortcuts = shortcuts != null ? shortcuts : new ArrayList<>();
        this.activeTab = tab != null ? tab : GuiTab.LIST;
        this.cfgReplaceMode = cfgReplaceMode;
        this.cfgPriority = cfgPriority;
        this.cfgAudit = cfgAudit;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(this.width - 16, 540);
        this.panelHeight = Math.min(this.height - 16, 320);
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;
        this.labels.clear();
        this.listSelector = null;

        initHeader();
        initFooter();
        switch (activeTab) {
            case LIST -> initList();
            case CREATE -> initCreate();
            case SETTINGS -> initSettings();
        }
    }

    // ------------------------------------------------------------------
    // Layout helpers
    // ------------------------------------------------------------------

    private int bodyX() { return leftPos + 8; }
    private int bodyY() { return topPos + 62; }
    private int bodyW() { return panelWidth - 16; }
    private int bodyH() { return panelHeight - 62 - 28; }

    private void initHeader() {
        final GuiTab[] tabs = GuiTab.values();
        final int gap = 2;
        final int tabW = (panelWidth - 16 - gap * (tabs.length - 1)) / tabs.length;
        int x = leftPos + 8;
        final int y = topPos + 24;
        for (GuiTab tab : tabs) {
            final boolean active = tab == activeTab;
            final String text = (active ? "§f§l" : "§7") + tab.label;
            addRenderableWidget(Button.builder(Component.literal(text), b -> {
                this.activeTab = tab;
                rebuildWidgets();
            }).bounds(x, y, tabW, 18).build());
            x += tabW + gap;
        }
    }

    private void initFooter() {
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(leftPos + 8, topPos + panelHeight - 24, 80, 18).build());

        if (activeTab == GuiTab.LIST && selectedAlias != null) {
            addRenderableWidget(Button.builder(Component.literal("§aGuardar cambios"), b -> saveSelected())
                    .bounds(leftPos + panelWidth - 158, topPos + panelHeight - 24, 150, 18).build());
        } else if (activeTab == GuiTab.CREATE) {
            addRenderableWidget(Button.builder(Component.literal("§aCrear atajo"), b -> createNew())
                    .bounds(leftPos + panelWidth - 158, topPos + panelHeight - 24, 150, 18).build());
        }
    }

    // ------------------------------------------------------------------
    // Tab: LIST
    // ------------------------------------------------------------------

    private void initList() {
        this.helpLine = "Izquierda: busca y clic en un atajo. Derecha: renombra, edita el comando y las opciones.";
        final int x = bodyX();
        final int y = bodyY();

        if (shortcuts.isEmpty()) {
            this.labels.add(new Label("§7No hay atajos todavia. Ve a §fCrear§7 para anadir el primero.", x, y + 8, 0xAAAAAA));
            addRenderableWidget(Button.builder(Component.literal("§b+ Crear atajo"), b -> {
                this.activeTab = GuiTab.CREATE;
                rebuildWidgets();
            }).bounds(x, y + 26, 140, 18).build());
            return;
        }

        final int colW = (bodyW() - 8) / 2;
        final int rightX = x + colW + 8;

        // --- Columna izquierda: búsqueda + lista. ---
        final EditBox search = new EditBox(this.font, x, y, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar atajo..."));
        addRenderableWidget(search);

        this.listSelector = new ScrollSelector<>(x, y + 20, colW, bodyH() - 22, 14,
                Shortcut::listLabel,
                s -> s.alias() + " " + s.command() + " " + s.description());
        this.listSelector.setItems(shortcuts);
        this.listSelector.onSelect(s -> {
            loadSelection(s);
            rebuildWidgets();
        });
        search.setResponder(this.listSelector::setQuery);
        addRenderableWidget(this.listSelector);
        if (selectedAlias != null) {
            this.listSelector.selectMatching(s -> s.alias().equals(selectedAlias));
        }

        // --- Columna derecha: editor del seleccionado. ---
        if (selectedAlias == null) {
            this.labels.add(new Label("§7Selecciona un atajo de la lista \u2190", rightX, y + 8, 0xAAAAAA));
            return;
        }

        this.labels.add(new Label("§bEditando: §f/" + selectedAlias, rightX, y, 0xFFFFFF));

        this.labels.add(new Label("§7Alias (renombrar):", rightX, y + 16, 0xAAAAAA));
        final EditBox aliasBox = new EditBox(this.font, rightX, y + 26, colW, 16, Component.empty());
        aliasBox.setMaxLength(32);
        aliasBox.setValue(editAlias);
        aliasBox.setResponder(s -> this.editAlias = s);
        addRenderableWidget(aliasBox);

        this.labels.add(new Label("§7Comando original:", rightX, y + 46, 0xAAAAAA));
        final EditBox cmdBox = new EditBox(this.font, rightX, y + 56, colW, 16, Component.empty());
        cmdBox.setMaxLength(256);
        cmdBox.setValue(editCommand);
        cmdBox.setHint(Component.literal("ej: gamemode creative {args}"));
        cmdBox.setResponder(s -> this.editCommand = s);
        addRenderableWidget(cmdBox);

        this.labels.add(new Label("§7Descripcion:", rightX, y + 76, 0xAAAAAA));
        final EditBox descBox = new EditBox(this.font, rightX, y + 86, colW, 16, Component.empty());
        descBox.setMaxLength(128);
        descBox.setValue(editDescription);
        descBox.setHint(Component.literal("(opcional)"));
        descBox.setResponder(s -> this.editDescription = s);
        addRenderableWidget(descBox);

        final int half = (colW - 4) / 2;
        addRenderableWidget(Button.builder(Component.literal(toggleLabel("Argumentos", editUseArgs)), b -> {
            this.editUseArgs = !this.editUseArgs;
            rebuildWidgets();
        }).bounds(rightX, y + 108, half, 16).build());
        addRenderableWidget(Button.builder(Component.literal(toggleLabel("Reemplazar", editReplace)), b -> {
            this.editReplace = !this.editReplace;
            rebuildWidgets();
        }).bounds(rightX + half + 4, y + 108, colW - half - 4, 16).build());

        addRenderableWidget(Button.builder(Component.literal("§cEliminar atajo"), b -> deleteSelected())
                .bounds(rightX, y + 130, colW, 16).build());
    }

    private void loadSelection(Shortcut s) {
        this.selectedAlias = s.alias();
        this.editAlias = s.alias();
        this.editCommand = s.command();
        this.editDescription = s.description();
        this.editUseArgs = s.useArgs();
        this.editReplace = s.replaceOriginal();
    }

    private void saveSelected() {
        if (selectedAlias == null) {
            return;
        }
        FSNetwork.sendToServer(new SaveShortcutPacket(
                selectedAlias, editAlias, editCommand, editDescription, editUseArgs, editReplace));
    }

    private void deleteSelected() {
        if (selectedAlias == null) {
            return;
        }
        FSNetwork.sendToServer(new DeleteShortcutPacket(selectedAlias));
        this.selectedAlias = null;
    }

    // ------------------------------------------------------------------
    // Tab: CREATE
    // ------------------------------------------------------------------

    private void initCreate() {
        this.helpLine = "Crea un atajo nuevo. Usa {args} para pasar argumentos. El atajo respeta tus permisos.";
        final int x = bodyX();
        final int y = bodyY();
        final int w = Math.min(bodyW(), 360);

        this.labels.add(new Label("§7Alias (lo que escribiras, sin barra):", x, y, 0xAAAAAA));
        final EditBox aliasBox = new EditBox(this.font, x, y + 12, w, 18, Component.empty());
        aliasBox.setMaxLength(32);
        aliasBox.setValue(createAlias);
        aliasBox.setHint(Component.literal("ej: gc"));
        aliasBox.setResponder(s -> this.createAlias = s);
        addRenderableWidget(aliasBox);

        this.labels.add(new Label("§7Comando original (sin barra):", x, y + 38, 0xAAAAAA));
        final EditBox cmdBox = new EditBox(this.font, x, y + 50, w, 18, Component.empty());
        cmdBox.setMaxLength(256);
        cmdBox.setValue(createCommand);
        cmdBox.setHint(Component.literal("ej: gamemode creative {args}"));
        cmdBox.setResponder(s -> this.createCommand = s);
        addRenderableWidget(cmdBox);

        addRenderableWidget(Button.builder(Component.literal("§aCrear atajo"), b -> createNew())
                .bounds(x, y + 78, w / 2 - 4, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Limpiar"), b -> {
            this.createAlias = "";
            this.createCommand = "";
            rebuildWidgets();
        }).bounds(x + w / 2 + 4, y + 78, w / 2 - 4, 18).build());

        this.labels.add(new Label("§8Ejemplo: alias §7gc §8-> comando §7gamemode creative", x, y + 104, 0x808080));
        this.labels.add(new Label("§8Ejemplo: alias §7tp §8-> comando §7teleport {args}", x, y + 116, 0x808080));
    }

    private void createNew() {
        final String alias = createAlias.trim();
        final String command = createCommand.trim();
        if (alias.isEmpty() || command.isEmpty()) {
            return;
        }
        FSNetwork.sendToServer(new CreateShortcutPacket(alias, command));
        this.createAlias = "";
        this.createCommand = "";
    }

    // ------------------------------------------------------------------
    // Tab: SETTINGS
    // ------------------------------------------------------------------

    private void initSettings() {
        this.helpLine = "Ajustes globales. Se editan en config/fantasticshortcuts/config.toml y requieren recargar.";
        final int x = bodyX();
        final int y = bodyY();
        this.labels.add(new Label("§b\u2726 Configuracion (config.toml)", x, y, 0xFFFFFF));
        this.labels.add(new Label("§7enableReplaceMode: " + onOff(cfgReplaceMode), x, y + 18, 0xAAAAAA));
        this.labels.add(new Label("§8  Si esta activo, los atajos con 'Reemplazar' ocultan el comando original del TAB.", x, y + 30, 0x808080));
        this.labels.add(new Label("§7shortcutPriority: §f" + cfgPriority, x, y + 48, 0xAAAAAA));
        this.labels.add(new Label("§8  Prioridad cuando un atajo y un comando comparten literal.", x, y + 60, 0x808080));
        this.labels.add(new Label("§7auditEnabled: " + onOff(cfgAudit), x, y + 78, 0xAAAAAA));
        this.labels.add(new Label("§8  Registra eventos en config/fantasticshortcuts/audit/.", x, y + 90, 0x808080));
        this.labels.add(new Label("§7Total de atajos: §f" + shortcuts.size(), x, y + 112, 0xAAAAAA));
    }

    // ------------------------------------------------------------------
    // Render
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(0, 0, this.width, this.height, 0x90000000);

        // Panel + bordes de acento.
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + panelHeight, PANEL_BG);
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + 20, HEADER_BG);
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + 1, ACCENT);
        g.fill(leftPos, topPos + panelHeight - 1, leftPos + panelWidth, topPos + panelHeight, ACCENT);
        g.fill(leftPos, topPos, leftPos + 1, topPos + panelHeight, ACCENT);
        g.fill(leftPos + panelWidth - 1, topPos, leftPos + panelWidth, topPos + panelHeight, ACCENT);
        g.fill(leftPos + 6, topPos + 46, leftPos + panelWidth - 6, topPos + 47, SEPARATOR);

        // Título y subtítulo.
        g.drawString(this.font, "§b\u2726 §fFantastic Shortcuts §b\u2726", leftPos + 8, topPos + 6, 0xFFFFFF, false);
        final String sub = "§7v1.0.0 - Pewez";
        g.drawString(this.font, sub, leftPos + panelWidth - this.font.width(sub) - 8, topPos + 6, 0xAAAAAA, false);

        if (helpLine != null && !helpLine.isEmpty()) {
            final String trimmed = this.font.plainSubstrByWidth("§7" + helpLine, panelWidth - 16);
            g.drawString(this.font, trimmed, leftPos + 8, topPos + 50, HELP_COLOR, false);
        }

        super.render(g, mouseX, mouseY, partialTick);

        for (Label l : labels) {
            g.drawString(this.font, l.text(), l.x(), l.y(), l.color(), false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    private static String toggleLabel(String name, boolean state) {
        return name + ": " + (state ? "§aActivado" : "§7Desactivado");
    }

    private static String onOff(boolean state) {
        return state ? "§aActivado" : "§cDesactivado";
    }

    private record Label(String text, int x, int y, int color) {}
}
