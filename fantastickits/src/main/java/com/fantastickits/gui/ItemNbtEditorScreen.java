package com.fantastickits.gui;

import com.fantastickits.gui.widget.ScrollSelector;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
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
 * Editor visual completo de NBT para un {@link ItemStack}, en espanol.
 *
 * <p>Pestanas: General (nombre con color de paleta, color personalizado #RRGGBB y
 * formato negrita/cursiva/subrayado/tachado/magico, CustomModelData, Irrompible, Dano),
 * Flags (cada {@code HideFlag} vanilla), Lore (multilinea con color por linea y codigos
 * & de formato), Encantamientos y Atributos (nombres en espanol). Un recuadro de vista
 * previa en la barra de titulo y una linea de previsualizacion del texto coloreado
 * muestran el resultado al instante. Todo se serializa a NBT vanilla.</p>
 */
public final class ItemNbtEditorScreen extends Screen {

    private static final String COLOR_CHARS = "f7e6cab9d5234180";
    private static final String SWATCH = "\u25a0"; // cuadrito ■
    private static final String FORMAT_CHARS = "0123456789abcdefklmnorABCDEFKLMNOR";
    private static final String[] OPS = {"Sumar", "x base", "x total"};
    private static final String[] SLOTS = {"any", "mainhand", "offhand", "head", "chest", "legs", "feet"};

    private static final int[] FLAG_BITS = {1, 2, 4, 8, 16, 32, 64, 128};
    private static final String[] FLAG_LABELS = {
            "Encantamientos", "Modificadores de atributo", "Irrompible (texto)", "CanDestroy",
            "CanPlaceOn", "Otros (efectos, libro...)", "Tinte (cuero)", "Ornamento (trim)"
    };
    private static final int LORE_LINES = 8;

    private final Screen parent;
    private final ItemStack stack;
    private Tab activeTab = Tab.GENERAL;

    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;
    private int previewX;
    private int previewY;

    private final List<Label> labels = new ArrayList<>();

    // Estado del nombre (se carga una vez del stack).
    private boolean nameLoaded = false;
    private String nameText = "";
    private char nameColor = 'f';
    private String nameHex = "";
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean strike;
    private boolean obfuscated;

    // Previsualizacion del lore (cadena con codigos § por linea) para dibujar en vivo.
    private final String[] lorePreview = new String[LORE_LINES];

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
        this.previewX = this.leftPos + this.panelWidth - 22;
        this.previewY = this.topPos + 2;
        this.labels.clear();

