package com.fantasticpass.gui.admin;

import com.fantasticpass.data.NametagStyle;
import com.fantasticpass.data.PassRankReward;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.gui.GuiTheme;
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
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TierEditorScreen extends Screen {
   private final Screen parent;
   private final TierDefinition tier;
   private EditBox searchBox;
   private EditBox countBox;
   private EditBox freeCmdBox;
   private EditBox premiumCmdBox;
   private EditBox rankIdBox;
   private ScrollSelector<Item> itemSelector;
   private ScrollSelector<TierEditorScreen.RewardRow> rewardSelector;

   public TierEditorScreen(Screen parent, TierDefinition tier) {
      super(Component.literal("Tier " + (tier == null ? 0 : tier.getTierNumber()) + " — Rewards"));
      this.parent = parent;
      this.tier = tier;
   }

   protected void init() {
      int listTop = 60;
      int listBottom = this.height - 16;
      int listHeight = Math.max(40, listBottom - listTop);
      this.searchBox = (EditBox)this.addRenderableWidget(new EditBox(this.font, 10, 40, 150, 16, Component.literal("search")));
      this.searchBox.setHint(Component.literal("Search items..."));
      this.searchBox.setResponder(s -> {
         if (this.itemSelector != null) {
            this.itemSelector.setQuery(s);
         }
      });
      this.itemSelector = (ScrollSelector<Item>)this.addRenderableWidget(
         new ScrollSelector<Item>(
            10, listTop, 156, listHeight, 18, RegistryItems::name, it -> RegistryItems.name(it) + " " + RegistryItems.id(it), ItemStack::new
         )
      );
      this.itemSelector.setItems(RegistryItems.all());
      int cx = 176;
      this.countBox = (EditBox)this.addRenderableWidget(new EditBox(this.font, cx, 40, 50, 16, Component.literal("count")));
      this.countBox.setFilter(s -> s.matches("\\d*"));
      this.countBox.setValue("1");
      this.addRenderableWidget(Button.builder(Component.literal("Add → Free"), b -> this.addItem(false)).bounds(cx, 60, 140, 18).build());
      this.addRenderableWidget(Button.builder(Component.literal("Add → Premium"), b -> this.addItem(true)).bounds(cx, 82, 140, 18).build());
      this.freeCmdBox = (EditBox)this.addRenderableWidget(new EditBox(this.font, cx, 116, 112, 16, Component.literal("free cmd")));
      this.freeCmdBox.setMaxLength(256);
      this.freeCmdBox.setHint(Component.literal("free command {player}"));
      this.addRenderableWidget(Button.builder(Component.literal("+"), b -> this.addCommand(false)).bounds(cx + 116, 116, 24, 16).build());
      this.premiumCmdBox = (EditBox)this.addRenderableWidget(new EditBox(this.font, cx, 140, 112, 16, Component.literal("premium cmd")));
      this.premiumCmdBox.setMaxLength(256);
      this.premiumCmdBox.setHint(Component.literal("premium command {player}"));
      this.addRenderableWidget(Button.builder(Component.literal("+"), b -> this.addCommand(true)).bounds(cx + 116, 140, 24, 16).build());
      this.addRenderableWidget(new GradientToggleWidget(cx, 172, 140, 16, Component.literal("Pass Rank Reward"), this.tier.hasRankReward(), this::onRankToggle));
      if (this.tier.getRankReward() != null) {
         PassRankReward reward = this.tier.getRankReward();
         this.rankIdBox = (EditBox)this.addRenderableWidget(new EditBox(this.font, cx, 192, 140, 16, Component.literal("rank id")));
         this.rankIdBox.setMaxLength(48);
         this.rankIdBox.setValue(reward.getRankId());
         this.rankIdBox.setResponder(reward::setRankId);
         this.addRenderableWidget(Button.builder(Component.literal("Edit Style & Text"), b -> this.openColorEditor(reward)).bounds(cx, 212, 140, 16).build());
      }

      int rx = 326;
      int rWidth = Math.max(80, this.width - rx - 10);
      this.rewardSelector = (ScrollSelector<TierEditorScreen.RewardRow>)this.addRenderableWidget(
         new ScrollSelector<TierEditorScreen.RewardRow>(rx, listTop, rWidth, listHeight, 18, r -> r.label, r -> r.label, r -> r.icon)
      );
      this.rewardSelector.onSelect(this::removeReward);
      this.refreshRewards();
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose()).bounds(this.width - 90, 8, 80, 18).build());
   }

   private int parseCount() {
      try {
         int c = this.countBox.getValue().isEmpty() ? 1 : Integer.parseInt(this.countBox.getValue());
         return Math.max(1, Math.min(64, c));
      } catch (NumberFormatException var2) {
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

   private void addCommand(boolean premium) {
      EditBox box = premium ? this.premiumCmdBox : this.freeCmdBox;
      String value = box.getValue().trim();
      if (!value.isEmpty()) {
         (premium ? this.tier.getPremiumCommands() : this.tier.getFreeCommands()).add(value);
         box.setValue("");
         this.refreshRewards();
      }
   }

   private void removeReward(TierEditorScreen.RewardRow row) {
      switch (row.kind) {
         case FREE_ITEM:
            safeRemove(this.tier.getFreeRewards(), row.index);
            break;
         case PREMIUM_ITEM:
            safeRemove(this.tier.getPremiumRewards(), row.index);
            break;
         case FREE_CMD:
            safeRemove(this.tier.getFreeCommands(), row.index);
            break;
         case PREMIUM_CMD:
            safeRemove(this.tier.getPremiumCommands(), row.index);
      }

      this.refreshRewards();
   }

   private static void safeRemove(List<?> list, int index) {
      if (index >= 0 && index < list.size()) {
         list.remove(index);
      }
   }

   private void refreshRewards() {
      List<TierEditorScreen.RewardRow> rows = new ArrayList<>();
      List<ItemStack> free = this.tier.getFreeRewards();

      for (int i = 0; i < free.size(); i++) {
         ItemStack s = free.get(i);
         rows.add(new TierEditorScreen.RewardRow(TierEditorScreen.Kind.FREE_ITEM, i, "§f" + s.getCount() + "x " + s.getHoverName().getString(), s));
      }

      List<ItemStack> prem = this.tier.getPremiumRewards();

      for (int i = 0; i < prem.size(); i++) {
         ItemStack s = prem.get(i);
         rows.add(new TierEditorScreen.RewardRow(TierEditorScreen.Kind.PREMIUM_ITEM, i, "§6[P] " + s.getCount() + "x " + s.getHoverName().getString(), s));
      }

      List<String> fc = this.tier.getFreeCommands();

      for (int i = 0; i < fc.size(); i++) {
         rows.add(new TierEditorScreen.RewardRow(TierEditorScreen.Kind.FREE_CMD, i, "§b/ " + fc.get(i), ItemStack.EMPTY));
      }

      List<String> pc = this.tier.getPremiumCommands();

      for (int i = 0; i < pc.size(); i++) {
         rows.add(new TierEditorScreen.RewardRow(TierEditorScreen.Kind.PREMIUM_CMD, i, "§6[P] /" + pc.get(i), ItemStack.EMPTY));
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

   public void onClose() {
      Minecraft.getInstance().setScreen(this.parent);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      GuiTheme.drawBackground(graphics, this.width, this.height);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, -16718337);
      graphics.drawString(this.font, Component.literal("All Items"), 10, 30, -5592406, false);
      graphics.drawString(this.font, Component.literal("Count"), 176, 30, -5592406, false);
      graphics.drawString(this.font, Component.literal("Current Rewards (click to remove)"), 326, 30, -5592406, false);
      graphics.drawString(this.font, Component.literal("Commands use {player}"), 176, 104, -8947832, false);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static enum Kind {
      FREE_ITEM,
      PREMIUM_ITEM,
      FREE_CMD,
      PREMIUM_CMD;
   }

   private static final class RewardRow {
      final TierEditorScreen.Kind kind;
      final int index;
      final String label;
      final ItemStack icon;

      RewardRow(TierEditorScreen.Kind kind, int index, String label, ItemStack icon) {
         this.kind = kind;
         this.index = index;
         this.label = label;
         this.icon = icon;
      }
   }
}
