package com.fscrates.client.screen;

import com.fscrates.animation.AnimationRegistry;
import com.fscrates.animation.CrateAnimation;
import com.fscrates.client.RegistryLists;
import com.fscrates.client.widget.ScrollSelector;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.ParticleLayer;
import com.fscrates.config.ParticleNames;
import com.fscrates.config.Rarity;
import com.fscrates.config.RewardEntry;
import com.fscrates.network.FSNetwork;
import com.fscrates.network.SaveCratePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The in-game crate editor. Tabs: Info, Premios, Prob., Anim., Aspecto, Part.,
 * Llave, Ajustes. Every list scrolls (mouse wheel) so it never overflows. The
 * particle tab is a full editor with unlimited layers; item rewards open a
 * structured NBT editor. All controls have descriptions (hover + help line).
 */
public class CrateEditorScreen extends Screen {

    private enum Tab {
        INFO("Info"), REWARDS("Premios"), PROBABILITY("Prob."),
        ANIMATION("Anim."), APPEARANCE("Aspecto"), PARTICLES("Part."),
        KEY("Llave"), SETTINGS("Ajustes");

        final String label;
        Tab(String label) { this.label = label; }
    }

    private record Label(String text, int x, int y, int color) {}
    private record TooltipZone(int x, int y, int w, int h, List<Component> lines) {}

    private final CrateConfig config;
    private Tab activeTab = Tab.INFO;
    private final List<Label> labels = new ArrayList<>();
    private final List<TooltipZone> tooltipZones = new ArrayList<>();
    private String helpLine = "";

    private int leftPos, topPos, panelWidth, panelHeight;
    private RewardEntry selectedReward;
    private ParticleLayer selectedLayer;
    private int probScroll = 0;

    /** Palette cycled by the per-line floating-text colour buttons. */
    private static final String COLOR_CHARS = "f7e6cab9d5234180";

