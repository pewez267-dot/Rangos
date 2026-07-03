package com.fshop.client.screen;

import com.fshop.client.widget.ScrollSelector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
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

/**
 * Full NBT editor for a shop item (ported/adapted from the Fantastic Crates /
 * Spawners editor): custom name with colour, lore lines with colour,
 * enchantments, attribute modifiers, unbreakable, CustomModelData, damage and
 * the "hide flags" toggle. Edits the given ItemStack in place.
 */
public final class NbtEditorScreen extends Screen {
   private static final String COLORS = "f7e6cab9d5234180";
   private static final String[] OPS = {"Sumar", "x base", "x total"};
   private static final String[] SLOTS = {"any", "mainhand", "offhand", "head", "chest", "legs", "feet"};

   private enum Tab {
      GENERAL, LORE, ENCHANTS, ATTRIBUTES
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

   private final Screen parent;
   private final ItemStack stack;
   private Tab activeTab = Tab.GENERAL;
   private int leftPos;
   private int topPos;
   private int panelW;
   private int panelH;
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
      Tab[] tabs = Tab.values();
      String[] names = {"General", "Lore", "Encantamientos", "Atributos"};
      int gap = 4;
      int tabW = (this.panelW - 16 - gap * (tabs.length - 1)) / tabs.length;
      for (int i = 0; i < tabs.length; i++) {
         Tab t = tabs[i];
         String text = (t == this.activeTab ? "\u00a7f\u00a7l" : "\u00a77") + names[i];
         addRenderableWidget(Button.builder(Component.literal(text), b -> {
            this.activeTab = t;
            rebuildWidgets();
         }).bounds(this.leftPos + 8 + i * (tabW + gap), this.topPos + 24, tabW, 18).build());
      }
      addRenderableWidget(Button.builder(Component.literal("\u00a7aListo"), b -> this.onClose())
            .bounds(this.leftPos + this.panelW - 88, this.topPos + this.panelH - 24, 80, 18).build());
      switch (this.activeTab) {
         case GENERAL -> initGeneral();
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
      return this.panelW - 24;
   }

