package com.fantastickits.gui;

import com.fantastickits.gui.widget.ScrollSelector;
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
import java.util.Locale;
import java.util.UUID;

/**
 * Full visual NBT editor for a single {@link ItemStack}, opened from the kit editor.
 *
 * <p>Tabs: General (name + colour, CustomModelData, Unbreakable, Damage), Flags
 * (individual vanilla {@code HideFlags} checkboxes), Lore (multi-line, per-line colour),
 * Enchantments (searchable picker + level), Attributes (type, amount, operation, slot).
 * A live preview slot in the header always shows the resulting item with its tooltip on
 * hover. The user never edits raw JSON; everything is serialised to {@link CompoundTag}
 * using only vanilla NBT.</p>
 */
public final class ItemNbtEditorScreen extends Screen {

    private static final String COLOR_CHARS = "f7e6cab9d5234180";
    private static final String[] OPS = {"Sumar", "x base", "x total"};
    private static final String[] SLOTS = {"any", "mainhand", "offhand", "head", "chest", "legs", "feet"};

    /** {@code HideFlags} bit -> label. */
    private static final int[] FLAG_BITS = {1, 2, 4, 8, 16, 32, 64, 128};
    private static final String[] FLAG_LABELS = {
            "Encantamientos", "Modificadores de atributo", "Irrompible (texto)", "CanDestroy",
            "CanPlaceOn", "Otros (efectos, libro...)", "Tinte (cuero)", "Ornamento (trim)"
    };

    private final Screen parent;
    private final ItemStack stack;
    private Tab activeTab = Tab.GENERAL;

    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;

    private int previewX;
    private int previewY;

    private final List<EnchEntry> enchEntries = new ArrayList<>();
    private boolean enchLoaded = false;
    private final List<AttrEntry> attrEntries = new ArrayList<>();
    private boolean attrLoaded = false;

