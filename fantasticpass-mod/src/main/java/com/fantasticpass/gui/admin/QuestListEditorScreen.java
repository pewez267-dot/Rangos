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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Visual editor for a single quest list (a daily pool or one week's free /
 * premium set). Pick an objective type; for parameterized types (kill entity /
 * mine block / craft item) a second picker lists EVERY registered entity, block
 * or item (vanilla and from any installed mod). Set target and points, add, and
 * remove existing quests by clicking. Edits the list in place; the parent
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

   private EditBox typeSearch;
   private EditBox paramSearch;
   private EditBox targetBox;
   private EditBox pointsBox;
   private ScrollSelector<QuestType> typeSelector;
   private ScrollSelector<ResourceLocation> paramSelector;
   private ScrollSelector<Quest> currentList;

   private QuestType selectedType;

   public QuestListEditorScreen(Screen parent, Component heading, List<Quest> target, String idPrefix) {
      super(Component.translatable("fantasticpass.gui.quest_editor"));
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

      // ---- Left: objective type ----
      this.typeSearch = this.addRenderableWidget(new EditBox(this.font, lx, this.topPos + 36, leftW, 16, Component.empty()));
      this.typeSearch.setHint(Component.translatable("fantasticpass.gui.search"));
      this.typeSearch.setResponder(q -> this.typeSelector.setQuery(q));

      this.typeSelector = this.addRenderableWidget(new ScrollSelector<>(lx, this.topPos + 56, leftW, this.panelHeight - 56 - 30, 16,
         t -> this.typeName(t), t -> this.typeName(t) + " " + t.getId(), t -> new ItemStack(Items.PAPER)));
      List<QuestType> types = new ArrayList<>();
      for (QuestType t : QuestType.values()) {
         if (t != QuestType.PLAY_MINUTES) {
            types.add(t);
         }
      }
      this.typeSelector.setItems(types);
      this.typeSelector.onSelect(this::onTypePicked);

      // ---- Right: target picker (for mod compat) + numbers + current list ----
      this.paramSearch = this.addRenderableWidget(new EditBox(this.font, rx, this.topPos + 36, rightW, 16, Component.empty()));
      this.paramSearch.setHint(Component.translatable("fantasticpass.gui.search"));
      this.paramSearch.setResponder(q -> this.paramSelector.setQuery(q));

      this.paramSelector = this.addRenderableWidget(new ScrollSelector<>(rx, this.topPos + 56, rightW, 60, 16,
         this::paramLabel, rl -> rl.toString(), this::paramIcon));

      int numY = this.topPos + 132;
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

      this.updateParamVisibility();
   }

   private String typeName(QuestType t) {
      return Component.translatable(t.descriptionKey(), "N", "...").getString();
   }

   private void onTypePicked(QuestType type) {
      this.selectedType = type;
      this.paramSearch.setValue("");
      this.paramSelector.setQuery("");
      this.paramSelector.clearSelection();
      this.paramSelector.setItems(this.targetsFor(type));
      this.updateParamVisibility();
   }

   private void updateParamVisibility() {
      boolean param = this.selectedType != null && this.selectedType.isParameterized();
      this.paramSearch.visible = param;
      this.paramSelector.visible = param;
      this.paramSearch.active = param;
      this.paramSelector.active = param;
   }

   private List<ResourceLocation> targetsFor(QuestType type) {
      List<ResourceLocation> out = new ArrayList<>();
      if (type == null) {
         return out;
      }
      switch (type.getParamKind()) {
         case ENTITY -> out.addAll(BuiltInRegistries.ENTITY_TYPE.keySet());
         case BLOCK -> out.addAll(BuiltInRegistries.BLOCK.keySet());
         case ITEM -> out.addAll(BuiltInRegistries.ITEM.keySet());
         default -> {
         }
      }
      out.sort((a, b) -> a.toString().compareTo(b.toString()));
      return out;
   }

   private String paramLabel(ResourceLocation rl) {
      if (this.selectedType == null) {
         return rl.toString();
      }
      return Quest.paramName(this.selectedType, rl.toString()).getString() + " \u00a78(" + rl + ")";
   }

   private ItemStack paramIcon(ResourceLocation rl) {
      if (this.selectedType == null) {
         return ItemStack.EMPTY;
      }
      switch (this.selectedType.getParamKind()) {
         case ITEM:
            return BuiltInRegistries.ITEM.containsKey(rl) ? new ItemStack(BuiltInRegistries.ITEM.get(rl)) : ItemStack.EMPTY;
         case BLOCK:
            return BuiltInRegistries.BLOCK.containsKey(rl) ? new ItemStack(BuiltInRegistries.BLOCK.get(rl)) : ItemStack.EMPTY;
         default:
            return new ItemStack(Items.NAME_TAG);
      }
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
      QuestType type = this.typeSelector.getSelected();
      if (type == null) {
         return;
      }
      String param = "";
      if (type.isParameterized()) {
         ResourceLocation sel = this.paramSelector.getSelected();
         if (sel == null) {
            return; // a parameterized quest needs a target
         }
         param = sel.toString();
      }

      String id = this.idPrefix + Long.toHexString((System.nanoTime() ^ (long)this.target.size() * 2654435761L) & 0xFFFFFFL);
      this.target.add(new Quest(id, type, param, this.parse(this.targetBox, 10), this.parse(this.pointsBox, 10)));
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

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(g);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xE0181A1F);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, 0xFF24262E);
      g.fill(this.leftPos, this.topPos + this.panelHeight - 1, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xFF3A2E12);
      g.renderOutline(this.leftPos, this.topPos, this.panelWidth, this.panelHeight, 0xFF5A4A1E);
      g.drawString(this.font, "\u00a7d\u2726 \u00a7f" + this.heading.getString(), this.leftPos + 10, this.topPos + 6, 0xFFFFFF, false);

      int lx = this.leftPos + 12;
      int leftW = (this.panelWidth - 36) / 2;
      int rx = lx + leftW + 12;
      int numY = this.topPos + 132;

      g.drawString(this.font, "\u00a77" + Component.translatable("fantasticpass.gui.objective_type").getString(), lx, this.topPos + 25, 0xC0C0C0, false);

      boolean param = this.selectedType != null && this.selectedType.isParameterized();
      if (param) {
         g.drawString(this.font, "\u00a77" + Component.translatable("fantasticpass.gui.objective_target").getString(), rx, this.topPos + 25, 0xC0C0C0, false);
      } else {
         g.drawString(this.font, "\u00a78" + Component.translatable("fantasticpass.gui.objective_none").getString(), rx, this.topPos + 60, 0x8A8A8A, false);
      }

      g.drawString(this.font, "\u00a77" + Component.translatable("fantasticpass.gui.count").getString(), rx, numY - 11, 0xC0C0C0, false);
      g.drawString(this.font, "\u00a77" + Component.translatable("fantasticpass.gui.points_field").getString(), rx + 92, numY - 11, 0xC0C0C0, false);
      g.drawString(this.font, "\u00a78" + Component.translatable("fantasticpass.gui.quest_remove_hint").getString(), rx, numY + 44, 0x9A9A9A, false);

      super.render(g, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
