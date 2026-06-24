/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.gui.screens;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.pewez777.fantastickits.Reference;
import com.pewez777.fantastickits.gui.client.EditorContext;
import com.pewez777.fantastickits.gui.widgets.ScrollSelector;
import com.pewez777.fantastickits.items.ItemEditorService;
import com.pewez777.fantastickits.kits.Kit;
import com.pewez777.fantastickits.nbt.NbtEditorService;
import com.pewez777.fantastickits.network.NetworkHandler;
import com.pewez777.fantastickits.network.packets.SaveKitPacket;
import com.pewez777.fantastickits.network.packets.TestKitPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The premium, fully client-side kit editor.
 *
 * <p>It is a real {@link Screen} (NOT a chest menu): it draws its own central
 * panel, header and footer, hosts in-screen TABS, uses {@link EditBox} widgets
 * for live text entry (never chat), {@link Button} widgets positioned with
 * {@code .bounds(...)} and a reusable {@link ScrollSelector} for every list. A
 * local copy of the kit is edited and, on Save, sent back for server-side
 * re-validation.</p>
 */
public final class KitEditorScreen extends Screen {

    private enum Tab {
        GENERAL("General"),
        ITEMS("Items"),
        NBT("NBT Editor"),
        GROUP("Group"),
        COMMANDS("Commands"),
        PREVIEW("Preview");

        private final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private static final int PANEL_W = 540;
    private static final int PANEL_H = 320;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private static final String[] HIDE_FLAG_LABELS = {"Hide Enchants", "Hide Attributes", "Hide Unbreakable"};
    private static final int[] HIDE_FLAG_BITS = {
            NbtEditorService.HIDE_ENCHANTMENTS,
            NbtEditorService.HIDE_ATTRIBUTES,
            NbtEditorService.HIDE_UNBREAKABLE
    };

    private final EditorContext ctx;
    private final Kit kit;

    private Tab activeTab = Tab.GENERAL;
    private int left;
    private int top;

    // General tab.
    private EditBox nameBox;
    private EditBox descBox;

    // Items tab.
    private ScrollSelector<ItemStack> itemSelector;

    // Group tab.
    private ScrollSelector<String> groupSelector;

    // Commands tab.
    private EditBox commandSearchBox;
    private ScrollSelector<String> catalogSelector;
    private ScrollSelector<String> ownedSelector;

    // NBT tab widgets.
    private EditBox nbtNameBox;
    private EditBox loreInputBox;
    private ScrollSelector<String> loreSelector;
    private EditBox enchantIdBox;
    private EditBox enchantLevelBox;
    private ScrollSelector<String> enchantSelector;
    private EditBox modelDataBox;
    private EditBox damageBox;
    private EditBox tagKeyBox;
    private EditBox tagValueBox;
    private ScrollSelector<ResourceLocation> attrSelector;
    private EditBox attrAmountBox;
    private int attrOperationIndex;
    private int attrSlotIndex;

    public KitEditorScreen(EditorContext ctx) {
        super(Component.translatable("fantastickits.title.editor"));
        this.ctx = ctx;
        this.kit = ctx.getKit();
    }

    private ItemStack selectedItem() {
        int index = ctx.getSelectedItemIndex();
        if (index < 0 || index >= kit.getItems().size()) {
            return ItemStack.EMPTY;
        }
        return kit.getItems().get(index);
    }

    // ========================================================================
    //  Initialization / layout
    // ========================================================================

    @Override
    protected void init() {
        this.left = (this.width - PANEL_W) / 2;
        this.top = (this.height - PANEL_H) / 2;
        buildTabBar();
        buildFooter();
        switch (activeTab) {
            case GENERAL -> buildGeneralTab();
            case ITEMS -> buildItemsTab();
            case NBT -> buildNbtTab();
            case GROUP -> buildGroupTab();
            case COMMANDS -> buildCommandsTab();
            case PREVIEW -> buildPreviewTab();
        }
    }

    private void buildTabBar() {
        Tab[] tabs = Tab.values();
        int tabW = (PANEL_W - 16) / tabs.length;
        int y = top + 26;
        for (int i = 0; i < tabs.length; i++) {
            Tab tab = tabs[i];
            Button button = Button.builder(Component.literal(tab.label), b -> switchTab(tab))
                    .bounds(left + 8 + i * tabW, y, tabW - 2, 18)
                    .build();
            button.active = tab != activeTab;
            addRenderableWidget(button);
        }
    }

