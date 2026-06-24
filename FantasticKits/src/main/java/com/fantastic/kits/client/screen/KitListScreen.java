package com.fantastic.kits.client.screen;

import com.fantastic.kits.client.widget.ScrollSelector;
import com.fantastic.kits.kits.Kit;
import com.fantastic.kits.network.DeleteKitPacket;
import com.fantastic.kits.network.FKNetwork;
import com.fantastic.kits.network.TestKitPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Top-level kit catalogue screen. Same visual language as
 * {@code CrateEditorScreen}: centred dark panel with a coloured header, a
 * helper line, a {@link ScrollSelector} on the left for the kit list and a
 * "details" panel on the right that adapts to the active mode.
 *
 * <p>Modes:
 * <ul>
 *   <li>EDIT - clicking a kit opens {@link KitEditorScreen}.</li>
 *   <li>DELETE - shows a red confirm panel; "Confirm" sends a {@link DeleteKitPacket}.</li>
 *   <li>TEST - clicking a kit sends a {@link TestKitPacket}.</li>
 *   <li>CREATE - reuses EDIT layout but the helper line invites the operator to
 *       run {@code /fkits create} for a new kit. Existing kits can be cloned by
 *       opening them and renaming.</li>
 * </ul>
 */
public class KitListScreen extends Screen {

    public enum Mode {
        CREATE, EDIT, DELETE, TEST;
        public static Mode fromWire(int wire) {
            Mode[] v = values();
            return wire >= 0 && wire < v.length ? v[wire] : EDIT;
        }
    }

    /** Compact group projection shipped from the server through GuiPayload. */
    public record GroupView(String name, String displayName, int weight) {}

    private final Mode mode;
    private final List<Kit> kits;
    private final List<GroupView> groups;
    private final List<String> commandCatalogue;

    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;

    private ScrollSelector<Kit> kitList;
    private Kit selected;
    private EditBox search;

    public KitListScreen(Mode mode, List<Kit> kits, List<GroupView> groups, List<String> commands) {
        super(Component.literal("Fantastic Kits"));
        this.mode = mode;
        this.kits = new ArrayList<>(kits);
        this.groups = new ArrayList<>(groups);
        this.commandCatalogue = new ArrayList<>(commands);
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(this.width - 16, 540);
        this.panelHeight = Math.min(this.height - 16, 320);
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;

        int x = leftPos + 8;
        int y = topPos + 56;
        int colW = (panelWidth - 24) / 2;
        int rightX = x + colW + 8;
        int bodyH = panelHeight - 56 - 32;

        // Search bar above the list.
        this.search = new EditBox(this.font, x, y, colW, 16, Component.empty());
        this.search.setHint(Component.literal("Buscar kit por nombre, id o grupo..."));
        this.addRenderableWidget(this.search);

        this.kitList = new ScrollSelector<>(x, y + 20, colW, bodyH - 22, 18,
                k -> ((Objects.equals(k, selected) ? "\u00A7e\u25B6 " : "\u00A7f")
                        + k.displayName() + " \u00A78(\u00A7b" + k.ownerGroup() + "\u00A78)"),
                k -> k.id() + " " + k.displayName() + " " + k.ownerGroup(),
                k -> k.icon().isEmpty() ? new ItemStack(Items.CHEST) : k.icon());
        this.kitList.setItems(this.kits);
        this.kitList.onSelect(k -> {
            this.selected = k;
            this.rebuildWidgets();
        });
        this.search.setResponder(this.kitList::setQuery);
        this.addRenderableWidget(this.kitList);

        // Right panel: details / actions for the selected kit.
        renderRightPanel(rightX, y, colW, bodyH);

        // Footer.
        this.addRenderableWidget(Button.builder(Component.literal("Cerrar"),
                b -> this.onClose()).bounds(leftPos + 8, topPos + panelHeight - 24, 80, 18).build());
    }

    private void renderRightPanel(int rightX, int y, int colW, int bodyH) {
        if (this.selected == null) {
            return;
        }
        Kit k = this.selected;
        int line = y;

        // Mode-specific action bar at the bottom of the right column.
        switch (mode) {
            case CREATE, EDIT -> {
                this.addRenderableWidget(Button.builder(Component.literal("\u00A7a\u270e Editar este kit"),
                        b -> Minecraft.getInstance().setScreen(new KitEditorScreen(this, k, groups, commandCatalogue)))
                        .bounds(rightX, y + bodyH - 22, colW, 18).build());
            }
            case DELETE -> {
                this.addRenderableWidget(Button.builder(Component.literal("\u00A7c\u2716 Eliminar permanentemente"),
                        b -> {
                            FKNetwork.sendToServer(new DeleteKitPacket(k.id()));
                            this.selected = null;
                            this.kits.removeIf(x -> x.uuid().equals(k.uuid()));
                            this.kitList.setItems(this.kits);
                            this.rebuildWidgets();
                        })
                        .bounds(rightX, y + bodyH - 22, colW, 18).build());
            }
            case TEST -> {
                this.addRenderableWidget(Button.builder(Component.literal("\u00A7a\u25B6 Entrega de prueba"),
                        b -> {
                            FKNetwork.sendToServer(new TestKitPacket(k.id()));
                            Minecraft.getInstance().player.displayClientMessage(
                                    Component.literal("\u00A77Pidiendo entrega de prueba al servidor..."), false);
                        })
                        .bounds(rightX, y + bodyH - 22, colW, 18).build());
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);

        // Panel background with the same palette as FantasticCrates.
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + panelHeight, 0xE01E1E22);
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + 20, 0xFF24242E);
        g.fill(leftPos, topPos + panelHeight - 1, leftPos + panelWidth, topPos + panelHeight, 0xFF3C3C4A);
        g.fill(leftPos + 6, topPos + 46, leftPos + panelWidth - 6, topPos + 47, 0xFF3C3C4A);

