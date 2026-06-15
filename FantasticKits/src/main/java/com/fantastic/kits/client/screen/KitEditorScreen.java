package com.fantastic.kits.client.screen;

import com.fantastic.kits.client.RegistryLists;
import com.fantastic.kits.client.widget.ScrollSelector;
import com.fantastic.kits.kits.Kit;
import com.fantastic.kits.network.FKNetwork;
import com.fantastic.kits.network.SaveKitPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Main kit editor. Same layout, tab grid, panel size and colour palette as
 * {@code CrateEditorScreen} so Fantastic Kits feels like part of the same
 * family of mods. Eight tabs are provided, mirroring the spec's required
 * categories of editable data.
 *
 * <p>The editor mutates a local {@link Kit} instance only. Changes are
 * persisted by clicking "Save", which sends a {@link SaveKitPacket} carrying
 * the kit's full NBT to the server, where the operator gate is re-checked.
 */
public class KitEditorScreen extends Screen {

    private static final String COLOR_CHARS = "f7e6cab9d5234180";

    private final Screen parent;
    private final Kit kit;
    private final List<KitListScreen.GroupView> groups;
    private final List<String> commandCatalogue;

    private Tab activeTab;
    private final List<Label> labels = new ArrayList<>();
    private final List<TooltipZone> tooltipZones = new ArrayList<>();
    private String helpLine = "";

    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;

    // Per-tab state that must survive rebuildWidgets().
    private int selectedItemIndex = -1;
    private int selectedCommandIndex = -1;

    public KitEditorScreen(Screen parent, Kit kit,
                           List<KitListScreen.GroupView> groups,
                           List<String> commandCatalogue) {
        super(Component.literal("Editor de Kit"));
        this.parent = parent;
        this.kit = kit;
        this.groups = groups == null ? new ArrayList<>() : groups;
        this.commandCatalogue = commandCatalogue == null ? new ArrayList<>() : commandCatalogue;
        this.activeTab = Tab.INFO;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(this.width - 16, 540);
        this.panelHeight = Math.min(this.height - 16, 320);
        this.leftPos = (this.width - this.panelWidth) / 2;
        this.topPos = (this.height - this.panelHeight) / 2;
        this.labels.clear();
        this.tooltipZones.clear();

        initHeader();
        initFooter();
        switch (activeTab) {
            case INFO -> initInfo();
            case GROUP -> initGroup();
            case ITEMS -> initItems();
            case NBT -> initNbt();
            case COMMANDS -> initCommands();
            case SECURITY -> initSecurity();
            case PREVIEW -> initPreview();
        }
    }

    private int bodyX() { return leftPos + 8; }
    private int bodyY() { return topPos + 62; }
    private int bodyW() { return panelWidth - 16; }
    private int bodyH() { return panelHeight - 62 - 28; }

    private void initHeader() {
        Tab[] tabs = Tab.values();
        int gap = 2;
        int tabW = (panelWidth - 16 - gap * (tabs.length - 1)) / tabs.length;
        int x = leftPos + 8;
        int y = topPos + 24;
        for (Tab tab : tabs) {
            boolean active = tab == activeTab;
            String text = (active ? "\u00A7f\u00A7l" : "\u00A77") + tab.label;
            this.addRenderableWidget(Button.builder(Component.literal(text), b -> {
                this.activeTab = tab;
                this.rebuildWidgets();
            }).bounds(x, y, tabW, 18).build());
            x += tabW + gap;
        }
    }

    private void initFooter() {
        int w = 150;
        this.addRenderableWidget(Button.builder(Component.literal("\u00A7aGuardar y sincronizar"), b -> {
            FKNetwork.sendToServer(new SaveKitPacket(kit.save()));
            this.onClose();
        }).bounds(leftPos + panelWidth - w - 8, topPos + panelHeight - 24, w, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> this.onClose())
                .bounds(leftPos + 8, topPos + panelHeight - 24, 80, 18).build());
    }

    @Override
    public void onClose() {
        if (parent != null) Minecraft.getInstance().setScreen(parent);
        else super.onClose();
    }

    // ------------------------------------------------------------------
    // INFO tab
    // ------------------------------------------------------------------

