package com.fscrates.client.screen;

import com.fscrates.animation.AnimationRegistry;
import com.fscrates.animation.CrateAnimation;
import com.fscrates.client.RegistryLists;
import com.fscrates.client.widget.ScrollSelector;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import com.fscrates.config.RewardEntry;
import com.fscrates.network.FSNetwork;
import com.fscrates.network.SaveCratePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The modern, in-game crate editor. Tabbed (Recompensas, Probabilidad,
 * Animacion, Apariencia, Llave, Ajustes), fully clickable, Spanish UI with
 * hover descriptions. Edits a working {@link CrateConfig} sent to the server on
 * "Guardar y Obtener".
 */
public class CrateEditorScreen extends Screen {

    private enum Tab {
        INFO("Info"), REWARDS("Recompensas"), PROBABILITY("Probabilidad"),
        ANIMATION("Animacion"), APPEARANCE("Apariencia"), KEY("Llave"), SETTINGS("Ajustes");

        final String label;
        Tab(String label) { this.label = label; }
    }

    private record Label(String text, int x, int y, int color) {}
    private record TooltipZone(int x, int y, int w, int h, List<Component> lines) {}

    private final CrateConfig config;
    private Tab activeTab = Tab.INFO;
    private final List<Label> labels = new ArrayList<>();
    private final List<TooltipZone> tooltipZones = new ArrayList<>();

    private int leftPos, topPos, panelWidth, panelHeight;
    private RewardEntry selectedReward;

    public CrateEditorScreen(CrateConfig config) {
        super(Component.literal("Editor de Crate"));
        this.config = config == null ? new CrateConfig() : config;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(this.width - 20, 470);
        panelHeight = Math.min(this.height - 20, 276);
        leftPos = (this.width - panelWidth) / 2;
        topPos = (this.height - panelHeight) / 2;
        labels.clear();
        tooltipZones.clear();

        initHeader();
        initFooter();

        switch (activeTab) {
            case INFO -> initInfo();
            case REWARDS -> initRewards();
            case PROBABILITY -> initProbability();
            case ANIMATION -> initAnimation();
            case APPEARANCE -> initAppearance();
            case KEY -> initKey();
            case SETTINGS -> initSettings();
        }
    }

    private int bodyX() { return leftPos + 8; }
    private int bodyY() { return topPos + 58; }
    private int bodyW() { return panelWidth - 16; }
    private int bodyH() { return panelHeight - 58 - 28; }

    private void initHeader() {
        Tab[] tabs = Tab.values();
        int gap = 2;
        int tabW = (panelWidth - 16 - gap * (tabs.length - 1)) / tabs.length;
        int x = leftPos + 8;
        int y = topPos + 24;
        for (Tab tab : tabs) {
            boolean active = tab == activeTab;
            String text = (active ? "\u00A7f" : "\u00A77") + tab.label;
            addRenderableWidget(Button.builder(Component.literal(text), b -> {
                this.activeTab = tab;
                this.rebuildWidgets();
            }).bounds(x, y, tabW, 18).build());
            x += tabW + gap;
        }
    }

