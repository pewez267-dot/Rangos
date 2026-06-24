package com.fantasticpass.gui.admin;

import com.fantasticpass.data.NametagStyle;
import com.fantasticpass.data.PassRankReward;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.gui.GuiTheme;
import com.fantasticpass.gui.RegistryItems;
import com.fantasticpass.gui.widgets.GradientToggleWidget;
import com.fantasticpass.gui.widgets.ScrollSelector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen reward editor for a single tier. Left: a searchable item picker (registry).
 * Middle: add-to-free/premium controls, command inputs, and the optional pass-rank reward.
 * Right: the current rewards list (click an entry to remove it). Edits the tier in place.
 */
public class TierEditorScreen extends Screen {

    private final Screen parent;
    private final TierDefinition tier;

    private EditBox searchBox;
    private EditBox countBox;
    private EditBox freeCmdBox;
    private EditBox premiumCmdBox;
    private EditBox rankIdBox;
    private ScrollSelector<Item> itemSelector;
    private ScrollSelector<RewardRow> rewardSelector;

    private enum Kind {
        FREE_ITEM,
        PREMIUM_ITEM,
        FREE_CMD,
        PREMIUM_CMD
    }

    /** A single row in the "current rewards" list. */
    private static final class RewardRow {
        final Kind kind;
        final int index;
        final String label;
        final ItemStack icon;

        RewardRow(Kind kind, int index, String label, ItemStack icon) {
            this.kind = kind;
            this.index = index;
            this.label = label;
            this.icon = icon;
        }
    }

    public TierEditorScreen(Screen parent, TierDefinition tier) {
        super(Component.literal("Tier " + (tier == null ? 0 : tier.getTierNumber()) + " \u2014 Rewards"));
        this.parent = parent;
        this.tier = tier;
    }