    private void initInfo() {
        helpLine = "Datos basicos del kit: id, nombre visible, descripcion y configuracion de seguridad.";
        int x = bodyX();
        int y = bodyY();

        EditBox id = new EditBox(this.font, x + 170, y, 200, 16, Component.empty());
        id.setMaxLength(48);
        id.setValue(kit.id());
        id.setResponder(s -> {
            // Re-build the kit through NBT so the id field is updated cleanly.
            net.minecraft.nbt.CompoundTag t = kit.save();
            t.putString("id", Kit.sanitizeId(s));
            applyNbt(t);
        });
        this.addRenderableWidget(id);
        addLabel("ID del kit:", x, y + 4, desc("Identificador unico (sin espacios).",
                "Se usa en /fkits get/edit/delete y en los nodos de LuckPerms."));

        EditBox name = new EditBox(this.font, x + 170, y + 24, 200, 16, Component.empty());
        name.setMaxLength(96);
        name.setValue(kit.displayName());
        name.setResponder(kit::displayName);
        this.addRenderableWidget(name);
        addLabel("Nombre visible:", x, y + 28,
                desc("Nombre que vera el jugador. Acepta codigos & y \u00A7."));

        EditBox desc = new EditBox(this.font, x + 170, y + 48, 200, 16, Component.empty());
        desc.setMaxLength(160);
        desc.setValue(kit.description());
        desc.setResponder(kit::description);
        this.addRenderableWidget(desc);
        addLabel("Descripcion:", x, y + 52, desc("Descripcion corta del kit."));

        addLabel("\u00A78Grupo: \u00A7b" + (kit.ownerGroup().isEmpty() ? "(sin asignar)" : kit.ownerGroup())
                + "   \u00A78Items: \u00A7f" + kit.contents().size()
                + "   \u00A78Comandos: \u00A7f" + kit.commands().size(),
                x, y + 84, null);
        addLabel("\u00A78Creado: \u00A77" + new java.util.Date(kit.createdAt()),
                x, y + 96, null);
        addLabel("\u00A78Editado: \u00A77" + new java.util.Date(kit.lastEdited()),
                x, y + 108, null);
    }

    private void applyNbt(net.minecraft.nbt.CompoundTag t) {
        Kit reloaded = Kit.load(t);
        kit.replaceContents(reloaded.contents());
        kit.replaceCommands(reloaded.commands());
        kit.displayName(reloaded.displayName());
        kit.description(reloaded.description());
        kit.ownerGroup(reloaded.ownerGroup());
        kit.icon(reloaded.icon());
        kit.customNbt(reloaded.customNbt());
        kit.security(reloaded.security());
    }

    // ------------------------------------------------------------------
    // GROUP tab
    // ------------------------------------------------------------------