    private void initFooter() {
        int w = 150;
        addRenderableWidget(Button.builder(Component.literal("Guardar y Obtener"), b -> {
            FSNetwork.sendToServer(new SaveCratePacket(config.save()));
            onClose();
        }).bounds(leftPos + panelWidth - w - 8, topPos + panelHeight - 24, w, 18).build());

        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(leftPos + 8, topPos + panelHeight - 24, 80, 18).build());
    }

    // ------------------------------------------------------------------
    // Tab: Info
    // ------------------------------------------------------------------

    private void initInfo() {
        int x = bodyX();
        int y = bodyY();

        EditBox id = new EditBox(font, x + 160, y, 200, 16, Component.empty());
        id.setMaxLength(48);
        id.setValue(config.id);
        id.setResponder(s -> config.id = s.trim().toLowerCase().replace(' ', '_'));
        addRenderableWidget(id);
        addLabel("ID de la crate:", x, y + 4, descId());

        EditBox name = new EditBox(font, x + 160, y + 24, 200, 16, Component.empty());
        name.setMaxLength(128);
        name.setValue(config.displayName);
        name.setResponder(s -> config.displayName = s);
        addRenderableWidget(name);
        addLabel("Nombre visible:", x, y + 28, desc("Nombre que se muestra en el item.",
                "Puedes usar c\u00f3digos de color con &."));

        addRenderableWidget(Button.builder(Component.literal("Rareza: " + config.rarity.color() + config.rarity.displayName()),
                b -> { config.rarity = config.rarity.next(); rebuildWidgets(); })
                .bounds(x + 160, y + 48, 200, 16).build());
        addLabel("Rareza:", x, y + 52, desc("Define color, modelo y partículas.",
                "Com\u00fan < Rara < \u00c9pica < Legendaria < M\u00edtica."));

        addIntField(x + 160, y + 72, 60, config.rolls, v -> config.rolls = Math.max(1, v),
                "Tiradas por apertura:", x, y + 76,
                desc("Cu\u00e1ntas recompensas (por peso) se entregan por apertura.",
                        "Las recompensas garantizadas se suman aparte.",
                        "Recomendado: 1."));

        addLabel("\u00A78Animaci\u00f3n: \u00A7f" + AnimationRegistry.get(config.animationId).displayName(), x, y + 100, null);
        addLabel("\u00A78Recompensas: \u00A7f" + config.rewards.size(), x, y + 112, null);
    }

    private List<Component> descId() {
        return desc("Identificador \u00fanico (sin espacios).",
                "Se usa en comandos: /fscrate give, key give, etc.",
                "Ej: cofre_legendario");
    }

    // ------------------------------------------------------------------
    // Tab: Rewards
    // ------------------------------------------------------------------

    private void initRewards() {
        int x = bodyX();
        int y = bodyY();
        int colW = (bodyW() - 8) / 2;
        int rightX = x + colW + 8;

        // left: item picker to add ITEM rewards
        EditBox search = new EditBox(font, x, y, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar item..."));
        addRenderableWidget(search);

        ScrollSelector<Item> items = new ScrollSelector<>(x, y + 20, colW, bodyH() - 22, 18,
                RegistryLists::itemName,
                it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it),
                it -> new ItemStack(it));
        items.setItems(RegistryLists.items());
        items.onSelect(it -> {
            RewardEntry r = new RewardEntry(RewardEntry.Type.ITEM);
            r.item = new ItemStack(it);
            r.label = RegistryLists.itemName(it);
            r.weight = 10;
            config.rewards.add(r);
            selectedReward = r;
            rebuildWidgets();
        });
        search.setResponder(items::setQuery);
        addRenderableWidget(items);

        // right: current rewards
        ScrollSelector<RewardEntry> current = new ScrollSelector<>(rightX, y, colW, bodyH() - 96, 16,
                r -> r.describe() + " \u00A77(peso " + r.weight + ")",
                r -> r.describe(),
                r -> r.type == RewardEntry.Type.ITEM ? r.item : ItemStack.EMPTY);
        current.setItems(new ArrayList<>(config.rewards));
        current.onSelect(r -> { selectedReward = r; rebuildWidgets(); });
        addRenderableWidget(current);

        // quick-add special reward types
        addRenderableWidget(Button.builder(Component.literal("+ Comando"), b -> {
            config.rewards.add(new RewardEntry(RewardEntry.Type.COMMAND));
            rebuildWidgets();
        }).bounds(rightX, y + bodyH() - 92, colW / 3 - 2, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+ XP"), b -> {
            RewardEntry r = new RewardEntry(RewardEntry.Type.XP); r.xp = 100; config.rewards.add(r);
            rebuildWidgets();
        }).bounds(rightX + colW / 3, y + bodyH() - 92, colW / 3 - 2, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+ Efecto"), b -> {
            config.rewards.add(new RewardEntry(RewardEntry.Type.EFFECT));
            rebuildWidgets();
        }).bounds(rightX + 2 * colW / 3, y + bodyH() - 92, colW / 3 - 2, 16).build());

        // selected reward editor
        if (selectedReward != null && config.rewards.contains(selectedReward)) {
            RewardEntry r = selectedReward;
            int fy = y + bodyH() - 70;
            addIntField(rightX + 44, fy, 44, r.weight, v -> r.weight = Math.max(0, v), "Peso", rightX, fy + 4,
                    desc("Probabilidad relativa frente a las dem\u00e1s recompensas.",
                            "M\u00e1s peso = m\u00e1s probable."));
            addIntField(rightX + 150, fy, 36, r.minAmount, v -> r.minAmount = Math.max(1, v), "Min", rightX + 110, fy + 4,
                    desc("Cantidad m\u00ednima entregada."));
            addIntField(rightX + 235, fy, 36, r.maxAmount, v -> r.maxAmount = Math.max(1, v), "Max", rightX + 200, fy + 4,
                    desc("Cantidad m\u00e1xima entregada."));

            addToggle(rightX, fy + 22, colW - 70, r.guaranteed ? "Garantizada: S\u00ed" : "Garantizada: No",
                    r.guaranteed, () -> { r.guaranteed = !r.guaranteed; rebuildWidgets(); },
                    desc("Si est\u00e1 activo, esta recompensa SIEMPRE se entrega."));
            addRenderableWidget(Button.builder(Component.literal("Quitar"), b -> {
                config.rewards.remove(r); selectedReward = null; rebuildWidgets();
            }).bounds(rightX + colW - 64, fy + 22, 64, 16).build());

            if (r.type == RewardEntry.Type.COMMAND) {
                EditBox cmd = new EditBox(font, rightX, fy + 44, colW, 16, Component.empty());
                cmd.setMaxLength(256);
                cmd.setValue(r.command);
                cmd.setResponder(s -> r.command = s);
                cmd.setHint(Component.literal("/give {player} ..."));
                addRenderableWidget(cmd);
            }
        }
    }

    // ------------------------------------------------------------------
    // Tab: Probability (visual bars)
    // ------------------------------------------------------------------

    private void initProbability() {
        // rendering happens in render(); nothing interactive here beyond info.
        addLabel("\u00A7dRanking de probabilidades (por peso):", bodyX(), bodyY() - 12, null);
    }

    // ------------------------------------------------------------------
    // Tab: Animation
    // ------------------------------------------------------------------

    private void initAnimation() {
        int x = bodyX();
        int y = bodyY();
        int colW = bodyW();

        ScrollSelector<CrateAnimation> list = new ScrollSelector<>(x, y, colW, bodyH() - 24, 14,
                a -> (a.id().equals(config.animationId) ? "\u00A7a\u2714 " : "\u00A7f") + a.displayName()
                        + " \u00A78[" + a.theme() + "]",
                a -> a.displayName() + " " + a.id() + " " + a.theme(),
                null);
        list.setItems(AnimationRegistry.all());
        list.onSelect(a -> { config.animationId = a.id(); rebuildWidgets(); });
        addRenderableWidget(list);

        CrateAnimation sel = AnimationRegistry.get(config.animationId);
        addLabel("\u00A78Seleccionada: \u00A7f" + sel.displayName() + " \u00A77(" + sel.durationTicks() / 20.0 + "s)",
                x, y + bodyH() - 18, null);
    }

    // ------------------------------------------------------------------
    // Tab: Appearance
    // ------------------------------------------------------------------

    private void initAppearance() {
        int x = bodyX();
        int y = bodyY();
        addToggle(x, y, 220, config.glow ? "Brillo (glint): Activado" : "Brillo (glint): Desactivado",
                config.glow, () -> { config.glow = !config.glow; rebuildWidgets(); },
                desc("Hace que el item de crate brille como encantado."));
        addToggle(x, y + 22, 220, config.particles ? "Part\u00edculas: Activado" : "Part\u00edculas: Desactivado",
                config.particles, () -> { config.particles = !config.particles; rebuildWidgets(); },
                desc("Part\u00edculas decorativas de identidad de la crate."));
        addToggle(x, y + 44, 220, config.floatingName ? "Nombre flotante: S\u00ed" : "Nombre flotante: No",
                config.floatingName, () -> { config.floatingName = !config.floatingName; rebuildWidgets(); },
                desc("Muestra el nombre flotante de la crate al colocarla (visual)."));

        EditBox hex = new EditBox(font, x + 200, y + 70, 120, 16, Component.empty());
        hex.setMaxLength(7);
        hex.setValue(config.nameColorHexOverride);
        hex.setHint(Component.literal("#RRGGBB"));
        hex.setResponder(s -> config.nameColorHexOverride = s.trim());
        addRenderableWidget(hex);
        addLabel("Color personalizado (opcional):", x, y + 74,
                desc("Color del nombre/haz en formato #RRGGBB.",
                        "Vac\u00edo = usa el color de la rareza."));
    }

    // ------------------------------------------------------------------
    // Tab: Key
    // ------------------------------------------------------------------

    private void initKey() {
        int x = bodyX();
        int y = bodyY();

        EditBox keyName = new EditBox(font, x + 160, y, 200, 16, Component.empty());
        keyName.setMaxLength(128);
        keyName.setValue(config.keyName);
        keyName.setResponder(s -> config.keyName = s);
        addRenderableWidget(keyName);
        addLabel("Nombre de la llave:", x, y + 4, null);

        EditBox keyLore = new EditBox(font, x + 160, y + 24, 200, 16, Component.empty());
        keyLore.setMaxLength(200);
        keyLore.setValue(config.keyLore);
        keyLore.setResponder(s -> config.keyLore = s);
        addRenderableWidget(keyLore);
        addLabel("Descripci\u00f3n (lore):", x, y + 28, null);

        addToggle(x, y + 50, 220, config.keyGlint ? "Glint obligatorio: S\u00ed" : "Glint obligatorio: No",
                config.keyGlint, () -> { config.keyGlint = !config.keyGlint; rebuildWidgets(); },
                desc("La llave brilla como encantada."));
        addToggle(x, y + 72, 220, config.consumeKey ? "Consumir al abrir: S\u00ed" : "Consumir al abrir: No",
                config.consumeKey, () -> { config.consumeKey = !config.consumeKey; rebuildWidgets(); },
                desc("Si est\u00e1 activo, la llave se gasta al abrir la crate."));

        addLabel("\u00A78La llave solo abre la crate con su mismo ID.", x, y + 96, null);
    }

    // ------------------------------------------------------------------
    // Tab: Settings
    // ------------------------------------------------------------------

    private void initSettings() {
        int x = bodyX();
        int y = bodyY();

        addIntField(x + 220, y, 60, config.cooldownSeconds, v -> config.cooldownSeconds = Math.max(0, v),
                "Cooldown por jugador (seg)", x, y + 4,
                desc("Tiempo que un jugador debe esperar para volver a abrir",
                        "ESTA crate. Es individual: no afecta a otros jugadores.",
                        "0 = sin cooldown."));
        addSecondsField(x + 220, y + 22, 60, config.openDelayTicks, v -> config.openDelayTicks = Math.max(0, v),
                "Retraso de apertura (seg)", x, y + 22 + 4,
                desc("Peque\u00f1a espera antifraude antes de abrir.",
                        "0 = inmediato."));

        addToggle(x, y + 48, 260, config.broadcast ? "Anuncio global: Activado" : "Anuncio global: Desactivado",
                config.broadcast, () -> { config.broadcast = !config.broadcast; rebuildWidgets(); },
                desc("Anuncia a todo el servidor cuando alguien gana."));
        addToggle(x, y + 70, 260, config.allowSkip ? "Saltar con SHIFT: Permitido" : "Saltar con SHIFT: Bloqueado",
                config.allowSkip, () -> { config.allowSkip = !config.allowSkip; rebuildWidgets(); },
                desc("Permite al jugador saltar la animaci\u00f3n con SHIFT."));

        EditBox perm = new EditBox(font, x + 220, y + 96, 200, 16, Component.empty());
        perm.setMaxLength(64);
        perm.setValue(config.requiredPermission);
        perm.setHint(Component.literal("(opcional)"));
        perm.setResponder(s -> config.requiredPermission = s.trim());
        addRenderableWidget(perm);
        addLabel("Permiso requerido (opcional):", x, y + 100,
                desc("Nodo de permiso extra. D\u00e9jalo vac\u00edo para no requerir nada."));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static List<Component> desc(String... lines) {
        List<Component> out = new ArrayList<>();
        for (String s : lines) {
            out.add(Component.literal(s));
        }
        return out;
    }

    private void addLabel(String text, int x, int y, List<Component> tooltip) {
        labels.add(new Label(text, x, y, 0xE0E0E0));
        if (tooltip != null) {
            tooltipZones.add(new TooltipZone(x, y - 2, 200, 14, tooltip));
        }
    }

    private void addIntField(int x, int y, int w, int value, java.util.function.IntConsumer setter,
                             String label, int labelX, int labelY, List<Component> tooltip) {
        EditBox box = new EditBox(font, x, y, w, 16, Component.empty());
        box.setMaxLength(10);
        box.setValue(Integer.toString(value));
        box.setResponder(s -> {
            try {
                setter.accept(Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        });
        addRenderableWidget(box);
        labels.add(new Label(label, labelX, labelY, 0xE0E0E0));
        if (tooltip != null) {
            tooltipZones.add(new TooltipZone(labelX, labelY - 2, (x + w) - labelX, 14, tooltip));
        }
    }

    private void addSecondsField(int x, int y, int w, int ticks, java.util.function.IntConsumer setterTicks,
                                 String label, int labelX, int labelY, List<Component> tooltip) {
        EditBox box = new EditBox(font, x, y, w, 16, Component.empty());
        box.setMaxLength(8);
        box.setValue(Long.toString(Math.round(ticks / 20.0)));
        box.setResponder(s -> {
            String t = s.trim();
            if (t.isEmpty()) {
                return;
            }
            try {
                double seconds = Double.parseDouble(t);
                setterTicks.accept((int) Math.round(Math.max(0, seconds) * 20.0));
            } catch (NumberFormatException ignored) {
            }
        });
        addRenderableWidget(box);
        labels.add(new Label(label, labelX, labelY, 0xE0E0E0));
        if (tooltip != null) {
            tooltipZones.add(new TooltipZone(labelX, labelY - 2, (x + w) - labelX, 14, tooltip));
        }
    }

    private void addToggle(int x, int y, int w, String text, boolean state, Runnable onToggle, List<Component> tooltip) {
        String prefix = state ? "\u00A7a" : "\u00A77";
        addRenderableWidget(Button.builder(Component.literal(prefix + text), b -> onToggle.run())
                .bounds(x, y, w, 16).build());
        if (tooltip != null) {
            tooltipZones.add(new TooltipZone(x, y, w, 16, tooltip));
        }
    }

    // ------------------------------------------------------------------
    // Render
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + panelHeight, 0xE0181822);
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + 20, 0xFF24243A);
        g.fill(leftPos, topPos + panelHeight - 1, leftPos + panelWidth, topPos + panelHeight, 0xFF3A3A4A);
        g.fill(leftPos + 6, topPos + 45, leftPos + panelWidth - 6, topPos + 46, 0xFF3A3A4A);
        g.drawString(font, "\u00A7d\u2726 \u00A7fFantastic Crates \u00A7d\u2726 \u00A77- "
                + config.rarity.color() + config.rarity.displayName(), leftPos + 8, topPos + 6, 0xFFFFFF, false);

        if (activeTab == Tab.PROBABILITY) {
            renderProbabilityBars(g);
        }

        super.render(g, mouseX, mouseY, partialTick);

        for (Label l : labels) {
            g.drawString(font, l.text(), l.x(), l.y(), l.color(), false);
        }

        for (TooltipZone z : tooltipZones) {
            if (mouseX >= z.x() && mouseX < z.x() + z.w() && mouseY >= z.y() && mouseY < z.y() + z.h()) {
                g.renderComponentTooltip(font, z.lines(), mouseX, mouseY);
                break;
            }
        }
    }

    private void renderProbabilityBars(GuiGraphics g) {
        int x = bodyX();
        int y = bodyY() + 4;
        int total = config.totalWeight();
        int maxBar = bodyW() - 150;
        int row = 0;
        for (RewardEntry r : config.rewards) {
            if (row >= 9) {
                g.drawString(font, "\u00A77... y " + (config.rewards.size() - row) + " m\u00e1s", x, y + row * 20, 0x909090, false);
                break;
            }
            int ry = y + row * 20;
            double pct = r.guaranteed ? 100.0 : (total > 0 ? (r.weight * 100.0 / total) : 0);
            int barLen = r.guaranteed ? maxBar : (int) (maxBar * (total > 0 ? (double) r.weight / total : 0));
            int color = r.guaranteed ? 0xFF55FF55 : 0xFF2D6CDF;
            g.fill(x + 140, ry, x + 140 + Math.max(2, barLen), ry + 12, color);
            String name = font.plainSubstrByWidth(r.describe(), 132);
            g.drawString(font, name, x, ry + 2, 0xE0E0E0, false);
            String pctStr = r.guaranteed ? "\u00A7a100% (fija)" : String.format("%.1f%%", pct);
            g.drawString(font, pctStr, x + 140 + Math.max(2, barLen) + 4, ry + 2, 0xFFFFFF, false);
            row++;
        }
        if (config.rewards.isEmpty()) {
            g.drawString(font, "\u00A77No hay recompensas. A\u00f1\u00e1delas en la pesta\u00f1a Recompensas.", x, y, 0x909090, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
