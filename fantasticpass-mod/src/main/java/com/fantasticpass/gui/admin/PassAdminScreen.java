package com.fantasticpass.gui.admin;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.network.PacketHandler;
import com.fantasticpass.network.SavePassPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Clean panel-style pass editor (same look & feel as the Fantastic Spawner /
 * Crates editors): a framed panel with a header, tabs, a body and a footer with
 * Save / Close. Everything is in Spanish and laid out with labelled fields.
 */
public class PassAdminScreen extends Screen {
   private final PassDefinition pass;
   private Tab tab = Tab.GENERAL;
   private int page;
   private int leftPos;
   private int topPos;
   private int panelWidth;
   private int panelHeight;
   private final List<Label> labels = new ArrayList<>();

   public PassAdminScreen(PassDefinition pass) {
      super(Component.translatable("fantasticpass.gui.admin.title"));
      this.pass = pass;
   }

   @Override
   protected void init() {
      this.panelWidth = Math.min(this.width - 20, 440);
      this.panelHeight = Math.min(this.height - 20, 250);
      this.leftPos = (this.width - this.panelWidth) / 2;
      this.topPos = (this.height - this.panelHeight) / 2;
      this.labels.clear();
      this.initTabs();
      this.initFooter();
      if (this.tab == Tab.GENERAL) {
         this.buildGeneralTab();
      } else if (this.tab == Tab.QUESTS) {
         this.buildQuestsTab();
      } else {
         this.buildTiersTab();
      }
   }

   private int bodyX() {
      return this.leftPos + 12;
   }

   private int bodyY() {
      return this.topPos + 56;
   }

   private void initTabs() {
      Tab[] tabs = Tab.values();
      int gap = 3;
      int tabW = (this.panelWidth - 24 - gap * (tabs.length - 1)) / tabs.length;
      int x = this.leftPos + 12;
      int y = this.topPos + 24;
      for (Tab t : tabs) {
         boolean active = t == this.tab;
         String text = (active ? "\u00a7f" : "\u00a77") + Component.translatable(t.key).getString();
         this.addRenderableWidget(Button.builder(Component.literal(text), b -> this.switchTab(t)).bounds(x, y, tabW, 18).build());
         x += tabW + gap;
      }
   }