    public ItemNbtEditorScreen(final Screen parent, final ItemStack stack) {
        super(Component.literal("Editor de NBT"));
        this.parent = parent;
        this.stack = stack;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(this.width - 20, 540);
        this.panelHeight = Math.min(this.height - 20, 320);
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;
        this.previewX = this.leftPos + this.panelWidth - 26;
        this.previewY = this.topPos + 24;

        final Tab[] tabs = Tab.values();
        final String[] names = {"General", "Flags", "Lore", "Encantamientos", "Atributos"};
        final int gap = 4;
        final int tabW = (this.panelWidth - 16 - gap * (tabs.length - 1)) / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            final Tab tab = tabs[i];
            final String text = (tab == this.activeTab ? "§f§l" : "§7") + names[i];
            addRenderableWidget(Button.builder(Component.literal(text), b -> {
                this.activeTab = tab;
                rebuildWidgets();
            }).bounds(this.leftPos + 8 + i * (tabW + gap), this.topPos + 24, tabW, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("§aListo"), b -> onClose())
                .bounds(this.leftPos + this.panelWidth - 88, this.topPos + this.panelHeight - 24, 80, 18).build());

        switch (this.activeTab) {
            case GENERAL -> initGeneral();
            case FLAGS -> initFlags();
            case LORE -> initLore();
            case ENCHANTS -> initEnchants();
            case ATTRIBUTES -> initAttributes();
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

    @Override
    public void onClose() {
        if (this.parent != null) {
            this.minecraft.setScreen(this.parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -535291870);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, -14408646);
        g.drawString(this.font, "§d\u2726 §fEditor de NBT: §e" + this.stack.getHoverName().getString(), this.leftPos + 8, this.topPos + 6, 16777215, false);
        g.drawString(this.font, "§7Vista previa en vivo \u2192", this.leftPos + 8, this.topPos + 46, 10133680, false);

        // Live preview slot.
        g.fill(this.previewX - 1, this.previewY - 1, this.previewX + 17, this.previewY + 17, -12961206);
        g.fill(this.previewX, this.previewY, this.previewX + 16, this.previewY + 16, -1072689128);
        g.renderItem(this.stack, this.previewX, this.previewY);
        g.renderItemDecorations(this.font, this.stack, this.previewX, this.previewY);

        super.render(g, mouseX, mouseY, partialTick);

        // Inline hint labels for the GENERAL tab.
        if (this.activeTab == Tab.GENERAL) {
            final int x = bx();
            final int y = by();
            g.drawString(this.font, "§7CustomModelData:", x + 130, y + 32, 14737632, false);
            g.drawString(this.font, "§7Dano (Damage):", x + 130, y + 54, 14737632, false);
            g.drawString(this.font, "§8Las flags vanilla estan en la pestana \"Flags\".", x, y + 78, 10133680, false);
        }

        // Tooltip of the resulting item when hovering the preview slot.
        if (mouseX >= this.previewX && mouseX < this.previewX + 16 && mouseY >= this.previewY && mouseY < this.previewY + 16) {
            g.renderTooltip(this.font, this.stack, mouseX, mouseY);
        }
    }

    // ---- GENERAL ------------------------------------------------------------

    private void initGeneral() {
        final int x = bx();
        final int y = by();

        addRenderableWidget(Button.builder(Component.literal("§" + currentNameColor()), b -> {
            final int idx = COLOR_CHARS.indexOf(currentNameColor());
            final char next = COLOR_CHARS.charAt((idx + 1) % COLOR_CHARS.length());
            applyName(next, stripColor(this.stack.hasCustomHoverName() ? this.stack.getHoverName().getString() : ""));
            rebuildWidgets();
        }).bounds(x, y, 18, 16).build());

        final EditBox name = new EditBox(this.font, x + 22, y, bw() - 22, 16, Component.empty());
        name.setMaxLength(128);
        name.setValue(stripColor(this.stack.hasCustomHoverName() ? this.stack.getHoverName().getString() : ""));
        name.setHint(Component.literal("Nombre personalizado del item"));
        name.setResponder(s -> applyName(currentNameColor(), s));
        addRenderableWidget(name);

        final boolean unbreakable = this.stack.getOrCreateTag().getBoolean("Unbreakable");
        addRenderableWidget(Button.builder(Component.literal((unbreakable ? "§a" : "§7") + "Irrompible: " + (unbreakable ? "Si" : "No")), b -> {
            final boolean now = !this.stack.getOrCreateTag().getBoolean("Unbreakable");
            if (now) {
                this.stack.getOrCreateTag().putBoolean("Unbreakable", true);
            } else {
                this.stack.getOrCreateTag().remove("Unbreakable");
            }
            rebuildWidgets();
        }).bounds(x, y + 28, 200, 16).build());

        final EditBox cmd = new EditBox(this.font, x + 240, y + 28, 60, 16, Component.empty());
        cmd.setMaxLength(8);
        cmd.setValue(this.stack.getOrCreateTag().contains("CustomModelData") ? Integer.toString(this.stack.getOrCreateTag().getInt("CustomModelData")) : "");
        cmd.setHint(Component.literal("CMD"));
        cmd.setResponder(s -> {
            final String t = s.trim();
            if (t.isEmpty()) {
                this.stack.getOrCreateTag().remove("CustomModelData");
            } else {
                try {
                    this.stack.getOrCreateTag().putInt("CustomModelData", Integer.parseInt(t));
                } catch (final NumberFormatException ignored) {
                }
            }
        });
        addRenderableWidget(cmd);

        final EditBox dmg = new EditBox(this.font, x + 240, y + 50, 60, 16, Component.empty());
        dmg.setMaxLength(8);
        dmg.setValue(this.stack.getOrCreateTag().contains("Damage") ? Integer.toString(this.stack.getOrCreateTag().getInt("Damage")) : "");
        dmg.setHint(Component.literal("Dano"));
        dmg.setResponder(s -> {
            final String t = s.trim();
            if (t.isEmpty()) {
                this.stack.getOrCreateTag().remove("Damage");
            } else {
                try {
                    this.stack.getOrCreateTag().putInt("Damage", Integer.parseInt(t));
                } catch (final NumberFormatException ignored) {
                }
            }
        });
        addRenderableWidget(dmg);
    }

    // ---- FLAGS --------------------------------------------------------------

    private void initFlags() {
        final int x = bx();
        final int y = by();
        for (int i = 0; i < FLAG_BITS.length; i++) {
            final int bit = FLAG_BITS[i];
            final boolean on = hasFlag(bit);
            final int row = i;
            addRenderableWidget(Button.builder(
                    Component.literal((on ? "§a\u2714 " : "§7\u2716 ") + "Ocultar: " + FLAG_LABELS[i]), b -> {
                        toggleFlag(bit);
                        rebuildWidgets();
                    }).bounds(x, y + row * 20, bw(), 16).build());
        }
    }

    private boolean hasFlag(final int bit) {
        return (this.stack.getOrCreateTag().getInt("HideFlags") & bit) != 0;
    }

    private void toggleFlag(final int bit) {
        final CompoundTag tag = this.stack.getOrCreateTag();
        int flags = tag.getInt("HideFlags");
        flags ^= bit;
        if (flags == 0) {
            tag.remove("HideFlags");
        } else {
            tag.putInt("HideFlags", flags);
        }
    }

    // ---- name helpers -------------------------------------------------------

    private char currentNameColor() {
        if (!this.stack.hasCustomHoverName()) {
            return 'f';
        }
        final String s = this.stack.getHoverName().getString();
        if (s.length() >= 2 && (s.charAt(0) == '§' || s.charAt(0) == '&') && COLOR_CHARS.indexOf(s.charAt(1)) >= 0) {
            return s.charAt(1);
        }
        return 'f';
    }

    private void applyName(final char color, final String text) {
        final String plain = stripColor(text);
        if (plain.isEmpty()) {
            this.stack.resetHoverName();
            return;
        }
        this.stack.setHoverName(Component.literal("§" + color + plain));
    }

    private static String stripColor(final String s) {
        if (s == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if ((c == '§' || c == '&') && i + 1 < s.length() && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(s.charAt(i + 1)) >= 0) {
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ---- LORE ---------------------------------------------------------------

    private void initLore() {
        final int x = bx();
        final int y = by();
        final int max = 8;
        final ListTag existing = this.stack.getOrCreateTagElement("display").getList("Lore", Tag.TAG_STRING);
        final char[] colors = new char[max];
        final String[] texts = new String[max];
        for (int i = 0; i < max; i++) {
            final String raw = i < existing.size() ? existing.getString(i) : "";
            final String plain = jsonToPlain(raw);
            char color = 'f';
            String text = plain;
            if (plain.length() >= 2 && (plain.charAt(0) == '§' || plain.charAt(0) == '&') && COLOR_CHARS.indexOf(plain.charAt(1)) >= 0) {
                color = plain.charAt(1);
                text = plain.substring(2);
            }
            colors[i] = color;
            texts[i] = text;
        }
        final Runnable sync = () -> {
            final CompoundTag display = this.stack.getOrCreateTagElement("display");
            final ListTag list = new ListTag();
            for (int k = 0; k < max; k++) {
                if (texts[k] != null && !texts[k].isEmpty()) {
                    list.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§" + colors[k] + texts[k]))));
                }
            }
            if (list.isEmpty()) {
                display.remove("Lore");
                if (display.isEmpty()) {
                    this.stack.removeTagKey("display");
                }
            } else {
                display.put("Lore", list);
            }
        };
        for (int j = 0; j < max; j++) {
            final int idx = j;
            final int ry = y + j * 20;
            addRenderableWidget(Button.builder(Component.literal("§" + colors[idx]), b -> {
                final int p = COLOR_CHARS.indexOf(colors[idx]);
                colors[idx] = COLOR_CHARS.charAt((p + 1) % COLOR_CHARS.length());
                sync.run();
                rebuildWidgets();
            }).bounds(x, ry, 18, 16).build());
            final EditBox line = new EditBox(this.font, x + 22, ry, bw() - 22, 16, Component.empty());
            line.setMaxLength(96);
            line.setValue(texts[idx]);
            line.setHint(Component.literal("Linea de lore " + (idx + 1)));
            line.setResponder(s -> {
                texts[idx] = s;
                sync.run();
            });
            addRenderableWidget(line);
        }
    }

    private static String jsonToPlain(final String json) {
        try {
            final Component c = Component.Serializer.fromJson(json);
            return c == null ? json : c.getString();
        } catch (final Exception e) {
            return json;
        }
    }

    // ---- ENCHANTMENTS -------------------------------------------------------

    private void loadEnchants() {
        this.enchEntries.clear();
        final ListTag list = this.stack.getOrCreateTag().getList("Enchantments", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
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
            if (e.id != null && !e.id.isEmpty()) {
                final CompoundTag t = new CompoundTag();
                t.putString("id", e.id);
                t.putShort("lvl", (short) Math.max(0, e.level));
                list.add(t);
            }
        }
        if (list.isEmpty()) {
            this.stack.removeTagKey("Enchantments");
        } else {
            this.stack.getOrCreateTag().put("Enchantments", list);
        }
    }

    private void initEnchants() {
        if (!this.enchLoaded) {
            loadEnchants();
        }
        final int x = bx();
        final int y = by();
        final int colW = (bw() - 8) / 2;
        final int rightX = x + colW + 8;

        final List<ResourceLocation> ids = new ArrayList<>();
        for (final Enchantment enchantment : ForgeRegistries.ENCHANTMENTS.getValues()) {
            final ResourceLocation rl = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            if (rl != null) {
                ids.add(rl);
            }
        }
        ids.sort(Comparator.comparing(ResourceLocation::toString));

        final EditBox search = new EditBox(this.font, x, y, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar encantamiento..."));
        addRenderableWidget(search);
        final ScrollSelector<ResourceLocation> picker = new ScrollSelector<>(x, y + 20, colW, bh() - 22, 14,
                ResourceLocation::getPath, ResourceLocation::toString, rl -> ItemStack.EMPTY);
        picker.setItems(ids);
        picker.onSelect(rl -> {
            final EnchEntry e = new EnchEntry();
            e.id = rl.toString();
            e.level = 1;
            this.enchEntries.add(e);
            saveEnchants();
            rebuildWidgets();
        });
        search.setResponder(picker::setQuery);
        addRenderableWidget(picker);

        int ry = y;
        for (int i = 0; i < this.enchEntries.size(); i++) {
            final EnchEntry e = this.enchEntries.get(i);
            final String pretty = e.id.startsWith("minecraft:") ? e.id.substring(10) : e.id;
            final EditBox idBox = new EditBox(this.font, rightX, ry, colW - 90, 16, Component.empty());
            idBox.setMaxLength(64);
            idBox.setValue(pretty);
            idBox.setResponder(s -> {
                e.id = s.contains(":") ? s : ("minecraft:" + s);
                saveEnchants();
            });
            addRenderableWidget(idBox);
            final EditBox lvl = new EditBox(this.font, rightX + colW - 86, ry, 36, 16, Component.empty());
            lvl.setValue(Integer.toString(e.level));
            lvl.setResponder(s -> {
                try {
                    e.level = Integer.parseInt(s.trim());
                    saveEnchants();
                } catch (final NumberFormatException ignored) {
                }
            });
            addRenderableWidget(lvl);
            final int gone = i;
            addRenderableWidget(Button.builder(Component.literal("§cX"), b -> {
                this.enchEntries.remove(gone);
                saveEnchants();
                rebuildWidgets();
            }).bounds(rightX + colW - 46, ry, 22, 16).build());
            ry += 18;
            if (ry > y + bh() - 22) {
                break;
            }
        }
    }

    // ---- ATTRIBUTES ---------------------------------------------------------

    private void loadAttrs() {
        this.attrEntries.clear();
        final ListTag list = this.stack.getOrCreateTag().getList("AttributeModifiers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag t = list.getCompound(i);
            final AttrEntry e = new AttrEntry();
            e.id = t.getString("AttributeName");
            e.amount = t.getDouble("Amount");
            e.op = t.getInt("Operation");
            e.slot = t.contains("Slot") ? t.getString("Slot") : "mainhand";
            this.attrEntries.add(e);
        }
        this.attrLoaded = true;
    }

    private void saveAttrs() {
        final ListTag list = new ListTag();
        for (final AttrEntry e : this.attrEntries) {
            if (e.id != null && !e.id.isEmpty()) {
                final CompoundTag t = new CompoundTag();
                t.putString("AttributeName", e.id);
                t.putString("Name", "fantastickits");
                t.putDouble("Amount", e.amount);
                t.putInt("Operation", Math.max(0, Math.min(2, e.op)));
                if (!"any".equals(e.slot)) {
                    t.putString("Slot", e.slot);
                }
                final UUID u = UUID.randomUUID();
                t.putIntArray("UUID", new int[]{
                        (int) (u.getMostSignificantBits() >> 32),
                        (int) (u.getMostSignificantBits() & 0xFFFFFFFFL),
                        (int) (u.getLeastSignificantBits() >> 32),
                        (int) (u.getLeastSignificantBits() & 0xFFFFFFFFL)
                });
                list.add(t);
            }
        }
        if (list.isEmpty()) {
            this.stack.removeTagKey("AttributeModifiers");
        } else {
            this.stack.getOrCreateTag().put("AttributeModifiers", list);
        }
    }

    private void initAttributes() {
        if (!this.attrLoaded) {
            loadAttrs();
        }
        final int x = bx();
        final int y = by();
        final int colW = (bw() - 8) / 2;
        final int rightX = x + colW + 8;

        final List<ResourceLocation> ids = new ArrayList<>();
        for (final Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            final ResourceLocation rl = ForgeRegistries.ATTRIBUTES.getKey(attribute);
            if (rl != null) {
                ids.add(rl);
            }
        }
        ids.sort(Comparator.comparing(ResourceLocation::toString));

        final EditBox search = new EditBox(this.font, x, y, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar atributo..."));
        addRenderableWidget(search);
        final ScrollSelector<ResourceLocation> picker = new ScrollSelector<>(x, y + 20, colW, bh() - 22, 14,
                ResourceLocation::getPath, ResourceLocation::toString, rl -> ItemStack.EMPTY);
        picker.setItems(ids);
        picker.onSelect(rl -> {
            final AttrEntry e = new AttrEntry();
            e.id = rl.toString();
            e.amount = 1.0;
            e.op = 0;
            e.slot = "mainhand";
            this.attrEntries.add(e);
            saveAttrs();
            rebuildWidgets();
        });
        search.setResponder(picker::setQuery);
        addRenderableWidget(picker);

        int ry = y;
        for (int i = 0; i < this.attrEntries.size(); i++) {
            final AttrEntry e = this.attrEntries.get(i);
            final String pretty = e.id.startsWith("minecraft:") ? e.id.substring(10) : e.id;
            final EditBox idBox = new EditBox(this.font, rightX, ry, colW - 90, 16, Component.empty());
            idBox.setMaxLength(96);
            idBox.setValue(pretty);
            idBox.setResponder(s -> {
                e.id = s.contains(":") ? s : ("minecraft:" + s);
                saveAttrs();
            });
            addRenderableWidget(idBox);
            final EditBox amt = new EditBox(this.font, rightX + colW - 86, ry, 36, 16, Component.empty());
            amt.setValue(String.format(Locale.ROOT, "%.2f", e.amount));
            amt.setResponder(s -> {
                try {
                    e.amount = Double.parseDouble(s.trim());
                    saveAttrs();
                } catch (final NumberFormatException ignored) {
                }
            });
            addRenderableWidget(amt);
            final int gone = i;
            addRenderableWidget(Button.builder(Component.literal("§cX"), b -> {
                this.attrEntries.remove(gone);
                saveAttrs();
                rebuildWidgets();
            }).bounds(rightX + colW - 46, ry, 22, 16).build());
            ry += 18;
            addRenderableWidget(Button.builder(Component.literal("Op: " + OPS[Math.max(0, Math.min(2, e.op))]), b -> {
                e.op = (e.op + 1) % 3;
                saveAttrs();
                rebuildWidgets();
            }).bounds(rightX, ry, (colW - 8) / 2, 16).build());
            addRenderableWidget(Button.builder(Component.literal("Slot: " + e.slot), b -> {
                int idx = 0;
                for (int k = 0; k < SLOTS.length; k++) {
                    if (SLOTS[k].equals(e.slot)) {
                        idx = k;
                        break;
                    }
                }
                e.slot = SLOTS[(idx + 1) % SLOTS.length];
                saveAttrs();
                rebuildWidgets();
            }).bounds(rightX + (colW - 8) / 2 + 8, ry, (colW - 8) / 2, 16).build());
            ry += 22;
            if (ry > y + bh() - 24) {
                break;
            }
        }
    }

    private enum Tab {
        GENERAL, FLAGS, LORE, ENCHANTS, ATTRIBUTES
    }

    private static final class EnchEntry {
        String id = "minecraft:sharpness";
        int level = 1;
    }

    private static final class AttrEntry {
        String id = "minecraft:generic.attack_damage";
        double amount = 1.0;
        int op = 0;
        String slot = "mainhand";
    }
}
