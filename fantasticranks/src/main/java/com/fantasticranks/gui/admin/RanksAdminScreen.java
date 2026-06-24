package com.fantasticranks.gui.admin;

import com.fantasticranks.data.NametagStyle;
import com.fantasticranks.data.RankDefinition;
import com.fantasticranks.data.RanksPackage;
import com.fantasticranks.gui.GuiTheme;
import com.fantasticranks.network.PacketHandler;
import com.fantasticranks.network.SavePackagePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Admin package creator/editor with a dark cyan/gold theme. Two tabs: General (name, id)
 * and Ranks (a scrollable, reorderable list with add/delete and a per-rank
 * {@link RankEditorPanel} including the full color editor).
 */
public class RanksAdminScreen extends Screen {

    private enum Tab {
        GENERAL,
        RANKS
    }

    private static final int LIST_X = 10;
    private static final int LIST_Y = 80;
    private static final int LIST_WIDTH = 168;
    private static final int ROW_HEIGHT = 14;

    private final RanksPackage pkg;
    private Tab tab = Tab.GENERAL;
    private int selectedIndex = -1;
    private int scrollOffset;

    private RankEditorPanel rankPanel;
    private EditBox nameField;
    private EditBox idField;

    public RanksAdminScreen(RanksPackage pkg) {
        super(Component.translatable("fantasticranks.gui.admin.title"));
        this.pkg = pkg;
    }

    public void refresh() {
        rebuildWidgets();
    }

