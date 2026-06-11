package com.fspawner.client.screen;

import com.fspawner.client.widget.ScrollSelector;
import com.fspawner.config.*;
import com.fspawner.integration.InfernalModifiers;
import com.fspawner.network.FSNetwork;
import com.fspawner.network.SaveConfigPacket;
import com.fspawner.util.FSAttributes;
import com.fspawner.util.RegistryLists;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The modern, fully interactive Fantastic Spawner editor. Tabbed layout
 * (Creative/JEI inspired) with real-time search lists, numeric fields, toggles
 * and item pickers. Edits a working {@link SpawnerConfig} that is sent to the
 * server when the admin presses "Save & Get".
 */
public class FSpawnerScreen extends Screen {

    private enum Tab {
        ENTITIES("Entidades"), SPAWN("Spawn"), ATTRIBUTES("Atributos"), EQUIPMENT("Equipo"),
        EFFECTS("Efectos"), INFERNAL("Infernal"), DROPS("Drops"), APPEARANCE("Aspecto");

        final String label;
        Tab(String label) { this.label = label; }
    }

    private record Label(String text, int x, int y, int color) {}

    private static final String[] NAME_COLORS = {
            "white", "yellow", "gold", "red", "aqua", "green", "light_purple", "blue", "dark_purple", "gray"
    };

    private final SpawnerConfig config;
    private Tab activeTab = Tab.ENTITIES;
    private final List<Label> labels = new ArrayList<>();

    // layout
    private int leftPos, topPos, panelWidth, panelHeight;

    // cross-rebuild selection state
    private EquipmentSlot selectedSlot = EquipmentSlot.MAINHAND;
    private EffectEntry selectedEffect;
    private DropEntry selectedDrop;
    private EntityEntry selectedEntity;

    public FSpawnerScreen(SpawnerConfig config) {
        super(Component.translatable("fspawner.title"));
        this.config = config == null ? new SpawnerConfig() : config;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(this.width - 20, 460);
        panelHeight = Math.min(this.height - 20, 272);
        leftPos = (this.width - panelWidth) / 2;
        topPos = (this.height - panelHeight) / 2;
        labels.clear();

        initHeader();
        initFooter();

        switch (activeTab) {
            case ENTITIES -> initEntities();
            case SPAWN -> initSpawn();
            case ATTRIBUTES -> initAttributes();
            case EQUIPMENT -> initEquipment();
            case EFFECTS -> initEffects();
            case INFERNAL -> initInfernal();
            case DROPS -> initDrops();
            case APPEARANCE -> initAppearance();
        }
    }

    private int bodyX() { return leftPos + 8; }
    private int bodyY() { return topPos + 58; }
    private int bodyW() { return panelWidth - 16; }
    private int bodyH() { return panelHeight - 58 - 28; }

    // ------------------------------------------------------------------
    // Header / footer
    // ------------------------------------------------------------------

    private void initHeader() {
        Tab[] tabs = Tab.values();
        int gap = 2;
        int tabW = (panelWidth - 16 - gap * (tabs.length - 1)) / tabs.length;
        int x = leftPos + 8;
        int y = topPos + 24;
        for (Tab tab : tabs) {
            boolean active = tab == activeTab;
            String text = (active ? "\u00A7f" : "\u00A77") + tab.label;
            Button b = Button.builder(Component.literal(text), btn -> {
                this.activeTab = tab;
                this.rebuildWidgets();
            }).bounds(x, y, tabW, 18).build();
            addRenderableWidget(b);
            x += tabW + gap;
        }
    }

    private void initFooter() {
        int w = 150;
        Button save = Button.builder(Component.translatable("fspawner.button.save"), b -> {
            FSNetwork.sendToServer(new SaveConfigPacket(config.save()));
            onClose();
        }).bounds(leftPos + panelWidth - w - 8, topPos + panelHeight - 24, w, 18).build();
        addRenderableWidget(save);

        Button close = Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(leftPos + 8, topPos + panelHeight - 24, 80, 18).build();
        addRenderableWidget(close);
    }

    // ------------------------------------------------------------------
    // Tab: Entities
    // ------------------------------------------------------------------

