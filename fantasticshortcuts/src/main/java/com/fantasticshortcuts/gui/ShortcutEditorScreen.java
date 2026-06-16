package com.fantasticshortcuts.gui;

import com.fantasticshortcuts.config.ShortcutsConfig;
import com.fantasticshortcuts.data.Shortcut;
import com.fantasticshortcuts.network.FSNetwork;
import com.fantasticshortcuts.network.RequestOpenMainPacket;
import com.fantasticshortcuts.network.SaveShortcutPacket;
import com.fantasticshortcuts.util.CommandDiscovery;
import com.fantasticshortcuts.util.ConflictChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.HashMap;
import java.util.Map;

/**
 * Create/edit screen, a single {@link AbstractContainerScreen} with tab navigation
 * (General, Comando Original, Shortcut, Replace) in the Fantastic family style. Edits a
 * local working copy; conflicts are shown live and the result is validated again server-side.
 */
public final class ShortcutEditorScreen extends AbstractContainerScreen<ShortcutEditorMenu> {

    private enum Tab {
        GENERAL("General"), COMANDO("Comando original"), ALIAS("Shortcut"), REPLACE("Replace");

        final String label;

        Tab(final String label) {
            this.label = label;
        }
    }

    private final Shortcut working;
    private final Map<String, String> otherAliases = new HashMap<>();
    private Tab activeTab = Tab.GENERAL;

    public ShortcutEditorScreen(final ShortcutEditorMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = 320;
        this.imageHeight = 230;
        this.working = menu.getShortcut().copy();
        for (final Shortcut s : menu.getOthers()) {
            this.otherAliases.put(s.aliasKey(), s.getName().isBlank() ? ("/" + s.aliasKey()) : s.getName());
        }
    }

    @Override
    protected void init() {
        super.init();
        final int x = this.leftPos + 8;
        final int w = this.imageWidth - 16;

        // Tab row.
        final Tab[] tabs = Tab.values();
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

        final int bodyY = this.topPos + 48;
        switch (this.activeTab) {
            case GENERAL -> initGeneral(x, w, bodyY);
            case COMANDO -> initComando(x, w, bodyY);
            case ALIAS -> initAlias(x, w, bodyY);
            case REPLACE -> initReplace(x, w, bodyY);
        }

        // Footer.
        final int by = this.topPos + this.imageHeight - 24;
        addRenderableWidget(Button.builder(Component.literal("§aGuardar"), b ->
                FSNetwork.sendToServer(new SaveShortcutPacket(this.working))).bounds(x + w - 150, by, 70, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Cancelar"), b ->
                FSNetwork.sendToServer(new RequestOpenMainPacket())).bounds(x + w - 76, by, 76, 18).build());
    }

    private void initGeneral(final int x, final int w, final int y) {
        final EditBox name = new EditBox(this.font, x, y + 12, w, 16, Component.empty());
        name.setMaxLength(64);
        name.setValue(this.working.getName());
        name.setHint(Component.literal("Nombre interno del shortcut"));
        name.setResponder(this.working::setName);
        addRenderableWidget(name);

        final EditBox desc = new EditBox(this.font, x, y + 48, w, 16, Component.empty());
        desc.setMaxLength(256);
        desc.setValue(this.working.getDescription());
        desc.setHint(Component.literal("Descripción"));
        desc.setResponder(this.working::setDescription);
        addRenderableWidget(desc);
    }

    private void initComando(final int x, final int w, final int y) {
        final EditBox original = new EditBox(this.font, x, y + 12, w, 16, Component.empty());
        original.setMaxLength(256);
        original.setValue(this.working.getOriginalCommand());
        original.setHint(Component.literal("/gamemode creative   o   /teleport {args}"));
        original.setResponder(this.working::setOriginalCommand);
        addRenderableWidget(original);

        addRenderableWidget(Button.builder(Component.literal("§bSeleccionar comando del servidor..."), b ->
                this.minecraft.setScreen(new CommandSelectorScreen(this, cmd -> {
                    // Selecting sets the base command; the admin can refine it (sub-args / {args}).
                    this.working.setOriginalCommand("/" + cmd);
                }))).bounds(x, y + 34, w, 18).build());
    }

