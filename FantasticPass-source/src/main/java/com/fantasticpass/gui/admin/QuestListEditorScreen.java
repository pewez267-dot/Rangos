/*
 * Decompiled with CFR 0.152.
 */
package com.fantasticpass.gui.admin;

import com.fantasticpass.gui.widgets.ScrollSelector;
import com.fantasticpass.quest.Quest;
import com.fantasticpass.quest.QuestType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public final class QuestListEditorScreen
extends Screen {
    private final Screen parent;
    private final List<Quest> target;
    private final String idPrefix;
    private final Component heading;
    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;
    private EditBox typeSearch;
    private EditBox paramSearch;
    private EditBox targetBox;
    private EditBox pointsBox;
    private ScrollSelector<QuestType> typeSelector;
    private ScrollSelector<ResourceLocation> paramSelector;
    private ScrollSelector<Quest> currentList;
    private Button categoryButton;
    private Button addButton;
    private Button removeButton;
    private Quest editing;
    private QuestType selectedType;
    private boolean showCustom;
    private final List<Hint> hints = new ArrayList<Hint>();
    private static final Set<EntityType<?>> LIVING_MISC = Set.of(EntityType.VILLAGER, EntityType.WANDERING_TRADER, EntityType.IRON_GOLEM, EntityType.SNOW_GOLEM, EntityType.ARMOR_STAND);

    public QuestListEditorScreen(Screen parent, Component heading, List<Quest> target, String idPrefix) {
        super((Component)Component.translatable((String)"fantasticpass.gui.quest_editor"));
        this.parent = parent;
        this.target = target;
        this.idPrefix = idPrefix;
        this.heading = heading;
    }

    protected void init() {
        this.hints.clear();
        this.panelWidth = Math.min(this.width - 16, 470);
        this.panelHeight = Math.min(this.height - 16, 270);
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;
        int lx = this.leftPos + 12;
        int leftW = (this.panelWidth - 36) / 2;
        int rx = lx + leftW + 12;
        int rightW = this.leftPos + this.panelWidth - 12 - rx;
        this.categoryButton = (Button)this.addRenderableWidget(Button.builder((Component)this.categoryLabel(), b -> this.toggleCategory()).bounds(lx, this.topPos + 34, leftW, 14).build());
        this.hint(lx, this.topPos + 34, leftW, 14, "fantasticpass.gui.objective_type", "fantasticpass.gui.tip_ql_category");
        this.typeSearch = (EditBox)this.addRenderableWidget(new EditBox(this.font, lx, this.topPos + 52, leftW, 16, (Component)Component.empty()));
        this.typeSearch.setHint((Component)Component.translatable((String)"fantasticpass.gui.search"));
        this.typeSearch.setResponder(q -> this.typeSelector.setQuery((String)q));
        this.hint(lx, this.topPos + 52, leftW, 16, "fantasticpass.gui.search", "fantasticpass.gui.tip_ql_search");
        this.typeSelector = (ScrollSelector)this.addRenderableWidget(new ScrollSelector<QuestType>(lx, this.topPos + 72, leftW, this.panelHeight - 72 - 30, 16, t -> this.typeName((QuestType)((Object)t)), t -> this.typeName((QuestType)((Object)t)) + " " + t.getId(), t -> this.typeIcon((QuestType)((Object)t))));
        this.typeSelector.setItems(this.typesForCategory());
        this.typeSelector.onSelect(this::onTypePicked);
        this.hint(lx, this.topPos + 72, leftW, this.panelHeight - 72 - 30, "fantasticpass.gui.objective_type", "fantasticpass.gui.tip_ql_type_list");
        this.paramSearch = (EditBox)this.addRenderableWidget(new EditBox(this.font, rx, this.topPos + 36, rightW, 16, (Component)Component.empty()));
        this.paramSearch.setHint((Component)Component.translatable((String)"fantasticpass.gui.search"));
        this.paramSearch.setResponder(q -> this.paramSelector.setQuery((String)q));
        this.paramSelector = (ScrollSelector)this.addRenderableWidget(new ScrollSelector<ResourceLocation>(rx, this.topPos + 56, rightW, 60, 16, this::paramLabel, rl -> rl.toString(), this::paramIcon));
        this.hint(rx, this.topPos + 36, rightW, 80, "fantasticpass.gui.objective_target", "fantasticpass.gui.tip_ql_target");
        int numY = this.topPos + 132;
        this.targetBox = (EditBox)this.addRenderableWidget(new EditBox(this.font, rx, numY, 70, 16, (Component)Component.empty()));
        this.targetBox.setFilter(s -> s.matches("\\d*"));
        this.targetBox.setValue("10");
        this.hint(rx, numY, 70, 16, "fantasticpass.gui.count", "fantasticpass.gui.tip_ql_count");
        this.pointsBox = (EditBox)this.addRenderableWidget(new EditBox(this.font, rx + 92, numY, 70, 16, (Component)Component.empty()));
        this.pointsBox.setFilter(s -> s.matches("\\d*"));
        this.pointsBox.setValue("10");
        this.hint(rx + 92, numY, 70, 16, "fantasticpass.gui.points_field", "fantasticpass.gui.tip_ql_points");
        int addW = rightW - 74;
        this.addButton = (Button)this.addRenderableWidget(Button.builder((Component)Component.translatable((String)"fantasticpass.gui.add_quest").withStyle(ChatFormatting.GREEN), b -> this.addQuest()).bounds(rx, numY + 22, addW, 18).build());
        this.hint(rx, numY + 22, addW, 18, "fantasticpass.gui.add_quest", "fantasticpass.gui.tip_ql_add");
        this.removeButton = (Button)this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7cQuitar"), b -> this.removeSelected()).bounds(rx + addW + 4, numY + 22, 70, 18).build());
        this.hints.add(new Hint(rx + addW + 4, numY + 22, 70, 18, List.of(Component.literal((String)"\u00a7c\u00a7lQuitar"), Component.literal((String)"\u00a77Elimina la misi\u00f3n seleccionada de la lista."), Component.literal((String)"\u00a77Toca una misi\u00f3n para cargar y editar sus valores."))));
        this.currentList = (ScrollSelector)this.addRenderableWidget(new ScrollSelector<Quest>(rx, numY + 54, rightW, this.panelHeight - (numY - this.topPos) - 54 - 28, 16, this::questLabel, this::questLabel, q -> new ItemStack((ItemLike)Items.WRITABLE_BOOK)));
        this.currentList.onSelect(this::loadForEdit);
        this.hint(rx, numY + 54, rightW, this.panelHeight - (numY - this.topPos) - 54 - 28, "fantasticpass.gui.current_list", "fantasticpass.gui.tip_ql_list");
        this.refreshList();
        this.updateAddButtonLabel();
        this.addRenderableWidget(Button.builder((Component)Component.translatable((String)"fantasticpass.gui.close"), b -> this.onClose()).bounds(lx, this.topPos + this.panelHeight - 24, leftW, 18).build());
        this.hint(lx, this.topPos + this.panelHeight - 24, leftW, 18, "fantasticpass.gui.close", "fantasticpass.gui.tip_ql_back");
        this.updateParamVisibility();
    }

    private void hint(int x, int y, int w, int h, String titleKey, String descKey) {
        this.hints.add(new Hint(x, y, w, h, List.of(Component.translatable((String)titleKey).withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}), Component.translatable((String)descKey).withStyle(ChatFormatting.GRAY))));
    }

    private String typeName(QuestType t) {
        return Component.translatable((String)t.descriptionKey(), (Object[])new Object[]{"N", "..."}).getString();
    }

    private List<QuestType> typesForCategory() {
        ArrayList<QuestType> out = new ArrayList<QuestType>();
        for (QuestType t : QuestType.values()) {
            if (t == QuestType.PLAY_MINUTES || t.isParameterized() != this.showCustom) continue;
            out.add(t);
        }
        return out;
    }

    private Component categoryLabel() {
        return this.showCustom ? Component.translatable((String)"fantasticpass.gui.cat_custom").withStyle(ChatFormatting.LIGHT_PURPLE) : Component.translatable((String)"fantasticpass.gui.cat_generic").withStyle(ChatFormatting.AQUA);
    }

    private void toggleCategory() {
        this.showCustom = !this.showCustom;
        this.categoryButton.setMessage(this.categoryLabel());
        this.selectedType = null;
        this.typeSearch.setValue("");
        this.typeSelector.clearSelection();
        this.typeSelector.setQuery("");
        this.typeSelector.setItems(this.typesForCategory());
        this.paramSelector.clearSelection();
        this.paramSelector.setItems(new ArrayList());
        this.updateParamVisibility();
    }

    private ItemStack typeIcon(QuestType t) {
        return t.isParameterized() ? new ItemStack((ItemLike)Items.NAME_TAG) : new ItemStack((ItemLike)Items.PAPER);
    }

    private void onTypePicked(QuestType type) {
        this.editing = null;
        this.currentList.clearSelection();
        this.updateAddButtonLabel();
        this.selectedType = type;
        this.paramSearch.setValue("");
        this.paramSelector.setQuery("");
        this.paramSelector.clearSelection();
        this.paramSelector.setItems(this.targetsFor(type));
        this.updateParamVisibility();
    }

    private void updateParamVisibility() {
        boolean param;
        this.paramSearch.visible = param = this.selectedType != null && this.selectedType.isParameterized();
        this.paramSelector.visible = param;
        this.paramSearch.active = param;
        this.paramSelector.active = param;
    }

    private List<ResourceLocation> targetsFor(QuestType type) {
        ArrayList<ResourceLocation> out = new ArrayList<ResourceLocation>();
        if (type == null) {
            return out;
        }
        switch (type.getParamKind()) {
            case ENTITY: {
                for (ResourceLocation rl : BuiltInRegistries.ENTITY_TYPE.keySet()) {
                    EntityType et = (EntityType)BuiltInRegistries.ENTITY_TYPE.get(rl);
                    if (et == null || !this.isEntityValid(type, et)) continue;
                    out.add(rl);
                }
                break;
            }
            case BLOCK: {
                for (ResourceLocation rl : BuiltInRegistries.BLOCK.keySet()) {
                    if (((Block)BuiltInRegistries.BLOCK.get(rl)).asItem() == Items.AIR) continue;
                    out.add(rl);
                }
                break;
            }
            case ITEM: {
                for (ResourceLocation rl : BuiltInRegistries.ITEM.keySet()) {
                    if (BuiltInRegistries.ITEM.get(rl) == Items.AIR) continue;
                    out.add(rl);
                }
                break;
            }
        }
        out.sort((a, b) -> a.toString().compareTo(b.toString()));
        return out;
    }

    private boolean isEntityValid(QuestType type, EntityType<?> et) {
        if (et == EntityType.PLAYER) {
            return false;
        }
        MobCategory cat = et.getCategory();
        boolean animal = cat == MobCategory.CREATURE || cat == MobCategory.WATER_CREATURE || cat == MobCategory.WATER_AMBIENT || cat == MobCategory.UNDERGROUND_WATER_CREATURE || cat == MobCategory.AXOLOTLS || cat == MobCategory.AMBIENT;
        switch (type) {
            case TAME_ENTITY: 
            case BREED_ENTITY: {
                return animal;
            }
            case KILL_ENTITY: {
                return cat != MobCategory.MISC || LIVING_MISC.contains(et);
            }
        }
        return true;
    }

    private String paramLabel(ResourceLocation rl) {
        if (this.selectedType == null) {
            return rl.toString();
        }
        return Quest.paramName(this.selectedType, rl.toString()).getString() + " \u00a78(" + rl + ")";
    }

    private ItemStack paramIcon(ResourceLocation rl) {
        if (this.selectedType == null) {
            return ItemStack.EMPTY;
        }
        switch (this.selectedType.getParamKind()) {
            case ITEM: {
                return BuiltInRegistries.ITEM.containsKey(rl) ? new ItemStack((ItemLike)BuiltInRegistries.ITEM.get(rl)) : ItemStack.EMPTY;
            }
            case BLOCK: {
                return BuiltInRegistries.BLOCK.containsKey(rl) ? new ItemStack((ItemLike)BuiltInRegistries.BLOCK.get(rl)) : ItemStack.EMPTY;
            }
        }
        return new ItemStack((ItemLike)Items.NAME_TAG);
    }

    private String questLabel(Quest q) {
        return q.getDescription().getString() + " \u00a7b+" + q.getPoints();
    }

    private int parse(EditBox box, int def) {
        try {
            return box.getValue().isEmpty() ? def : Integer.parseInt(box.getValue());
        }
        catch (NumberFormatException e) {
            return def;
        }
    }

    private void addQuest() {
        if (this.editing != null) {
            int idx = this.target.indexOf(this.editing);
            Quest updated = new Quest(this.editing.getId(), this.editing.getType(), this.editing.getParam(), this.parse(this.targetBox, 10), this.parse(this.pointsBox, 10), this.editing.getCustomTitle());
            if (idx >= 0) {
                this.target.set(idx, updated);
            } else {
                this.target.add(updated);
            }
            this.editing = null;
            this.currentList.clearSelection();
            this.updateAddButtonLabel();
            this.refreshList();
            return;
        }
        QuestType type = this.typeSelector.getSelected();
        if (type == null) {
            return;
        }
        String param = "";
        if (type.isParameterized()) {
            ResourceLocation sel = this.paramSelector.getSelected();
            if (sel == null) {
                return;
            }
            param = sel.toString();
        }
        String id = this.idPrefix + Long.toHexString(System.nanoTime()) + "_" + this.target.size();
        this.target.add(new Quest(id, type, param, this.parse(this.targetBox, 10), this.parse(this.pointsBox, 10)));
        this.refreshList();
    }

    private void loadForEdit(Quest q) {
        if (q == null) {
            return;
        }
        this.editing = q;
        this.targetBox.setValue(String.valueOf(q.getTarget()));
        this.pointsBox.setValue(String.valueOf(q.getPoints()));
        this.updateAddButtonLabel();
    }

    private void removeSelected() {
        Quest sel = this.editing != null ? this.editing : this.currentList.getSelected();
        if (sel == null) {
            return;
        }
        this.target.remove(sel);
        this.editing = null;
        this.currentList.clearSelection();
        this.updateAddButtonLabel();
        this.refreshList();
    }

    private void updateAddButtonLabel() {
        if (this.addButton == null) {
            return;
        }
        this.addButton.setMessage(this.editing != null ? Component.literal((String)"\u00a7eGuardar cambios") : Component.translatable((String)"fantasticpass.gui.add_quest").withStyle(ChatFormatting.GREEN));
    }

    private void refreshList() {
        this.currentList.setItems(new ArrayList<Quest>(this.target));
    }

    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean param;
        this.renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -535291361);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, -14408146);
        g.fill(this.leftPos, this.topPos + this.panelHeight - 1, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -12964334);
        g.renderOutline(this.leftPos, this.topPos, this.panelWidth, this.panelHeight, -10860002);
        g.drawString(this.font, "\u00a7d\u2726 \u00a7f" + this.heading.getString(), this.leftPos + 10, this.topPos + 6, 0xFFFFFF, false);
        int lx = this.leftPos + 12;
        int leftW = (this.panelWidth - 36) / 2;
        int rx = lx + leftW + 12;
        int numY = this.topPos + 132;
        g.drawString(this.font, "\u00a7f" + Component.translatable((String)"fantasticpass.gui.objective_type").getString(), lx, this.topPos + 25, 0xE0E0E0, false);
        boolean bl = param = this.selectedType != null && this.selectedType.isParameterized();
        if (param) {
            g.drawString(this.font, "\u00a7f" + Component.translatable((String)"fantasticpass.gui.objective_target").getString(), rx, this.topPos + 25, 0xE0E0E0, false);
        }
        g.drawString(this.font, "\u00a7f" + Component.translatable((String)"fantasticpass.gui.count").getString(), rx, numY - 11, 0xE0E0E0, false);
        g.drawString(this.font, "\u00a7f" + Component.translatable((String)"fantasticpass.gui.points_field").getString(), rx + 92, numY - 11, 0xE0E0E0, false);
        g.drawString(this.font, "\u00a7f" + Component.translatable((String)"fantasticpass.gui.current_list").getString(), rx, numY + 44, 0xE0E0E0, false);
        super.render(g, mouseX, mouseY, partialTick);
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

    private record Hint(int x, int y, int w, int h, List<Component> lines) {
    }
}

