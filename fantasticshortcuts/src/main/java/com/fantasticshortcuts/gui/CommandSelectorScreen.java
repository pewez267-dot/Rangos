package com.fantasticshortcuts.gui;

import com.fantasticshortcuts.gui.widget.ScrollSelector;
import com.fantasticshortcuts.util.CommandDiscovery;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Command picker opened from the editor. Reads the live command tree via
 * {@link CommandDiscovery} (the real server dispatcher as synced to this admin), with
 * real-time text search and an origin filter (All / Vanilla / Mod). Selecting a command
 * invokes the callback and returns to the parent editor screen.
 */
public final class CommandSelectorScreen extends Screen {

    private enum Origin {
        ALL("Todos"), VANILLA("Vanilla"), MOD("Mods");

        final String label;

        Origin(final String label) {
            this.label = label;
        }

        Origin next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private final Screen parent;
    private final Consumer<String> onSelect;
    private final List<CommandDiscovery.CommandInfo> commands;

    private int panelWidth;
    private int panelHeight;
    private int leftPos;
    private int topPos;
    private String search = "";
    private Origin origin = Origin.ALL;
    private EditBox searchBox;

    public CommandSelectorScreen(final Screen parent, final Consumer<String> onSelect) {
        super(Component.literal("Seleccionar comando"));
        this.parent = parent;
        this.onSelect = onSelect;
        this.commands = CommandDiscovery.discover();
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(this.width - 16, 540);
        this.panelHeight = Math.min(this.height - 16, 320);
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;

        final int x = this.leftPos + 8;
        final int w = this.panelWidth - 16;

        this.searchBox = new EditBox(this.font, x, this.topPos + 24, w - 80, 16, Component.empty());
        this.searchBox.setHint(Component.literal("Buscar comando..."));
        this.searchBox.setValue(this.search);
        addRenderableWidget(this.searchBox);

        addRenderableWidget(Button.builder(Component.literal("§b" + this.origin.label), b -> {
            this.origin = this.origin.next();
            rebuildWidgets();
        }).bounds(x + w - 76, this.topPos + 24, 76, 16).build());

        final ScrollSelector<CommandDiscovery.CommandInfo> list = new ScrollSelector<>(
                x, this.topPos + 44, w, this.panelHeight - 44 - 28, 14,
                info -> "§f/" + info.name() + " §8(" + info.sourceLabel() + ", ns: " + info.namespace() + ")",
                CommandDiscovery.CommandInfo::name);
        list.setItems(filtered());
        list.setQuery(this.search);
        list.onSelect(info -> {
            if (this.onSelect != null) {
                this.onSelect.accept(info.name());
            }
            onClose();
        });
        addRenderableWidget(list);
        this.searchBox.setResponder(value -> {
            this.search = value;
            list.setQuery(value);
        });

        addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> onClose())
                .bounds(x + w - 80, this.topPos + this.panelHeight - 24, 80, 18).build());
    }

    private List<CommandDiscovery.CommandInfo> filtered() {
        final List<CommandDiscovery.CommandInfo> out = new ArrayList<>();
        for (final CommandDiscovery.CommandInfo info : this.commands) {
            final boolean keep = switch (this.origin) {
                case ALL -> true;
                case VANILLA -> info.vanilla();
                case MOD -> !info.vanilla();
            };
            if (keep) {
                out.add(info);
            }
        }
        return out;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -535291870);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, -14408646);
        g.fill(this.leftPos, this.topPos + this.panelHeight - 1, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -12961206);
        g.drawString(this.font, "§d\u2726 §fSelecciona el comando original", this.leftPos + 8, this.topPos + 6, 16777215, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.parent != null) {
            this.minecraft.setScreen(this.parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
