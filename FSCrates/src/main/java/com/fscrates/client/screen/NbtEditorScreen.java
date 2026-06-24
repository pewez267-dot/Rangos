package com.fscrates.client.screen;

import com.fscrates.client.RegistryLists;
import com.fscrates.client.widget.ScrollSelector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * A real, structured NBT editor for an item reward. Edits the actual NBT of the
 * provided {@link ItemStack} (custom name, lore lines with per-line colour,
 * unbreakable, custom-model-data, damage, enchantments and attribute
 * modifiers). No SNBT pasting required.
 */
public class NbtEditorScreen extends Screen {

    private enum Tab { GENERAL, LORE, ENCHANTS, ATTRIBUTES }

    private static final String COLOR_CHARS = "f7e6cab9d5234180";

    private static final String[] OPS = { "Sumar", "x base", "x total" };
    private static final String[] SLOTS = {
            "any", "mainhand", "offhand", "head", "chest", "legs", "feet"
    };

    private final Screen parent;
    private final ItemStack stack;

    private Tab activeTab = Tab.GENERAL;
    private int leftPos, topPos, panelWidth, panelHeight;

    public NbtEditorScreen(Screen parent, ItemStack stack) {
        super(Component.literal("Editor de NBT"));
        this.parent = parent;
        this.stack = stack;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(this.width - 20, 540);
        panelHeight = Math.min(this.height - 20, 320);
        leftPos = (this.width - panelWidth) / 2;
        topPos = (this.height - panelHeight) / 2;

        // header tabs
        Tab[] tabs = Tab.values();
        String[] names = { "General", "Lore", "Encantamientos", "Atributos" };
        int gap = 4;
        int tabW = (panelWidth - 16 - gap * (tabs.length - 1)) / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            final Tab t = tabs[i];
            String text = (t == activeTab ? "\u00A7f\u00A7l" : "\u00A77") + names[i];
            addRenderableWidget(Button.builder(Component.literal(text), b -> {
                activeTab = t; rebuildWidgets();
            }).bounds(leftPos + 8 + i * (tabW + gap), topPos + 24, tabW, 18).build());
        }

        // footer
        addRenderableWidget(Button.builder(Component.literal("\u00A7aListo"), b -> onClose())
                .bounds(leftPos + panelWidth - 88, topPos + panelHeight - 24, 80, 18).build());