    private void initGroup() {
        helpLine = "Solo jugadores cuyo GRUPO PRIMARIO sea exactamente el seleccionado podran reclamar/usar este kit.";
        int x = bodyX();
        int y = bodyY();
        int colW = bodyW();

        if (groups.isEmpty()) {
            addLabel("\u00A7eLuckPerms no esta instalado o no hay grupos cargados.", x, y, null);
            addLabel("\u00A77Instala LuckPerms-Forge para asignar grupos a este kit.", x, y + 12, null);
            return;
        }

        EditBox search = new EditBox(this.font, x, y, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar grupo..."));
        this.addRenderableWidget(search);

        ScrollSelector<KitListScreen.GroupView> picker = new ScrollSelector<>(x, y + 20, colW, bodyH() - 22, 16,
                g -> ((g.name().equalsIgnoreCase(kit.ownerGroup()) ? "\u00A7a\u2714 " : "\u00A7f")
                        + g.displayName() + " \u00A78(weight " + g.weight() + ")"),
                g -> g.name() + " " + g.displayName(),
                g -> ItemStack.EMPTY);
        picker.setItems(this.groups);
        picker.onSelect(g -> {
            kit.ownerGroup(g.name());
            this.rebuildWidgets();
        });
        search.setResponder(picker::setQuery);
        this.addRenderableWidget(picker);
    }

    // ------------------------------------------------------------------
    // ITEMS tab
    // ------------------------------------------------------------------

    private void initItems() {
        helpLine = "Izquierda: catalogo. Click en un item para anadirlo. Derecha: contenido del kit; click para seleccionar y editar.";
        int x = bodyX();
        int y = bodyY();
        int colW = (bodyW() - 8) / 2;
        int rightX = x + colW + 8;

        EditBox search = new EditBox(this.font, x, y, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar item..."));
        this.addRenderableWidget(search);
        ScrollSelector<Item> catalog = new ScrollSelector<>(x, y + 20, colW, bodyH() - 22, 18,
                RegistryLists::itemName,
                it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it),
                it -> new ItemStack(it));
        catalog.setItems(RegistryLists.items());
        catalog.onSelect(it -> {
            List<ItemStack> list = new ArrayList<>(kit.contents());
            list.add(new ItemStack(it));
            kit.replaceContents(list);
            this.selectedItemIndex = list.size() - 1;
            this.rebuildWidgets();
        });
        search.setResponder(catalog::setQuery);
        this.addRenderableWidget(catalog);

        // Right pane: kit contents.
        ScrollSelector<Integer> current = new ScrollSelector<>(rightX, y, colW, bodyH() - 70, 18,
                idx -> ((idx == this.selectedItemIndex ? "\u00A7e\u25B6 " : "\u00A7f")
                        + (idx + 1) + ". " + safeStackName(idx)),
                idx -> safeStackName(idx),
                idx -> idx >= 0 && idx < kit.contents().size() ? kit.contents().get(idx) : ItemStack.EMPTY);
        current.setItems(indexes(kit.contents().size()));
        current.onSelect(idx -> {
            this.selectedItemIndex = idx;
            this.rebuildWidgets();
        });
        this.addRenderableWidget(current);

        // Action buttons for the selected item.
        if (this.selectedItemIndex >= 0 && this.selectedItemIndex < kit.contents().size()) {
            int actionsY = y + bodyH() - 64;
            int btnW = (colW - 12) / 3;
            int slot = this.selectedItemIndex;

            this.addRenderableWidget(Button.builder(Component.literal("\u25B2 Subir"), b -> {
                if (slot > 0) {
                    List<ItemStack> list = new ArrayList<>(kit.contents());
                    java.util.Collections.swap(list, slot, slot - 1);
                    kit.replaceContents(list);
                    this.selectedItemIndex = slot - 1;
                    this.rebuildWidgets();
                }
            }).bounds(rightX, actionsY, btnW, 16).build());

            this.addRenderableWidget(Button.builder(Component.literal("\u25BC Bajar"), b -> {
                if (slot < kit.contents().size() - 1) {
                    List<ItemStack> list = new ArrayList<>(kit.contents());
                    java.util.Collections.swap(list, slot, slot + 1);
                    kit.replaceContents(list);
                    this.selectedItemIndex = slot + 1;
                    this.rebuildWidgets();
                }
            }).bounds(rightX + btnW + 6, actionsY, btnW, 16).build());

            this.addRenderableWidget(Button.builder(Component.literal("\u00A7c\u2716 Quitar"), b -> {
                List<ItemStack> list = new ArrayList<>(kit.contents());
                list.remove(slot);
                kit.replaceContents(list);
                this.selectedItemIndex = Math.min(slot, list.size() - 1);
                this.rebuildWidgets();
            }).bounds(rightX + 2 * (btnW + 6), actionsY, btnW, 16).build());

            // Cantidad + NBT editor + usar como icono.
            EditBox count = new EditBox(this.font, rightX + 70, actionsY + 22, 60, 16, Component.empty());
            count.setMaxLength(3);
            count.setValue(Integer.toString(kit.contents().get(slot).getCount()));
            count.setResponder(s -> {
                try {
                    int v = Math.max(1, Math.min(64, Integer.parseInt(s.trim())));
                    kit.contents().get(slot).setCount(v);
                } catch (NumberFormatException ignored) {}
            });
            this.addRenderableWidget(count);
            addLabel("Cantidad:", rightX, actionsY + 26, null);

            this.addRenderableWidget(Button.builder(Component.literal("\u00A7b\u270e Editar NBT"), b -> {
                Minecraft.getInstance().setScreen(new NbtEditorScreen(this, kit.contents().get(slot)));
            }).bounds(rightX + 140, actionsY + 22, 90, 16).build());

            this.addRenderableWidget(Button.builder(Component.literal("\u00A7eUsar como icono"), b -> {
                kit.icon(kit.contents().get(slot).copy());
                this.rebuildWidgets();
            }).bounds(rightX + 234, actionsY + 22, colW - 234, 16).build());
        } else {
            addLabel("\u00A78Selecciona un item arriba para reordenar, editar NBT o quitar.",
                    rightX, y + bodyH() - 64, null);
        }
    }

    private String safeStackName(int idx) {
        if (idx < 0 || idx >= kit.contents().size()) return "(vacio)";
        ItemStack s = kit.contents().get(idx);
        return s.getCount() + "x " + s.getHoverName().getString();
    }

    private static List<Integer> indexes(int n) {
        List<Integer> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(i);
        return out;
    }

    // ------------------------------------------------------------------
    // NBT tab - shortcut to NbtEditorScreen for the selected item
    // ------------------------------------------------------------------

    private void initNbt() {
        helpLine = "Atajo al editor NBT del item seleccionado en la pestana 'Items'.";
        int x = bodyX();
        int y = bodyY();
        if (this.selectedItemIndex < 0 || this.selectedItemIndex >= kit.contents().size()) {
            addLabel("\u00A7eNo hay item seleccionado. Vuelve a la pestana 'Items' y elige uno.",
                    x, y, null);
            return;
        }
        ItemStack target = kit.contents().get(this.selectedItemIndex);
        addLabel("Item: \u00A7f" + target.getHoverName().getString(), x, y, null);
        this.addRenderableWidget(Button.builder(Component.literal("\u00A7b\u270e Abrir editor NBT"),
                b -> Minecraft.getInstance().setScreen(new NbtEditorScreen(this, target)))
                .bounds(x, y + 18, 220, 18).build());
    }

    // ------------------------------------------------------------------
    // COMMANDS tab
    // ------------------------------------------------------------------

    private void initCommands() {
        helpLine = "Solo el grupo propietario podra usar estos comandos. Vista escaneada en vivo del servidor.";
        int x = bodyX();
        int y = bodyY();
        int colW = (bodyW() - 8) / 2;
        int rightX = x + colW + 8;

        EditBox search = new EditBox(this.font, x, y, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar comando..."));
        this.addRenderableWidget(search);

        // Filter out commands already attached.
        List<String> available = new ArrayList<>();
        List<String> attached = new ArrayList<>(kit.commands());
        for (String c : commandCatalogue) {
            if (!attached.stream().anyMatch(a -> a.equalsIgnoreCase(c))) available.add(c);
        }
        available.sort(Comparator.naturalOrder());

        ScrollSelector<String> catalog = new ScrollSelector<>(x, y + 20, colW, bodyH() - 22, 14,
                s -> "\u00A7f/" + s, s -> s, s -> ItemStack.EMPTY);
        catalog.setItems(available);
        catalog.onSelect(s -> {
            List<String> list = new ArrayList<>(kit.commands());
            if (!list.contains(s)) list.add(s);
            kit.replaceCommands(list);
            this.rebuildWidgets();
        });
        search.setResponder(catalog::setQuery);
        this.addRenderableWidget(catalog);

        ScrollSelector<Integer> current = new ScrollSelector<>(rightX, y, colW, bodyH() - 70, 14,
                idx -> ((idx == this.selectedCommandIndex ? "\u00A7e\u25B6 " : "\u00A7a/")
                        + (idx >= 0 && idx < kit.commands().size() ? kit.commands().get(idx) : "?")),
                idx -> idx >= 0 && idx < kit.commands().size() ? kit.commands().get(idx) : "",
                idx -> ItemStack.EMPTY);
        current.setItems(indexes(kit.commands().size()));
        current.onSelect(idx -> {
            this.selectedCommandIndex = idx;
            this.rebuildWidgets();
        });
        this.addRenderableWidget(current);

        // Add custom command field.
        EditBox custom = new EditBox(this.font, rightX, y + bodyH() - 64, colW - 80, 16, Component.empty());
        custom.setMaxLength(160);
        custom.setHint(Component.literal("/comando con {player} {kit}"));
        this.addRenderableWidget(custom);
        this.addRenderableWidget(Button.builder(Component.literal("\u00A7a+ Agregar"), b -> {
            String s = custom.getValue().trim();
            if (s.startsWith("/")) s = s.substring(1);
            if (!s.isEmpty()) {
                List<String> list = new ArrayList<>(kit.commands());
                list.add(s);
                kit.replaceCommands(list);
                this.rebuildWidgets();
            }
        }).bounds(rightX + colW - 76, y + bodyH() - 64, 76, 16).build());

        // Remove selected command.
        if (this.selectedCommandIndex >= 0 && this.selectedCommandIndex < kit.commands().size()) {
            int idx = this.selectedCommandIndex;
            this.addRenderableWidget(Button.builder(Component.literal("\u00A7c\u2716 Quitar seleccionado"), b -> {
                List<String> list = new ArrayList<>(kit.commands());
                list.remove(idx);
                kit.replaceCommands(list);
                this.selectedCommandIndex = -1;
                this.rebuildWidgets();
            }).bounds(rightX, y + bodyH() - 42, colW, 16).build());
        }
    }

    // ------------------------------------------------------------------
    // SECURITY tab
    // ------------------------------------------------------------------

    private void initSecurity() {
        helpLine = "Validaciones server-side. Recuerda que strictGroupMatching es OBLIGATORIO segun la spec.";
        int x = bodyX();
        int y = bodyY();
        int w = 320;

        addToggle(x, y, w, "Strict group matching (recomendado)",
                kit.security().strictGroupMatching, () -> {
                    kit.security().strictGroupMatching = !kit.security().strictGroupMatching;
                    this.rebuildWidgets();
                }, desc("Solo el GRUPO PRIMARIO exacto puede reclamar/usar el kit.",
                        "La spec exige que esto este activo."));

        addToggle(x, y + 22, w, "Bloquear si el inventario esta lleno",
                kit.security().blockOnFullInventory, () -> {
                    kit.security().blockOnFullInventory = !kit.security().blockOnFullInventory;
                    this.rebuildWidgets();
                }, desc("Evita perdida de items por inventario lleno."));

        addToggle(x, y + 44, w, "Bloquear contextos inseguros (muerto/espectador)",
                kit.security().blockUnsafeContexts, () -> {
                    kit.security().blockUnsafeContexts = !kit.security().blockUnsafeContexts;
                    this.rebuildWidgets();
                }, desc("Rechaza claims durante la muerte, modo espectador o portal."));

        addToggle(x, y + 66, w, "Validar sincronia de inventario en cada claim",
                kit.security().validateInventorySync, () -> {
                    kit.security().validateInventorySync = !kit.security().validateInventorySync;
                    this.rebuildWidgets();
                }, desc("Re-verifica el snapshot del servidor antes de entregar."));

        addToggle(x, y + 88, w, "Rechazar paquetes de cliente forjados",
                kit.security().rejectForgedClient, () -> {
                    kit.security().rejectForgedClient = !kit.security().rejectForgedClient;
                    this.rebuildWidgets();
                }, desc("Protege contra packet spoofing y banderas falsificadas."));
    }

    // ------------------------------------------------------------------
    // PREVIEW tab
    // ------------------------------------------------------------------

    private void initPreview() {
        helpLine = "Vista previa de los items y comandos exactos que recibira el jugador.";
    }

    private void renderPreviewBody(GuiGraphics g) {
        int x = bodyX();
        int y = bodyY();
        int slotsPerRow = Math.max(1, bodyW() / 20);
        int slotX = x;
        int slotY = y;
        for (int i = 0; i < kit.contents().size(); i++) {
            ItemStack stack = kit.contents().get(i);
            g.fill(slotX, slotY, slotX + 18, slotY + 18, 0x60000000);
            g.renderItem(stack, slotX + 1, slotY + 1);
            g.renderItemDecorations(this.font, stack, slotX + 1, slotY + 1);
            slotX += 20;
            if ((i + 1) % slotsPerRow == 0) {
                slotX = x;
                slotY += 20;
            }
        }
        int line = slotY + 28;
        g.drawString(this.font, "\u00A77Comandos:", x, line, 0xCFD8DC, false);
        line += 10;
        for (String c : kit.commands()) {
            g.drawString(this.font, "\u00A7a/" + c, x + 8, line, 0xA0E0A0, false);
            line += 10;
            if (line > topPos + panelHeight - 32) break;
        }
    }

    // ------------------------------------------------------------------
    // Render
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);

        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + panelHeight, 0xE01E1E22);
        g.fill(leftPos, topPos, leftPos + panelWidth, topPos + 20, 0xFF24242E);
        g.fill(leftPos, topPos + panelHeight - 1, leftPos + panelWidth, topPos + panelHeight, 0xFF3C3C4A);
        g.fill(leftPos + 6, topPos + 46, leftPos + panelWidth - 6, topPos + 47, 0xFF3C3C4A);