   private void initFooter() {
      int y = this.topPos + this.panelHeight - 26;
      this.addRenderableWidget(
         Button.builder(Component.translatable("fantasticpass.gui.save").withStyle(net.minecraft.ChatFormatting.GREEN), b -> this.save())
            .bounds(this.leftPos + this.panelWidth - 158, y, 150, 18)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("fantasticpass.gui.close"), b -> this.onClose()).bounds(this.leftPos + 12, y, 90, 18).build()
      );
   }

   private void switchTab(Tab newTab) {
      this.tab = newTab;
      this.rebuildWidgets();
   }

   private void buildGeneralTab() {
      int x = this.bodyX();
      int y = this.bodyY();
      int fieldX = x + 170;
      int fieldW = this.panelWidth - 24 - 170;

      EditBox nameField = this.addRenderableWidget(new EditBox(this.font, fieldX, y, fieldW, 18, Component.empty()));
      nameField.setMaxLength(48);
      nameField.setValue(this.pass.getName());
      nameField.setResponder(this.pass::setName);
      this.labels.add(new Label(Component.translatable("fantasticpass.gui.name").getString(), x, y + 5, 0xE0E0E0));

      EditBox idField = this.addRenderableWidget(new EditBox(this.font, fieldX, y + 26, fieldW, 18, Component.empty()));
      idField.setMaxLength(48);
      idField.setValue(this.pass.getId());
      idField.setFilter(s -> s.matches("[a-zA-Z0-9_\\-]*"));
      idField.setResponder(this.pass::setId);
      this.labels.add(new Label(Component.translatable("fantasticpass.gui.id").getString(), x, y + 31, 0xE0E0E0));

      EditBox tierCountField = this.addRenderableWidget(new EditBox(this.font, fieldX, y + 52, 70, 18, Component.empty()));
      tierCountField.setMaxLength(3);
      tierCountField.setFilter(s -> s.matches("\\d*"));
      tierCountField.setValue(String.valueOf(this.pass.getTierCount()));
      tierCountField.setResponder(this::onTierCountChanged);
      this.labels.add(new Label(Component.translatable("fantasticpass.gui.tier_count").getString() + " \u00a78(1-100)", x, y + 57, 0xE0E0E0));

      EditBox minutesField = this.addRenderableWidget(new EditBox(this.font, fieldX, y + 78, 70, 18, Component.empty()));
      minutesField.setMaxLength(6);
      minutesField.setFilter(s -> s.matches("\\d*"));
      minutesField.setValue(String.valueOf(this.pass.getMinutesPerTierOverride()));
      minutesField.setResponder(this::onMinutesChanged);
      this.labels.add(new Label(Component.translatable("fantasticpass.gui.minutes_per_tier").getString() + " \u00a78(0=global)", x, y + 83, 0xE0E0E0));

      this.labels.add(new Label("\u00a77" + Component.translatable("fantasticpass.gui.general_hint").getString(), x, y + 112, 0x9A9A9A));
   }

   private void onTierCountChanged(String value) {
      try {
         if (!value.isEmpty()) {
            this.pass.setTierCount(Integer.parseInt(value));
         }
      } catch (NumberFormatException ignored) {
      }
   }

   private void onMinutesChanged(String value) {
      try {
         this.pass.setMinutesPerTierOverride(value.isEmpty() ? 0 : Integer.parseInt(value));
      } catch (NumberFormatException ignored) {
      }
   }

   private int tierPages() {
      return Math.max(1, (this.pass.getTierCount() + 9) / 10);
   }

   private void buildQuestsTab() {
      int x = this.bodyX();
      int y = this.bodyY();
      int fieldX = x + 230;

      EditBox freeField = this.addRenderableWidget(new EditBox(this.font, fieldX, y, 60, 18, Component.empty()));
      freeField.setMaxLength(2);
      freeField.setFilter(s -> s.matches("\\d*"));
      freeField.setValue(String.valueOf(this.pass.getDailyFreeCount()));
      freeField.setResponder(v -> this.setInt(v, this.pass::setDailyFreeCount));
      this.labels.add(new Label(Component.translatable("fantasticpass.gui.daily_free_count").getString() + " \u00a78(0=global)", x, y + 5, 0xE0E0E0));

      EditBox premField = this.addRenderableWidget(new EditBox(this.font, fieldX, y + 26, 60, 18, Component.empty()));
      premField.setMaxLength(2);
      premField.setFilter(s -> s.matches("\\d*"));
      premField.setValue(String.valueOf(this.pass.getDailyPremiumCount()));
      premField.setResponder(v -> this.setInt(v, this.pass::setDailyPremiumCount));
      this.labels.add(new Label(Component.translatable("fantasticpass.gui.daily_premium_count").getString() + " \u00a78(0=global)", x, y + 31, 0xE0E0E0));

      EditBox weekField = this.addRenderableWidget(new EditBox(this.font, fieldX, y + 52, 60, 18, Component.empty()));
      weekField.setMaxLength(1);
      weekField.setFilter(s -> s.matches("\\d*"));
      weekField.setValue(String.valueOf(this.pass.getWeekCountOverride()));
      weekField.setResponder(v -> this.setInt(v, this.pass::setWeekCountOverride));
      this.labels.add(new Label(Component.translatable("fantasticpass.gui.week_count_field").getString() + " \u00a78(0=global, max 8)", x, y + 57, 0xE0E0E0));

      this.labels.add(new Label("\u00a77" + Component.translatable("fantasticpass.gui.quests_pool_info",
            com.fantasticpass.quest.DefaultQuests.DAILY_FREE_POOL.size(),
            com.fantasticpass.quest.DefaultQuests.DAILY_PREMIUM_POOL.size(),
            com.fantasticpass.quest.DefaultQuests.maxWeeks()).getString(),
         x, y + 92, 0x9A9A9A));
      this.labels.add(new Label("\u00a77" + Component.translatable("fantasticpass.gui.quests_hint").getString(), x, y + 108, 0x9A9A9A));
   }

   private void setInt(String value, java.util.function.IntConsumer setter) {
      try {
         setter.accept(value.isEmpty() ? 0 : Integer.parseInt(value));
      } catch (NumberFormatException ignored) {
      }
   }

   private void buildTiersTab() {
      int x = this.bodyX();
      int y = this.bodyY();
      this.page = Math.min(this.page, this.tierPages() - 1);
      this.addRenderableWidget(Button.builder(Component.literal("\u25c0"), b -> this.changePage(-1)).bounds(x, y, 22, 18).build());
      this.addRenderableWidget(Button.builder(Component.literal("\u25b6"), b -> this.changePage(1)).bounds(x + 26, y, 22, 18).build());

      int gridY = y + 26;
      int colW = (this.panelWidth - 24 - 8) / 2;
      for (int i = 0; i < 10; i++) {
         int tierNumber = this.page * 10 + i + 1;
         if (tierNumber > this.pass.getTierCount()) {
            break;
         }

         int col = i / 5;
         int row = i % 5;
         TierDefinition def = this.pass.getTier(tierNumber);
         String marker = def != null && !def.isEmpty() ? " \u00a7a\u2714" : " \u00a78\u2014";
         this.addRenderableWidget(
            Button.builder(Component.literal(Component.translatable("fantasticpass.gui.tier_info", tierNumber).getString() + marker), b -> this.openTier(tierNumber))
               .bounds(x + col * (colW + 8), gridY + row * 22, colW, 20)
               .build()
         );
      }
   }

   private void changePage(int delta) {
      this.page = Math.max(0, Math.min(this.tierPages() - 1, this.page + delta));
      this.rebuildWidgets();
   }

   private void openTier(int tierNumber) {
      Minecraft.getInstance().setScreen(new TierEditorScreen(this, this.pass.getTier(tierNumber)));
   }

   private void save() {
      if (this.pass.getId() != null && !this.pass.getId().isEmpty()) {
         PacketHandler.sendToServer(new SavePassPacket(this.pass));
         this.onClose();
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(null);
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(g);
      // Panel + header + footer bands (Fantastic Spawner style).
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xE0181A1F);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, 0xFF24262E);
      g.fill(this.leftPos, this.topPos + this.panelHeight - 1, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xFF3A2E12);
      g.fill(this.leftPos + 6, this.topPos + 45, this.leftPos + this.panelWidth - 6, this.topPos + 46, 0xFF3A2E12);
      g.renderOutline(this.leftPos, this.topPos, this.panelWidth, this.panelHeight, 0xFF5A4A1E);
      g.drawString(this.font, "\u00a7d\u2726 \u00a7fFantastic Pass \u00a76Editor \u00a7d\u2726", this.leftPos + 10, this.topPos + 6, 0xFFFFFF, false);

      if (this.tab == Tab.TIERS) {
         g.drawCenteredString(this.font, "\u00a7e" + (this.page + 1) + "/" + this.tierPages(), this.bodyX() + 90, this.bodyY() + 5, 0xFFFFFF);
         g.drawString(this.font, "\u00a78" + Component.translatable("fantasticpass.gui.tiers_hint").getString(), this.bodyX(), this.topPos + this.panelHeight - 40, 0x9A9A9A, false);
      }

      super.render(g, mouseX, mouseY, partialTick);

      for (Label l : this.labels) {
         g.drawString(this.font, l.text, l.x, l.y, l.color, false);
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private enum Tab {
      GENERAL("fantasticpass.gui.general"),
      QUESTS("fantasticpass.gui.quests"),
      TIERS("fantasticpass.gui.tiers");

      final String key;

      Tab(String key) {
         this.key = key;
      }
   }

   private record Label(String text, int x, int y, int color) {
   }
}