    private int listBottom() {
        return this.height - 36;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - LIST_Y) / ROW_HEIGHT);
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("fantasticranks.gui.general"), b -> switchTab(Tab.GENERAL))
                .bounds(10, 26, 70, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("fantasticranks.gui.ranks"), b -> switchTab(Tab.RANKS))
                .bounds(84, 26, 70, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("fantasticranks.gui.save"), b -> save())
                .bounds(this.width - 184, 26, 84, 18).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width - 94, 26, 84, 18).build());

        if (tab == Tab.GENERAL) {
            buildGeneralTab();
        } else {
            buildRanksTab();
        }
    }

    private void switchTab(Tab newTab) {
        this.tab = newTab;
        rebuildWidgets();
    }

    private void buildGeneralTab() {
        nameField = addRenderableWidget(new EditBox(this.font, 20, 82, 220, 18,
                Component.translatable("fantasticranks.gui.name")));
        nameField.setMaxLength(48);
        nameField.setValue(pkg.getName());
        nameField.setResponder(pkg::setName);

        idField = addRenderableWidget(new EditBox(this.font, 20, 120, 220, 18,
                Component.translatable("fantasticranks.gui.id")));
        idField.setMaxLength(48);
        idField.setValue(pkg.getId());
        idField.setFilter(s -> s.matches("[a-zA-Z0-9_\\-]*"));
        idField.setResponder(pkg::setId);
    }

    private void buildRanksTab() {
        addRenderableWidget(Button.builder(Component.translatable("fantasticranks.gui.add_rank"), b -> addRank())
                .bounds(LIST_X, 56, 50, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("fantasticranks.gui.delete_rank"), b -> deleteRank())
                .bounds(LIST_X + 54, 56, 50, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("fantasticranks.gui.move_up"), b -> moveUp())
                .bounds(LIST_X + 108, 56, 28, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("fantasticranks.gui.move_down"), b -> moveDown())
                .bounds(LIST_X + 138, 56, 30, 18).build());

        clampSelection();
        if (selectedIndex >= 0 && selectedIndex < pkg.size()) {
            rankPanel = new RankEditorPanel(pkg.get(selectedIndex), this);
            rankPanel.build(this::addRenderableWidget, this.font, 192, 58);
        } else {
            rankPanel = null;
        }
    }

    private void addRank() {
        RankDefinition last = pkg.size() > 0 ? pkg.get(pkg.size() - 1) : null;
        double hours = last != null ? last.getHoursRequired() + 10.0D : 0.0D;
        pkg.addRank(new RankDefinition(pkg.size() + 1, "New Rank", hours, new NametagStyle()));
        selectedIndex = pkg.size() - 1;
        ensureVisible();
        rebuildWidgets();
    }

    private void deleteRank() {
        if (selectedIndex >= 0 && selectedIndex < pkg.size()) {
            pkg.removeRank(selectedIndex);
            selectedIndex = Math.min(selectedIndex, pkg.size() - 1);
            rebuildWidgets();
        }
    }

    private void moveUp() {
        if (selectedIndex > 0) {
            selectedIndex = pkg.moveUp(selectedIndex);
            ensureVisible();
            rebuildWidgets();
        }
    }

    private void moveDown() {
        if (selectedIndex >= 0 && selectedIndex < pkg.size() - 1) {
            selectedIndex = pkg.moveDown(selectedIndex);
            ensureVisible();
            rebuildWidgets();
        }
    }

    private void clampSelection() {
        if (pkg.size() == 0) {
            selectedIndex = -1;
        } else if (selectedIndex >= pkg.size()) {
            selectedIndex = pkg.size() - 1;
        }
        int maxScroll = Math.max(0, pkg.size() - visibleRows());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
    }

    private void ensureVisible() {
        if (selectedIndex < 0) {
            return;
        }
        int rows = visibleRows();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + rows) {
            scrollOffset = selectedIndex - rows + 1;
        }
    }

    private void save() {
        if (pkg.getId() == null || pkg.getId().isEmpty()) {
            return;
        }
        pkg.renumber();
        PacketHandler.sendToServer(new SavePackagePacket(pkg));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.drawBackground(graphics, this.width, this.height);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFF00E5FF);

        int tabX = tab == Tab.GENERAL ? 10 : 84;
        graphics.fill(tabX, 45, tabX + 70, 46, 0xFF000000 | GuiTheme.ACCENT_GOLD);

        if (tab == Tab.GENERAL) {
            graphics.drawString(this.font, Component.translatable("fantasticranks.gui.name"), 20, 72, 0xFFAAAAAA, false);
            graphics.drawString(this.font, Component.translatable("fantasticranks.gui.id"), 20, 110, 0xFFAAAAAA, false);
        } else {
            renderRankList(graphics);
            GuiTheme.drawPanel(graphics, 186, 54, this.width - 198, this.height - 90);
            if (rankPanel != null) {
                rankPanel.render(graphics, this.font);
            } else {
                graphics.drawString(this.font, Component.literal("Select or add a rank"), 200, 70, 0xFFAAAAAA, false);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderRankList(GuiGraphics graphics) {
        GuiTheme.drawPanel(graphics, LIST_X, LIST_Y, LIST_WIDTH, listBottom() - LIST_Y);
        int rows = visibleRows();
        for (int row = 0; row < rows; row++) {
            int index = scrollOffset + row;
            if (index >= pkg.size()) {
                break;
            }
            RankDefinition rank = pkg.get(index);
            int rowY = LIST_Y + row * ROW_HEIGHT;
            if (index == selectedIndex) {
                graphics.fill(LIST_X + 1, rowY, LIST_X + LIST_WIDTH - 1, rowY + ROW_HEIGHT, 0xFF1E2A30);
                graphics.renderOutline(LIST_X + 1, rowY, LIST_WIDTH - 2, ROW_HEIGHT, 0xFF000000 | GuiTheme.ACCENT_CYAN);
            }
            int color = 0xFF000000 | rank.getStyle().getColor();
            String label = (index + 1) + ". " + rank.getRankName() + "  (" + trimHours(rank.getHoursRequired()) + "h)";
            graphics.drawString(this.font, label, LIST_X + 4, rowY + 3, color, false);
        }

        if (pkg.size() > rows) {
            graphics.drawString(this.font, "\u25B2\u25BC", LIST_X + LIST_WIDTH - 16, LIST_Y - 10, 0xFF888888, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == Tab.RANKS && button == 0
                && mouseX >= LIST_X && mouseX <= LIST_X + LIST_WIDTH
                && mouseY >= LIST_Y && mouseY < listBottom()) {
            int row = (int) ((mouseY - LIST_Y) / ROW_HEIGHT);
            int index = scrollOffset + row;
            if (index >= 0 && index < pkg.size()) {
                selectedIndex = index;
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tab == Tab.RANKS
                && mouseX >= LIST_X && mouseX <= LIST_X + LIST_WIDTH
                && mouseY >= LIST_Y && mouseY < listBottom()) {
            int maxScroll = Math.max(0, pkg.size() - visibleRows());
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String trimHours(double hours) {
        if (hours == Math.floor(hours)) {
            return String.valueOf((long) hours);
        }
        return String.valueOf(hours);
    }
}