    private void initEntities() {
        int colW = (bodyW() - 8) / 2;
        int listX = bodyX();
        int rightX = bodyX() + colW + 8;
        int searchY = bodyY();
        int listY = searchY + 20;
        int listH = bodyH() - 22;

        // mode toggle
        addRenderableWidget(Button.builder(
                Component.literal("Modo: " + (config.entityMode == SpawnerConfig.EntityMode.FIXED ? "Fijo" : "Pool")),
                b -> {
                    config.entityMode = config.entityMode == SpawnerConfig.EntityMode.FIXED
                            ? SpawnerConfig.EntityMode.POOL : SpawnerConfig.EntityMode.FIXED;
                    rebuildWidgets();
                }).bounds(rightX, searchY, colW, 16).build());

        // search box (left)
        EditBox search = new EditBox(font, listX, searchY, colW, 16, Component.empty());
        search.setHint(Component.translatable("fspawner.search"));
        addRenderableWidget(search);

        ScrollSelector<EntityType<?>> list = new ScrollSelector<>(listX, listY, colW, listH, 12,
                RegistryLists::entityName,
                t -> RegistryLists.entityName(t) + " " + RegistryLists.entityId(t),
                null);
        list.setItems(RegistryLists.entities());
        list.onSelect(t -> {
            String id = RegistryLists.entityId(t);
            if (config.entityMode == SpawnerConfig.EntityMode.FIXED) {
                config.entities.clear();
                config.entities.add(new EntityEntry(id));
            } else if (config.entities.stream().noneMatch(e -> e.id.equals(id))) {
                config.entities.add(new EntityEntry(id));
            }
            rebuildWidgets();
        });
        search.setResponder(list::setQuery);
        addRenderableWidget(list);

        // selected entities (right)
        ScrollSelector<EntityEntry> selected = new ScrollSelector<>(rightX, listY, colW, listH - 22, 12,
                e -> RegistryLists.entityName(typeOf(e.id)) + " (x" + e.weight + ")",
                e -> e.id, null);
        selected.setItems(new ArrayList<>(config.entities));
        selected.onSelect(e -> selectedEntity = e);
        addRenderableWidget(selected);

        addRenderableWidget(Button.builder(Component.literal("Quitar"), b -> {
            if (selectedEntity != null) {
                config.entities.remove(selectedEntity);
                selectedEntity = null;
                rebuildWidgets();
            }
        }).bounds(rightX, listY + listH - 18, colW, 16).build());

        labels.add(new Label("Selecciona entidades (cualquier mod):", listX, bodyY() - 10, 0xA0A0A0));
    }

    private EntityType<?> typeOf(String id) {
        EntityType<?> t = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                .getValue(net.minecraft.resources.ResourceLocation.tryParse(id));
        return t == null ? EntityType.PIG : t;
    }

    // ------------------------------------------------------------------
    // Tab: Spawn
    // ------------------------------------------------------------------

    private void initSpawn() {
        int x = bodyX();
        int y = bodyY();
        int fw = 60;
        int colGap = 150;

        addIntField(x + colGap, y, fw, config.spawnDelayMin, v -> config.spawnDelayMin = v, "Spawn Delay Min", x, y);
        addIntField(x + colGap, y + 20, fw, config.spawnDelayMax, v -> config.spawnDelayMax = v, "Spawn Delay Max", x, y + 20);
        addIntField(x + colGap, y + 40, fw, config.spawnCount, v -> config.spawnCount = v, "Spawn Count", x, y + 40);
        addIntField(x + colGap, y + 60, fw, config.spawnRange, v -> config.spawnRange = v, "Spawn Range", x, y + 60);
        addIntField(x + colGap, y + 80, fw, config.activationRange, v -> config.activationRange = v, "Activation Range", x, y + 80);
        addIntField(x + colGap, y + 100, fw, config.maxNearbyEntities, v -> config.maxNearbyEntities = v, "Max Nearby Entities", x, y + 100);

        int ty = y + 124;
        addToggle(x, ty, 130, "Oleadas", config.waves, () -> { config.waves = !config.waves; rebuildWidgets(); });
        addToggle(x + 140, ty, 130, "Boss Mode", config.bossMode, () -> { config.bossMode = !config.bossMode; rebuildWidgets(); });
        addToggle(x, ty + 20, 130, "Spawn Continuo", config.continuous, () -> { config.continuous = !config.continuous; rebuildWidgets(); });
    }

