package com.fshop.client.screen;

import com.fshop.client.widget.ScrollSelector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
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

/**
 * Editor visual completo de NBT para un item, en español: nombre con color de
 * paleta o hex #RRGGBB y formato; lore multilínea con color por línea (paleta o
 * hex) y códigos & de formato; ocultar flags vanilla; encantamientos y atributos
 * con nombres en español. Vista previa del item (con lore) al pasar el ratón por
 * el icono de la barra de título. Edita el ItemStack recibido en el sitio.
 */
public final class NbtEditorScreen extends Screen {
   private static final String COLORS = "f7e6cab9d5234180";
   private static final String SWATCH = "\u25a0";
   private static final String FORMAT_CHARS = "0123456789abcdefklmnorABCDEFKLMNOR";
   private static final String[] OPS = {"Suma fija", "% base", "% total"};
   private static final String[] SLOTS = {"any", "mainhand", "offhand", "head", "chest", "legs", "feet"};
   private static final int LORE_LINES = 7;

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
      UUID uuid = null;
   }

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
   private int panelW;
   private int panelH;
   private int previewX;
   private int previewY;
   private final List<Label> labels = new ArrayList<>();

   private boolean nameLoaded;
   private String nameText = "";
   private char nameColor = 'f';
   private String nameHex = "";
   private boolean bold;
   private boolean italic;
   private boolean underline;
   private boolean strike;
   private boolean obfuscated;

   private final List<EnchEntry> enchEntries = new ArrayList<>();
   private boolean enchLoaded;
   private final List<AttrEntry> attrEntries = new ArrayList<>();
   private boolean attrLoaded;

   public NbtEditorScreen(Screen parent, ItemStack stack) {
      super(Component.literal("Editor de NBT"));
      this.parent = parent;
      this.stack = stack;
   }

   @Override
   protected void init() {
      this.panelW = Math.min(this.width - 20, 540);
      this.panelH = Math.min(this.height - 20, 320);
      this.leftPos = (this.width - this.panelW) / 2;
      this.topPos = (this.height - this.panelH) / 2;
      this.previewX = this.leftPos + this.panelW - 22;
      this.previewY = this.topPos + 2;
      this.labels.clear();
      if (!this.nameLoaded) {
         loadName();
      }
      Tab[] tabs = Tab.values();
      String[] names = {"General", "Flags", "Lore", "Encantamientos", "Atributos"};
      int gap = 4;
      int tabW = (this.panelW - 16 - gap * (tabs.length - 1)) / tabs.length;
      for (int i = 0; i < tabs.length; i++) {
         Tab tab = tabs[i];
         String text = (tab == this.activeTab ? "\u00a7f\u00a7l" : "\u00a77") + names[i];
         addRenderableWidget(Button.builder(Component.literal(text), b -> {
            this.activeTab = tab;
            rebuildWidgets();
         }).bounds(this.leftPos + 8 + i * (tabW + gap), this.topPos + 24, tabW, 18).build());
      }
      addRenderableWidget(Button.builder(Component.literal("\u00a7aListo"), b -> onClose())
            .bounds(this.leftPos + this.panelW - 88, this.topPos + this.panelH - 24, 80, 18).build());
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
      return this.panelW - 24;
   }

   private int bh() {
      return this.panelH - 52 - 28;
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
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      renderBackground(g);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelW, this.topPos + this.panelH, 0xE01A1A1A);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelW, this.topPos + 20, 0xFF2A2A2A);
      g.fill(this.leftPos + 6, this.topPos + 44, this.leftPos + this.panelW - 6, this.topPos + 45, 0xFF3A3A3A);
      String title = this.font.plainSubstrByWidth("\u00a7d\u2726 \u00a7fEditor de NBT: \u00a7e"
            + this.stack.getHoverName().getString(), this.panelW - 34);
      g.drawString(this.font, title, this.leftPos + 8, this.topPos + 6, 0xFFFFFF, false);
      g.fill(this.previewX - 1, this.previewY - 1, this.previewX + 17, this.previewY + 17, 0xFF3A3A3A);
      g.fill(this.previewX, this.previewY, this.previewX + 16, this.previewY + 16, 0xC01A1A1A);
      g.renderFakeItem(this.stack, this.previewX, this.previewY);
      super.render(g, mouseX, mouseY, partial);
      for (Label l : this.labels) {
         String trimmed = this.font.plainSubstrByWidth(l.text(), this.leftPos + this.panelW - 6 - l.x());
         g.drawString(this.font, trimmed, l.x(), l.y(), 0xE0E0E0, false);
      }
      if (this.activeTab == Tab.GENERAL) {
         g.drawString(this.font, "\u00a77Vista previa:", bx(), by() + 134, 0x9AC0F0, false);
         g.drawString(this.font, this.stack.getHoverName(), bx(), by() + 146, 0xFFFFFF, false);
      }
      if (mouseX >= this.previewX && mouseX < this.previewX + 16 && mouseY >= this.previewY && mouseY < this.previewY + 16) {
         g.renderTooltip(this.font, this.stack, mouseX, mouseY);
      }
   }

   private void addLabel(String text, int x, int y) {
      this.labels.add(new Label(text, x, y));
   }

   // ---- nombre --------------------------------------------------------------

   private void loadName() {
      if (this.stack.hasCustomHoverName()) {
         Component hover = this.stack.getHoverName();
         this.nameText = stripColor(hover.getString());
         Style style = hover.getStyle();
         TextColor color = style.getColor();
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
      int rgb = isValidHex(this.nameHex) ? Integer.parseInt(this.nameHex.substring(1), 16) : legacyRgb(this.nameColor);
      Style style = Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(this.bold).withItalic(this.italic)
            .withUnderlined(this.underline).withStrikethrough(this.strike).withObfuscated(this.obfuscated);
      this.stack.setHoverName(Component.literal(this.nameText).setStyle(style));
   }

   private static int legacyRgb(char code) {
      ChatFormatting cf = ChatFormatting.getByCode(code);
      return cf != null && cf.getColor() != null ? cf.getColor() : 0xFFFFFF;
   }

   private static boolean isValidHex(String hex) {
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

   private static char legacyColorOf(String s) {
      if (s.length() >= 2 && (s.charAt(0) == '\u00a7' || s.charAt(0) == '&') && COLORS.indexOf(s.charAt(1)) >= 0) {
         return s.charAt(1);
      }
      return 'f';
   }

   private static String stripColor(String s) {
      if (s == null) {
         return "";
      }
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if ((c == '\u00a7' || c == '&') && i + 1 < s.length() && FORMAT_CHARS.indexOf(s.charAt(i + 1)) >= 0) {
            i++;
         } else {
            sb.append(c);
         }
      }
      return sb.toString();
   }

   private static String amp(String s) {
      if (s == null) {
         return "";
      }
      char[] a = s.toCharArray();
      for (int i = 0; i < a.length - 1; i++) {
         if (a[i] == '&' && FORMAT_CHARS.indexOf(a[i + 1]) >= 0) {
            a[i] = '\u00a7';
         }
      }
      return new String(a);
   }

   private void initGeneral() {
      int x = bx();
      int y = by();
      String swatchColor = isValidHex(this.nameHex) ? "\u00a7f" : ("\u00a7" + this.nameColor);
      addRenderableWidget(Button.builder(Component.literal(swatchColor + SWATCH), b -> {
         int idx = COLORS.indexOf(this.nameColor);
         this.nameColor = COLORS.charAt((idx + 1) % COLORS.length());
         this.nameHex = "";
         applyName();
         rebuildWidgets();
      }).tooltip(Tooltip.create(Component.literal("Color del nombre (paleta). Clic para cambiar."))).bounds(x, y, 18, 16).build());
      EditBox name = new EditBox(this.font, x + 22, y, bw() - 22, 16, Component.empty());
      name.setMaxLength(128);
      name.setValue(this.nameText);
      name.setHint(Component.literal("Nombre personalizado del item"));
      name.setResponder(s -> {
         this.nameText = s;
         applyName();
      });
      addRenderableWidget(name);

      int fbW = (bw() - 4 * 4) / 5;
      int fx = x;
      addFormatToggle(fx, y + 22, fbW, "\u00a7lN", this.bold, () -> this.bold = !this.bold);
      fx += fbW + 4;
      addFormatToggle(fx, y + 22, fbW, "\u00a7oC", this.italic, () -> this.italic = !this.italic);
      fx += fbW + 4;
      addFormatToggle(fx, y + 22, fbW, "\u00a7nS", this.underline, () -> this.underline = !this.underline);
      fx += fbW + 4;
      addFormatToggle(fx, y + 22, fbW, "\u00a7mT", this.strike, () -> this.strike = !this.strike);
      fx += fbW + 4;
      addFormatToggle(fx, y + 22, fbW, "\u00a7kM", this.obfuscated, () -> this.obfuscated = !this.obfuscated);
      addLabel("\u00a78N negrita \u00b7 C cursiva \u00b7 S subrayado \u00b7 T tachado \u00b7 M m\u00e1gico", x, y + 44);

      addLabel("\u00a77Color personalizado:", x, y + 64);
      EditBox hex = new EditBox(this.font, x + 130, y + 60, 80, 16, Component.empty());
      hex.setMaxLength(7);
      hex.setValue(this.nameHex);
      hex.setHint(Component.literal("#RRGGBB"));
      hex.setResponder(s -> {
         this.nameHex = s.trim();
         applyName();
      });
      addRenderableWidget(hex);
      addLabel("\u00a78(vac\u00edo = usa el color del cuadrito)", x + 216, y + 64);

      boolean unbreakable = this.stack.getOrCreateTag().getBoolean("Unbreakable");
      addRenderableWidget(Button.builder(
            Component.literal((unbreakable ? "\u00a7a" : "\u00a77") + "Irrompible: " + (unbreakable ? "S\u00ed" : "No")), b -> {
               if (this.stack.getOrCreateTag().getBoolean("Unbreakable")) {
                  this.stack.getOrCreateTag().remove("Unbreakable");
               } else {
                  this.stack.getOrCreateTag().putBoolean("Unbreakable", true);
               }
               rebuildWidgets();
            }).bounds(x, y + 84, 150, 16).build());

      addLabel("\u00a77CustomModelData:", x + 160, y + 88);
      EditBox cmd = new EditBox(this.font, x + 270, y + 84, 60, 16, Component.empty());
      cmd.setMaxLength(8);
      cmd.setValue(this.stack.getOrCreateTag().contains("CustomModelData")
            ? Integer.toString(this.stack.getOrCreateTag().getInt("CustomModelData")) : "");
      cmd.setHint(Component.literal("0"));
      cmd.setResponder(s -> writeIntTag("CustomModelData", s));
      addRenderableWidget(cmd);

      addLabel("\u00a77Da\u00f1o (Damage):", x, y + 110);
      EditBox dmg = new EditBox(this.font, x + 130, y + 106, 60, 16, Component.empty());
      dmg.setMaxLength(8);
      dmg.setValue(this.stack.getOrCreateTag().contains("Damage")
            ? Integer.toString(this.stack.getOrCreateTag().getInt("Damage")) : "");
      dmg.setHint(Component.literal("0"));
      dmg.setResponder(s -> writeIntTag("Damage", s));
      addRenderableWidget(dmg);
   }

   private void addFormatToggle(int x, int y, int w, String letter, boolean on, Runnable toggle) {
      String bg = on ? "\u00a7a" : "\u00a77";
      addRenderableWidget(Button.builder(Component.literal(bg + letter), b -> {
         toggle.run();
         applyName();
         rebuildWidgets();
      }).bounds(x, y, w, 16).build());
   }

   private void writeIntTag(String key, String raw) {
      String t = raw.trim();
      if (t.isEmpty()) {
         this.stack.getOrCreateTag().remove(key);
         return;
      }
      try {
         this.stack.getOrCreateTag().putInt(key, Integer.parseInt(t));
      } catch (NumberFormatException ignored) {
      }
   }

   private void initFlags() {
      int x = bx();
      int y = by();
      addLabel("\u00a77Marca qu\u00e9 informaci\u00f3n vanilla ocultar en el \u00edtem:", x, y);
      for (int i = 0; i < FLAG_BITS.length; i++) {
         int bit = FLAG_BITS[i];
         boolean on = (this.stack.getOrCreateTag().getInt("HideFlags") & bit) != 0;
         int row = i;
         addRenderableWidget(Button.builder(
               Component.literal((on ? "\u00a7a\u2714 " : "\u00a77\u2716 ") + "Ocultar: " + FLAG_LABELS[i]), b -> {
                  toggleFlag(bit);
                  rebuildWidgets();
               }).bounds(x, y + 14 + row * 18, bw(), 16).build());
      }
   }

   private void toggleFlag(int bit) {
      CompoundTag tag = this.stack.getOrCreateTag();
      int flags = tag.getInt("HideFlags") ^ bit;
      if (flags == 0) {
         tag.remove("HideFlags");
      } else {
         tag.putInt("HideFlags", flags);
      }
   }

   // ---- LORE con color por paleta o hex por linea ---------------------------

   private void initLore() {
      int x = bx();
      int y = by();
      ListTag existing = this.stack.getOrCreateTagElement("display").getList("Lore", Tag.TAG_STRING);
      char[] colors = new char[LORE_LINES];
      String[] hexes = new String[LORE_LINES];
      String[] texts = new String[LORE_LINES];
      for (int i = 0; i < LORE_LINES; i++) {
         colors[i] = 'f';
         hexes[i] = "";
         texts[i] = "";
         if (i < existing.size()) {
            Component c = jsonToComponent(existing.getString(i));
            if (c != null) {
               texts[i] = stripColor(c.getString());
               TextColor tc = c.getStyle().getColor();
               if (tc != null) {
                  hexes[i] = String.format("#%06X", tc.getValue() & 0xFFFFFF);
               } else {
                  colors[i] = legacyColorOf(c.getString());
               }
            }
         }
      }
      Runnable sync = () -> {
         CompoundTag display = this.stack.getOrCreateTagElement("display");
         ListTag list = new ListTag();
         for (int k = 0; k < LORE_LINES; k++) {
            if (texts[k] != null && !texts[k].isEmpty()) {
               Component line;
               if (isValidHex(hexes[k])) {
                  line = Component.literal(amp(texts[k]))
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(Integer.parseInt(hexes[k].substring(1), 16))));
               } else {
                  line = Component.literal("\u00a7" + colors[k] + amp(texts[k]));
               }
               list.add(StringTag.valueOf(Component.Serializer.toJson(line)));
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
      addLabel("\u00a77Cuadrito = color de paleta \u00b7 Hex #RRGGBB = color exacto \u00b7 usa &l &o... para formato", x, y);
      int hexW = 58;
      int boxW = bw() - 22 - hexW - 4;
      for (int j = 0; j < LORE_LINES; j++) {
         int idx = j;
         int ry = y + 14 + j * 20;
         String sw = isValidHex(hexes[idx]) ? "\u00a7f" : ("\u00a7" + colors[idx]);
         addRenderableWidget(Button.builder(Component.literal(sw + SWATCH), b -> {
            int p = COLORS.indexOf(colors[idx]);
            colors[idx] = COLORS.charAt((p + 1) % COLORS.length());
            hexes[idx] = "";
            sync.run();
            rebuildWidgets();
         }).tooltip(Tooltip.create(Component.literal("Color de paleta de esta l\u00ednea"))).bounds(x, ry, 18, 16).build());
         EditBox line = new EditBox(this.font, x + 22, ry, boxW, 16, Component.empty());
         line.setMaxLength(128);
         line.setValue(texts[idx]);
         line.setHint(Component.literal("L\u00ednea de lore " + (idx + 1)));
         line.setResponder(s -> {
            texts[idx] = s;
            sync.run();
         });
         addRenderableWidget(line);
         EditBox hex = new EditBox(this.font, x + 22 + boxW + 4, ry, hexW, 16, Component.empty());
         hex.setMaxLength(7);
         hex.setValue(hexes[idx]);
         hex.setHint(Component.literal("#hex"));
         hex.setResponder(s -> {
            hexes[idx] = s.trim();
            sync.run();
         });
         addRenderableWidget(hex);
      }
   }

   private static Component jsonToComponent(String json) {
      try {
         return Component.Serializer.fromJson(json);
      } catch (Exception e) {
         return Component.literal(json);
      }
   }

   // ---- encantamientos ------------------------------------------------------

   private static String enchName(ResourceLocation rl) {
      Enchantment e = rl == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(rl);
      return e != null ? Component.translatable(e.getDescriptionId()).getString() : (rl == null ? "" : rl.getPath());
   }

   private void loadEnchants() {
      this.enchEntries.clear();
      ListTag list = this.stack.getOrCreateTag().getList("Enchantments", Tag.TAG_COMPOUND);
      for (int i = 0; i < list.size(); i++) {
         CompoundTag t = list.getCompound(i);
         EnchEntry e = new EnchEntry();
         e.id = t.getString("id");
         e.level = t.getInt("lvl");
         this.enchEntries.add(e);
      }
      this.enchLoaded = true;
   }

   private void saveEnchants() {
      ListTag list = new ListTag();
      for (EnchEntry e : this.enchEntries) {
         if (e.id != null && !e.id.isEmpty()) {
            CompoundTag t = new CompoundTag();
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
      int x = bx();
      int y = by();
      int colW = (bw() - 8) / 2;
      int rightX = x + colW + 8;
      List<ResourceLocation> ids = new ArrayList<>();
      for (Enchantment ench : ForgeRegistries.ENCHANTMENTS.getValues()) {
         ResourceLocation rl = ForgeRegistries.ENCHANTMENTS.getKey(ench);
         if (rl != null) {
            ids.add(rl);
         }
      }
      ids.sort(Comparator.comparing(NbtEditorScreen::enchName, String.CASE_INSENSITIVE_ORDER));
      addLabel("\u00a77Buscar y a\u00f1adir:", x, y);
      EditBox search = new EditBox(this.font, x, y + 12, colW, 16, Component.empty());
      search.setHint(Component.literal("Buscar encantamiento..."));
      addRenderableWidget(search);
      ScrollSelector<ResourceLocation> picker = new ScrollSelector<>(x, y + 32, colW, bh() - 34, 14,
            NbtEditorScreen::enchName, rl -> enchName(rl) + " " + rl, rl -> ItemStack.EMPTY);
      picker.setItems(ids);
      picker.onSelect(rl -> {
         EnchEntry e = new EnchEntry();
         e.id = rl.toString();
         e.level = 1;
         this.enchEntries.add(e);
         saveEnchants();
         rebuildWidgets();
      });
      search.setResponder(picker::setQuery);
      addRenderableWidget(picker);
      addLabel("\u00a77Asignados (nombre / nivel):", rightX, y);
      int ry = y + 14;
      for (int i = 0; i < this.enchEntries.size(); i++) {
         EnchEntry e = this.enchEntries.get(i);
         addLabel("\u00a7f" + this.font.plainSubstrByWidth(enchName(ResourceLocation.tryParse(e.id)), colW - 92), rightX, ry + 4);
         EditBox lvl = new EditBox(this.font, rightX + colW - 86, ry, 36, 16, Component.empty());
         lvl.setValue(Integer.toString(e.level));
         lvl.setResponder(s -> {
            try {
               e.level = Integer.parseInt(s.trim());
               saveEnchants();
            } catch (NumberFormatException ignored) {
            }
         });
         addRenderableWidget(lvl);
         int gone = i;
         addRenderableWidget(Button.builder(Component.literal("\u00a7cX"), b -> {
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

   // ---- atributos -----------------------------------------------------------

   private static String attrName(ResourceLocation rl) {
      Attribute a = rl == null ? null : ForgeRegistries.ATTRIBUTES.getValue(rl);
      return a != null ? Component.translatable(a.getDescriptionId()).getString() : (rl == null ? "" : rl.getPath());
   }

   private static String slotEs(String slot) {
      return switch (slot) {
         case "mainhand" -> "Mano principal";
         case "offhand" -> "Mano secundaria";
         case "head" -> "Casco";
         case "chest" -> "Pechera";
         case "legs" -> "Polainas";
         case "feet" -> "Botas";
         default -> "Cualquiera";
      };
   }

   private void loadAttrs() {
      this.attrEntries.clear();
      ListTag list = this.stack.getOrCreateTag().getList("AttributeModifiers", Tag.TAG_COMPOUND);
      for (int i = 0; i < list.size(); i++) {
         CompoundTag t = list.getCompound(i);
         AttrEntry e = new AttrEntry();
         e.id = t.getString("AttributeName");
         e.amount = t.getDouble("Amount");
         e.op = t.getInt("Operation");
         e.slot = t.contains("Slot") ? t.getString("Slot") : "mainhand";
         if (t.contains("UUID")) {
            try {
               e.uuid = NbtUtils.loadUUID(t.get("UUID"));
            } catch (Exception ignored) {
            }
         }
         this.attrEntries.add(e);
      }
      this.attrLoaded = true;
   }

   private void saveAttrs() {
      ListTag list = new ListTag();
      for (AttrEntry e : this.attrEntries) {
         if (e.id != null && !e.id.isEmpty()) {
            CompoundTag t = new CompoundTag();
            t.putString("AttributeName", e.id);
            t.putString("Name", "fshop");
            t.putDouble("Amount", e.amount);
            t.putInt("Operation", Math.max(0, Math.min(2, e.op)));
            if (!"any".equals(e.slot)) {
               t.putString("Slot", e.slot);
            }
            UUID u = e.uuid != null ? e.uuid : UUID.randomUUID();
            t.put("UUID", NbtUtils.createUUID(u));
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
      int x = bx();
      int y = by();
      int colW = (bw() - 8) / 2;
      int rightX = x + colW + 8;
      List<ResourceLocation> ids = new ArrayList<>();
      for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
         ResourceLocation rl = ForgeRegistries.ATTRIBUTES.getKey(attribute);
         if (rl != null) {
            ids.add(rl);
         }
      }
      ids.sort(Comparator.comparing(NbtEditorScreen::attrName, String.CASE_INSENSITIVE_ORDER));
      addLabel("\u00a77Buscar y a\u00f1adir:", x, y);
      EditBox search = new EditBox(this.font, x, y + 12, colW, 16, Component.empty());
      search.setHint(Component.literal("Buscar atributo..."));
      addRenderableWidget(search);
      ScrollSelector<ResourceLocation> picker = new ScrollSelector<>(x, y + 32, colW, bh() - 34, 14,
            NbtEditorScreen::attrName, rl -> attrName(rl) + " " + rl, rl -> ItemStack.EMPTY);
      picker.setItems(ids);
      picker.onSelect(rl -> {
         AttrEntry e = new AttrEntry();
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
      addLabel("\u00a77Asignados (cantidad \u00b7 c\u00f3mo \u00b7 ranura):", rightX, y);
      int ry = y + 14;
      for (int i = 0; i < this.attrEntries.size(); i++) {
         AttrEntry e = this.attrEntries.get(i);
         addLabel("\u00a7f" + this.font.plainSubstrByWidth(attrName(ResourceLocation.tryParse(e.id)), colW - 92), rightX, ry + 4);
         EditBox amt = new EditBox(this.font, rightX + colW - 86, ry, 36, 16, Component.empty());
         amt.setValue(String.format(Locale.ROOT, "%.2f", e.amount));
         amt.setResponder(s -> {
            try {
               e.amount = Double.parseDouble(s.trim());
               saveAttrs();
            } catch (NumberFormatException ignored) {
            }
         });
         addRenderableWidget(amt);
         int gone = i;
         addRenderableWidget(Button.builder(Component.literal("\u00a7cX"), b -> {
            this.attrEntries.remove(gone);
            saveAttrs();
            rebuildWidgets();
         }).bounds(rightX + colW - 46, ry, 22, 16).build());
         ry += 18;
         addRenderableWidget(Button.builder(Component.literal("C\u00f3mo: " + OPS[Math.max(0, Math.min(2, e.op))]), b -> {
            e.op = (e.op + 1) % 3;
            saveAttrs();
            rebuildWidgets();
         }).bounds(rightX, ry, (colW - 8) / 2, 16).build());
         addRenderableWidget(Button.builder(Component.literal("Ranura: " + slotEs(e.slot)), b -> {
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
}