    public CrateEditorScreen(CrateConfig config) {
        super(Component.literal("Editor de Crate"));
        this.config = config == null ? new CrateConfig() : config;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(this.width - 16, 540);
        panelHeight = Math.min(this.height - 16, 320);
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
            case PARTICLES -> initParticles();
            case KEY -> initKey();
            case SETTINGS -> initSettings();
        }
    }

    private int bodyX() { return leftPos + 8; }
    private int bodyY() { return topPos + 62; }
    private int bodyW() { return panelWidth - 16; }
    private int bodyH() { return panelHeight - 62 - 28; }

    private void initHeader() {
        Tab[] tabs = Tab.values();
        int gap = 2;
        int tabW = (panelWidth - 16 - gap * (tabs.length - 1)) / tabs.length;
        int x = leftPos + 8;
        int y = topPos + 24;
        for (Tab tab : tabs) {
            boolean active = tab == activeTab;
            String text = (active ? "\u00A7f\u00A7l" : "\u00A77") + tab.label;
            addRenderableWidget(Button.builder(Component.literal(text), b -> {
                this.activeTab = tab;
                this.rebuildWidgets();
            }).bounds(x, y, tabW, 18).build());
            x += tabW + gap;
        }
    }

    private void initFooter() {
        int w = 150;
        addRenderableWidget(Button.builder(Component.literal("\u00A7aGuardar y Obtener"), b -> {
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
        helpLine = "Datos basicos: ID, nombre, tier y tiradas por apertura.";
        int x = bodyX();
        int y = bodyY();

        EditBox id = new EditBox(font, x + 170, y, 200, 16, Component.empty());
        id.setMaxLength(48);
        id.setValue(config.id);
        id.setResponder(s -> config.id = s.trim().toLowerCase().replace(' ', '_'));
        addRenderableWidget(id);
        addLabel("ID de la crate:", x, y + 4, desc(
                "Identificador unico (sin espacios).",
                "Se usa en /fscrate give, edit, delete.",
                "Ej: cofre_legendario"));

        EditBox name = new EditBox(font, x + 170, y + 24, 200, 16, Component.empty());
        name.setMaxLength(128);
        name.setValue(config.displayName);
        name.setResponder(s -> config.displayName = s);
        addRenderableWidget(name);
        addLabel("Nombre visible:", x, y + 28, desc(
                "Nombre del item y del holograma. Acepta codigos & o \u00A7."));

        addRenderableWidget(Button.builder(Component.literal("Tier: " + config.rarity.color() + config.rarity.displayName()),
                b -> { config.rarity = config.rarity.next(); rebuildWidgets(); })
                .bounds(x + 170, y + 48, 200, 16).build());
        addLabel("Tier (rareza):", x, y + 52, desc(
                "Define color, sonidos por rareza y QUE LLAVE lo abre.",
                "Una crate de tier X se abre con la llave de tier X."));

        addIntField(x + 170, y + 72, 60, config.rolls, v -> config.rolls = Math.max(1, v),
                "Tiradas por apertura:", x, y + 76,
                desc("Cuantas recompensas (por probabilidad) se entregan.",
                        "Las garantizadas se suman aparte."));

        addLabel("\u00A78Animacion: \u00A7f" + AnimationRegistry.get(config.animationId).displayName(), x, y + 100, null);
        addLabel("\u00A78Recompensas: \u00A7f" + config.rewards.size()
                + "  \u00A78Capas de particulas: \u00A7f" + config.particleLayers.size(), x, y + 112, null);
    }

    // ------------------------------------------------------------------
    // Tab: Rewards
    // ------------------------------------------------------------------

    private void initRewards() {
        helpLine = "Izquierda: busca y clic en un item. Derecha: lista (scroll) y editor de la seleccionada.";
        int x = bodyX();
        int y = bodyY();
        int colW = (bodyW() - 8) / 2;
        int rightX = x + colW + 8;

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
            r.chance = 10.0;
            config.rewards.add(r);
            selectedReward = r;
            rebuildWidgets();
        });
        search.setResponder(items::setQuery);
        addRenderableWidget(items);

        ScrollSelector<RewardEntry> current = new ScrollSelector<>(rightX, y, colW, bodyH() - 98, 16,
                r -> (r == selectedReward ? "\u00A7e\u25B6 " : "\u00A7f") + r.describe()
                        + " \u00A77(" + fmt(config.normalizedPercent(r)) + "%)",
                RewardEntry::describe,
                r -> r.type == RewardEntry.Type.ITEM ? r.item : ItemStack.EMPTY);
        current.setItems(new ArrayList<>(config.rewards));
        current.onSelect(r -> { selectedReward = r; rebuildWidgets(); });
        addRenderableWidget(current);

        int addY = y + bodyH() - 94;
        addRenderableWidget(Button.builder(Component.literal("+ Comando"), b -> {
            config.rewards.add(new RewardEntry(RewardEntry.Type.COMMAND)); rebuildWidgets();
        }).bounds(rightX, addY, colW / 4 - 2, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+ XP"), b -> {
            RewardEntry r = new RewardEntry(RewardEntry.Type.XP); r.xp = 100; config.rewards.add(r); rebuildWidgets();
        }).bounds(rightX + colW / 4, addY, colW / 4 - 2, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+ Efecto"), b -> {
            config.rewards.add(new RewardEntry(RewardEntry.Type.EFFECT)); rebuildWidgets();
        }).bounds(rightX + 2 * colW / 4, addY, colW / 4 - 2, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+ Llave"), b -> {
            RewardEntry r = new RewardEntry(RewardEntry.Type.KEY); r.keyRarity = config.rarity.name();
            config.rewards.add(r); rebuildWidgets();
        }).bounds(rightX + 3 * colW / 4, addY, colW / 4 - 2, 16).build());

        if (selectedReward != null && config.rewards.contains(selectedReward)) {
            RewardEntry r = selectedReward;
            int fy = y + bodyH() - 72;
            addDoubleField(rightX + 70, fy, 50, r.chance, v -> r.chance = Math.max(0, v),
                    "Prob. (%)", rightX, fy + 4,
                    desc("Probabilidad en %. Se normaliza con las demas para sumar 100%."));
            addIntField(rightX + 150, fy, 36, r.minAmount, v -> r.minAmount = Math.max(1, v), "Min", rightX + 122, fy + 4,
                    desc("Cantidad minima entregada."));
            addIntField(rightX + 235, fy, 36, r.maxAmount, v -> r.maxAmount = Math.max(1, v), "Max", rightX + 200, fy + 4,
                    desc("Cantidad maxima entregada."));

            addToggle(rightX, fy + 22, colW - 70, r.guaranteed ? "Garantizada: Si" : "Garantizada: No",
                    r.guaranteed, () -> { r.guaranteed = !r.guaranteed; rebuildWidgets(); },
                    desc("Si esta activo, SIEMPRE se entrega (100%)."));
            addRenderableWidget(Button.builder(Component.literal("\u00A7cQuitar"), b -> {
                config.rewards.remove(r); selectedReward = null; rebuildWidgets();
            }).bounds(rightX + colW - 64, fy + 22, 64, 16).build());

            if (r.type == RewardEntry.Type.COMMAND) {
                EditBox cmd = new EditBox(font, rightX, fy + 44, colW, 16, Component.empty());
                cmd.setMaxLength(256);
                cmd.setValue(r.command);
                cmd.setResponder(s -> r.command = s);
                cmd.setHint(Component.literal("/give {player} ..."));
                addRenderableWidget(cmd);
            } else if (r.type == RewardEntry.Type.XP) {
                addIntField(rightX + 40, fy + 44, 80, r.xp, v -> r.xp = Math.max(0, v), "XP", rightX, fy + 48,
                        desc("Puntos de experiencia entregados."));
            } else if (r.type == RewardEntry.Type.KEY) {
                addRenderableWidget(Button.builder(Component.literal("Tier llave: "
                                + Rarity.byName(r.keyRarity).color() + Rarity.byName(r.keyRarity).displayName()),
                        b -> { r.keyRarity = Rarity.byName(r.keyRarity).next().name(); rebuildWidgets(); })
                        .bounds(rightX, fy + 44, colW, 16).build());
            } else if (r.type == RewardEntry.Type.ITEM) {
                addRenderableWidget(Button.builder(Component.literal("\u00A7b\u270e Editar NBT del item"), b -> {
                    if (r.item != null && !r.item.isEmpty()) {
                        this.minecraft.setScreen(new NbtEditorScreen(this, r.item));
                    }
                }).bounds(rightX, fy + 44, colW, 16).build());
                tooltipZones.add(new TooltipZone(rightX, fy + 44, colW, 16, desc(
                        "Abre el editor de NBT: nombre, lore con color,",
                        "encantamientos, atributos, irrompible, CustomModelData...",
                        "Todo manual, sin pegar comandos.")));
            }
        }
    }

    // ------------------------------------------------------------------
    // Tab: Probability
    // ------------------------------------------------------------------

    private void initProbability() {
        helpLine = "Escribe el % de cada recompensa (se normaliza a 100%). Rueda del raton para desplazarte.";
        int x = bodyX();
        int y = bodyY();
        int rowsVisible = Math.max(1, bodyH() / 22);
        int total = config.rewards.size();
        probScroll = Math.max(0, Math.min(probScroll, Math.max(0, total - rowsVisible)));
        for (int i = 0; i < rowsVisible; i++) {
            int index = probScroll + i;
            if (index >= total) {
                break;
            }
            RewardEntry r = config.rewards.get(index);
            int ry = y + i * 22;
            if (!r.guaranteed) {
                addDoubleField(x + 150, ry, 50, r.chance, v -> r.chance = Math.max(0, v), null, 0, 0,
                        desc("Probabilidad relativa en %. Se normaliza con el resto."));
            }
        }
        // Centred in the footer so it never overlaps "Cerrar" or "Guardar".
        addRenderableWidget(Button.builder(Component.literal("Igualar todas"), b -> {
            int n = 0;
            for (RewardEntry r : config.rewards) if (!r.guaranteed) n++;
            if (n > 0) {
                double each = 100.0 / n;
                for (RewardEntry r : config.rewards) if (!r.guaranteed) r.chance = each;
            }
            rebuildWidgets();
        }).bounds(leftPos + panelWidth / 2 - 60, topPos + panelHeight - 24, 120, 18).build());
    }

    // ------------------------------------------------------------------
    // Tab: Animation
    // ------------------------------------------------------------------

    private void initAnimation() {
        helpLine = "Elige la animacion del cofre. Ocurre EN el cofre, en el mundo, con tension antes del premio.";
        int x = bodyX();
        int y = bodyY();
        int colW = bodyW();

        ScrollSelector<CrateAnimation> list = new ScrollSelector<>(x, y, colW, bodyH() - 28, 14,
                a -> (a.id().equals(config.animationId) ? "\u00A7a\u2714 " : "\u00A7f") + a.displayName()
                        + " \u00A78(" + a.durationTicks() / 20.0 + "s)",
                a -> a.displayName() + " " + a.id(),
                null);
        list.setItems(AnimationRegistry.all());
        list.onSelect(a -> { config.animationId = a.id(); rebuildWidgets(); });
        addRenderableWidget(list);

        CrateAnimation sel = AnimationRegistry.get(config.animationId);
        addLabel("\u00A7e" + sel.displayName() + ": \u00A77" + sel.description(), x, y + bodyH() - 22, null);
    }

    // ------------------------------------------------------------------
    // Tab: Appearance (+ per-line coloured floating text)
    // ------------------------------------------------------------------

    private void initAppearance() {
        helpLine = "Brillo, particulas on/off, nombre flotante, color del nombre y texto flotante (color por linea).";
        int x = bodyX();
        int y = bodyY();
        int colW = (bodyW() - 10) / 2;

        addToggle(x, y, colW, config.glow ? "Brillo del item: Activado" : "Brillo del item: Desactivado",
                config.glow, () -> { config.glow = !config.glow; rebuildWidgets(); },
                desc("El item de crate brilla como encantado."));
        addToggle(x, y + 22, colW, config.particles ? "Particulas: Activado" : "Particulas: Desactivado",
                config.particles, () -> { config.particles = !config.particles; rebuildWidgets(); },
                desc("Particulas de reposo alrededor de la crate."));
        addToggle(x, y + 44, colW, config.floatingName ? "Nombre flotante: Si" : "Nombre flotante: No",
                config.floatingName, () -> { config.floatingName = !config.floatingName; rebuildWidgets(); },
                desc("Muestra el nombre flotando sobre la crate."));

        EditBox hex = new EditBox(font, x + 70, y + 70, 110, 16, Component.empty());
        hex.setMaxLength(7);
        hex.setValue(config.nameColorHexOverride);
        hex.setHint(Component.literal("#RRGGBB"));
        hex.setResponder(s -> config.nameColorHexOverride = s.trim());
        addRenderableWidget(hex);
        addLabel("Color:", x, y + 74, desc("Color del nombre (#RRGGBB). Vacio = color del tier."));

        addToggle(x, y + 92, colW, config.showOdds ? "Mostrar % encima: Si" : "Mostrar % encima: No",
                config.showOdds, () -> { config.showOdds = !config.showOdds; rebuildWidgets(); },
                desc("Muestra la probabilidad de cada recompensa flotando sobre el cofre.",
                        "Util para que los jugadores vean las posibilidades."));

        // Floating-text editor (right column): up to 6 lines, each with its own colour
        int tx = x + colW + 10;
        addLabel("\u00A7eTexto flotante (color por linea):", tx, y - 2, desc(
                "El boton \u25A0 cambia el color de ESA linea.",
                "Tambien aceptas codigos & dentro del texto."));
        final int maxLines = 6;
        final char[] lineColors = new char[maxLines];
        final String[] lineTexts = new String[maxLines];
        for (int i = 0; i < maxLines; i++) {
            String raw = i < config.floatingText.size() ? config.floatingText.get(i) : "";
            char col = 'f';
            String txt = raw;
            if (raw.length() >= 2 && (raw.charAt(0) == '&' || raw.charAt(0) == '\u00A7')
                    && COLOR_CHARS.indexOf(raw.charAt(1)) >= 0) {
                col = raw.charAt(1);
                txt = raw.substring(2);
            }
            lineColors[i] = col;
            lineTexts[i] = txt;
        }
        Runnable sync = () -> {
            List<String> out = new ArrayList<>();
            for (int i = 0; i < maxLines; i++) {
                out.add(lineTexts[i].isEmpty() ? "" : "&" + lineColors[i] + lineTexts[i]);
            }
            config.setFloatingText(String.join("\n", out));
        };
        for (int i = 0; i < maxLines; i++) {
            final int idx = i;
            int ry = y + 12 + i * 21;
            addRenderableWidget(Button.builder(Component.literal("\u00A7" + lineColors[i] + "\u25A0"), b -> {
                int pos = COLOR_CHARS.indexOf(lineColors[idx]);
                lineColors[idx] = COLOR_CHARS.charAt((pos + 1) % COLOR_CHARS.length());
                sync.run();
                rebuildWidgets();
            }).bounds(tx, ry, 18, 16).build());
            EditBox line = new EditBox(font, tx + 22, ry, colW - 22, 16, Component.empty());
            line.setMaxLength(96);
            line.setValue(lineTexts[i]);
            line.setHint(Component.literal("Linea " + (i + 1)));
            line.setResponder(s -> { lineTexts[idx] = s; sync.run(); });
            addRenderableWidget(line);
        }
    }

    // ------------------------------------------------------------------
    // Tab: Particles (full editor — unlimited layers, all scrollable)
    // ------------------------------------------------------------------

    private void initParticles() {
        helpLine = "Capas sin limite. Izq: tus capas (scroll). Centro: tipo (scroll, busca). Der: ajustes de la capa.";
        int x = bodyX();
        int y = bodyY();
        int listW = 118;
        int midW = 126;
        int midX = x + listW + 6;
        int rx = midX + midW + 8;
        int fw = leftPos + panelWidth - 8 - rx;

        if (selectedLayer != null && !config.particleLayers.contains(selectedLayer)) {
            selectedLayer = null;
        }

        // --- left: layer list + add ---
        ScrollSelector<ParticleLayer> layers = new ScrollSelector<>(x, y, listW, bodyH() - 20, 22,
                l -> (l == selectedLayer ? "\u00A7e\u25B6 " : "") + l.shortLabel(),
                ParticleLayer::shortLabel, l -> ItemStack.EMPTY);
        layers.setItems(new ArrayList<>(config.particleLayers));
        layers.onSelect(l -> { selectedLayer = l; rebuildWidgets(); });
        addRenderableWidget(layers);
        addRenderableWidget(Button.builder(Component.literal("\u00A7a+ Capa"), b -> {
            ParticleLayer l = new ParticleLayer();
            config.particleLayers.add(l);
            selectedLayer = l;
            rebuildWidgets();
        }).bounds(x, y + bodyH() - 18, listW, 16).build());

        // --- center: particle type picker (Spanish names) ---
        EditBox search = new EditBox(font, midX, y, midW, 16, Component.empty());
        search.setHint(Component.literal("Buscar particula..."));
        addRenderableWidget(search);
        ScrollSelector<ResourceLocation> types = new ScrollSelector<>(midX, y + 20, midW, bodyH() - 22, 13,
                rl -> (selectedLayer != null && rl.toString().equals(selectedLayer.particleId) ? "\u00A7a\u2714 " : "\u00A7f")
                        + ParticleNames.spanish(rl.getPath()),
                rl -> ParticleNames.spanish(rl.getPath()) + " " + rl,
                rl -> ItemStack.EMPTY);
        types.setItems(RegistryLists.particles());
        types.onSelect(rl -> {
            if (selectedLayer != null) {
                selectedLayer.particleId = rl.toString();
                rebuildWidgets();
            }
        });
        search.setResponder(types::setQuery);
        addRenderableWidget(types);

        // --- right: selected-layer fields (compact, 2-column, no overlap) ---
        if (selectedLayer == null) {
            addLabel("\u00A77Selecciona o", rx, y + 4, null);
            addLabel("\u00A77crea una capa \u2190", rx, y + 16, null);
            return;
        }
        ParticleLayer l = selectedLayer;
        int half = fw / 2;
        int fieldW = 42;

        addLabel("\u00A7e" + ParticleNames.spanish(
                l.particleId.contains(":") ? l.particleId.substring(l.particleId.indexOf(':') + 1) : l.particleId),
                rx, y, null);

        addRenderableWidget(Button.builder(Component.literal("Fase: \u00A7e" + l.phase.label), b -> {
            l.phase = l.phase.next(); rebuildWidgets();
        }).bounds(rx, y + 12, fw, 16).build());
        tooltipZones.add(new TooltipZone(rx, y + 12, fw, 16, desc(
                "Cuando emite:", "Reposo, Tension, Apertura, Revelacion, Final.")));

        addRenderableWidget(Button.builder(Component.literal("Forma: \u00A7b" + l.shape.label), b -> {
            l.shape = l.shape.next();
            l.applyShapeDefaults();
            rebuildWidgets();
        }).bounds(rx, y + 32, fw, 16).build());
        tooltipZones.add(new TooltipZone(rx, y + 32, fw, 16, desc(
                "Forma/movimiento. Al cambiarla se reajustan radio/altura",
                "para que quede bien (ej. el anillo rodea el cofre por fuera).")));

        // 3 rows x 2 columns of numeric fields
        int r1 = y + 54, r2 = y + 74, r3 = y + 94;
        addIntField(rx + 60, r1, fieldW, l.count, v -> l.count = Math.max(1, v), "Cant.", rx, r1 + 4,
                desc("Particulas por emision."));
        addDoubleField(rx + half + 56, r1, fieldW, l.speed, v -> l.speed = Math.max(0, v), "Vel.", rx + half, r1 + 4,
                desc("Empuje de las particulas."));
        addDoubleField(rx + 60, r2, fieldW, l.spread, v -> l.spread = Math.max(0, v), "Disp.", rx, r2 + 4,
                desc("Apertura aleatoria."));
        addDoubleField(rx + half + 56, r2, fieldW, l.radius, v -> l.radius = Math.max(0, v), "Radio", rx + half, r2 + 4,
                desc("Radio del anillo/halo/orbita. ~0.95 rodea el cofre por fuera."));
        addDoubleField(rx + 60, r3, fieldW, l.yOffset, v -> l.yOffset = v, "Alt.", rx, r3 + 4,
                desc("Altura sobre el bloque. ~0.45 para anillo al ras del suelo."));
        addIntField(rx + half + 56, r3, fieldW, l.interval, v -> l.interval = Math.max(1, v), "Int.", rx + half, r3 + 4,
                desc("Solo en Reposo: emite cada N ticks (20 = 1s)."));

        int cy = y + 116;
        addToggle(rx, cy, fw, l.useRarityColor ? "Color: tier" : "Color: hex",
                l.useRarityColor, () -> { l.useRarityColor = !l.useRarityColor; rebuildWidgets(); },
                desc("Solo afecta a 'Polvo de color'. Tier = color de la rareza."));
        cy += 20;
        if (!l.useRarityColor) {
            EditBox hex = new EditBox(font, rx + 36, cy, fw - 36, 16, Component.empty());
            hex.setMaxLength(7);
            hex.setValue(l.colorHex);
            hex.setHint(Component.literal("#RRGGBB"));
            hex.setResponder(s -> l.colorHex = s.trim());
            addRenderableWidget(hex);
            addLabel("Hex:", rx, cy + 4, null);
            cy += 20;
        }
        addRenderableWidget(Button.builder(Component.literal("\u00A7cQuitar capa"), b -> {
            config.particleLayers.remove(l); selectedLayer = null; rebuildWidgets();
        }).bounds(rx, cy, fw, 16).build());
    }

    // ------------------------------------------------------------------
    // Tab: Key
    // ------------------------------------------------------------------

    private void initKey() {
        helpLine = "Las llaves son por TIER (5: Comun, Rara, Epica, Legendaria, Mitica). No se ligan a una crate.";
        int x = bodyX();
        int y = bodyY();

        addLabel("\u00A7fEsta crate se abre con: " + config.rarity.color() + "Llave " + config.rarity.displayName(),
                x, y, desc("Cualquier llave de este tier abre esta crate.",
                        "Entrega: /fscrate key give <jugador> " + config.rarity.id()));

        addLabel("\u00A77Las 5 llaves de tier:", x, y + 22, null);
        int ly = y + 36;
        for (Rarity r : Rarity.values()) {
            addLabel("  " + r.color() + "\u2726 Llave " + r.displayName()
                    + " \u00A78(/fscrate key give <jugador> " + r.id() + ")", x, ly, null);
            ly += 12;
        }

        addToggle(x, ly + 6, 260, config.consumeKey ? "Consumir llave al abrir: Si" : "Consumir llave al abrir: No",
                config.consumeKey, () -> { config.consumeKey = !config.consumeKey; rebuildWidgets(); },
                desc("Si esta activo, la llave se gasta al abrir."));
    }

    // ------------------------------------------------------------------
    // Tab: Settings
    // ------------------------------------------------------------------

    private void initSettings() {
        helpLine = "Cooldown por jugador, anuncio global, saltar animacion y permisos.";
        int x = bodyX();
        int y = bodyY();

        addIntField(x + 240, y, 60, config.cooldownSeconds, v -> config.cooldownSeconds = Math.max(0, v),
                "Cooldown por jugador (seg):", x, y + 4,
                desc("Espera individual para reabrir ESTA crate. 0 = sin cooldown."));
        addSecondsField(x + 240, y + 22, 60, config.openDelayTicks, v -> config.openDelayTicks = Math.max(0, v),
                "Retraso de apertura (seg):", x, y + 26,
                desc("Espera antifraude. 0 = inmediato."));

        addToggle(x, y + 48, 280, config.broadcast ? "Anuncio global: Activado" : "Anuncio global: Desactivado",
                config.broadcast, () -> { config.broadcast = !config.broadcast; rebuildWidgets(); },
                desc("Anuncia a todo el servidor cuando alguien gana."));
        addToggle(x, y + 70, 280, config.allowSkip ? "Saltar con SHIFT: Permitido" : "Saltar con SHIFT: Bloqueado",
                config.allowSkip, () -> { config.allowSkip = !config.allowSkip; rebuildWidgets(); },
                desc("Permite saltar la animacion abriendo con SHIFT."));

        EditBox perm = new EditBox(font, x + 240, y + 96, 200, 16, Component.empty());
        perm.setMaxLength(64);
        perm.setValue(config.requiredPermission);
        perm.setHint(Component.literal("(opcional)"));
        perm.setResponder(s -> config.requiredPermission = s.trim());
        addRenderableWidget(perm);
        addLabel("Permiso requerido (opcional):", x, y + 100,
                desc("Nodo de permiso extra. Vacio = nada adicional."));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    private static List<Component> desc(String... lines) {
        List<Component> out = new ArrayList<>();
        for (String s : lines) out.add(Component.literal(s));
        return out;
    }

    private void addLabel(String text, int x, int y, List<Component> tooltip) {
        labels.add(new Label(text, x, y, 0xE0E0E0));
        if (tooltip != null) {
            tooltipZones.add(new TooltipZone(x, y - 2, Math.max(200, font.width(text) + 8), 14, tooltip));
        }
    }

    private void addIntField(int x, int y, int w, int value, java.util.function.IntConsumer setter,
                             String label, int labelX, int labelY, List<Component> tooltip) {
        EditBox box = new EditBox(font, x, y, w, 16, Component.empty());
        box.setMaxLength(10);
        box.setValue(Integer.toString(value));
        box.setResponder(s -> {
            try { setter.accept(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
        });
        addRenderableWidget(box);
        if (label != null) {
            labels.add(new Label(label, labelX, labelY, 0xE0E0E0));
            if (tooltip != null) {
                tooltipZones.add(new TooltipZone(labelX, labelY - 2, (x + w) - labelX, 14, tooltip));
            }
        }
    }

    private void addDoubleField(int x, int y, int w, double value, java.util.function.DoubleConsumer setter,
                                String label, int labelX, int labelY, List<Component> tooltip) {
        EditBox box = new EditBox(font, x, y, w, 16, Component.empty());
        box.setMaxLength(8);
        box.setValue(fmt(value));
        box.setResponder(s -> {
            try { setter.accept(Double.parseDouble(s.trim())); } catch (NumberFormatException ignored) {}
        });
        addRenderableWidget(box);
        if (label != null) {
            labels.add(new Label(label, labelX, labelY, 0xE0E0E0));
        }
        if (tooltip != null) {
            tooltipZones.add(new TooltipZone(x, y, w, 16, tooltip));
        }
    }

    private void addSecondsField(int x, int y, int w, int ticks, java.util.function.IntConsumer setterTicks,
                                 String label, int labelX, int labelY, List<Component> tooltip) {
        EditBox box = new EditBox(font, x, y, w, 16, Component.empty());
        box.setMaxLength(8);
        box.setValue(Long.toString(Math.round(ticks / 20.0)));
        box.setResponder(s -> {
            String t = s.trim();
            if (t.isEmpty()) return;
            try {
                double seconds = Double.parseDouble(t);
                setterTicks.accept((int) Math.round(Math.max(0, seconds) * 20.0));
            } catch (NumberFormatException ignored) {}
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
        g.fill(leftPos + 6, topPos + 46, leftPos + panelWidth - 6, topPos + 47, 0xFF3A3A4A);
        g.drawString(font, "\u00A7d\u2726 \u00A7fFantastic Crates \u00A7d\u2726 \u00A77- "
                + config.rarity.color() + config.rarity.displayName(), leftPos + 8, topPos + 6, 0xFFFFFF, false);

        if (helpLine != null && !helpLine.isEmpty()) {
            String trimmed = font.plainSubstrByWidth("\u00A77" + helpLine, panelWidth - 16);
            g.drawString(font, trimmed, leftPos + 8, topPos + 50, 0x9AA0B0, false);
        }

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
        int y = bodyY();
        int maxBar = bodyW() - 230;
        int rowsVisible = Math.max(1, bodyH() / 22);
        int total = config.rewards.size();
        for (int i = 0; i < rowsVisible; i++) {
            int index = probScroll + i;
            if (index >= total) {
                break;
            }
            RewardEntry r = config.rewards.get(index);
            int ry = y + i * 22;
            double pct = config.normalizedPercent(r);
            int barLen = (int) (maxBar * pct / 100.0);
            int color = r.guaranteed ? 0xFF55FF55 : 0xFF2D6CDF;
            String nameStr = font.plainSubstrByWidth(r.describe(), 140);
            g.drawString(font, nameStr, x, ry + 4, 0xE0E0E0, false);
            int barX = x + 210;
            g.fill(barX, ry + 2, barX + Math.max(2, barLen), ry + 14, color);
            String pctStr = r.guaranteed ? "\u00A7a100% fija" : fmt(pct) + "%";
            g.drawString(font, pctStr, barX + Math.max(2, barLen) + 4, ry + 4, 0xFFFFFF, false);
        }
        if (total > rowsVisible) {
            int from = probScroll + 1;
            int to = Math.min(total, probScroll + rowsVisible);
            g.drawString(font, "\u00A78\u25B2\u25BC " + from + "-" + to + " de " + total + " (rueda)",
                    x + maxBar + 60, y + rowsVisible * 22 - 10, 0x9AA0B0, false);
        }
        if (config.rewards.isEmpty()) {
            g.drawString(font, "\u00A77No hay recompensas. Anadelas en Premios.", x, y, 0x909090, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (activeTab == Tab.PROBABILITY && mouseX >= leftPos && mouseX <= leftPos + panelWidth
                && mouseY >= bodyY() - 4 && mouseY <= bodyY() + bodyH()) {
            int total = config.rewards.size();
            int rowsVisible = Math.max(1, bodyH() / 22);
            int max = Math.max(0, total - rowsVisible);
            int old = probScroll;
            probScroll = Math.max(0, Math.min(max, probScroll - (int) Math.signum(delta)));
            if (probScroll != old) {
                rebuildWidgets();
                return true;
            }
            return false;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