        // Title.
        String modeTag = switch (mode) {
            case CREATE -> "\u00A7a Crear";
            case EDIT -> "\u00A7b Editar";
            case DELETE -> "\u00A7c Eliminar";
            case TEST -> "\u00A7e Probar";
        };
        g.drawString(this.font,
                "\u00A7d\u2726 \u00A7fFantastic Kits \u00A7d\u2726 \u00A77- " + modeTag
                        + " \u00A78(" + kits.size() + " kit" + (kits.size() == 1 ? "" : "s") + ")",
                leftPos + 8, topPos + 6, 0xFFFFFF, false);

        // Helper line.
        String help = switch (mode) {
            case CREATE -> "Selecciona un kit para clonarlo o usa /fkits create para uno vacio.";
            case EDIT -> "Click en un kit y pulsa 'Editar' para abrir el editor con todas las pestanas.";
            case DELETE -> "Click en un kit y pulsa 'Eliminar' para borrarlo. La accion es irreversible.";
            case TEST -> "Click en un kit y pulsa 'Entrega de prueba'. No registra reclamo.";
        };
        g.drawString(this.font, "\u00A77" + help, leftPos + 8, topPos + 50, 0x9AA8B0, false);

        super.render(g, mouseX, mouseY, partial);

        // Right-side preview when something is selected.
        if (selected != null) {
            int rx = leftPos + 8 + (panelWidth - 24) / 2 + 8;
            int ry = topPos + 56;
            renderKitDetails(g, rx, ry);
        } else {
            int rx = leftPos + 8 + (panelWidth - 24) / 2 + 8;
            int ry = topPos + 56;
            g.drawString(this.font, "\u00A78Selecciona un kit a la izquierda \u2190",
                    rx, ry + 8, 0x9AA8B0, false);
        }
    }

    private void renderKitDetails(GuiGraphics g, int rx, int ry) {
        Kit k = selected;
        int w = (panelWidth - 24) / 2;
        g.fill(rx, ry, rx + w, ry + 4, 0xFF3C3C4A);

        int line = ry + 10;
        g.drawString(this.font, "\u00A7e" + k.displayName(), rx, line, 0xFFFFFF, false); line += 12;
        g.drawString(this.font, "\u00A78id: \u00A7f" + k.id(), rx, line, 0xCFD8DC, false); line += 10;
        g.drawString(this.font, "\u00A78uuid: \u00A77" + k.uuid().toString().substring(0, 13) + "...",
                rx, line, 0x90A0AB, false); line += 14;

        g.drawString(this.font, "\u00A77Grupo propietario: \u00A7b" + (k.ownerGroup().isEmpty() ? "(no asignado)" : k.ownerGroup()),
                rx, line, 0xCFD8DC, false); line += 12;
        g.drawString(this.font, "\u00A77Items: \u00A7f" + k.contents().size(),
                rx, line, 0xCFD8DC, false); line += 10;
        g.drawString(this.font, "\u00A77Comandos: \u00A7f" + k.commands().size(),
                rx, line, 0xCFD8DC, false); line += 14;

        g.drawString(this.font, "\u00A78strictGroupMatching: \u00A7a" + k.security().strictGroupMatching,
                rx, line, 0x9AA8B0, false); line += 9;
        g.drawString(this.font, "\u00A78blockOnFullInventory: \u00A7a" + k.security().blockOnFullInventory,
                rx, line, 0x9AA8B0, false); line += 9;
        g.drawString(this.font, "\u00A78validateInventorySync: \u00A7a" + k.security().validateInventorySync,
                rx, line, 0x9AA8B0, false); line += 14;

        // Tiny preview strip of up to 9 items.
        int slotX = rx;
        int slotY = line;
        int max = Math.min(9, k.contents().size());
        for (int i = 0; i < max; i++) {
            ItemStack stack = k.contents().get(i);
            g.fill(slotX, slotY, slotX + 18, slotY + 18, 0x60000000);
            g.renderItem(stack, slotX + 1, slotY + 1);
            g.renderItemDecorations(this.font, stack, slotX + 1, slotY + 1);
            slotX += 20;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
