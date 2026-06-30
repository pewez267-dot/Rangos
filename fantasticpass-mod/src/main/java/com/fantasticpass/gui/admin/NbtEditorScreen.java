package com.fantasticpass.gui.admin;

import com.fantasticpass.gui.widgets.ScrollSelector;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Fully visual custom-item editor (no commands / no raw NBT needed). Edit the
 * display name and its colour, bold/italic, up to three lore lines, any number
 * of enchantments with levels, CustomModelData and the unbreakable flag, with a
 * live item preview. Saves the built ItemStack into the free or premium track.
 */
public final class NbtEditorScreen extends Screen {
   public interface Saver {
      void save(ItemStack stack, boolean premium);
   }

   private static final ChatFormatting[] COLORS = {
      ChatFormatting.WHITE, ChatFormatting.BLACK, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_GREEN,
      ChatFormatting.DARK_AQUA, ChatFormatting.DARK_RED, ChatFormatting.DARK_PURPLE, ChatFormatting.GOLD,
      ChatFormatting.GRAY, ChatFormatting.DARK_GRAY, ChatFormatting.BLUE, ChatFormatting.GREEN,
      ChatFormatting.AQUA, ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW
   };
   private static final int SWATCH = 11;
   private static final int SWATCH_STEP = 12;

   private final Screen parent;
   private final ItemStack base;
   private final Saver saver;
   @Nullable
   private final Runnable remover;

   private int leftPos;
   private int topPos;
   private int panelWidth;
   private int panelHeight;
   private int swatchX;
   private int swatchY;

   private EditBox nameBox;
   private EditBox lore1;
   private EditBox lore2;
   private EditBox lore3;
   private EditBox cmdBox;
   private EditBox countBox;
   private EditBox enchSearch;
   private EditBox levelBox;
   private ScrollSelector<Enchantment> enchSelector;
   private ScrollSelector<EnchEntry> currentEnch;
   private Button boldButton;
   private Button italicButton;
   private Button unbreakableButton;

   private int colorIndex = -1;
   private boolean bold;
   private boolean italic;
   private boolean unbreakable;
   private final List<EnchEntry> enchantments = new ArrayList<>();

   private String pendingName = "";
   private String pendingLore1 = "";
   private String pendingLore2 = "";
   private String pendingLore3 = "";
   private String pendingCmd = "";

   public NbtEditorScreen(Screen parent, ItemStack base, Saver saver, @Nullable Runnable remover) {
      super(Component.translatable("fantasticpass.gui.nbt_editor"));
      this.parent = parent;
      this.base = base.copy();
      this.saver = saver;
      this.remover = remover;
      this.readFrom(this.base);
   }

