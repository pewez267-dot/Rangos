package com.fscrates.client.screen;

import com.fscrates.animation.AnimationRegistry;
import com.fscrates.animation.CrateAnimation;
import com.fscrates.client.RegistryLists;
import com.fscrates.client.render.CrateStyles;
import com.fscrates.client.screen.NbtEditorScreen;
import com.fscrates.client.widget.ScrollSelector;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.ParticleLayer;
import com.fscrates.config.ParticleNames;
import com.fscrates.config.Rarity;
import com.fscrates.config.RewardEntry;
import com.fscrates.network.FSNetwork;
import com.fscrates.network.SaveCratePacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class CrateEditorScreen
extends Screen {
    private final CrateConfig config;
    private final BlockPos boundPos;
    private Tab activeTab = Tab.INFO;
    private final List<Label> labels = new ArrayList<Label>();
    private final List<TooltipZone> tooltipZones = new ArrayList<TooltipZone>();
    private String helpLine = "";
    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;
    private RewardEntry selectedReward;
    private ParticleLayer selectedLayer;
    private int probScroll;
    private static final String COLOR_CHARS = "f7e6cab9d5234180";

    public CrateEditorScreen(CrateConfig config) {
        this(config, null);
    }

    public CrateEditorScreen(CrateConfig config, BlockPos boundPos) {
        super((Component)Component.literal((String)"Editor de Crate"));
        this.config = config == null ? new CrateConfig() : config;
        this.boundPos = boundPos;
    }

    protected void init() {
        this.panelWidth = Math.min(this.width - 16, 540);
        this.panelHeight = Math.min(this.height - 16, 320);
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;
        this.labels.clear();
        this.tooltipZones.clear();
        this.initHeader();
        this.initFooter();
        switch (this.activeTab) {
            case INFO: {
                this.initInfo();
                break;
            }
            case REWARDS: {
                this.initRewards();
                break;
            }
            case PROBABILITY: {
                this.initProbability();
                break;
            }

            case APPEARANCE: {
                this.initAppearance();
                break;
            }
            case STYLE: {
                this.initStyle();
                break;
            }
            case PARTICLES: {
                this.initParticles();
                break;
            }
            case KEY: {
                this.initKey();
                break;
            }
            case SETTINGS: {
                this.initSettings();
            }
        }
    }

    private int bodyX() {
        return this.leftPos + 8;
    }

    private int bodyY() {
        return this.topPos + 62;
    }

    private int bodyW() {
        return this.panelWidth - 16;
    }

    private int bodyH() {
        return this.panelHeight - 62 - 28;
    }

    private void initHeader() {
        Tab[] tabs = Tab.values();
        int gap = 2;
        int tabW = (this.panelWidth - 16 - 2 * (tabs.length - 1)) / tabs.length;
        int x = this.leftPos + 8;
        int y = this.topPos + 24;
        for (Tab tab : tabs) {
            boolean active = tab == this.activeTab;
            String text = (active ? "\u00a7f\u00a7l" : "\u00a77") + tab.label;
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)text), b -> {
                this.activeTab = tab;
                this.rebuildWidgets();
            }).bounds(x, y, tabW, 18).build());
            x += tabW + 2;
        }
    }

    private void initFooter() {
        int w = 150;
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)(this.boundPos != null ? "\u00a7aGuardar cambios" : "\u00a7aGuardar y Obtener")), b -> {
            FSNetwork.sendToServer(new SaveCratePacket(this.config.save(), this.boundPos));
            this.onClose();
        }).bounds(this.leftPos + this.panelWidth - 150 - 8, this.topPos + this.panelHeight - 24, 150, 18).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Cerrar"), b -> this.onClose()).bounds(this.leftPos + 8, this.topPos + this.panelHeight - 24, 80, 18).build());
    }

    private void initInfo() {
        this.helpLine = "Datos b\u00e1sicos: ID, nombre, tier y tiradas por apertura.";
        int x = this.bodyX();
        int y = this.bodyY();
        EditBox id = new EditBox(this.font, x + 170, y, 200, 16, (Component)Component.empty());
        id.setMaxLength(48);
        id.setValue(this.config.id);
        id.setResponder(s -> {
            this.config.id = s.trim().toLowerCase().replace(' ', '_');
        });
        this.addRenderableWidget(id);
        this.addLabel("ID de la crate:", x, y + 4, CrateEditorScreen.desc("Identificador \u00fanico (sin espacios).", "Se usa en /fscrate give, edit, delete.", "Ej: cofre_legendario"));
        EditBox name = new EditBox(this.font, x + 170, y + 24, 200, 16, (Component)Component.empty());
        name.setMaxLength(128);
        name.setValue(this.config.displayName);
        name.setResponder(s -> {
            this.config.displayName = s;
        });
        this.addRenderableWidget(name);
        this.addLabel("Nombre visible:", x, y + 28, CrateEditorScreen.desc("Nombre del item y del holograma. Acepta c\u00f3digos & o \u00a7."));
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)("Rareza base: " + this.config.rarity.color() + this.config.rarity.displayName())), b -> {
            this.config.rarity = this.config.rarity.next();
            this.rebuildWidgets();
        }).bounds(x + 170, y + 48, 200, 16).build());
        this.addLabel("Rareza base:", x, y + 52, CrateEditorScreen.desc("Rareza por defecto de la crate: color del item y pool 'Auto'.", "La llave es UNIVERSAL (Fantastic Key). Las probabilidades de", "cada rareza se editan en la pesta\u00f1a \u00abRarezas\u00bb."));
        this.addIntField(x + 170, y + 72, 60, this.config.rolls, v -> {
            this.config.rolls = Math.max(1, v);
        }, "Tiradas por apertura:", x, y + 76, CrateEditorScreen.desc("Cu\u00e1ntas recompensas (por probabilidad) se entregan.", "Las garantizadas se suman aparte."));
        this.addLabel("\u00a78Estilo: \u00a7b" + CrateStyles.displayName(this.config.styleId) + " \u00a77\u2192 pesta\u00f1a \u00abDise\u00f1o\u00bb", x, y + 100, CrateEditorScreen.desc("Dise\u00f1o visual de la crate (independiente del tier).", "C\u00e1mbialo en la pesta\u00f1a \u00abDise\u00f1o\u00bb."));
        this.addLabel("\u00a78Animaci\u00f3n: \u00a7f" + AnimationRegistry.get(this.config.animationId).displayName(), x, y + 124, null);
        this.addLabel("\u00a78Recompensas: \u00a7f" + this.config.rewards.size() + "  \u00a78Capas de part\u00edculas: \u00a7f" + this.config.particleLayers.size(), x, y + 136, null);
    }

    private void initRewards() {
        boolean editingEffect;
        this.helpLine = "Izquierda: busca y clic en un item. Derecha: lista (scroll) y editor de la seleccionada.";
        int x = this.bodyX();
        int y = this.bodyY();
        int colW = (this.bodyW() - 8) / 2;
        int rightX = x + colW + 8;
        EditBox search = new EditBox(this.font, x, y, colW, 16, (Component)Component.empty());
        boolean bl = editingEffect = this.selectedReward != null && this.config.rewards.contains(this.selectedReward) && this.selectedReward.type == RewardEntry.Type.EFFECT;
        if (editingEffect) {
            RewardEntry er = this.selectedReward;
            search.setHint((Component)Component.literal((String)"Buscar efecto..."));
            this.addRenderableWidget(search);
            ScrollSelector<ResourceLocation> effects = new ScrollSelector<ResourceLocation>(x, y + 20, colW, this.bodyH() - 22, 16, rl -> (rl.toString().equals(er.effectId) ? "\u00a7a\u2714 " : "\u00a7f") + RegistryLists.effectName(rl), rl -> RegistryLists.effectName(rl) + " " + rl, rl -> ItemStack.EMPTY);
            effects.setItems(RegistryLists.effects());
            effects.onSelect(rl -> {
                er.effectId = rl.toString();
                this.rebuildWidgets();
            });
            search.setResponder(effects::setQuery);
            this.addRenderableWidget(effects);
        } else {
            search.setHint((Component)Component.literal((String)"Buscar item..."));
            this.addRenderableWidget(search);
            ScrollSelector<Item> items = new ScrollSelector<Item>(x, y + 20, colW, this.bodyH() - 22, 18, RegistryLists::itemName, it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it), it -> new ItemStack((ItemLike)it));
            items.setItems(RegistryLists.items());
            items.onSelect(it -> {
                RewardEntry r2 = new RewardEntry(RewardEntry.Type.ITEM);
                r2.item = new ItemStack((ItemLike)it);
                r2.label = RegistryLists.itemName(it);
                r2.chance = 10.0;
                this.config.rewards.add(r2);
                this.selectedReward = r2;
                this.rebuildWidgets();
            });
            search.setResponder(items::setQuery);
            this.addRenderableWidget(items);
        }
        Object selR = null;
        ScrollSelector<RewardEntry> current = new ScrollSelector<RewardEntry>(rightX, y, colW, this.bodyH() - 98, 16, rx -> (rx == this.selectedReward ? "\u00a7e\u25b6 " : "\u00a7f") + rx.describe() + " " + ((RewardEntry)rx).effectiveRarity(this.config.rarity).color() + "[" + ((RewardEntry)rx).effectiveRarity(this.config.rarity).displayName() + "] \u00a77(" + CrateEditorScreen.fmt(this.config.normalizedPercentInPool((RewardEntry)rx)) + "%)", RewardEntry::describe, rx -> rx.type == RewardEntry.Type.ITEM ? rx.item : ItemStack.EMPTY);
        current.setItems(new ArrayList<RewardEntry>(this.config.rewards));
        current.onSelect(rx -> {
            this.selectedReward = rx;
            this.rebuildWidgets();
        });
        this.addRenderableWidget(current);
        int addY = y + this.bodyH() - 94;
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"+ XP"), b -> {
            RewardEntry rx = new RewardEntry(RewardEntry.Type.XP);
            rx.xp = 100;
            this.config.rewards.add(rx);
            this.rebuildWidgets();
        }).bounds(rightX, addY, colW / 2 - 2, 16).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"+ Efecto"), b -> {
            this.config.rewards.add(new RewardEntry(RewardEntry.Type.EFFECT));
            this.rebuildWidgets();
        }).bounds(rightX + colW / 2, addY, colW / 2 - 2, 16).build());
        if (this.selectedReward != null && this.config.rewards.contains(this.selectedReward)) {
            RewardEntry r = this.selectedReward;
            int fy = y + this.bodyH() - 72;
            this.addDoubleField(rightX + 70, fy, 50, r.chance, v -> {
                r.chance = Math.max(0.0, v);
            }, "Prob. (%)", rightX, fy + 4, CrateEditorScreen.desc("Probabilidad en %. Se normaliza con las dem\u00e1s para sumar 100%."));
            this.addIntField(rightX + 150, fy, 36, r.minAmount, v -> {
                r.minAmount = Math.max(1, v);
            }, "Min", rightX + 122, fy + 4, CrateEditorScreen.desc("Cantidad m\u00ednima entregada."));
            this.addIntField(rightX + 235, fy, 36, r.maxAmount, v -> {
                r.maxAmount = Math.max(1, v);
            }, "Max", rightX + 200, fy + 4, CrateEditorScreen.desc("Cantidad m\u00e1xima entregada."));
            this.addToggle(rightX, fy + 22, colW - 70, r.guaranteed ? "Garantizada: S\u00ed" : "Garantizada: No", r.guaranteed, () -> {
                r.guaranteed = !r.guaranteed;
                this.rebuildWidgets();
            }, CrateEditorScreen.desc("Si est\u00e1 activo, SIEMPRE se entrega (100%)."));
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7cQuitar"), b -> {
                this.config.rewards.remove(r);
                this.selectedReward = null;
                this.rebuildWidgets();
            }).bounds(rightX + colW - 64, fy + 22, 64, 16).build());
            if (r.type == RewardEntry.Type.XP) {
                this.addIntField(rightX + 40, fy + 44, 80, r.xp, v -> {
                    r.xp = Math.max(0, v);
                }, "XP", rightX, fy + 48, CrateEditorScreen.desc("Puntos de experiencia entregados."));
            } else if (r.type == RewardEntry.Type.EFFECT) {
                this.addIntField(rightX + 44, fy + 44, 40, r.effectAmplifier + 1, v -> {
                    r.effectAmplifier = Math.max(0, v - 1);
                }, "Nivel", rightX, fy + 48, CrateEditorScreen.desc("Nivel del efecto (1 = nivel I, 2 = nivel II...).", "Aumenta la potencia del efecto."));
                this.addSecondsField(rightX + 175, fy + 44, 50, r.effectDuration, v -> {
                    r.effectDuration = Math.max(1, v);
                }, "Duraci\u00f3n (s)", rightX + 96, fy + 48, CrateEditorScreen.desc("Duraci\u00f3n del efecto en segundos.", "Elige el efecto en la lista de la izquierda."));
            } else if (r.type == RewardEntry.Type.ITEM) {
                int halfBtn = (colW - 4) / 2;
                Rarity itemR = r.effectiveRarity(this.config.rarity);
                String rarLabel = r.rarity != null && !r.rarity.isBlank() ? "Rareza: " + itemR.color() + itemR.displayName() : "\u00a77Rareza: Auto";
                this.addRenderableWidget(Button.builder((Component)Component.literal((String)rarLabel), b -> {
                    r.rarity = CrateEditorScreen.cycleItemRarity(r.rarity);
                    this.rebuildWidgets();
                }).bounds(rightX, fy + 44, halfBtn, 16).build());
                this.tooltipZones.add(new TooltipZone(rightX, fy + 44, halfBtn, 16, CrateEditorScreen.desc("Rareza de ESTE item del pool.", "Define el color de la luz, el sonido y las part\u00edculas", "cuando este item es el premio. Auto = usa el tier de la crate.")));
                this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7b\u270e NBT del item"), b -> {
                    if (r.item != null && !r.item.isEmpty()) {
                        this.minecraft.setScreen((Screen)new NbtEditorScreen(this, r.item));
                    }
                }).bounds(rightX + halfBtn + 4, fy + 44, colW - halfBtn - 4, 16).build());
                this.tooltipZones.add(new TooltipZone(rightX + halfBtn + 4, fy + 44, colW - halfBtn - 4, 16, CrateEditorScreen.desc("Editor de NBT: nombre, lore con color,", "encantamientos, atributos, irrompible, CustomModelData...", "Todo manual, sin pegar comandos.")));
            }
        }
    }

    private void initProbability() {
        int visibleRows;
        this.helpLine = "Escribe la probabilidad de cada recompensa en %. Usa la rueda del rat\u00f3n para desplazar la lista. Se normaliza a 100% autom\u00e1ticamente.";
        int x = this.bodyX();
        int y = this.bodyY();
        int rowH = 22;
        int total = this.config.rewards.size();
        int maxScroll = Math.max(0, total - (visibleRows = Math.max(1, this.bodyH() / 22)));
        if (this.probScroll > maxScroll) {
            this.probScroll = maxScroll;
        }
        if (this.probScroll < 0) {
            this.probScroll = 0;
        }
        int end = Math.min(total, this.probScroll + visibleRows);
        for (int i = this.probScroll; i < end; ++i) {
            RewardEntry r = this.config.rewards.get(i);
            int ry = y + (i - this.probScroll) * 22;
            if (r.guaranteed) continue;
            this.addDoubleField(x + 150, ry, 50, r.chance, v -> {
                r.chance = Math.max(0.0, v);
            }, null, 0, 0, CrateEditorScreen.desc("Probabilidad relativa en %. Se normaliza con el resto."));
        }
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Igualar todas"), b -> {
            int n = 0;
            for (RewardEntry rx : this.config.rewards) {
                if (rx.guaranteed) continue;
                ++n;
            }
            if (n > 0) {
                double each = 100.0 / (double)n;
                for (RewardEntry r2 : this.config.rewards) {
                    if (r2.guaranteed) continue;
                    r2.chance = each;
                }
            }
            this.rebuildWidgets();
        }).bounds(this.leftPos + 92, this.topPos + this.panelHeight - 24, 110, 18).build());
    }

    private void initAnimation() {
        this.helpLine = "Elige la animaci\u00f3n del cofre. Ocurre EN el cofre, en el mundo, con tensi\u00f3n antes del premio.";
        int x = this.bodyX();
        int y = this.bodyY();
        int colW = this.bodyW();
        ScrollSelector<CrateAnimation> list = new ScrollSelector<CrateAnimation>(x, y, colW, this.bodyH() - 28, 14, a -> (a.id().equals(this.config.animationId) ? "\u00a7a\u2714 " : "\u00a7f") + a.displayName() + " \u00a78(" + (double)a.durationTicks() / 20.0 + "s)", a -> a.displayName() + " " + a.id(), null);
        list.setItems(AnimationRegistry.all());
        list.onSelect(a -> {
            this.config.animationId = a.id();
            this.rebuildWidgets();
        });
        this.addRenderableWidget(list);
        CrateAnimation sel = AnimationRegistry.get(this.config.animationId);
        this.addLabel("\u00a7e" + sel.displayName() + ": \u00a77" + sel.description(), x, y + this.bodyH() - 22, null);
    }

    private void initStyle() {
        this.helpLine = "Elige el DISE\u00d1O visual del cofre (independiente del tier). Escribe para buscar entre todos los modelos.";
        int x = this.bodyX();
        int y = this.bodyY();
        int colW = this.bodyW();
        List<String> ids = CrateStyles.cycleIds();
        ScrollSelector<String> list = new ScrollSelector<String>(x, y, colW, this.bodyH() - 28, 14, id -> (id.equals(this.config.styleId) ? "\u00a7a\u2714 " : "\u00a7f") + CrateStyles.displayName(id), id -> CrateStyles.displayName(id) + " " + id, null);
        list.setItems(ids);
        list.onSelect(id -> {
            this.config.styleId = id;
            this.rebuildWidgets();
        });
        this.addRenderableWidget(list);
        this.addLabel("\u00a7eDise\u00f1o actual: \u00a7b" + CrateStyles.displayName(this.config.styleId) + " \u00a77(" + ids.size() + " dise\u00f1os)", x, y + this.bodyH() - 22, null);
    }

    private void initAppearance() {
        this.helpLine = "Brillo, part\u00edculas on/off, nombre flotante, color del nombre y texto flotante (color por l\u00ednea).";
        int x = this.bodyX();
        int y = this.bodyY();
        int colW = (this.bodyW() - 10) / 2;
        this.addToggle(x, y, colW, this.config.glow ? "Brillo del item: Activado" : "Brillo del item: Desactivado", this.config.glow, () -> {
            this.config.glow = !this.config.glow;
            this.rebuildWidgets();
        }, CrateEditorScreen.desc("El item de crate brilla como encantado."));
        this.addToggle(x, y + 22, colW, this.config.particles ? "Part\u00edculas: Activado" : "Part\u00edculas: Desactivado", this.config.particles, () -> {
            this.config.particles = !this.config.particles;
            this.rebuildWidgets();
        }, CrateEditorScreen.desc("Part\u00edculas de reposo alrededor de la crate."));
        this.addToggle(x, y + 44, colW, this.config.floatingName ? "Nombre flotante: S\u00ed" : "Nombre flotante: No", this.config.floatingName, () -> {
            this.config.floatingName = !this.config.floatingName;
            this.rebuildWidgets();
        }, CrateEditorScreen.desc("Muestra el nombre flotando sobre la crate."));
        EditBox hex = new EditBox(this.font, x + 70, y + 70, 110, 16, (Component)Component.empty());
        hex.setMaxLength(7);
        hex.setValue(this.config.nameColorHexOverride);
        hex.setHint((Component)Component.literal((String)"#RRGGBB"));
        hex.setResponder(s -> {
            this.config.nameColorHexOverride = s.trim();
        });
        this.addRenderableWidget(hex);
        this.addLabel("Color:", x, y + 74, CrateEditorScreen.desc("Color del nombre (#RRGGBB). Vac\u00edo = color del tier."));
        this.addToggle(x, y + 92, colW, this.config.showOdds ? "Mostrar % encima: S\u00ed" : "Mostrar % encima: No", this.config.showOdds, () -> {
            this.config.showOdds = !this.config.showOdds;
            this.rebuildWidgets();
        }, CrateEditorScreen.desc("Muestra la probabilidad de cada recompensa flotando sobre el cofre.", "\u00datil para que los jugadores vean las posibilidades."));
        int tx = x + colW + 10;
        this.addLabel("\u00a7eTexto flotante (color por l\u00ednea):", tx, y - 2, CrateEditorScreen.desc("El bot\u00f3n \u25a0 cambia el color de ESA l\u00ednea.", "Tambi\u00e9n aceptas c\u00f3digos & dentro del texto."));
        int maxLines = 6;
        char[] lineColors = new char[6];
        String[] lineTexts = new String[6];
        for (int i = 0; i < 6; ++i) {
            String raw = i < this.config.floatingText.size() ? this.config.floatingText.get(i) : "";
            char col = 'f';
            String txt = raw;
            if (raw.length() >= 2 && (raw.charAt(0) == '&' || raw.charAt(0) == '\u00a7') && COLOR_CHARS.indexOf(raw.charAt(1)) >= 0) {
                col = raw.charAt(1);
                txt = raw.substring(2);
            }
            lineColors[i] = col;
            lineTexts[i] = txt;
        }
        Runnable sync = () -> {
            ArrayList<String> out = new ArrayList<String>();
            for (int k = 0; k < 6; ++k) {
                out.add(lineTexts[k].isEmpty() ? "" : "&" + lineColors[k] + lineTexts[k]);
            }
            this.config.setFloatingText(String.join((CharSequence)"\n", out));
        };
        for (int j = 0; j < 6; ++j) {
            int idx = j;
            int ry = y + 12 + j * 21;
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)("\u00a7" + lineColors[j])), b -> {
                int pos = COLOR_CHARS.indexOf(lineColors[idx]);
                lineColors[idx] = COLOR_CHARS.charAt((pos + 1) % COLOR_CHARS.length());
                sync.run();
                this.rebuildWidgets();
            }).bounds(tx, ry, 18, 16).build());
            EditBox line = new EditBox(this.font, tx + 22, ry, colW - 22, 16, (Component)Component.empty());
            line.setMaxLength(96);
            line.setValue(lineTexts[j]);
            line.setHint((Component)Component.literal((String)("L\u00ednea " + (j + 1))));
            line.setResponder(s -> {
                lineTexts[idx] = s;
                sync.run();
            });
            this.addRenderableWidget(line);
        }
    }

    private void initParticles() {
        this.helpLine = "Capas sin l\u00edmite. La forma ESPIRAL/Anillo/Halo se adapta al TAMA\u00d1O del cofre. El espiral \u00e9pico viene por defecto en fase Tensi\u00f3n (editable).";
        int x = this.bodyX();
        int y = this.bodyY();
        int listW = 118;
        int midW = 126;
        int midX = x + 118 + 6;
        int rx = midX + 126 + 8;
        int fw = this.leftPos + this.panelWidth - 8 - rx;
        if (this.selectedLayer != null && !this.config.particleLayers.contains(this.selectedLayer)) {
            this.selectedLayer = null;
        }
        ScrollSelector<ParticleLayer> layers = new ScrollSelector<ParticleLayer>(x, y, 118, this.bodyH() - 20, 22, pl -> (pl == this.selectedLayer ? "\u00a7e\u25b6 " : "") + pl.shortLabel(), ParticleLayer::shortLabel, pl -> ItemStack.EMPTY);
        layers.setItems(new ArrayList<ParticleLayer>(this.config.particleLayers));
        layers.onSelect(pl -> {
            this.selectedLayer = pl;
            this.rebuildWidgets();
        });
        this.addRenderableWidget(layers);
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7a+ Capa"), b -> {
            ParticleLayer newL = new ParticleLayer();
            this.config.particleLayers.add(newL);
            this.selectedLayer = newL;
            this.rebuildWidgets();
        }).bounds(x, y + this.bodyH() - 18, 118, 16).build());
        EditBox search = new EditBox(this.font, midX, y, 126, 16, (Component)Component.empty());
        search.setHint((Component)Component.literal((String)"Buscar part\u00edcula..."));
        this.addRenderableWidget(search);
        ScrollSelector<ResourceLocation> types = new ScrollSelector<ResourceLocation>(midX, y + 20, 126, this.bodyH() - 22, 13, rl -> (this.selectedLayer != null && rl.toString().equals(this.selectedLayer.particleId) ? "\u00a7a\u2714 " : "\u00a7f") + ParticleNames.spanish(rl.getPath()), rl -> ParticleNames.spanish(rl.getPath()) + " " + rl, rl -> ItemStack.EMPTY);
        types.setItems(RegistryLists.particles());
        types.onSelect(rl -> {
            if (this.selectedLayer != null) {
                this.selectedLayer.particleId = rl.toString();
                this.rebuildWidgets();
            }
        });
        search.setResponder(types::setQuery);
        this.addRenderableWidget(types);
        if (this.selectedLayer == null) {
            this.addLabel("\u00a77Selecciona o", rx, y + 4, null);
            this.addLabel("\u00a77crea una capa \u2190", rx, y + 16, null);
        } else {
            ParticleLayer l = this.selectedLayer;
            ParticleLayer layerVar = this.selectedLayer;
            int half = fw / 2;
            int fieldW = 42;
            this.addLabel("\u00a7e" + ParticleNames.spanish(l.particleId.contains(":") ? l.particleId.substring(l.particleId.indexOf(58) + 1) : l.particleId), rx, y, null);
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)("Fase: \u00a7e" + l.phase.label)), b -> {
                l.phase = l.phase.next();
                this.rebuildWidgets();
            }).bounds(rx, y + 12, fw, 16).build());
            this.tooltipZones.add(new TooltipZone(rx, y + 12, fw, 16, CrateEditorScreen.desc("Cuando emite:", "Reposo, Tensi\u00f3n, Apertura, Revelaci\u00f3n, Final.")));
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)("Forma: \u00a7b" + l.shape.label)), b -> {
                l.shape = l.shape.next();
                l.applyShapeDefaults();
                this.rebuildWidgets();
            }).bounds(rx, y + 32, fw, 16).build());
            this.tooltipZones.add(new TooltipZone(rx, y + 32, fw, 16, CrateEditorScreen.desc("Forma/movimiento: Halo, Anillo, Espiral, V\u00f3rtice, Fuente...", "Anillo/Halo/Espiral/V\u00f3rtice ENVUELVEN el cofre seg\u00fan su TAMA\u00d1O", "real (un cofre legendario/m\u00edtico es m\u00e1s grande -> radio mayor).")));
            int r1 = y + 54;
            int r2 = y + 74;
            int r3 = y + 94;
            this.addIntField(rx + 60, r1, 42, l.count, v -> {
                l.count = Math.max(1, v);
            }, "Cant.", rx, r1 + 4, CrateEditorScreen.desc("Part\u00edculas por emisi\u00f3n."));
            this.addDoubleField(rx + half + 56, r1, 42, l.speed, v -> {
                l.speed = Math.max(0.0, v);
            }, "Vel.", rx + half, r1 + 4, CrateEditorScreen.desc("Empuje de las part\u00edculas."));
            this.addDoubleField(rx + 60, r2, 42, l.spread, v -> {
                l.spread = Math.max(0.0, v);
            }, "Disp.", rx, r2 + 4, CrateEditorScreen.desc("Apertura aleatoria."));
            this.addDoubleField(rx + half + 56, r2, 42, l.radius, v -> {
                l.radius = Math.max(0.0, v);
            }, "Radio", rx + half, r2 + 4, CrateEditorScreen.desc("Radio del anillo/halo/\u00f3rbita/espiral. ~0.85 rodea el cofre.", "Se MULTIPLICA por el tama\u00f1o del cofre (legendario/m\u00edtico m\u00e1s grande)."));
            this.addDoubleField(rx + 60, r3, 42, l.yOffset, v -> {
                l.yOffset = v;
            }, "Alt.", rx, r3 + 4, CrateEditorScreen.desc("Altura sobre el bloque (se escala con el tama\u00f1o del cofre).", "~0.2 al ras del suelo, ~1.1 a la altura de la tapa."));
            this.addIntField(rx + half + 56, r3, 42, l.interval, v -> {
                l.interval = Math.max(1, v);
            }, "Int.", rx + half, r3 + 4, CrateEditorScreen.desc("Solo en Reposo: emite cada N ticks (20 = 1s)."));
            int cy = y + 116;
            this.addToggle(rx, cy, fw, l.useRarityColor ? "Color: tier" : "Color: hex", l.useRarityColor, () -> {
                l.useRarityColor = !l.useRarityColor;
                this.rebuildWidgets();
            }, CrateEditorScreen.desc("Solo afecta a 'Polvo de color'. Tier = color de la rareza."));
            cy += 20;
            if (!l.useRarityColor) {
                EditBox hex = new EditBox(this.font, rx + 36, cy, fw - 36, 16, (Component)Component.empty());
                hex.setMaxLength(7);
                hex.setValue(l.colorHex);
                hex.setHint((Component)Component.literal((String)"#RRGGBB"));
                hex.setResponder(s -> {
                    l.colorHex = s.trim();
                });
                this.addRenderableWidget(hex);
                this.addLabel("Hex:", rx, cy + 4, null);
                cy += 20;
            }
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7cQuitar capa"), b -> {
                this.config.particleLayers.remove(l);
                this.selectedLayer = null;
                this.rebuildWidgets();
            }).bounds(rx, cy, fw, 16).build());
        }
    }

    private void initKey() {
        this.helpLine = "Llave UNIVERSAL (Fantastic Key). Aqu\u00ed defines la PROBABILIDAD de cada RAREZA al abrir. Los items de cada rareza se ponen en \u00abPremios\u00bb (bot\u00f3n Rareza de cada item).";
        int x = this.bodyX();
        int y = this.bodyY();
        this.addLabel("\u00a7fSe abre con la \u00a7d\u2726 Fantastic Key \u2726\u00a7f (llave universal, abre TODAS).", x, y, CrateEditorScreen.desc("Una sola llave abre cualquier crate.", "Entrega: /fscrate key give <jugador> [cantidad]"));
        this.addLabel("\u00a7ePROBABILIDAD DE RAREZA \u00a77(peso; se normaliza a 100%)", x, y + 20, CrateEditorScreen.desc("Al abrir, la crate tira UNA rareza seg\u00fan estos pesos,", "y entrega un item del POOL de esa rareza (pesta\u00f1a Premios)."));
        int ly = y + 38;
        for (Rarity r : Rarity.values()) {
            final Rarity rr = r;
            int poolN = this.config.rewardCountForRarity(rr);
            String lbl = "  " + rr.color() + rr.displayName() + " \u00a77(" + CrateEditorScreen.fmt(this.config.rarityChancePercent(rr)) + "%, " + poolN + " item" + (poolN == 1 ? "" : "s") + ")";
            this.addLabel(lbl, x, ly + 4, CrateEditorScreen.desc(poolN == 0 ? "\u00a7cSin items en esta rareza: si sale, cae a otro pool." : "\u00a77" + poolN + " item(s) en el pool de " + rr.displayName() + "."));
            this.addDoubleField(x + 230, ly, 60, this.config.rarityChance(rr), v -> {
                this.config.rarityChances.put(rr, Math.max(0.0, v));
                this.rebuildWidgets();
            }, null, 0, 0, CrateEditorScreen.desc("Peso relativo de la rareza " + rr.displayName() + "."));
            ly += 22;
        }
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a77Igualar rarezas"), b -> {
            double each = 100.0 / (double)Rarity.values().length;
            for (Rarity r2 : Rarity.values()) {
                this.config.rarityChances.put(r2, each);
            }
            this.rebuildWidgets();
        }).bounds(x, ly + 2, 140, 16).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a77Preset 60/25/10/4/1"), b -> {
            this.config.rarityChances.clear();
            this.config.rarityChances.putAll(CrateConfig.defaultRarityChances());
            this.rebuildWidgets();
        }).bounds(x + 148, ly + 2, 170, 16).build());
        this.addToggle(x, ly + 26, 260, this.config.consumeKey ? "Consumir llave al abrir: S\u00ed" : "Consumir llave al abrir: No", this.config.consumeKey, () -> {
            this.config.consumeKey = !this.config.consumeKey;
            this.rebuildWidgets();
        }, CrateEditorScreen.desc("Si est\u00e1 activo, la Fantastic Key se gasta al abrir."));
    }

    private void initSettings() {
        this.helpLine = "Izq: cooldown, anuncio, saltar y permiso. Der: tama\u00f1o, altura, orientaci\u00f3n (barritas) y apertura \u00fanica por jugador.";
        int x = this.bodyX();
        int y = this.bodyY();
        int colW = (this.bodyW() - 12) / 2;
        int fieldW = 56;
        this.addIntField(x + colW - fieldW, y, fieldW, this.config.cooldownSeconds, v -> {
            this.config.cooldownSeconds = Math.max(0, v);
        }, "Cooldown por jugador (seg):", x, y + 4, CrateEditorScreen.desc("Espera individual para reabrir ESTA crate. 0 = sin cooldown."));
        this.addSecondsField(x + colW - fieldW, y + 22, fieldW, this.config.openDelayTicks, v -> {
            this.config.openDelayTicks = Math.max(0, v);
        }, "Retraso de apertura (seg):", x, y + 26, CrateEditorScreen.desc("Espera antifraude. 0 = inmediato."));
        this.addToggle(x, y + 44, colW, this.config.broadcast ? "Anuncio global: Activado" : "Anuncio global: Desactivado", this.config.broadcast, () -> {
            this.config.broadcast = !this.config.broadcast;
            this.rebuildWidgets();
        }, CrateEditorScreen.desc("Anuncia a todo el servidor cuando alguien gana."));
        this.addToggle(x, y + 66, colW, this.config.allowSkip ? "Saltar con SHIFT: Permitido" : "Saltar con SHIFT: Bloqueado", this.config.allowSkip, () -> {
            this.config.allowSkip = !this.config.allowSkip;
            this.rebuildWidgets();
        }, CrateEditorScreen.desc("Permite saltar la animaci\u00f3n abriendo con SHIFT."));
        this.addLabel("Permiso requerido (opcional):", x, y + 92, CrateEditorScreen.desc("Nodo de permiso extra. Vac\u00edo = nada adicional."));
        EditBox perm = new EditBox(this.font, x, y + 104, colW, 16, (Component)Component.empty());
        perm.setMaxLength(64);
        perm.setValue(this.config.requiredPermission);
        perm.setHint((Component)Component.literal((String)"(opcional)"));
        perm.setResponder(s -> {
            this.config.requiredPermission = s.trim();
        });
        this.addRenderableWidget(perm);
        int tx = x + colW + 12;
        this.addLabel("\u00a7e\u2726 Tama\u00f1o y posici\u00f3n de la caja", tx, y, null);
        this.addSlider(tx, y + 12, colW, 0.3, 3.0, this.config.sizeScale, 2, "Tama\u00f1o", "x", v -> {
            this.config.sizeScale = (float)v;
        }, CrateEditorScreen.desc("Escala de la caja (1.00x = tama\u00f1o por defecto).", "Arrastra la barrita. No deforma el modelo."));
        this.addSlider(tx, y + 34, colW, -0.5, 3.0, this.config.yOffset, 2, "Altura", " bloques", v -> {
            this.config.yOffset = (float)v;
        }, CrateEditorScreen.desc("Sube o baja la caja (0.00 = sobre el bloque).", "\u00datil para flotarla o hundirla."));
        this.addSlider(tx, y + 56, colW, 0.0, 360.0, this.config.yawOffset, 0, "Orientaci\u00f3n", "\u00b0", v -> {
            this.config.yawOffset = (float)v;
        }, CrateEditorScreen.desc("Rotaci\u00f3n extra en grados sobre la direcci\u00f3n de colocaci\u00f3n."));
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a77Restablecer tama\u00f1o/posici\u00f3n"), b -> {
            this.config.sizeScale = 1.0f;
            this.config.yOffset = 0.0f;
            this.config.yawOffset = 0.0f;
            this.rebuildWidgets();
        }).bounds(tx, y + 78, colW, 16).build());
        this.addToggle(tx, y + 100, colW, this.config.openOncePerPlayer ? "Apertura \u00fanica por jugador: S\u00ed" : "Apertura \u00fanica por jugador: No", this.config.openOncePerPlayer, () -> {
            this.config.openOncePerPlayer = !this.config.openOncePerPlayer;
            this.rebuildWidgets();
        }, CrateEditorScreen.desc("Si est\u00e1 activo, CADA jugador solo puede abrir", "esta caja UNA vez. Otros jugadores a\u00fan pueden", "abrirla su propia vez. (Se recuerda por caja colocada.)"));
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String cycleItemRarity(String current) {
        if (current != null && !current.isBlank()) {
            Rarity r = Rarity.byName(current);
            return r == Rarity.MYTHIC ? "" : r.next().name();
        }
        return Rarity.COMMON.name();
    }

    private static List<Component> desc(String ... lines) {
        ArrayList<Component> out = new ArrayList<Component>();
        for (String s : lines) {
            out.add((Component)Component.literal((String)s));
        }
        return out;
    }

    private void addLabel(String text, int x, int y, List<Component> tooltip) {
        this.labels.add(new Label(text, x, y, 0xE0E0E0));
        if (tooltip != null) {
            this.tooltipZones.add(new TooltipZone(x, y - 2, Math.max(200, this.font.width(text) + 8), 14, tooltip));
        }
    }

    private void addIntField(int x, int y, int w, int value, IntConsumer setter, String label, int labelX, int labelY, List<Component> tooltip) {
        EditBox box = new EditBox(this.font, x, y, w, 16, (Component)Component.empty());
        box.setMaxLength(10);
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
        if (label != null) {
            this.labels.add(new Label(label, labelX, labelY, 0xE0E0E0));
            if (tooltip != null) {
                this.tooltipZones.add(new TooltipZone(labelX, labelY - 2, x + w - labelX, 14, tooltip));
            }
        }
    }

    private void addDoubleField(int x, int y, int w, double value, DoubleConsumer setter, String label, int labelX, int labelY, List<Component> tooltip) {
        EditBox box = new EditBox(this.font, x, y, w, 16, (Component)Component.empty());
        box.setMaxLength(8);
        box.setValue(CrateEditorScreen.fmt(value));
        box.setResponder(s -> {
            try {
                setter.accept(Double.parseDouble(s.trim()));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        });
        this.addRenderableWidget(box);
        if (label != null) {
            this.labels.add(new Label(label, labelX, labelY, 0xE0E0E0));
        }
        if (tooltip != null) {
            this.tooltipZones.add(new TooltipZone(x, y, w, 16, tooltip));
        }
    }

    private void addSecondsField(int x, int y, int w, int ticks, IntConsumer setterTicks, String label, int labelX, int labelY, List<Component> tooltip) {
        EditBox box = new EditBox(this.font, x, y, w, 16, (Component)Component.empty());
        box.setMaxLength(8);
        box.setValue(Long.toString(Math.round((double)ticks / 20.0)));
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
            this.tooltipZones.add(new TooltipZone(labelX, labelY - 2, x + w - labelX, 14, tooltip));
        }
    }

    private void addSlider(int x, int y, int w, double min, double max, double value, int decimals, String label, String suffix, DoubleConsumer setter, List<Component> tooltip) {
        this.addRenderableWidget(new FSSlider(x, y, w, min, max, value, decimals, label, suffix, setter));
        if (tooltip != null) {
            this.tooltipZones.add(new TooltipZone(x, y, w, 18, tooltip));
        }
    }

    private void addToggle(int x, int y, int w, String text, boolean state, Runnable onToggle, List<Component> tooltip) {
        String prefix = state ? "\u00a7a" : "\u00a77";
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)(prefix + text)), b -> onToggle.run()).bounds(x, y, w, 16).build());
        if (tooltip != null) {
            this.tooltipZones.add(new TooltipZone(x, y, w, 16, tooltip));
        }
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -535291870);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, -14408646);
        g.fill(this.leftPos, this.topPos + this.panelHeight - 1, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -12961206);
        g.fill(this.leftPos + 6, this.topPos + 46, this.leftPos + this.panelWidth - 6, this.topPos + 47, -12961206);
        g.drawString(this.font, "\u00a7d\u2726 \u00a7fFantastic Crates \u00a7d\u2726 \u00a77- " + this.config.rarity.color() + this.config.rarity.displayName(), this.leftPos + 8, this.topPos + 6, 0xFFFFFF, false);
        if (this.helpLine != null && !this.helpLine.isEmpty()) {
            String trimmed = this.font.plainSubstrByWidth("\u00a77" + this.helpLine, this.panelWidth - 16);
            g.drawString(this.font, trimmed, this.leftPos + 8, this.topPos + 50, 10133680, false);
        }
        if (this.activeTab == Tab.PROBABILITY) {
            this.renderProbabilityBars(g);
        }
        super.render(g, mouseX, mouseY, partialTick);
        for (Label l : this.labels) {
            g.drawString(this.font, l.text(), l.x(), l.y(), l.color(), false);
        }
        for (TooltipZone z : this.tooltipZones) {
            if (mouseX < z.x() || mouseX >= z.x() + z.w() || mouseY < z.y() || mouseY >= z.y() + z.h()) continue;
            g.renderComponentTooltip(this.font, z.lines(), mouseX, mouseY);
            break;
        }
    }

    private void renderProbabilityBars(GuiGraphics g) {
        int x = this.bodyX();
        int y = this.bodyY();
        int total = this.config.rewards.size();
        if (total == 0) {
            g.drawString(this.font, "\u00a77No hay recompensas. A\u00f1\u00e1delas en Premios.", x, y, 0x909090, false);
        } else {
            int rowH = 22;
            int maxBar = this.bodyW() - 250;
            int visibleRows = Math.max(1, this.bodyH() / 22);
            int maxScroll = Math.max(0, total - visibleRows);
            int scroll = Math.max(0, Math.min(this.probScroll, maxScroll));
            int end = Math.min(total, scroll + visibleRows);
            for (int i = scroll; i < end; ++i) {
                RewardEntry r = this.config.rewards.get(i);
                int ry = y + (i - scroll) * 22;
                double pct = this.config.normalizedPercent(r);
                int barLen = (int)((double)maxBar * pct / 100.0);
                int color = r.guaranteed ? -11141291 : -13800225;
                String nameStr = this.font.plainSubstrByWidth(r.describe(), 140);
                g.drawString(this.font, nameStr, x, ry + 4, 0xE0E0E0, false);
                int barX = x + 210;
                g.fill(barX, ry + 2, barX + Math.max(2, barLen), ry + 14, color);
                String pctStr = r.guaranteed ? "\u00a7a100% fija" : CrateEditorScreen.fmt(pct);
                g.drawString(this.font, pctStr, barX + Math.max(2, barLen) + 4, ry + 4, 0xFFFFFF, false);
            }
            if (maxScroll > 0) {
                int trackX = this.leftPos + this.panelWidth - 12;
                int trackH = visibleRows * 22;
                g.fill(trackX, y, trackX + 4, y + trackH, 0x60000000);
                int thumbH = Math.max(10, trackH * visibleRows / total);
                int thumbY = y + (trackH - thumbH) * scroll / maxScroll;
                g.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, -8355680);
            }
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (super.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        if (this.activeTab == Tab.PROBABILITY) {
            int visibleRows;
            int rowH = 22;
            int total = this.config.rewards.size();
            int maxScroll = Math.max(0, total - (visibleRows = Math.max(1, this.bodyH() / 22)));
            if (maxScroll > 0) {
                int before = this.probScroll;
                this.probScroll = Math.max(0, Math.min(maxScroll, this.probScroll - (int)Math.signum(delta)));
                if (this.probScroll != before) {
                    this.rebuildWidgets();
                }
                return true;
            }
        }
        return false;
    }

    public boolean isPauseScreen() {
        return false;
    }

    private static enum Tab {
        INFO("Info"),
        REWARDS("Premios"),
        PROBABILITY("Prob."),
        APPEARANCE("Aspecto"),
        STYLE("Dise\u00f1o"),
        PARTICLES("Part."),
        KEY("Rarezas"),
        SETTINGS("Ajustes");

        final String label;

        private Tab(String label) {
            this.label = label;
        }
    }

    record TooltipZone(int x, int y, int w, int h, List<Component> lines) {
    }

    record Label(String text, int x, int y, int color) {
    }

    private static final class FSSlider
    extends AbstractSliderButton {
        private final double min;
        private final double max;
        private final int decimals;
        private final String label;
        private final String suffix;
        private final DoubleConsumer setter;

        FSSlider(int x, int y, int w, double min, double max, double value, int decimals, String label, String suffix, DoubleConsumer setter) {
            super(x, y, w, 18, (Component)Component.empty(), max > min ? (value - min) / (max - min) : 0.0);
            this.min = min;
            this.max = max;
            this.decimals = decimals;
            this.label = label;
            this.suffix = suffix;
            this.setter = setter;
            this.updateMessage();
        }

        private double current() {
            return this.min + this.value * (this.max - this.min);
        }

        protected void updateMessage() {
            this.setMessage((Component)Component.literal((String)(this.label + ": \u00a7a" + String.format(Locale.ROOT, "%." + this.decimals + "f", this.current()) + this.suffix)));
        }

        protected void applyValue() {
            this.setter.accept(this.current());
        }
    }
}