    // ------------------------------------------------------------------
    // Tab: Attributes
    // ------------------------------------------------------------------

    private void initAttributes() {
        int x = bodyX();
        int y = bodyY();
        int row = 0;
        for (FSAttributes.Attr attr : FSAttributes.ALL) {
            int fy = y + row * 22;
            Double current = config.attributes.get(attr.id);
            String val = current == null ? "" : trim(current);
            EditBox box = new EditBox(font, x + 180, fy, 70, 16, Component.empty());
            box.setValue(val);
            box.setMaxLength(16);
            box.setHint(Component.literal(trim(attr.defaultValue)));
            box.setResponder(s -> {
                String t = s.trim();
                if (t.isEmpty()) {
                    config.attributes.remove(attr.id);
                    return;
                }
                try {
                    config.attributes.put(attr.id, Double.parseDouble(t));
                } catch (NumberFormatException ignored) {
                }
            });
            addRenderableWidget(box);
            labels.add(new Label(attr.label, x, fy + 4, 0xE0E0E0));
            row++;
        }
        labels.add(new Label("Deja vac\u00edo para usar el valor por defecto.", x, y + row * 22 + 4, 0x808080));
    }

    // ------------------------------------------------------------------
    // Tab: Equipment
    // ------------------------------------------------------------------

    private void initEquipment() {
        int x = bodyX();
        int y = bodyY();
        int colW = (bodyW() - 8) / 2;

        // slot selector row
        EquipmentSlot[] slots = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] names = {"Mano", "2da Mano", "Casco", "Pechera", "Pantal\u00f3n", "Botas"};
        int bw = colW / 3 - 2;
        for (int i = 0; i < slots.length; i++) {
            EquipmentSlot s = slots[i];
            boolean active = s == selectedSlot;
            int bx = x + (i % 3) * (bw + 3);
            int by = y + (i / 3) * 18;
            addRenderableWidget(Button.builder(
                    Component.literal((active ? "\u00A7e" : "") + names[i]),
                    b -> { selectedSlot = s; rebuildWidgets(); }).bounds(bx, by, bw, 16).build());
        }

        EquipmentEntry entry = getOrCreateEquipment(selectedSlot);

        // current item display + fields
        int fy = y + 40;
        labels.add(new Label("Item: " + (entry.item.isEmpty() ? "\u00A77(vac\u00edo)" : entry.item.getHoverName().getString()),
                x, fy + 4, 0xFFFFFF));

        addRenderableWidget(Button.builder(Component.literal("Limpiar slot"), b -> {
            entry.item = ItemStack.EMPTY;
            rebuildWidgets();
        }).bounds(x + colW - 90, fy, 90, 16).build());

        addFloatField(x + 150, fy + 22, 60, entry.dropChance, v -> entry.dropChance = clamp01(v), "Prob. de Drop (0-1)", x, fy + 22 + 4);
        addFloatField(x + 150, fy + 44, 60, entry.appearChance, v -> entry.appearChance = clamp01(v), "Prob. de Aparici\u00f3n (0-1)", x, fy + 44 + 4);

        // item picker (right column)
        int rightX = x + colW + 8;
        EditBox search = new EditBox(font, rightX, y, colW, 16, Component.empty());
        search.setHint(Component.translatable("fspawner.search"));
        addRenderableWidget(search);

