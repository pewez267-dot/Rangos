// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.client.screen;

import com.fscrates.network.FSNetwork;
import com.fscrates.network.SaveCratePacket;
import java.util.Iterator;
import net.minecraft.client.gui.GuiGraphics;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import com.fscrates.config.ParticleNames;
import java.util.function.Function;
import com.fscrates.animation.CrateAnimation;
import com.fscrates.config.Rarity;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.Objects;
import net.minecraft.world.item.Item;
import com.fscrates.client.widget.ScrollSelector;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
import com.fscrates.client.RegistryLists;
import com.fscrates.animation.AnimationRegistry;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Button;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import com.fscrates.config.ParticleLayer;
import com.fscrates.config.RewardEntry;
import java.util.List;
import com.fscrates.config.CrateConfig;
import net.minecraft.client.gui.screens.Screen;

public class CrateEditorScreen extends Screen
{
    private final CrateConfig config;
    private Tab activeTab;
    private final List<Label> labels;
    private final List<TooltipZone> tooltipZones;
    private String helpLine;
    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;
    private RewardEntry selectedReward;
    private ParticleLayer selectedLayer;
    private static final String COLOR_CHARS = "f7e6cab9d5234180";
    
    public CrateEditorScreen(final CrateConfig config) {
        super((Component)Component.m_237113_("Editor de Crate"));
        this.activeTab = Tab.INFO;
        this.labels = new ArrayList<Label>();
        this.tooltipZones = new ArrayList<TooltipZone>();
        this.helpLine = "";
        this.config = ((config == null) ? new CrateConfig() : config);
    }
    