    private void initAlias(final int x, final int w, final int y) {
        final EditBox alias = new EditBox(this.font, x, y + 12, w, 16, Component.empty());
        alias.setMaxLength(48);
        alias.setValue(this.working.getAlias());
        alias.setHint(Component.literal("/gc"));
        alias.setResponder(this.working::setAlias);
        addRenderableWidget(alias);
    }

    private void initReplace(final int x, final int w, final int y) {
        addRenderableWidget(Button.builder(
                Component.literal((this.working.isReplaceOriginal() ? "§a" : "§7")
                        + "Reemplazar comando original: " + (this.working.isReplaceOriginal() ? "Sí" : "No")),
                b -> {
                    this.working.setReplaceOriginal(!this.working.isReplaceOriginal());
                    rebuildWidgets();
                }).bounds(x, y + 12, w, 18).build());
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -535291870);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 18, -14408646);
        g.fill(this.leftPos, this.topPos + this.imageHeight - 1, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -12961206);
        g.drawString(this.font, "§d\u2726 §fEditor de Shortcut", this.leftPos + 8, this.topPos + 5, 16777215, false);

        final int x = this.leftPos + 8;
        final int y = this.topPos + 48;
        switch (this.activeTab) {
            case GENERAL -> {
                g.drawString(this.font, "§7Nombre interno:", x, y, 10133680, false);
                g.drawString(this.font, "§7Descripción:", x, y + 36, 10133680, false);
            }
            case COMANDO -> {
                g.drawString(this.font, "§7Comando original (admite {args}):", x, y, 10133680, false);
                final String root = this.working.originalRootLiteral();
                final String src = root.isEmpty() ? "—" : (CommandDiscovery.isVanilla(root) ? "Minecraft (vanilla)" : "Mod / otro");
                g.drawString(this.font, "§8Raíz: §7" + (root.isEmpty() ? "—" : root) + "  §8Fuente: §7" + src
                        + "  §8{args}: §7" + (this.working.usesArgs() ? "sí" : "no"), x, y + 56, 10133680, false);
            }
            case ALIAS -> {
                g.drawString(this.font, "§7Alias corto:", x, y, 10133680, false);
                g.drawString(this.font, "§8Pon {args} en el comando original para pasar argumentos del jugador.", x, y + 34, 10133680, false);
                renderConflict(g, x, y + 50);
            }
            case REPLACE -> {
                g.drawString(this.font, "§7Cuando está activo, el comando original desaparece del", x, y + 34, 10133680, false);
                g.drawString(this.font, "§7autocompletado del jugador y solo se ve el alias en su lugar.", x, y + 46, 10133680, false);
                g.drawString(this.font, "§8(El servidor mantiene el comando original internamente.)", x, y + 62, 10133680, false);
            }
        }
    }

    private void renderConflict(final GuiGraphics g, final int x, final int y) {
        if (!ShortcutsConfig.showConflictWarnings() || this.working.getAlias().isBlank()) {
            return;
        }
        final ConflictChecker.Result result = ConflictChecker.check(this.working.getAlias(), this.otherAliases,
                name -> {
                    final var connection = Minecraft.getInstance().getConnection();
                    return connection != null && connection.getCommands().getRoot().getChild(name) != null;
                });
        final String color = switch (result.severity()) {
            case ERROR -> "§c";
            case WARNING -> "§e";
            case OK -> "§a";
        };
        g.drawString(this.font, this.font.plainSubstrByWidth(color + result.message(), this.imageWidth - 16), x, y, 16777215, false);
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Suppress default labels.
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (keyCode == 256) {
            FSNetwork.sendToServer(new RequestOpenMainPacket());
            return true;
        }
        if (getFocused() instanceof EditBox box && box.canConsumeInput()) {
            box.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
