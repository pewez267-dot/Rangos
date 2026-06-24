package com.fantasticranks.gui.admin;

import com.fantasticranks.data.RankDefinition;
import com.fantasticranks.gui.GuiTheme;
import com.fantasticranks.nametag.NametagBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Right-hand editor for a single rank: its display name, the required hours, and its full
 * visual style/text (edited in {@link ColorEditorScreen}). Also shows a small live preview.
 */
public final class RankEditorPanel {

    private final RankDefinition rank;
    private final RanksAdminScreen screen;

    private int x;
    private int y;

    private EditBox nameField;
    private EditBox hoursField;

    public RankEditorPanel(RankDefinition rank, RanksAdminScreen screen) {
        this.rank = rank;
        this.screen = screen;
    }

    public void build(ColorEditorWidget.WidgetSink sink, Font font, int originX, int originY) {
        this.x = originX;
        this.y = originY;

        nameField = sink.accept(new EditBox(font, x, y + 24, 200, 18,
                Component.translatable("fantasticranks.gui.rank_name")));
        nameField.setMaxLength(48);
        nameField.setValue(rank.getRankName());
        nameField.setResponder(rank::setRankName);

        hoursField = sink.accept(new EditBox(font, x, y + 62, 100, 18,
                Component.translatable("fantasticranks.gui.hours")));
        hoursField.setMaxLength(10);
        hoursField.setFilter(s -> s.matches("\\d*\\.?\\d*"));
        hoursField.setValue(trimHours(rank.getHoursRequired()));
        hoursField.setResponder(this::onHoursChanged);

        sink.accept(Button.builder(Component.translatable("fantasticranks.gui.edit_style"), b -> openColorEditor())
                .bounds(x, y + 90, 200, 18).build());
    }

    private void onHoursChanged(String value) {
        try {
            rank.setHoursRequired(value.isEmpty() || value.equals(".") ? 0.0D : Double.parseDouble(value));
        } catch (NumberFormatException ignored) {
            // Keep previous value on invalid input.
        }
    }

    private void openColorEditor() {
        Minecraft.getInstance().setScreen(new ColorEditorScreen(screen, rank.getStyle(),
                rank.getRankName(), rank.getRankNumber(), (style, text) -> {
            rank.setStyle(style);
            rank.setRankName(text);
        }));
    }

    public void render(GuiGraphics graphics, Font font) {
        graphics.drawString(font, Component.literal("Editing rank #" + rank.getRankNumber()),
                x, y, 0xFF00E5FF, false);

        graphics.drawString(font, Component.translatable("fantasticranks.gui.rank_name"),
                x, y + 14, 0xFFAAAAAA, false);
        graphics.drawString(font, Component.translatable("fantasticranks.gui.hours"),
                x, y + 52, 0xFFAAAAAA, false);

        // Live preview line.
        graphics.drawString(font, Component.translatable("fantasticranks.gui.preview"),
                x, y + 116, 0xFFAAAAAA, false);
        graphics.drawString(font,
                Component.literal("Lvl " + rank.getRankNumber() + " ")
                        .append(NametagBuilder.buildStyledText(rank.getRankName(), rank.getStyle())),
                x, y + 128, 0xFFFFFFFF, false);
    }

    private static String trimHours(double hours) {
        if (hours == Math.floor(hours)) {
            return String.valueOf((long) hours);
        }
        return String.valueOf(hours);
    }
}
