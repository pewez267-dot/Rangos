package com.fantasticpass.gui.admin;

import com.fantasticpass.data.NametagStyle;
import com.fantasticpass.data.PassRankReward;
import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.gui.GuiTheme;
import com.fantasticpass.gui.widgets.GradientToggleWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Right-hand editor for a single tier: free/premium item rewards (added from the held
 * item, removed by clicking), free/premium reward commands (with {@code {player}}), and
 * an optional pass-rank reward whose style/text is edited in {@link ColorEditorScreen}.
 */
public final class TierEditorPanel {

    private final TierDefinition tier;
    private final PassAdminScreen screen;

    private int x;
    private int y;

    private EditBox freeCommandInput;
    private EditBox premiumCommandInput;
    private EditBox rankIdInput;

    private static final int ICON = 18;
    private static final int LINE = 10;

    public TierEditorPanel(TierDefinition tier, PassAdminScreen screen) {
        this.tier = tier;
        this.screen = screen;
    }

    public void build(ColorEditorWidget.WidgetSink sink, Font font, int originX, int originY) {
        this.x = originX;
        this.y = originY;

        // Free reward command input + add.
        freeCommandInput = sink.accept(new EditBox(font, x, y + 46, 150, 16,
                Component.literal("free command")));
        freeCommandInput.setMaxLength(256);
        sink.accept(Button.builder(Component.literal("+"), b -> addCommand(tier.getFreeCommands(), freeCommandInput))
                .bounds(x + 154, y + 46, 20, 16).build());

        // Free reward item add (from held item).
        sink.accept(Button.builder(Component.literal("Add Held \u2192 Free"), b -> addHeldItem(tier.getFreeRewards()))
                .bounds(x, y + 24, 174, 16).build());

        // Premium reward command input + add.
        premiumCommandInput = sink.accept(new EditBox(font, x, y + 144, 150, 16,
                Component.literal("premium command")));
        premiumCommandInput.setMaxLength(256);
        sink.accept(Button.builder(Component.literal("+"), b -> addCommand(tier.getPremiumCommands(), premiumCommandInput))
                .bounds(x + 154, y + 144, 20, 16).build());

        sink.accept(Button.builder(Component.literal("Add Held \u2192 Premium"), b -> addHeldItem(tier.getPremiumRewards()))
                .bounds(x, y + 122, 174, 16).build());

        // Rank reward toggle.
        sink.accept(new GradientToggleWidget(x, y + 196, 174, 16,
                Component.translatable("fantasticpass.gui.rank_reward"), tier.hasRankReward(), this::onRankToggle));

        if (tier.getRankReward() != null) {
            PassRankReward reward = tier.getRankReward();
            rankIdInput = sink.accept(new EditBox(font, x, y + 216, 174, 16, Component.literal("rank id")));
            rankIdInput.setMaxLength(48);
            rankIdInput.setValue(reward.getRankId());
            rankIdInput.setResponder(reward::setRankId);

            sink.accept(Button.builder(Component.literal("Edit Style & Text"), b -> openColorEditor(reward))
                    .bounds(x, y + 236, 174, 16).build());
        }
    }

    private void onRankToggle(boolean on) {
        if (on) {
            if (tier.getRankReward() == null) {
                tier.setRankReward(new PassRankReward("", "", new NametagStyle()));
            }
        } else {
            tier.setRankReward(null);
        }
        screen.refresh();
    }

    private void openColorEditor(PassRankReward reward) {
        Minecraft.getInstance().setScreen(new ColorEditorScreen(screen, reward.getStyle(),
                reward.getRankDisplayText(), (style, text) -> {
            reward.setStyle(style);
            reward.setRankDisplayText(text);
        }));
    }

    private void addCommand(List<String> commands, EditBox input) {
        String value = input.getValue().trim();
        if (!value.isEmpty()) {
            commands.add(value);
            input.setValue("");
        }
    }

    private void addHeldItem(List<ItemStack> rewards) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        ItemStack held = Minecraft.getInstance().player.getMainHandItem();
        if (!held.isEmpty()) {
            rewards.add(held.copy());
        }
    }

    public void render(GuiGraphics graphics, Font font) {
        graphics.drawString(font, Component.literal("Tier " + tier.getTierNumber()), x, y, 0xFF00E5FF, false);

        graphics.drawString(font, Component.translatable("fantasticpass.gui.free_rewards"),
                x, y + 12, 0xFFC0C0C8, false);
        renderItemRow(graphics, font, tier.getFreeRewards(), x, y + 64);
        renderCommandList(graphics, font, tier.getFreeCommands(), x, y + 66);

        graphics.drawString(font, Component.translatable("fantasticpass.gui.premium_rewards"),
                x, y + 110, 0xFF000000 | GuiTheme.ACCENT_GOLD, false);
        renderItemRow(graphics, font, tier.getPremiumRewards(), x, y + 162);
        renderCommandList(graphics, font, tier.getPremiumCommands(), x, y + 164);

        graphics.drawString(font, Component.literal("(click an item or command to remove it)"),
                x, y + 256, 0xFF777788, false);
    }

    private void renderItemRow(GuiGraphics graphics, Font font, List<ItemStack> items, int rowX, int rowY) {
        for (int i = 0; i < items.size(); i++) {
            int ix = rowX + i * ICON;
            ItemStack stack = items.get(i);
            graphics.renderItem(stack, ix, rowY);
            graphics.renderItemDecorations(font, stack, ix, rowY);
        }
    }

    private void renderCommandList(GuiGraphics graphics, Font font, List<String> commands, int listX, int listY) {
        for (int i = 0; i < commands.size(); i++) {
            graphics.drawString(font, "> " + commands.get(i), listX, listY + i * LINE, 0xFFAAAAAA, false);
        }
    }

    /** Handles clicks for removing items/commands. Returns true if something was removed. */
    public boolean handleClick(double mouseX, double mouseY) {
        if (removeFromItemRow(tier.getFreeRewards(), mouseX, mouseY, x, y + 64)) {
            return true;
        }
        if (removeFromItemRow(tier.getPremiumRewards(), mouseX, mouseY, x, y + 162)) {
            return true;
        }
        if (removeFromCommandList(tier.getFreeCommands(), mouseX, mouseY, x, y + 66)) {
            return true;
        }
        return removeFromCommandList(tier.getPremiumCommands(), mouseX, mouseY, x, y + 164);
    }

    private boolean removeFromItemRow(List<ItemStack> items, double mouseX, double mouseY, int rowX, int rowY) {
        if (mouseY < rowY || mouseY > rowY + 16) {
            return false;
        }
        for (int i = 0; i < items.size(); i++) {
            int ix = rowX + i * ICON;
            if (mouseX >= ix && mouseX < ix + 16) {
                items.remove(i);
                return true;
            }
        }
        return false;
    }

    private boolean removeFromCommandList(List<String> commands, double mouseX, double mouseY, int listX, int listY) {
        for (int i = 0; i < commands.size(); i++) {
            int lineY = listY + i * LINE;
            if (mouseY >= lineY && mouseY < lineY + LINE && mouseX >= listX && mouseX < listX + 200) {
                commands.remove(i);
                return true;
            }
        }
        return false;
    }
}