        ScrollSelector<Item> list = new ScrollSelector<>(rightX, y + 20, colW, bodyH() - 22, 18,
                RegistryLists::itemName,
                it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it),
                it -> new ItemStack(it));
        list.setItems(RegistryLists.items());
        list.onSelect(it -> {
            ItemStack stack = new ItemStack(it);
            // preserve existing enchantments/nbt if same item, otherwise fresh
            entry.item = stack;
            rebuildWidgets();
        });
        search.setResponder(list::setQuery);
        addRenderableWidget(list);
    }

    private EquipmentEntry getOrCreateEquipment(EquipmentSlot slot) {
        EquipmentEntry e = config.equipmentFor(slot);
        if (e == null) {
            e = new EquipmentEntry(slot);
            e.item = ItemStack.EMPTY;
            config.equipment.add(e);
        }
        return e;
    }

    // ------------------------------------------------------------------
    // Tab: Effects
    // ------------------------------------------------------------------

    private void initEffects() {
        int x = bodyX();
        int y = bodyY();
        int colW = (bodyW() - 8) / 2;
        int rightX = x + colW + 8;

        // left: all effects searchable
        EditBox search = new EditBox(font, x, y, colW, 16, Component.empty());
        search.setHint(Component.translatable("fspawner.search"));
        addRenderableWidget(search);

        ScrollSelector<MobEffect> all = new ScrollSelector<>(x, y + 20, colW, bodyH() - 22, 12,
                RegistryLists::effectName,
                e -> RegistryLists.effectName(e) + " " + RegistryLists.effectId(e), null);
        all.setItems(RegistryLists.effects());
        all.onSelect(e -> {
            String id = RegistryLists.effectId(e);
            if (config.effects.stream().noneMatch(fx -> fx.id.equals(id))) {
                config.effects.add(new EffectEntry(id));
            }
            rebuildWidgets();
        });
        search.setResponder(all::setQuery);
        addRenderableWidget(all);

        // right: current effects + editing
        ScrollSelector<EffectEntry> current = new ScrollSelector<>(rightX, y, colW, bodyH() - 70, 12,
                fx -> effectLabel(fx), fx -> fx.id, null);
        current.setItems(new ArrayList<>(config.effects));
        current.onSelect(fx -> { selectedEffect = fx; rebuildWidgets(); });
        addRenderableWidget(current);

        if (selectedEffect != null && config.effects.contains(selectedEffect)) {
            EffectEntry fx = selectedEffect;
            int fy = y + bodyH() - 66;
            addIntField(rightX + 70, fy, 40, fx.amplifier + 1, v -> fx.amplifier = Math.max(0, v - 1), "Nivel", rightX, fy + 4);
            addIntField(rightX + 70, fy + 18, 60, fx.duration, v -> fx.duration = Math.max(1, v), "Duraci\u00f3n", rightX, fy + 18 + 4);
            addToggle(rightX + 140, fy, colW - 140, fx.permanent ? "Permanente" : "Temporal", fx.permanent,
                    () -> { fx.permanent = !fx.permanent; rebuildWidgets(); });
            addRenderableWidget(Button.builder(Component.literal("Quitar efecto"), b -> {
                config.effects.remove(fx);
                selectedEffect = null;
                rebuildWidgets();
            }).bounds(rightX + 140, fy + 18, colW - 140, 16).build());
        }
    }

    private String effectLabel(EffectEntry fx) {
        MobEffect effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS
                .getValue(net.minecraft.resources.ResourceLocation.tryParse(fx.id));
        String name = effect != null ? effect.getDisplayName().getString() : fx.id;
        return name + " " + (fx.amplifier + 1) + (fx.permanent ? " \u00A7b\u221e" : "");
    }

    // ------------------------------------------------------------------
    // Tab: Infernal
    // ------------------------------------------------------------------

    private void initInfernal() {
        int x = bodyX();
        int y = bodyY();
        int colW = (bodyW() - 8) / 2;
        int rightX = x + colW + 8;

        addRenderableWidget(Button.builder(
                Component.literal("Modo: " + infernalModeLabel(config.infernal.mode)),
                b -> {
                    InfernalConfig.Mode[] modes = InfernalConfig.Mode.values();
                    config.infernal.mode = modes[(config.infernal.mode.ordinal() + 1) % modes.length];
                    rebuildWidgets();
                }).bounds(x, y, colW, 16).build());

        addIntField(x + 90, y + 22, 50, config.infernal.min, v -> config.infernal.min = Math.max(0, v), "M\u00ednimo", x, y + 22 + 4);
        addIntField(x + 90, y + 42, 50, config.infernal.max, v -> config.infernal.max = Math.max(0, v), "M\u00e1ximo", x, y + 42 + 4);

        boolean usePool = config.infernal.mode == InfernalConfig.Mode.RANDOM;
        labels.add(new Label(usePool ? "Pool permitido (aleatorio):" : "Modificadores fijos:", rightX, y - 10, 0xA0A0A0));

        List<String> mods = new ArrayList<>(InfernalModifiers.FRIENDLY.keySet());
        ScrollSelector<String> list = new ScrollSelector<>(rightX, y, colW, bodyH(), 12,
                InfernalModifiers::friendly,
                m -> InfernalModifiers.friendly(m) + " " + m, null);
        list.withCheckbox(m -> targetModList().contains(m));
        list.setItems(mods);
        list.onSelect(m -> {
            List<String> target = targetModList();
            if (target.contains(m)) {
                target.remove(m);
            } else {
                target.add(m);
            }
        });
        addRenderableWidget(list);

        labels.add(new Label("Infernal Mobs " + (com.fspawner.integration.InfernalMobsIntegration.isLoaded()
                ? "\u00A7adetectado" : "\u00A7cno instalado"), x, y + 70, 0xFFFFFF));
    }

    private List<String> targetModList() {
        return config.infernal.mode == InfernalConfig.Mode.RANDOM ? config.infernal.pool : config.infernal.mods;
    }

    private String infernalModeLabel(InfernalConfig.Mode mode) {
        return switch (mode) {
            case DISABLED -> "Desactivado";
            case ALWAYS -> "Siempre Infernal";
            case RANDOM -> "Aleatorio";
            case CUSTOM -> "Personalizado";
        };
    }

    // ------------------------------------------------------------------
    // Tab: Drops
    // ------------------------------------------------------------------

    private void initDrops() {
        int x = bodyX();
        int y = bodyY();
        int colW = (bodyW() - 8) / 2;
        int rightX = x + colW + 8;

        EditBox search = new EditBox(font, x, y, colW, 16, Component.empty());
        search.setHint(Component.translatable("fspawner.search"));
        addRenderableWidget(search);

        ScrollSelector<Item> all = new ScrollSelector<>(x, y + 20, colW, bodyH() - 22, 18,
                RegistryLists::itemName,
                it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it),
                it -> new ItemStack(it));
        all.setItems(RegistryLists.items());
        all.onSelect(it -> {
            config.drops.add(new DropEntry(new ItemStack(it), 1, 1, 1.0f));
            rebuildWidgets();
        });
        search.setResponder(all::setQuery);
        addRenderableWidget(all);

        ScrollSelector<DropEntry> current = new ScrollSelector<>(rightX, y, colW, bodyH() - 92, 18,
                d -> dropLabel(d), d -> d.item.getHoverName().getString(), d -> d.item);
        current.setItems(new ArrayList<>(config.drops));
        current.onSelect(d -> { selectedDrop = d; rebuildWidgets(); });
        addRenderableWidget(current);

        addToggle(rightX, y + bodyH() - 18, colW,
                config.keepVanillaDrops ? "Mantener Drops Vanilla" : "Reemplazar Drops Vanilla",
                config.keepVanillaDrops, () -> { config.keepVanillaDrops = !config.keepVanillaDrops; rebuildWidgets(); });

        if (selectedDrop != null && config.drops.contains(selectedDrop)) {
            DropEntry d = selectedDrop;
            int fy = y + bodyH() - 88;
            addIntField(rightX + 40, fy, 40, d.min, v -> d.min = Math.max(0, v), "Min", rightX, fy + 4);
            addIntField(rightX + 120, fy, 40, d.max, v -> d.max = Math.max(0, v), "Max", rightX + 90, fy + 4);
            addFloatField(rightX + 60, fy + 20, 50, d.chance, v -> d.chance = clamp01(v), "Chance(0-1)", rightX, fy + 20 + 4);
            addRenderableWidget(Button.builder(Component.literal("Quitar"), b -> {
                config.drops.remove(d);
                selectedDrop = null;
                rebuildWidgets();
            }).bounds(rightX + 130, fy + 20, colW - 130, 16).build());
        }
    }

    private String dropLabel(DropEntry d) {
        int pct = Math.round(d.chance * 100f);
        return d.item.getHoverName().getString() + " \u00A77" + d.min + "-" + d.max + " (" + pct + "%)";
    }

    // ------------------------------------------------------------------
    // Tab: Appearance
    // ------------------------------------------------------------------

    private void initAppearance() {
        int x = bodyX();
        int y = bodyY();
        int fw = bodyW() - 170;

        EditBox itemName = new EditBox(font, x + 160, y, Math.max(120, fw), 16, Component.empty());
        itemName.setMaxLength(128);
        itemName.setValue(config.itemName);
        itemName.setResponder(s -> config.itemName = s);
        addRenderableWidget(itemName);
        labels.add(new Label("Nombre del Item:", x, y + 4, 0xE0E0E0));

        EditBox mobName = new EditBox(font, x + 160, y + 22, Math.max(120, fw), 16, Component.empty());
        mobName.setMaxLength(128);
        mobName.setValue(config.mobName);
        mobName.setResponder(s -> config.mobName = s);
        addRenderableWidget(mobName);
        labels.add(new Label("Nombre Visible (mob):", x, y + 26, 0xE0E0E0));

        addRenderableWidget(Button.builder(Component.literal("Color: " + config.nameColor), b -> {
            int idx = 0;
            for (int i = 0; i < NAME_COLORS.length; i++) {
                if (NAME_COLORS[i].equals(config.nameColor)) { idx = i; break; }
            }
            config.nameColor = NAME_COLORS[(idx + 1) % NAME_COLORS.length];
            rebuildWidgets();
        }).bounds(x + 160, y + 44, 150, 16).build());
        labels.add(new Label("Color del Nombre:", x, y + 48, 0xE0E0E0));

        addToggle(x, y + 70, 200, config.mobNameVisible ? "Nombre Siempre Visible" : "Nombre Oculto",
                config.mobNameVisible, () -> { config.mobNameVisible = !config.mobNameVisible; rebuildWidgets(); });
        addToggle(x, y + 92, 200, config.glowing ? "Brillo (Glow) ON" : "Brillo (Glow) OFF",
                config.glowing, () -> { config.glowing = !config.glowing; rebuildWidgets(); });
        addToggle(x, y + 114, 200, config.particles ? "Part\u00edculas ON" : "Part\u00edculas OFF",
                config.particles, () -> { config.particles = !config.particles; rebuildWidgets(); });
    }

    // ------------------------------------------------------------------
    // Field helpers
    // ------------------------------------------------------------------

    private void addIntField(int x, int y, int w, int value, java.util.function.IntConsumer setter,
                             String label, int labelX, int labelY) {
        EditBox box = new EditBox(font, x, y, w, 16, Component.empty());
        box.setMaxLength(12);
        box.setValue(Integer.toString(value));
        box.setResponder(s -> {
            try {
                setter.accept(Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        });
        addRenderableWidget(box);
        labels.add(new Label(label, labelX, labelY, 0xE0E0E0));
    }

    private void addFloatField(int x, int y, int w, float value, java.util.function.DoubleConsumer setter,
                               String label, int labelX, int labelY) {
        EditBox box = new EditBox(font, x, y, w, 16, Component.empty());
        box.setMaxLength(12);
        box.setValue(trim(value));
        box.setResponder(s -> {
            try {
                setter.accept(Double.parseDouble(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        });
        addRenderableWidget(box);
        labels.add(new Label(label, labelX, labelY, 0xE0E0E0));
    }

    private void addToggle(int x, int y, int w, String text, boolean state, Runnable onToggle) {
        String prefix = state ? "\u00A7a" : "\u00A77";
        addRenderableWidget(Button.builder(Component.literal(prefix + text), b -> onToggle.run())
                .bounds(x, y, w, 16).build());
    }

    private static float clamp01(double v) {
        return (float) Math.max(0.0, Math.min(1.0, v));
    }

    private static String trim(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        // panel
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + panelHeight, 0xE0181822);
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + 20, 0xFF24243A);
        g.fill(leftPos, topPos + panelHeight - 1, leftPos + panelWidth, topPos + panelHeight, 0xFF3A3A4A);
        // divider between the tab row and the body
        g.fill(leftPos + 6, topPos + 45, leftPos + panelWidth - 6, topPos + 46, 0xFF3A3A4A);
        g.drawString(font, "\u00A7d\u2726 \u00A7fFantastic Spawner \u00A7d\u2726", leftPos + 8, topPos + 6, 0xFFFFFF, false);

        super.render(g, mouseX, mouseY, partialTick);

        for (Label l : labels) {
            g.drawString(font, l.text(), l.x(), l.y(), l.color(), false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