    private void buildFooter() {
        int y = top + PANEL_H - 24;
        addRenderableWidget(Button.builder(Component.translatable("fantastickits.button.save"),
                        b -> save())
                .bounds(left + 8, y, 110, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("fantastickits.button.cancel"),
                        b -> onClose())
                .bounds(left + 124, y, 110, 18)
                .build());
        if (ctx.isEditMode()) {
            addRenderableWidget(Button.builder(Component.literal("Test"), b -> testKit())
                    .bounds(left + PANEL_W - 118, y, 110, 18)
                    .build());
        }
    }

    private void switchTab(Tab tab) {
        this.activeTab = tab;
        rebuildWidgets();
    }

    private int contentTop() {
        return top + 50;
    }

    // ========================================================================
    //  GENERAL tab
    // ========================================================================

    private void buildGeneralTab() {
        int x = left + 16;
        int y = contentTop() + 14;

        nameBox = new EditBox(this.font, x, y, 260, 18, Component.translatable("fantastickits.label.name"));
        nameBox.setMaxLength(48);
        nameBox.setValue(kit.getName());
        nameBox.setResponder(kit::setName);
        addRenderableWidget(nameBox);

        y += 40;
        descBox = new EditBox(this.font, x, y, 360, 18,
                Component.translatable("fantastickits.label.description"));
        descBox.setMaxLength(160);
        descBox.setValue(kit.getDescription());
        descBox.setResponder(kit::setDescription);
        addRenderableWidget(descBox);

        y += 44;
        Button strictButton = Button.builder(strictLabel(), b -> {
            kit.setStrictGroupMatching(!kit.isStrictGroupMatching());
            b.setMessage(strictLabel());
        }).bounds(x, y, 240, 18).build();
        addRenderableWidget(strictButton);

        y += 24;
        Button singleButton = Button.builder(singleLabel(), b -> {
            kit.setSingleClaim(!kit.isSingleClaim());
            b.setMessage(singleLabel());
        }).bounds(x, y, 240, 18).build();
        addRenderableWidget(singleButton);
    }

