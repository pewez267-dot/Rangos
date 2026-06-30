package com.fantasticpass.gui.admin;

import com.fantasticpass.gui.widgets.ScrollSelector;
import com.fantasticpass.quest.Quest;
import com.fantasticpass.quest.QuestType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Visual editor for a single quest list (a daily pool or one week's free /
 * premium set). Pick an objective type, set its target and points, add it, and
 * remove existing ones by clicking. Edits the pass list in place; the parent
 * editor's Save button persists everything.
 */
public final class QuestListEditorScreen extends Screen {
   private final Screen parent;
   private final List<Quest> target;
   private final String idPrefix;
   private final Component heading;

   private int leftPos;
   private int topPos;
   private int panelWidth;
   private int panelHeight;

   private EditBox search;
   private EditBox targetBox;
   private EditBox pointsBox;
   private ScrollSelector<QuestType> typeSelector;
   private ScrollSelector<Quest> currentList;

   public QuestListEditorScreen(Screen parent, Component heading, List<Quest> target, String idPrefix) {
      super(Component.translatable("fantasticpass.gui.quest_editor"));
      this.parent = parent;
      this.target = target;
      this.idPrefix = idPrefix;
      this.heading = heading;
   }

   @Override
   protected void init() {
      this.panelWidth = Math.min(this.width - 16, 460);
      this.panelHeight = Math.min(this.height - 16, 250);
      this.leftPos = (this.width - this.panelWidth) / 2;
      this.topPos = (this.height - this.panelHeight) / 2;

      int lx = this.leftPos + 12;
      int rx = this.leftPos + this.panelWidth / 2 + 8;
      int colW = this.panelWidth / 2 - 22;
      int y = this.topPos + 34;

      // Left: objective type picker.
      this.search = this.addRenderableWidget(new EditBox(this.font, lx, y, colW, 16, Component.empty()));
      this.search.setHint(Component.translatable("fantasticpass.gui.search"));
      this.search.setResponder(q -> this.typeSelector.setQuery(q));

      this.typeSelector = this.addRenderableWidget(new ScrollSelector<>(lx, y + 20, colW, 116, 16,
         t -> Component.translatable(t.descriptionKey(), "N").getString(),
         t -> Component.translatable(t.descriptionKey(), "N").getString() + " " + t.getId(),
         t -> new ItemStack(Items.PAPER)));
      List<QuestType> types = new ArrayList<>();
      for (QuestType t : QuestType.values()) {
         if (t != QuestType.PLAY_MINUTES) {
            types.add(t);
         }
      }
      this.typeSelector.setItems(types);

      // Right: target + points + add, then the current list.
      this.targetBox = this.addRenderableWidget(new EditBox(this.font, rx + 54, y, colW - 54, 16, Component.empty()));
      this.targetBox.setFilter(s -> s.matches("\\d*"));
      this.targetBox.setValue("10");

      this.pointsBox = this.addRenderableWidget(new EditBox(this.font, rx + 54, y + 20, colW - 54, 16, Component.empty()));
      this.pointsBox.setFilter(s -> s.matches("\\d*"));
      this.pointsBox.setValue("10");

      this.addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.add_quest").withStyle(ChatFormatting.GREEN), b -> this.addQuest())
         .bounds(rx, y + 40, colW, 18).build());

      this.currentList = this.addRenderableWidget(new ScrollSelector<>(rx, y + 62, colW, 74, 16,
         this::questLabel, this::questLabel, q -> new ItemStack(Items.WRITABLE_BOOK)));
      this.currentList.onSelect(this::removeQuest);
      this.refreshList();

      int by = this.topPos + this.panelHeight - 24;
      this.addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.close"), b -> this.onClose())
         .bounds(this.leftPos + this.panelWidth - 90, by, 80, 18).build());
   }

   private String questLabel(Quest q) {
      return Component.translatable(q.getType().descriptionKey(), q.getTarget()).getString() + " \u00a7b+" + q.getPoints();
   }

   private int parse(EditBox box, int def) {
      try {
         return box.getValue().isEmpty() ? def : Integer.parseInt(box.getValue());
      } catch (NumberFormatException e) {
         return def;
      }
   }

   private void addQuest() {
      QuestType type = this.typeSelector.getSelected();
      if (type != null) {
         String id = this.idPrefix + Long.toHexString((System.nanoTime() ^ (long)this.target.size() * 2654435761L) & 0xFFFFFFL);
         this.target.add(new Quest(id, type, this.parse(this.targetBox, 10), this.parse(this.pointsBox, 10)));
         this.refreshList();
      }
   }

   private void removeQuest(Quest q) {
      this.target.remove(q);
      this.currentList.clearSelection();
      this.refreshList();
   }

   private void refreshList() {
      this.currentList.setItems(new ArrayList<>(this.target));
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
      g.drawString(this.font, "\u00a7d\u2726 \u00a7f" + this.heading.getString(), this.leftPos + 10, this.topPos + 6, 0xFFFFFF, false);

      int rx = this.leftPos + this.panelWidth / 2 + 8;
      int y = this.topPos + 34;
      g.drawString(this.font, "\u00a77" + Component.translatable("fantasticpass.gui.target").getString(), rx, y + 4, 0xC0C0C0, false);
      g.drawString(this.font, "\u00a77" + Component.translatable("fantasticpass.gui.points_field").getString(), rx, y + 24, 0xC0C0C0, false);
      g.drawString(this.font, "\u00a78" + Component.translatable("fantasticpass.gui.quest_remove_hint").getString(), rx, this.topPos + this.panelHeight - 38, 0x9A9A9A, false);

      super.render(g, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
