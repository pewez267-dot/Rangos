package com.fantasticpass.gui.admin;

import com.fantasticpass.gui.widgets.ScrollSelector;
import com.fantasticpass.quest.Quest;
import com.fantasticpass.quest.QuestType;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Dedicated "design your own quest from scratch" screen: the admin writes the
 * mission text themselves and chooses what the player has to do (the action).
 * Every field carries a hover explanation and there is no loose grey text.
 */
public final class CustomQuestEditorScreen extends Screen {
   /** Self-explanatory actions the hand-made quest can track. */
   private static final QuestType[] ACTIONS = {
      QuestType.KILL_MONSTERS, QuestType.KILL_ANIMALS, QuestType.KILL_ZOMBIES, QuestType.KILL_SKELETONS,
      QuestType.KILL_CREEPERS, QuestType.KILL_ENDERMEN, QuestType.BREAK_BLOCKS, QuestType.MINE_STONE,
      QuestType.MINE_ORES, QuestType.CHOP_WOOD, QuestType.HARVEST_CROPS, QuestType.PLACE_BLOCKS,
      QuestType.EAT_FOOD, QuestType.CRAFT_ITEMS, QuestType.SMELT_ITEMS, QuestType.CATCH_FISH,
      QuestType.BREED_ANIMALS, QuestType.TAME_ANIMALS, QuestType.DEAL_DAMAGE, QuestType.GAIN_XP,
      QuestType.TRAVEL_BLOCKS
   };

   private final Screen parent;
   private final List<Quest> target;
   private final String idPrefix;
   private final Component heading;

   private int leftPos;
   private int topPos;
   private int panelWidth;
   private int panelHeight;

   private EditBox titleBox;
   private EditBox targetBox;
   private EditBox pointsBox;
   private ScrollSelector<QuestType> actionSelector;
   private ScrollSelector<Quest> currentList;
   private final List<Hint> hints = new ArrayList<>();

   public CustomQuestEditorScreen(Screen parent, Component heading, List<Quest> target, String idPrefix) {
      super(Component.translatable("fantasticpass.gui.custom_quest_editor"));
      this.parent = parent;
      this.target = target;
      this.idPrefix = idPrefix;
      this.heading = heading;
   }

   @Override
   protected void init() {
      this.hints.clear();
      this.panelWidth = Math.min(this.width - 16, 470);
      this.panelHeight = Math.min(this.height - 16, 270);
      this.leftPos = (this.width - this.panelWidth) / 2;
      this.topPos = (this.height - this.panelHeight) / 2;

      int lx = this.leftPos + 12;
      int leftW = (this.panelWidth - 36) / 2;
      int rx = lx + leftW + 12;
      int rightW = this.leftPos + this.panelWidth - 12 - rx;

      // Row 1: the mission text (full width).
      this.titleBox = this.addRenderableWidget(new EditBox(this.font, lx, this.topPos + 44, this.panelWidth - 24, 16, Component.empty()));
      this.titleBox.setMaxLength(80);
      this.titleBox.setHint(Component.translatable("fantasticpass.gui.custom_quest_title_hint"));
      this.hint(lx, this.topPos + 44, this.panelWidth - 24, 16, "fantasticpass.gui.custom_quest_title", "fantasticpass.gui.tip_cq_title");

      // Left: pick the action.
      int selY = this.topPos + 82;
      int selH = this.panelHeight - 82 - 30;
      this.actionSelector = this.addRenderableWidget(new ScrollSelector<>(lx, selY, leftW, selH, 16,
         this::actionName, this::actionName, t -> new ItemStack(Items.PAPER)));
      this.actionSelector.setItems(Arrays.asList(ACTIONS));
      this.hint(lx, selY, leftW, selH, "fantasticpass.gui.custom_quest_action", "fantasticpass.gui.tip_cq_action");

      // Right: amount, points, add, list.
      int numY = this.topPos + 92;
      this.targetBox = this.addRenderableWidget(new EditBox(this.font, rx, numY, 70, 16, Component.empty()));
      this.targetBox.setFilter(s -> s.matches("\\d*"));
      this.targetBox.setValue("10");
      this.hint(rx, numY, 70, 16, "fantasticpass.gui.count", "fantasticpass.gui.tip_cq_count");
      this.pointsBox = this.addRenderableWidget(new EditBox(this.font, rx + 92, numY, 70, 16, Component.empty()));
      this.pointsBox.setFilter(s -> s.matches("\\d*"));
      this.pointsBox.setValue("10");
      this.hint(rx + 92, numY, 70, 16, "fantasticpass.gui.points_field", "fantasticpass.gui.tip_cq_points");

      this.addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.add_quest").withStyle(ChatFormatting.GREEN), b -> this.addQuest())
         .bounds(rx, numY + 22, rightW, 18).build());
      this.hint(rx, numY + 22, rightW, 18, "fantasticpass.gui.add_quest", "fantasticpass.gui.tip_cq_add");

