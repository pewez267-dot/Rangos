package com.fantasticpass.gui.admin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Custom-item NBT editor (same panel look as the rest of the editor). Lets an
 * admin paste/edit raw SNBT for a reward item (enchantments, custom name, lore,
 * CustomModelData, attributes, mod data...) and add it to the free or premium
 * track, or remove an existing reward. Mirrors how Fantastic Spawner / Crates
 * expose item NBT editing.
 */
public final class NbtEditorScreen extends Screen {
   /** Receives the edited stack and whether it goes to the premium track. */
   public interface Saver {
      void save(ItemStack stack, boolean premium);
   }

   private final Screen parent;
   private final ItemStack base;
   private final Saver saver;
   @Nullable
   private final Runnable remover;

   private int leftPos;
   private int topPos;
   private int panelWidth;
   private int panelHeight;
   private EditBox countBox;
   private MultiLineEditBox nbtBox;
   private String error = "";

   public NbtEditorScreen(Screen parent, ItemStack base, Saver saver, @Nullable Runnable remover) {
      super(Component.translatable("fantasticpass.gui.nbt_editor"));
      this.parent = parent;
      this.base = base.copy();
      this.saver = saver;
      this.remover = remover;
   }

   @Override
   protected void init() {
      this.panelWidth = Math.min(this.width - 20, 420);
      this.panelHeight = Math.min(this.height - 20, 240);
      this.leftPos = (this.width - this.panelWidth) / 2;
      this.topPos = (this.height - this.panelHeight) / 2;

      int x = this.leftPos + 12;
      int y = this.topPos + 44;

      this.countBox = this.addRenderableWidget(new EditBox(this.font, x + 70, y, 60, 18, Component.empty()));
      this.countBox.setFilter(s -> s.matches("\\d*"));
      this.countBox.setValue(String.valueOf(Math.max(1, this.base.getCount())));

      this.nbtBox = this.addRenderableWidget(new MultiLineEditBox(
         this.font, x, y + 28, this.panelWidth - 24, this.panelHeight - 44 - 28 - 28,
         Component.translatable("fantasticpass.gui.nbt_hint"),
         Component.translatable("fantasticpass.gui.nbt_editor")));
      CompoundTag tag = this.base.getTag();
      this.nbtBox.setValue(tag == null ? "" : tag.toString());

      int by = this.topPos + this.panelHeight - 24;
      this.addRenderableWidget(Button.builder(
            Component.translatable("fantasticpass.gui.save_free").withStyle(ChatFormatting.AQUA), b -> this.apply(false))
         .bounds(x, by, 110, 18).build());
      this.addRenderableWidget(Button.builder(
            Component.translatable("fantasticpass.gui.save_premium").withStyle(ChatFormatting.LIGHT_PURPLE), b -> this.apply(true))
         .bounds(x + 116, by, 120, 18).build());
      if (this.remover != null) {
         this.addRenderableWidget(Button.builder(
               Component.translatable("fantasticpass.gui.remove").withStyle(ChatFormatting.RED), b -> {
                  this.remover.run();
                  this.onClose();
               })
            .bounds(this.leftPos + this.panelWidth - 122, by, 56, 18).build());
      }
      this.addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.close"), b -> this.onClose())
         .bounds(this.leftPos + this.panelWidth - 62, by, 50, 18).build());
   }

   private int parseCount() {
      try {
         int c = this.countBox.getValue().isEmpty() ? 1 : Integer.parseInt(this.countBox.getValue());
         return Math.max(1, Math.min(64, c));
      } catch (NumberFormatException e) {
         return 1;
      }
   }

   private void apply(boolean premium) {
      String snbt = this.nbtBox.getValue().trim();
      ItemStack stack = new ItemStack(this.base.getItem(), this.parseCount());
      if (!snbt.isEmpty()) {
         try {
            CompoundTag tag = TagParser.parseTag(snbt);
            stack.setTag(tag);
         } catch (CommandSyntaxException e) {
            this.error = "\u00a7c" + Component.translatable("fantasticpass.gui.nbt_invalid").getString() + " " + e.getMessage();
            return;
         }
      }

      this.error = "";
      this.saver.save(stack, premium);
      this.onClose();
   }

   @Override
   public void onClose() {
      Minecraft.getInstance().setScreen(this.parent);
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(g);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xE0181A1F);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, 0xFF24262E);
      g.fill(this.leftPos, this.topPos + this.panelHeight - 1, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xFF3A2E12);
      g.renderOutline(this.leftPos, this.topPos, this.panelWidth, this.panelHeight, 0xFF5A4A1E);
      g.drawString(this.font, "\u00a7d\u2726 \u00a7f" + this.title.getString() + " \u2014 " + this.base.getHoverName().getString(), this.leftPos + 10, this.topPos + 6, 0xFFFFFF, false);
      g.renderFakeItem(this.base, this.leftPos + this.panelWidth - 28, this.topPos + 2);

      g.drawString(this.font, "\u00a77" + Component.translatable("fantasticpass.gui.count").getString(), this.leftPos + 12, this.topPos + 49, 0xC0C0C0, false);

      super.render(g, mouseX, mouseY, partialTick);

      if (!this.error.isEmpty()) {
         g.drawString(this.font, this.font.plainSubstrByWidth(this.error, this.panelWidth - 24), this.leftPos + 12, this.topPos + this.panelHeight - 38, 0xFFFF5555, false);
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
