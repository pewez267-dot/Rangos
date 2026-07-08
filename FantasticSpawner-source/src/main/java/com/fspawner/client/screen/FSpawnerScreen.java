/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraftforge.registries.ForgeRegistries
 */
package com.fspawner.client.screen;

import com.fspawner.client.widget.ScrollSelector;
import com.fspawner.config.DropEntry;
import com.fspawner.config.EffectEntry;
import com.fspawner.config.EntityEntry;
import com.fspawner.config.EquipmentEntry;
import com.fspawner.config.InfernalConfig;
import com.fspawner.config.SpawnerConfig;
import com.fspawner.integration.InfernalMobsIntegration;
import com.fspawner.integration.InfernalModifiers;
import com.fspawner.network.EditContext;
import com.fspawner.network.FSNetwork;
import com.fspawner.network.SaveConfigPacket;
import com.fspawner.util.FSAttributes;
import com.fspawner.util.RegistryLists;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

public class FSpawnerScreen
extends Screen {
    private static final String[] NAME_COLORS = new String[]{"white", "yellow", "gold", "red", "aqua", "green", "light_purple", "blue", "dark_purple", "gray"};
    private final SpawnerConfig config;
    private final EditContext context;
    private Tab activeTab = Tab.ENTITIES;
    private final List<Label> labels = new ArrayList<Label>();
    private final List<TooltipZone> tooltipZones = new ArrayList<TooltipZone>();
    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;
    private EquipmentSlot selectedSlot = EquipmentSlot.MAINHAND;
    private EffectEntry selectedEffect;
    private DropEntry selectedDrop;
    // Fuente de items para drops: false = registro (todos), true = inventario del jugador (para items con NBT unico, p.ej. llaves de crates).
    private boolean dropFromInventory = false;
    private EntityEntry selectedEntity;
    private ItemStack editingIcon;
    private int editingIconX;
    private int editingIconY;

    public FSpawnerScreen(SpawnerConfig config) {
        this(config, EditContext.newSession());
    }

    public FSpawnerScreen(SpawnerConfig config, EditContext context) {
        super((Component)Component.translatable((String)"fspawner.title"));
        this.config = config == null ? new SpawnerConfig() : config;
        this.context = context == null ? EditContext.newSession() : context;
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
        Tab[] tabs = Tab.values();
        int gap = 2;
        int tabW = (this.panelWidth - 16 - 2 * (tabs.length - 1)) / tabs.length;
        int x = this.leftPos + 8;
        int y = this.topPos + 24;
        for (Tab tab : tabs) {
            boolean active = tab == this.activeTab;
            String text = (active ? "\u00a7f" : "\u00a77") + tab.label;
            Button b = Button.builder((Component)Component.literal((String)text), btn -> {
                this.activeTab = tab;
                this.rebuildWidgets();
            }).bounds(x, y, tabW, 18).build();
            this.addRenderableWidget(b);
            x += tabW + 2;
        }
    }

    private void initFooter() {
        int w = 150;
        String saveLabel = switch (this.context.source) {
            case BLOCK -> "Guardar en bloque";
            case MAIN_HAND, OFF_HAND -> "Actualizar item";
            default -> "Guardar y obtener";
        };
        Button save = Button.builder((Component)Component.literal((String)saveLabel), b -> {
            FSNetwork.sendToServer(new SaveConfigPacket(this.config.save(), this.context));
            this.onClose();
        }).bounds(this.leftPos + this.panelWidth - 150 - 8, this.topPos + this.panelHeight - 24, 150, 18).build();
        this.addRenderableWidget(save);
        Button close = Button.builder((Component)Component.literal((String)"Cerrar"), b -> this.onClose()).bounds(this.leftPos + 8, this.topPos + this.panelHeight - 24, 80, 18).build();
        this.addRenderableWidget(close);
    }

    private void initEntities() {
        int colW = (this.bodyW() - 8) / 2;
        int listX = this.bodyX();
        int rightX = this.bodyX() + colW + 8;
        int searchY = this.bodyY();
        int listY = searchY + 20;
        int listH = this.bodyH() - 22;
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)("Modo: " + (this.config.entityMode == SpawnerConfig.EntityMode.FIXED ? "Fijo" : "Pool"))), b -> {
            this.config.entityMode = this.config.entityMode == SpawnerConfig.EntityMode.FIXED ? SpawnerConfig.EntityMode.POOL : SpawnerConfig.EntityMode.FIXED;
            this.rebuildWidgets();
        }).bounds(rightX, searchY, colW, 16).build());
        EditBox search = new EditBox(this.font, listX, searchY, colW, 16, (Component)Component.empty());
        search.setHint((Component)Component.translatable((String)"fspawner.search"));
        this.addRenderableWidget(search);
        ScrollSelector<EntityType<?>> list = new ScrollSelector<EntityType<?>>(listX, listY, colW, listH, 12, RegistryLists::entityName, t -> RegistryLists.entityName(t) + " " + RegistryLists.entityId(t), null);
        list.setItems(RegistryLists.entities());
        list.onSelect(t -> {
            String id = RegistryLists.entityId(t);
            if (this.config.entityMode == SpawnerConfig.EntityMode.FIXED) {
                this.config.entities.clear();
                this.config.entities.add(new EntityEntry(id));
            } else if (this.config.entities.stream().noneMatch(e -> e.id.equals(id))) {
                this.config.entities.add(new EntityEntry(id));
            }
            this.rebuildWidgets();
        });
        search.setResponder(list::setQuery);
        this.addRenderableWidget(list);
        ScrollSelector<EntityEntry> selected = new ScrollSelector<EntityEntry>(rightX, listY, colW, listH - 22, 12, e -> RegistryLists.entityName(this.typeOf(e.id)) + " (x" + e.weight, e -> e.id, null);
        selected.setItems(new ArrayList<EntityEntry>(this.config.entities));
        selected.onSelect(e -> {
            this.selectedEntity = e;
        });
        selected.setSelected(this.selectedEntity);
        this.addRenderableWidget(selected);
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Quitar"), b -> {
            if (this.selectedEntity != null) {
                this.config.entities.remove(this.selectedEntity);
                this.selectedEntity = null;
                this.rebuildWidgets();
            }
        }).bounds(rightX, listY + listH - 18, colW, 16).build());
        this.labels.add(new Label("Selecciona entidades (cualquier mod):", listX, this.bodyY() - 10, 0xA0A0A0));
    }

    private EntityType<?> typeOf(String id) {
        EntityType t = (EntityType)ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse((String)id));
        return t == null ? EntityType.PIG : t;
    }

    private void initSpawn() {
        int x = this.bodyX();
        int y = this.bodyY();
        int rowH = 18;
        int leftW = 200;
        int labelW = 130;
        int fieldX = x + 130;
        int fieldW = 60;
        this.addSecondsField(fieldX, y + 0, 60, this.config.spawnDelayMin, v -> {
            this.config.spawnDelayMin = v;
        }, "Tiempo m\u00edn (seg)", x, y + 0 + 4, FSpawnerScreen.desc("Tiempo m\u00ednimo entre apariciones, en segundos."));
        this.addSecondsField(fieldX, y + 18, 60, this.config.spawnDelayMax, v -> {
            this.config.spawnDelayMax = v;
        }, "Tiempo m\u00e1x (seg)", x, y + 18 + 4, FSpawnerScreen.desc("Tiempo m\u00e1ximo entre apariciones, en segundos."));
        this.addIntField(fieldX, y + 36, 60, this.config.spawnCount, v -> {
            this.config.spawnCount = v;
        }, "Cantidad por oleada", x, y + 36 + 4, FSpawnerScreen.desc("Cu\u00e1ntas entidades intenta generar cada vez."));
        this.addIntField(fieldX, y + 54, 60, this.config.spawnRange, v -> {
            this.config.spawnRange = v;
        }, "Radio (bloques)", x, y + 54 + 4, FSpawnerScreen.desc("Distancia en bloques alrededor del spawner."));
        this.addIntField(fieldX, y + 72, 60, this.config.activationRange, v -> {
            this.config.activationRange = v;
        }, "Activaci\u00f3n (bloques)", x, y + 72 + 4, FSpawnerScreen.desc("Distancia a la que un jugador activa el spawner."));
        this.addIntField(fieldX, y + 90, 60, this.config.maxNearbyEntities, v -> {
            this.config.maxNearbyEntities = v;
        }, "M\u00e1x. cercanas", x, y + 90 + 4, FSpawnerScreen.desc("L\u00edmite de entidades del mismo tipo cerca del spawner."));
        this.addSecondsField(fieldX, y + 108, 60, this.config.extraCooldown, v -> {
            this.config.extraCooldown = v;
        }, "Cooldown extra (seg)", x, y + 108 + 4, FSpawnerScreen.desc("Tiempo extra (segundos) sumado a los retardos."));
        int rx = x + 210;
        int rw = this.bodyW() - 210;
        int halfW = (rw - 6) / 2;
        int btnH = 16;
        int gap = 2;
        this.addCycle(rx, y, rw, "D\u00eda/Noche: " + FSpawnerScreen.dayCycleLabel(this.config.dayCycle), () -> {
            SpawnerConfig.DayCycle[] vals = SpawnerConfig.DayCycle.values();
            this.config.dayCycle = vals[(this.config.dayCycle.ordinal() + 1) % vals.length];
            this.rebuildWidgets();
        }, FSpawnerScreen.desc("Cualquiera / Solo d\u00eda / Solo noche."));
        int ry = y + 18;
        this.addCycle(rx, ry, rw, "Clima: " + FSpawnerScreen.weatherLabel(this.config.weather), () -> {
            SpawnerConfig.Weather[] vals2 = SpawnerConfig.Weather.values();
            this.config.weather = vals2[(this.config.weather.ordinal() + 1) % vals2.length];
            this.rebuildWidgets();
        }, FSpawnerScreen.desc("Cualquiera / Despejado / Lluvia / Tormenta."));
        this.addToggle(rx, ry += 18, halfW, "Oleadas", this.config.waves, () -> {
            this.config.waves = !this.config.waves;
            this.rebuildWidgets();
        });
        this.addToggle(rx + halfW + 6, ry, halfW, "Modo Jefe", this.config.bossMode, () -> {
            boolean bl = this.config.bossMode = !this.config.bossMode;
            if (this.config.bossMode) {
                this.config.spawnCount = 1;
                this.config.maxNearbyEntities = 1;
                this.config.spawnDelayMin = 20;
                this.config.spawnDelayMax = 40;
                this.config.extraCooldown = 0;
                this.config.spawnRange = 3;
                this.config.activationRange = 16;
                this.config.requiresPlayer = true;
                this.config.waves = false;
                this.config.continuous = false;
                this.config.spawnOnce = true;
                this.config.dropToInventory = true;
                this.config.glowing = true;
                if (this.config.rewardRadius <= 0) {
                    this.config.rewardRadius = 24;
                }
            }
            this.rebuildWidgets();
        }, FSpawnerScreen.desc("PRESET DE JEFE: 1 solo mob fuerte, brillante y persistente (no despawnea).", "Aparece r\u00e1pido al entrar un jugador al radio, una sola vez,", "y entrega el bot\u00edn directo al inventario."));
        this.addToggle(rx, ry += 18, halfW, "Continuo", this.config.continuous, () -> {
            this.config.continuous = !this.config.continuous;
            this.rebuildWidgets();
        });
        this.addToggle(rx + halfW + 6, ry, halfW, this.config.requiresPlayer ? "Req. jugador" : "Sin jugador", this.config.requiresPlayer, () -> {
            this.config.requiresPlayer = !this.config.requiresPlayer;
            this.rebuildWidgets();
        }, FSpawnerScreen.desc("Si est\u00e1 activo, s\u00f3lo aparece con jugador en rango."));
        this.addToggle(rx, ry += 18, halfW, this.config.requiresSky ? "Necesita cielo" : "Cielo libre", this.config.requiresSky, () -> {
            boolean bl = this.config.requiresSky = !this.config.requiresSky;
            if (this.config.requiresSky) {
                this.config.requiresNoSky = false;
            }
            this.rebuildWidgets();
        }, FSpawnerScreen.desc("S\u00f3lo aparece si la posici\u00f3n ve cielo (al aire libre)."));
        this.addToggle(rx + halfW + 6, ry, halfW, this.config.requiresNoSky ? "Bajo techo" : "Techo libre", this.config.requiresNoSky, () -> {
            boolean bl = this.config.requiresNoSky = !this.config.requiresNoSky;
            if (this.config.requiresNoSky) {
                this.config.requiresSky = false;
            }
            this.rebuildWidgets();
        }, FSpawnerScreen.desc("S\u00f3lo aparece bajo techo (sin acceso a cielo)."));
        this.addToggle(rx, ry += 18, halfW, "Jefe \u00fanico (1 vez)", this.config.spawnOnce, () -> {
            this.config.spawnOnce = !this.config.spawnOnce;
            this.rebuildWidgets();
        }, FSpawnerScreen.desc("Genera el jefe UNA sola vez cuando un jugador (supervivencia) entra al radio.", "El spawner se elimina tras aparecer el jefe."));
        this.addToggle(rx + halfW + 6, ry, halfW, "Drop al inventario", this.config.dropToInventory, () -> {
            this.config.dropToInventory = !this.config.dropToInventory;
            this.rebuildWidgets();
        }, FSpawnerScreen.desc("Al morir, entrega el bot\u00edn directo al inventario", "de cada jugador que pele\u00f3 contra el jefe (da\u00f1o o cercan\u00eda)."));
        this.addIntField(fieldX, y + 126, 60, this.config.rewardRadius, v -> {
            this.config.rewardRadius = FSpawnerScreen.clamp(v, 0, 256);
        }, "Radio recompensa", x, y + 126 + 4, FSpawnerScreen.desc("Radio (bloques) para repartir el bot\u00edn del jefe", "a los jugadores cercanos al morir."));
        int luzLabelW = 80;
        this.addIntField(rx + 80, ry += 18, halfW - 80 - 4, this.config.minLight, v -> {
            this.config.minLight = FSpawnerScreen.clamp(v, 0, 15);
        }, "Luz m\u00edn 0-15", rx, ry + 4);
        this.addIntField(rx + halfW + 6 + 80, ry, halfW - 80 - 4, this.config.maxLight, v -> {
            this.config.maxLight = FSpawnerScreen.clamp(v, 0, 15);
        }, "Luz m\u00e1x 0-15", rx + halfW + 6, ry + 4);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String dayCycleLabel(SpawnerConfig.DayCycle c) {
        return switch (c) {
            default -> throw new IncompatibleClassChangeError();
            case ANY -> "Cualquiera";
            case DAY_ONLY -> "Solo d\u00eda";
            case NIGHT_ONLY -> "Solo noche";
        };
    }

    private static String weatherLabel(SpawnerConfig.Weather w) {
        return switch (w) {
            default -> throw new IncompatibleClassChangeError();
            case ANY -> "Cualquiera";
            case CLEAR -> "Despejado";
            case RAIN -> "Lluvia";
            case THUNDER -> "Tormenta";
        };
    }

    private void initAttributes() {
        int x = this.bodyX();
        int y = this.bodyY();
        int row = 0;
        for (FSAttributes.Attr attr : FSAttributes.ALL) {
            int fy = y + row * 22;
            Double current = this.config.attributes.get(attr.id);
            String val = current == null ? "" : FSpawnerScreen.trim(current);
            EditBox box = new EditBox(this.font, x + 180, fy, 70, 16, (Component)Component.empty());
            box.setValue(val);
            box.setMaxLength(16);
            box.setHint((Component)Component.literal((String)FSpawnerScreen.trim(attr.defaultValue)));
            box.setResponder(s -> {
                String t = s.trim();
                if (t.isEmpty()) {
                    this.config.attributes.remove(attr.id);
                } else {
                    try {
                        this.config.attributes.put(attr.id, Double.parseDouble(t));
                    }
                    catch (NumberFormatException numberFormatException) {
                        // empty catch block
                    }
                }
            });
            this.addRenderableWidget(box);
            this.labels.add(new Label(attr.label, x, fy + 4, 0xE0E0E0));
            this.addTooltip(x, fy + 2, 170, 14, FSpawnerScreen.desc("Valor por defecto del mob: " + FSpawnerScreen.trim(attr.defaultValue), "Vac\u00edo = no modificarlo."));
            ++row;
        }
        this.labels.add(new Label("Deja vac\u00edo para usar el valor por defecto.", x, y + row * 22 + 4, 0x808080));
    }

    private void initEquipment() {
        int x = this.bodyX();
        int y = this.bodyY();
        int colW = (this.bodyW() - 8) / 2;
        EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] names = new String[]{"Mano", "2da Mano", "Casco", "Pechera", "Pantal\u00f3n", "Botas"};
        int bw = colW / 3 - 2;
        for (int i = 0; i < slots.length; ++i) {
            EquipmentSlot s = slots[i];
            boolean active = s == this.selectedSlot;
            int bx = x + i % 3 * (bw + 3);
            int by = y + i / 3 * 18;
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)((active ? "\u00a7e" : "") + names[i])), b -> {
                this.selectedSlot = s;
                this.rebuildWidgets();
            }).bounds(bx, by, bw, 16).build());
        }
        EquipmentEntry entry = this.getOrCreateEquipment(this.selectedSlot);
        int fy = y + 40;
        this.labels.add(new Label("Item: " + (entry.item.isEmpty() ? "\u00a77(vac\u00edo)" : entry.item.getHoverName().getString()), x, fy + 4, 0xFFFFFF));
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Limpiar slot"), b -> {
            entry.item = ItemStack.EMPTY;
            this.rebuildWidgets();
        }).bounds(x + colW - 90, fy, 90, 16).build());
        this.addPercentField(x + 150, fy + 22, 60, entry.dropChance, v -> {
            entry.dropChance = (float)v;
        }, "Prob. de Drop (%)", x, fy + 22 + 4);
        this.addPercentField(x + 150, fy + 44, 60, entry.appearChance, v -> {
            entry.appearChance = (float)v;
        }, "Prob. de Aparici\u00f3n (%)", x, fy + 44 + 4);
        int rightX = x + colW + 8;
        EditBox search = new EditBox(this.font, rightX, y, colW, 16, (Component)Component.empty());
        search.setHint((Component)Component.translatable((String)"fspawner.search"));
        this.addRenderableWidget(search);
        ScrollSelector<Item> list = new ScrollSelector<Item>(rightX, y + 20, colW, this.bodyH() - 22, 18, RegistryLists::itemName, it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it), it -> new ItemStack((ItemLike)it));
        list.setItems(RegistryLists.items());
        list.onSelect(it -> {
            entry.item = new ItemStack((ItemLike)it);
            this.rebuildWidgets();
        });
        search.setResponder(list::setQuery);
        this.addRenderableWidget(list);
    }

    private EquipmentEntry getOrCreateEquipment(EquipmentSlot slot) {
        EquipmentEntry e = this.config.equipmentFor(slot);
        if (e == null) {
            e = new EquipmentEntry(slot);
            e.item = ItemStack.EMPTY;
            this.config.equipment.add(e);
        }
        return e;
    }

    private void initEffects() {
        int x = this.bodyX();
        int y = this.bodyY();
        int colW = (this.bodyW() - 8) / 2;
        int rightX = x + colW + 8;
        EditBox search = new EditBox(this.font, x, y, colW, 16, (Component)Component.empty());
        search.setHint((Component)Component.translatable((String)"fspawner.search"));
        this.addRenderableWidget(search);
        ScrollSelector<MobEffect> all = new ScrollSelector<MobEffect>(x, y + 20, colW, this.bodyH() - 22, 12, RegistryLists::effectName, e -> RegistryLists.effectName(e) + " " + RegistryLists.effectId(e), null);
        all.setItems(RegistryLists.effects());
        all.onSelect(e -> {
            String id = RegistryLists.effectId(e);
            if (this.config.effects.stream().noneMatch(fxx -> fxx.id.equals(id))) {
                this.config.effects.add(new EffectEntry(id));
            }
            this.rebuildWidgets();
        });
        search.setResponder(all::setQuery);
        this.addRenderableWidget(all);
        ScrollSelector<EffectEntry> current = new ScrollSelector<EffectEntry>(rightX, y, colW, this.bodyH() - 70, 12, this::effectLabel, fxe -> fxe.id, null);
        current.setItems(new ArrayList<EffectEntry>(this.config.effects));
        current.onSelect(fxe -> {
            this.selectedEffect = fxe;
            this.rebuildWidgets();
        });
        current.setSelected(this.selectedEffect);
        this.addRenderableWidget(current);
        if (this.selectedEffect != null && this.config.effects.contains(this.selectedEffect)) {
            EffectEntry fx = this.selectedEffect;
            int fy = y + this.bodyH() - 66;
            this.addIntField(rightX + 70, fy, 40, fx.amplifier + 1, v -> {
                fx.amplifier = Math.max(0, v - 1);
            }, "Nivel", rightX, fy + 4);
            this.addIntField(rightX + 70, fy + 18, 60, fx.duration, v -> {
                fx.duration = Math.max(1, v);
            }, "Duraci\u00f3n", rightX, fy + 18 + 4);
            this.addToggle(rightX + 140, fy, colW - 140, fx.permanent ? "Permanente" : "Temporal", fx.permanent, () -> {
                fx.permanent = !fx.permanent;
                this.rebuildWidgets();
            });
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Quitar efecto"), b -> {
                this.config.effects.remove(fx);
                this.selectedEffect = null;
                this.rebuildWidgets();
            }).bounds(rightX + 140, fy + 18, colW - 140, 16).build());
        }
    }

    private String effectLabel(EffectEntry fx) {
        MobEffect effect = (MobEffect)ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse((String)fx.id));
        String name = effect != null ? effect.getDisplayName().getString() : fx.id;
        return name + " " + (fx.amplifier + 1) + (fx.permanent ? " \u00a7b\u221e" : "");
    }

    private void initInfernal() {
        int x = this.bodyX();
        int y = this.bodyY();
        int colW = (this.bodyW() - 8) / 2;
        int rightX = x + colW + 8;
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)("Modo: " + FSpawnerScreen.infernalModeLabel(this.config.infernal.mode))), b -> {
            InfernalConfig.Mode[] modes = InfernalConfig.Mode.values();
            this.config.infernal.mode = modes[(this.config.infernal.mode.ordinal() + 1) % modes.length];
            this.rebuildWidgets();
        }).bounds(x, y, colW, 16).build());
        this.addIntField(x + 90, y + 22, 50, this.config.infernal.min, v -> {
            this.config.infernal.min = Math.max(0, v);
        }, "M\u00ednimo", x, y + 22 + 4);
        this.addIntField(x + 90, y + 42, 50, this.config.infernal.max, v -> {
            this.config.infernal.max = Math.max(0, v);
        }, "M\u00e1ximo", x, y + 42 + 4);
        boolean usePool = this.config.infernal.mode == InfernalConfig.Mode.RANDOM;
        this.labels.add(new Label(usePool ? "Pool permitido (aleatorio):" : "Modificadores fijos:", rightX, y - 10, 0xA0A0A0));
        ArrayList<String> mods = new ArrayList<String>(InfernalModifiers.FRIENDLY.keySet());
        ScrollSelector<String> list = new ScrollSelector<String>(rightX, y, colW, this.bodyH(), 12, InfernalModifiers::friendly, m -> InfernalModifiers.friendly(m) + " " + m, null);
        list.withCheckbox(m -> this.targetModList().contains(m));
        list.setItems(mods);
        list.onSelect(m -> {
            List<String> target = this.targetModList();
            if (target.contains(m)) {
                target.remove(m);
            } else {
                target.add((String)m);
            }
        });
        this.addRenderableWidget(list);
        this.labels.add(new Label("Infernal Mobs " + (InfernalMobsIntegration.isLoaded() ? "\u00a7adetectado" : "\u00a7cno instalado"), x, y + 70, 0xFFFFFF));
    }

    private List<String> targetModList() {
        return this.config.infernal.mode == InfernalConfig.Mode.RANDOM ? this.config.infernal.pool : this.config.infernal.mods;
    }

    private static String infernalModeLabel(InfernalConfig.Mode mode) {
        return switch (mode) {
            default -> throw new IncompatibleClassChangeError();
            case DISABLED -> "Desactivado";
            case ALWAYS -> "Siempre Infernal";
            case RANDOM -> "Aleatorio";
            case CUSTOM -> "Personalizado";
        };
    }

    private void initDrops() {
        int x = this.bodyX();
        int y = this.bodyY();
        int colW = (this.bodyW() - 8) / 2;
        int rightX = x + colW + 8;
        // Toggle de fuente: Registro (todos los items) o Inventario del jugador (para items con NBT unico
        // como las llaves de crates, que no existen en el creativo ni por comando).
        this.addToggle(x, y, colW, this.dropFromInventory ? "Fuente: \u00a7bInventario" : "Fuente: \u00a7eRegistro", this.dropFromInventory, () -> {
            this.dropFromInventory = !this.dropFromInventory;
            this.rebuildWidgets();
        });
        int searchY = y + 18;
        int listY = searchY + 18;
        int listH = this.bodyH() - 40;
        EditBox search = new EditBox(this.font, x, searchY, colW, 16, (Component)Component.empty());
        search.setHint((Component)Component.translatable((String)"fspawner.search"));
        this.addRenderableWidget(search);
        if (this.dropFromInventory) {
            java.util.List<ItemStack> invItems = new ArrayList<ItemStack>();
            net.minecraft.world.entity.player.Player p = net.minecraft.client.Minecraft.getInstance().player;
            if (p != null) {
                for (ItemStack st : p.getInventory().items) {
                    if (st != null && !st.isEmpty()) {
                        invItems.add(st.copy());
                    }
                }
            }
            ScrollSelector<ItemStack> invList = new ScrollSelector<ItemStack>(x, listY, colW, listH, 18,
                st -> st.getHoverName().getString(),
                st -> st.getHoverName().getString() + " " + RegistryLists.itemId(st.getItem()),
                st -> st);
            invList.setItems(invItems);
            invList.onSelect(st -> {
                ItemStack add = st.copy();
                add.setCount(1);
                this.config.drops.add(new DropEntry(add, 1, 1, 1.0f));
                this.rebuildWidgets();
            });
            search.setResponder(invList::setQuery);
            this.addRenderableWidget(invList);
            if (invItems.isEmpty()) {
                this.labels.add(new Label("\u00a77Tu inventario esta vacio.", x, listY + 4, 0x909090));
            }
        } else {
            ScrollSelector<Item> all = new ScrollSelector<Item>(x, listY, colW, listH, 18, RegistryLists::itemName, it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it), it -> new ItemStack((ItemLike)it));
            all.setItems(RegistryLists.items());
            all.onSelect(it -> {
                this.config.drops.add(new DropEntry(new ItemStack((ItemLike)it), 1, 1, 1.0f));
                this.rebuildWidgets();
            });
            search.setResponder(all::setQuery);
            this.addRenderableWidget(all);
        }
        ScrollSelector<DropEntry> current = new ScrollSelector<DropEntry>(rightX, y, colW, this.bodyH() - 92, 18, this::dropLabel, de -> de.item.getHoverName().getString(), de -> de.item);
        current.setItems(new ArrayList<DropEntry>(this.config.drops));
        current.onSelect(de -> {
            this.selectedDrop = de;
            this.rebuildWidgets();
        });
        current.setSelected(this.selectedDrop);
        this.addRenderableWidget(current);
        this.addToggle(rightX, y + this.bodyH() - 18, colW, this.config.keepVanillaDrops ? "Mantener Drops Vanilla" : "Reemplazar Drops Vanilla", this.config.keepVanillaDrops, () -> {
            this.config.keepVanillaDrops = !this.config.keepVanillaDrops;
            this.rebuildWidgets();
        });
        if (this.selectedDrop != null && this.config.drops.contains(this.selectedDrop)) {
            DropEntry d = this.selectedDrop;
            int fy = y + this.bodyH() - 90;
            String editName = d.item.getHoverName().getString();
            String trimmedName = this.font.plainSubstrByWidth("Editando: " + editName, colW - 24);
            this.editingIcon = d.item;
            this.editingIconX = rightX;
            this.editingIconY = fy - 20;
            this.labels.add(new Label("\u00a7eEditando:\u00a7r " + trimmedName, rightX + 20, fy - 16, 0xFFFF55));
            this.addIntField(rightX + 48, fy, 36, d.min, v -> {
                d.min = Math.max(0, v);
            }, "Cant. m\u00edn", rightX, fy + 4);
            this.addIntField(rightX + 145, fy, 36, d.max, v -> {
                d.max = Math.max(0, v);
            }, "Max", rightX + 110, fy + 4);
            this.addPercentField(rightX + 110, fy + 22, 45, d.chance, v -> {
                d.chance = (float)v;
            }, "Probabilidad (%)", rightX, fy + 22 + 4);
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Quitar"), b -> {
                this.config.drops.remove(d);
                this.selectedDrop = null;
                this.rebuildWidgets();
            }).bounds(rightX, fy + 44, colW, 16).build());
        } else {
            this.editingIcon = null;
            this.labels.add(new Label("\u00a77Selecciona un item de la lista para editarlo.", rightX, y + this.bodyH() - 86, 0x909090));
        }
    }

    private String dropLabel(DropEntry d) {
        int pct = Math.round(d.chance * 100.0f);
        return d.item.getHoverName().getString() + " \u00a77" + d.min + "-" + d.max + " (" + pct + "%)";
    }

    private void initAppearance() {
        int x = this.bodyX();
        int y = this.bodyY();
        int fw = this.bodyW() - 170;
        EditBox itemName = new EditBox(this.font, x + 160, y, Math.max(120, fw), 16, (Component)Component.empty());
        itemName.setMaxLength(128);
        itemName.setValue(this.config.itemName);
        itemName.setResponder(s -> {
            this.config.itemName = s;
        });
        this.addRenderableWidget(itemName);
        this.labels.add(new Label("Nombre del Item:", x, y + 4, 0xE0E0E0));
        EditBox mobName = new EditBox(this.font, x + 160, y + 22, Math.max(120, fw), 16, (Component)Component.empty());
        mobName.setMaxLength(128);
        mobName.setValue(this.config.mobName);
        mobName.setResponder(s -> {
            this.config.mobName = s;
        });
        this.addRenderableWidget(mobName);
        this.labels.add(new Label("Nombre Visible (mob):", x, y + 26, 0xE0E0E0));
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)("Color: " + FSpawnerScreen.colorEs(this.config.nameColor))), b -> {
            int idx = 0;
            for (int i = 0; i < NAME_COLORS.length; ++i) {
                if (!NAME_COLORS[i].equals(this.config.nameColor)) continue;
                idx = i;
                break;
            }
            this.config.nameColor = NAME_COLORS[(idx + 1) % NAME_COLORS.length];
            this.rebuildWidgets();
        }).bounds(x + 160, y + 44, 150, 16).build());
        this.labels.add(new Label("Color del Nombre:", x, y + 48, 0xE0E0E0));
        this.addToggle(x, y + 70, 200, this.config.mobNameVisible ? "Nombre Siempre Visible" : "Nombre Oculto", this.config.mobNameVisible, () -> {
            this.config.mobNameVisible = !this.config.mobNameVisible;
            this.rebuildWidgets();
        });
        this.addToggle(x, y + 92, 200, this.config.glowing ? "Brillo: Activado" : "Brillo: Desactivado", this.config.glowing, () -> {
            this.config.glowing = !this.config.glowing;
            this.rebuildWidgets();
        });
        this.addToggle(x, y + 114, 200, this.config.particles ? "Part\u00edculas: Activado" : "Part\u00edculas: Desactivado", this.config.particles, () -> {
            this.config.particles = !this.config.particles;
            this.rebuildWidgets();
        });
    }

    private static String colorEs(String c) {
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

    private static List<Component> desc(String ... lines) {
        ArrayList<Component> out = new ArrayList<Component>();
        for (String s : lines) {
            out.add((Component)Component.literal((String)s));
        }
        return out;
    }

    private void addTooltip(int x, int y, int w, int h, List<Component> lines) {
        if (lines != null && !lines.isEmpty()) {
            this.tooltipZones.add(new TooltipZone(x, y, w, h, lines));
        }
    }

    private void addIntField(int x, int y, int w, int value, IntConsumer setter, String label, int labelX, int labelY) {
        this.addIntField(x, y, w, value, setter, label, labelX, labelY, null);
    }

    private void addIntField(int x, int y, int w, int value, IntConsumer setter, String label, int labelX, int labelY, List<Component> tooltip) {
        EditBox box = new EditBox(this.font, x, y, w, 16, (Component)Component.empty());
        box.setMaxLength(12);
        box.setValue(Integer.toString(value));
        box.setResponder(s -> {
            try {
                setter.accept(Integer.parseInt(s.trim()));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        });
        this.addRenderableWidget(box);
        this.labels.add(new Label(label, labelX, labelY, 0xE0E0E0));
        if (tooltip != null) {
            this.addTooltip(labelX, labelY - 2, x + w - labelX, 14, tooltip);
        }
    }

    private void addSecondsField(int x, int y, int w, int ticks, IntConsumer setterTicks, String label, int labelX, int labelY, List<Component> tooltip) {
        EditBox box = new EditBox(this.font, x, y, w, 16, (Component)Component.empty());
        box.setMaxLength(8);
        box.setValue(FSpawnerScreen.trim(Math.round((double)ticks / 20.0)));
        box.setResponder(s -> {
            String t = s.trim();
            if (!t.isEmpty()) {
                try {
                    double seconds = Double.parseDouble(t);
                    setterTicks.accept((int)Math.round(Math.max(0.0, seconds) * 20.0));
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            }
        });
        this.addRenderableWidget(box);
        this.labels.add(new Label(label, labelX, labelY, 0xE0E0E0));
        if (tooltip != null) {
            this.addTooltip(labelX, labelY - 2, x + w - labelX, 14, tooltip);
        }
    }

    private void addPercentField(int x, int y, int w, float value01, DoubleConsumer setter01, String label, int labelX, int labelY) {
        this.addPercentField(x, y, w, value01, setter01, label, labelX, labelY, null);
    }

    private void addPercentField(int x, int y, int w, float value01, DoubleConsumer setter01, String label, int labelX, int labelY, List<Component> tooltip) {
        EditBox box = new EditBox(this.font, x, y, w, 16, (Component)Component.empty());
        box.setMaxLength(6);
        box.setValue(FSpawnerScreen.trim(Math.round(FSpawnerScreen.clamp01(value01) * 100.0f)));
        box.setResponder(s -> {
            String t = s.trim();
            if (!t.isEmpty()) {
                try {
                    setter01.accept(FSpawnerScreen.clamp01(Double.parseDouble(t) / 100.0));
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            }
        });
        this.addRenderableWidget(box);
        this.labels.add(new Label(label, labelX, labelY, 0xE0E0E0));
        if (tooltip != null) {
            this.addTooltip(labelX, labelY - 2, x + w - labelX, 14, tooltip);
        }
    }

    private void addToggle(int x, int y, int w, String text, boolean state, Runnable onToggle) {
        this.addToggle(x, y, w, text, state, onToggle, null);
    }

    private void addToggle(int x, int y, int w, String text, boolean state, Runnable onToggle, List<Component> tooltip) {
        String prefix = state ? "\u00a7a" : "\u00a77";
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)(prefix + text)), b -> onToggle.run()).bounds(x, y, w, 16).build());
        if (tooltip != null) {
            this.addTooltip(x, y, w, 16, tooltip);
        }
    }

    private void addCycle(int x, int y, int w, String text, Runnable onClick, List<Component> tooltip) {
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)("\u00a7e" + text)), b -> onClick.run()).bounds(x, y, w, 16).build());
        if (tooltip != null) {
            this.addTooltip(x, y, w, 16, tooltip);
        }
    }

    private static float clamp01(double v) {
        return (float)Math.max(0.0, Math.min(1.0, v));
    }

    private static String trim(double value) {
        return value == Math.floor(value) && !Double.isInfinite(value) ? String.valueOf((long)value) : String.valueOf(value);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -535291870);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, -14408646);
        g.fill(this.leftPos, this.topPos + this.panelHeight - 1, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -12961206);
        g.fill(this.leftPos + 6, this.topPos + 45, this.leftPos + this.panelWidth - 6, this.topPos + 46, -12961206);
        g.drawString(this.font, "\u00a7d\u2726 \u00a7fFantastic Spawner \u00a7d\u2726", this.leftPos + 8, this.topPos + 6, 0xFFFFFF, false);
        super.render(g, mouseX, mouseY, partialTick);
        for (Label l : this.labels) {
            g.drawString(this.font, l.text(), l.x(), l.y(), l.color(), false);
        }
        if (this.editingIcon != null && !this.editingIcon.isEmpty()) {
            g.renderItem(this.editingIcon, this.editingIconX, this.editingIconY);
        }
        for (TooltipZone z : this.tooltipZones) {
            if (mouseX < z.x() || mouseX >= z.x() + z.w() || mouseY < z.y() || mouseY >= z.y() + z.h()) continue;
            g.renderComponentTooltip(this.font, z.lines(), mouseX, mouseY);
            break;
        }
    }

    public boolean isPauseScreen() {
        return false;
    }

    public boolean shouldCloseOnEsc() {
        return true;
    }

    private static enum Tab {
        ENTITIES("Entidades"),
        SPAWN("Aparici\u00f3n"),
        ATTRIBUTES("Atributos"),
        EQUIPMENT("Equipo"),
        EFFECTS("Efectos"),
        INFERNAL("Infernal"),
        DROPS("Drops"),
        APPEARANCE("Aspecto");

        final String label;

        private Tab(String label) {
            this.label = label;
        }
    }

    record Label(String text, int x, int y, int color) {
    }

    record TooltipZone(int x, int y, int w, int h, List<Component> lines) {
    }
}

