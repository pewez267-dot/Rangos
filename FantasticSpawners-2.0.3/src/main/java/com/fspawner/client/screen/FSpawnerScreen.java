// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.client.screen;

import com.fspawner.network.FSNetwork;
import com.fspawner.network.SaveConfigPacket;
import net.minecraft.client.gui.GuiGraphics;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import com.fspawner.integration.InfernalMobsIntegration;
import com.fspawner.integration.InfernalModifiers;
import com.fspawner.config.InfernalConfig;
import net.minecraft.world.effect.MobEffect;
import com.fspawner.config.EquipmentEntry;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import java.util.Iterator;
import com.fspawner.util.FSAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.Objects;
import java.util.function.Function;
import com.fspawner.client.widget.ScrollSelector;
import net.minecraft.world.entity.EntityType;
import com.fspawner.util.RegistryLists;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Button;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.fspawner.config.EntityEntry;
import com.fspawner.config.DropEntry;
import com.fspawner.config.EffectEntry;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.List;
import com.fspawner.network.EditContext;
import com.fspawner.config.SpawnerConfig;
import net.minecraft.client.gui.screens.Screen;

public class FSpawnerScreen extends Screen
{
    private static final String[] NAME_COLORS;
    private final SpawnerConfig config;
    private final EditContext context;
    private Tab activeTab;
    private final List<Label> labels;
    private final List<TooltipZone> tooltipZones;
    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;
    private EquipmentSlot selectedSlot;
    private EffectEntry selectedEffect;
    private DropEntry selectedDrop;
    private EntityEntry selectedEntity;
    private ItemStack editingIcon;
    private int editingIconX;
    private int editingIconY;
    
    public FSpawnerScreen(final SpawnerConfig config) {
        this(config, EditContext.newSession());
    }
    
    public FSpawnerScreen(final SpawnerConfig config, final EditContext context) {
        super((Component)Component.translatable("fspawner.title"));
        this.activeTab = Tab.ENTITIES;
        this.labels = new ArrayList<Label>();
        this.tooltipZones = new ArrayList<TooltipZone>();
        this.selectedSlot = EquipmentSlot.MAINHAND;
        this.config = ((config == null) ? new SpawnerConfig() : config);
        this.context = ((context == null) ? EditContext.newSession() : context);
    }
    
    protected void init() {
        this.panelWidth = Math.min(this.width - 20, 480);
        this.panelHeight = Math.min(this.height - 20, 320);
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;
        this.labels.clear();
        this.tooltipZones.clear();
        this.editingIcon = null;
        this.initHeader();
        this.initFooter();
        switch (this.activeTab) {
            case ENTITIES: {
                this.initEntities();
                break;
            }
            case SPAWN: {
                this.initSpawn();
                break;
            }
            case ATTRIBUTES: {
                this.initAttributes();
                break;
            }
            case EQUIPMENT: {
                this.initEquipment();
                break;
            }
            case EFFECTS: {
                this.initEffects();
                break;
            }
            case INFERNAL: {
                this.initInfernal();
                break;
            }
            case DROPS: {
                this.initDrops();
                break;
            }
            case APPEARANCE: {
                this.initAppearance();
                break;
            }
        }
    }
    
    private int bodyX() {
        return this.leftPos + 8;
    }
    
    private int bodyY() {
        return this.topPos + 58;
    }
    
    private int bodyW() {
        return this.panelWidth - 16;
    }
    
    private int bodyH() {
        return this.panelHeight - 58 - 28;
    }
    
    private void initHeader() {
        final Tab[] tabs = Tab.values();
        final int gap = 2;
        final int tabW = (this.panelWidth - 16 - gap * (tabs.length - 1)) / tabs.length;
        int x = this.leftPos + 8;
        final int y = this.topPos + 24;
        final Tab[] array = tabs;
        for (int length = array.length, i = 0; i < length; ++i) {
            final Tab tab = array[i];
            final boolean active = tab == this.activeTab;
            final String text = (active ? "§f" : "§7") + tab.label;
            final Button b = Button.builder((Component)Component.literal(text), btn -> {
                this.activeTab = tab;
                this.rebuildWidgets();
            }).bounds(x, y, tabW, 18).build();
            this.addRenderableWidget(b);
            x += tabW + gap;
        }
    }
    
    private void initFooter() {
        final int w = 150;
        final String saveLabel = switch (this.context.source) {
            case BLOCK -> "Guardar en bloque";
            case MAIN_HAND,  OFF_HAND -> "Actualizar item";
            default -> "Guardar y obtener";
        };
        final Button save = Button.builder((Component)Component.literal(saveLabel), b -> {
            FSNetwork.sendToServer(new SaveConfigPacket(this.config.save(), this.context));
            this.onClose();
        }).bounds(this.leftPos + this.panelWidth - w - 8, this.topPos + this.panelHeight - 24, w, 18).build();
        this.addRenderableWidget(save);
        final Button close = Button.builder((Component)Component.literal("Cerrar"), b -> this.onClose()).bounds(this.leftPos + 8, this.topPos + this.panelHeight - 24, 80, 18).build();
        this.addRenderableWidget(close);
    }
    
