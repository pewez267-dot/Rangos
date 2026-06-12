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
        super((Component)Component.literal("Editor de NBT"));
        this.activeTab = Tab.GENERAL;
        this.enchEntries = new ArrayList<EnchEntry>();
        this.enchLoaded = false;
        this.attrEntries = new ArrayList<AttrEntry>();
        this.attrLoaded = false;
        this.parent = parent;
        this.stack = stack;
    }
    
    protected void init() {
        this.panelWidth = Math.min(this.width - 20, 540);
        this.panelHeight = Math.min(this.height - 20, 320);
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;
        final Tab[] tabs = Tab.values();
        final String[] names = { "General", "Lore", "Encantamientos", "Atributos" };
        final int gap = 4;
        final int tabW = (this.panelWidth - 16 - gap * (tabs.length - 1)) / tabs.length;
        for (int i = 0; i < tabs.length; ++i) {
            final Tab t = tabs[i];
            final String text = ((t == this.activeTab) ? "§f§l" : "§7") + names[i];
            this.addRenderableWidget(Button.builder((Component)Component.literal(text), b -> {
                this.activeTab = t;
                this.rebuildWidgets();
            }).bounds(this.leftPos + 8 + i * (tabW + gap), this.topPos + 24, tabW, 18).build());
        }
        this.addRenderableWidget(Button.builder((Component)Component.literal("§aListo"), b -> this.onClose()).bounds(this.leftPos + this.panelWidth - 88, this.topPos + this.panelHeight - 24, 80, 18).build());
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
    
    public void onClose() {
        if (this.parent != null) {
            this.minecraft.setScreen(this.parent);
        }
        else {
            super.onClose();
        }
    }
    
    public boolean isPauseScreen() {
        return false;
    }
    
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        this.renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -535291870);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, -14408646);
        g.drawString(this.font, "§d\u2726 §fEditor de NBT del item: §e" + this.stack.getHoverName().getString(), this.leftPos + 8, this.topPos + 6, 16777215, false);
        g.drawString(this.font, "§7Cambia nombre, lore, encantamientos y atributos del item.", this.leftPos + 8, this.topPos + 46, 10133680, false);
        super.render(g, mouseX, mouseY, partialTick);
    }
    
    private void initGeneral() {
        final int x = this.bx();
        final int y = this.by();
        final char nameColor = this.currentNameColor();
        this.addRenderableWidget(Button.builder((Component)Component.literal("§" + nameColor), b -> {
            final char c = this.currentNameColor();
            final int idx = "f7e6cab9d5234180".indexOf(c);
            final char next = "f7e6cab9d5234180".charAt((idx + 1) % "f7e6cab9d5234180".length());
            final String txt = stripColor(this.stack.hasCustomHoverName() ? this.stack.getHoverName().getString() : "");
            this.applyName(next, txt);
            this.rebuildWidgets();
        }).bounds(x, y, 18, 16).build());
        final EditBox name = new EditBox(this.font, x + 22, y, this.bw() - 22, 16, (Component)Component.empty());
        name.setMaxLength(128);
        name.setValue(stripColor(this.stack.hasCustomHoverName() ? this.stack.getHoverName().getString() : ""));
        name.setHint((Component)Component.literal("Nombre personalizado del item"));
        name.setResponder(s -> this.applyName(this.currentNameColor(), s));
        this.addRenderableWidget(name);
        final boolean unbreak = this.stack.getOrCreateTag().getBoolean("Unbreakable");
        this.addRenderableWidget(Button.builder((Component)Component.literal((unbreak ? "§a" : "§7") + "Irrompible: " + (unbreak ? "S\u00ed" : "No")), b -> {
            final boolean now = !this.stack.getOrCreateTag().getBoolean("Unbreakable");
            this.stack.getOrCreateTag().putBoolean("Unbreakable", now);
            if (!now) {
                this.stack.getOrCreateTag().remove("Unbreakable");
            }
            this.rebuildWidgets();
        }).bounds(x, y + 28, 200, 16).build());
        final EditBox cmd = new EditBox(this.font, x + 220, y + 28, 80, 16, (Component)Component.empty());
        cmd.setMaxLength(8);
        cmd.setValue(this.stack.getOrCreateTag().contains("CustomModelData") ? Integer.toString(this.stack.getOrCreateTag().getInt("CustomModelData")) : "");
        cmd.setHint((Component)Component.literal("CMD"));
        cmd.setResponder(s -> {
            final String t2 = s.trim();
            if (t2.isEmpty()) {
                this.stack.getOrCreateTag().remove("CustomModelData");
                return;
            }
            else {
                try {
                    this.stack.getOrCreateTag().putInt("CustomModelData", Integer.parseInt(t2));
                }
                catch (final NumberFormatException ex) {}
                return;
            }
        });
        this.addRenderableWidget(cmd);
        final EditBox dmg = new EditBox(this.font, x + 220, y + 50, 80, 16, (Component)Component.empty());
        dmg.setMaxLength(8);
        dmg.setValue(this.stack.getOrCreateTag().contains("Damage") ? Integer.toString(this.stack.getOrCreateTag().getInt("Damage")) : "");
        dmg.setHint((Component)Component.literal("Da\u00f1o"));
        dmg.setResponder(s -> {
            final String t4 = s.trim();
            if (t4.isEmpty()) {
                this.stack.getOrCreateTag().remove("Damage");
                return;
            }
            else {
                try {
                    this.stack.getOrCreateTag().putInt("Damage", Integer.parseInt(t4));
                }
                catch (final NumberFormatException ex2) {}
                return;
            }
        });
        this.addRenderableWidget(dmg);
        final boolean hideAll = this.stack.getOrCreateTag().getInt("HideFlags") == 127;
        this.addRenderableWidget(Button.builder((Component)Component.literal((hideAll ? "§a" : "§7") + "Ocultar flags vanilla: " + (hideAll ? "S\u00ed" : "No")), b -> {
            final CompoundTag t = this.stack.getOrCreateTag();
            if (t.getInt("HideFlags") == 127) {
                t.remove("HideFlags");
            }
            else {
                t.putInt("HideFlags", 127);
            }
            this.rebuildWidgets();
        }).bounds(x, y + 50, 200, 16).build());
    }
    
    private char currentNameColor() {
        if (!this.stack.hasCustomHoverName()) {
            return 'f';
        }
        final String full = Component.Serializer.toJson(this.stack.getHoverName());
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
            this.stack.resetHoverName();
            return;
        }
        this.stack.setHoverName((Component)Component.literal("§" + color + text));
    }
    
    private void initLore() {
        final int x = this.bx();
        final int y = this.by();
        final int max = 8;
        final ListTag existing = this.stack.getOrCreateTagElement("display").getList("Lore", 8);
        final char[] colors = new char[max];
        final String[] texts = new String[max];
        for (int i = 0; i < max; ++i) {
            final String raw = (i < existing.size()) ? existing.getString(i) : "";
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
            final CompoundTag display = this.stack.getOrCreateTagElement("display");
            final ListTag list = new ListTag();
            for (int k = 0; k < max; ++k) {
                if (texts[k] != null) {
                    if (!texts[k].isEmpty()) {
                        final String json = Component.Serializer.toJson((Component)Component.literal("§" + colors[k] + texts[k]));
                        list.add(StringTag.valueOf(json));
                    }
                }
            }
            if (list.isEmpty()) {
                display.remove("Lore");
                if (display.isEmpty()) {
                    this.stack.removeTagKey("display");
                }
            }
            else {
                display.put("Lore", (Tag)list);
            }
            return;
        };
        for (int j = 0; j < max; ++j) {
            final int idx = j;
            final int ry = y + j * 20;
            this.addRenderableWidget(Button.builder((Component)Component.literal("§" + colors[idx]), b -> {
                final int p = "f7e6cab9d5234180".indexOf(colors[idx]);
                colors[idx] = "f7e6cab9d5234180".charAt((p + 1) % "f7e6cab9d5234180".length());
                sync.run();
                this.rebuildWidgets();
            }).bounds(x, ry, 18, 16).build());
            final EditBox eb = new EditBox(this.font, x + 22, ry, this.bw() - 22, 16, (Component)Component.empty());
            eb.setMaxLength(96);
            eb.setValue(texts[idx]);
            eb.setHint((Component)Component.literal("Linea de lore " + (idx + 1)));
            eb.setResponder(s -> {
                texts[idx] = s;
                sync.run();
                return;
            });
            this.addRenderableWidget(eb);
        }
    }
    
    private static String jsonToPlain(final String json) {
        try {
            final Component c = (Component)Component.Serializer.fromJson(json);
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
        final ListTag list = this.stack.getOrCreateTag().getList("Enchantments", 10);
        for (int i = 0; i < list.size(); ++i) {
            final CompoundTag t = list.getCompound(i);
            final EnchEntry e = new EnchEntry();
            e.id = t.getString("id");
            e.level = t.getInt("lvl");
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
                t.putString("id", e.id);
                t.putShort("lvl", (short)Math.max(0, e.level));
                list.add(t);
            }
        }
        if (list.isEmpty()) {
            this.stack.removeTagKey("Enchantments");
        }
        else {
            this.stack.getOrCreateTag().put("Enchantments", (Tag)list);
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
            rl = ForgeRegistries.ENCHANTMENTS.getKey(e);
            if (rl != null) {
                ids.add(rl);
            }
        }
        ids.sort(Comparator.comparing((Function<? super ResourceLocation, ? extends Comparable>)ResourceLocation::toString));
        final EditBox search = new EditBox(this.font, x, y, colW, 16, (Component)Component.empty());
        search.setHint((Component)Component.literal("Buscar encantamiento..."));
        this.addRenderableWidget(search);
        final ScrollSelector<ResourceLocation> picker = new ScrollSelector<ResourceLocation>(x, y + 20, colW, this.bh() - 22, 14, rl -> rl.getPath(), ResourceLocation::toString, rl -> ItemStack.EMPTY);
        picker.setItems(ids);
        picker.onSelect(rl -> {
            final EnchEntry e3 = new EnchEntry();
            e3.id = rl.toString();
            e3.level = 1;
            this.enchEntries.add(e3);
            this.saveEnchants();
            this.rebuildWidgets();
            return;
        });
        final EditBox editBox = search;
        final ScrollSelector<ResourceLocation> obj = picker;
        Objects.requireNonNull(obj);
        editBox.setResponder(obj::setQuery);
        this.addRenderableWidget(picker);
        int ry = y;
        for (int i = 0; i < this.enchEntries.size(); ++i) {
            final EnchEntry e2 = this.enchEntries.get(i);
            final String pretty = e2.id.startsWith("minecraft:") ? e2.id.substring(10) : e2.id;
            final EditBox idBox = new EditBox(this.font, rightX, ry, colW - 90, 16, (Component)Component.empty());
            idBox.setMaxLength(64);
            idBox.setValue(pretty);
            idBox.setResponder(s -> {
                e.id = (s.contains(":") ? s : ("minecraft:" + s));
                this.saveEnchants();
                return;
            });
            this.addRenderableWidget(idBox);
            final EditBox lvl = new EditBox(this.font, rightX + colW - 86, ry, 36, 16, (Component)Component.empty());
            lvl.setValue(Integer.toString(e2.level));
            lvl.setResponder(s -> {
                try {
                    e.level = Integer.parseInt(s.trim());
                    this.saveEnchants();
                }
                catch (final NumberFormatException ex) {}
                return;
            });
            this.addRenderableWidget(lvl);
            final int gone = i;
            this.addRenderableWidget(Button.builder((Component)Component.literal("§cX"), b -> {
                this.enchEntries.remove(gone);
                this.saveEnchants();
                this.rebuildWidgets();
            }).bounds(rightX + colW - 46, ry, 22, 16).build());
            ry += 18;
            if (ry > y + this.bh() - 22) {
                break;
            }
        }
    }
    
    private void loadAttrs() {
        this.attrEntries.clear();
        final ListTag list = this.stack.getOrCreateTag().getList("AttributeModifiers", 10);
        for (int i = 0; i < list.size(); ++i) {
            final CompoundTag t = list.getCompound(i);
            final AttrEntry e = new AttrEntry();
            e.id = t.getString("AttributeName");
            e.amount = t.getDouble("Amount");
            e.op = t.getInt("Operation");
            e.slot = (t.contains("Slot") ? t.getString("Slot") : "mainhand");
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
                t.putString("AttributeName", e.id);
                t.putString("Name", "fscrates");
                t.putDouble("Amount", e.amount);
                t.putInt("Operation", Math.max(0, Math.min(2, e.op)));
                if (!"any".equals(e.slot)) {
                    t.putString("Slot", e.slot);
                }
                final UUID u = UUID.randomUUID();
                t.putIntArray("UUID", new int[] { (int)(u.getMostSignificantBits() >> 32), (int)(u.getMostSignificantBits() & 0xFFFFFFFFL), (int)(u.getLeastSignificantBits() >> 32), (int)(u.getLeastSignificantBits() & 0xFFFFFFFFL) });
                list.add(t);
            }
        }
        if (list.isEmpty()) {
            this.stack.removeTagKey("AttributeModifiers");
        }
        else {
            this.stack.getOrCreateTag().put("AttributeModifiers", (Tag)list);
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
            rl = ForgeRegistries.ATTRIBUTES.getKey(a);
            if (rl != null) {
                ids.add(rl);
            }
        }
        ids.sort(Comparator.comparing((Function<? super ResourceLocation, ? extends Comparable>)ResourceLocation::toString));
        final EditBox search = new EditBox(this.font, x, y, colW, 16, (Component)Component.empty());
        search.setHint((Component)Component.literal("Buscar atributo..."));
        this.addRenderableWidget(search);
        final ScrollSelector<ResourceLocation> picker = new ScrollSelector<ResourceLocation>(x, y + 20, colW, this.bh() - 22, 14, rl -> rl.getPath(), ResourceLocation::toString, rl -> ItemStack.EMPTY);
        picker.setItems(ids);
        picker.onSelect(rl -> {
            final AttrEntry e2 = new AttrEntry();
            e2.id = rl.toString();
            e2.amount = 1.0;
            e2.op = 0;
            e2.slot = "mainhand";
            this.attrEntries.add(e2);
            this.saveAttrs();
            this.rebuildWidgets();
            return;
        });
        final EditBox editBox = search;
        final ScrollSelector<ResourceLocation> obj = picker;
        Objects.requireNonNull(obj);
        editBox.setResponder(obj::setQuery);
        this.addRenderableWidget(picker);
        int ry = y;
        for (int i = 0; i < this.attrEntries.size(); ++i) {
            final AttrEntry e = this.attrEntries.get(i);
            final String pretty = e.id.startsWith("minecraft:") ? e.id.substring(10) : e.id;
            final EditBox idBox = new EditBox(this.font, rightX, ry, colW - 90, 16, (Component)Component.empty());
            idBox.setMaxLength(96);
            idBox.setValue(pretty);
            idBox.setResponder(s -> {
                e.id = (s.contains(":") ? s : ("minecraft:" + s));
                this.saveAttrs();
                return;
            });
            this.addRenderableWidget(idBox);
            final EditBox amt = new EditBox(this.font, rightX + colW - 86, ry, 36, 16, (Component)Component.empty());
            amt.setValue(String.format(Locale.ROOT, "%.2f", e.amount));
            amt.setResponder(s -> {
                try {
                    e.amount = Double.parseDouble(s.trim());
                    this.saveAttrs();
                }
                catch (final NumberFormatException ex) {}
                return;
            });
            this.addRenderableWidget(amt);
            final int gone = i;
            this.addRenderableWidget(Button.builder((Component)Component.literal("§cX"), b -> {
                this.attrEntries.remove(gone);
                this.saveAttrs();
                this.rebuildWidgets();
            }).bounds(rightX + colW - 46, ry, 22, 16).build());
            ry += 18;
            this.addRenderableWidget(Button.builder((Component)Component.literal("Op: " + NbtEditorScreen.OPS[e.op]), b -> {
                e.op = (e.op + 1) % 3;
                this.saveAttrs();
                this.rebuildWidgets();
            }).bounds(rightX, ry, (colW - 8) / 2, 16).build());
            this.addRenderableWidget(Button.builder((Component)Component.literal("Slot: " + e.slot), b -> {
                int idx = 0;
                for (int k = 0; k < NbtEditorScreen.SLOTS.length; ++k) {
                    if (NbtEditorScreen.SLOTS[k].equals(e.slot)) {
                        idx = k;
                        break;
                    }
                }
                e.slot = NbtEditorScreen.SLOTS[(idx + 1) % NbtEditorScreen.SLOTS.length];
                this.saveAttrs();
                this.rebuildWidgets();
            }).bounds(rightX + (colW - 8) / 2 + 8, ry, (colW - 8) / 2, 16).build());
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