   private int bh() {
      return this.panelH - 56 - 28;
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
      this.renderBackground(g);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelW, this.topPos + this.panelH, 0xE01A1A1A);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelW, this.topPos + 20, 0xFF2A2A2A);
      g.drawString(this.font, "\u00a7d\u2726 \u00a7fEditor de NBT: \u00a7e" + this.stack.getHoverName().getString(),
            this.leftPos + 8, this.topPos + 6, 0xFFFFFF, false);
      g.drawString(this.font, "\u00a77Nombre, lore, encantamientos y atributos del item.",
            this.leftPos + 8, this.topPos + 44, 0x9AC0F0, false);
      g.renderFakeItem(this.stack, this.leftPos + this.panelW - 26, this.topPos + 2);
      super.render(g, mouseX, mouseY, partial);
   }

   private void initGeneral() {
      int x = bx();
      int y = by();
      char nameColor = currentNameColor();
      addRenderableWidget(Button.builder(Component.literal("\u00a7" + nameColor), b -> {
         char c = currentNameColor();
         int idx = COLORS.indexOf(c);
         char next = COLORS.charAt((idx + 1) % COLORS.length());
         applyName(next, stripColor(this.stack.hasCustomHoverName() ? this.stack.getHoverName().getString() : ""));
         rebuildWidgets();
      }).bounds(x, y, 18, 16).build());
      EditBox name = new EditBox(this.font, x + 22, y, bw() - 22, 16, Component.empty());
      name.setMaxLength(128);
      name.setValue(stripColor(this.stack.hasCustomHoverName() ? this.stack.getHoverName().getString() : ""));
      name.setHint(Component.literal("Nombre personalizado del item"));
      name.setResponder(s -> applyName(currentNameColor(), s));
      addRenderableWidget(name);

      boolean unbreak = this.stack.getOrCreateTag().getBoolean("Unbreakable");
      addRenderableWidget(Button.builder(
            Component.literal((unbreak ? "\u00a7a" : "\u00a77") + "Irrompible: " + (unbreak ? "S\u00ed" : "No")), b -> {
               boolean now = !this.stack.getOrCreateTag().getBoolean("Unbreakable");
               if (now) {
                  this.stack.getOrCreateTag().putBoolean("Unbreakable", true);
               } else {
                  this.stack.getOrCreateTag().remove("Unbreakable");
               }
               rebuildWidgets();
            }).bounds(x, y + 28, 200, 16).build());

      EditBox cmd = new EditBox(this.font, x + 220, y + 28, 80, 16, Component.empty());
      cmd.setMaxLength(8);
      cmd.setValue(this.stack.getOrCreateTag().contains("CustomModelData")
            ? Integer.toString(this.stack.getOrCreateTag().getInt("CustomModelData")) : "");
      cmd.setHint(Component.literal("CMD"));
      cmd.setResponder(s -> {
         String t = s.trim();
         if (t.isEmpty()) {
            this.stack.getOrCreateTag().remove("CustomModelData");
         } else {
            try {
               this.stack.getOrCreateTag().putInt("CustomModelData", Integer.parseInt(t));
            } catch (NumberFormatException ignored) {
            }
         }
      });
      addRenderableWidget(cmd);

      boolean hideAll = this.stack.getOrCreateTag().getInt("HideFlags") == 127;
      addRenderableWidget(Button.builder(
            Component.literal((hideAll ? "\u00a7a" : "\u00a77") + "Ocultar flags: " + (hideAll ? "S\u00ed" : "No")), b -> {
               CompoundTag t = this.stack.getOrCreateTag();
               if (t.getInt("HideFlags") == 127) {
                  t.remove("HideFlags");
               } else {
                  t.putInt("HideFlags", 127);
               }
               rebuildWidgets();
            }).bounds(x, y + 50, 200, 16).build());
   }

   private char currentNameColor() {
      if (!this.stack.hasCustomHoverName()) {
         return 'f';
      }
      String full = Component.Serializer.toJson(this.stack.getHoverName());
      int idx = full.indexOf("\"color\":\"");
      if (idx < 0) {
         return 'f';
      }
      String tail = full.substring(idx + 9);
      int end = tail.indexOf('"');
      String colorName = end > 0 ? tail.substring(0, end) : "white";
      return colorNameToChar(colorName);
   }

   private static char colorNameToChar(String n) {
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

   private static String stripColor(String s) {
      if (s == null) {
         return "";
      }
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if ((c == '\u00a7' || c == '&') && i + 1 < s.length()
               && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(s.charAt(i + 1)) >= 0) {
            i++;
         } else {
            sb.append(c);
         }
      }
      return sb.toString();
   }

   private void applyName(char color, String text) {
      if (text == null || text.isEmpty()) {
         this.stack.resetHoverName();
      } else {
         this.stack.setHoverName(Component.literal("\u00a7" + color + text));
      }
   }

   private void initLore() {
      int x = bx();
      int y = by();
      int max = 8;
      ListTag existing = this.stack.getOrCreateTagElement("display").getList("Lore", 8);
      char[] colors = new char[max];
      String[] texts = new String[max];
      for (int i = 0; i < max; i++) {
         String raw = i < existing.size() ? existing.getString(i) : "";
         String plain = jsonToPlain(raw);
         char c = 'f';
         String t = plain;
         if (plain.length() >= 2 && (plain.charAt(0) == '\u00a7' || plain.charAt(0) == '&')
               && COLORS.indexOf(plain.charAt(1)) >= 0) {
            c = plain.charAt(1);
            t = plain.substring(2);
         }
         colors[i] = c;
         texts[i] = t;
      }
      Runnable sync = () -> {
         CompoundTag display = this.stack.getOrCreateTagElement("display");
         ListTag list = new ListTag();
         for (int k = 0; k < max; k++) {
            if (texts[k] != null && !texts[k].isEmpty()) {
               String json = Component.Serializer.toJson(Component.literal("\u00a7" + colors[k] + texts[k]));
               list.add(StringTag.valueOf(json));
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
         int idx = j;
         int ry = y + j * 20;
         addRenderableWidget(Button.builder(Component.literal("\u00a7" + colors[idx]), b -> {
            int p = COLORS.indexOf(colors[idx]);
            colors[idx] = COLORS.charAt((p + 1) % COLORS.length());
            sync.run();
            rebuildWidgets();
         }).bounds(x, ry, 18, 16).build());
         EditBox eb = new EditBox(this.font, x + 22, ry, bw() - 22, 16, Component.empty());
         eb.setMaxLength(96);
         eb.setValue(texts[idx]);
         eb.setHint(Component.literal("Linea de lore " + (idx + 1)));
         eb.setResponder(s -> {
            texts[idx] = s;
            sync.run();
         });
         addRenderableWidget(eb);
      }
   }

   private static String jsonToPlain(String json) {
      try {
         Component c = Component.Serializer.fromJson(json);
         return c == null ? json : c.getString();
      } catch (Exception e) {
         return json;
      }
   }

   private void loadEnchants() {
      this.enchEntries.clear();
      ListTag list = this.stack.getOrCreateTag().getList("Enchantments", 10);
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
      ids.sort(Comparator.comparing(ResourceLocation::toString));
      EditBox search = new EditBox(this.font, x, y, colW, 16, Component.empty());
      search.setHint(Component.literal("Buscar encantamiento..."));
      addRenderableWidget(search);
      ScrollSelector<ResourceLocation> picker = new ScrollSelector<>(x, y + 20, colW, bh() - 22, 14,
            ResourceLocation::getPath, ResourceLocation::toString, rl -> ItemStack.EMPTY);
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
      int ry = y;
      for (int i = 0; i < this.enchEntries.size(); i++) {
         EnchEntry e = this.enchEntries.get(i);
         String pretty = e.id.startsWith("minecraft:") ? e.id.substring(10) : e.id;
         EditBox idBox = new EditBox(this.font, rightX, ry, colW - 90, 16, Component.empty());
         idBox.setMaxLength(64);
         idBox.setValue(pretty);
         idBox.setResponder(s -> {
            e.id = s.contains(":") ? s : "minecraft:" + s;
            saveEnchants();
         });
         addRenderableWidget(idBox);
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
         if (ry > y + bh() - 22) {
            break;
         }
      }
   }

   private void loadAttrs() {
      this.attrEntries.clear();
      ListTag list = this.stack.getOrCreateTag().getList("AttributeModifiers", 10);
      for (int i = 0; i < list.size(); i++) {
         CompoundTag t = list.getCompound(i);
         AttrEntry e = new AttrEntry();
         e.id = t.getString("AttributeName");
         e.amount = t.getDouble("Amount");
         e.op = t.getInt("Operation");
         e.slot = t.contains("Slot") ? t.getString("Slot") : "mainhand";
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
            UUID u = UUID.randomUUID();
            t.putIntArray("UUID", new int[] {(int) (u.getMostSignificantBits() >> 32),
                  (int) (u.getMostSignificantBits() & 0xFFFFFFFFL), (int) (u.getLeastSignificantBits() >> 32),
                  (int) (u.getLeastSignificantBits() & 0xFFFFFFFFL)});
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
      for (Attribute a : ForgeRegistries.ATTRIBUTES.getValues()) {
         ResourceLocation rl = ForgeRegistries.ATTRIBUTES.getKey(a);
         if (rl != null) {
            ids.add(rl);
         }
      }
      ids.sort(Comparator.comparing(ResourceLocation::toString));
      EditBox search = new EditBox(this.font, x, y, colW, 16, Component.empty());
      search.setHint(Component.literal("Buscar atributo..."));
      addRenderableWidget(search);
      ScrollSelector<ResourceLocation> picker = new ScrollSelector<>(x, y + 20, colW, bh() - 22, 14,
            ResourceLocation::getPath, ResourceLocation::toString, rl -> ItemStack.EMPTY);
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
      int ry = y;
      for (int i = 0; i < this.attrEntries.size(); i++) {
         AttrEntry e = this.attrEntries.get(i);
         String pretty = e.id.startsWith("minecraft:") ? e.id.substring(10) : e.id;
         EditBox idBox = new EditBox(this.font, rightX, ry, colW - 90, 16, Component.empty());
         idBox.setMaxLength(96);
         idBox.setValue(pretty);
         idBox.setResponder(s -> {
            e.id = s.contains(":") ? s : "minecraft:" + s;
            saveAttrs();
         });
         addRenderableWidget(idBox);
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
         addRenderableWidget(Button.builder(Component.literal("Op: " + OPS[e.op]), b -> {
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
}