        if (!this.nameLoaded) {
            loadName();
        }

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
        return this.topPos + 52;
    }

    private int bw() {
        return this.panelWidth - 24;
    }

    private int bh() {
        return this.panelHeight - 52 - 28;
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
        g.fill(this.leftPos + 6, this.topPos + 44, this.leftPos + this.panelWidth - 6, this.topPos + 45, -12961206);

        final String title = this.font.plainSubstrByWidth("§d\u2726 §fEditor de NBT: §e" + this.stack.getHoverName().getString(), this.panelWidth - 34);
        g.drawString(this.font, title, this.leftPos + 8, this.topPos + 6, 16777215, false);

        // Recuadro de vista previa en la barra de titulo (despejado de la fila de pestanas).
        g.fill(this.previewX - 1, this.previewY - 1, this.previewX + 17, this.previewY + 17, -12961206);
        g.fill(this.previewX, this.previewY, this.previewX + 16, this.previewY + 16, -1072689128);
        g.renderItem(this.stack, this.previewX, this.previewY);
        g.renderItemDecorations(this.font, this.stack, this.previewX, this.previewY);

        super.render(g, mouseX, mouseY, partialTick);

        for (final Label l : this.labels) {
            final String trimmed = this.font.plainSubstrByWidth(l.text(), this.leftPos + this.panelWidth - 6 - l.x());
            g.drawString(this.font, trimmed, l.x(), l.y(), 14737632, false);
        }

        if (this.activeTab == Tab.GENERAL) {
            renderNamePreview(g);
        } else if (this.activeTab == Tab.LORE) {
            renderLorePreview(g);
        }

        if (mouseX >= this.previewX && mouseX < this.previewX + 16 && mouseY >= this.previewY && mouseY < this.previewY + 16) {
            g.renderTooltip(this.font, this.stack, mouseX, mouseY);
        }
    }

    private void renderNamePreview(final GuiGraphics g) {
        final int x = bx();
        final int y = by();
        g.drawString(this.font, "§7Vista previa:", x, y + 134, 10133680, false);
        final String preview = this.font.plainSubstrByWidth(this.stack.getHoverName().getString(), bw() - 4);
        g.drawString(this.font, preview, x, y + 146, 16777215, false);
    }

    private void renderLorePreview(final GuiGraphics g) {
        final int x = bx();
        final int y = by();
        final int boxW = bw() - 22 - 130;
        final int previewColX = x + 22 + boxW + 8;
        for (int i = 0; i < LORE_LINES; i++) {
            if (this.lorePreview[i] != null && !this.lorePreview[i].isEmpty()) {
                final int ry = y + 14 + i * 19;
                final String preview = this.font.plainSubstrByWidth(this.lorePreview[i], this.leftPos + this.panelWidth - 8 - previewColX);
                g.drawString(this.font, preview, previewColX, ry + 4, 16777215, false);
            }
        }
    }

    private void addLabel(final String text, final int x, final int y) {
        this.labels.add(new Label(text, x, y));
    }

    // ---- nombre: color + formato --------------------------------------------

    private void loadName() {
        if (this.stack.hasCustomHoverName()) {
            final Component hover = this.stack.getHoverName();
            this.nameText = stripColor(hover.getString());
            final Style style = hover.getStyle();
            final TextColor color = style.getColor();
            if (color != null) {
                this.nameHex = String.format("#%06X", color.getValue() & 0xFFFFFF);
                this.nameColor = 'f';
            } else {
                this.nameColor = legacyColorOf(hover.getString());
                this.nameHex = "";
            }
            this.bold = style.isBold();
            this.italic = style.isItalic();
            this.underline = style.isUnderlined();
            this.strike = style.isStrikethrough();
            this.obfuscated = style.isObfuscated();
        }
        this.nameLoaded = true;
    }

    private void applyName() {
        if (this.nameText == null || this.nameText.isEmpty()) {
            this.stack.resetHoverName();
            return;
        }
        final int rgb = isValidHex(this.nameHex) ? Integer.parseInt(this.nameHex.substring(1), 16) : legacyRgb(this.nameColor);
        final Style style = Style.EMPTY
                .withColor(TextColor.fromRgb(rgb))
                .withBold(this.bold)
                .withItalic(this.italic)
                .withUnderlined(this.underline)
                .withStrikethrough(this.strike)
                .withObfuscated(this.obfuscated);
        this.stack.setHoverName(Component.literal(this.nameText).setStyle(style));
    }

    private static int legacyRgb(final char code) {
        final ChatFormatting cf = ChatFormatting.getByCode(code);
        if (cf != null && cf.getColor() != null) {
            return cf.getColor();
        }
        return 0xFFFFFF;
    }

    private static boolean isValidHex(final String hex) {
        if (hex == null || hex.length() != 7 || hex.charAt(0) != '#') {
            return false;
        }
        for (int i = 1; i < 7; i++) {
            if (Character.digit(hex.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static char legacyColorOf(final String s) {
        if (s.length() >= 2 && (s.charAt(0) == '§' || s.charAt(0) == '&') && COLOR_CHARS.indexOf(s.charAt(1)) >= 0) {
            return s.charAt(1);
        }
        return 'f';
    }

    private static String stripColor(final String s) {
        if (s == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if ((c == '§' || c == '&') && i + 1 < s.length() && FORMAT_CHARS.indexOf(s.charAt(i + 1)) >= 0) {
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Convierte codigos & validos en § (para que el lore acepte formato escrito a mano). */
    private static String amp(final String s) {
        if (s == null) {
            return "";
        }
        final char[] a = s.toCharArray();
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i] == '&' && FORMAT_CHARS.indexOf(a[i + 1]) >= 0) {
                a[i] = '§';
            }
        }
        return new String(a);
    }

    // ---- GENERAL ------------------------------------------------------------

    private void initGeneral() {
        final int x = bx();
        final int y = by();

        // Cuadrito de color: muestra el color ACTUAL como cuadro de ese color.
        final String swatchColor = isValidHex(this.nameHex) ? "§f" : ("§" + this.nameColor);
        addRenderableWidget(Button.builder(Component.literal(swatchColor + SWATCH), b -> {
            final int idx = COLOR_CHARS.indexOf(this.nameColor);
            this.nameColor = COLOR_CHARS.charAt((idx + 1) % COLOR_CHARS.length());
            this.nameHex = "";
            applyName();
            rebuildWidgets();
        }).bounds(x, y, 18, 16).build());

        final EditBox name = new EditBox(this.font, x + 22, y, bw() - 22, 16, Component.empty());
        name.setMaxLength(128);
        name.setValue(this.nameText);
        name.setHint(Component.literal("Nombre personalizado del item"));
        name.setResponder(s -> {
            this.nameText = s;
            applyName();
        });
        addRenderableWidget(name);

        // Formato: negrita / cursiva / subrayado / tachado / magico.
        final int fbW = (bw() - 4 * 4) / 5;
        int fx = x;
        addFormatToggle(fx, y + 22, fbW, "§lN", this.bold, () -> this.bold = !this.bold);
        fx += fbW + 4;
        addFormatToggle(fx, y + 22, fbW, "§oC", this.italic, () -> this.italic = !this.italic);
        fx += fbW + 4;
        addFormatToggle(fx, y + 22, fbW, "§nS", this.underline, () -> this.underline = !this.underline);
        fx += fbW + 4;
        addFormatToggle(fx, y + 22, fbW, "§mT", this.strike, () -> this.strike = !this.strike);
        fx += fbW + 4;
        addFormatToggle(fx, y + 22, fbW, "§kM", this.obfuscated, () -> this.obfuscated = !this.obfuscated);
        addLabel("§8N negrita · C cursiva · S subrayado · T tachado · M mágico", x, y + 44);

        addLabel("§7Color personalizado:", x, y + 64);
        final EditBox hex = new EditBox(this.font, x + 130, y + 60, 80, 16, Component.empty());
        hex.setMaxLength(7);
        hex.setValue(this.nameHex);
        hex.setHint(Component.literal("#RRGGBB"));
        hex.setResponder(s -> {
            this.nameHex = s.trim();
            applyName();
        });
        addRenderableWidget(hex);
        addLabel("§8(vacío = usa el color del cuadrito)", x + 216, y + 64);

        final boolean unbreakable = this.stack.getOrCreateTag().getBoolean("Unbreakable");
        addRenderableWidget(Button.builder(Component.literal((unbreakable ? "§a" : "§7") + "Irrompible: " + (unbreakable ? "Sí" : "No")), b -> {
            if (this.stack.getOrCreateTag().getBoolean("Unbreakable")) {
                this.stack.getOrCreateTag().remove("Unbreakable");
            } else {
                this.stack.getOrCreateTag().putBoolean("Unbreakable", true);
            }
            rebuildWidgets();
        }).bounds(x, y + 84, 150, 16).build());

        addLabel("§7CustomModelData:", x + 160, y + 88);
        final EditBox cmd = new EditBox(this.font, x + 270, y + 84, 60, 16, Component.empty());
        cmd.setMaxLength(8);
        cmd.setValue(this.stack.getOrCreateTag().contains("CustomModelData") ? Integer.toString(this.stack.getOrCreateTag().getInt("CustomModelData")) : "");
        cmd.setHint(Component.literal("0"));
        cmd.setResponder(s -> writeIntTag("CustomModelData", s));
        addRenderableWidget(cmd);

        addLabel("§7Daño (Damage):", x, y + 110);
        final EditBox dmg = new EditBox(this.font, x + 130, y + 106, 60, 16, Component.empty());
        dmg.setMaxLength(8);
        dmg.setValue(this.stack.getOrCreateTag().contains("Damage") ? Integer.toString(this.stack.getOrCreateTag().getInt("Damage")) : "");
        dmg.setHint(Component.literal("0"));
        dmg.setResponder(s -> writeIntTag("Damage", s));
        addRenderableWidget(dmg);
    }

    private void addFormatToggle(final int x, final int y, final int w, final String formattedLetter, final boolean on, final Runnable toggle) {
        final String bg = on ? "§a" : "§7";
        addRenderableWidget(Button.builder(Component.literal(bg + formattedLetter), b -> {
            toggle.run();
            applyName();
            rebuildWidgets();
        }).bounds(x, y, w, 16).build());
    }

    private void writeIntTag(final String key, final String raw) {
        final String t = raw.trim();
        if (t.isEmpty()) {
            this.stack.getOrCreateTag().remove(key);
            return;
        }
        try {
            this.stack.getOrCreateTag().putInt(key, Integer.parseInt(t));
        } catch (final NumberFormatException ignored) {
        }
    }

    // ---- FLAGS --------------------------------------------------------------

    private void initFlags() {
        final int x = bx();
        final int y = by();
        addLabel("§7Marca qué información vanilla ocultar en el ítem:", x, y);
        for (int i = 0; i < FLAG_BITS.length; i++) {
            final int bit = FLAG_BITS[i];
            final boolean on = hasFlag(bit);
            final int row = i;
            addRenderableWidget(Button.builder(
                    Component.literal((on ? "§a\u2714 " : "§7\u2716 ") + "Ocultar: " + FLAG_LABELS[i]), b -> {
                        toggleFlag(bit);
                        rebuildWidgets();
                    }).bounds(x, y + 14 + row * 18, bw(), 16).build());
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

    // ---- LORE ---------------------------------------------------------------

    private void initLore() {
        final int x = bx();
        final int y = by();
        final ListTag existing = this.stack.getOrCreateTagElement("display").getList("Lore", Tag.TAG_STRING);
        final char[] colors = new char[LORE_LINES];
        final String[] texts = new String[LORE_LINES];
        for (int i = 0; i < LORE_LINES; i++) {
            final String raw = i < existing.size() ? existing.getString(i) : "";
            final String plain = jsonToPlain(raw).replace('§', '&');
            char color = 'f';
            String text = plain;
            if (plain.length() >= 2 && plain.charAt(0) == '&' && COLOR_CHARS.indexOf(plain.charAt(1)) >= 0) {
                color = plain.charAt(1);
                text = plain.substring(2);
            }
            colors[i] = color;
            texts[i] = text;
            this.lorePreview[i] = "§" + color + amp(text);
        }
        final Runnable sync = () -> {
            final CompoundTag display = this.stack.getOrCreateTagElement("display");
            final ListTag list = new ListTag();
            for (int k = 0; k < LORE_LINES; k++) {
                this.lorePreview[k] = (texts[k] == null || texts[k].isEmpty()) ? "" : ("§" + colors[k] + amp(texts[k]));
                if (texts[k] != null && !texts[k].isEmpty()) {
                    list.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§" + colors[k] + amp(texts[k])))));
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

        addLabel("§7Cuadrito = color de la línea. Escribe &l, &o... para más formato:", x, y);
        final int boxW = bw() - 22 - 130;
        for (int j = 0; j < LORE_LINES; j++) {
            final int idx = j;
            final int ry = y + 14 + j * 19;
            addRenderableWidget(Button.builder(Component.literal("§" + colors[idx] + SWATCH), b -> {
                final int p = COLOR_CHARS.indexOf(colors[idx]);
                colors[idx] = COLOR_CHARS.charAt((p + 1) % COLOR_CHARS.length());
                sync.run();
                rebuildWidgets();
            }).bounds(x, ry, 18, 16).build());
            final EditBox line = new EditBox(this.font, x + 22, ry, boxW, 16, Component.empty());
            line.setMaxLength(128);
            line.setValue(texts[idx]);
            line.setHint(Component.literal("Línea " + (idx + 1)));
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

    // ---- ENCANTAMIENTOS -----------------------------------------------------

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
        ids.sort(Comparator.comparing(Translations::enchantment, String.CASE_INSENSITIVE_ORDER));

        addLabel("§7Buscar y añadir:", x, y);
        final EditBox search = new EditBox(this.font, x, y + 12, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar encantamiento..."));
        addRenderableWidget(search);
        final ScrollSelector<ResourceLocation> picker = new ScrollSelector<>(x, y + 32, colW, bh() - 34, 14,
                Translations::enchantment,
                rl -> Translations.enchantment(rl) + " " + rl,
                rl -> ItemStack.EMPTY);
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

        addLabel("§7Asignados (nombre / nivel):", rightX, y);
        int ry = y + 14;
        for (int i = 0; i < this.enchEntries.size(); i++) {
            final EnchEntry e = this.enchEntries.get(i);
            final String name = Translations.enchantment(ResourceLocation.tryParse(e.id));
            addLabel("§f" + this.font.plainSubstrByWidth(name, colW - 92), rightX, ry + 4);
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
            if (ry > y + bh() - 18) {
                break;
            }
        }
    }

    // ---- ATRIBUTOS ----------------------------------------------------------

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
        ids.sort(Comparator.comparing(Translations::attribute, String.CASE_INSENSITIVE_ORDER));

        addLabel("§7Buscar y añadir:", x, y);
        final EditBox search = new EditBox(this.font, x, y + 12, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar atributo..."));
        addRenderableWidget(search);
        final ScrollSelector<ResourceLocation> picker = new ScrollSelector<>(x, y + 32, colW, bh() - 34, 14,
                Translations::attribute,
                rl -> Translations.attribute(rl) + " " + rl,
                rl -> ItemStack.EMPTY);
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

        addLabel("§7Asignados:", rightX, y);
        int ry = y + 14;
        for (int i = 0; i < this.attrEntries.size(); i++) {
            final AttrEntry e = this.attrEntries.get(i);
            final String name = Translations.attribute(ResourceLocation.tryParse(e.id));
            addLabel("§f" + this.font.plainSubstrByWidth(name, colW - 92), rightX, ry + 4);
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

    private record Label(String text, int x, int y) {
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
