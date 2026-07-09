package com.fsmobs.client.screen;

import com.fsmobs.MobControl;
import com.fsmobs.client.ClientState;
import com.fsmobs.client.RegistryLists;
import com.fsmobs.client.Sfx;
import com.fsmobs.client.widget.ScrollSelector;
import com.fsmobs.network.Net;
import com.fsmobs.network.SetConfigPacket;
import com.fsmobs.network.ToggleStatsPacket;
import com.fsmobs.stats.ServerStats;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * GUI de control de mobs con dos pestanas:
 *  - "Limites": escribe topes (por radio) por categoria y por mob concreto + radio y aparicion.
 *  - "Estadisticas": rendimiento del servidor y conteo de mobs cerca de ti y en general.
 * Los valores se escriben a mano y se aplican al pulsar "Guardar y aplicar" (o Enter).
 */
public final class MobControlScreen extends Screen {

    private static final String[] CAT_LABELS = {
            "Monstruos", "Animales", "Ambiente (murcielagos)", "Criaturas de agua",
            "Peces / agua ambiente", "Ajolotes", "Agua subterranea"
    };

    private int activeTab = 0; // 0 = Limites, 1 = Estadisticas
    private boolean statsWatching = false;

    private int leftPos;
    private int topPos;
    private int panelW;
    private int panelH;

    private EntityType<?> selectedMob;
    private EditBox radiusBox;
    private EditBox multBox;
    private EditBox mobBox;
    private final Map<String, EditBox> catBoxes = new LinkedHashMap<>();
    private long saveFlashUntil;

    private final List<Label> labels = new ArrayList<>();

    public MobControlScreen() {
        super(Component.literal("Fantastic Mobs"));
    }

    /** Refresca si llega config del servidor, pero no interrumpe mientras escribes. */
    public void onConfigSynced() {
        if (this.getFocused() instanceof EditBox) {
            return;
        }
        this.rebuildWidgets();
    }

    private void send(int op, String id, double value) {
        Net.CHANNEL.sendToServer(new SetConfigPacket(op, id, value));
    }

    // -------------------------------------------------- init

