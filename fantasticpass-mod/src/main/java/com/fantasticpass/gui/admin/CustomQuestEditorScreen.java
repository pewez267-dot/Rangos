package com.fantasticpass.gui.admin;

import com.fantasticpass.gui.widgets.ScrollSelector;
import com.fantasticpass.quest.Quest;
import com.fantasticpass.quest.QuestType;
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
 * "From scratch" quest editor: the admin writes their OWN mission title and
 * picks a preset ACTION (kill, eat, mine, place, travel, ...). This is distinct
 * from {@link QuestListEditorScreen} (which auto-generates the text from the
 * chosen objective) — here the displayed text is fully authored by hand.
 * Edits the same list in place; the parent editor's Save persists everything.
 */
public final class CustomQuestEditorScreen extends Screen {
   /** Curated, self-explanatory actions offered by the manual builder. */
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

   public CustomQuestEditorScreen(Screen parent, Component heading, List<Quest> target, String idPrefix) {
      super(Component.translatable("fantasticpass.gui.custom_quest_editor"));
      this.parent = parent;
      this.target = target;
      this.idPrefix = idPrefix;
      this.heading = heading;
   }

   @Override
   protected void init() {
      this.panelWidth = Math.min(this.width - 16, 470);
      this.panelHeight = Math.min(this.height - 16, 270);
      this.leftPos = (this.width - this.panelWidth) / 2;
      this.topPos = (this.height - this.panelHeight) / 2;

      int lx = this.leftPos + 12;
      int leftW = (this.panelWidth - 36) / 2;
      int rx = lx + leftW + 12;
      int rightW = this.leftPos + this.panelWidth - 12 - rx;

      // Title (full width, top).
      this.titleBox = this.addRenderableWidget(new EditBox(this.font, lx, this.topPos + 34, this.panelWidth - 24, 16, Component.empty()));
      this.titleBox.setMaxLength(80);
      this.titleBox.setHint(Component.translatable("fantasticpass.gui.custom_quest_title_hint"));

      // Left: action picker.
      this.actionSelector = this.addRenderableWidget(new ScrollSelector<>(lx, this.topPos + 68, leftW, this.panelHeight - 68 - 30, 16,
         this::actionName, this::actionName, t -> new ItemStack(Items.PAPER)));
      this.actionSelector.setItems(java.util.Arrays.asList(ACTIONS));

      // Right: amount, points, add, current list.
      int numY = this.topPos + 78;
      this.targetBox = this.addRenderableWidget(new EditBox(this.font, rx, numY, 70, 16, Component.empty()));
      this.targetBox.setFilter(s -> s.matches("\\d*"));
      this.targetBox.setValue("10");
      this.pointsBox = this.addRenderableWidget(new EditBox(this.font, rx + 92, numY, 70, 16, Component.empty()));
      this.pointsBox.setFilter(s -> s.matches("\\d*"));
      this.pointsBox.setValue("10");

      this.addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.add_quest").withStyle(ChatFormatting.GREEN), b -> this.addQuest())
         .bounds(rx, numY + 22, rightW, 18).build());

      this.currentList = this.addRenderableWidget(new ScrollSelector<>(rx, numY + 54, rightW, this.panelHeight - (numY - this.topPos) - 54 - 28, 16,
         this::questLabel, this::questLabel, q -> new ItemStack(Items.WRITABLE_BOOK)));
      this.currentList.onSelect(this::removeQuest);
      this.refreshList();

      this.addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.close"), b -> this.onClose())
         .bounds(lx, this.topPos + this.panelHeight - 24, leftW, 18).build());
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
         return; // must pick an action
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
      this.currentList.setItems(new java.util.ArrayList<>(this.target));
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
      g.drawString(this.font, "\u00a7d\u2726 \u00a7f" + this.heading.getString() + " \u00a77(" + Component.translatable("fantasticpass.gui.manual").getString() + ")", this.leftPos + 10, this.topPos + 6, 0xFFFFFF, false);

      int lx = this.leftPos + 12;
      int leftW = (this.panelWidth - 36) / 2;
      int rx = lx + leftW + 12;
      int numY = this.topPos + 78;

      g.drawString(this.font, "\u00a77" + Component.translatable("fantasticpass.gui.custom_quest_title").getString(), lx, this.topPos + 24, 0xC0C0C0, false);
      g.drawString(this.font, "\u00a77" + Component.translatable("fantasticpass.gui.custom_quest_action").getString(), lx, this.topPos + 58, 0xC0C0C0, false);
      g.drawString(this.font, "\u00a77" + Component.translatable("fantasticpass.gui.count").getString(), rx, numY - 11, 0xC0C0C0, false);
      g.drawString(this.font, "\u00a77" + Component.translatable("fantasticpass.gui.points_field").getString(), rx + 92, numY - 11, 0xC0C0C0, false);
      g.drawString(this.font, "\u00a78" + Component.translatable("fantasticpass.gui.quest_remove_hint").getString(), rx, numY + 42, 0x9A9A9A, false);

      super.render(g, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