    protected void m_7856_() {
        this.panelWidth = Math.min(this.f_96543_ - 16, 540);
        this.panelHeight = Math.min(this.f_96544_ - 16, 320);
        this.leftPos = (this.f_96543_ - this.panelWidth) / 2;
        this.topPos = (this.f_96544_ - this.panelHeight) / 2;
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
            case ANIMATION: {
                this.initAnimation();
                break;
            }
            case APPEARANCE: {
                this.initAppearance();
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
                break;
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
        final Tab[] tabs = Tab.values();
        final int gap = 2;
        final int tabW = (this.panelWidth - 16 - gap * (tabs.length - 1)) / tabs.length;
        int x = this.leftPos + 8;
        final int y = this.topPos + 24;
        final Tab[] array = tabs;
        for (int length = array.length, i = 0; i < length; ++i) {
            final Tab tab = array[i];
            final boolean active = tab == this.activeTab;
            final String text = (active ? "§f§l" : "§7") + tab.label;
            this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_(text), b -> {
                this.activeTab = tab;
                this.m_232761_();
            }).m_252987_(x, y, tabW, 18).m_253136_());
            x += tabW + gap;
        }
    }
    
    private void initFooter() {
        final int w = 150;
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("§aGuardar y Obtener"), b -> {
            FSNetwork.sendToServer(new SaveCratePacket(this.config.save()));
            this.m_7379_();
        }).m_252987_(this.leftPos + this.panelWidth - w - 8, this.topPos + this.panelHeight - 24, w, 18).m_253136_());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("Cerrar"), b -> this.m_7379_()).m_252987_(this.leftPos + 8, this.topPos + this.panelHeight - 24, 80, 18).m_253136_());
    }
    
    private void initInfo() {
        this.helpLine = "Datos basicos: ID, nombre, tier y tiradas por apertura.";
        final int x = this.bodyX();
        final int y = this.bodyY();
        final EditBox id = new EditBox(this.f_96547_, x + 170, y, 200, 16, (Component)Component.m_237119_());
        id.m_94199_(48);
        id.m_94144_(this.config.id);
        id.m_94151_(s -> this.config.id = s.trim().toLowerCase().replace(' ', '_'));
        this.m_142416_((GuiEventListener)id);
        this.addLabel("ID de la crate:", x, y + 4, desc("Identificador unico (sin espacios).", "Se usa en /fscrate give, edit, delete.", "Ej: cofre_legendario"));
        final EditBox name = new EditBox(this.f_96547_, x + 170, y + 24, 200, 16, (Component)Component.m_237119_());
        name.m_94199_(128);
        name.m_94144_(this.config.displayName);
        name.m_94151_(s -> this.config.displayName = s);
        this.m_142416_((GuiEventListener)name);
        this.addLabel("Nombre visible:", x, y + 28, desc("Nombre del item y del holograma. Acepta codigos & o §."));
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("Tier: " + String.valueOf(this.config.rarity.color()) + this.config.rarity.displayName()), b -> {
            this.config.rarity = this.config.rarity.next();
            this.m_232761_();
        }).m_252987_(x + 170, y + 48, 200, 16).m_253136_());
        this.addLabel("Tier (rareza):", x, y + 52, desc("Define color, sonidos por rareza y QUE LLAVE lo abre.", "Una crate de tier X se abre con la llave de tier X."));
        this.addIntField(x + 170, y + 72, 60, this.config.rolls, v -> this.config.rolls = Math.max(1, v), "Tiradas por apertura:", x, y + 76, desc("Cuantas recompensas (por probabilidad) se entregan.", "Las garantizadas se suman aparte."));
        this.addLabel("§8Animacion: §f" + AnimationRegistry.get(this.config.animationId).displayName(), x, y + 100, (List<Component>)null);
        this.addLabel("§8Recompensas: §f" + this.config.rewards.size() + "  §8Capas de particulas: §f" + this.config.particleLayers.size(), x, y + 112, (List<Component>)null);
    }
    
    private void initRewards() {
        this.helpLine = "Izquierda: busca y clic en un item. Derecha: lista (scroll) y editor de la seleccionada.";
        final int x = this.bodyX();
        final int y = this.bodyY();
        final int colW = (this.bodyW() - 8) / 2;
        final int rightX = x + colW + 8;
        final EditBox search = new EditBox(this.f_96547_, x, y, colW, 16, (Component)Component.m_237119_());
        search.m_257771_((Component)Component.m_237113_("Buscar item..."));
        this.m_142416_((GuiEventListener)search);
        final ScrollSelector<Item> items = new ScrollSelector<Item>(x, y + 20, colW, this.bodyH() - 22, 18, RegistryLists::itemName, it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it), it -> new ItemStack((ItemLike)it));
        items.setItems(RegistryLists.items());
        items.onSelect(it -> {
            final RewardEntry r2 = new RewardEntry(RewardEntry.Type.ITEM);
            r2.item = new ItemStack((ItemLike)it);
            r2.label = RegistryLists.itemName(it);
            r2.chance = 10.0;
            this.config.rewards.add(r2);
            this.selectedReward = r2;
            this.m_232761_();
            return;
        });
        final EditBox editBox = search;
        final ScrollSelector<Item> obj = items;
        Objects.requireNonNull(obj);
        editBox.m_94151_((Consumer)obj::setQuery);
        this.m_142416_((GuiEventListener)items);
        RewardEntry r = null;
        final ScrollSelector<RewardEntry> current = new ScrollSelector<RewardEntry>(rightX, y, colW, this.bodyH() - 98, 16, r -> ((r == this.selectedReward) ? "§e\u25b6 " : "§f") + r.describe() + " §7(" + fmt(this.config.normalizedPercent(r)) + "%)", RewardEntry::describe, r -> (r.type == RewardEntry.Type.ITEM) ? r.item : ItemStack.f_41583_);
        current.setItems(new ArrayList<RewardEntry>(this.config.rewards));
        current.onSelect(r -> {
            this.selectedReward = r;
            this.m_232761_();
            return;
        });
        this.m_142416_((GuiEventListener)current);
        final int addY = y + this.bodyH() - 94;
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("+ Comando"), b -> {
            this.config.rewards.add(new RewardEntry(RewardEntry.Type.COMMAND));
            this.m_232761_();
        }).m_252987_(rightX, addY, colW / 4 - 2, 16).m_253136_());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("+ XP"), b -> {
            final RewardEntry r = new RewardEntry(RewardEntry.Type.XP);
            r.xp = 100;
            this.config.rewards.add(r);
            this.m_232761_();
        }).m_252987_(rightX + colW / 4, addY, colW / 4 - 2, 16).m_253136_());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("+ Efecto"), b -> {
            this.config.rewards.add(new RewardEntry(RewardEntry.Type.EFFECT));
            this.m_232761_();
        }).m_252987_(rightX + 2 * colW / 4, addY, colW / 4 - 2, 16).m_253136_());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("+ Llave"), b -> {
            final RewardEntry r = new RewardEntry(RewardEntry.Type.KEY);
            r.keyRarity = this.config.rarity.name();
            this.config.rewards.add(r);
            this.m_232761_();
        }).m_252987_(rightX + 3 * colW / 4, addY, colW / 4 - 2, 16).m_253136_());
        if (this.selectedReward != null && this.config.rewards.contains(this.selectedReward)) {
            r = this.selectedReward;
            final int fy = y + this.bodyH() - 72;
            this.addDoubleField(rightX + 70, fy, 50, r.chance, v -> r.chance = Math.max(0.0, v), "Prob. (%)", rightX, fy + 4, desc("Probabilidad en %. Se normaliza con las demas para sumar 100%."));
            this.addIntField(rightX + 150, fy, 36, r.minAmount, v -> r.minAmount = Math.max(1, v), "Min", rightX + 122, fy + 4, desc("Cantidad minima entregada."));
            this.addIntField(rightX + 235, fy, 36, r.maxAmount, v -> r.maxAmount = Math.max(1, v), "Max", rightX + 200, fy + 4, desc("Cantidad maxima entregada."));
            this.addToggle(rightX, fy + 22, colW - 70, r.guaranteed ? "Garantizada: Si" : "Garantizada: No", r.guaranteed, () -> {
                r.guaranteed = !r.guaranteed;
                this.m_232761_();
                return;
            }, desc("Si esta activo, SIEMPRE se entrega (100%)."));
            this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("§cQuitar"), b -> {
                this.config.rewards.remove(r);
                this.selectedReward = null;
                this.m_232761_();
            }).m_252987_(rightX + colW - 64, fy + 22, 64, 16).m_253136_());
            if (r.type == RewardEntry.Type.COMMAND) {
                final EditBox cmd = new EditBox(this.f_96547_, rightX, fy + 44, colW, 16, (Component)Component.m_237119_());
                cmd.m_94199_(256);
                cmd.m_94144_(r.command);
                cmd.m_94151_(s -> r.command = s);
                cmd.m_257771_((Component)Component.m_237113_("/give {player} ..."));
                this.m_142416_((GuiEventListener)cmd);
            }
            else if (r.type == RewardEntry.Type.XP) {
                this.addIntField(rightX + 40, fy + 44, 80, r.xp, v -> r.xp = Math.max(0, v), "XP", rightX, fy + 48, desc("Puntos de experiencia entregados."));
            }
            else if (r.type == RewardEntry.Type.KEY) {
                this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("Tier llave: " + String.valueOf(Rarity.byName(r.keyRarity).color()) + Rarity.byName(r.keyRarity).displayName()), b -> {
                    r.keyRarity = Rarity.byName(r.keyRarity).next().name();
                    this.m_232761_();
                }).m_252987_(rightX, fy + 44, colW, 16).m_253136_());
            }
            else if (r.type == RewardEntry.Type.ITEM) {
                this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("§b\u270e Editar NBT del item"), b -> {
                    if (r.item != null && !r.item.m_41619_()) {
                        this.f_96541_.m_91152_((Screen)new NbtEditorScreen(this, r.item));
                    }
                }).m_252987_(rightX, fy + 44, colW, 16).m_253136_());
                this.tooltipZones.add(new TooltipZone(rightX, fy + 44, colW, 16, desc("Abre el editor de NBT: nombre, lore con color,", "encantamientos, atributos, irrompible, CustomModelData...", "Todo manual, sin pegar comandos.")));
            }
        }
    }
    
    private void initProbability() {
        this.helpLine = "Escribe la probabilidad de cada recompensa en %. Se normaliza a 100% automaticamente.";
        final int x = this.bodyX();
        final int y = this.bodyY();
        for (int rows = Math.min(this.config.rewards.size(), Math.max(1, this.bodyH() / 22)), i = 0; i < rows; ++i) {
            final RewardEntry r = this.config.rewards.get(i);
            final int ry = y + i * 22;
            if (!r.guaranteed) {
                this.addDoubleField(x + 150, ry, 50, r.chance, v -> r.chance = Math.max(0.0, v), null, 0, 0, desc("Probabilidad relativa en %. Se normaliza con el resto."));
            }
        }
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("Igualar todas"), b -> {
            int n = 0;
            for (final RewardEntry r : this.config.rewards) {
                if (!r.guaranteed) {
                    ++n;
                }
            }
            if (n > 0) {
                final double each = 100.0 / n;
                for (final RewardEntry r2 : this.config.rewards) {
                    if (!r2.guaranteed) {
                        r2.chance = each;
                    }
                }
            }
            this.m_232761_();
        }).m_252987_(x, this.topPos + this.panelHeight - 24, 110, 18).m_253136_());
    }
    
    private void initAnimation() {
        this.helpLine = "Elige la animacion del cofre. Ocurre EN el cofre, en el mundo, con tension antes del premio.";
        final int x = this.bodyX();
        final int y = this.bodyY();
        final int colW = this.bodyW();
        final ScrollSelector<CrateAnimation> list = new ScrollSelector<CrateAnimation>(x, y, colW, this.bodyH() - 28, 14, a -> (a.id().equals(this.config.animationId) ? "§a\u2714 " : "§f") + a.displayName() + " §8(" + a.durationTicks() / 20.0 + "s)", a -> a.displayName() + " " + a.id(), (Function<CrateAnimation, ItemStack>)null);
        list.setItems(AnimationRegistry.all());
        list.onSelect(a -> {
            this.config.animationId = a.id();
            this.m_232761_();
            return;
        });
        this.m_142416_((GuiEventListener)list);
        final CrateAnimation sel = AnimationRegistry.get(this.config.animationId);
        this.addLabel("§e" + sel.displayName() + ": §7" + sel.description(), x, y + this.bodyH() - 22, (List<Component>)null);
    }
    
    private void initAppearance() {
        this.helpLine = "Brillo, particulas on/off, nombre flotante, color del nombre y texto flotante (color por linea).";
        final int x = this.bodyX();
        final int y = this.bodyY();
        final int colW = (this.bodyW() - 10) / 2;
        this.addToggle(x, y, colW, this.config.glow ? "Brillo del item: Activado" : "Brillo del item: Desactivado", this.config.glow, () -> {
            this.config.glow = !this.config.glow;
            this.m_232761_();
            return;
        }, desc("El item de crate brilla como encantado."));
        this.addToggle(x, y + 22, colW, this.config.particles ? "Particulas: Activado" : "Particulas: Desactivado", this.config.particles, () -> {
            this.config.particles = !this.config.particles;
            this.m_232761_();
            return;
        }, desc("Particulas de reposo alrededor de la crate."));
        this.addToggle(x, y + 44, colW, this.config.floatingName ? "Nombre flotante: Si" : "Nombre flotante: No", this.config.floatingName, () -> {
            this.config.floatingName = !this.config.floatingName;
            this.m_232761_();
            return;
        }, desc("Muestra el nombre flotando sobre la crate."));
        final EditBox hex = new EditBox(this.f_96547_, x + 70, y + 70, 110, 16, (Component)Component.m_237119_());
        hex.m_94199_(7);
        hex.m_94144_(this.config.nameColorHexOverride);
        hex.m_257771_((Component)Component.m_237113_("#RRGGBB"));
        hex.m_94151_(s -> this.config.nameColorHexOverride = s.trim());
        this.m_142416_((GuiEventListener)hex);
        this.addLabel("Color:", x, y + 74, desc("Color del nombre (#RRGGBB). Vacio = color del tier."));
        this.addToggle(x, y + 92, colW, this.config.showOdds ? "Mostrar % encima: Si" : "Mostrar % encima: No", this.config.showOdds, () -> {
            this.config.showOdds = !this.config.showOdds;
            this.m_232761_();
            return;
        }, desc("Muestra la probabilidad de cada recompensa flotando sobre el cofre.", "Util para que los jugadores vean las posibilidades."));
        final int tx = x + colW + 10;
        this.addLabel("§eTexto flotante (color por linea):", tx, y - 2, desc("El boton \u25a0 cambia el color de ESA linea.", "Tambien aceptas codigos & dentro del texto."));
        final int maxLines = 6;
        final char[] lineColors = new char[6];
        final String[] lineTexts = new String[6];
        for (int i = 0; i < 6; ++i) {
            final String raw = (i < this.config.floatingText.size()) ? this.config.floatingText.get(i) : "";
            char col = 'f';
            String txt = raw;
            if (raw.length() >= 2 && (raw.charAt(0) == '&' || raw.charAt(0) == '§') && "f7e6cab9d5234180".indexOf(raw.charAt(1)) >= 0) {
                col = raw.charAt(1);
                txt = raw.substring(2);
            }
            lineColors[i] = col;
            lineTexts[i] = txt;
        }
        final Runnable sync = () -> {
            final ArrayList<String> out = new ArrayList<String>();
            for (int k = 0; k < 6; ++k) {
                out.add(lineTexts[k].isEmpty() ? "" : ("&" + lineColors[k] + lineTexts[k]));
            }
            this.config.setFloatingText(String.join("\n", out));
            return;
        };
        for (int j = 0; j < 6; ++j) {
            final int idx = j;
            final int ry = y + 12 + j * 21;
            this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("§" + lineColors[j]), b -> {
                final int pos = "f7e6cab9d5234180".indexOf(lineColors[idx]);
                lineColors[idx] = "f7e6cab9d5234180".charAt((pos + 1) % "f7e6cab9d5234180".length());
                sync.run();
                this.m_232761_();
            }).m_252987_(tx, ry, 18, 16).m_253136_());
            final EditBox line = new EditBox(this.f_96547_, tx + 22, ry, colW - 22, 16, (Component)Component.m_237119_());
            line.m_94199_(96);
            line.m_94144_(lineTexts[j]);
            line.m_257771_((Component)Component.m_237113_("Linea " + (j + 1)));
            line.m_94151_(s -> {
                lineTexts[idx] = s;
                sync.run();
                return;
            });
            this.m_142416_((GuiEventListener)line);
        }
    }
    
    private void initParticles() {
        this.helpLine = "Capas sin limite. Izq: tus capas (scroll). Centro: tipo (scroll, busca). Der: ajustes de la capa.";
        final int x = this.bodyX();
        final int y = this.bodyY();
        final int listW = 118;
        final int midW = 126;
        final int midX = x + listW + 6;
        final int rx = midX + midW + 8;
        final int fw = this.leftPos + this.panelWidth - 8 - rx;
        if (this.selectedLayer != null && !this.config.particleLayers.contains(this.selectedLayer)) {
            this.selectedLayer = null;
        }
        final ParticleLayer l;
        final ScrollSelector<ParticleLayer> layers = new ScrollSelector<ParticleLayer>(x, y, listW, this.bodyH() - 20, 22, l -> ((l == this.selectedLayer) ? "§e\u25b6 " : "") + l.shortLabel(), ParticleLayer::shortLabel, l -> ItemStack.f_41583_);
        layers.setItems(new ArrayList<ParticleLayer>(this.config.particleLayers));
        layers.onSelect(l -> {
            this.selectedLayer = l;
            this.m_232761_();
            return;
        });
        this.m_142416_((GuiEventListener)layers);
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("§a+ Capa"), b -> {
            final ParticleLayer l = new ParticleLayer();
            this.config.particleLayers.add(l);
            this.selectedLayer = l;
            this.m_232761_();
        }).m_252987_(x, y + this.bodyH() - 18, listW, 16).m_253136_());
        final EditBox search = new EditBox(this.f_96547_, midX, y, midW, 16, (Component)Component.m_237119_());
        search.m_257771_((Component)Component.m_237113_("Buscar particula..."));
        this.m_142416_((GuiEventListener)search);
        final ScrollSelector<ResourceLocation> types = new ScrollSelector<ResourceLocation>(midX, y + 20, midW, this.bodyH() - 22, 13, rl -> ((this.selectedLayer != null && rl.toString().equals(this.selectedLayer.particleId)) ? "§a\u2714 " : "§f") + ParticleNames.spanish(rl.m_135815_()), rl -> ParticleNames.spanish(rl.m_135815_()) + " " + String.valueOf(rl), rl -> ItemStack.f_41583_);
        types.setItems(RegistryLists.particles());
        types.onSelect(rl -> {
            if (this.selectedLayer != null) {
                this.selectedLayer.particleId = rl.toString();
                this.m_232761_();
            }
            return;
        });
        final EditBox editBox = search;
        final ScrollSelector<ResourceLocation> obj = types;
        Objects.requireNonNull(obj);
        editBox.m_94151_((Consumer)obj::setQuery);
        this.m_142416_((GuiEventListener)types);
        if (this.selectedLayer == null) {
            this.addLabel("§7Selecciona o", rx, y + 4, null);
            this.addLabel("§7crea una capa \u2190", rx, y + 16, null);
            return;
        }
        l = this.selectedLayer;
        final int half = fw / 2;
        final int fieldW = 42;
        this.addLabel("§e" + ParticleNames.spanish(l.particleId.contains(":") ? l.particleId.substring(l.particleId.indexOf(58) + 1) : l.particleId), rx, y, (List<Component>)null);
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("Fase: §e" + l.phase.label), b -> {
            l.phase = l.phase.next();
            this.m_232761_();
        }).m_252987_(rx, y + 12, fw, 16).m_253136_());
        this.tooltipZones.add(new TooltipZone(rx, y + 12, fw, 16, desc("Cuando emite:", "Reposo, Tension, Apertura, Revelacion, Final.")));
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("Forma: §b" + l.shape.label), b -> {
            l.shape = l.shape.next();
            l.applyShapeDefaults();
            this.m_232761_();
        }).m_252987_(rx, y + 32, fw, 16).m_253136_());
        this.tooltipZones.add(new TooltipZone(rx, y + 32, fw, 16, desc("Forma/movimiento. Al cambiarla se reajustan radio/altura", "para que quede bien (ej. el anillo rodea el cofre por fuera).")));
        final int r1 = y + 54;
        final int r2 = y + 74;
        final int r3 = y + 94;
        this.addIntField(rx + 60, r1, fieldW, l.count, v -> l.count = Math.max(1, v), "Cant.", rx, r1 + 4, desc("Particulas por emision."));
        this.addDoubleField(rx + half + 56, r1, fieldW, l.speed, v -> l.speed = Math.max(0.0, v), "Vel.", rx + half, r1 + 4, desc("Empuje de las particulas."));
        this.addDoubleField(rx + 60, r2, fieldW, l.spread, v -> l.spread = Math.max(0.0, v), "Disp.", rx, r2 + 4, desc("Apertura aleatoria."));
        this.addDoubleField(rx + half + 56, r2, fieldW, l.radius, v -> l.radius = Math.max(0.0, v), "Radio", rx + half, r2 + 4, desc("Radio del anillo/halo/orbita. ~0.95 rodea el cofre por fuera."));
        this.addDoubleField(rx + 60, r3, fieldW, l.yOffset, v -> l.yOffset = v, "Alt.", rx, r3 + 4, desc("Altura sobre el bloque. ~0.45 para anillo al ras del suelo."));
        this.addIntField(rx + half + 56, r3, fieldW, l.interval, v -> l.interval = Math.max(1, v), "Int.", rx + half, r3 + 4, desc("Solo en Reposo: emite cada N ticks (20 = 1s)."));
        int cy = y + 116;
        this.addToggle(rx, cy, fw, l.useRarityColor ? "Color: tier" : "Color: hex", l.useRarityColor, () -> {
            l.useRarityColor = !l.useRarityColor;
            this.m_232761_();
            return;
        }, desc("Solo afecta a 'Polvo de color'. Tier = color de la rareza."));
        cy += 20;
        if (!l.useRarityColor) {
            final EditBox hex = new EditBox(this.f_96547_, rx + 36, cy, fw - 36, 16, (Component)Component.m_237119_());
            hex.m_94199_(7);
            hex.m_94144_(l.colorHex);
            hex.m_257771_((Component)Component.m_237113_("#RRGGBB"));
            hex.m_94151_(s -> l.colorHex = s.trim());
            this.m_142416_((GuiEventListener)hex);
            this.addLabel("Hex:", rx, cy + 4, null);
            cy += 20;
        }
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("§cQuitar capa"), b -> {
            this.config.particleLayers.remove(l);
            this.selectedLayer = null;
            this.m_232761_();
        }).m_252987_(rx, cy, fw, 16).m_253136_());
    }
    
    private void initKey() {
        this.helpLine = "Las llaves son por TIER (5: Comun, Rara, Epica, Legendaria, Mitica). No se ligan a una crate.";
        final int x = this.bodyX();
        final int y = this.bodyY();
        this.addLabel("§fEsta crate se abre con: " + String.valueOf(this.config.rarity.color()) + "Llave " + this.config.rarity.displayName(), x, y, desc("Cualquier llave de este tier abre esta crate.", "Entrega: /fscrate key give <jugador> " + this.config.rarity.id()));
        this.addLabel("§7Las 5 llaves de tier:", x, y + 22, null);
        int ly = y + 36;
        final Rarity[] values = Rarity.values();
        for (int length = values.length, i = 0; i < length; ++i) {
            final Rarity r = values[i];
            this.addLabel("  " + String.valueOf(r.color()) + "\u2726 Llave " + r.displayName() + " §8(/fscrate key give <jugador> " + r.id(), x, ly, (List<Component>)null);
            ly += 12;
        }
        this.addToggle(x, ly + 6, 260, this.config.consumeKey ? "Consumir llave al abrir: Si" : "Consumir llave al abrir: No", this.config.consumeKey, () -> {
            this.config.consumeKey = !this.config.consumeKey;
            this.m_232761_();
        }, desc("Si esta activo, la llave se gasta al abrir."));
    }
    
    private void initSettings() {
        this.helpLine = "Cooldown por jugador, anuncio global, saltar animacion y permisos.";
        final int x = this.bodyX();
        final int y = this.bodyY();
        this.addIntField(x + 240, y, 60, this.config.cooldownSeconds, v -> this.config.cooldownSeconds = Math.max(0, v), "Cooldown por jugador (seg):", x, y + 4, desc("Espera individual para reabrir ESTA crate. 0 = sin cooldown."));
        this.addSecondsField(x + 240, y + 22, 60, this.config.openDelayTicks, v -> this.config.openDelayTicks = Math.max(0, v), "Retraso de apertura (seg):", x, y + 26, desc("Espera antifraude. 0 = inmediato."));
        this.addToggle(x, y + 48, 280, this.config.broadcast ? "Anuncio global: Activado" : "Anuncio global: Desactivado", this.config.broadcast, () -> {
            this.config.broadcast = !this.config.broadcast;
            this.m_232761_();
            return;
        }, desc("Anuncia a todo el servidor cuando alguien gana."));
        this.addToggle(x, y + 70, 280, this.config.allowSkip ? "Saltar con SHIFT: Permitido" : "Saltar con SHIFT: Bloqueado", this.config.allowSkip, () -> {
            this.config.allowSkip = !this.config.allowSkip;
            this.m_232761_();
            return;
        }, desc("Permite saltar la animacion abriendo con SHIFT."));
        final EditBox perm = new EditBox(this.f_96547_, x + 240, y + 96, 200, 16, (Component)Component.m_237119_());
        perm.m_94199_(64);
        perm.m_94144_(this.config.requiredPermission);
        perm.m_257771_((Component)Component.m_237113_("(opcional)"));
        perm.m_94151_(s -> this.config.requiredPermission = s.trim());
        this.m_142416_((GuiEventListener)perm);
        this.addLabel("Permiso requerido (opcional):", x, y + 100, desc("Nodo de permiso extra. Vacio = nada adicional."));
    }
    
    private static String fmt(final double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }
    
    private static List<Component> desc(final String... lines) {
        final List<Component> out = new ArrayList<Component>();
        for (final String s : lines) {
            out.add((Component)Component.m_237113_(s));
        }
        return out;
    }
    
    private void addLabel(final String text, final int x, final int y, final List<Component> tooltip) {
        this.labels.add(new Label(text, x, y, 14737632));
        if (tooltip != null) {
            this.tooltipZones.add(new TooltipZone(x, y - 2, Math.max(200, this.f_96547_.m_92895_(text) + 8), 14, tooltip));
        }
    }
    
    private void addIntField(final int x, final int y, final int w, final int value, final IntConsumer setter, final String label, final int labelX, final int labelY, final List<Component> tooltip) {
        final EditBox box = new EditBox(this.f_96547_, x, y, w, 16, (Component)Component.m_237119_());
        box.m_94199_(10);
        box.m_94144_(Integer.toString(value));
        box.m_94151_(s -> {
            try {
                setter.accept(Integer.parseInt(s.trim()));
            }
            catch (final NumberFormatException ex) {}
            return;
        });
        this.m_142416_((GuiEventListener)box);
        if (label != null) {
            this.labels.add(new Label(label, labelX, labelY, 14737632));
            if (tooltip != null) {
                this.tooltipZones.add(new TooltipZone(labelX, labelY - 2, x + w - labelX, 14, tooltip));
            }
        }
    }
    
    private void addDoubleField(final int x, final int y, final int w, final double value, final DoubleConsumer setter, final String label, final int labelX, final int labelY, final List<Component> tooltip) {
        final EditBox box = new EditBox(this.f_96547_, x, y, w, 16, (Component)Component.m_237119_());
        box.m_94199_(8);
        box.m_94144_(fmt(value));
        box.m_94151_(s -> {
            try {
                setter.accept(Double.parseDouble(s.trim()));
            }
            catch (final NumberFormatException ex) {}
            return;
        });
        this.m_142416_((GuiEventListener)box);
        if (label != null) {
            this.labels.add(new Label(label, labelX, labelY, 14737632));
        }
        if (tooltip != null) {
            this.tooltipZones.add(new TooltipZone(x, y, w, 16, tooltip));
        }
    }
    
    private void addSecondsField(final int x, final int y, final int w, final int ticks, final IntConsumer setterTicks, final String label, final int labelX, final int labelY, final List<Component> tooltip) {
        final EditBox box = new EditBox(this.f_96547_, x, y, w, 16, (Component)Component.m_237119_());
        box.m_94199_(8);
        box.m_94144_(Long.toString(Math.round(ticks / 20.0)));
        box.m_94151_(s -> {
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
        this.m_142416_((GuiEventListener)box);
        this.labels.add(new Label(label, labelX, labelY, 14737632));
        if (tooltip != null) {
            this.tooltipZones.add(new TooltipZone(labelX, labelY - 2, x + w - labelX, 14, tooltip));
        }
    }
    
    private void addToggle(final int x, final int y, final int w, final String text, final boolean state, final Runnable onToggle, final List<Component> tooltip) {
        final String prefix = state ? "§a" : "§7";
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_(prefix + text), b -> onToggle.run()).m_252987_(x, y, w, 16).m_253136_());
        if (tooltip != null) {
            this.tooltipZones.add(new TooltipZone(x, y, w, 16, tooltip));
        }
    }
    
    public void m_88315_(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        this.m_280273_(g);
        g.m_280509_(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -535291870);
        g.m_280509_(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, -14408646);
        g.m_280509_(this.leftPos, this.topPos + this.panelHeight - 1, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -12961206);
        g.m_280509_(this.leftPos + 6, this.topPos + 46, this.leftPos + this.panelWidth - 6, this.topPos + 47, -12961206);
        g.m_280056_(this.f_96547_, "§d\u2726 §fFantastic Crates §d\u2726 §7- " + String.valueOf(this.config.rarity.color()) + this.config.rarity.displayName(), this.leftPos + 8, this.topPos + 6, 16777215, false);
        if (this.helpLine != null && !this.helpLine.isEmpty()) {
            final String trimmed = this.f_96547_.m_92834_("§7" + this.helpLine, this.panelWidth - 16);
            g.m_280056_(this.f_96547_, trimmed, this.leftPos + 8, this.topPos + 50, 10133680, false);
        }
        if (this.activeTab == Tab.PROBABILITY) {
            this.renderProbabilityBars(g);
        }
        super.m_88315_(g, mouseX, mouseY, partialTick);
        for (final Label l : this.labels) {
            g.m_280056_(this.f_96547_, l.text(), l.x(), l.y(), l.color(), false);
        }
        for (final TooltipZone z : this.tooltipZones) {
            if (mouseX >= z.x() && mouseX < z.x() + z.w() && mouseY >= z.y() && mouseY < z.y() + z.h()) {
                g.m_280666_(this.f_96547_, (List)z.lines(), mouseX, mouseY);
                break;
            }
        }
    }
    
    private void renderProbabilityBars(final GuiGraphics g) {
        final int x = this.bodyX();
        final int y = this.bodyY();
        final int maxBar = this.bodyW() - 230;
        final int rows = Math.max(1, this.bodyH() / 22);
        final int shown = Math.min(this.config.rewards.size(), rows);
        for (int i = 0; i < shown; ++i) {
            final RewardEntry r = this.config.rewards.get(i);
            final int ry = y + i * 22;
            final double pct = this.config.normalizedPercent(r);
            final int barLen = (int)(maxBar * pct / 100.0);
            final int color = r.guaranteed ? -11141291 : -13800225;
            final String nameStr = this.f_96547_.m_92834_(r.describe(), 140);
            g.m_280056_(this.f_96547_, nameStr, x, ry + 4, 14737632, false);
            final int barX = x + 210;
            g.m_280509_(barX, ry + 2, barX + Math.max(2, barLen), ry + 14, color);
            final String pctStr = r.guaranteed ? "§a100% fija" : fmt(pct);
            g.m_280056_(this.f_96547_, pctStr, barX + Math.max(2, barLen) + 4, ry + 4, 16777215, false);
        }
        if (this.config.rewards.size() > shown) {
            g.m_280056_(this.f_96547_, "§7... y " + (this.config.rewards.size() - shown) + " mas (usa la pestana Premios)", x, y + shown * 22, 9474192, false);
        }
        if (this.config.rewards.isEmpty()) {
            g.m_280056_(this.f_96547_, "§7No hay recompensas. Anadelas en Premios.", x, y, 9474192, false);
        }
    }
    
    public boolean m_7043_() {
        return false;
    }
    
    private enum Tab
    {
        INFO("Info"), 
        REWARDS("Premios"), 
        PROBABILITY("Prob."), 
        ANIMATION("Anim."), 
        APPEARANCE("Aspecto"), 
        PARTICLES("Part."), 
        KEY("Llave"), 
        SETTINGS("Ajustes");
        
        final String label;
        
        private Tab(final String label) {
            this.label = label;
        }
    }
    
    record Label(String text, int x, int y, int color) {}
    
    record TooltipZone(int x, int y, int w, int h, List<Component> lines) {}
}