    @Override
    protected void init() {
        this.panelW = Math.min(this.width - 16, 512);
        this.panelH = Math.min(this.height - 16, 330);
        this.leftPos = (this.width - this.panelW) / 2;
        this.topPos = (this.height - this.panelH) / 2;
        this.labels.clear();
        this.catBoxes.clear();
        this.mobBox = null;

        boolean wantStats = this.activeTab == 1;
        if (wantStats != this.statsWatching) {
            this.statsWatching = wantStats;
            Net.CHANNEL.sendToServer(new ToggleStatsPacket(wantStats));
        }

        int tabW = 100;
        this.addRenderableWidget(tab(this.leftPos + 8, this.topPos + 20, tabW, "Limites", 0,
                "Ajusta cuantos mobs pueden aparecer."));
        this.addRenderableWidget(tab(this.leftPos + 8 + tabW + 4, this.topPos + 20, tabW, "Estadisticas", 1,
                "Mira el rendimiento y el conteo de mobs."));
        this.addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> this.onClose())
                .bounds(this.leftPos + this.panelW - 66, this.topPos + 20, 58, 16).build());

        if (this.activeTab == 0) {
            addLabel("\u00a77Limita cuantos mobs aparecen en un radio. Escribe el numero y pulsa Guardar.", this.leftPos + 10, this.topPos + 40);
            initLimits();
        } else {
            addLabel("\u00a77Rendimiento del servidor y cuantos mobs hay cerca de ti y en total.", this.leftPos + 10, this.topPos + 40);
        }
    }

    private Button tab(int x, int y, int w, String label, int tab, String tip) {
        return Button.builder(Component.literal(this.activeTab == tab ? "\u00a7f\u00a7l" + label : "\u00a77" + label),
                        b -> switchTab(tab))
                .bounds(x, y, w, 16)
                .tooltip(Tooltip.create(Component.literal(tip))).build();
    }

    private void switchTab(int tab) {
        this.activeTab = tab;
        Sfx.click();
        this.rebuildWidgets();
    }

    private void initLimits() {
        int x = this.leftPos + 12;
        int leftW = 234;
        int fieldW = 54;
        int fieldX = x + leftW - fieldW;
        int contentTop = this.topPos + 52;
        int contentBottom = this.topPos + this.panelH - 30;

        // ----- Columna izquierda -----
        int y = contentTop;
        this.radiusBox = numberBox(fieldX, y, fieldW, String.valueOf(ClientState.radius), 3, "32");
        this.addRenderableWidget(this.radiusBox);
        addLabel("\u00a7fRadio \u00a77(bloques)", x, y + 4);
        y += 22;

        this.multBox = numberBox(fieldX, y, fieldW, String.valueOf((int) Math.round(ClientState.multiplier * 100)), 3, "100");
        this.addRenderableWidget(this.multBox);
        addLabel("\u00a7fAparicion natural \u00a77(%)", x, y + 4);
        y += 24;

        addLabel("\u00a7bTopes por categoria \u00a77(max dentro del radio)", x, y);
        y += 13;
        for (int i = 0; i < MobControl.CATEGORIES.length; i++) {
            String cat = MobControl.CATEGORIES[i];
            int cap = ClientState.categoryCaps.getOrDefault(cat, -1);
            EditBox box = numberBox(fieldX, y, fieldW, cap < 0 ? "" : String.valueOf(cap), 3, "\u221e");
            this.catBoxes.put(cat, box);
            this.addRenderableWidget(box);
            addLabel("\u00a7f" + CAT_LABELS[i], x, y + 4);
            y += 19;
        }
        addLabel("\u00a78Vacio = sin limite \u00b7 0 = ninguno \u00b7 ej. 20", x, y + 2);

        // ----- Columna derecha: topes por mob concreto -----
        int rightX = x + leftW + 14;
        int rightW = this.panelW - (rightX - this.leftPos) - 12;
        addLabel("\u00a7bTopes por mob especifico", rightX, contentTop);
        addLabel("\u00a77Busca, elige un mob y ponle su tope propio.", rightX, contentTop + 11);

        EditBox search = new EditBox(this.font, rightX, contentTop + 23, rightW, 16, Component.empty());
        search.setHint(Component.literal("Buscar mob..."));
        this.addRenderableWidget(search);

        int selRowY = contentBottom - 20;
        int listTop = contentTop + 43;
        int listH = (selRowY - 8) - listTop;
        ScrollSelector<EntityType<?>> picker = new ScrollSelector<EntityType<?>>(rightX, listTop, rightW, listH, 18,
                RegistryLists::name,
                t -> RegistryLists.name(t) + " " + RegistryLists.id(t),
                RegistryLists::icon)
                .withCheckbox(t -> ClientState.typeCaps.containsKey(RegistryLists.id(t)))
                .onSelect(t -> {
                    this.selectedMob = t;
                    Sfx.click();
                    this.rebuildWidgets();
                });
        picker.setItems(RegistryLists.mobs());
        search.setResponder(picker::setQuery);
        this.addRenderableWidget(picker);

        if (this.selectedMob != null) {
            String id = RegistryLists.id(this.selectedMob);
            Integer capObj = ClientState.typeCaps.get(id);
            int cap = capObj == null ? -1 : capObj;
            addLabel("\u00a7e" + RegistryLists.name(this.selectedMob) + " \u00a77(max):", rightX, selRowY - 10);
            this.mobBox = numberBox(rightX, selRowY, 54, cap < 0 ? "" : String.valueOf(cap), 3, "\u221e");
            this.addRenderableWidget(this.mobBox);
            this.addRenderableWidget(Button.builder(Component.literal("Quitar"), b -> {
                        ClientState.typeCaps.remove(id);
                        send(SetConfigPacket.OP_TYPE, id, -1);
                        this.mobBox.setValue("");
                        this.selectedMob = null;
                        Sfx.click();
                        this.rebuildWidgets();
                    }).bounds(rightX + 58, selRowY, 56, 16)
                    .tooltip(Tooltip.create(Component.literal("Quita el tope propio de este mob (vuelve a usar el de su categoria)."))).build());
        } else {
            addLabel("\u00a77Elige un mob de la lista de arriba.", rightX, selRowY + 2);
        }

        // ----- Barra inferior: guardar -----
        int barY = this.topPos + this.panelH - 24;
        this.addRenderableWidget(Button.builder(Component.literal("\u00a7aGuardar y aplicar"), b -> save())
                .bounds(x, barY, 130, 18)
                .tooltip(Tooltip.create(Component.literal("Guarda y aplica todos los valores al momento. Tambien puedes pulsar Enter."))).build());
    }

    private EditBox numberBox(int x, int y, int w, String value, int maxLen, String hint) {
        EditBox box = new EditBox(this.font, x, y, w, 16, Component.empty());
        box.setMaxLength(maxLen);
        box.setFilter(s -> s.isEmpty() || s.matches("[0-9]{1," + maxLen + "}"));
        box.setValue(value);
        box.setHint(Component.literal(hint));
        return box;
    }

    // -------------------------------------------------- guardar

    private void save() {
        Integer r = digits(this.radiusBox.getValue());
        if (r != null) {
            int v = Math.max(4, Math.min(128, r));
            ClientState.radius = v;
            send(SetConfigPacket.OP_RADIUS, "", v);
        }
        Integer m = digits(this.multBox.getValue());
        if (m != null) {
            int v = Math.max(0, Math.min(100, m));
            ClientState.multiplier = v / 100.0;
            send(SetConfigPacket.OP_MULT, "", v / 100.0);
        }
        for (Map.Entry<String, EditBox> e : this.catBoxes.entrySet()) {
            int cap = parseCap(e.getValue().getValue());
            ClientState.categoryCaps.put(e.getKey(), cap);
            send(SetConfigPacket.OP_CATEGORY, e.getKey(), cap);
        }
        if (this.selectedMob != null && this.mobBox != null) {
            String id = RegistryLists.id(this.selectedMob);
            int cap = parseCap(this.mobBox.getValue());
            if (cap < 0) {
                ClientState.typeCaps.remove(id);
                send(SetConfigPacket.OP_TYPE, id, -1);
            } else {
                ClientState.typeCaps.put(id, cap);
                send(SetConfigPacket.OP_TYPE, id, cap);
            }
        }
        this.saveFlashUntil = System.currentTimeMillis() + 1800;
        Sfx.click();
    }

    private static Integer digits(String s) {
        s = s.trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Vacio = -1 (sin limite); si no, numero limitado a 0..999. */
    private static int parseCap(String s) {
        Integer d = digits(s);
        return d == null ? -1 : Math.max(0, Math.min(999, d));
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (this.activeTab == 0 && (key == 257 || key == 335)) { // Enter / Enter numerico
            save();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void addLabel(String text, int x, int y) {
        this.labels.add(new Label(text, x, y));
    }

    // -------------------------------------------------- render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelW, this.topPos + this.panelH, -535160294);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelW, this.topPos + 16, -14013910);
        g.fill(this.leftPos, this.topPos + this.panelH - 1, this.leftPos + this.panelW, this.topPos + this.panelH, -12961222);
        g.drawString(this.font, "\u00a76\u2726 Fantastic Mobs \u00a77- control de cantidad de mobs",
                this.leftPos + 8, this.topPos + 4, 0xFFFFFF, false);
        // Linea divisoria entre columnas (solo en Limites)
        if (this.activeTab == 0) {
            int divX = this.leftPos + 12 + 234 + 6;
            g.fill(divX, this.topPos + 50, divX + 1, this.topPos + this.panelH - 30, -12961222);
        }

        super.render(g, mouseX, mouseY, partial);

        for (Label l : this.labels) {
            g.drawString(this.font, l.text, l.x, l.y, 0xE0E0E0, false);
        }

        if (this.activeTab == 0 && System.currentTimeMillis() < this.saveFlashUntil) {
            g.drawString(this.font, "\u00a7a\u2713 Guardado y aplicado", this.leftPos + 148, this.topPos + this.panelH - 19, 0xFFFFFF, false);
        }

        if (this.activeTab == 1) {
            renderStats(g);
        }
    }

    private void renderStats(GuiGraphics g) {
        int x = this.leftPos + 12;
        int y = this.topPos + 54;
        ServerStats s = ClientState.stats();
        if (s == null) {
            g.drawString(this.font, "\u00a77Recopilando datos del servidor... (aparecen en ~1 segundo)", x, y, 0xE0E0E0, false);
            return;
        }
        int line = 11;
        g.drawString(this.font, tpsColor(s.tps) + "TPS: " + fmt(s.tps) + "\u00a78/20   \u00a7fMSPT: \u00a7e" + fmt(s.mspt) + " ms", x, y, 0xFFFFFF, false);
        y += line;
        g.drawString(this.font, "\u00a78Ritmo del servidor: 20 TPS = perfecto. Menos = lag. MSPT = ms por tick (menos es mejor).", x, y, 0xFFFFFF, false);
        y += line + 3;
        g.drawString(this.font, "\u00a77RAM: \u00a7f" + s.memUsed + "\u00a77/\u00a7f" + s.memMax + " MB     \u00a77Chunks cargados: \u00a7f" + s.loadedChunks + "     \u00a77Dim: \u00a7f" + shortDim(s.dim), x, y, 0xFFFFFF, false);
        y += line + 6;

        g.drawString(this.font, "\u00a7eEn tu radio de tope \u00a77(" + s.radius + " bloques): \u00a7f" + s.totalMobsNear() + " mobs", x, y, 0xFFFFFF, false);
        y += line;
        g.drawString(this.font, "\u00a78Esta es la zona donde se aplican los topes.", x, y, 0xFFFFFF, false);
        y += line;
        y = renderGroups(g, x + 6, y, s.near);
        y += 4;

        g.drawString(this.font, "\u00a76A tu alrededor \u00a77(" + s.zoneRadius + " bloques): \u00a7f" + s.totalMobsZone() + " mobs", x, y, 0xFFFFFF, false);
        y += line;
        g.drawString(this.font, "\u00a78Area amplia, parecida a lo que ves en el minimapa.", x, y, 0xFFFFFF, false);
        y += line;
        y = renderGroups(g, x + 6, y, s.zone);
        y += 4;

        g.drawString(this.font, "\u00a7bEn toda la dimension: \u00a7f" + s.totalMobsGlobal() + " mobs \u00a77(" + s.totalEntities + " entidades)", x, y, 0xFFFFFF, false);
        y += line;
        renderGroups(g, x + 6, y, s.global);
    }

    private int renderGroups(GuiGraphics g, int x, int y, int[] counts) {
        String[] colors = {"\u00a7c", "\u00a7a", "\u00a7e", "\u00a79", "\u00a7d", "\u00a77"};
        for (int i = 0; i < ServerStats.GROUPS.length; i++) {
            int col = x + (i % 3) * 158;
            int row = y + (i / 3) * 11;
            g.drawString(this.font, colors[i] + ServerStats.GROUPS[i] + ": \u00a7f" + counts[i], col, row, 0xFFFFFF, false);
        }
        return y + 22;
    }

    private static String tpsColor(float tps) {
        if (tps >= 19.0f) {
            return "\u00a7a";
        }
        if (tps >= 15.0f) {
            return "\u00a7e";
        }
        return "\u00a7c";
    }

    private static String fmt(float v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String shortDim(String dim) {
        int i = dim.indexOf(':');
        return i >= 0 ? dim.substring(i + 1) : dim;
    }

    @Override
    public void onClose() {
        if (this.statsWatching) {
            Net.CHANNEL.sendToServer(new ToggleStatsPacket(false));
            this.statsWatching = false;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Label(String text, int x, int y) {
    }
}