    private Component strictLabel() {
        return Component.literal("Strict group matching: "
                + (kit.isStrictGroupMatching() ? "ON" : "OFF"))
                .withStyle(kit.isStrictGroupMatching() ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private Component singleLabel() {
        return Component.literal("Single permanent claim: "
                + (kit.isSingleClaim() ? "ON" : "OFF"))
                .withStyle(kit.isSingleClaim() ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    // ========================================================================
    //  ITEMS tab
    // ========================================================================

    private void buildItemsTab() {
        int x = left + 16;
        int y = contentTop() + 14;

        itemSelector = new ScrollSelector<>(x, y, 300, 200, Component.literal("Items"));
        itemSelector.setLabelFunction(stack -> stack.getHoverName());
        itemSelector.setItems(new ArrayList<>(kit.getItems()));
        itemSelector.setOnSelect(stack -> ctx.setSelectedItemIndex(kit.getItems().indexOf(stack)));
        addRenderableWidget(itemSelector);

        int bx = x + 312;
        addRenderableWidget(Button.builder(Component.literal("Add from hand"), b -> addFromHand())
                .bounds(bx, y, 150, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("fantastickits.button.remove"),
                        b -> removeSelectedItem())
                .bounds(bx, y + 24, 150, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("fantastickits.button.up"),
                        b -> moveItem(true))
                .bounds(bx, y + 48, 72, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("fantastickits.button.down"),
                        b -> moveItem(false))
                .bounds(bx + 78, y + 48, 72, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Edit NBT"), b -> {
            if (ctx.getSelectedItemIndex() >= 0) {
                switchTab(Tab.NBT);
            }
        }).bounds(bx, y + 72, 150, 18).build());
    }

    private void addFromHand() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        ItemStack hand = minecraft.player.getMainHandItem();
        if (hand.isEmpty()) {
            return;
        }
        ItemEditorService.addItem(kit.getItems(), hand);
        rebuildWidgets();
    }

    private void removeSelectedItem() {
        int index = ctx.getSelectedItemIndex();
        if (ItemEditorService.removeItem(kit.getItems(), index)) {
            ctx.setSelectedItemIndex(-1);
            rebuildWidgets();
        }
    }

    private void moveItem(boolean up) {
        int index = ctx.getSelectedItemIndex();
        boolean moved = up ? ItemEditorService.moveUp(kit.getItems(), index)
                : ItemEditorService.moveDown(kit.getItems(), index);
        if (moved) {
            ctx.setSelectedItemIndex(up ? index - 1 : index + 1);
            rebuildWidgets();
        }
    }

    // ========================================================================
    //  GROUP tab
    // ========================================================================

    private void buildGroupTab() {
        int x = left + 16;
        int y = contentTop() + 24;
        groupSelector = new ScrollSelector<>(x, y, 280, 180, Component.literal("Groups"));
        groupSelector.setLabelFunction(Component::literal);
        groupSelector.setItems(new ArrayList<>(ctx.getGroups()));
        groupSelector.setOnSelect(kit::setOwnerGroup);
        addRenderableWidget(groupSelector);
    }

    // ========================================================================
    //  COMMANDS tab
    // ========================================================================

    private void buildCommandsTab() {
        int x = left + 16;
        int y = contentTop() + 6;

        commandSearchBox = new EditBox(this.font, x, y, 280, 18,
                Component.translatable("fantastickits.label.search"));
        commandSearchBox.setHint(Component.literal("Search commands..."));
        commandSearchBox.setResponder(text -> {
            if (catalogSelector != null) {
                catalogSelector.setFilter(text);
            }
        });
        addRenderableWidget(commandSearchBox);

        catalogSelector = new ScrollSelector<>(x, y + 24, 280, 170, Component.literal("Catalog"));
        catalogSelector.setLabelFunction(c -> Component.literal("/" + c));
        catalogSelector.setItems(new ArrayList<>(ctx.getCommandCatalog()));
        addRenderableWidget(catalogSelector);

        int rx = x + 296;
        ownedSelector = new ScrollSelector<>(rx, y + 24, 200, 170, Component.literal("Owned"));
        ownedSelector.setLabelFunction(c -> Component.literal("/" + c));
        ownedSelector.setItems(new ArrayList<>(kit.getCommands()));
        addRenderableWidget(ownedSelector);

        addRenderableWidget(Button.builder(Component.literal(">> Add"), b -> addSelectedCommand())
                .bounds(rx, y, 96, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Remove <<"), b -> removeSelectedCommand())
                .bounds(rx + 100, y, 100, 18).build());
    }

    private void addSelectedCommand() {
        if (catalogSelector == null) {
            return;
        }
        String selected = catalogSelector.getSelected();
        if (selected != null && !kit.getCommands().contains(selected)) {
            kit.getCommands().add(selected);
            ownedSelector.setItems(new ArrayList<>(kit.getCommands()));
        }
    }

    private void removeSelectedCommand() {
        if (ownedSelector == null) {
            return;
        }
        String selected = ownedSelector.getSelected();
        if (selected != null) {
            kit.getCommands().remove(selected);
            ownedSelector.setItems(new ArrayList<>(kit.getCommands()));
            ownedSelector.clearSelection();
        }
    }

    // ========================================================================
    //  NBT tab
    // ========================================================================

    private void buildNbtTab() {
        ItemStack stack = selectedItem();
        if (stack.isEmpty()) {
            return; // overlay text explains how to select an item
        }
        int colA = left + 14;
        int colB = left + 280;
        int y = contentTop();

        // -- Column A: display name + lore --
        nbtNameBox = new EditBox(this.font, colA, y + 12, 200, 16, Component.literal("Display name"));
        nbtNameBox.setMaxLength(80);
        addRenderableWidget(nbtNameBox);
        addRenderableWidget(Button.builder(Component.literal("Set Name"),
                        b -> applyAndRefresh(() -> NbtEditorService.setDisplayName(stack, nbtNameBox.getValue())))
                .bounds(colA + 206, y + 12, 50, 16).build());

        loreInputBox = new EditBox(this.font, colA, y + 36, 200, 16, Component.literal("Lore line"));
        loreInputBox.setMaxLength(120);
        addRenderableWidget(loreInputBox);
        addRenderableWidget(Button.builder(Component.literal("Add"), b -> addLore(stack))
                .bounds(colA + 206, y + 36, 50, 16).build());

        loreSelector = new ScrollSelector<>(colA, y + 58, 256, 80, Component.literal("Lore"));
        loreSelector.setLabelFunction(Component::literal);
        loreSelector.setItems(NbtEditorService.getLoreRaw(stack));
        addRenderableWidget(loreSelector);
        addRenderableWidget(Button.builder(Component.literal("Remove lore"), b -> removeLore(stack))
                .bounds(colA, y + 142, 120, 16).build());

        // -- Column A bottom: enchantments --
        enchantIdBox = new EditBox(this.font, colA, y + 166, 150, 16, Component.literal("Enchant id"));
        enchantIdBox.setHint(Component.literal("minecraft:sharpness"));
        addRenderableWidget(enchantIdBox);
        enchantLevelBox = new EditBox(this.font, colA + 156, y + 166, 40, 16, Component.literal("Lvl"));
        enchantLevelBox.setValue("1");
        addRenderableWidget(enchantLevelBox);
        addRenderableWidget(Button.builder(Component.literal("Add Ench"), b -> addEnchant(stack))
                .bounds(colA + 200, y + 166, 56, 16).build());
        enchantSelector = new ScrollSelector<>(colA, y + 188, 256, 56, Component.literal("Enchants"));
        enchantSelector.setLabelFunction(Component::literal);
        enchantSelector.setItems(currentEnchantStrings(stack));
        addRenderableWidget(enchantSelector);
        addRenderableWidget(Button.builder(Component.literal("Remove Ench"), b -> removeEnchant(stack))
                .bounds(colA, y + 246, 120, 16).build());

        // -- Column B: flags, durability, model data, tags, attributes --
        for (int i = 0; i < HIDE_FLAG_LABELS.length; i++) {
            final int bit = HIDE_FLAG_BITS[i];
            boolean on = (NbtEditorService.getHideFlags(stack) & bit) != 0;
            addRenderableWidget(Button.builder(
                            Component.literal(HIDE_FLAG_LABELS[i] + ": " + (on ? "ON" : "OFF")),
                            b -> applyAndRefresh(() ->
                                    NbtEditorService.toggleHideFlag(stack, bit,
                                            (NbtEditorService.getHideFlags(stack) & bit) == 0)))
                    .bounds(colB, y + 12 + i * 20, 150, 16).build());
        }

        boolean unbreakable = NbtEditorService.isUnbreakable(stack);
        addRenderableWidget(Button.builder(
                        Component.literal("Unbreakable: " + (unbreakable ? "ON" : "OFF")),
                        b -> applyAndRefresh(() ->
                                NbtEditorService.setUnbreakable(stack, !NbtEditorService.isUnbreakable(stack))))
                .bounds(colB, y + 72, 150, 16).build());

        damageBox = new EditBox(this.font, colB, y + 94, 70, 16, Component.literal("Damage"));
        damageBox.setValue(String.valueOf(stack.getDamageValue()));
        addRenderableWidget(damageBox);
        addRenderableWidget(Button.builder(Component.literal("Set Dmg"),
                        b -> applyAndRefresh(() ->
                                NbtEditorService.setDamage(stack, parseInt(damageBox.getValue(), 0))))
                .bounds(colB + 76, y + 94, 74, 16).build());

        modelDataBox = new EditBox(this.font, colB, y + 116, 70, 16, Component.literal("Model data"));
        modelDataBox.setValue(String.valueOf(NbtEditorService.getCustomModelData(stack)));
        addRenderableWidget(modelDataBox);
        addRenderableWidget(Button.builder(Component.literal("Set CMD"),
                        b -> applyAndRefresh(() ->
                                NbtEditorService.setCustomModelData(stack, parseInt(modelDataBox.getValue(), 0))))
                .bounds(colB + 76, y + 116, 74, 16).build());

        tagKeyBox = new EditBox(this.font, colB, y + 138, 70, 16, Component.literal("Tag key"));
        tagKeyBox.setHint(Component.literal("key"));
        addRenderableWidget(tagKeyBox);
        tagValueBox = new EditBox(this.font, colB + 76, y + 138, 74, 16, Component.literal("Tag value"));
        tagValueBox.setHint(Component.literal("value"));
        addRenderableWidget(tagValueBox);
        addRenderableWidget(Button.builder(Component.literal("Add Tag"),
                        b -> applyAndRefresh(() ->
                                NbtEditorService.putCustomTag(stack, tagKeyBox.getValue(), tagValueBox.getValue())))
                .bounds(colB, y + 158, 150, 16).build());

        // Attributes.
        attrSelector = new ScrollSelector<>(colB, y + 182, 150, 44, Component.literal("Attributes"));
        attrSelector.setLabelFunction(rl -> Component.literal(rl.getPath()));
        List<ResourceLocation> attrKeys = new ArrayList<>(ForgeRegistries.ATTRIBUTES.getKeys());
        attrKeys.sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
        attrSelector.setItems(attrKeys);
        addRenderableWidget(attrSelector);

        attrAmountBox = new EditBox(this.font, colB, y + 230, 50, 16, Component.literal("Amount"));
        attrAmountBox.setValue("1.0");
        addRenderableWidget(attrAmountBox);
        addRenderableWidget(Button.builder(Component.literal(opLabel()), b -> {
            attrOperationIndex = (attrOperationIndex + 1) % AttributeModifier.Operation.values().length;
            b.setMessage(Component.literal(opLabel()));
        }).bounds(colB + 54, y + 230, 44, 16).build());
        addRenderableWidget(Button.builder(Component.literal(slotLabel()), b -> {
            attrSlotIndex = (attrSlotIndex + 1) % EquipmentSlot.values().length;
            b.setMessage(Component.literal(slotLabel()));
        }).bounds(colB + 100, y + 230, 50, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Add Attr"), b -> addAttribute(stack))
                .bounds(colB, y + 250, 74, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Clear Attr"),
                        b -> applyAndRefresh(() -> NbtEditorService.clearAttributeModifiers(stack)))
                .bounds(colB + 78, y + 250, 72, 16).build());
    }

    private String opLabel() {
        return switch (AttributeModifier.Operation.values()[attrOperationIndex]) {
            case ADDITION -> "ADD";
            case MULTIPLY_BASE -> "xBASE";
            case MULTIPLY_TOTAL -> "xTOT";
        };
    }

    private String slotLabel() {
        return EquipmentSlot.values()[attrSlotIndex].getName();
    }

    private void addLore(ItemStack stack) {
        String line = loreInputBox.getValue();
        if (line == null || line.isEmpty()) {
            return;
        }
        List<String> lore = NbtEditorService.getLoreRaw(stack);
        lore.add(line);
        NbtEditorService.setLore(stack, lore);
        rebuildWidgets();
    }

    private void removeLore(ItemStack stack) {
        if (loreSelector == null) {
            return;
        }
        String selected = loreSelector.getSelected();
        if (selected == null) {
            return;
        }
        List<String> lore = NbtEditorService.getLoreRaw(stack);
        lore.remove(selected);
        NbtEditorService.setLore(stack, lore);
        rebuildWidgets();
    }

    private void addEnchant(ItemStack stack) {
        ResourceLocation id = ResourceLocation.tryParse(enchantIdBox.getValue().trim());
        if (id == null) {
            return;
        }
        Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(id);
        if (enchantment == null) {
            return;
        }
        int level = parseInt(enchantLevelBox.getValue(), 1);
        NbtEditorService.setEnchantment(stack, enchantment, level);
        rebuildWidgets();
    }

    private void removeEnchant(ItemStack stack) {
        if (enchantSelector == null) {
            return;
        }
        String selected = enchantSelector.getSelected();
        if (selected == null) {
            return;
        }
        String idPart = selected.contains(" ") ? selected.substring(0, selected.indexOf(' ')) : selected;
        ResourceLocation id = ResourceLocation.tryParse(idPart);
        if (id == null) {
            return;
        }
        Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(id);
        if (enchantment != null) {
            NbtEditorService.removeEnchantment(stack, enchantment);
            rebuildWidgets();
        }
    }

    private void addAttribute(ItemStack stack) {
        if (attrSelector == null) {
            return;
        }
        ResourceLocation key = attrSelector.getSelected();
        if (key == null) {
            return;
        }
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(key);
        if (attribute == null) {
            return;
        }
        double amount = parseDouble(attrAmountBox.getValue(), 1.0);
        AttributeModifier.Operation op = AttributeModifier.Operation.values()[attrOperationIndex];
        EquipmentSlot slot = EquipmentSlot.values()[attrSlotIndex];
        NbtEditorService.addAttributeModifier(stack, attribute, key.getPath(), amount, op, slot);
        rebuildWidgets();
    }

    private List<String> currentEnchantStrings(ItemStack stack) {
        List<String> out = new ArrayList<>();
        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);
        for (Map.Entry<Enchantment, Integer> entry : map.entrySet()) {
            ResourceLocation rl = BuiltInRegistries.ENCHANTMENT.getKey(entry.getKey());
            if (rl != null) {
                out.add(rl + " " + entry.getValue());
            }
        }
        return out;
    }

    private void applyAndRefresh(Runnable action) {
        action.run();
        rebuildWidgets();
    }

    // ========================================================================
    //  PREVIEW tab
    // ========================================================================

    private void buildPreviewTab() {
        // Preview is render-only; no interactive widgets needed here.
    }

    // ========================================================================
    //  Save / test
    // ========================================================================

    private void save() {
        if (kit.getName() == null || kit.getName().isBlank()) {
            return;
        }
        NetworkHandler.sendToServer(new SaveKitPacket(kit.toNbt()));
        onClose();
    }

    private void testKit() {
        NetworkHandler.sendToServer(new TestKitPacket(kit.getName()));
    }

    // ========================================================================
    //  Rendering
    // ========================================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        // Panel.
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xF00E0E16);
        graphics.renderOutline(left, top, PANEL_W, PANEL_H, 0xFF44445A);
        // Header bar.
        graphics.fill(left, top, left + PANEL_W, top + 22, 0xFF1A1A28);
        // Footer bar.
        graphics.fill(left, top + PANEL_H - 28, left + PANEL_W, top + PANEL_H, 0xFF1A1A28);

        String title = (ctx.isEditMode() ? "Editing kit: " : "Creating kit: ")
                + (kit.getName().isEmpty() ? "(unnamed)" : kit.getName());
        graphics.drawString(this.font, Component.literal(title).withStyle(ChatFormatting.WHITE),
                left + 10, top + 7, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);

        switch (activeTab) {
            case GENERAL -> renderGeneralOverlay(graphics);
            case ITEMS -> renderItemsOverlay(graphics, mouseX, mouseY);
            case NBT -> renderNbtOverlay(graphics);
            case GROUP -> renderGroupOverlay(graphics);
            case COMMANDS -> renderCommandsOverlay(graphics);
            case PREVIEW -> renderPreviewOverlay(graphics, mouseX, mouseY);
        }

        graphics.drawString(this.font,
                Component.literal(Reference.COPYRIGHT.substring(0, Math.min(Reference.COPYRIGHT.length(), 64)))
                        .withStyle(ChatFormatting.DARK_GRAY),
                left + 10, top + PANEL_H - 40, 0x808080, false);
    }

    private void label(GuiGraphics graphics, String text, int x, int y) {
        graphics.drawString(this.font, Component.literal(text).withStyle(ChatFormatting.GRAY),
                x, y, 0xFFB0B0B0, false);
    }

    private void renderGeneralOverlay(GuiGraphics graphics) {
        int x = left + 16;
        label(graphics, "Name", x, contentTop() + 2);
        label(graphics, "Description", x, contentTop() + 42);
        label(graphics, "Owner group: " + (kit.getOwnerGroup().isEmpty() ? "(none)" : kit.getOwnerGroup()),
                x, contentTop() + 156);
        label(graphics, "UUID: " + kit.getId(), x, contentTop() + 170);
        label(graphics, "Created: " + DATE.format(Instant.ofEpochMilli(kit.getCreatedAt())),
                x, contentTop() + 184);
    }

    private void renderItemsOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
        label(graphics, "Items (" + kit.getItems().size() + "/" + ItemEditorService.MAX_ITEMS + ")",
                left + 16, contentTop() + 2);
        // Icon + tooltip of the selected item.
        ItemStack selected = selectedItem();
        if (!selected.isEmpty()) {
            int ix = left + 330;
            int iy = contentTop() + 110;
            graphics.renderItem(selected, ix, iy);
            graphics.renderItemDecorations(this.font, selected, ix, iy);
            if (mouseX >= ix && mouseX <= ix + 16 && mouseY >= iy && mouseY <= iy + 16) {
                graphics.renderTooltip(this.font, selected, mouseX, mouseY);
            }
        }
    }

