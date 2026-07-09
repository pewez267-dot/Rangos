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
import java.util.List;
import java.util.Locale;

/**
 * GUI de control de mobs. Pestana "Limites": radio, multiplicador de aparicion, topes por categoria y
 * topes por mob concreto. Pestana "Estadisticas": rendimiento del servidor y conteo de mobs cerca de
 * ti y en la dimension. Todo con texto descriptivo.
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
    private final List<Label> labels = new ArrayList<>();

    public MobControlScreen() {
        super(Component.literal("Fantastic Mobs"));
    }

    public void onConfigSynced() {
        this.rebuildWidgets();
    }

    // -------------------------------------------------- helpers de envio

    private void send(int op, String id, double value) {
        Net.CHANNEL.sendToServer(new SetConfigPacket(op, id, value));
    }

    private int catCap(String cat) {
        return ClientState.categoryCaps.getOrDefault(cat, -1);
    }

    private String capText(int v) {
        return v < 0 ? "\u221e" : String.valueOf(v);
    }

    // -------------------------------------------------- init

    @Override
    protected void init() {
        this.panelW = Math.min(this.width - 16, 470);
        this.panelH = Math.min(this.height - 16, 306);
        this.leftPos = (this.width - this.panelW) / 2;
        this.topPos = (this.height - this.panelH) / 2;
        this.labels.clear();

        // Avisar al servidor si queremos recibir estadisticas (solo cuando cambia la pestana).
        boolean wantStats = this.activeTab == 1;
        if (wantStats != this.statsWatching) {
            this.statsWatching = wantStats;
            Net.CHANNEL.sendToServer(new ToggleStatsPacket(wantStats));
        }

        int tabW = 96;
        this.addRenderableWidget(Button.builder(Component.literal(this.activeTab == 0 ? "\u00a7f\u00a7lLimites" : "\u00a77Limites"),
                b -> switchTab(0)).bounds(this.leftPos + 8, this.topPos + 20, tabW, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal(this.activeTab == 1 ? "\u00a7f\u00a7lEstadisticas" : "\u00a77Estadisticas"),
                b -> switchTab(1)).bounds(this.leftPos + 8 + tabW + 4, this.topPos + 20, tabW, 16).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> this.onClose())
                .bounds(this.leftPos + this.panelW - 68, this.topPos + 20, 60, 16).build());

        if (this.activeTab == 0) {
            initLimits();
        }
        // La pestana de estadisticas se dibuja en render() (solo texto).
    }

    private void switchTab(int tab) {
        this.activeTab = tab;
        Sfx.click();
        this.rebuildWidgets();
    }

    private void initLimits() {
        int x = this.leftPos + 10;
        int y = this.topPos + 44;
        int colW = (this.panelW - 28) / 2;
        int rightX = x + colW + 8;

        // ---- Columna izquierda: globales + categorias ----
        // Radio
        addLabel("\u00a7eRadio: \u00a7f" + ClientState.radius + " bloques", x, y + 3);
        int bx = x + colW - 40;
        this.addRenderableWidget(mini(bx, y, "-", () -> { ClientState.radius = Math.max(4, ClientState.radius - 4); send(SetConfigPacket.OP_RADIUS, "", ClientState.radius); },
                "Zona donde se cuentan los mobs para aplicar el tope. Menor = topes mas locales."));
        this.addRenderableWidget(mini(bx + 20, y, "+", () -> { ClientState.radius = Math.min(128, ClientState.radius + 4); send(SetConfigPacket.OP_RADIUS, "", ClientState.radius); },
                "Zona donde se cuentan los mobs para aplicar el tope. Mayor = cuenta un area mas grande."));

        // Multiplicador de aparicion
        y += 18;
        int pct = (int) Math.round(ClientState.multiplier * 100);
        addLabel("\u00a7eAparicion natural: \u00a7f" + pct + "%", x, y + 3);
        this.addRenderableWidget(mini(bx, y, "-", () -> { ClientState.multiplier = Math.max(0.0, ClientState.multiplier - 0.1); send(SetConfigPacket.OP_MULT, "", ClientState.multiplier); },
                "Probabilidad de que un mob aparezca de forma natural. 100% = normal, 50% = aparecen la mitad, 0% = ninguno."));
        this.addRenderableWidget(mini(bx + 20, y, "+", () -> { ClientState.multiplier = Math.min(1.0, ClientState.multiplier + 0.1); send(SetConfigPacket.OP_MULT, "", ClientState.multiplier); },
                "Probabilidad de que un mob aparezca de forma natural. 100% = normal, 50% = aparecen la mitad."));

        // Separador
        y += 20;
        addLabel("\u00a7bTopes por categoria \u00a77(max en el radio)", x, y);
        y += 12;

        // Categorias
        for (int i = 0; i < MobControl.CATEGORIES.length; i++) {
            String cat = MobControl.CATEGORIES[i];
            int cap = catCap(cat);
            addLabel("\u00a7f" + CAT_LABELS[i] + ": \u00a7e" + capText(cap), x, y + 3);
            int cbx = x + colW - 58;
            String tip = "Maximo de '" + CAT_LABELS[i] + "' dentro del radio. \u221e = sin limite, 0 = ninguno.";
            this.addRenderableWidget(mini(cbx, y, "\u221e", () -> { ClientState.categoryCaps.put(cat, -1); send(SetConfigPacket.OP_CATEGORY, cat, -1); }, tip));
            this.addRenderableWidget(mini(cbx + 18, y, "-", () -> { int v = Math.max(0, (catCap(cat) < 0 ? 0 : catCap(cat)) - 10); ClientState.categoryCaps.put(cat, v); send(SetConfigPacket.OP_CATEGORY, cat, v); }, tip));
            this.addRenderableWidget(mini(cbx + 36, y, "+", () -> { int v = Math.min(999, (catCap(cat) < 0 ? 0 : catCap(cat)) + 10); ClientState.categoryCaps.put(cat, v); send(SetConfigPacket.OP_CATEGORY, cat, v); }, tip));
            y += 16;
        }

        // ---- Columna derecha: topes por mob concreto ----
        addLabel("\u00a7bTopes por mob \u00a77(busca y elige un mob)", rightX, this.topPos + 44);
        EditBox search = new EditBox(this.font, rightX, this.topPos + 56, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar mob..."));
        this.addRenderableWidget(search);

        int listH = this.panelH - (56 - 44) - 44 - 56;
        ScrollSelector<EntityType<?>> picker = new ScrollSelector<EntityType<?>>(rightX, this.topPos + 74, colW, listH, 18,
                RegistryLists::name,
                t -> RegistryLists.name(t) + " " + RegistryLists.id(t),
                RegistryLists::icon)
                .withCheckbox(t -> ClientState.typeCaps.containsKey(RegistryLists.id(t)))
                .onSelect(t -> { this.selectedMob = t; Sfx.click(); this.rebuildWidgets(); });
        picker.setItems(RegistryLists.mobs());
        search.setResponder(picker::setQuery);
        this.addRenderableWidget(picker);

        // Stepper del mob seleccionado
        int sy = this.topPos + this.panelH - 30;
        if (this.selectedMob != null) {
            String id = RegistryLists.id(this.selectedMob);
            Integer capObj = ClientState.typeCaps.get(id);
            int cap = capObj == null ? -1 : capObj;
            addLabel("\u00a7f" + RegistryLists.name(this.selectedMob) + ": \u00a7e" + capText(cap), rightX, sy - 12);
            String tip = "Maximo de este mob concreto dentro del radio. \u221e = sin limite (usa la categoria).";
            int sbx = rightX;
            this.addRenderableWidget(mini(sbx, sy, "\u221e", () -> { ClientState.typeCaps.remove(id); send(SetConfigPacket.OP_TYPE, id, -1); }, "Quita el tope de este mob (vuelve a mandar su categoria)."));
            this.addRenderableWidget(mini(sbx + 18, sy, "-5", () -> { int v = Math.max(0, (cap < 0 ? 0 : cap) - 5); ClientState.typeCaps.put(id, v); send(SetConfigPacket.OP_TYPE, id, v); }, tip));
            this.addRenderableWidget(mini(sbx + 38, sy, "-1", () -> { int v = Math.max(0, (cap < 0 ? 0 : cap) - 1); ClientState.typeCaps.put(id, v); send(SetConfigPacket.OP_TYPE, id, v); }, tip));
            this.addRenderableWidget(mini(sbx + 58, sy, "+1", () -> { int v = Math.min(999, (cap < 0 ? 0 : cap) + 1); ClientState.typeCaps.put(id, v); send(SetConfigPacket.OP_TYPE, id, v); }, tip));
            this.addRenderableWidget(mini(sbx + 78, sy, "+5", () -> { int v = Math.min(999, (cap < 0 ? 0 : cap) + 5); ClientState.typeCaps.put(id, v); send(SetConfigPacket.OP_TYPE, id, v); }, tip));
        } else {
            addLabel("\u00a77Elige un mob de la lista para ponerle un tope propio.", rightX, sy - 6);
        }
    }

    private Button mini(int x, int y, String label, Runnable action, String tooltip) {
        Button.Builder b = Button.builder(Component.literal(label), btn -> {
            action.run();
            Sfx.click();
            this.rebuildWidgets();
        }).bounds(x, y, label.length() > 1 ? 18 : 16, 14);
        if (tooltip != null) {
            b.tooltip(Tooltip.create(Component.literal(tooltip)));
        }
        return b.build();
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

        super.render(g, mouseX, mouseY, partial);

        for (Label l : this.labels) {
            g.drawString(this.font, l.text, l.x, l.y, 0xE0E0E0, false);
        }

        if (this.activeTab == 1) {
            renderStats(g);
        }
    }

    private void renderStats(GuiGraphics g) {
        int x = this.leftPos + 12;
        int y = this.topPos + 46;
        ServerStats s = ClientState.stats();
        if (s == null) {
            g.drawString(this.font, "\u00a77Recopilando datos del servidor...", x, y, 0xE0E0E0, false);
            return;
        }
        int line = 12;
        g.drawString(this.font, tpsColor(s.tps) + "TPS: " + fmt(s.tps) + "\u00a77 (objetivo 20)  \u00a7fMSPT: " + fmt(s.mspt) + " ms", x, y, 0xFFFFFF, false);
        y += line;
        g.drawString(this.font, "\u00a77Ritmo del servidor: menos TPS = mas lag. MSPT = ms por tick.", x, y, 0xA0A0A0, false);
        y += line + 4;
        g.drawString(this.font, "\u00a77RAM: \u00a7f" + s.memUsed + " \u00a77/ \u00a7f" + s.memMax + " MB    \u00a77Chunks cargados: \u00a7f" + s.loadedChunks, x, y, 0xFFFFFF, false);
        y += line;
        g.drawString(this.font, "\u00a77Dimension: \u00a7f" + s.dim, x, y, 0xFFFFFF, false);
        y += line + 6;

        g.drawString(this.font, "\u00a7eCerca de ti \u00a77(radio " + s.radius + " bloques): \u00a7f" + s.totalMobsNear() + " mobs", x, y, 0xFFFFFF, false);
        y += line;
        y = renderGroups(g, x + 6, y, s.near);
        y += 6;

        g.drawString(this.font, "\u00a7bEn toda la dimension: \u00a7f" + s.totalMobsGlobal() + " mobs \u00a77(" + s.totalEntities + " entidades en total)", x, y, 0xFFFFFF, false);
        y += line;
        renderGroups(g, x + 6, y, s.global);
    }

    private int renderGroups(GuiGraphics g, int x, int y, int[] counts) {
        for (int i = 0; i < ServerStats.GROUPS.length; i++) {
            int col = x + (i % 3) * 140;
            int row = y + (i / 3) * 12;
            g.drawString(this.font, "\u00a77" + ServerStats.GROUPS[i] + ": \u00a7f" + counts[i], col, row, 0xFFFFFF, false);
        }
        return y + 24;
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