    private void initEntities() {
        final int colW = (this.bodyW() - 8) / 2;
        final int listX = this.bodyX();
        final int rightX = this.bodyX() + colW + 8;
        final int searchY = this.bodyY();
        final int listY = searchY + 20;
        final int listH = this.bodyH() - 22;
        this.addRenderableWidget(Button.builder((Component)Component.literal("Modo: " + ((this.config.entityMode == SpawnerConfig.EntityMode.FIXED) ? "Fijo" : "Pool")), b -> {
            this.config.entityMode = ((this.config.entityMode == SpawnerConfig.EntityMode.FIXED) ? SpawnerConfig.EntityMode.POOL : SpawnerConfig.EntityMode.FIXED);
            this.rebuildWidgets();
        }).bounds(rightX, searchY, colW, 16).build());
        final EditBox search = new EditBox(this.font, listX, searchY, colW, 16, (Component)Component.empty());
        search.setHint((Component)Component.translatable("fspawner.search"));
        this.addRenderableWidget(search);
        final ScrollSelector<EntityType<?>> list = new ScrollSelector<EntityType<?>>(listX, listY, colW, listH, 12, RegistryLists::entityName, t -> RegistryLists.entityName((EntityType<?>)t) + " " + RegistryLists.entityId((EntityType<?>)t), (Function<EntityType<?>, ItemStack>)null);
        list.setItems(RegistryLists.entities());
        list.onSelect(t -> {
            final String id = RegistryLists.entityId((EntityType<?>)t);
            if (this.config.entityMode == SpawnerConfig.EntityMode.FIXED) {
                this.config.entities.clear();
                this.config.entities.add(new EntityEntry(id));
            }
            else if (this.config.entities.stream().noneMatch(e -> e.id.equals(id))) {
                this.config.entities.add(new EntityEntry(id));
            }
            this.rebuildWidgets();
            return;
        });
        final EditBox editBox = search;
        final ScrollSelector<EntityType<?>> obj = list;
        Objects.requireNonNull(obj);
        editBox.setResponder(obj::setQuery);
        this.addRenderableWidget(list);
        final ScrollSelector<EntityEntry> selected = new ScrollSelector<EntityEntry>(rightX, listY, colW, listH - 22, 12, e -> RegistryLists.entityName(this.typeOf(e.id)) + " (x" + e.weight, e -> e.id, (Function<EntityEntry, ItemStack>)null);
        selected.setItems(new ArrayList<EntityEntry>(this.config.entities));
        selected.onSelect(e -> this.selectedEntity = e);
        selected.setSelected(this.selectedEntity);
        this.addRenderableWidget(selected);
        this.addRenderableWidget(Button.builder((Component)Component.literal("Quitar"), b -> {
            if (this.selectedEntity != null) {
                this.config.entities.remove(this.selectedEntity);
                this.selectedEntity = null;
                this.rebuildWidgets();
            }
        }).bounds(rightX, listY + listH - 18, colW, 16).build());
        this.labels.add(new Label("Selecciona entidades (cualquier mod):", listX, this.bodyY() - 10, 10526880));
    }
    
