package com.fantasticpass.gui.admin;

import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.gui.GuiTheme;
import com.fantasticpass.network.PacketHandler;
import com.fantasticpass.network.SavePassPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Admin pass creator/editor with a dark cyan/gold theme. Two tabs: General (name, id,
 * minutes-per-tier override) and Tiers (paginated 10/page, each opening a
 * {@link TierEditorPanel}). Saving sends the full definition to the server.
 */
public class PassAdminScreen extends Screen {

    private enum Tab {
        GENERAL,
        TIERS
    }

    private static final int TIERS_PER_PAGE = 10;

    private final PassDefinition pass;
    private Tab tab = Tab.GENERAL;
    private int page;
    private int selectedTier;

    private TierEditorPanel tierPanel;
    private EditBox nameField;
    private EditBox idField;
    private EditBox minutesField;

    public PassAdminScreen(PassDefinition pass) {
        super(Component.translatable("fantasticpass.gui.admin.title"));
        this.pass = pass;
    }

    /** Public hook so sub-components (the tier panel) can request a widget rebuild. */
    public void refresh() {
        rebuildWidgets();
    }

    @Override
    protected void init() {
        // Tab + action buttons.
        addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.general"), b -> switchTab(Tab.GENERAL))
                .bounds(10, 26, 70, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.tiers"), b -> switchTab(Tab.TIERS))
                .bounds(84, 26, 70, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("fantasticpass.gui.save"), b -> save())
                .bounds(this.width - 184, 26, 84, 18).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width - 94, 26, 84, 18).build());

        if (tab == Tab.GENERAL) {
            buildGeneralTab();
        } else {
            buildTiersTab();
        }
    }

    private void switchTab(Tab newTab) {
        this.tab = newTab;
        rebuildWidgets();
    }

    private void buildGeneralTab() {
        nameField = addRenderableWidget(new EditBox(this.font, 20, 82, 220, 18,
                Component.translatable("fantasticpass.gui.name")));
        nameField.setMaxLength(48);
        nameField.setValue(pass.getName());
        nameField.setResponder(pass::setName);

        idField = addRenderableWidget(new EditBox(this.font, 20, 120, 220, 18,
                Component.translatable("fantasticpass.gui.id")));
        idField.setMaxLength(48);
        idField.setValue(pass.getId());
        idField.setFilter(s -> s.matches("[a-zA-Z0-9_\\-]*"));
        idField.setResponder(pass::setId);

        minutesField = addRenderableWidget(new EditBox(this.font, 20, 158, 80, 18,
                Component.translatable("fantasticpass.gui.minutes_per_tier")));
        minutesField.setMaxLength(6);
        minutesField.setFilter(s -> s.matches("\\d*"));
        minutesField.setValue(String.valueOf(pass.getMinutesPerTierOverride()));
        minutesField.setResponder(this::onMinutesChanged);
    }

    private void onMinutesChanged(String value) {
        try {
            pass.setMinutesPerTierOverride(value.isEmpty() ? 0 : Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            // Keep previous value on invalid input.
        }
    }

    private void buildTiersTab() {
        addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1))
                .bounds(10, 58, 30, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1))
                .bounds(84, 58, 30, 16).build());

        for (int i = 0; i < TIERS_PER_PAGE; i++) {
            int tierNumber = page * TIERS_PER_PAGE + i + 1;
            if (tierNumber > PassDefinition.TIER_COUNT) {
                break;
            }
            final int selected = tierNumber;
            addRenderableWidget(Button.builder(Component.literal("Tier " + tierNumber), b -> selectTier(selected))
                    .bounds(10, 80 + i * 18, 104, 16).build());
        }

        if (selectedTier >= 1 && selectedTier <= PassDefinition.TIER_COUNT) {
            tierPanel = new TierEditorPanel(pass.getTier(selectedTier), this);
            tierPanel.build(this::addRenderableWidget, this.font, 140, 70);
        } else {
            tierPanel = null;
        }
    }

    private void changePage(int delta) {
        int pages = PassDefinition.TIER_COUNT / TIERS_PER_PAGE;
        page = Math.max(0, Math.min(pages - 1, page + delta));
        rebuildWidgets();
    }

    private void selectTier(int tierNumber) {
        this.selectedTier = tierNumber;
        rebuildWidgets();
    }

    private void save() {
        if (pass.getId() == null || pass.getId().isEmpty()) {
            return;
        }
        PacketHandler.sendToServer(new SavePassPacket(pass));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.drawBackground(graphics, this.width, this.height);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFF00E5FF);

        // Active-tab underline.
        int tabX = tab == Tab.GENERAL ? 10 : 84;
        graphics.fill(tabX, 45, tabX + 70, 46, 0xFF000000 | GuiTheme.ACCENT_GOLD);

        if (tab == Tab.GENERAL) {
            graphics.drawString(this.font, Component.translatable("fantasticpass.gui.name"), 20, 72, 0xFFAAAAAA, false);
            graphics.drawString(this.font, Component.translatable("fantasticpass.gui.id"), 20, 110, 0xFFAAAAAA, false);
            graphics.drawString(this.font, Component.translatable("fantasticpass.gui.minutes_per_tier"), 20, 148, 0xFFAAAAAA, false);
            graphics.drawString(this.font, Component.literal("(0 = use global config)"), 110, 162, 0xFF777788, false);
        } else {
            int pages = PassDefinition.TIER_COUNT / TIERS_PER_PAGE;
            graphics.drawCenteredString(this.font, (page + 1) + "/" + pages, 62, 60, 0xFFFFFFFF);
            GuiTheme.drawPanel(graphics, 132, 60, this.width - 144, this.height - 96);
            if (tierPanel != null) {
                tierPanel.render(graphics, this.font);
            } else {
                graphics.drawString(this.font, Component.literal("Select a tier to edit"), 150, 80, 0xFFAAAAAA, false);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == Tab.TIERS && tierPanel != null && button == 0 && tierPanel.handleClick(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
