package com.fantasticpass.gui.admin;

import com.fantasticpass.data.NametagStyle;
import com.fantasticpass.data.PassRankReward;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.gui.RegistryItems;
import com.fantasticpass.gui.widgets.GradientToggleWidget;
import com.fantasticpass.gui.widgets.ScrollSelector;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Clean panel-style tier reward editor: pick an item on the left, add it to the
 * free or premium track in the middle, see/remove current rewards on the right.
 * Same look & feel as the Fantastic Spawner / Crates editors. Spanish UI.
 */
public class TierEditorScreen extends Screen {
   private final Screen parent;
   private final TierDefinition tier;
   private int leftPos;
   private int topPos;
   private int panelWidth;
   private int panelHeight;
   private final List<Label> labels = new ArrayList<>();
   private EditBox searchBox;
   private EditBox countBox;
   private EditBox freeCmdBox;
   private EditBox premiumCmdBox;
   private EditBox rankIdBox;
   private ScrollSelector<Item> itemSelector;
   private ScrollSelector<RewardRow> rewardSelector;

   public TierEditorScreen(Screen parent, TierDefinition tier) {
      super(Component.translatable("fantasticpass.gui.tier_info", tier == null ? 0 : tier.getTierNumber()));
      this.parent = parent;
      this.tier = tier;
   }