    @Override
    protected void init() {
        int listTop = 60;
        int listBottom = this.height - 16;
        int listHeight = Math.max(40, listBottom - listTop);

        // ----- Column 1: item picker -----
        searchBox = addRenderableWidget(new EditBox(this.font, 10, 40, 150, 16, Component.literal("search")));
        searchBox.setHint(Component.literal("Search items..."));
        searchBox.setResponder(s -> {
            if (itemSelector != null) {
                itemSelector.setQuery(s);
            }
        });

        itemSelector = addRenderableWidget(new ScrollSelector<>(10, listTop, 156, listHeight, 18,
                RegistryItems::name,
                it -> RegistryItems.name(it) + " " + RegistryItems.id(it),
                ItemStack::new));
        itemSelector.setItems(RegistryItems.all());

        // ----- Column 2: controls -----
        int cx = 176;
        countBox = addRenderableWidget(new EditBox(this.font, cx, 40, 50, 16, Component.literal("count")));
        countBox.setFilter(s -> s.matches("\\d*"));
        countBox.setValue("1");

        addRenderableWidget(Button.builder(Component.literal("Add \u2192 Free"), b -> addItem(false))
                .bounds(cx, 60, 140, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Add \u2192 Premium"), b -> addItem(true))
                .bounds(cx, 82, 140, 18).build());

        freeCmdBox = addRenderableWidget(new EditBox(this.font, cx, 116, 112, 16, Component.literal("free cmd")));
        freeCmdBox.setMaxLength(256);
        freeCmdBox.setHint(Component.literal("free command {player}"));
        addRenderableWidget(Button.builder(Component.literal("+"), b -> addCommand(false))
                .bounds(cx + 116, 116, 24, 16).build());

        premiumCmdBox = addRenderableWidget(new EditBox(this.font, cx, 140, 112, 16, Component.literal("premium cmd")));
        premiumCmdBox.setMaxLength(256);
        premiumCmdBox.setHint(Component.literal("premium command {player}"));
        addRenderableWidget(Button.builder(Component.literal("+"), b -> addCommand(true))
                .bounds(cx + 116, 140, 24, 16).build());

        // Rank reward.
        addRenderableWidget(new GradientToggleWidget(cx, 172, 140, 16,
                Component.literal("Pass Rank Reward"), tier.hasRankReward(), this::onRankToggle));
        if (tier.getRankReward() != null) {
            PassRankReward reward = tier.getRankReward();
            rankIdBox = addRenderableWidget(new EditBox(this.font, cx, 192, 140, 16, Component.literal("rank id")));
            rankIdBox.setMaxLength(48);
            rankIdBox.setValue(reward.getRankId());
            rankIdBox.setResponder(reward::setRankId);
            addRenderableWidget(Button.builder(Component.literal("Edit Style & Text"), b -> openColorEditor(reward))
                    .bounds(cx, 212, 140, 16).build());
        }

        // ----- Column 3: current rewards -----
        int rx = 326;
        int rWidth = Math.max(80, this.width - rx - 10);
        rewardSelector = addRenderableWidget(new ScrollSelector<>(rx, listTop, rWidth, listHeight, 18,
                r -> r.label, r -> r.label, r -> r.icon));
        rewardSelector.onSelect(this::removeReward);
        refreshRewards();

        // Done.
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width - 90, 8, 80, 18).build());
    }

    private int parseCount() {
        try {
            int c = countBox.getValue().isEmpty() ? 1 : Integer.parseInt(countBox.getValue());
            return Math.max(1, Math.min(64, c));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void addItem(boolean premium) {
        Item selected = itemSelector.getSelected();
        if (selected == null) {
            return;
        }
        ItemStack stack = new ItemStack(selected, parseCount());
        (premium ? tier.getPremiumRewards() : tier.getFreeRewards()).add(stack);
        refreshRewards();
    }

    private void addCommand(boolean premium) {
        EditBox box = premium ? premiumCmdBox : freeCmdBox;
        String value = box.getValue().trim();
        if (!value.isEmpty()) {
            (premium ? tier.getPremiumCommands() : tier.getFreeCommands()).add(value);
            box.setValue("");
            refreshRewards();
        }
    }

    private void removeReward(RewardRow row) {
        switch (row.kind) {
            case FREE_ITEM -> safeRemove(tier.getFreeRewards(), row.index);
            case PREMIUM_ITEM -> safeRemove(tier.getPremiumRewards(), row.index);
            case FREE_CMD -> safeRemove(tier.getFreeCommands(), row.index);
            case PREMIUM_CMD -> safeRemove(tier.getPremiumCommands(), row.index);
        }
        refreshRewards();
    }

    private static void safeRemove(List<?> list, int index) {
        if (index >= 0 && index < list.size()) {
            list.remove(index);
        }
    }

    private void refreshRewards() {
        List<RewardRow> rows = new ArrayList<>();
        List<ItemStack> free = tier.getFreeRewards();
        for (int i = 0; i < free.size(); i++) {
            ItemStack s = free.get(i);
            rows.add(new RewardRow(Kind.FREE_ITEM, i, "\u00a7f" + s.getCount() + "x " + s.getHoverName().getString(), s));
        }
        List<ItemStack> prem = tier.getPremiumRewards();
        for (int i = 0; i < prem.size(); i++) {
            ItemStack s = prem.get(i);
            rows.add(new RewardRow(Kind.PREMIUM_ITEM, i, "\u00a76[P] " + s.getCount() + "x " + s.getHoverName().getString(), s));
        }
        List<String> fc = tier.getFreeCommands();
        for (int i = 0; i < fc.size(); i++) {
            rows.add(new RewardRow(Kind.FREE_CMD, i, "\u00a7b/ " + fc.get(i), ItemStack.EMPTY));
        }
        List<String> pc = tier.getPremiumCommands();
        for (int i = 0; i < pc.size(); i++) {
            rows.add(new RewardRow(Kind.PREMIUM_CMD, i, "\u00a76[P] /" + pc.get(i), ItemStack.EMPTY));
        }
        rewardSelector.setItems(rows);
        rewardSelector.clearSelection();
    }

    private void onRankToggle(boolean on) {
        if (on) {
            if (tier.getRankReward() == null) {
                tier.setRankReward(new PassRankReward("", "", new NametagStyle()));
            }
        } else {
            tier.setRankReward(null);
        }
        rebuildWidgets();
    }

    private void openColorEditor(PassRankReward reward) {
        Minecraft.getInstance().setScreen(new ColorEditorScreen(this, reward.getStyle(),
                reward.getRankDisplayText(), (style, text) -> {
            reward.setStyle(style);
            reward.setRankDisplayText(text);
        }));
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.drawBackground(graphics, this.width, this.height);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFF00E5FF);

        graphics.drawString(this.font, Component.literal("All Items"), 10, 30, 0xFFAAAAAA, false);
        graphics.drawString(this.font, Component.literal("Count"), 176, 30, 0xFFAAAAAA, false);
        graphics.drawString(this.font, Component.literal("Current Rewards (click to remove)"), 326, 30, 0xFFAAAAAA, false);
        graphics.drawString(this.font, Component.literal("Commands use {player}"), 176, 104, 0xFF777788, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
