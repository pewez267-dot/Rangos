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

/**
 * Admin pass creator/editor with a dark cyan/gold theme. Two tabs: General (name, id,
 * minutes-per-tier override) and Tiers (paginated 10/page; clicking a tier opens the
 * full-screen {@link TierEditorScreen}). Saving sends the full definition to the server.
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

    private EditBox nameField;
    private EditBox idField;
    private EditBox minutesField;

    public PassAdminScreen(PassDefinition pass) {
        super(Component.translatable("fantasticpass.gui.admin.title"));
        this.pass = pass;
    }

    @Override
    protected void init() {
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
        nameField = addRenderableWidget(new EditBox(this.font, 20, 84, 220, 18, Component.translatable("fantasticpass.gui.name")));
        nameField.setMaxLength(48);
        nameField.setValue(pass.getName());
        nameField.setResponder(pass::setName);

        idField = addRenderableWidget(new EditBox(this.font, 20, 124, 220, 18, Component.translatable("fantasticpass.gui.id")));
        idField.setMaxLength(48);
        idField.setValue(pass.getId());
        idField.setFilter(s -> s.matches("[a-zA-Z0-9_\\-]*"));
        idField.setResponder(pass::setId);

        minutesField = addRenderableWidget(new EditBox(this.font, 20, 164, 80, 18, Component.translatable("fantasticpass.gui.minutes_per_tier")));
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
        int pages = PassDefinition.TIER_COUNT / TIERS_PER_PAGE;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1))
                .bounds(20, 58, 30, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1))
                .bounds(94, 58, 30, 16).build());

        for (int i = 0; i < TIERS_PER_PAGE; i++) {
            int tierNumber = page * TIERS_PER_PAGE + i + 1;
            if (tierNumber > PassDefinition.TIER_COUNT) {
                break;
            }
            int col = i / 5;
            int row = i % 5;
            TierDefinition def = pass.getTier(tierNumber);
            String marker = def != null && !def.isEmpty() ? " \u00a7a\u2714" : "";
            addRenderableWidget(Button.builder(Component.literal("Tier " + tierNumber + marker), b -> openTier(tierNumber))
                    .bounds(20 + col * 150, 82 + row * 22, 140, 20).build());
        }
    }

    private void changePage(int delta) {
        int pages = PassDefinition.TIER_COUNT / TIERS_PER_PAGE;
        page = Math.max(0, Math.min(pages - 1, page + delta));
        rebuildWidgets();
    }

    private void openTier(int tierNumber) {
        Minecraft.getInstance().setScreen(new TierEditorScreen(this, pass.getTier(tierNumber)));
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

        int tabX = tab == Tab.GENERAL ? 10 : 84;
        graphics.fill(tabX, 45, tabX + 70, 46, 0xFF000000 | GuiTheme.ACCENT_GOLD);

        if (tab == Tab.GENERAL) {
            graphics.drawString(this.font, Component.translatable("fantasticpass.gui.name"), 20, 74, 0xFFAAAAAA, false);
            graphics.drawString(this.font, Component.translatable("fantasticpass.gui.id"), 20, 114, 0xFFAAAAAA, false);
            graphics.drawString(this.font, Component.translatable("fantasticpass.gui.minutes_per_tier"), 20, 154, 0xFFAAAAAA, false);
            graphics.drawString(this.font, Component.literal("(0 = use global config)"), 110, 168, 0xFF777788, false);
        } else {
            int pages = PassDefinition.TIER_COUNT / TIERS_PER_PAGE;
            graphics.drawCenteredString(this.font, "Page " + (page + 1) + "/" + pages, 72, 60, 0xFFFFFFFF);
            graphics.drawString(this.font, Component.literal("Click a tier to edit its rewards. \u00a7a\u2714\u00a77 = has rewards"),
                    20, this.height - 20, 0xFF777788, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
