// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.client.screen;

import java.util.Locale;
import net.minecraft.world.entity.ai.attributes.Attribute;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.Objects;
import com.fscrates.client.widget.ScrollSelector;
import java.util.function.Function;
import java.util.Comparator;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.Iterator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Button;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.Screen;

public class NbtEditorScreen extends Screen
{
    private static final String COLOR_CHARS = "f7e6cab9d5234180";
    private static final String[] OPS;
    private static final String[] SLOTS;
    private final Screen parent;
    private final ItemStack stack;
    private Tab activeTab;
    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;
    private final List<EnchEntry> enchEntries;
    private boolean enchLoaded;
    private final List<AttrEntry> attrEntries;
    private boolean attrLoaded;
    
    public NbtEditorScreen(final Screen parent, final ItemStack stack) {
        super((Component)Component.m_237113_("Editor de NBT"));
        this.activeTab = Tab.GENERAL;
        this.enchEntries = new ArrayList<EnchEntry>();
        this.enchLoaded = false;
        this.attrEntries = new ArrayList<AttrEntry>();
        this.attrLoaded = false;
        this.parent = parent;
        this.stack = stack;
    }
    
    protected void m_7856_() {
        this.panelWidth = Math.min(this.f_96543_ - 20, 540);
        this.panelHeight = Math.min(this.f_96544_ - 20, 320);
        this.leftPos = (this.f_96543_ - this.panelWidth) / 2;
        this.topPos = (this.f_96544_ - this.panelHeight) / 2;
        final Tab[] tabs = Tab.values();
        final String[] names = { "General", "Lore", "Encantamientos", "Atributos" };
        final int gap = 4;
        final int tabW = (this.panelWidth - 16 - gap * (tabs.length - 1)) / tabs.length;
        for (int i = 0; i < tabs.length; ++i) {
            final Tab t = tabs[i];
            final String text = ((t == this.activeTab) ? "§f§l" : "§7") + names[i];
            this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_(text), b -> {
                this.activeTab = t;
                this.m_232761_();
            }).m_252987_(this.leftPos + 8 + i * (tabW + gap), this.topPos + 24, tabW, 18).m_253136_());
        }
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("§aListo"), b -> this.m_7379_()).m_252987_(this.leftPos + this.panelWidth - 88, this.topPos + this.panelHeight - 24, 80, 18).m_253136_());
        switch (this.activeTab) {
            case GENERAL: {
                this.initGeneral();
                break;
            }
            case LORE: {
                this.initLore();
                break;
            }
            case ENCHANTS: {
                this.initEnchants();
                break;
            }
            case ATTRIBUTES: {
                this.initAttributes();
                break;
            }
        }
    }
    
    private int bx() {
        return this.leftPos + 12;
    }
    
    private int by() {
        return this.topPos + 56;
    }
    
    private int bw() {
        return this.panelWidth - 24;
    }
    
    private int bh() {
        return this.panelHeight - 56 - 28;
    }
    
    public void m_7379_() {
        if (this.parent != null) {
            this.f_96541_.m_91152_(this.parent);
        }
        else {
            super.m_7379_();
        }
    }
    
    public boolean m_7043_() {
        return false;
    }
    
    public void m_88315_(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        this.m_280273_(g);
        g.m_280509_(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -535291870);
        g.m_280509_(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, -14408646);
        g.m_280056_(this.f_96547_, "§d\u2726 §fEditor de NBT del item: §e" + this.stack.m_41786_().getString(), this.leftPos + 8, this.topPos + 6, 16777215, false);
        g.m_280056_(this.f_96547_, "§7Cambia nombre, lore, encantamientos y atributos del item.", this.leftPos + 8, this.topPos + 46, 10133680, false);
        super.m_88315_(g, mouseX, mouseY, partialTick);
    }
    
    private void initGeneral() {
        final int x = this.bx();
        final int y = this.by();
        final char nameColor = this.currentNameColor();
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("§" + nameColor), b -> {
            final char c = this.currentNameColor();
            final int idx = "f7e6cab9d5234180".indexOf(c);
            final char next = "f7e6cab9d5234180".charAt((idx + 1) % "f7e6cab9d5234180".length());
            final String txt = stripColor(this.stack.m_41788_() ? this.stack.m_41786_().getString() : "");
            this.applyName(next, txt);
            this.m_232761_();
        }).m_252987_(x, y, 18, 16).m_253136_());
        final EditBox name = new EditBox(this.f_96547_, x + 22, y, this.bw() - 22, 16, (Component)Component.m_237119_());
        name.m_94199_(128);
        name.m_94144_(stripColor(this.stack.m_41788_() ? this.stack.m_41786_().getString() : ""));
        name.m_257771_((Component)Component.m_237113_("Nombre personalizado del item"));
        name.m_94151_(s -> this.applyName(this.currentNameColor(), s));
        this.m_142416_((GuiEventListener)name);
        final boolean unbreak = this.stack.m_41784_().m_128471_("Unbreakable");
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((unbreak ? "§a" : "§7") + "Irrompible: " + (unbreak ? "S\u00ed" : "No")), b -> {
            final boolean now = !this.stack.m_41784_().m_128471_("Unbreakable");
            this.stack.m_41784_().m_128379_("Unbreakable", now);
            if (!now) {
                this.stack.m_41784_().m_128473_("Unbreakable");
            }
            this.m_232761_();
        }).m_252987_(x, y + 28, 200, 16).m_253136_());
        final EditBox cmd = new EditBox(this.f_96547_, x + 220, y + 28, 80, 16, (Component)Component.m_237119_());
        cmd.m_94199_(8);
        cmd.m_94144_(this.stack.m_41784_().m_128441_("CustomModelData") ? Integer.toString(this.stack.m_41784_().m_128451_("CustomModelData")) : "");
        cmd.m_257771_((Component)Component.m_237113_("CMD"));
        cmd.m_94151_(s -> {
            final String t2 = s.trim();
            if (t2.isEmpty()) {
                this.stack.m_41784_().m_128473_("CustomModelData");
                return;
            }
            else {
                try {
                    this.stack.m_41784_().m_128405_("CustomModelData", Integer.parseInt(t2));
                }
                catch (final NumberFormatException ex) {}
                return;
            }
        });
        this.m_142416_((GuiEventListener)cmd);
        final EditBox dmg = new EditBox(this.f_96547_, x + 220, y + 50, 80, 16, (Component)Component.m_237119_());
        dmg.m_94199_(8);
        dmg.m_94144_(this.stack.m_41784_().m_128441_("Damage") ? Integer.toString(this.stack.m_41784_().m_128451_("Damage")) : "");
        dmg.m_257771_((Component)Component.m_237113_("Da\u00f1o"));
        dmg.m_94151_(s -> {
            final String t4 = s.trim();
            if (t4.isEmpty()) {
                this.stack.m_41784_().m_128473_("Damage");
                return;
            }
            else {
                try {
                    this.stack.m_41784_().m_128405_("Damage", Integer.parseInt(t4));
                }
                catch (final NumberFormatException ex2) {}
                return;
            }
        });
        this.m_142416_((GuiEventListener)dmg);
        final boolean hideAll = this.stack.m_41784_().m_128451_("HideFlags") == 127;
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((hideAll ? "§a" : "§7") + "Ocultar flags vanilla: " + (hideAll ? "S\u00ed" : "No")), b -> {
            final CompoundTag t = this.stack.m_41784_();
            if (t.m_128451_("HideFlags") == 127) {
                t.m_128473_("HideFlags");
            }
            else {
                t.m_128405_("HideFlags", 127);
            }
            this.m_232761_();
        }).m_252987_(x, y + 50, 200, 16).m_253136_());
    }
    
    private char currentNameColor() {
        if (!this.stack.m_41788_()) {
            return 'f';
        }
        final String full = Component.Serializer.m_130703_(this.stack.m_41786_());
        final int idx = full.indexOf("\"color\":\"");
        if (idx < 0) {
            return 'f';
        }
        final String tail = full.substring(idx + 9);
        final int end = tail.indexOf(34);
        final String name = (end > 0) ? tail.substring(0, end) : "white";
        return colorNameToChar(name);
    }
    
    private static char colorNameToChar(final String n) {
        return switch (n) {
            case "black" -> '0';
            case "dark_blue" -> '1';
            case "dark_green" -> '2';
            case "dark_aqua" -> '3';
            case "dark_red" -> '4';
            case "dark_purple" -> '5';
            case "gold" -> '6';
            case "gray" -> '7';
            case "dark_gray" -> '8';
            case "blue" -> '9';
            case "green" -> 'a';
            case "aqua" -> 'b';
            case "red" -> 'c';
            case "light_purple" -> 'd';
            case "yellow" -> 'e';
            default -> 'f';
        };
    }
    
    private static String stripColor(final String s) {
        if (s == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            final char c = s.charAt(i);
            if ((c == '§' || c == '&') && i + 1 < s.length() && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(s.charAt(i + 1)) >= 0) {
                ++i;
            }
            else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    private void applyName(final char color, final String text) {
        if (text == null || text.isEmpty()) {
            this.stack.m_41787_();
            return;
        }
        this.stack.m_41714_((Component)Component.m_237113_("§" + color + text));
    }
    
    private void initLore() {
        final int x = this.bx();
        final int y = this.by();
        final int max = 8;
        final ListTag existing = this.stack.m_41698_("display").m_128437_("Lore", 8);
        final char[] colors = new char[max];
        final String[] texts = new String[max];
        for (int i = 0; i < max; ++i) {
            final String raw = (i < existing.size()) ? existing.m_128778_(i) : "";
            final String plain = jsonToPlain(raw);
            char c = 'f';
            String t = plain;
            if (plain.length() >= 2 && (plain.charAt(0) == '§' || plain.charAt(0) == '&') && "f7e6cab9d5234180".indexOf(plain.charAt(1)) >= 0) {
                c = plain.charAt(1);
                t = plain.substring(2);
            }
            colors[i] = c;
            texts[i] = t;
        }
        final Runnable sync = () -> {
            final CompoundTag display = this.stack.m_41698_("display");
            final ListTag list = new ListTag();
            for (int k = 0; k < max; ++k) {
                if (texts[k] != null) {
                    if (!texts[k].isEmpty()) {
                        final String json = Component.Serializer.m_130703_((Component)Component.m_237113_("§" + colors[k] + texts[k]));
                        list.add((Object)StringTag.m_129297_(json));
                    }
                }
            }
            if (list.isEmpty()) {
                display.m_128473_("Lore");
                if (display.m_128456_()) {
                    this.stack.m_41749_("display");
                }
            }
            else {
                display.m_128365_("Lore", (Tag)list);
            }
            return;
        };
        for (int j = 0; j < max; ++j) {
            final int idx = j;
            final int ry = y + j * 20;
            this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("§" + colors[idx]), b -> {
                final int p = "f7e6cab9d5234180".indexOf(colors[idx]);
                colors[idx] = "f7e6cab9d5234180".charAt((p + 1) % "f7e6cab9d5234180".length());
                sync.run();
                this.m_232761_();
            }).m_252987_(x, ry, 18, 16).m_253136_());
            final EditBox eb = new EditBox(this.f_96547_, x + 22, ry, this.bw() - 22, 16, (Component)Component.m_237119_());
            eb.m_94199_(96);
            eb.m_94144_(texts[idx]);
            eb.m_257771_((Component)Component.m_237113_("Linea de lore " + (idx + 1)));
            eb.m_94151_(s -> {
                texts[idx] = s;
                sync.run();
                return;
            });
            this.m_142416_((GuiEventListener)eb);
        }
    }
    
    private static String jsonToPlain(final String json) {
        try {
            final Component c = (Component)Component.Serializer.m_130701_(json);
            if (c == null) {
                return json;
            }
            return c.getString();
        }
        catch (final Exception e) {
            return json;
        }
    }
    
    private void loadEnchants() {
        this.enchEntries.clear();
        final ListTag list = this.stack.m_41784_().m_128437_("Enchantments", 10);
        for (int i = 0; i < list.size(); ++i) {
            final CompoundTag t = list.m_128728_(i);
            final EnchEntry e = new EnchEntry();
            e.id = t.m_128461_("id");
            e.level = t.m_128451_("lvl");
            this.enchEntries.add(e);
        }
        this.enchLoaded = true;
    }
    
    private void saveEnchants() {
        final ListTag list = new ListTag();
        for (final EnchEntry e : this.enchEntries) {
            if (e.id != null) {
                if (e.id.isEmpty()) {
                    continue;
                }
                final CompoundTag t = new CompoundTag();
                t.m_128359_("id", e.id);
                t.m_128376_("lvl", (short)Math.max(0, e.level));
                list.add((Object)t);
            }
        }
        if (list.isEmpty()) {
            this.stack.m_41749_("Enchantments");
        }
        else {
            this.stack.m_41784_().m_128365_("Enchantments", (Tag)list);
        }
    }
    
    private void initEnchants() {
        if (!this.enchLoaded) {
            this.loadEnchants();
        }
        final int x = this.bx();
        final int y = this.by();
        final int colW = (this.bw() - 8) / 2;
        final int rightX = x + colW + 8;
        final List<ResourceLocation> ids = new ArrayList<ResourceLocation>();
        final Iterator iterator = ForgeRegistries.ENCHANTMENTS.getValues().iterator();
        Enchantment e = null;
        ResourceLocation rl = null;
        while (iterator.hasNext()) {
            e = (Enchantment)iterator.next();
            rl = ForgeRegistries.ENCHANTMENTS.getKey((Object)e);
            if (rl != null) {
                ids.add(rl);
            }
        }
        ids.sort(Comparator.comparing((Function<? super ResourceLocation, ? extends Comparable>)ResourceLocation::toString));
        final EditBox search = new EditBox(this.f_96547_, x, y, colW, 16, (Component)Component.m_237119_());
        search.m_257771_((Component)Component.m_237113_("Buscar encantamiento..."));
        this.m_142416_((GuiEventListener)search);
        final ScrollSelector<ResourceLocation> picker = new ScrollSelector<ResourceLocation>(x, y + 20, colW, this.bh() - 22, 14, rl -> rl.m_135815_(), ResourceLocation::toString, rl -> ItemStack.f_41583_);
        picker.setItems(ids);
        picker.onSelect(rl -> {
            final EnchEntry e3 = new EnchEntry();
            e3.id = rl.toString();
            e3.level = 1;
            this.enchEntries.add(e3);
            this.saveEnchants();
            this.m_232761_();
            return;
        });
        final EditBox editBox = search;
        final ScrollSelector<ResourceLocation> obj = picker;
        Objects.requireNonNull(obj);
        editBox.m_94151_((Consumer)obj::setQuery);
        this.m_142416_((GuiEventListener)picker);
        int ry = y;
        for (int i = 0; i < this.enchEntries.size(); ++i) {
            final EnchEntry e2 = this.enchEntries.get(i);
            final String pretty = e2.id.startsWith("minecraft:") ? e2.id.substring(10) : e2.id;
            final EditBox idBox = new EditBox(this.f_96547_, rightX, ry, colW - 90, 16, (Component)Component.m_237119_());
            idBox.m_94199_(64);
            idBox.m_94144_(pretty);
            idBox.m_94151_(s -> {
                e.id = (s.contains(":") ? s : ("minecraft:" + s));
                this.saveEnchants();
                return;
            });
            this.m_142416_((GuiEventListener)idBox);
            final EditBox lvl = new EditBox(this.f_96547_, rightX + colW - 86, ry, 36, 16, (Component)Component.m_237119_());
            lvl.m_94144_(Integer.toString(e2.level));
            lvl.m_94151_(s -> {
                try {
                    e.level = Integer.parseInt(s.trim());
                    this.saveEnchants();
                }
                catch (final NumberFormatException ex) {}
                return;
            });
            this.m_142416_((GuiEventListener)lvl);
            final int gone = i;
            this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("§cX"), b -> {
                this.enchEntries.remove(gone);
                this.saveEnchants();
                this.m_232761_();
            }).m_252987_(rightX + colW - 46, ry, 22, 16).m_253136_());
            ry += 18;
            if (ry > y + this.bh() - 22) {
                break;
            }
        }
    }
    
    private void loadAttrs() {
        this.attrEntries.clear();
        final ListTag list = this.stack.m_41784_().m_128437_("AttributeModifiers", 10);
        for (int i = 0; i < list.size(); ++i) {
            final CompoundTag t = list.m_128728_(i);
            final AttrEntry e = new AttrEntry();
            e.id = t.m_128461_("AttributeName");
            e.amount = t.m_128459_("Amount");
            e.op = t.m_128451_("Operation");
            e.slot = (t.m_128441_("Slot") ? t.m_128461_("Slot") : "mainhand");
            this.attrEntries.add(e);
        }
        this.attrLoaded = true;
    }
    
    private void saveAttrs() {
        final ListTag list = new ListTag();
        for (final AttrEntry e : this.attrEntries) {
            if (e.id != null) {
                if (e.id.isEmpty()) {
                    continue;
                }
                final CompoundTag t = new CompoundTag();
                t.m_128359_("AttributeName", e.id);
                t.m_128359_("Name", "fscrates");
                t.m_128347_("Amount", e.amount);
                t.m_128405_("Operation", Math.max(0, Math.min(2, e.op)));
                if (!"any".equals(e.slot)) {
                    t.m_128359_("Slot", e.slot);
                }
                final UUID u = UUID.randomUUID();
                t.m_128385_("UUID", new int[] { (int)(u.getMostSignificantBits() >> 32), (int)(u.getMostSignificantBits() & 0xFFFFFFFFL), (int)(u.getLeastSignificantBits() >> 32), (int)(u.getLeastSignificantBits() & 0xFFFFFFFFL) });
                list.add((Object)t);
            }
        }
        if (list.isEmpty()) {
            this.stack.m_41749_("AttributeModifiers");
        }
        else {
            this.stack.m_41784_().m_128365_("AttributeModifiers", (Tag)list);
        }
    }
    
    private void initAttributes() {
        if (!this.attrLoaded) {
            this.loadAttrs();
        }
        final int x = this.bx();
        final int y = this.by();
        final int colW = (this.bw() - 8) / 2;
        final int rightX = x + colW + 8;
        final List<ResourceLocation> ids = new ArrayList<ResourceLocation>();
        ResourceLocation rl = null;
        for (final Attribute a : ForgeRegistries.ATTRIBUTES.getValues()) {
            rl = ForgeRegistries.ATTRIBUTES.getKey((Object)a);
            if (rl != null) {
                ids.add(rl);
            }
        }
        ids.sort(Comparator.comparing((Function<? super ResourceLocation, ? extends Comparable>)ResourceLocation::toString));
        final EditBox search = new EditBox(this.f_96547_, x, y, colW, 16, (Component)Component.m_237119_());
        search.m_257771_((Component)Component.m_237113_("Buscar atributo..."));
        this.m_142416_((GuiEventListener)search);
        final ScrollSelector<ResourceLocation> picker = new ScrollSelector<ResourceLocation>(x, y + 20, colW, this.bh() - 22, 14, rl -> rl.m_135815_(), ResourceLocation::toString, rl -> ItemStack.f_41583_);
        picker.setItems(ids);
        picker.onSelect(rl -> {
            final AttrEntry e2 = new AttrEntry();
            e2.id = rl.toString();
            e2.amount = 1.0;
            e2.op = 0;
            e2.slot = "mainhand";
            this.attrEntries.add(e2);
            this.saveAttrs();
            this.m_232761_();
            return;
        });
        final EditBox editBox = search;
        final ScrollSelector<ResourceLocation> obj = picker;
        Objects.requireNonNull(obj);
        editBox.m_94151_((Consumer)obj::setQuery);
        this.m_142416_((GuiEventListener)picker);
        int ry = y;
        for (int i = 0; i < this.attrEntries.size(); ++i) {
            final AttrEntry e = this.attrEntries.get(i);
            final String pretty = e.id.startsWith("minecraft:") ? e.id.substring(10) : e.id;
            final EditBox idBox = new EditBox(this.f_96547_, rightX, ry, colW - 90, 16, (Component)Component.m_237119_());
            idBox.m_94199_(96);
            idBox.m_94144_(pretty);
            idBox.m_94151_(s -> {
                e.id = (s.contains(":") ? s : ("minecraft:" + s));
                this.saveAttrs();
                return;
            });
            this.m_142416_((GuiEventListener)idBox);
            final EditBox amt = new EditBox(this.f_96547_, rightX + colW - 86, ry, 36, 16, (Component)Component.m_237119_());
            amt.m_94144_(String.format(Locale.ROOT, "%.2f", e.amount));
            amt.m_94151_(s -> {
                try {
                    e.amount = Double.parseDouble(s.trim());
                    this.saveAttrs();
                }
                catch (final NumberFormatException ex) {}
                return;
            });
            this.m_142416_((GuiEventListener)amt);
            final int gone = i;
            this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("§cX"), b -> {
                this.attrEntries.remove(gone);
                this.saveAttrs();
                this.m_232761_();
            }).m_252987_(rightX + colW - 46, ry, 22, 16).m_253136_());
            ry += 18;
            this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("Op: " + NbtEditorScreen.OPS[e.op]), b -> {
                e.op = (e.op + 1) % 3;
                this.saveAttrs();
                this.m_232761_();
            }).m_252987_(rightX, ry, (colW - 8) / 2, 16).m_253136_());
            this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_("Slot: " + e.slot), b -> {
                int idx = 0;
                for (int k = 0; k < NbtEditorScreen.SLOTS.length; ++k) {
                    if (NbtEditorScreen.SLOTS[k].equals(e.slot)) {
                        idx = k;
                        break;
                    }
                }
                e.slot = NbtEditorScreen.SLOTS[(idx + 1) % NbtEditorScreen.SLOTS.length];
                this.saveAttrs();
                this.m_232761_();
            }).m_252987_(rightX + (colW - 8) / 2 + 8, ry, (colW - 8) / 2, 16).m_253136_());
            ry += 22;
            if (ry > y + this.bh() - 24) {
                break;
            }
        }
    }
    
    static {
        OPS = new String[] { "Sumar", "x base", "x total" };
        SLOTS = new String[] { "any", "mainhand", "offhand", "head", "chest", "legs", "feet" };
    }
    
    private enum Tab
    {
        GENERAL, 
        LORE, 
        ENCHANTS, 
        ATTRIBUTES;
    }
    
    private static class EnchEntry
    {
        String id;
        int level;
        
        private EnchEntry() {
            this.id = "minecraft:sharpness";
            this.level = 1;
        }
    }
    
    private static class AttrEntry
    {
        String id;
        double amount;
        int op;
        String slot;
        
        private AttrEntry() {
            this.id = "minecraft:generic.attack_damage";
            this.amount = 1.0;
            this.op = 0;
            this.slot = "mainhand";
        }
    }
}
