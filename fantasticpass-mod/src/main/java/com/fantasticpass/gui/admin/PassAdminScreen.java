package com.fantasticpass.gui.admin;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.gui.widgets.ScrollSelector;
import com.fantasticpass.network.PacketHandler;
import com.fantasticpass.network.SavePassPacket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
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
 * Clean panel-style pass editor (same look & feel as the Fantastic Spawner /
 * Crates editors): a framed panel with a header, tabs, a body and a footer with
 * Save / Close. Every interactive element carries a hover tooltip that explains
 * what it does, and there is no loose grey helper text on the panel body.
 */
public class PassAdminScreen extends Screen {
   private final PassDefinition pass;
   private Tab tab = Tab.GENERAL;
   private int page;
   private int leftPos;
   private int topPos;
   private int panelWidth;
   private int panelHeight;
   private long errorUntil;
   private long msgUntil;
   private String msg = "";
   private EditBox musicUrlBox;
   private ScrollSelector<String> musicList;
   private EditBox bgUrlBox;
   private ScrollSelector<String> bgList;
   private EditBox questWeekField;
   private final List<Label> labels = new ArrayList<>();
   private final List<Hint> hints = new ArrayList<>();

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
      this.hints.clear();
      this.initTabs();
      this.initFooter();
      switch (this.tab) {
         case GENERAL -> this.buildGeneralTab();
         case QUESTS -> this.buildQuestsTab();
         case MUSIC -> this.buildMusicTab();
         case FONDOS -> this.buildBackgroundsTab();
         default -> this.buildTiersTab();
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
         Button b = this.addRenderableWidget(Button.builder(Component.literal(text), btn -> this.switchTab(t)).bounds(x, y, tabW, 18).build());
         this.addHint(x, y, tabW, 18, t.key, t.tipKey);
         x += tabW + gap;
      }
   }

   private void initFooter() {
      int y = this.topPos + this.panelHeight - 26;
      this.addRenderableWidget(
         Button.builder(Component.translatable("fantasticpass.gui.save").withStyle(ChatFormatting.GREEN), b -> this.save())
            .bounds(this.leftPos + this.panelWidth - 158, y, 150, 18)
            .build()
      );
      this.addHint(this.leftPos + this.panelWidth - 158, y, 150, 18, "fantasticpass.gui.save", "fantasticpass.gui.tip_save");
      this.addRenderableWidget(
         Button.builder(Component.translatable("fantasticpass.gui.close"), b -> this.onClose()).bounds(this.leftPos + 12, y, 90, 18).build()
      );
      this.addHint(this.leftPos + 12, y, 90, 18, "fantasticpass.gui.close", "fantasticpass.gui.tip_close");
   }

   private void switchTab(Tab newTab) {
      this.tab = newTab;
      this.rebuildWidgets();
   }

   // ---- General tab --------------------------------------------------------

   private void buildGeneralTab() {
      int x = this.bodyX();
      int y = this.bodyY();
      int fieldX = x + 150;
      int fieldW = this.panelWidth - 24 - 150;

      EditBox nameField = this.addRenderableWidget(new EditBox(this.font, fieldX, y, fieldW, 18, Component.empty()));
      nameField.setMaxLength(48);
      nameField.setValue(this.pass.getName());
      nameField.setResponder(this.pass::setName);
      this.field(x, y, fieldX + fieldW - x, "fantasticpass.gui.name", "fantasticpass.gui.tip_name");

      EditBox idField = this.addRenderableWidget(new EditBox(this.font, fieldX, y + 26, fieldW, 18, Component.empty()));
      idField.setMaxLength(48);
      idField.setValue(this.pass.getId());
      idField.setFilter(s -> s.matches("[a-zA-Z0-9_\\-]*"));
      idField.setResponder(this.pass::setId);
      this.field(x, y + 26, fieldX + fieldW - x, "fantasticpass.gui.id", "fantasticpass.gui.tip_id");

      EditBox tierCountField = this.addRenderableWidget(new EditBox(this.font, fieldX, y + 52, 70, 18, Component.empty()));
      tierCountField.setMaxLength(3);
      tierCountField.setFilter(s -> s.matches("\\d*"));
      tierCountField.setValue(String.valueOf(this.pass.getTierCount()));
      tierCountField.setResponder(this::onTierCountChanged);
      this.field(x, y + 52, fieldX + 70 - x, "fantasticpass.gui.tier_count", "fantasticpass.gui.tip_tier_count");

      EditBox minutesField = this.addRenderableWidget(new EditBox(this.font, fieldX, y + 78, 70, 18, Component.empty()));
      minutesField.setMaxLength(6);
      minutesField.setFilter(s -> s.matches("\\d*"));
      minutesField.setValue(String.valueOf(this.pass.getMinutesPerTierOverride()));
      minutesField.setResponder(this::onMinutesChanged);
      this.field(x, y + 78, fieldX + 70 - x, "fantasticpass.gui.minutes_per_tier", "fantasticpass.gui.tip_minutes");

      EditBox pointsField = this.addRenderableWidget(new EditBox(this.font, fieldX, y + 104, 70, 18, Component.empty()));
      pointsField.setMaxLength(6);
      pointsField.setFilter(s -> s.matches("\\d*"));
      pointsField.setValue(String.valueOf(this.pass.getPointsPerTierOverride()));
      pointsField.setResponder(v -> this.setInt(v, this.pass::setPointsPerTierOverride));
      this.field(x, y + 104, fieldX + 70 - x, "fantasticpass.gui.points_per_tier", "fantasticpass.gui.tip_points_per_tier");
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

   // ---- Quests tab ---------------------------------------------------------

   private void buildQuestsTab() {
      int x = this.bodyX();
      int y = this.bodyY();
      int fieldX = x + 150;
      int bx = x + 222;
      int bw = this.panelWidth - 24 - 222;

      // Section headers (sit between the divider and the first row).
      this.labels.add(new Label("\u00a76\u25b8 " + Component.translatable("fantasticpass.gui.q_counts_header").getString(), x, this.topPos + 47, 0xFFD24B));
      this.labels.add(new Label("\u00a76\u25b8 " + Component.translatable("fantasticpass.gui.q_editors_header").getString(), bx, this.topPos + 47, 0xFFD24B));

      // Left column: how many of each kind of quest appear.
      this.countField(fieldX, y, this.pass.getDailyFreeCount(), this.pass::setDailyFreeCount, 2, "fantasticpass.gui.daily_free_count", "fantasticpass.gui.tip_daily_free");
      this.countField(fieldX, y + 24, this.pass.getDailyPremiumCount(), this.pass::setDailyPremiumCount, 2, "fantasticpass.gui.daily_premium_count", "fantasticpass.gui.tip_daily_premium");
      this.countField(fieldX, y + 48, this.pass.getWeeklyFreeCount(), this.pass::setWeeklyFreeCount, 1, "fantasticpass.gui.weekly_free_count", "fantasticpass.gui.tip_weekly_free");
      this.countField(fieldX, y + 72, this.pass.getWeeklyPremiumCount(), this.pass::setWeeklyPremiumCount, 1, "fantasticpass.gui.weekly_premium_count", "fantasticpass.gui.tip_weekly_premium");
      this.countField(fieldX, y + 96, this.pass.getWeekCountOverride(), this.pass::setWeekCountOverride, 2, "fantasticpass.gui.week_count_field", "fantasticpass.gui.tip_week_count");

      // Right column: open the list editors.
      this.editButton(bx, y, bw, ChatFormatting.AQUA, "fantasticpass.gui.edit_daily_free", "fantasticpass.gui.tip_edit_daily_free",
         () -> this.openQuestList(Component.translatable("fantasticpass.gui.daily_free_count"), this.pass.getCustomDailyFree(), "df_c_"));
      this.editButton(bx, y + 22, bw, ChatFormatting.LIGHT_PURPLE, "fantasticpass.gui.edit_daily_premium", "fantasticpass.gui.tip_edit_daily_premium",
         () -> this.openQuestList(Component.translatable("fantasticpass.gui.daily_premium_count"), this.pass.getCustomDailyPremium(), "dp_c_"));

      this.questWeekField = this.addRenderableWidget(new EditBox(this.font, bx, y + 48, 34, 18, Component.empty()));
      this.questWeekField.setFilter(s -> s.matches("\\d*"));
      this.questWeekField.setValue("1");
      this.labels.add(new Label(Component.translatable("fantasticpass.gui.week_selector").getString(), bx + 40, y + 53, 0xE0E0E0));
      this.addHint(bx, y + 48, bw, 18, "fantasticpass.gui.week_selector", "fantasticpass.gui.tip_week_selector");

      this.editButton(bx, y + 70, bw, ChatFormatting.AQUA, "fantasticpass.gui.edit_week_free", "fantasticpass.gui.tip_edit_week_free", () -> this.openWeekList(false));
      this.editButton(bx, y + 92, bw, ChatFormatting.LIGHT_PURPLE, "fantasticpass.gui.edit_week_premium", "fantasticpass.gui.tip_edit_week_premium", () -> this.openWeekList(true));
   }

   /** A labelled numeric field + a tooltip that covers the whole row. */
   private void countField(int fieldX, int y, int value, IntConsumer setter, int maxLen, String labelKey, String tipKey) {
      int labelX = this.bodyX();
      EditBox box = this.addRenderableWidget(new EditBox(this.font, fieldX, y, 50, 18, Component.empty()));
      box.setMaxLength(maxLen);
      box.setFilter(s -> s.matches("\\d*"));
      box.setValue(String.valueOf(value));
      box.setResponder(v -> this.setInt(v, setter));
      this.labels.add(new Label(Component.translatable(labelKey).getString(), labelX, y + 5, 0xE0E0E0));
      this.addHint(labelX, y, fieldX + 50 - labelX, 18, labelKey, tipKey);
   }

   private void editButton(int bx, int y, int bw, ChatFormatting color, String labelKey, String tipKey, Runnable action) {
      this.addRenderableWidget(Button.builder(Component.translatable(labelKey).withStyle(color), b -> action.run()).bounds(bx, y, bw, 18).build());
      this.addHint(bx, y, bw, 18, labelKey, tipKey);
   }

   private void field(int x, int y, int w, String labelKey, String tipKey) {
      this.labels.add(new Label(Component.translatable(labelKey).getString(), x, y + 5, 0xE0E0E0));
      this.addHint(x, y, w, 18, labelKey, tipKey);
   }

   private int questWeek() {
      try {
         int w = this.questWeekField.getValue().isEmpty() ? 1 : Integer.parseInt(this.questWeekField.getValue());
         return Math.max(1, Math.min(PassDefinition.MAX_WEEKS, w));
      } catch (NumberFormatException e) {
         return 1;
      }
   }

   private void openWeekList(boolean premium) {
      int week = this.questWeek();
      List<com.fantasticpass.quest.Quest> list = premium ? this.pass.getCustomWeekPremium(week) : this.pass.getCustomWeekFree(week);
      Component title = Component.translatable(premium ? "fantasticpass.gui.edit_week_premium" : "fantasticpass.gui.edit_week_free")
         .append(" - ").append(Component.translatable("fantasticpass.gui.week", week));
      this.openQuestList(title, list, (premium ? "wp_c" : "wf_c") + week + "_");
   }

   private void openQuestList(Component title, List<com.fantasticpass.quest.Quest> list, String idPrefix) {
      Minecraft.getInstance().setScreen(new QuestListEditorScreen(this, title, list, idPrefix));
   }

   private void setInt(String value, IntConsumer setter) {
      try {
         setter.accept(value.isEmpty() ? 0 : Integer.parseInt(value));
      } catch (NumberFormatException ignored) {
      }
   }

   // ---- Tiers tab ----------------------------------------------------------

   private void buildTiersTab() {
      int x = this.bodyX();
      int y = this.bodyY();
      this.page = Math.min(this.page, this.tierPages() - 1);
      this.addRenderableWidget(Button.builder(Component.literal("\u25c0"), b -> this.changePage(-1)).bounds(x, y, 22, 18).build());
      this.addHint(x, y, 22, 18, "fantasticpass.gui.prev", "fantasticpass.gui.tip_tier_prev");
      this.addRenderableWidget(Button.builder(Component.literal("\u25b6"), b -> this.changePage(1)).bounds(x + 26, y, 22, 18).build());
      this.addHint(x + 26, y, 22, 18, "fantasticpass.gui.next", "fantasticpass.gui.tip_tier_next");

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
         int bxp = x + col * (colW + 8);
         int byp = gridY + row * 22;
         this.addRenderableWidget(
            Button.builder(Component.literal(Component.translatable("fantasticpass.gui.tier_info", tierNumber).getString() + marker), b -> this.openTier(tierNumber))
               .bounds(bxp, byp, colW, 20)
               .build()
         );
         this.addHint(bxp, byp, colW, 20, "fantasticpass.gui.tier_info", "fantasticpass.gui.tip_tier_edit", tierNumber);
      }
   }

   private void changePage(int delta) {
      this.page = Math.max(0, Math.min(this.tierPages() - 1, this.page + delta));
      this.rebuildWidgets();
   }

   private void openTier(int tierNumber) {
      Minecraft.getInstance().setScreen(new TierEditorScreen(this, this.pass.getTier(tierNumber)));
   }

   // ---- Music playlist tab -------------------------------------------------

   private void buildMusicTab() {
      int x = this.bodyX();
      int y = this.bodyY();
      int fullW = this.panelWidth - 24;
      int addW = 60;
      int urlW = fullW - addW - 4;

      this.musicUrlBox = this.addRenderableWidget(new EditBox(this.font, x, y, urlW, 18, Component.empty()));
      this.musicUrlBox.setMaxLength(512);
      this.musicUrlBox.setHint(Component.literal("https://... .mp3"));
      this.addHint(x, y, urlW, 18, "fantasticpass.gui.music", "fantasticpass.gui.tip_music_url");
      this.editButton(x + urlW + 4, y, addW, ChatFormatting.GREEN, "fantasticpass.gui.music_add", "fantasticpass.gui.tip_music_add", this::addMusicUrl);

      int listY = y + 26;
      int listH = this.topPos + this.panelHeight - 44 - listY;
      this.musicList = this.addRenderableWidget(new ScrollSelector<>(x, listY, fullW, listH, 16,
         this::musicLabel, s -> s, s -> new ItemStack(Items.MUSIC_DISC_CAT)));
      this.musicList.onSelect(this::removeMusicUrl);
      this.addHint(x, listY, fullW, listH, "fantasticpass.gui.music", "fantasticpass.gui.tip_music_list");
      this.refreshMusicList();
   }

   private String musicLabel(String url) {
      int i = this.pass.getMusicUrls().indexOf(url) + 1;
      String shown = url.length() > 54 ? url.substring(0, 53) + "\u2026" : url;
      return "\u00a7b" + i + ". \u00a7f" + shown;
   }

   private void refreshMusicList() {
      if (this.musicList != null) {
         this.musicList.setItems(new ArrayList<>(this.pass.getMusicUrls()));
         this.musicList.clearSelection();
      }
   }

   private void addMusicUrl() {
      if (this.musicUrlBox == null) {
         return;
      }
      String url = this.musicUrlBox.getValue().trim();
      if (url.isEmpty()) {
         return;
      }
      if (!isHttpUrl(url)) {
         this.flash("\u00a7c\u26a0 " + Component.translatable("fantasticpass.gui.music_invalid").getString(), 4000L);
         return;
      }
      this.pass.getMusicUrls().add(url);
      this.musicUrlBox.setValue("");
      this.flash("\u00a7a\u2714 " + Component.translatable("fantasticpass.gui.music_added").getString(), 2500L);
      this.refreshMusicList();
   }

   private void removeMusicUrl(String url) {
      this.pass.getMusicUrls().remove(url);
      this.refreshMusicList();
   }

   // ---- Background wallpapers tab -----------------------------------------

   private void buildBackgroundsTab() {
      int x = this.bodyX();
      int y = this.bodyY();
      int fullW = this.panelWidth - 24;
      int addW = 60;
      int urlW = fullW - addW - 4;

      this.bgUrlBox = this.addRenderableWidget(new EditBox(this.font, x, y, urlW, 18, Component.empty()));
      this.bgUrlBox.setMaxLength(512);
      this.bgUrlBox.setHint(Component.literal("https://... .png / .jpg"));
      this.addHint(x, y, urlW, 18, "fantasticpass.gui.backgrounds", "fantasticpass.gui.tip_bg_url");
      this.editButton(x + urlW + 4, y, addW, ChatFormatting.GREEN, "fantasticpass.gui.music_add", "fantasticpass.gui.tip_bg_add", this::addBackgroundUrl);

      this.labels.add(new Label(Component.translatable("fantasticpass.gui.bg_interval").getString(), x, y + 27, 0xE0E0E0));
      EditBox intervalBox = this.addRenderableWidget(new EditBox(this.font, x + 118, y + 22, 50, 18, Component.empty()));
      intervalBox.setMaxLength(4);
      intervalBox.setFilter(s -> s.matches("\\d*"));
      intervalBox.setValue(String.valueOf(this.pass.getBackgroundIntervalSeconds()));
      intervalBox.setResponder(v -> this.setInt(v, this.pass::setBackgroundIntervalSeconds));
      this.addHint(x, y + 22, 168, 18, "fantasticpass.gui.bg_interval", "fantasticpass.gui.tip_bg_interval");

      int listY = y + 46;
      int listH = this.topPos + this.panelHeight - 44 - listY;
      this.bgList = this.addRenderableWidget(new ScrollSelector<>(x, listY, fullW, listH, 16,
         this::bgLabel, s -> s, s -> new ItemStack(Items.PAINTING)));
      this.bgList.onSelect(this::removeBackgroundUrl);
      this.addHint(x, listY, fullW, listH, "fantasticpass.gui.backgrounds", "fantasticpass.gui.tip_bg_list");
      this.refreshBgList();
   }

   private String bgLabel(String url) {
      int i = this.pass.getBackgroundUrls().indexOf(url) + 1;
      String shown = url.length() > 54 ? url.substring(0, 53) + "\u2026" : url;
      return "\u00a7b" + i + ". \u00a7f" + shown;
   }

   private void refreshBgList() {
      if (this.bgList != null) {
         this.bgList.setItems(new ArrayList<>(this.pass.getBackgroundUrls()));
         this.bgList.clearSelection();
      }
   }

   private void addBackgroundUrl() {
      if (this.bgUrlBox == null) {
         return;
      }
      String url = this.bgUrlBox.getValue().trim();
      if (url.isEmpty()) {
         return;
      }
      if (!isHttpUrl(url)) {
         this.flash("\u00a7c\u26a0 " + Component.translatable("fantasticpass.gui.music_invalid").getString(), 4000L);
         return;
      }
      this.pass.getBackgroundUrls().add(url);
      this.bgUrlBox.setValue("");
      this.flash("\u00a7a\u2714 " + Component.translatable("fantasticpass.gui.bg_added").getString(), 2500L);
      this.refreshBgList();
   }

   private void removeBackgroundUrl(String url) {
      this.pass.getBackgroundUrls().remove(url);
      this.refreshBgList();
   }

   private static boolean isHttpUrl(String url) {
      try {
         String scheme = new URI(url).getScheme();
         return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
      } catch (Exception e) {
         return false;
      }
   }

   private void flash(String message, long ms) {
      this.msg = message;
      this.msgUntil = System.currentTimeMillis() + ms;
   }

   private void save() {
      if (this.pass.getId() == null || this.pass.getId().isEmpty()) {
         this.errorUntil = System.currentTimeMillis() + 5000L;
         if (this.tab != Tab.GENERAL) {
            this.switchTab(Tab.GENERAL);
         }
         return;
      }

      PacketHandler.sendToServer(new SavePassPacket(this.pass));
      this.onClose();
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(null);
   }

   // ---- Tooltip helpers ----------------------------------------------------

   private void addHint(int x, int y, int w, int h, String titleKey, String descKey, Object... titleArgs) {
      this.hints.add(new Hint(x, y, w, h,
         List.of(
            Component.translatable(titleKey, titleArgs).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
            Component.translatable(descKey).withStyle(ChatFormatting.GRAY))));
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(g);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xE0181A1F);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, 0xFF24262E);
      g.fill(this.leftPos, this.topPos + this.panelHeight - 1, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, 0xFF3A2E12);
      g.fill(this.leftPos + 6, this.topPos + 45, this.leftPos + this.panelWidth - 6, this.topPos + 46, 0xFF3A2E12);
      g.renderOutline(this.leftPos, this.topPos, this.panelWidth, this.panelHeight, 0xFF5A4A1E);
      g.drawString(this.font, "\u00a7d\u2726 \u00a7fFantastic Pass \u00a76Editor \u00a7d\u2726", this.leftPos + 10, this.topPos + 6, 0xFFFFFF, false);

      if (this.tab == Tab.TIERS) {
         g.drawCenteredString(this.font, "\u00a7e" + (this.page + 1) + "/" + this.tierPages(), this.bodyX() + 90, this.bodyY() + 5, 0xFFFFFF);
      }

      super.render(g, mouseX, mouseY, partialTick);

      for (Label l : this.labels) {
         g.drawString(this.font, l.text, l.x, l.y, l.color, false);
      }

      if (System.currentTimeMillis() < this.errorUntil) {
         String m = "\u00a7c\u26a0 " + Component.translatable("fantasticpass.msg.pass_id_required").getString();
         g.drawCenteredString(this.font, m, this.leftPos + this.panelWidth / 2, this.topPos + this.panelHeight - 42, 0xFFFF5555);
      }
      if (System.currentTimeMillis() < this.msgUntil) {
         g.drawCenteredString(this.font, this.msg, this.leftPos + this.panelWidth / 2, this.topPos + this.panelHeight - 40, 0xFFFFFFFF);
      }

      // Hover explanations for every element (drawn last, on top).
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

   private enum Tab {
      GENERAL("fantasticpass.gui.general", "fantasticpass.gui.tip_tab_general"),
      QUESTS("fantasticpass.gui.quests", "fantasticpass.gui.tip_tab_quests"),
      TIERS("fantasticpass.gui.tiers", "fantasticpass.gui.tip_tab_tiers"),
      MUSIC("fantasticpass.gui.music", "fantasticpass.gui.tip_tab_music"),
      FONDOS("fantasticpass.gui.backgrounds", "fantasticpass.gui.tip_tab_backgrounds");

      final String key;
      final String tipKey;

      Tab(String key, String tipKey) {
         this.key = key;
         this.tipKey = tipKey;
      }
   }

   private record Label(String text, int x, int y, int color) {
   }

   private record Hint(int x, int y, int w, int h, List<Component> lines) {
   }
}