        g.drawString(this.font,
                "\u00A7d\u2726 \u00A7fFantastic Kits \u00A7d\u2726 \u00A77- \u00A7e" + kit.displayName()
                        + " \u00A78(" + kit.id() + ")",
                leftPos + 8, topPos + 6, 0xFFFFFF, false);

        if (helpLine != null && !helpLine.isEmpty()) {
            String trimmed = this.font.plainSubstrByWidth("\u00A77" + helpLine, panelWidth - 16);
            g.drawString(this.font, trimmed, leftPos + 8, topPos + 50, 0x9AA8B0, false);
        }

        super.render(g, mouseX, mouseY, partial);

        for (Label l : labels) {
            g.drawString(this.font, l.text(), l.x(), l.y(), l.color(), false);
        }

        if (activeTab == Tab.PREVIEW) {
            renderPreviewBody(g);
        }

        for (TooltipZone z : tooltipZones) {
            if (mouseX >= z.x() && mouseX < z.x() + z.w() && mouseY >= z.y() && mouseY < z.y() + z.h()) {
                g.renderComponentTooltip(this.font, z.lines(), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ------------------------------------------------------------------
    // Helpers (mirroring CrateEditorScreen)
    // ------------------------------------------------------------------

    private static List<Component> desc(String... lines) {
        List<Component> out = new ArrayList<>();
        for (String s : lines) out.add(Component.literal(s));
        return out;
    }

    private void addLabel(String text, int x, int y, List<Component> tooltip) {
        labels.add(new Label(text, x, y, 0xE0E0E0));
        if (tooltip != null) {
            tooltipZones.add(new TooltipZone(x, y - 2,
                    Math.max(200, this.font.width(text) + 8), 14, tooltip));
        }
    }

    private void addToggle(int x, int y, int w, String text, boolean state,
                           Runnable onToggle, List<Component> tooltip) {
        String prefix = state ? "\u00A7a" : "\u00A77";
        String marker = state ? "\u2714 " : "\u2716 ";
        this.addRenderableWidget(Button.builder(Component.literal(prefix + marker + text),
                b -> onToggle.run()).bounds(x, y, w, 16).build());
        if (tooltip != null) tooltipZones.add(new TooltipZone(x, y, w, 16, tooltip));
    }

    private void addIntField(int x, int y, int w, int value, IntConsumer setter,
                             String label, int labelX, int labelY, List<Component> tooltip) {
        EditBox box = new EditBox(this.font, x, y, w, 16, Component.empty());
        box.setMaxLength(10);
        box.setValue(Integer.toString(value));
        box.setResponder(s -> {
            try { setter.accept(Integer.parseInt(s.trim())); }
            catch (NumberFormatException ignored) {}
        });
        this.addRenderableWidget(box);
        if (label != null) {
            labels.add(new Label(label, labelX, labelY, 0xE0E0E0));
            if (tooltip != null) tooltipZones.add(new TooltipZone(labelX, labelY - 2,
                    x + w - labelX, 14, tooltip));
        }
    }

    private static String fmt(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    // ------------------------------------------------------------------
    // Tab + label DTOs
    // ------------------------------------------------------------------

    private enum Tab {
        INFO("Info"),
        GROUP("Grupo"),
        ITEMS("Items"),
        NBT("NBT"),
        COMMANDS("Comandos"),
        SECURITY("Seguridad"),
        PREVIEW("Vista");

        final String label;
        Tab(String label) { this.label = label; }
    }

    record Label(String text, int x, int y, int color) {}
    record TooltipZone(int x, int y, int w, int h, List<Component> lines) {}
}
