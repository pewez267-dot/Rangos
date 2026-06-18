package com.fantasticchest.gui.admin;

import com.fantasticchest.network.CreateChestPacket;
import com.fantasticchest.network.EditChestPacket;
import com.fantasticchest.network.PacketHandler;
import com.fantasticchest.network.RefreshStockPacket;
import com.fantasticchest.network.UpdatePermissionsPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin GUI (Interface 1), a single {@link AbstractContainerScreen} with tabs, in the
 * Fantastic family style. Creation mode shows Items / General / Seguridad; edit mode shows
 * Items / Refrescar Stock / Seguridad. All actions are sent to the server, which validates
 * (OP, id uniqueness, distance) and executes.
 */
public final class ChestAdminScreen extends AbstractContainerScreen<ChestAdminMenu> {

    public enum Tab {
        ITEMS("Items"), GENERAL("General"), SECURITY("Seguridad"), REFRESH("Refrescar Stock");

        public final String label;

        Tab(final String label) {
            this.label = label;
        }
    }

    // Immutable context.
    public final boolean editMode;
    public final BlockPos pos;
    public final List<String> existingIds;

    // Editable draft state shared with the tab classes.
    public String draftId;
    public String draftName;
    public boolean doBulk = false;
    public long bulkValue = 0L;
    public long draftItemQty = 0L;     // persists the "cantidad" field across refreshes
    public String draftItemSearch = ""; // persists the search field across refreshes
    public final LinkedHashMap<Item, Long> overrides = new LinkedHashMap<>();
    public final List<String> permitted = new ArrayList<>();
    public Item selectedItem = null;

    private Tab activeTab = Tab.ITEMS;
    private final List<EditBox> editBoxes = new ArrayList<>();

    private final ItemsTab itemsTab = new ItemsTab();
    private final GeneralTab generalTab = new GeneralTab();
    private final SecurityTab securityTab = new SecurityTab();

    public ChestAdminScreen(final ChestAdminMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = 540;
        this.imageHeight = 320;
        this.editMode = menu.isEditMode();
        this.pos = menu.getChestPos();
        this.draftId = menu.getChestId();
        this.draftName = menu.getChestName();
        this.existingIds = new ArrayList<>(menu.getExistingIds());
        this.permitted.addAll(menu.getPermitted());
    }

    private Tab[] tabsForMode() {
        return this.editMode
                ? new Tab[]{Tab.ITEMS, Tab.REFRESH, Tab.SECURITY}
                : new Tab[]{Tab.ITEMS, Tab.GENERAL, Tab.SECURITY};
    }

    @Override
    protected void init() {
        this.imageWidth = Math.min(this.width - 16, 540);
        this.imageHeight = Math.min(this.height - 16, 320);
        super.init();
        this.editBoxes.clear();

        final Tab[] tabs = tabsForMode();
        final int x = this.leftPos + 8;
        final int w = this.imageWidth - 16;
        final int gap = 2;
        final int tabW = (w - gap * (tabs.length - 1)) / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            final Tab tab = tabs[i];
            final String text = (tab == this.activeTab ? "§f§l" : "§7") + tab.label;
            addRenderableWidget(Button.builder(Component.literal(text), b -> {
                this.activeTab = tab;
                rebuildWidgets();
            }).bounds(x + i * (tabW + gap), this.topPos + 22, tabW, 16).build());
        }

        switch (this.activeTab) {
            case ITEMS -> this.itemsTab.build(this);
            case GENERAL -> this.generalTab.build(this);
            case SECURITY -> this.securityTab.build(this);
            case REFRESH -> buildRefreshTab();
        }

        final int by = this.topPos + this.imageHeight - 24;
        if (!this.editMode) {
            addRenderableWidget(Button.builder(Component.literal("§aCrear cofre"), b -> sendCreate())
                    .bounds(x + w - 150, by, 90, 18).build());
        } else if (this.activeTab == Tab.ITEMS) {
            addRenderableWidget(Button.builder(Component.literal("§aGuardar items"), b -> sendEdit())
                    .bounds(x + w - 150, by, 90, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(x + w - 56, by, 56, 18).build());
    }

    private void buildRefreshTab() {
        addRenderableWidget(Button.builder(Component.literal("§bRefrescar Stock (restaurar originales)"), b -> {
            PacketHandler.sendToServer(new RefreshStockPacket(this.pos));
        }).bounds(bx(), by() + 20, Math.min(320, bw()), 18).build());
    }

    // ---- helpers used by the tab classes ----

    public void addW(final AbstractWidget widget) {
        addRenderableWidget(widget);
        if (widget instanceof EditBox box) {
            this.editBoxes.add(box);
        }
    }

    public Font font() {
        return this.font;
    }

    public int bx() {
        return this.leftPos + 8;
    }

    public int by() {
        return this.topPos + 42;
    }

    public int bw() {
        return this.imageWidth - 16;
    }

    public int bh() {
        return this.imageHeight - 42 - 30;
    }

    public void refresh() {
        rebuildWidgets();
    }

    public void sendPermissionsNow() {
        if (this.editMode) {
            PacketHandler.sendToServer(new UpdatePermissionsPacket(this.pos, new ArrayList<>(this.permitted)));
        }
    }

    private Map<String, Long> overridesAsIdMap() {
        final Map<String, Long> map = new LinkedHashMap<>();
        for (final Map.Entry<Item, Long> e : this.overrides.entrySet()) {
            final ResourceLocation rl = ForgeRegistries.ITEMS.getKey(e.getKey());
            if (rl != null) {
                map.put(rl.toString(), e.getValue());
            }
        }
        return map;
    }

    private void sendCreate() {
        PacketHandler.sendToServer(new CreateChestPacket(this.draftId, this.draftName, this.doBulk, this.bulkValue,
                overridesAsIdMap(), new ArrayList<>(this.permitted)));
        onClose();
    }

    private void sendEdit() {
        PacketHandler.sendToServer(new EditChestPacket(this.pos, this.doBulk, this.bulkValue, overridesAsIdMap()));
        onClose();
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -535291870);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 20, -14408646);
        g.fill(this.leftPos, this.topPos + this.imageHeight - 1, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -12961206);
        g.drawString(this.font, "§d\u2726 §fFantastic Chest §d\u2726 §7- " + (this.editMode ? "Edicion" : "Creacion"),
                this.leftPos + 8, this.topPos + 6, 16777215, false);

        switch (this.activeTab) {
            case ITEMS -> this.itemsTab.renderLabels(this, g);
            case GENERAL -> this.generalTab.renderLabels(this, g);
            case SECURITY -> this.securityTab.renderLabels(this, g);
            case REFRESH -> g.drawString(this.font,
                    "§7Restaura todas las cantidades a los valores originales configurados.", bx(), by(), 10133680, false);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Suppress default labels.
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        for (final EditBox box : this.editBoxes) {
            if (box.isFocused() && box.canConsumeInput()) {
                if (box.keyPressed(keyCode, scanCode, modifiers)) return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c, final int modifiers) {
        for (final EditBox box : this.editBoxes) {
            if (box.isFocused()) {
                return box.charTyped(c, modifiers);
            }
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