    private void renderNbtOverlay(GuiGraphics graphics) {
        if (selectedItem().isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.literal("Select an item in the Items tab to edit its NBT.")
                            .withStyle(ChatFormatting.YELLOW),
                    left + PANEL_W / 2, contentTop() + 80, 0xFFFF55);
            return;
        }
        label(graphics, "Display / Lore", left + 14, contentTop() - 2);
        label(graphics, "Flags / Durability / Tags / Attributes", left + 280, contentTop() - 2);
    }

    private void renderGroupOverlay(GuiGraphics graphics) {
        label(graphics, "Group Assignment", left + 16, contentTop() + 2);
        label(graphics, "Selected owner group: "
                        + (kit.getOwnerGroup().isEmpty() ? "(none)" : kit.getOwnerGroup()),
                left + 16, contentTop() + 8);
        if (!ctx.isLuckPermsAvailable()) {
            graphics.drawString(this.font,
                    Component.literal("LuckPerms not detected - group list may be empty.")
                            .withStyle(ChatFormatting.RED),
                    left + 310, contentTop() + 30, 0xFF5555, false);
        } else {
            graphics.drawString(this.font,
                    Component.literal("Groups loaded from LuckPerms.").withStyle(ChatFormatting.GREEN),
                    left + 310, contentTop() + 30, 0x55FF55, false);
        }
    }