    private EntityType<?> typeOf(final String id) {
        final EntityType<?> t = (EntityType<?>)ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(id));
        return (EntityType<?>)((t == null) ? EntityType.PIG : t);
    }
    
    private void initSpawn() {
        final int x = this.bodyX();
        final int y = this.bodyY();
        final int rowH = 18;
        final int leftW = 200;
        final int labelW = 130;
        final int fieldX = x + labelW;
        final int fieldW = leftW - labelW - 10;
        this.addSecondsField(fieldX, y + 0 * rowH, fieldW, this.config.spawnDelayMin, v -> this.config.spawnDelayMin = v, "Tiempo m\u00edn (seg)", x, y + 0 * rowH + 4, desc("Tiempo m\u00ednimo entre apariciones, en segundos."));
        this.addSecondsField(fieldX, y + 1 * rowH, fieldW, this.config.spawnDelayMax, v -> this.config.spawnDelayMax = v, "Tiempo m\u00e1x (seg)", x, y + 1 * rowH + 4, desc("Tiempo m\u00e1ximo entre apariciones, en segundos."));
        this.addIntField(fieldX, y + 2 * rowH, fieldW, this.config.spawnCount, v -> this.config.spawnCount = v, "Cantidad por oleada", x, y + 2 * rowH + 4, desc("Cu\u00e1ntas entidades intenta generar cada vez."));
        this.addIntField(fieldX, y + 3 * rowH, fieldW, this.config.spawnRange, v -> this.config.spawnRange = v, "Radio (bloques)", x, y + 3 * rowH + 4, desc("Distancia en bloques alrededor del spawner."));
        this.addIntField(fieldX, y + 4 * rowH, fieldW, this.config.activationRange, v -> this.config.activationRange = v, "Activaci\u00f3n (bloques)", x, y + 4 * rowH + 4, desc("Distancia a la que un jugador activa el spawner."));
        this.addIntField(fieldX, y + 5 * rowH, fieldW, this.config.maxNearbyEntities, v -> this.config.maxNearbyEntities = v, "M\u00e1x. cercanas", x, y + 5 * rowH + 4, desc("L\u00edmite de entidades del mismo tipo cerca del spawner."));
        this.addSecondsField(fieldX, y + 6 * rowH, fieldW, this.config.extraCooldown, v -> this.config.extraCooldown = v, "Cooldown extra (seg)", x, y + 6 * rowH + 4, desc("Tiempo extra (segundos) sumado a los retardos."));
        final int rx = x + 210;
        final int rw = this.bodyW() - 210;
        final int halfW = (rw - 6) / 2;
        final int btnH = 16;
        final int gap = 2;
        int ry = y;
        this.addCycle(rx, ry, rw, "D\u00eda/Noche: " + dayCycleLabel(this.config.dayCycle), () -> {
            final SpawnerConfig.DayCycle[] vals = SpawnerConfig.DayCycle.values();
            this.config.dayCycle = vals[(this.config.dayCycle.ordinal() + 1) % vals.length];
            this.rebuildWidgets();
            return;
        }, desc("Cualquiera / Solo d\u00eda / Solo noche."));
        ry += btnH + gap;
        this.addCycle(rx, ry, rw, "Clima: " + weatherLabel(this.config.weather), () -> {
            final SpawnerConfig.Weather[] vals2 = SpawnerConfig.Weather.values();
            this.config.weather = vals2[(this.config.weather.ordinal() + 1) % vals2.length];
            this.rebuildWidgets();
            return;
        }, desc("Cualquiera / Despejado / Lluvia / Tormenta."));
        ry += btnH + gap;
        this.addToggle(rx, ry, halfW, "Oleadas", this.config.waves, () -> {
            this.config.waves = !this.config.waves;
            this.rebuildWidgets();
            return;
        });
        this.addToggle(rx + halfW + 6, ry, halfW, "Modo Jefe", this.config.bossMode, () -> {
            this.config.bossMode = !this.config.bossMode;
            this.rebuildWidgets();
            return;
        }, desc("Genera una sola entidad fuerte, brillante y persistente."));
        ry += btnH + gap;
        this.addToggle(rx, ry, halfW, "Continuo", this.config.continuous, () -> {
            this.config.continuous = !this.config.continuous;
            this.rebuildWidgets();
            return;
        });
        this.addToggle(rx + halfW + 6, ry, halfW, this.config.requiresPlayer ? "Req. jugador" : "Sin jugador", this.config.requiresPlayer, () -> {
            this.config.requiresPlayer = !this.config.requiresPlayer;
            this.rebuildWidgets();
            return;
        }, desc("Si est\u00e1 activo, s\u00f3lo aparece con jugador en rango."));
        ry += btnH + gap;
        this.addToggle(rx, ry, halfW, this.config.requiresSky ? "Necesita cielo" : "Cielo libre", this.config.requiresSky, () -> {
            this.config.requiresSky = !this.config.requiresSky;
            if (this.config.requiresSky) {
                this.config.requiresNoSky = false;
            }
            this.rebuildWidgets();
            return;
        }, desc("S\u00f3lo aparece si la posici\u00f3n ve cielo (al aire libre)."));
        this.addToggle(rx + halfW + 6, ry, halfW, this.config.requiresNoSky ? "Bajo techo" : "Techo libre", this.config.requiresNoSky, () -> {
            this.config.requiresNoSky = !this.config.requiresNoSky;
            if (this.config.requiresNoSky) {
                this.config.requiresSky = false;
            }
            this.rebuildWidgets();
            return;
        }, desc("S\u00f3lo aparece bajo techo (sin acceso a cielo)."));
        ry += btnH + gap;
        final int luzLabelW = 80;
        this.addIntField(rx + luzLabelW, ry, halfW - luzLabelW - 4, this.config.minLight, v -> this.config.minLight = clamp(v, 0, 15), "Luz m\u00edn 0-15", rx, ry + 4);
        this.addIntField(rx + halfW + 6 + luzLabelW, ry, halfW - luzLabelW - 4, this.config.maxLight, v -> this.config.maxLight = clamp(v, 0, 15), "Luz m\u00e1x 0-15", rx + halfW + 6, ry + 4);
    }
    
    private static int clamp(final int v, final int min, final int max) {
        return Math.max(min, Math.min(max, v));
    }
    
    private static String dayCycleLabel(final SpawnerConfig.DayCycle c) {
        return switch (c) {
            default -> throw new IncompatibleClassChangeError();
            case ANY -> "Cualquiera";
            case DAY_ONLY -> "Solo d\u00eda";
            case NIGHT_ONLY -> "Solo noche";
        };
    }
    
    private static String weatherLabel(final SpawnerConfig.Weather w) {
        return switch (w) {
            default -> throw new IncompatibleClassChangeError();
            case ANY -> "Cualquiera";
            case CLEAR -> "Despejado";
            case RAIN -> "Lluvia";
            case THUNDER -> "Tormenta";
        };
    }
    
    private void initAttributes() {
        final int x = this.bodyX();
        final int y = this.bodyY();
        int row = 0;
        for (FSAttributes.Attr attr : FSAttributes.ALL) {
            final int fy = y + row * 22;
            final Double current = this.config.attributes.get(attr.id);
            final String val = (current == null) ? "" : trim(current);
            final EditBox box = new EditBox(this.font, x + 180, fy, 70, 16, (Component)Component.empty());
            box.setValue(val);
            box.setMaxLength(16);
            box.setHint((Component)Component.literal(trim(attr.defaultValue)));
            box.setResponder(s -> {
                final String t = s.trim();
                if (t.isEmpty()) {
                    this.config.attributes.remove(attr.id);
                }
                else {
                    try {
                        this.config.attributes.put(attr.id, Double.parseDouble(t));
                    }
                    catch (final NumberFormatException ex) {}
                }
                return;
            });
            this.addRenderableWidget(box);
            this.labels.add(new Label(attr.label, x, fy + 4, 14737632));
            this.addTooltip(x, fy + 2, 170, 14, desc("Valor por defecto del mob: " + trim(attr.defaultValue), "Vac\u00edo = no modificarlo."));
            ++row;
        }
        this.labels.add(new Label("Deja vac\u00edo para usar el valor por defecto.", x, y + row * 22 + 4, 8421504));
    }
    
    private void initEquipment() {
        final int x = this.bodyX();
        final int y = this.bodyY();
        final int colW = (this.bodyW() - 8) / 2;
        final EquipmentSlot[] slots = { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
        final String[] names = { "Mano", "2da Mano", "Casco", "Pechera", "Pantal\u00f3n", "Botas" };
        final int bw = colW / 3 - 2;
        for (int i = 0; i < slots.length; ++i) {
            final EquipmentSlot s = slots[i];
            final boolean active = s == this.selectedSlot;
            final int bx = x + i % 3 * (bw + 3);
            final int by = y + i / 3 * 18;
            this.addRenderableWidget(Button.builder((Component)Component.literal((active ? "§e" : "") + names[i]), b -> {
                this.selectedSlot = s;
                this.rebuildWidgets();
            }).bounds(bx, by, bw, 16).build());
        }
        final EquipmentEntry entry = this.getOrCreateEquipment(this.selectedSlot);
        final int fy = y + 40;
        this.labels.add(new Label("Item: " + (entry.item.isEmpty() ? "§7(vac\u00edo)" : entry.item.getHoverName().getString()), x, fy + 4, 16777215));
        this.addRenderableWidget(Button.builder((Component)Component.literal("Limpiar slot"), b -> {
            entry.item = ItemStack.EMPTY;
            this.rebuildWidgets();
        }).bounds(x + colW - 90, fy, 90, 16).build());
        this.addPercentField(x + 150, fy + 22, 60, entry.dropChance, v -> entry.dropChance = (float)v, "Prob. de Drop (%)", x, fy + 22 + 4);
        this.addPercentField(x + 150, fy + 44, 60, entry.appearChance, v -> entry.appearChance = (float)v, "Prob. de Aparici\u00f3n (%)", x, fy + 44 + 4);
        final int rightX = x + colW + 8;
        final EditBox search = new EditBox(this.font, rightX, y, colW, 16, (Component)Component.empty());
        search.setHint((Component)Component.translatable("fspawner.search"));
        this.addRenderableWidget(search);
        final ScrollSelector<Item> list = new ScrollSelector<Item>(rightX, y + 20, colW, this.bodyH() - 22, 18, RegistryLists::itemName, it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it), it -> new ItemStack((ItemLike)it));
        list.setItems(RegistryLists.items());
        list.onSelect(it -> {
            entry.item = new ItemStack((ItemLike)it);
            this.rebuildWidgets();
            return;
        });
        final EditBox editBox = search;
        final ScrollSelector<Item> obj = list;
        Objects.requireNonNull(obj);
        editBox.setResponder(obj::setQuery);
        this.addRenderableWidget(list);
    }
    
    private EquipmentEntry getOrCreateEquipment(final EquipmentSlot slot) {
        EquipmentEntry e = this.config.equipmentFor(slot);
        if (e == null) {
            e = new EquipmentEntry(slot);
            e.item = ItemStack.EMPTY;
            this.config.equipment.add(e);
        }
        return e;
    }
    
    private void initEffects() {
        final int x = this.bodyX();
        final int y = this.bodyY();
        final int colW = (this.bodyW() - 8) / 2;
        final int rightX = x + colW + 8;
        final EditBox search = new EditBox(this.font, x, y, colW, 16, (Component)Component.empty());
        search.setHint((Component)Component.translatable("fspawner.search"));
        this.addRenderableWidget(search);
        final ScrollSelector<MobEffect> all = new ScrollSelector<MobEffect>(x, y + 20, colW, this.bodyH() - 22, 12, RegistryLists::effectName, e -> RegistryLists.effectName(e) + " " + RegistryLists.effectId(e), (Function<MobEffect, ItemStack>)null);
        all.setItems(RegistryLists.effects());
        all.onSelect(e -> {
            final String id = RegistryLists.effectId(e);
            if (this.config.effects.stream().noneMatch(fxx -> fxx.id.equals(id))) {
                this.config.effects.add(new EffectEntry(id));
            }
            this.rebuildWidgets();
            return;
        });
        final EditBox editBox = search;
        final ScrollSelector<MobEffect> obj = all;
        Objects.requireNonNull(obj);
        editBox.setResponder(obj::setQuery);
        this.addRenderableWidget(all);
        final ScrollSelector<EffectEntry> current = new ScrollSelector<EffectEntry>(rightX, y, colW, this.bodyH() - 70, 12, this::effectLabel, fxe -> fxe.id, (Function<EffectEntry, ItemStack>)null);
        current.setItems(new ArrayList<EffectEntry>(this.config.effects));
        current.onSelect(fxe -> {
            this.selectedEffect = fxe;
            this.rebuildWidgets();
            return;
        });
        current.setSelected(this.selectedEffect);
        this.addRenderableWidget(current);
        if (this.selectedEffect != null && this.config.effects.contains(this.selectedEffect)) {
            final EffectEntry fx = this.selectedEffect;
            final int fy = y + this.bodyH() - 66;
            this.addIntField(rightX + 70, fy, 40, fx.amplifier + 1, v -> fx.amplifier = Math.max(0, v - 1), "Nivel", rightX, fy + 4);
            this.addIntField(rightX + 70, fy + 18, 60, fx.duration, v -> fx.duration = Math.max(1, v), "Duraci\u00f3n", rightX, fy + 18 + 4);
            this.addToggle(rightX + 140, fy, colW - 140, fx.permanent ? "Permanente" : "Temporal", fx.permanent, () -> {
                fx.permanent = !fx.permanent;
                this.rebuildWidgets();
                return;
            });
            this.addRenderableWidget(Button.builder((Component)Component.literal("Quitar efecto"), b -> {
                this.config.effects.remove(fx);
                this.selectedEffect = null;
                this.rebuildWidgets();
            }).bounds(rightX + 140, fy + 18, colW - 140, 16).build());
        }
    }
    
    private String effectLabel(final EffectEntry fx) {
        final MobEffect effect = (MobEffect)ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(fx.id));
        final String name = (effect != null) ? effect.getDisplayName().getString() : fx.id;
        return name + " " + (fx.amplifier + 1) + (fx.permanent ? " §b\u221e" : "");
    }
    
    private void initInfernal() {
        final int x = this.bodyX();
        final int y = this.bodyY();
        final int colW = (this.bodyW() - 8) / 2;
        final int rightX = x + colW + 8;
        this.addRenderableWidget(Button.builder((Component)Component.literal("Modo: " + infernalModeLabel(this.config.infernal.mode)), b -> {
            final InfernalConfig.Mode[] modes = InfernalConfig.Mode.values();
            this.config.infernal.mode = modes[(this.config.infernal.mode.ordinal() + 1) % modes.length];
            this.rebuildWidgets();
        }).bounds(x, y, colW, 16).build());
        this.addIntField(x + 90, y + 22, 50, this.config.infernal.min, v -> this.config.infernal.min = Math.max(0, v), "M\u00ednimo", x, y + 22 + 4);
        this.addIntField(x + 90, y + 42, 50, this.config.infernal.max, v -> this.config.infernal.max = Math.max(0, v), "M\u00e1ximo", x, y + 42 + 4);
        final boolean usePool = this.config.infernal.mode == InfernalConfig.Mode.RANDOM;
        this.labels.add(new Label(usePool ? "Pool permitido (aleatorio):" : "Modificadores fijos:", rightX, y - 10, 10526880));
        final List<String> mods = new ArrayList<String>(InfernalModifiers.FRIENDLY.keySet());
        final ScrollSelector<String> list = new ScrollSelector<String>(rightX, y, colW, this.bodyH(), 12, InfernalModifiers::friendly, m -> InfernalModifiers.friendly(m) + " " + m, (Function<String, ItemStack>)null);
        list.withCheckbox(m -> this.targetModList().contains(m));
        list.setItems(mods);
        list.onSelect(m -> {
            final List<String> target = this.targetModList();
            if (target.contains(m)) {
                target.remove(m);
            }
            else {
                target.add(m);
            }
            return;
        });
        this.addRenderableWidget(list);
        this.labels.add(new Label("Infernal Mobs " + (InfernalMobsIntegration.isLoaded() ? "§adetectado" : "§cno instalado"), x, y + 70, 16777215));
    }
    
    private List<String> targetModList() {
        return (this.config.infernal.mode == InfernalConfig.Mode.RANDOM) ? this.config.infernal.pool : this.config.infernal.mods;
    }
    
    private static String infernalModeLabel(final InfernalConfig.Mode mode) {
        return switch (mode) {
            default -> throw new IncompatibleClassChangeError();
            case DISABLED -> "Desactivado";
            case ALWAYS -> "Siempre Infernal";
            case RANDOM -> "Aleatorio";
            case CUSTOM -> "Personalizado";
        };
    }
    
    private void initDrops() {
        final int x = this.bodyX();
        final int y = this.bodyY();
        final int colW = (this.bodyW() - 8) / 2;
        final int rightX = x + colW + 8;
        final EditBox search = new EditBox(this.font, x, y, colW, 16, (Component)Component.empty());
        search.setHint((Component)Component.translatable("fspawner.search"));
        this.addRenderableWidget(search);
        final ScrollSelector<Item> all = new ScrollSelector<Item>(x, y + 20, colW, this.bodyH() - 22, 18, RegistryLists::itemName, it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it), it -> new ItemStack((ItemLike)it));
        all.setItems(RegistryLists.items());
        all.onSelect(it -> {
            new DropEntry(new ItemStack((ItemLike)it), 1, 1, 1.0f);
            final DropEntry dropEntry;
            final DropEntry de = dropEntry;
            this.config.drops.add(de);
            this.rebuildWidgets();
            return;
        });
        final EditBox editBox = search;
        final ScrollSelector<Item> obj = all;
        Objects.requireNonNull(obj);
        editBox.setResponder(obj::setQuery);
        this.addRenderableWidget(all);
        DropEntry d = null;
        final ScrollSelector<DropEntry> current = new ScrollSelector<DropEntry>(rightX, y, colW, this.bodyH() - 92, 18, this::dropLabel, de -> de.item.getHoverName().getString(), de -> de.item);
        current.setItems(new ArrayList<DropEntry>(this.config.drops));
        current.onSelect(de -> {
            this.selectedDrop = de;
            this.rebuildWidgets();
            return;
        });
        current.setSelected(this.selectedDrop);
        this.addRenderableWidget(current);
        this.addToggle(rightX, y + this.bodyH() - 18, colW, this.config.keepVanillaDrops ? "Mantener Drops Vanilla" : "Reemplazar Drops Vanilla", this.config.keepVanillaDrops, () -> {
            this.config.keepVanillaDrops = !this.config.keepVanillaDrops;
            this.rebuildWidgets();
            return;
        });
        if (this.selectedDrop != null && this.config.drops.contains(this.selectedDrop)) {
            d = this.selectedDrop;
            final int fy = y + this.bodyH() - 90;
            final String editName = d.item.getHoverName().getString();
            final String trimmedName = this.font.plainSubstrByWidth("Editando: " + editName, colW - 24);
            this.editingIcon = d.item;
            this.editingIconX = rightX;
            this.editingIconY = fy - 20;
            this.labels.add(new Label("§eEditando:§r " + trimmedName, rightX + 20, fy - 16, 16777045));
            this.addIntField(rightX + 48, fy, 36, d.min, v -> d.min = Math.max(0, v), "Cant. m\u00edn", rightX, fy + 4);
            this.addIntField(rightX + 145, fy, 36, d.max, v -> d.max = Math.max(0, v), "Max", rightX + 110, fy + 4);
            this.addPercentField(rightX + 110, fy + 22, 45, d.chance, v -> d.chance = (float)v, "Probabilidad (%)", rightX, fy + 22 + 4);
            this.addRenderableWidget(Button.builder((Component)Component.literal("Quitar"), b -> {
                this.config.drops.remove(d);
                this.selectedDrop = null;
                this.rebuildWidgets();
            }).bounds(rightX, fy + 44, colW, 16).build());
        }
        else {
            this.editingIcon = null;
            this.labels.add(new Label("§7Selecciona un item de la lista para editarlo.", rightX, y + this.bodyH() - 86, 9474192));
        }
    }
    
    private String dropLabel(final DropEntry d) {
        final int pct = Math.round(d.chance * 100.0f);
        return d.item.getHoverName().getString() + " §7" + d.min + "-" + d.max + " (" + pct + "%)";
    }
    
    private void initAppearance() {
        final int x = this.bodyX();
        final int y = this.bodyY();
        final int fw = this.bodyW() - 170;
        final EditBox itemName = new EditBox(this.font, x + 160, y, Math.max(120, fw), 16, (Component)Component.empty());
        itemName.setMaxLength(128);
        itemName.setValue(this.config.itemName);
        itemName.setResponder(s -> this.config.itemName = s);
        this.addRenderableWidget(itemName);
        this.labels.add(new Label("Nombre del Item:", x, y + 4, 14737632));
        final EditBox mobName = new EditBox(this.font, x + 160, y + 22, Math.max(120, fw), 16, (Component)Component.empty());
        mobName.setMaxLength(128);
        mobName.setValue(this.config.mobName);
        mobName.setResponder(s -> this.config.mobName = s);
        this.addRenderableWidget(mobName);
        this.labels.add(new Label("Nombre Visible (mob):", x, y + 26, 14737632));
        this.addRenderableWidget(Button.builder((Component)Component.literal("Color: " + colorEs(this.config.nameColor)), b -> {
            int idx = 0;
            for (int i = 0; i < FSpawnerScreen.NAME_COLORS.length; ++i) {
                if (FSpawnerScreen.NAME_COLORS[i].equals(this.config.nameColor)) {
                    idx = i;
                    break;
                }
            }
            this.config.nameColor = FSpawnerScreen.NAME_COLORS[(idx + 1) % FSpawnerScreen.NAME_COLORS.length];
            this.rebuildWidgets();
        }).bounds(x + 160, y + 44, 150, 16).build());
        this.labels.add(new Label("Color del Nombre:", x, y + 48, 14737632));
        this.addToggle(x, y + 70, 200, this.config.mobNameVisible ? "Nombre Siempre Visible" : "Nombre Oculto", this.config.mobNameVisible, () -> {
            this.config.mobNameVisible = !this.config.mobNameVisible;
            this.rebuildWidgets();
            return;
        });
        this.addToggle(x, y + 92, 200, this.config.glowing ? "Brillo: Activado" : "Brillo: Desactivado", this.config.glowing, () -> {
            this.config.glowing = !this.config.glowing;
            this.rebuildWidgets();
            return;
        });
        this.addToggle(x, y + 114, 200, this.config.particles ? "Part\u00edculas: Activado" : "Part\u00edculas: Desactivado", this.config.particles, () -> {
            this.config.particles = !this.config.particles;
            this.rebuildWidgets();
        });
    }
    
    private static String colorEs(final String c) {
        return switch (c) {
            case "white" -> "Blanco";
            case "yellow" -> "Amarillo";
            case "gold" -> "Dorado";
            case "red" -> "Rojo";
            case "aqua" -> "Cian";
            case "green" -> "Verde";
            case "light_purple" -> "Rosa";
            case "blue" -> "Azul";
            case "dark_purple" -> "Morado";
            case "gray" -> "Gris";
            default -> c;
        };
    }
    
    private static List<Component> desc(final String... lines) {
        final List<Component> out = new ArrayList<Component>();
        for (final String s : lines) {
            out.add((Component)Component.literal(s));
        }
        return out;
    }
    
    private void addTooltip(final int x, final int y, final int w, final int h, final List<Component> lines) {
        if (lines != null && !lines.isEmpty()) {
            this.tooltipZones.add(new TooltipZone(x, y, w, h, lines));
        }
    }
    
    private void addIntField(final int x, final int y, final int w, final int value, final IntConsumer setter, final String label, final int labelX, final int labelY) {
        this.addIntField(x, y, w, value, setter, label, labelX, labelY, null);
    }
    
    private void addIntField(final int x, final int y, final int w, final int value, final IntConsumer setter, final String label, final int labelX, final int labelY, final List<Component> tooltip) {
        final EditBox box = new EditBox(this.font, x, y, w, 16, (Component)Component.empty());
        box.setMaxLength(12);
        box.setValue(Integer.toString(value));
        box.setResponder(s -> {
            try {
                setter.accept(Integer.parseInt(s.trim()));
            }
            catch (final NumberFormatException ex) {}
            return;
        });
        this.addRenderableWidget(box);
        this.labels.add(new Label(label, labelX, labelY, 14737632));
        if (tooltip != null) {
            this.addTooltip(labelX, labelY - 2, x + w - labelX, 14, tooltip);
        }
    }
    
    private void addSecondsField(final int x, final int y, final int w, final int ticks, final IntConsumer setterTicks, final String label, final int labelX, final int labelY, final List<Component> tooltip) {
        final EditBox box = new EditBox(this.font, x, y, w, 16, (Component)Component.empty());
        box.setMaxLength(8);
        box.setValue(trim((double)Math.round(ticks / 20.0)));
        box.setResponder(s -> {
            final String t = s.trim();
            if (t.isEmpty()) {
                return;
            }
            else {
                try {
                    final double seconds = Double.parseDouble(t);
                    setterTicks.accept((int)Math.round(Math.max(0.0, seconds) * 20.0));
                }
                catch (final NumberFormatException ex) {}
                return;
            }
        });
        this.addRenderableWidget(box);
        this.labels.add(new Label(label, labelX, labelY, 14737632));
        if (tooltip != null) {
            this.addTooltip(labelX, labelY - 2, x + w - labelX, 14, tooltip);
        }
    }
    
    private void addPercentField(final int x, final int y, final int w, final float value01, final DoubleConsumer setter01, final String label, final int labelX, final int labelY) {
        this.addPercentField(x, y, w, value01, setter01, label, labelX, labelY, null);
    }
    
    private void addPercentField(final int x, final int y, final int w, final float value01, final DoubleConsumer setter01, final String label, final int labelX, final int labelY, final List<Component> tooltip) {
        final EditBox box = new EditBox(this.font, x, y, w, 16, (Component)Component.empty());
        box.setMaxLength(6);
        box.setValue(trim(Math.round(clamp01(value01) * 100.0f)));
        box.setResponder(s -> {
            final String t = s.trim();
            if (t.isEmpty()) {
                return;
            }
            else {
                try {
                    setter01.accept(clamp01(Double.parseDouble(t) / 100.0));
                }
                catch (final NumberFormatException ex) {}
                return;
            }
        });
        this.addRenderableWidget(box);
        this.labels.add(new Label(label, labelX, labelY, 14737632));
        if (tooltip != null) {
            this.addTooltip(labelX, labelY - 2, x + w - labelX, 14, tooltip);
        }
    }
    
    private void addToggle(final int x, final int y, final int w, final String text, final boolean state, final Runnable onToggle) {
        this.addToggle(x, y, w, text, state, onToggle, null);
    }
    
    private void addToggle(final int x, final int y, final int w, final String text, final boolean state, final Runnable onToggle, final List<Component> tooltip) {
        final String prefix = state ? "§a" : "§7";
        this.addRenderableWidget(Button.builder((Component)Component.literal(prefix + text), b -> onToggle.run()).bounds(x, y, w, 16).build());
        if (tooltip != null) {
            this.addTooltip(x, y, w, 16, tooltip);
        }
    }
    
    private void addCycle(final int x, final int y, final int w, final String text, final Runnable onClick, final List<Component> tooltip) {
        this.addRenderableWidget(Button.builder((Component)Component.literal("§e" + text), b -> onClick.run()).bounds(x, y, w, 16).build());
        if (tooltip != null) {
            this.addTooltip(x, y, w, 16, tooltip);
        }
    }
    
    private static float clamp01(final double v) {
        return (float)Math.max(0.0, Math.min(1.0, v));
    }
    
    private static String trim(final double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long)value);
        }
        return String.valueOf(value);
    }
    
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        this.renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -535291870);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, -14408646);
        g.fill(this.leftPos, this.topPos + this.panelHeight - 1, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -12961206);
        g.fill(this.leftPos + 6, this.topPos + 45, this.leftPos + this.panelWidth - 6, this.topPos + 46, -12961206);
        g.drawString(this.font, "§d\u2726 §fFantastic Spawner §d\u2726", this.leftPos + 8, this.topPos + 6, 16777215, false);
        super.render(g, mouseX, mouseY, partialTick);
        for (final Label l : this.labels) {
            g.drawString(this.font, l.text(), l.x(), l.y(), l.color(), false);
        }
        if (this.editingIcon != null && !this.editingIcon.isEmpty()) {
            g.renderItem(this.editingIcon, this.editingIconX, this.editingIconY);
        }
        for (final TooltipZone z : this.tooltipZones) {
            if (mouseX >= z.x() && mouseX < z.x() + z.w() && mouseY >= z.y() && mouseY < z.y() + z.h()) {
                g.renderComponentTooltip(this.font, (List)z.lines(), mouseX, mouseY);
                break;
            }
        }
    }
    
    public boolean isPauseScreen() {
        return false;
    }
    
    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    static {
        NAME_COLORS = new String[] { "white", "yellow", "gold", "red", "aqua", "green", "light_purple", "blue", "dark_purple", "gray" };
    }
    
    private enum Tab
    {
        ENTITIES("Entidades"), 
        SPAWN("Aparici\u00f3n"), 
        ATTRIBUTES("Atributos"), 
        EQUIPMENT("Equipo"), 
        EFFECTS("Efectos"), 
        INFERNAL("Infernal"), 
        DROPS("Drops"), 
        APPEARANCE("Aspecto");
        
        final String label;
        
        private Tab(final String label) {
            this.label = label;
        }
    }
    
    record Label(String text, int x, int y, int color) {}
    
    record TooltipZone(int x, int y, int w, int h, List<Component> lines) {}
}