        switch (activeTab) {
            case GENERAL -> initGeneral();
            case LORE -> initLore();
            case ENCHANTS -> initEnchants();
            case ATTRIBUTES -> initAttributes();
        }
    }

    private int bx() { return leftPos + 12; }
    private int by() { return topPos + 56; }
    private int bw() { return panelWidth - 24; }
    private int bh() { return panelHeight - 56 - 28; }

    @Override
    public void onClose() {
        if (parent != null) {
            this.minecraft.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + panelHeight, 0xE0181822);
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + 20, 0xFF24243A);
        g.drawString(font, "\u00A7d\u2726 \u00A7fEditor de NBT del item: \u00A7e"
                + stack.getHoverName().getString(), leftPos + 8, topPos + 6, 0xFFFFFF, false);
        g.drawString(font, "\u00A77Cambia nombre, lore, encantamientos y atributos del item.",
                leftPos + 8, topPos + 46, 0x9AA0B0, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    // ------------------------------------------------------------------
    // GENERAL: name, color, unbreakable, custom-model-data, damage
    // ------------------------------------------------------------------

    private void initGeneral() {
        int x = bx();
        int y = by();

        // -- custom name
        char nameColor = currentNameColor();
        addRenderableWidget(Button.builder(Component.literal("\u00A7" + nameColor + "\u25A0"), b -> {
            char c = currentNameColor();
            int idx = COLOR_CHARS.indexOf(c);
            char next = COLOR_CHARS.charAt((idx + 1) % COLOR_CHARS.length());
            String txt = stripColor(stack.hasCustomHoverName() ? stack.getHoverName().getString() : "");
            applyName(next, txt);
            rebuildWidgets();
        }).bounds(x, y, 18, 16).build());
        EditBox name = new EditBox(font, x + 22, y, bw() - 22, 16, Component.empty());
        name.setMaxLength(128);
        name.setValue(stripColor(stack.hasCustomHoverName() ? stack.getHoverName().getString() : ""));
        name.setHint(Component.literal("Nombre personalizado del item"));
        name.setResponder(s -> applyName(currentNameColor(), s));
        addRenderableWidget(name);

        // -- unbreakable
        boolean unbreak = stack.getOrCreateTag().getBoolean("Unbreakable");
        addRenderableWidget(Button.builder(Component.literal((unbreak ? "\u00A7a" : "\u00A77")
                        + "Irrompible: " + (unbreak ? "S\u00ed" : "No")), b -> {
            boolean now = !stack.getOrCreateTag().getBoolean("Unbreakable");
            stack.getOrCreateTag().putBoolean("Unbreakable", now);
            if (!now) stack.getOrCreateTag().remove("Unbreakable");
            rebuildWidgets();
        }).bounds(x, y + 28, 200, 16).build());

        // -- custom model data
        EditBox cmd = new EditBox(font, x + 220, y + 28, 80, 16, Component.empty());
        cmd.setMaxLength(8);
        cmd.setValue(stack.getOrCreateTag().contains("CustomModelData")
                ? Integer.toString(stack.getOrCreateTag().getInt("CustomModelData")) : "");
        cmd.setHint(Component.literal("CMD"));
        cmd.setResponder(s -> {
            String t = s.trim();
            if (t.isEmpty()) {
                stack.getOrCreateTag().remove("CustomModelData");
                return;
            }
            try {
                stack.getOrCreateTag().putInt("CustomModelData", Integer.parseInt(t));
            } catch (NumberFormatException ignored) {}
        });
        addRenderableWidget(cmd);

        // -- damage
        EditBox dmg = new EditBox(font, x + 220, y + 50, 80, 16, Component.empty());
        dmg.setMaxLength(8);
        dmg.setValue(stack.getOrCreateTag().contains("Damage")
                ? Integer.toString(stack.getOrCreateTag().getInt("Damage")) : "");
        dmg.setHint(Component.literal("Da\u00f1o"));
        dmg.setResponder(s -> {
            String t = s.trim();
            if (t.isEmpty()) {
                stack.getOrCreateTag().remove("Damage");
                return;
            }
            try {
                stack.getOrCreateTag().putInt("Damage", Integer.parseInt(t));
            } catch (NumberFormatException ignored) {}
        });
        addRenderableWidget(dmg);

        // -- HideFlags
        boolean hideAll = stack.getOrCreateTag().getInt("HideFlags") == 127;
        addRenderableWidget(Button.builder(Component.literal((hideAll ? "\u00A7a" : "\u00A77")
                        + "Ocultar flags vanilla: " + (hideAll ? "S\u00ed" : "No")), b -> {
            CompoundTag t = stack.getOrCreateTag();
            if (t.getInt("HideFlags") == 127) {
                t.remove("HideFlags");
            } else {
                t.putInt("HideFlags", 127);
            }
            rebuildWidgets();
        }).bounds(x, y + 50, 200, 16).build());
    }

    private char currentNameColor() {
        if (!stack.hasCustomHoverName()) return 'f';
        String full = Component.Serializer.toJson(stack.getHoverName());
        int idx = full.indexOf("\"color\":\"");
        if (idx < 0) return 'f';
        // map colour name back to a code char
        String tail = full.substring(idx + 9);
        int end = tail.indexOf('"');
        String name = end > 0 ? tail.substring(0, end) : "white";
        return colorNameToChar(name);
    }

    private static char colorNameToChar(String n) {
        return switch (n) {
            case "black" -> '0'; case "dark_blue" -> '1'; case "dark_green" -> '2';
            case "dark_aqua" -> '3'; case "dark_red" -> '4'; case "dark_purple" -> '5';
            case "gold" -> '6'; case "gray" -> '7'; case "dark_gray" -> '8';
            case "blue" -> '9'; case "green" -> 'a'; case "aqua" -> 'b';
            case "red" -> 'c'; case "light_purple" -> 'd'; case "yellow" -> 'e';
            default -> 'f';
        };
    }

    private static String stripColor(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '\u00A7' || c == '&') && i + 1 < s.length()
                    && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(s.charAt(i + 1)) >= 0) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private void applyName(char color, String text) {
        if (text == null || text.isEmpty()) {
            stack.resetHoverName();
            return;
        }
        stack.setHoverName(Component.literal("\u00A7" + color + text));
    }

    // ------------------------------------------------------------------
    // LORE: 8 lines, each with its own colour
    // ------------------------------------------------------------------

    private void initLore() {
        int x = bx();
        int y = by();
        int max = 8;

        ListTag existing = stack.getOrCreateTagElement("display").getList("Lore", Tag.TAG_STRING);
        char[] colors = new char[max];
        String[] texts = new String[max];
        for (int i = 0; i < max; i++) {
            String raw = i < existing.size() ? existing.getString(i) : "";
            String plain = jsonToPlain(raw);
            char c = 'f';
            String t = plain;
            if (plain.length() >= 2 && (plain.charAt(0) == '\u00A7' || plain.charAt(0) == '&')
                    && COLOR_CHARS.indexOf(plain.charAt(1)) >= 0) {
                c = plain.charAt(1);
                t = plain.substring(2);
            }
            colors[i] = c;
            texts[i] = t;
        }

        Runnable sync = () -> {
            CompoundTag display = stack.getOrCreateTagElement("display");
            ListTag list = new ListTag();
            for (int i = 0; i < max; i++) {
                if (texts[i] == null || texts[i].isEmpty()) continue;
                String json = Component.Serializer.toJson(
                        Component.literal("\u00A7" + colors[i] + texts[i]));
                list.add(StringTag.valueOf(json));
            }
            if (list.isEmpty()) {
                display.remove("Lore");
                if (display.isEmpty()) stack.removeTagKey("display");
            } else {
                display.put("Lore", list);
            }
        };

        for (int i = 0; i < max; i++) {
            final int idx = i;
            int ry = y + i * 20;
            addRenderableWidget(Button.builder(Component.literal("\u00A7" + colors[idx] + "\u25A0"), b -> {
                int p = COLOR_CHARS.indexOf(colors[idx]);
                colors[idx] = COLOR_CHARS.charAt((p + 1) % COLOR_CHARS.length());
                sync.run();
                rebuildWidgets();
            }).bounds(x, ry, 18, 16).build());
            EditBox eb = new EditBox(font, x + 22, ry, bw() - 22, 16, Component.empty());
            eb.setMaxLength(96);
            eb.setValue(texts[idx]);
            eb.setHint(Component.literal("Linea de lore " + (idx + 1)));
            eb.setResponder(s -> { texts[idx] = s; sync.run(); });
            addRenderableWidget(eb);
        }
    }

    private static String jsonToPlain(String json) {
        try {
            Component c = Component.Serializer.fromJson(json);
            if (c == null) return json;
            return c.getString();
        } catch (Exception e) {
            return json;
        }
    }

    // ------------------------------------------------------------------
    // ENCHANTS: list of {id, level}
    // ------------------------------------------------------------------

    private static class EnchEntry { String id = "minecraft:sharpness"; int level = 1; }
    private final List<EnchEntry> enchEntries = new ArrayList<>();
    private boolean enchLoaded = false;

    private void loadEnchants() {
        enchEntries.clear();
        ListTag list = stack.getOrCreateTag().getList("Enchantments", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            EnchEntry e = new EnchEntry();
            e.id = t.getString("id");
            e.level = t.getInt("lvl");
            enchEntries.add(e);
        }
        enchLoaded = true;
    }

    private void saveEnchants() {
        ListTag list = new ListTag();
        for (EnchEntry e : enchEntries) {
            if (e.id == null || e.id.isEmpty()) continue;
            CompoundTag t = new CompoundTag();
            t.putString("id", e.id);
            t.putShort("lvl", (short) Math.max(0, e.level));
            list.add(t);
        }
        if (list.isEmpty()) {
            stack.removeTagKey("Enchantments");
        } else {
            stack.getOrCreateTag().put("Enchantments", list);
        }
    }

    private void initEnchants() {
        if (!enchLoaded) loadEnchants();
        int x = bx();
        int y = by();
        int colW = (bw() - 8) / 2;
        int rightX = x + colW + 8;

        // left: enchantment id picker
        List<ResourceLocation> ids = new ArrayList<>();
        for (Enchantment e : ForgeRegistries.ENCHANTMENTS.getValues()) {
            ResourceLocation rl = ForgeRegistries.ENCHANTMENTS.getKey(e);
            if (rl != null) ids.add(rl);
        }
        ids.sort(Comparator.comparing(ResourceLocation::toString));

        EditBox search = new EditBox(font, x, y, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar encantamiento..."));
        addRenderableWidget(search);
        ScrollSelector<ResourceLocation> picker = new ScrollSelector<>(x, y + 20, colW, bh() - 22, 14,
                rl -> rl.getPath(), ResourceLocation::toString, rl -> ItemStack.EMPTY);
        picker.setItems(ids);
        picker.onSelect(rl -> {
            EnchEntry e = new EnchEntry();
            e.id = rl.toString();
            e.level = 1;
            enchEntries.add(e);
            saveEnchants();
            rebuildWidgets();
        });
        search.setResponder(picker::setQuery);
        addRenderableWidget(picker);

        // right: current enchantments list with level + remove
        int ry = y;
        for (int i = 0; i < enchEntries.size(); i++) {
            EnchEntry e = enchEntries.get(i);
            String pretty = e.id.startsWith("minecraft:") ? e.id.substring(10) : e.id;
            EditBox idBox = new EditBox(font, rightX, ry, colW - 90, 16, Component.empty());
            idBox.setMaxLength(64);
            idBox.setValue(pretty);
            idBox.setResponder(s -> {
                e.id = s.contains(":") ? s : "minecraft:" + s;
                saveEnchants();
            });
            addRenderableWidget(idBox);
            EditBox lvl = new EditBox(font, rightX + colW - 86, ry, 36, 16, Component.empty());
            lvl.setValue(Integer.toString(e.level));
            lvl.setResponder(s -> {
                try { e.level = Integer.parseInt(s.trim()); saveEnchants(); }
                catch (NumberFormatException ignored) {}
            });
            addRenderableWidget(lvl);
            int gone = i;
            addRenderableWidget(Button.builder(Component.literal("\u00A7cX"), b -> {
                enchEntries.remove(gone); saveEnchants(); rebuildWidgets();
            }).bounds(rightX + colW - 46, ry, 22, 16).build());
            ry += 18;
            if (ry > y + bh() - 22) break;
        }
    }

    // ------------------------------------------------------------------
    // ATTRIBUTES: list of {attribute, amount, op, slot}
    // ------------------------------------------------------------------

    private static class AttrEntry {
        String id = "minecraft:generic.attack_damage";
        double amount = 1.0;
        int op = 0; // 0 add, 1 multiply_base, 2 multiply_total
        String slot = "mainhand";
    }
    private final List<AttrEntry> attrEntries = new ArrayList<>();
    private boolean attrLoaded = false;

    private void loadAttrs() {
        attrEntries.clear();
        ListTag list = stack.getOrCreateTag().getList("AttributeModifiers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            AttrEntry e = new AttrEntry();
            e.id = t.getString("AttributeName");
            e.amount = t.getDouble("Amount");
            e.op = t.getInt("Operation");
            e.slot = t.contains("Slot") ? t.getString("Slot") : "mainhand";
            attrEntries.add(e);
        }
        attrLoaded = true;
    }

    private void saveAttrs() {
        ListTag list = new ListTag();
        for (AttrEntry e : attrEntries) {
            if (e.id == null || e.id.isEmpty()) continue;
            CompoundTag t = new CompoundTag();
            t.putString("AttributeName", e.id);
            t.putString("Name", "fscrates");
            t.putDouble("Amount", e.amount);
            t.putInt("Operation", Math.max(0, Math.min(2, e.op)));
            if (!"any".equals(e.slot)) {
                t.putString("Slot", e.slot);
            }
            UUID u = UUID.randomUUID();
            t.putIntArray("UUID", new int[] {
                    (int) (u.getMostSignificantBits() >> 32),
                    (int) (u.getMostSignificantBits() & 0xFFFFFFFFL),
                    (int) (u.getLeastSignificantBits() >> 32),
                    (int) (u.getLeastSignificantBits() & 0xFFFFFFFFL)
            });
            list.add(t);
        }
        if (list.isEmpty()) {
            stack.removeTagKey("AttributeModifiers");
        } else {
            stack.getOrCreateTag().put("AttributeModifiers", list);
        }
    }

    private void initAttributes() {
        if (!attrLoaded) loadAttrs();
        int x = bx();
        int y = by();
        int colW = (bw() - 8) / 2;
        int rightX = x + colW + 8;

        // attribute picker
        List<ResourceLocation> ids = new ArrayList<>();
        for (Attribute a : ForgeRegistries.ATTRIBUTES.getValues()) {
            ResourceLocation rl = ForgeRegistries.ATTRIBUTES.getKey(a);
            if (rl != null) ids.add(rl);
        }
        ids.sort(Comparator.comparing(ResourceLocation::toString));

        EditBox search = new EditBox(font, x, y, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar atributo..."));
        addRenderableWidget(search);
        ScrollSelector<ResourceLocation> picker = new ScrollSelector<>(x, y + 20, colW, bh() - 22, 14,
                rl -> rl.getPath(), ResourceLocation::toString, rl -> ItemStack.EMPTY);
        picker.setItems(ids);
        picker.onSelect(rl -> {
            AttrEntry e = new AttrEntry();
            e.id = rl.toString();
            e.amount = 1.0;
            e.op = 0;
            e.slot = "mainhand";
            attrEntries.add(e);
            saveAttrs();
            rebuildWidgets();
        });
        search.setResponder(picker::setQuery);
        addRenderableWidget(picker);

        // current list
        int ry = y;
        for (int i = 0; i < attrEntries.size(); i++) {
            AttrEntry e = attrEntries.get(i);
            String pretty = e.id.startsWith("minecraft:") ? e.id.substring(10) : e.id;
            // first row: attribute id + amount + remove
            EditBox idBox = new EditBox(font, rightX, ry, colW - 90, 16, Component.empty());
            idBox.setMaxLength(96);
            idBox.setValue(pretty);
            idBox.setResponder(s -> {
                e.id = s.contains(":") ? s : "minecraft:" + s;
                saveAttrs();
            });
            addRenderableWidget(idBox);
            EditBox amt = new EditBox(font, rightX + colW - 86, ry, 36, 16, Component.empty());
            amt.setValue(String.format(java.util.Locale.ROOT, "%.2f", e.amount));
            amt.setResponder(s -> {
                try { e.amount = Double.parseDouble(s.trim()); saveAttrs(); }
                catch (NumberFormatException ignored) {}
            });
            addRenderableWidget(amt);
            int gone = i;
            addRenderableWidget(Button.builder(Component.literal("\u00A7cX"), b -> {
                attrEntries.remove(gone); saveAttrs(); rebuildWidgets();
            }).bounds(rightX + colW - 46, ry, 22, 16).build());
            // second row: operation + slot
            ry += 18;
            addRenderableWidget(Button.builder(Component.literal("Op: " + OPS[e.op]), b -> {
                e.op = (e.op + 1) % 3; saveAttrs(); rebuildWidgets();
            }).bounds(rightX, ry, (colW - 8) / 2, 16).build());
            addRenderableWidget(Button.builder(Component.literal("Slot: " + e.slot), b -> {
                int idx = 0;
                for (int k = 0; k < SLOTS.length; k++) if (SLOTS[k].equals(e.slot)) { idx = k; break; }
                e.slot = SLOTS[(idx + 1) % SLOTS.length];
                saveAttrs(); rebuildWidgets();
            }).bounds(rightX + (colW - 8) / 2 + 8, ry, (colW - 8) / 2, 16).build());
            ry += 22;
            if (ry > y + bh() - 24) break;
        }
    }

}
