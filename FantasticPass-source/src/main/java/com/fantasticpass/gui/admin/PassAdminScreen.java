/*
 * Decompiled with CFR 0.152.
 */
package com.fantasticpass.gui.admin;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.gui.admin.QuestListEditorScreen;
import com.fantasticpass.gui.admin.TierEditorScreen;
import com.fantasticpass.gui.widgets.ScrollSelector;
import com.fantasticpass.network.PacketHandler;
import com.fantasticpass.network.SavePassPacket;
import com.fantasticpass.quest.Quest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public class PassAdminScreen
extends Screen {
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
    private final List<Label> labels = new ArrayList<Label>();
    private final List<Hint> hints = new ArrayList<Hint>();

    public PassAdminScreen(PassDefinition pass) {
        super((Component)Component.translatable((String)"fantasticpass.gui.admin.title"));
        this.pass = pass;
    }

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
            case GENERAL: {
                this.buildGeneralTab();
                break;
            }
            case QUESTS: {
                this.buildQuestsTab();
                break;
            }
            case MUSIC: {
                this.buildMusicTab();
                break;
            }
            case FONDOS: {
                this.buildBackgroundsTab();
                break;
            }
            default: {
                this.buildTiersTab();
            }
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
            String text = (active ? "\u00a7f" : "\u00a77") + Component.translatable((String)t.key).getString();
            Button b = (Button)this.addRenderableWidget(Button.builder((Component)Component.literal((String)text), btn -> this.switchTab(t)).bounds(x, y, tabW, 18).build());
            this.addHint(x, y, tabW, 18, t.key, t.tipKey, new Object[0]);
            x += tabW + gap;
        }
    }

    private void initFooter() {
        int y = this.topPos + this.panelHeight - 26;
        this.addRenderableWidget(Button.builder((Component)Component.translatable((String)"fantasticpass.gui.save").withStyle(ChatFormatting.GREEN), b -> this.save()).bounds(this.leftPos + this.panelWidth - 158, y, 150, 18).build());
        this.addHint(this.leftPos + this.panelWidth - 158, y, 150, 18, "fantasticpass.gui.save", "fantasticpass.gui.tip_save", new Object[0]);
        this.addRenderableWidget(Button.builder((Component)Component.translatable((String)"fantasticpass.gui.close"), b -> this.onClose()).bounds(this.leftPos + 12, y, 90, 18).build());
        this.addHint(this.leftPos + 12, y, 90, 18, "fantasticpass.gui.close", "fantasticpass.gui.tip_close", new Object[0]);
    }

    private void switchTab(Tab newTab) {
        this.tab = newTab;
        this.rebuildWidgets();
    }

    private void buildGeneralTab() {
        int x = this.bodyX();
        int y = this.bodyY();
        int fieldX = x + 150;
        int fieldW = this.panelWidth - 24 - 150;
        EditBox nameField = (EditBox)this.addRenderableWidget(new EditBox(this.font, fieldX, y, fieldW, 18, (Component)Component.empty()));
        nameField.setMaxLength(48);
        nameField.setValue(this.pass.getName());
        nameField.setResponder(this.pass::setName);
        this.field(x, y, fieldX + fieldW - x, "fantasticpass.gui.name", "fantasticpass.gui.tip_name");
        EditBox idField = (EditBox)this.addRenderableWidget(new EditBox(this.font, fieldX, y + 26, fieldW, 18, (Component)Component.empty()));
        idField.setMaxLength(48);
        idField.setValue(this.pass.getId());
        idField.setFilter(s -> s.matches("[a-zA-Z0-9_\\-]*"));
        idField.setResponder(this.pass::setId);
        this.field(x, y + 26, fieldX + fieldW - x, "fantasticpass.gui.id", "fantasticpass.gui.tip_id");
        EditBox tierCountField = (EditBox)this.addRenderableWidget(new EditBox(this.font, fieldX, y + 52, 70, 18, (Component)Component.empty()));
        tierCountField.setMaxLength(3);
        tierCountField.setFilter(s -> s.matches("\\d*"));
        tierCountField.setValue(String.valueOf(this.pass.getTierCount()));
        tierCountField.setResponder(this::onTierCountChanged);
        this.field(x, y + 52, fieldX + 70 - x, "fantasticpass.gui.tier_count", "fantasticpass.gui.tip_tier_count");
        EditBox minutesField = (EditBox)this.addRenderableWidget(new EditBox(this.font, fieldX, y + 78, 70, 18, (Component)Component.empty()));
        minutesField.setMaxLength(6);
        minutesField.setFilter(s -> s.matches("\\d*"));
        minutesField.setValue(String.valueOf(this.pass.getMinutesPerTierOverride()));
        minutesField.setResponder(this::onMinutesChanged);
        this.field(x, y + 78, fieldX + 70 - x, "fantasticpass.gui.minutes_per_tier", "fantasticpass.gui.tip_minutes");
        EditBox pointsField = (EditBox)this.addRenderableWidget(new EditBox(this.font, fieldX, y + 104, 70, 18, (Component)Component.empty()));
        pointsField.setMaxLength(6);
        pointsField.setFilter(s -> s.matches("\\d*"));
        pointsField.setValue(String.valueOf(this.pass.getPointsPerTierOverride()));
        pointsField.setResponder(v -> this.setInt((String)v, this.pass::setPointsPerTierOverride));
        this.field(x, y + 104, fieldX + 70 - x, "fantasticpass.gui.points_per_tier", "fantasticpass.gui.tip_points_per_tier");
    }

    private void onTierCountChanged(String value) {
        try {
            if (!value.isEmpty()) {
                this.pass.setTierCount(Integer.parseInt(value));
            }
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
    }

    private void onMinutesChanged(String value) {
        try {
            this.pass.setMinutesPerTierOverride(value.isEmpty() ? 0 : Integer.parseInt(value));
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
    }

    private int tierPages() {
        return Math.max(1, (this.pass.getTierCount() + 9) / 10);
    }

    private void buildQuestsTab() {
        int x = this.bodyX();
        int y = this.bodyY();
        int fieldX = x + 150;
        int bx = x + 222;
        int bw = this.panelWidth - 24 - 222;
        this.labels.add(new Label("\u00a76\u25b8 " + Component.translatable((String)"fantasticpass.gui.q_counts_header").getString(), x, this.topPos + 47, 16765515));
        this.labels.add(new Label("\u00a76\u25b8 " + Component.translatable((String)"fantasticpass.gui.q_editors_header").getString(), bx, this.topPos + 47, 16765515));
        this.countField(fieldX, y, this.pass.getDailyFreeCount(), this.pass::setDailyFreeCount, 2, "fantasticpass.gui.daily_free_count", "fantasticpass.gui.tip_daily_free");
        this.countField(fieldX, y + 24, this.pass.getDailyPremiumCount(), this.pass::setDailyPremiumCount, 2, "fantasticpass.gui.daily_premium_count", "fantasticpass.gui.tip_daily_premium");
        this.countField(fieldX, y + 48, this.pass.getWeeklyFreeCount(), this.pass::setWeeklyFreeCount, 1, "fantasticpass.gui.weekly_free_count", "fantasticpass.gui.tip_weekly_free");
        this.countField(fieldX, y + 72, this.pass.getWeeklyPremiumCount(), this.pass::setWeeklyPremiumCount, 1, "fantasticpass.gui.weekly_premium_count", "fantasticpass.gui.tip_weekly_premium");
        this.countField(fieldX, y + 96, this.pass.getWeekCountOverride(), this.pass::setWeekCountOverride, 2, "fantasticpass.gui.week_count_field", "fantasticpass.gui.tip_week_count");
        this.editButton(bx, y, bw, ChatFormatting.AQUA, "fantasticpass.gui.edit_daily_free", "fantasticpass.gui.tip_edit_daily_free", () -> this.openSeededQuestList((Component)Component.translatable((String)"fantasticpass.gui.daily_free_count"), this.pass.getCustomDailyFree(), com.fantasticpass.quest.DefaultQuests.DAILY_FREE_POOL, "df_c_"));
        this.editButton(bx, y + 22, bw, ChatFormatting.LIGHT_PURPLE, "fantasticpass.gui.edit_daily_premium", "fantasticpass.gui.tip_edit_daily_premium", () -> this.openSeededQuestList((Component)Component.translatable((String)"fantasticpass.gui.daily_premium_count"), this.pass.getCustomDailyPremium(), com.fantasticpass.quest.DefaultQuests.DAILY_PREMIUM_POOL, "dp_c_"));
        this.questWeekField = (EditBox)this.addRenderableWidget(new EditBox(this.font, bx, y + 48, 34, 18, (Component)Component.empty()));
        this.questWeekField.setFilter(s -> s.matches("\\d*"));
        this.questWeekField.setValue("1");
        this.labels.add(new Label(Component.translatable((String)"fantasticpass.gui.week_selector").getString(), bx + 40, y + 53, 0xE0E0E0));
        this.addHint(bx, y + 48, bw, 18, "fantasticpass.gui.week_selector", "fantasticpass.gui.tip_week_selector", new Object[0]);
        this.editButton(bx, y + 70, bw, ChatFormatting.AQUA, "fantasticpass.gui.edit_week_free", "fantasticpass.gui.tip_edit_week_free", () -> this.openWeekList(false));
        this.editButton(bx, y + 92, bw, ChatFormatting.LIGHT_PURPLE, "fantasticpass.gui.edit_week_premium", "fantasticpass.gui.tip_edit_week_premium", () -> this.openWeekList(true));
    }

    private void countField(int fieldX, int y, int value, IntConsumer setter, int maxLen, String labelKey, String tipKey) {
        int labelX = this.bodyX();
        EditBox box = (EditBox)this.addRenderableWidget(new EditBox(this.font, fieldX, y, 50, 18, (Component)Component.empty()));
        box.setMaxLength(maxLen);
        box.setFilter(s -> s.matches("\\d*"));
        box.setValue(String.valueOf(value));
        box.setResponder(v -> this.setInt((String)v, setter));
        this.labels.add(new Label(Component.translatable((String)labelKey).getString(), labelX, y + 5, 0xE0E0E0));
        this.addHint(labelX, y, fieldX + 50 - labelX, 18, labelKey, tipKey, new Object[0]);
    }

    private void editButton(int bx, int y, int bw, ChatFormatting color, String labelKey, String tipKey, Runnable action) {
        this.addRenderableWidget(Button.builder((Component)Component.translatable((String)labelKey).withStyle(color), b -> action.run()).bounds(bx, y, bw, 18).build());
        this.addHint(bx, y, bw, 18, labelKey, tipKey, new Object[0]);
    }

    private void field(int x, int y, int w, String labelKey, String tipKey) {
        this.labels.add(new Label(Component.translatable((String)labelKey).getString(), x, y + 5, 0xE0E0E0));
        this.addHint(x, y, w, 18, labelKey, tipKey, new Object[0]);
    }

    private int questWeek() {
        try {
            int w = this.questWeekField.getValue().isEmpty() ? 1 : Integer.parseInt(this.questWeekField.getValue());
            return Math.max(1, Math.min(52, w));
        }
        catch (NumberFormatException e) {
            return 1;
        }
    }

    private void openWeekList(boolean premium) {
        int week = this.questWeek();
        List<Quest> list = premium ? this.pass.getCustomWeekPremium(week) : this.pass.getCustomWeekFree(week);
        List<Quest> defaults = premium ? com.fantasticpass.quest.DefaultQuests.premiumWeekQuestsCyclic(week) : com.fantasticpass.quest.DefaultQuests.weekQuestsCyclic(week);
        MutableComponent title = Component.translatable((String)(premium ? "fantasticpass.gui.edit_week_premium" : "fantasticpass.gui.edit_week_free")).append(" - ").append((Component)Component.translatable((String)"fantasticpass.gui.week", (Object[])new Object[]{week}));
        this.openSeededQuestList((Component)title, list, defaults, (premium ? "wp_c" : "wf_c") + week + "_");
    }

    private void openSeededQuestList(Component title, List<Quest> list, List<Quest> defaults, String idPrefix) {
        if (list.isEmpty() && defaults != null && !defaults.isEmpty()) {
            list.addAll(defaults);
        }
        this.openQuestList(title, list, idPrefix);
    }

    private void openQuestList(Component title, List<Quest> list, String idPrefix) {
        Minecraft.getInstance().setScreen((Screen)new QuestListEditorScreen(this, title, list, idPrefix));
    }

    private void setInt(String value, IntConsumer setter) {
        try {
            setter.accept(value.isEmpty() ? 0 : Integer.parseInt(value));
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
    }

    private void buildTiersTab() {
        int tierNumber;
        int x = this.bodyX();
        int y = this.bodyY();
        this.page = Math.min(this.page, this.tierPages() - 1);
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u25c0"), b -> this.changePage(-1)).bounds(x, y, 22, 18).build());
        this.addHint(x, y, 22, 18, "fantasticpass.gui.prev", "fantasticpass.gui.tip_tier_prev", new Object[0]);
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u25b6"), b -> this.changePage(1)).bounds(x + 26, y, 22, 18).build());
        this.addHint(x + 26, y, 22, 18, "fantasticpass.gui.next", "fantasticpass.gui.tip_tier_next", new Object[0]);
        int gridY = y + 26;
        int colW = (this.panelWidth - 24 - 8) / 2;
        for (int i = 0; i < 10 && (tierNumber = this.page * 10 + i + 1) <= this.pass.getTierCount(); ++i) {
            int col = i / 5;
            int row = i % 5;
            int finalTier = tierNumber;
            TierDefinition def = this.pass.getTier(finalTier);
            String marker = def != null && !def.isEmpty() ? " \u00a7a\u2714" : " \u00a78\u2014";
            int bxp = x + col * (colW + 8);
            int byp = gridY + row * 22;
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)(Component.translatable((String)"fantasticpass.gui.tier_info", (Object[])new Object[]{finalTier}).getString() + marker)), b -> this.openTier(finalTier)).bounds(bxp, byp, colW, 20).build());
            this.addHint(bxp, byp, colW, 20, "fantasticpass.gui.tier_info", "fantasticpass.gui.tip_tier_edit", finalTier);
        }
    }

    private void changePage(int delta) {
        this.page = Math.max(0, Math.min(this.tierPages() - 1, this.page + delta));
        this.rebuildWidgets();
    }

    private void openTier(int tierNumber) {
        Minecraft.getInstance().setScreen((Screen)new TierEditorScreen(this, this.pass.getTier(tierNumber)));
    }

    private void buildMusicTab() {
        int x = this.bodyX();
        int y = this.bodyY();
        int fullW = this.panelWidth - 24;
        int addW = 60;
        int urlW = fullW - addW - 4;
        this.musicUrlBox = (EditBox)this.addRenderableWidget(new EditBox(this.font, x, y, urlW, 18, (Component)Component.empty()));
        this.musicUrlBox.setMaxLength(512);
        this.musicUrlBox.setHint((Component)Component.literal((String)"https://... .mp3"));
        this.addHint(x, y, urlW, 18, "fantasticpass.gui.music", "fantasticpass.gui.tip_music_url", new Object[0]);
        this.editButton(x + urlW + 4, y, addW, ChatFormatting.GREEN, "fantasticpass.gui.music_add", "fantasticpass.gui.tip_music_add", this::addMusicUrl);
        int listY = y + 26;
        int listH = this.topPos + this.panelHeight - 44 - listY;
        this.musicList = (ScrollSelector)this.addRenderableWidget(new ScrollSelector<String>(x, listY, fullW, listH, 16, this::musicLabel, s -> s, s -> new ItemStack((ItemLike)Items.MUSIC_DISC_CAT)));
        this.musicList.onSelect(this::removeMusicUrl);
        this.addHint(x, listY, fullW, listH, "fantasticpass.gui.music", "fantasticpass.gui.tip_music_list", new Object[0]);
        this.refreshMusicList();
    }

    private String musicLabel(String url) {
        int i = this.pass.getMusicUrls().indexOf(url) + 1;
        String shown = url.length() > 54 ? url.substring(0, 53) + "\u2026" : url;
        return "\u00a7b" + i + ". \u00a7f" + shown;
    }

    private void refreshMusicList() {
        if (this.musicList != null) {
            this.musicList.setItems(new ArrayList<String>(this.pass.getMusicUrls()));
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
        if (!PassAdminScreen.isHttpUrl(url)) {
            this.flash("\u00a7c\u26a0 " + Component.translatable((String)"fantasticpass.gui.music_invalid").getString(), 4000L);
            return;
        }
        this.pass.getMusicUrls().add(url);
        this.musicUrlBox.setValue("");
        this.flash("\u00a7a\u2714 " + Component.translatable((String)"fantasticpass.gui.music_added").getString(), 2500L);
        this.refreshMusicList();
    }

    private void removeMusicUrl(String url) {
        this.pass.getMusicUrls().remove(url);
        this.refreshMusicList();
    }

    private void buildBackgroundsTab() {
        int x = this.bodyX();
        int y = this.bodyY();
        int fullW = this.panelWidth - 24;
        int addW = 60;
        int urlW = fullW - addW - 4;
        this.bgUrlBox = (EditBox)this.addRenderableWidget(new EditBox(this.font, x, y, urlW, 18, (Component)Component.empty()));
        this.bgUrlBox.setMaxLength(512);
        this.bgUrlBox.setHint((Component)Component.literal((String)"https://... .png / .jpg"));
        this.addHint(x, y, urlW, 18, "fantasticpass.gui.backgrounds", "fantasticpass.gui.tip_bg_url", new Object[0]);
        this.editButton(x + urlW + 4, y, addW, ChatFormatting.GREEN, "fantasticpass.gui.music_add", "fantasticpass.gui.tip_bg_add", this::addBackgroundUrl);
        this.labels.add(new Label(Component.translatable((String)"fantasticpass.gui.bg_interval").getString(), x, y + 27, 0xE0E0E0));
        EditBox intervalBox = (EditBox)this.addRenderableWidget(new EditBox(this.font, x + 118, y + 22, 50, 18, (Component)Component.empty()));
        intervalBox.setMaxLength(4);
        intervalBox.setFilter(s -> s.matches("\\d*"));
        intervalBox.setValue(String.valueOf(this.pass.getBackgroundIntervalSeconds()));
        intervalBox.setResponder(v -> this.setInt((String)v, this.pass::setBackgroundIntervalSeconds));
        this.addHint(x, y + 22, 168, 18, "fantasticpass.gui.bg_interval", "fantasticpass.gui.tip_bg_interval", new Object[0]);
        int listY = y + 46;
        int listH = this.topPos + this.panelHeight - 44 - listY;
        this.bgList = (ScrollSelector)this.addRenderableWidget(new ScrollSelector<String>(x, listY, fullW, listH, 16, this::bgLabel, s -> s, s -> new ItemStack((ItemLike)Items.PAINTING)));
        this.bgList.onSelect(this::removeBackgroundUrl);
        this.addHint(x, listY, fullW, listH, "fantasticpass.gui.backgrounds", "fantasticpass.gui.tip_bg_list", new Object[0]);
        this.refreshBgList();
    }

    private String bgLabel(String url) {
        int i = this.pass.getBackgroundUrls().indexOf(url) + 1;
        String shown = url.length() > 54 ? url.substring(0, 53) + "\u2026" : url;
        return "\u00a7b" + i + ". \u00a7f" + shown;
    }

    private void refreshBgList() {
        if (this.bgList != null) {
            this.bgList.setItems(new ArrayList<String>(this.pass.getBackgroundUrls()));
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
        if (!PassAdminScreen.isHttpUrl(url)) {
            this.flash("\u00a7c\u26a0 " + Component.translatable((String)"fantasticpass.gui.music_invalid").getString(), 4000L);
            return;
        }
        this.pass.getBackgroundUrls().add(url);
        this.bgUrlBox.setValue("");
        this.flash("\u00a7a\u2714 " + Component.translatable((String)"fantasticpass.gui.bg_added").getString(), 2500L);
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
        }
        catch (Exception e) {
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

    public void onClose() {
        this.minecraft.setScreen(null);
    }

    private void addHint(int x, int y, int w, int h, String titleKey, String descKey, Object ... titleArgs) {
        this.hints.add(new Hint(x, y, w, h, List.of(Component.translatable((String)titleKey, (Object[])titleArgs).withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}), Component.translatable((String)descKey).withStyle(ChatFormatting.GRAY))));
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -535291361);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, -14408146);
        g.fill(this.leftPos, this.topPos + this.panelHeight - 1, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -12964334);
        g.fill(this.leftPos + 6, this.topPos + 45, this.leftPos + this.panelWidth - 6, this.topPos + 46, -12964334);
        g.renderOutline(this.leftPos, this.topPos, this.panelWidth, this.panelHeight, -10860002);
        g.drawString(this.font, "\u00a7d\u2726 \u00a7fFantastic Pass \u00a76Editor \u00a7d\u2726", this.leftPos + 10, this.topPos + 6, 0xFFFFFF, false);
        if (this.tab == Tab.TIERS) {
            g.drawCenteredString(this.font, "\u00a7e" + (this.page + 1) + "/" + this.tierPages(), this.bodyX() + 90, this.bodyY() + 5, 0xFFFFFF);
        }
        super.render(g, mouseX, mouseY, partialTick);
        for (Label l : this.labels) {
            g.drawString(this.font, l.text, l.x, l.y, l.color, false);
        }
        if (System.currentTimeMillis() < this.errorUntil) {
            String m = "\u00a7c\u26a0 " + Component.translatable((String)"fantasticpass.msg.pass_id_required").getString();
            g.drawCenteredString(this.font, m, this.leftPos + this.panelWidth / 2, this.topPos + this.panelHeight - 42, -43691);
        }
        if (System.currentTimeMillis() < this.msgUntil) {
            g.drawCenteredString(this.font, this.msg, this.leftPos + this.panelWidth / 2, this.topPos + this.panelHeight - 40, -1);
        }
        List<Component> tip = null;
        for (Hint hh : this.hints) {
            if (mouseX < hh.x() || mouseX >= hh.x() + hh.w() || mouseY < hh.y() || mouseY >= hh.y() + hh.h()) continue;
            tip = hh.lines();
        }
        if (tip != null) {
            g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
        }
    }

    public boolean isPauseScreen() {
        return false;
    }

    private static enum Tab {
        GENERAL("fantasticpass.gui.general", "fantasticpass.gui.tip_tab_general"),
        QUESTS("fantasticpass.gui.quests", "fantasticpass.gui.tip_tab_quests"),
        TIERS("fantasticpass.gui.tiers", "fantasticpass.gui.tip_tab_tiers"),
        MUSIC("fantasticpass.gui.music", "fantasticpass.gui.tip_tab_music"),
        FONDOS("fantasticpass.gui.backgrounds", "fantasticpass.gui.tip_tab_backgrounds");

        final String key;
        final String tipKey;

        private Tab(String key, String tipKey) {
            this.key = key;
            this.tipKey = tipKey;
        }
    }

    private record Label(String text, int x, int y, int color) {
    }

    private record Hint(int x, int y, int w, int h, List<Component> lines) {
    }
}