    private void renderCommandsOverlay(GuiGraphics graphics) {
        label(graphics, "Command Manager - search, add and remove per-kit commands",
                left + 16, contentTop() - 8);
        label(graphics, "Catalog", left + 16, contentTop() + 18);
        label(graphics, "Owned by this kit", left + 312, contentTop() + 18);
    }

    private void renderPreviewOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = left + 16;
        int y = contentTop();
        graphics.drawString(this.font,
                Component.literal("Preview - exactly what the player receives")
                        .withStyle(ChatFormatting.AQUA), x, y, 0x55FFFF, false);
        graphics.renderItem(kit.getIcon(), x, y + 14);
        graphics.drawString(this.font, Component.literal(kit.getName()).withStyle(ChatFormatting.GOLD),
                x + 22, y + 18, 0xFFD700, false);
        label(graphics, kit.getDescription(), x, y + 36);
        label(graphics, "Owner group: " + (kit.getOwnerGroup().isEmpty() ? "(none)" : kit.getOwnerGroup()),
                x, y + 50);

        // Item grid with tooltips.
        int gridX = x;
        int gridY = y + 70;
        int col = 0;
        int row = 0;
        for (ItemStack stack : kit.getItems()) {
            int ix = gridX + col * 20;
            int iy = gridY + row * 20;
            graphics.renderItem(stack, ix, iy);
            graphics.renderItemDecorations(this.font, stack, ix, iy);
            if (mouseX >= ix && mouseX <= ix + 16 && mouseY >= iy && mouseY <= iy + 16) {
                graphics.renderTooltip(this.font, stack, mouseX, mouseY);
            }
            col++;
            if (col >= 18) {
                col = 0;
                row++;
            }
        }

        int cmdY = gridY + (row + 2) * 20;
        label(graphics, "Commands:", x, cmdY);
        int i = 0;
        for (String command : kit.getCommands()) {
            graphics.drawString(this.font, Component.literal("/" + command).withStyle(ChatFormatting.GRAY),
                    x, cmdY + 12 + i * 10, 0xFFB0B0B0, false);
            i++;
            if (i > 6) {
                break;
            }
        }
    }

    // ========================================================================
    //  Utility
    // ========================================================================

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