   private void readFrom(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      if (tag != null && tag.contains("display", 10)) {
         CompoundTag display = tag.getCompound("display");
         if (display.contains("Name", 8)) {
            Component name = safeJson(display.getString("Name"));
            if (name != null) {
               this.pendingName = name.getString();
               if (name.getStyle().getColor() != null) {
                  for (int i = 0; i < COLORS.length; i++) {
                     if (COLORS[i].getColor() != null && COLORS[i].getColor().equals(name.getStyle().getColor().getValue())) {
                        this.colorIndex = i;
                     }
                  }
               }
               this.bold = name.getStyle().isBold();
               this.italic = name.getStyle().isItalic();
            }
         }
         if (display.contains("Lore", 9)) {
            ListTag lore = display.getList("Lore", 8);
            if (lore.size() > 0) {
               this.pendingLore1 = plain(safeJson(lore.getString(0)));
            }
            if (lore.size() > 1) {
               this.pendingLore2 = plain(safeJson(lore.getString(1)));
            }
            if (lore.size() > 2) {
               this.pendingLore3 = plain(safeJson(lore.getString(2)));
            }
         }
      }
      if (tag != null && tag.contains("CustomModelData")) {
         this.pendingCmd = String.valueOf(tag.getInt("CustomModelData"));
      }
      this.unbreakable = tag != null && tag.getBoolean("Unbreakable");

      for (var entry : net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack).entrySet()) {
         this.enchantments.add(new EnchEntry(entry.getKey(), entry.getValue()));
      }
   }

   @Override
   protected void init() {
      this.panelWidth = Math.min(this.width - 16, 472);
      this.panelHeight = Math.min(this.height - 16, 264);
      this.leftPos = (this.width - this.panelWidth) / 2;
      this.topPos = (this.height - this.panelHeight) / 2;

      int lx = this.leftPos + 12;
      int leftW = (this.panelWidth - 36) / 2;
      int rx = lx + leftW + 12;
      int rightW = this.leftPos + this.panelWidth - 12 - rx;

      // ---- Left column ----
      this.nameBox = this.addRenderableWidget(new EditBox(this.font, lx, this.topPos + 34, leftW, 16, Component.empty()));
      this.nameBox.setMaxLength(80);
      this.nameBox.setHint(Component.translatable("fantasticpass.gui.item_name"));
      this.nameBox.setValue(this.pendingName);

      this.swatchX = lx;
      this.swatchY = this.topPos + 64;

      int styleY = this.topPos + 80;
      this.boldButton = this.addRenderableWidget(Button.builder(this.boldLabel(), b -> {
         this.bold = !this.bold;
         this.boldButton.setMessage(this.boldLabel());
      }).bounds(lx, styleY, leftW / 2 - 2, 16).build());
      this.italicButton = this.addRenderableWidget(Button.builder(this.italicLabel(), b -> {
         this.italic = !this.italic;
         this.italicButton.setMessage(this.italicLabel());
      }).bounds(lx + leftW / 2 + 2, styleY, leftW / 2 - 2, 16).build());

      int loreY = this.topPos + 110;
      this.lore1 = this.addRenderableWidget(new EditBox(this.font, lx, loreY, leftW, 14, Component.empty()));
      this.lore1.setMaxLength(96);
      this.lore1.setHint(Component.translatable("fantasticpass.gui.lore_line"));
      this.lore1.setValue(this.pendingLore1);
      this.lore2 = this.addRenderableWidget(new EditBox(this.font, lx, loreY + 17, leftW, 14, Component.empty()));
      this.lore2.setMaxLength(96);
      this.lore2.setValue(this.pendingLore2);
      this.lore3 = this.addRenderableWidget(new EditBox(this.font, lx, loreY + 34, leftW, 14, Component.empty()));
      this.lore3.setMaxLength(96);
      this.lore3.setValue(this.pendingLore3);

      int cmdY = loreY + 54;
      this.cmdBox = this.addRenderableWidget(new EditBox(this.font, lx, cmdY, leftW, 16, Component.empty()));
      this.cmdBox.setFilter(s -> s.matches("\\d*"));
      this.cmdBox.setHint(Component.literal("CustomModelData"));
      this.cmdBox.setValue(this.pendingCmd);

      int flagY = cmdY + 22;
      this.unbreakableButton = this.addRenderableWidget(Button.builder(this.unbreakableLabel(), b -> {
         this.unbreakable = !this.unbreakable;
         this.unbreakableButton.setMessage(this.unbreakableLabel());
      }).bounds(lx, flagY, leftW / 2 - 2, 16).build());
      this.countBox = this.addRenderableWidget(new EditBox(this.font, lx + leftW / 2 + 2, flagY, leftW / 2 - 2, 16, Component.empty()));
      this.countBox.setFilter(s -> s.matches("\\d*"));
      this.countBox.setHint(Component.translatable("fantasticpass.gui.count"));
      this.countBox.setValue(String.valueOf(Math.max(1, this.base.getCount())));

      // ---- Right column: enchantments ----
      this.enchSearch = this.addRenderableWidget(new EditBox(this.font, rx, this.topPos + 34, rightW, 16, Component.empty()));
      this.enchSearch.setHint(Component.translatable("fantasticpass.gui.search"));
      this.enchSearch.setResponder(q -> this.enchSelector.setQuery(q));

      this.enchSelector = this.addRenderableWidget(new ScrollSelector<>(rx, this.topPos + 54, rightW, 78, 16,
         e -> Component.translatable(e.getDescriptionId()).getString(),
         e -> Component.translatable(e.getDescriptionId()).getString() + " " + enchKey(e),
         e -> new ItemStack(Items.ENCHANTED_BOOK)));
      List<Enchantment> all = new ArrayList<>();
      for (Enchantment e : BuiltInRegistries.ENCHANTMENT) {
         all.add(e);
      }
      this.enchSelector.setItems(all);

      int lvlY = this.topPos + 148;
      this.levelBox = this.addRenderableWidget(new EditBox(this.font, rx, lvlY, 40, 16, Component.empty()));
      this.levelBox.setFilter(s -> s.matches("\\d*"));
      this.levelBox.setHint(Component.translatable("fantasticpass.gui.level_short"));
      this.levelBox.setValue("1");
      this.addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.add_ench").withStyle(ChatFormatting.GREEN), b -> this.addEnchant())
         .bounds(rx + 46, lvlY, rightW - 46, 16).build());

      this.currentEnch = this.addRenderableWidget(new ScrollSelector<>(rx, this.topPos + 180, rightW, this.panelHeight - 180 - 30, 16,
         EnchEntry::label, EnchEntry::label, e -> new ItemStack(Items.ENCHANTED_BOOK)));
      this.currentEnch.onSelect(this::removeEnchant);
      this.refreshCurrentEnch();

      // ---- Footer ----
      int by = this.topPos + this.panelHeight - 24;
      this.addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.save_free").withStyle(ChatFormatting.AQUA), b -> this.apply(false))
         .bounds(lx, by, 104, 18).build());
      this.addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.save_premium").withStyle(ChatFormatting.LIGHT_PURPLE), b -> this.apply(true))
         .bounds(lx + 108, by, 116, 18).build());
      if (this.remover != null) {
         this.addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.remove").withStyle(ChatFormatting.RED), b -> {
            this.remover.run();
            this.onClose();
         }).bounds(lx + 228, by, 60, 18).build());
      }
      this.addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.close"), b -> this.onClose())
         .bounds(this.leftPos + this.panelWidth - 72, by, 60, 18).build());
   }

   private Component boldLabel() {
      return Component.literal((this.bold ? "\u00a7a" : "\u00a77") + Component.translatable("fantasticpass.gui.bold").getString());
   }

   private Component italicLabel() {
      return Component.literal((this.italic ? "\u00a7a" : "\u00a77") + Component.translatable("fantasticpass.gui.italic").getString());
   }

   private Component unbreakableLabel() {
      return Component.literal((this.unbreakable ? "\u00a7a" : "\u00a77") + Component.translatable("fantasticpass.gui.unbreakable").getString());
   }

   private static String enchKey(Enchantment e) {
      ResourceLocation rl = BuiltInRegistries.ENCHANTMENT.getKey(e);
      return rl == null ? "" : rl.getPath();
   }

   private void addEnchant() {
      Enchantment selected = this.enchSelector.getSelected();
      if (selected != null) {
         int level = 1;
         try {
            level = Math.max(1, Math.min(255, this.levelBox.getValue().isEmpty() ? 1 : Integer.parseInt(this.levelBox.getValue())));
         } catch (NumberFormatException ignored) {
         }
         this.enchantments.removeIf(e -> e.ench == selected);
         this.enchantments.add(new EnchEntry(selected, level));
         this.refreshCurrentEnch();
      }
   }

   private void removeEnchant(EnchEntry entry) {
      this.enchantments.remove(entry);
      this.currentEnch.clearSelection();
      this.refreshCurrentEnch();
   }

   private void refreshCurrentEnch() {
      this.currentEnch.setItems(new ArrayList<>(this.enchantments));
   }

   private int parseCount() {
      try {
         int c = this.countBox.getValue().isEmpty() ? 1 : Integer.parseInt(this.countBox.getValue());
         return Math.max(1, Math.min(64, c));
      } catch (NumberFormatException e) {
         return 1;
      }
   }

   private ItemStack buildStack() {
      ItemStack stack = new ItemStack(this.base.getItem(), this.parseCount());
      CompoundTag tag = new CompoundTag();

      String name = this.nameBox.getValue().trim();
      CompoundTag display = new CompoundTag();
      if (!name.isEmpty()) {
         Style style = Style.EMPTY.withBold(this.bold).withItalic(this.italic);
         if (this.colorIndex >= 0 && COLORS[this.colorIndex].getColor() != null) {
            style = style.withColor(net.minecraft.network.chat.TextColor.fromRgb(COLORS[this.colorIndex].getColor()));
         }
         MutableComponent comp = Component.literal(name).setStyle(style);
         display.putString("Name", Component.Serializer.toJson(comp));
      }

      ListTag lore = new ListTag();
      addLore(lore, this.lore1.getValue());
      addLore(lore, this.lore2.getValue());
      addLore(lore, this.lore3.getValue());
      if (!lore.isEmpty()) {
         display.put("Lore", lore);
      }
      if (!display.isEmpty()) {
         tag.put("display", display);
      }

      if (!this.cmdBox.getValue().isEmpty()) {
         try {
            tag.putInt("CustomModelData", Integer.parseInt(this.cmdBox.getValue()));
         } catch (NumberFormatException ignored) {
         }
      }
      if (this.unbreakable) {
         tag.putBoolean("Unbreakable", true);
      }

      if (!this.enchantments.isEmpty()) {
         boolean book = stack.is(Items.ENCHANTED_BOOK);
         ListTag list = new ListTag();
         for (EnchEntry e : this.enchantments) {
            ResourceLocation rl = BuiltInRegistries.ENCHANTMENT.getKey(e.ench);
            if (rl != null) {
               CompoundTag et = new CompoundTag();
               et.putString("id", rl.toString());
               et.putShort("lvl", (short)e.level);
               list.add(et);
            }
         }
         tag.put(book ? "StoredEnchantments" : "Enchantments", list);
      }

      if (!tag.isEmpty()) {
         stack.setTag(tag);
      }
      return stack;
   }

   private static void addLore(ListTag lore, String line) {
      if (line != null && !line.trim().isEmpty()) {
         MutableComponent c = Component.literal(line).setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY));
         lore.add(StringTag.valueOf(Component.Serializer.toJson(c)));
      }
   }

   private void apply(boolean premium) {
      this.saver.save(this.buildStack(), premium);
      this.onClose();
   }

   @Nullable
   private static Component safeJson(String json) {
      try {
         return Component.Serializer.fromJson(json);
      } catch (Exception e) {
         return Component.literal(json);
      }
   }

   private static String plain(@Nullable Component c) {
      return c == null ? "" : c.getString();
   }

   @Override
   public void onClose() {
      Minecraft.getInstance().setScreen(this.parent);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (this.overSwatch(mouseX, mouseY, -1)) {
            this.colorIndex = -1;
            return true;
         }
         for (int i = 0; i < COLORS.length; i++) {
            if (this.overSwatch(mouseX, mouseY, i)) {
               this.colorIndex = i;
               return true;
            }
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   private boolean overSwatch(double mx, double my, int index) {
      int x = this.swatchX + (index + 1) * SWATCH_STEP;
      return mx >= x && mx < x + SWATCH && my >= this.swatchY && my < this.swatchY + SWATCH;
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(g);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xE0181A1F);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, 0xFF24262E);
      g.fill(this.leftPos, this.topPos + this.panelHeight - 1, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xFF3A2E12);
      g.renderOutline(this.leftPos, this.topPos, this.panelWidth, this.panelHeight, 0xFF5A4A1E);
      g.drawString(this.font, "\u00a7d\u2726 \u00a7f" + this.title.getString(), this.leftPos + 10, this.topPos + 6, 0xFFFFFF, false);

      ItemStack preview = this.buildStack();
      g.renderFakeItem(preview, this.leftPos + this.panelWidth - 26, this.topPos + 2);

      int lx = this.leftPos + 12;
      int leftW = (this.panelWidth - 36) / 2;
      int rx = lx + leftW + 12;

      // captions sit just above their controls so nothing overlaps
      g.drawString(this.font, "\u00a77" + Component.translatable("fantasticpass.gui.item_name").getString(), lx, this.topPos + 24, 0xC0C0C0, false);
      g.drawString(this.font, "\u00a77" + Component.translatable("fantasticpass.gui.color").getString(), lx, this.topPos + 56, 0xC0C0C0, false);
      g.drawString(this.font, "\u00a77" + Component.translatable("fantasticpass.gui.enchantments").getString(), rx, this.topPos + 24, 0xC0C0C0, false);
      g.drawString(this.font, "\u00a78" + Component.translatable("fantasticpass.gui.ench_remove_hint").getString(), rx, this.topPos + 170, 0x9A9A9A, false);

      super.render(g, mouseX, mouseY, partialTick);

      // colour swatches (drawn after widgets so the selection outline is on top)
      this.renderSwatch(g, -1, 0, "\u2715");
      for (int i = 0; i < COLORS.length; i++) {
         Integer rgb = COLORS[i].getColor();
         this.renderSwatch(g, i, rgb == null ? 0xFFFFFFFF : 0xFF000000 | rgb, null);
      }
   }

   private void renderSwatch(GuiGraphics g, int index, int argb, @Nullable String mark) {
      int x = this.swatchX + (index + 1) * SWATCH_STEP;
      int y = this.swatchY;
      g.fill(x, y, x + SWATCH, y + SWATCH, argb == 0 ? 0xFF202020 : argb);
      g.renderOutline(x, y, SWATCH, SWATCH, index == this.colorIndex ? 0xFFFFFFFF : 0xFF000000);
      if (mark != null) {
         g.drawString(this.font, mark, x + 2, y + 1, 0xFFFF5555, false);
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private record EnchEntry(Enchantment ench, int level) {
      String label() {
         return Component.translatable(this.ench.getDescriptionId()).getString() + " " + this.level;
      }
   }
}
