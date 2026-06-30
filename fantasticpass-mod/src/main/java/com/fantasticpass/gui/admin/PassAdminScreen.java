package com.fantasticpass.gui.admin;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.gui.GuiTheme;
import com.fantasticpass.network.PacketHandler;
import com.fantasticpass.network.SavePassPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class PassAdminScreen extends Screen {
   private static final int TIERS_PER_PAGE = 10;
   private final PassDefinition pass;
   private PassAdminScreen.Tab tab = PassAdminScreen.Tab.GENERAL;
   private int page;
   private EditBox nameField;
   private EditBox idField;
   private EditBox minutesField;
   private EditBox tierCountField;

   public PassAdminScreen(PassDefinition pass) {
      super(Component.translatable("fantasticpass.gui.admin.title"));
      this.pass = pass;
   }

   protected void init() {
      this.addRenderableWidget(
         Button.builder(Component.translatable("fantasticpass.gui.general"), b -> this.switchTab(PassAdminScreen.Tab.GENERAL)).bounds(10, 26, 70, 18).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("fantasticpass.gui.tiers"), b -> this.switchTab(PassAdminScreen.Tab.TIERS)).bounds(84, 26, 70, 18).build()
      );
      this.addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.save"), b -> this.save()).bounds(this.width - 184, 26, 84, 18).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose()).bounds(this.width - 94, 26, 84, 18).build());
      if (this.tab == PassAdminScreen.Tab.GENERAL) {
         this.buildGeneralTab();
      } else {
         this.buildTiersTab();
      }
   }

   private void switchTab(PassAdminScreen.Tab newTab) {
      this.tab = newTab;
      this.rebuildWidgets();
   }

   private void buildGeneralTab() {
      this.nameField = (EditBox)this.addRenderableWidget(new EditBox(this.font, 20, 84, 220, 18, Component.translatable("fantasticpass.gui.name")));
      this.nameField.setMaxLength(48);
      this.nameField.setValue(this.pass.getName());
      this.nameField.setResponder(this.pass::setName);
      this.idField = (EditBox)this.addRenderableWidget(new EditBox(this.font, 20, 124, 220, 18, Component.translatable("fantasticpass.gui.id")));
      this.idField.setMaxLength(48);
      this.idField.setValue(this.pass.getId());
      this.idField.setFilter(s -> s.matches("[a-zA-Z0-9_\\-]*"));
      this.idField.setResponder(this.pass::setId);
      this.minutesField = (EditBox)this.addRenderableWidget(
         new EditBox(this.font, 20, 164, 80, 18, Component.translatable("fantasticpass.gui.minutes_per_tier"))
      );
      this.minutesField.setMaxLength(6);
      this.minutesField.setFilter(s -> s.matches("\\d*"));
      this.minutesField.setValue(String.valueOf(this.pass.getMinutesPerTierOverride()));
      this.minutesField.setResponder(this::onMinutesChanged);
      this.tierCountField = (EditBox)this.addRenderableWidget(
         new EditBox(this.font, 20, 204, 80, 18, Component.translatable("fantasticpass.gui.tier_count"))
      );
      this.tierCountField.setMaxLength(3);
      this.tierCountField.setFilter(s -> s.matches("\\d*"));
      this.tierCountField.setValue(String.valueOf(this.pass.getTierCount()));
      this.tierCountField.setResponder(this::onTierCountChanged);
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
      } catch (NumberFormatException var3) {
      }
   }

   private void buildTiersTab() {
      int tierCount = this.pass.getTierCount();
      int pages = Math.max(1, (tierCount + 9) / 10);
      this.page = Math.min(this.page, pages - 1);
      this.addRenderableWidget(Button.builder(Component.literal("<"), b -> this.changePage(-1)).bounds(20, 58, 30, 16).build());
      this.addRenderableWidget(Button.builder(Component.literal(">"), b -> this.changePage(1)).bounds(94, 58, 30, 16).build());

      for (int i = 0; i < 10; i++) {
         int tierNumber = this.page * 10 + i + 1;
         if (tierNumber > tierCount) {
            break;
         }

         int col = i / 5;
         int row = i % 5;
         TierDefinition def = this.pass.getTier(tierNumber);
         String marker = def != null && !def.isEmpty() ? " §a✔" : "";
         this.addRenderableWidget(
            Button.builder(Component.literal("Tier " + tierNumber + marker), b -> this.openTier(tierNumber))
               .bounds(20 + col * 150, 82 + row * 22, 140, 20)
               .build()
         );
      }
   }

   private void changePage(int delta) {
      int pages = Math.max(1, (this.pass.getTierCount() + 9) / 10);
      this.page = Math.max(0, Math.min(pages - 1, this.page + delta));
      this.rebuildWidgets();
   }

   private void openTier(int tierNumber) {
      Minecraft.getInstance().setScreen(new TierEditorScreen(this, this.pass.getTier(tierNumber)));
   }

   private void save() {
      if (this.pass.getId() != null && !this.pass.getId().isEmpty()) {
         PacketHandler.sendToServer(new SavePassPacket(this.pass));
      }
   }

   public void onClose() {
      this.minecraft.setScreen(null);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      GuiTheme.drawBackground(graphics, this.width, this.height);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, -16718337);
      int tabX = this.tab == PassAdminScreen.Tab.GENERAL ? 10 : 84;
      graphics.fill(tabX, 45, tabX + 70, 46, -10496);
      if (this.tab == PassAdminScreen.Tab.GENERAL) {
         graphics.drawString(this.font, Component.translatable("fantasticpass.gui.name"), 20, 74, -5592406, false);
         graphics.drawString(this.font, Component.translatable("fantasticpass.gui.id"), 20, 114, -5592406, false);
         graphics.drawString(this.font, Component.translatable("fantasticpass.gui.minutes_per_tier"), 20, 154, -5592406, false);
         graphics.drawString(this.font, Component.literal("(0 = use global config)"), 110, 168, -8947832, false);
         graphics.drawString(this.font, Component.translatable("fantasticpass.gui.tier_count"), 20, 194, -5592406, false);
         graphics.drawString(this.font, Component.literal("(1-100)"), 110, 208, -8947832, false);
      } else {
         int pages = Math.max(1, (this.pass.getTierCount() + 9) / 10);
         graphics.drawCenteredString(this.font, "Page " + (this.page + 1) + "/" + pages, 72, 60, -1);
         graphics.drawString(this.font, Component.literal("Click a tier to edit its rewards. §a✔§7 = has rewards"), 20, this.height - 20, -8947832, false);
      }

      super.render(graphics, mouseX, mouseY, partialTick);
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static enum Tab {
      GENERAL,
      TIERS;
   }
}