      int listY = numY + 54;
      int listH = this.topPos + this.panelHeight - 30 - listY;
      this.currentList = this.addRenderableWidget(new ScrollSelector<>(rx, listY, rightW, listH, 16,
         this::questLabel, this::questLabel, q -> new ItemStack(Items.WRITABLE_BOOK)));
      this.currentList.onSelect(this::removeQuest);
      this.hint(rx, listY, rightW, listH, "fantasticpass.gui.custom_quest_list", "fantasticpass.gui.tip_cq_list");
      this.refreshList();

      this.addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.close"), b -> this.onClose())
         .bounds(lx, this.topPos + this.panelHeight - 24, leftW, 18).build());
      this.hint(lx, this.topPos + this.panelHeight - 24, leftW, 18, "fantasticpass.gui.close", "fantasticpass.gui.tip_cq_back");
   }

   private String actionName(QuestType t) {
      return Component.translatable(t.descriptionKey(), "N", "...").getString();
   }

   private String questLabel(Quest q) {
      return q.getDescription().getString() + " \u00a7b+" + q.getPoints();
   }

   private int parse(EditBox box, int def) {
      try {
         return box.getValue().isEmpty() ? def : Integer.parseInt(box.getValue());
      } catch (NumberFormatException e) {
         return def;
      }
   }

   private void addQuest() {
      QuestType action = this.actionSelector.getSelected();
      if (action == null) {
         return; // must pick an action first
      }
      String title = this.titleBox.getValue().trim();
      String id = this.idPrefix + Long.toHexString(System.nanoTime()) + "_" + this.target.size();
      this.target.add(new Quest(id, action, "", this.parse(this.targetBox, 10), this.parse(this.pointsBox, 10), title));
      this.refreshList();
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

   private void hint(int x, int y, int w, int h, String titleKey, String descKey) {
      this.hints.add(new Hint(x, y, w, h, List.of(
         Component.translatable(titleKey).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
         Component.translatable(descKey).withStyle(ChatFormatting.GRAY))));
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(g);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xE0181A1F);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, 0xFF24262E);
      g.fill(this.leftPos, this.topPos + this.panelHeight - 1, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xFF3A2E12);
      g.renderOutline(this.leftPos, this.topPos, this.panelWidth, this.panelHeight, 0xFF5A4A1E);
      g.drawString(this.font, "\u00a7d\u271a \u00a7f" + Component.translatable("fantasticpass.gui.custom_quest_editor").getString()
         + " \u00a77\u00b7 " + this.heading.getString(), this.leftPos + 10, this.topPos + 6, 0xFFFFFF, false);

      int lx = this.leftPos + 12;
      int rx = lx + (this.panelWidth - 36) / 2 + 12;
      int numY = this.topPos + 92;

      g.drawString(this.font, "\u00a7f" + Component.translatable("fantasticpass.gui.custom_quest_title").getString(), lx, this.topPos + 32, 0xE0E0E0, false);
      g.drawString(this.font, "\u00a7f" + Component.translatable("fantasticpass.gui.custom_quest_action").getString(), lx, this.topPos + 70, 0xE0E0E0, false);
      g.drawString(this.font, "\u00a7f" + Component.translatable("fantasticpass.gui.count").getString(), rx, numY - 11, 0xE0E0E0, false);
      g.drawString(this.font, "\u00a7f" + Component.translatable("fantasticpass.gui.points_field").getString(), rx + 92, numY - 11, 0xE0E0E0, false);

      super.render(g, mouseX, mouseY, partialTick);

      List<Component> tip = null;
      for (Hint hh : this.hints) {
         if (mouseX >= hh.x() && mouseX < hh.x() + hh.w() && mouseY >= hh.y() && mouseY < hh.y() + hh.h()) {
            tip = hh.lines();
         }
      }
      if (tip != null) {
         g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private record Hint(int x, int y, int w, int h, List<Component> lines) {
   }
}
