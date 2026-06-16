package com.fantasticshortcuts.gui;

import com.fantasticshortcuts.data.Shortcut;
import com.fantasticshortcuts.gui.widget.ScrollSelector;
import com.fantasticshortcuts.network.DeleteShortcutPacket;
import com.fantasticshortcuts.network.FSNetwork;
import com.fantasticshortcuts.network.RequestOpenEditorPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Main shortcut-management screen: searchable, scrollable list of shortcuts with Create /
 * Edit / Delete (two-step confirm) actions. Built on {@link AbstractContainerMenu} /
 * {@code MenuType} via {@link AbstractContainerScreen} and styled like the Fantastic family.
 */
public final class ShortcutsScreen extends AbstractContainerScreen<ShortcutsMenu> {

    private final List<Shortcut> shortcuts;
    private String search = "";
    private Shortcut selected;
    private boolean confirmDelete;
    private EditBox searchBox;

    public ShortcutsScreen(final ShortcutsMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = 320;
        this.imageHeight = 230;
        this.shortcuts = new ArrayList<>(menu.getShortcuts());
    }

    @Override
    protected void init() {
        // Match the FantasticCrates / FantasticSpawners family panel size (adaptive, max 540x320).
        this.imageWidth = Math.min(this.width - 16, 540);
        this.imageHeight = Math.min(this.height - 16, 320);
        super.init();
        final int x = this.leftPos + 8;
        final int w = this.imageWidth - 16;

        this.searchBox = new EditBox(this.font, x, this.topPos + 22, w, 16, Component.empty());
        this.searchBox.setHint(Component.literal("Buscar por alias, comando o nombre..."));
        this.searchBox.setValue(this.search);
        addRenderableWidget(this.searchBox);

        final int listH = this.imageHeight - 42 - 52;
        final ScrollSelector<Shortcut> list = new ScrollSelector<>(x, this.topPos + 42, w, listH, 16,
                s -> (s == this.selected ? "§e\u25b6 " : "§f") + "/" + s.aliasKey()
                        + " §7\u2192 §f" + s.getOriginalCommand()
                        + (s.isReplaceOriginal() ? " §a[reemplaza]" : "")
                        + (s.getName().isBlank() ? "" : " §8(" + s.getName() + ")"),
                s -> s.aliasKey() + " " + s.getOriginalCommand() + " " + s.getName());
        list.setItems(this.shortcuts);
        list.setQuery(this.search);
        list.onSelect(s -> {
            this.selected = s;
            this.confirmDelete = false;
            rebuildWidgets();
        });
        addRenderableWidget(list);
        this.searchBox.setResponder(value -> {
            this.search = value;
            list.setQuery(value);
        });

        final int by = this.topPos + this.imageHeight - 24;
        final int bw = (w - 9) / 4;
        addRenderableWidget(Button.builder(Component.literal("§aCrear"), b ->
                FSNetwork.sendToServer(new RequestOpenEditorPacket(""))).bounds(x, by, bw, 18).build());

        final Button edit = Button.builder(Component.literal("Editar"), b -> {
            if (this.selected != null) {
                FSNetwork.sendToServer(new RequestOpenEditorPacket(this.selected.getId()));
            }
        }).bounds(x + bw + 3, by, bw, 18).build();
        edit.active = this.selected != null;
        addRenderableWidget(edit);

        final Button delete = Button.builder(Component.literal(this.confirmDelete ? "§c¿Confirmar?" : "Eliminar"), b -> {
            if (this.selected == null) {
                return;
            }
            if (!this.confirmDelete) {
                this.confirmDelete = true;
                rebuildWidgets();
            } else {
                FSNetwork.sendToServer(new DeleteShortcutPacket(this.selected.getId()));
            }
        }).bounds(x + 2 * (bw + 3), by, bw, 18).build();
        delete.active = this.selected != null;
        addRenderableWidget(delete);

        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(x + 3 * (bw + 3), by, bw, 18).build());
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -535291870);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 20, -14408646);
        g.fill(this.leftPos, this.topPos + this.imageHeight - 1, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -12961206);
        g.drawString(this.font, "§d\u2726 §fFantastic Shortcuts §d\u2726 §7- " + this.shortcuts.size() + " shortcut(s)",
                this.leftPos + 8, this.topPos + 6, 16777215, false);
        final String detail = this.selected == null
                ? "§7Selecciona un shortcut de la lista."
                : "§7Sel: §f/" + this.selected.aliasKey() + " §7\u2192 §f" + this.selected.getOriginalCommand();
        g.drawString(this.font, this.font.plainSubstrByWidth(detail, this.imageWidth - 16),
                this.leftPos + 8, this.topPos + this.imageHeight - 38, 10133680, false);
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Suppress the default container title / inventory labels; all text drawn in renderBg.
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        if (this.searchBox != null && this.searchBox.isFocused() && this.searchBox.canConsumeInput()) {
            this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