   @Override
   protected void init() {
      this.panelWidth = Math.min(this.width - 20, 500);
      this.panelHeight = Math.min(this.height - 20, 280);
      this.leftPos = (this.width - this.panelWidth) / 2;
      this.topPos = (this.height - this.panelHeight) / 2;
      this.labels.clear();

      int bodyY = this.topPos + 44;
      int listH = this.panelHeight - 44 - 30;
      int colW = (this.panelWidth - 24 - 16) / 3;
      int leftX = this.leftPos + 12;
      int midX = leftX + colW + 8;
      int rightX = midX + colW + 8;

      // Left: item search + list.
      this.searchBox = this.addRenderableWidget(new EditBox(this.font, leftX, bodyY, colW, 16, Component.empty()));
      this.searchBox.setHint(Component.translatable("fantasticpass.gui.search"));
      this.searchBox.setResponder(s -> {
         if (this.itemSelector != null) {
            this.itemSelector.setQuery(s);
         }
      });
      this.itemSelector = this.addRenderableWidget(
         new ScrollSelector<Item>(leftX, bodyY + 20, colW, listH - 20, 18, RegistryItems::name, it -> RegistryItems.name(it) + " " + RegistryItems.id(it), ItemStack::new)
      );
      this.itemSelector.setItems(RegistryItems.all());
      this.labels.add(new Label("\u00a7f" + Component.translatable("fantasticpass.gui.all_items").getString(), leftX, bodyY - 12, 0xE0E0E0));

      // Middle: count + add buttons + commands + rank.
      this.labels.add(new Label("\u00a77" + Component.translatable("fantasticpass.gui.count").getString(), midX, bodyY + 3, 0xC0C0C0));
      this.countBox = this.addRenderableWidget(new EditBox(this.font, midX + 60, bodyY, colW - 60, 16, Component.empty()));
      this.countBox.setFilter(s -> s.matches("\\d*"));
      this.countBox.setValue("1");
      int halfW = (colW - 4) / 2;
      this.addRenderableWidget(
         Button.builder(Component.translatable("fantasticpass.gui.add_free").withStyle(net.minecraft.ChatFormatting.AQUA), b -> this.addItem(false))
            .bounds(midX, bodyY + 22, halfW, 18).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("fantasticpass.gui.add_premium").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), b -> this.addItem(true))
            .bounds(midX + halfW + 4, bodyY + 22, colW - halfW - 4, 18).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("fantasticpass.gui.add_nbt").withStyle(net.minecraft.ChatFormatting.GOLD), b -> this.openNbtForNew())
            .bounds(midX, bodyY + 44, colW, 18).build()
      );

      this.labels.add(new Label("\u00a78" + Component.translatable("fantasticpass.gui.cmd_hint").getString(), midX, bodyY + 68, 0x9A9A9A));
      this.freeCmdBox = this.addRenderableWidget(new EditBox(this.font, midX, bodyY + 80, colW - 24, 16, Component.empty()));
      this.freeCmdBox.setMaxLength(256);
      this.freeCmdBox.setHint(Component.translatable("fantasticpass.gui.free_cmd"));
      this.addRenderableWidget(Button.builder(Component.literal("+"), b -> this.addCommand(false)).bounds(midX + colW - 20, bodyY + 80, 20, 16).build());
      this.premiumCmdBox = this.addRenderableWidget(new EditBox(this.font, midX, bodyY + 100, colW - 24, 16, Component.empty()));
      this.premiumCmdBox.setMaxLength(256);
      this.premiumCmdBox.setHint(Component.translatable("fantasticpass.gui.premium_cmd"));
      this.addRenderableWidget(Button.builder(Component.literal("+"), b -> this.addCommand(true)).bounds(midX + colW - 20, bodyY + 100, 20, 16).build());

      this.addRenderableWidget(
         new GradientToggleWidget(midX, bodyY + 124, colW, 16, Component.translatable("fantasticpass.gui.rank_reward"), this.tier.hasRankReward(), this::onRankToggle)
      );
      if (this.tier.getRankReward() != null) {
         PassRankReward reward = this.tier.getRankReward();
         this.rankIdBox = this.addRenderableWidget(new EditBox(this.font, midX, bodyY + 144, colW, 16, Component.empty()));
         this.rankIdBox.setMaxLength(48);
         this.rankIdBox.setHint(Component.translatable("fantasticpass.gui.rank_id"));
         this.rankIdBox.setValue(reward.getRankId());
         this.rankIdBox.setResponder(reward::setRankId);
         this.addRenderableWidget(
            Button.builder(Component.translatable("fantasticpass.gui.edit_style"), b -> this.openColorEditor(reward)).bounds(midX, bodyY + 164, colW, 16).build()
         );
      }

      // Right: current rewards (click to edit NBT / remove).
      this.rewardSelector = this.addRenderableWidget(
         new ScrollSelector<RewardRow>(rightX, bodyY, colW, listH, 18, r -> r.label, r -> r.label, r -> r.icon)
      );
      this.rewardSelector.onSelect(this::onRewardClicked);
      this.refreshRewards();
      this.labels.add(new Label("\u00a7f" + Component.translatable("fantasticpass.gui.current_rewards").getString(), rightX, bodyY - 12, 0xE0E0E0));

      this.addRenderableWidget(
         Button.builder(Component.translatable("fantasticpass.gui.close"), b -> this.onClose())
            .bounds(this.leftPos + this.panelWidth - 92, this.topPos + this.panelHeight - 24, 84, 18).build()
      );
   }

   private int parseCount() {
      try {
         int c = this.countBox.getValue().isEmpty() ? 1 : Integer.parseInt(this.countBox.getValue());
         return Math.max(1, Math.min(64, c));
      } catch (NumberFormatException e) {
         return 1;
      }
   }

   private void addItem(boolean premium) {
      Item selected = this.itemSelector.getSelected();
      if (selected != null) {
         ItemStack stack = new ItemStack(selected, this.parseCount());
         (premium ? this.tier.getPremiumRewards() : this.tier.getFreeRewards()).add(stack);
         this.refreshRewards();
      }
   }

   /** Open the NBT editor to add a brand-new custom item (from the left selection). */
   private void openNbtForNew() {
      Item selected = this.itemSelector.getSelected();
      if (selected != null) {
         ItemStack base = new ItemStack(selected, this.parseCount());
         Minecraft.getInstance().setScreen(new NbtEditorScreen(this, base,
            (stack, premium) -> (premium ? this.tier.getPremiumRewards() : this.tier.getFreeRewards()).add(stack),
            null));
      }
   }

   /** Click on a current reward: items open the NBT editor; commands are removed; rank opens the style editor. */
   private void onRewardClicked(RewardRow row) {
      if (row.kind == Kind.RANK) {
         PassRankReward reward = this.tier.getRankReward();
         if (reward != null) {
            this.openColorEditor(reward);
         }
         return;
      }
      if (row.kind == Kind.FREE_ITEM || row.kind == Kind.PREMIUM_ITEM) {
         boolean fromPremium = row.kind == Kind.PREMIUM_ITEM;
         List<ItemStack> list = fromPremium ? this.tier.getPremiumRewards() : this.tier.getFreeRewards();
         int idx = row.index;
         if (idx < 0 || idx >= list.size()) {
            return;
         }

         ItemStack current = list.get(idx);
         Minecraft.getInstance().setScreen(new NbtEditorScreen(this, current,
            (stack, premium) -> {
               safeRemove(list, idx);
               (premium ? this.tier.getPremiumRewards() : this.tier.getFreeRewards()).add(stack);
            },
            () -> safeRemove(list, idx)));
      } else {
         this.removeReward(row);
      }
   }

   private void addCommand(boolean premium) {
      EditBox box = premium ? this.premiumCmdBox : this.freeCmdBox;
      String value = box.getValue().trim();
      if (!value.isEmpty()) {
         (premium ? this.tier.getPremiumCommands() : this.tier.getFreeCommands()).add(value);
         box.setValue("");
         this.refreshRewards();
      }
   }

   private void removeReward(RewardRow row) {
      switch (row.kind) {
         case FREE_ITEM -> safeRemove(this.tier.getFreeRewards(), row.index);
         case PREMIUM_ITEM -> safeRemove(this.tier.getPremiumRewards(), row.index);
         case FREE_CMD -> safeRemove(this.tier.getFreeCommands(), row.index);
         case PREMIUM_CMD -> safeRemove(this.tier.getPremiumCommands(), row.index);
         case RANK -> {
         } // rank is toggled off via the switch, not removed by click
      }

      this.refreshRewards();
   }

   private static void safeRemove(List<?> list, int index) {
      if (index >= 0 && index < list.size()) {
         list.remove(index);
      }
   }

   private void refreshRewards() {
      List<RewardRow> rows = new ArrayList<>();
      List<ItemStack> free = this.tier.getFreeRewards();
      for (int i = 0; i < free.size(); i++) {
         ItemStack s = free.get(i);
         rows.add(new RewardRow(Kind.FREE_ITEM, i, "\u00a7b" + s.getCount() + "x " + s.getHoverName().getString(), s));
      }

      List<ItemStack> prem = this.tier.getPremiumRewards();
      for (int i = 0; i < prem.size(); i++) {
         ItemStack s = prem.get(i);
         rows.add(new RewardRow(Kind.PREMIUM_ITEM, i, "\u00a7d[P] " + s.getCount() + "x " + s.getHoverName().getString(), s));
      }

      List<String> fc = this.tier.getFreeCommands();
      for (int i = 0; i < fc.size(); i++) {
         rows.add(new RewardRow(Kind.FREE_CMD, i, "\u00a7b/ " + fc.get(i), ItemStack.EMPTY));
      }

      List<String> pc = this.tier.getPremiumCommands();
      for (int i = 0; i < pc.size(); i++) {
         rows.add(new RewardRow(Kind.PREMIUM_CMD, i, "\u00a7d[P] /" + pc.get(i), ItemStack.EMPTY));
      }

      // Visual rank shown as a reward row so it's clearly part of this tier
      // (cosmetic, granted with the FREE reward). Click it to edit its style.
      if (this.tier.hasRankReward()) {
         PassRankReward rr = this.tier.getRankReward();
         String txt = rr.getRankDisplayText() == null || rr.getRankDisplayText().isEmpty()
            ? Component.translatable("fantasticpass.gui.rank_untitled").getString()
            : rr.getRankDisplayText();
         rows.add(new RewardRow(Kind.RANK, 0, "\u00a7d\u2756 " + Component.translatable("fantasticpass.gui.rank_reward").getString() + ": \u00a7f" + txt,
            new ItemStack(Items.NAME_TAG)));
      }

      this.rewardSelector.setItems(rows);
      this.rewardSelector.clearSelection();
   }

   private void onRankToggle(boolean on) {
      if (on) {
         if (this.tier.getRankReward() == null) {
            this.tier.setRankReward(new PassRankReward("", "", new NametagStyle()));
         }
      } else {
         this.tier.setRankReward(null);
      }

      this.rebuildWidgets();
   }

   private void openColorEditor(PassRankReward reward) {
      Minecraft.getInstance().setScreen(new ColorEditorScreen(this, reward.getStyle(), reward.getRankDisplayText(), (style, text) -> {
         reward.setStyle(style);
         reward.setRankDisplayText(text);
      }));
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
      g.drawString(this.font, "\u00a7d\u2726 \u00a7f" + this.title.getString() + " \u2014 " + Component.translatable("fantasticpass.gui.rewards").getString(), this.leftPos + 10, this.topPos + 6, 0xFFFFFF, false);

      super.render(g, mouseX, mouseY, partialTick);

      for (Label l : this.labels) {
         g.drawString(this.font, l.text, l.x, l.y, l.color, false);
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private enum Kind {
      FREE_ITEM,
      PREMIUM_ITEM,
      FREE_CMD,
      PREMIUM_CMD,
      RANK;
   }

   private record RewardRow(Kind kind, int index, String label, ItemStack icon) {
   }

   private record Label(String text, int x, int y, int color) {
   }
}
