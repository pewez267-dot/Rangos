package com.fantastickits.gui;

import com.fantastickits.data.GroupCommandStore;
import com.fantastickits.data.Kit;
import com.fantastickits.gui.widget.ScrollSelector;
import com.fantastickits.network.FKNetwork;
import com.fantastickits.network.SaveKitPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntConsumer;

/**
 * In-game kit editor, a single {@link Screen} with tab navigation, styled to match the
 * FantasticCrates / FantasticSpawners family (centred dark panel, header bar, tab row,
 * footer actions, hover tooltips). No GUI state lives on the server: the screen edits a
 * client-side {@link Kit} copy and only the final result is sent back for validation.
 */
public final class KitEditorScreen extends Screen {

    private static final int CLIENT_ITEM_CAP = 54;

    private final Kit kit;
    private final List<String> groups;
    private final List<String> assignedCommands;

    private Tab activeTab = Tab.INFO;
    private final List<Label> labels = new ArrayList<>();
    private final List<TooltipZone> tooltipZones = new ArrayList<>();
    private String helpLine = "";

    private int leftPos;
    private int topPos;
    private int panelWidth;
    private int panelHeight;

    // Cross-rebuild UI state.
    private ItemStack selectedItem;
    private String itemSearch = "";
    private String commandSearch = "";
    private CommandOrigin commandFilter = CommandOrigin.ALL;
    private List<String> serverCommands;

    public KitEditorScreen(final Kit kit, final List<String> groups, final List<String> assignedCommands) {
        super(Component.literal("Editor de Kits"));
        this.kit = kit == null ? new Kit() : kit;
        this.groups = groups == null ? new ArrayList<>() : new ArrayList<>(groups);
        this.assignedCommands = assignedCommands == null ? new ArrayList<>() : new ArrayList<>(assignedCommands);
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
        switch (this.activeTab) {
            case INFO -> initInfo();
            case GROUP -> initGroup();
            case ITEMS -> initItems();
            case COMMANDS -> initCommands();
        }
    }

    private int bodyX() {
        return this.leftPos + 8;
    }

    private int bodyY() {
        return this.topPos + 62;
    }

    private int bodyW() {
        return this.panelWidth - 16;
    }

    private int bodyH() {
        return this.panelHeight - 62 - 28;
    }

    private void initHeader() {
        final Tab[] tabs = Tab.values();
        final int gap = 2;
        final int tabW = (this.panelWidth - 16 - gap * (tabs.length - 1)) / tabs.length;
        int x = this.leftPos + 8;
        final int y = this.topPos + 24;
        for (final Tab tab : tabs) {
            final boolean active = tab == this.activeTab;
            final String text = (active ? "§f§l" : "§7") + tab.label;
            final int bx = x;
            addRenderableWidget(Button.builder(Component.literal(text), b -> {
                this.activeTab = tab;
                rebuildWidgets();
            }).bounds(bx, y, tabW, 18).build());
            x += tabW + gap;
        }
    }

    private void initFooter() {
        final int w = 150;
        addRenderableWidget(Button.builder(Component.literal("§aGuardar kit"), b -> {
            FKNetwork.sendToServer(new SaveKitPacket(this.kit.toNbt(), new ArrayList<>(this.assignedCommands)));
            onClose();
        }).bounds(this.leftPos + this.panelWidth - w - 8, this.topPos + this.panelHeight - 24, w, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(this.leftPos + 8, this.topPos + this.panelHeight - 24, 80, 18).build());
    }

    // ---- INFO ---------------------------------------------------------------

    private void initInfo() {
        this.helpLine = "Datos basicos del kit: identificador y nombre visible (acepta codigos & de color).";
        final int x = bodyX();
        final int y = bodyY();

        final EditBox id = new EditBox(this.font, x + 170, y, 200, 16, Component.empty());
        id.setMaxLength(48);
        id.setValue(this.kit.id);
        id.setResponder(s -> this.kit.id = Kit.normalizeId(s));
        addRenderableWidget(id);
        addLabel("ID del kit:", x, y + 4, desc("Identificador unico, sin espacios.", "Se usa en /fkits get, edit, delete.", "Ej: vip, ultra, elite"));

        final EditBox name = new EditBox(this.font, x + 170, y + 24, 200, 16, Component.empty());
        name.setMaxLength(128);
        name.setValue(this.kit.displayName);
        name.setResponder(s -> this.kit.displayName = s);
        addRenderableWidget(name);
        addLabel("Nombre visible:", x, y + 28, desc("Nombre mostrado al reclamar. Acepta codigos & o §."));

        addLabel("§8Grupo asignado: " + (this.kit.hasGroup() ? "§a" + this.kit.group : "§cninguno"), x, y + 56, null);
        addLabel("§8Items en el kit: §f" + this.kit.items.size(), x, y + 70, null);
        addLabel("§8Comandos del grupo: §f" + this.assignedCommands.size(), x, y + 84, null);
        addLabel("§7Configura el grupo, los items y los comandos en sus pestanas.", x, y + 104, null);
    }

    // ---- GROUP --------------------------------------------------------------

    private void initGroup() {
        this.helpLine = "Asigna exactamente UN grupo de LuckPerms. Solo sus miembros podran reclamar este kit.";
        final int x = bodyX();
        final int y = bodyY();
        final int colW = (bodyW() - 10) / 2;
        final int rightX = x + colW + 10;

        final ScrollSelector<String> list = new ScrollSelector<>(x, y, colW, bodyH() - 2, 14,
                g -> (g.equalsIgnoreCase(this.kit.group) ? "§a\u2714 " : "§f") + g,
                g -> g,
                g -> ItemStack.EMPTY);
        list.setItems(this.groups);
        list.onSelect(g -> {
            this.kit.group = g;
            rebuildWidgets();
        });
        addRenderableWidget(list);

        addLabel("§eGrupos de LuckPerms (en vivo):", x, y - 12, null);

        if (this.groups.isEmpty()) {
            addLabel("§7No se detectaron grupos.", rightX, y, null);
            addLabel("§7LuckPerms ausente o sin grupos.", rightX, y + 12, null);
            addLabel("§7Asigna el grupo manualmente:", rightX, y + 28, null);
        } else {
            addLabel("§7Grupo actual: " + (this.kit.hasGroup() ? "§a" + this.kit.group : "§cninguno"), rightX, y, null);
            addLabel("§7O escribelo manualmente:", rightX, y + 28, null);
        }

        final EditBox manual = new EditBox(this.font, rightX, y + 42, colW, 16, Component.empty());
        manual.setMaxLength(64);
        manual.setValue(this.kit.group);
        manual.setHint(Component.literal("nombre del grupo"));
        manual.setResponder(s -> this.kit.group = s.trim());
        addRenderableWidget(manual);

        addRenderableWidget(Button.builder(Component.literal("§cQuitar grupo"), b -> {
            this.kit.group = "";
            rebuildWidgets();
        }).bounds(rightX, y + 64, colW, 16).build());
    }

    // ---- ITEMS --------------------------------------------------------------

    private void initItems() {
        this.helpLine = "Izquierda: busca y clic en un item para anadirlo. Derecha: lista del kit y editor del item seleccionado.";
        final int x = bodyX();
        final int y = bodyY();
        final int colW = (bodyW() - 8) / 2;
        final int rightX = x + colW + 8;

        // Left: item picker.
        final EditBox search = new EditBox(this.font, x, y, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar item..."));
        search.setValue(this.itemSearch);
        addRenderableWidget(search);

        final ScrollSelector<Item> picker = new ScrollSelector<>(x, y + 20, colW, bodyH() - 22, 18,
                item -> new ItemStack(item).getHoverName().getString(),
                item -> new ItemStack(item).getHoverName().getString() + " " + itemId(item),
                item -> new ItemStack(item));
        picker.setItems(allItems());
        picker.setQuery(this.itemSearch);
        picker.onSelect(item -> {
            if (this.kit.items.size() >= CLIENT_ITEM_CAP) {
                return;
            }
            final ItemStack stack = new ItemStack(item);
            this.kit.items.add(stack);
            this.selectedItem = stack;
            rebuildWidgets();
        });
        search.setResponder(s -> {
            this.itemSearch = s;
            picker.setQuery(s);
        });
        addRenderableWidget(picker);

        // Right: current kit items.
        if (this.selectedItem != null && !this.kit.items.contains(this.selectedItem)) {
            this.selectedItem = null;
        }
        final ScrollSelector<ItemStack> current = new ScrollSelector<>(rightX, y, colW, bodyH() - 66, 18,
                stack -> (stack == this.selectedItem ? "§e\u25b6 " : "§f") + stack.getHoverName().getString() + " §7x" + stack.getCount(),
                stack -> stack.getHoverName().getString(),
                stack -> stack);
        current.setItems(new ArrayList<>(this.kit.items));
        current.onSelect(stack -> {
            this.selectedItem = stack;
            rebuildWidgets();
        });
        addRenderableWidget(current);

        addLabel("§7Items: §f" + this.kit.items.size() + "§7/§f" + CLIENT_ITEM_CAP, rightX, y - 12, null);

        if (this.selectedItem != null) {
            final ItemStack stack = this.selectedItem;
            final int fy = y + bodyH() - 60;
            addIntField(rightX + 70, fy, 50, stack.getCount(), v -> stack.setCount(Math.max(1, Math.min(64, v))),
                    "Cantidad:", rightX, fy + 4, desc("Cantidad entregada de este item (1-64)."));
            addRenderableWidget(Button.builder(Component.literal("§b\u270e Editar NBT"), b -> {
                this.minecraft.setScreen(new ItemNbtEditorScreen(this, stack));
            }).bounds(rightX, fy + 24, colW - 70, 16).build());
            this.tooltipZones.add(new TooltipZone(rightX, fy + 24, colW - 70, 16,
                    desc("Nombre, lore, encantamientos, atributos,", "flags, CustomModelData, irrompible...", "todo desde la GUI, sin escribir JSON.")));
            addRenderableWidget(Button.builder(Component.literal("§cQuitar"), b -> {
                this.kit.items.remove(stack);
                this.selectedItem = null;
                rebuildWidgets();
            }).bounds(rightX + colW - 64, fy + 24, 64, 16).build());
        } else {
            addLabel("§7Selecciona un item de la lista", rightX, y + bodyH() - 56, null);
            addLabel("§7para editar su cantidad y NBT.", rightX, y + bodyH() - 44, null);
        }
    }

    // ---- COMMANDS -----------------------------------------------------------

    private void initCommands() {
        this.helpLine = "Comandos asociados al grupo del kit. Solo los miembros de ese grupo podran usarlos.";
        final int x = bodyX();
        final int y = bodyY();
        final int colW = (bodyW() - 8) / 2;
        final int rightX = x + colW + 8;

        if (!this.kit.hasGroup()) {
            addLabel("§cAsigna primero un grupo en la pestana \"Grupo\".", x, y - 12, null);
        } else {
            addLabel("§eComandos para el grupo §a" + this.kit.group + "§e:", x, y - 12, null);
        }

        // Left: command picker with origin filter + search.
        final EditBox search = new EditBox(this.font, x, y, colW - 70, 16, Component.empty());
        search.setHint(Component.literal("Buscar comando..."));
        search.setValue(this.commandSearch);
        addRenderableWidget(search);

        addRenderableWidget(Button.builder(Component.literal("§b" + this.commandFilter.label), b -> {
            this.commandFilter = this.commandFilter.next();
            rebuildWidgets();
        }).bounds(x + colW - 66, y, 66, 16).build());
        this.tooltipZones.add(new TooltipZone(x + colW - 66, y, 66, 16,
                desc("Filtra por origen del comando:", "Todos / Vanilla / Mods.")));

        final ScrollSelector<String> picker = new ScrollSelector<>(x, y + 20, colW, bodyH() - 22, 13,
                command -> (isAssigned(command) ? "§a\u2714 " : "§f") + "/" + command + (isVanilla(command) ? " §8(vanilla)" : " §8(mod)"),
                command -> command,
                command -> ItemStack.EMPTY);
        picker.setItems(filteredServerCommands());
        picker.setQuery(this.commandSearch);
        picker.onSelect(command -> {
            toggleCommand(command);
            rebuildWidgets();
        });
        search.setResponder(s -> {
            this.commandSearch = s;
            picker.setQuery(s);
        });
        addRenderableWidget(picker);

        // Right: assigned commands.
        addLabel("§7Asignados (§f" + this.assignedCommands.size() + "§7):", rightX, y - 12, null);
        final ScrollSelector<String> assigned = new ScrollSelector<>(rightX, y, colW, bodyH() - 22, 13,
                command -> "§f/" + command,
                command -> command,
                command -> ItemStack.EMPTY);
        assigned.setItems(new ArrayList<>(this.assignedCommands));
        assigned.onSelect(command -> {
            this.assignedCommands.remove(command);
            rebuildWidgets();
        });
        addRenderableWidget(assigned);
        addLabel("§8Clic en un asignado para quitarlo.", rightX, y + bodyH() - 10, null);
    }

    private boolean isAssigned(final String command) {
        return this.assignedCommands.contains(GroupCommandStore.normalizeCommand(command));
    }

    private void toggleCommand(final String rawCommand) {
        final String command = GroupCommandStore.normalizeCommand(rawCommand);
        if (command.isEmpty()) {
            return;
        }
        if (!this.assignedCommands.remove(command)) {
            this.assignedCommands.add(command);
        }
    }

    private List<String> filteredServerCommands() {
        final List<String> out = new ArrayList<>();
        for (final String command : serverCommands()) {
            final boolean vanilla = isVanilla(command);
            if (this.commandFilter == CommandOrigin.ALL
                    || (this.commandFilter == CommandOrigin.VANILLA && vanilla)
                    || (this.commandFilter == CommandOrigin.OTHER && !vanilla)) {
                out.add(command);
            }
        }
        return out;
    }

    /** Reads the synced command tree (vanilla + Forge + every other mod) from the client. */
    private List<String> serverCommands() {
        if (this.serverCommands != null) {
            return this.serverCommands;
        }
        final Set<String> names = new TreeSet<>();
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            final CommandDispatcher<SharedSuggestionProvider> dispatcher = connection.getCommands();
            for (final CommandNode<SharedSuggestionProvider> node : dispatcher.getRoot().getChildren()) {
                if (node.getName() != null && !node.getName().isBlank()) {
                    names.add(node.getName().toLowerCase(Locale.ROOT));
                }
            }
        }
        this.serverCommands = new ArrayList<>(names);
        return this.serverCommands;
    }

    private static boolean isVanilla(final String command) {
        return VanillaCommands.NAMES.contains(GroupCommandStore.normalizeCommand(command));
    }

    private static List<Item> allItems() {
        final List<Item> list = new ArrayList<>(ForgeRegistries.ITEMS.getValues());
        list.sort(Comparator.comparing(KitEditorScreen::itemId));
        return list;
    }

    private static String itemId(final Item item) {
        final ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        return rl == null ? "minecraft:air" : rl.toString();
    }

    // ---- shared helpers (mirror the family's editor) ------------------------

    private static List<Component> desc(final String... lines) {
        final List<Component> out = new ArrayList<>();
        for (final String line : lines) {
            out.add(Component.literal(line));
        }
        return out;
    }

    private void addLabel(final String text, final int x, final int y, final List<Component> tooltip) {
        this.labels.add(new Label(text, x, y, 14737632));
        if (tooltip != null) {
            this.tooltipZones.add(new TooltipZone(x, y - 2, Math.max(200, this.font.width(text) + 8), 14, tooltip));
        }
    }

    private void addIntField(final int x, final int y, final int w, final int value, final IntConsumer setter,
                             final String label, final int labelX, final int labelY, final List<Component> tooltip) {
        final EditBox box = new EditBox(this.font, x, y, w, 16, Component.empty());
        box.setMaxLength(10);
        box.setValue(Integer.toString(value));
        box.setResponder(s -> {
            try {
                setter.accept(Integer.parseInt(s.trim()));
            } catch (final NumberFormatException ignored) {
            }
        });
        addRenderableWidget(box);
        if (label != null) {
            this.labels.add(new Label(label, labelX, labelY, 14737632));
            if (tooltip != null) {
                this.tooltipZones.add(new TooltipZone(labelX, labelY - 2, x + w - labelX, 14, tooltip));
            }
        }
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -535291870);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelWidth, this.topPos + 20, -14408646);
        g.fill(this.leftPos, this.topPos + this.panelHeight - 1, this.leftPos + this.panelWidth, this.topPos + this.panelHeight, -12961206);
        g.fill(this.leftPos + 6, this.topPos + 46, this.leftPos + this.panelWidth - 6, this.topPos + 47, -12961206);
        g.drawString(this.font, "§d\u2726 §fFantastic Kits §d\u2726 §7- editor de kits", this.leftPos + 8, this.topPos + 6, 16777215, false);
        if (this.helpLine != null && !this.helpLine.isEmpty()) {
            final String trimmed = this.font.plainSubstrByWidth("§7" + this.helpLine, this.panelWidth - 16);
            g.drawString(this.font, trimmed, this.leftPos + 8, this.topPos + 50, 10133680, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
        for (final Label l : this.labels) {
            g.drawString(this.font, l.text(), l.x(), l.y(), l.color(), false);
        }
        for (final TooltipZone z : this.tooltipZones) {
            if (mouseX >= z.x() && mouseX < z.x() + z.w() && mouseY >= z.y() && mouseY < z.y() + z.h()) {
                g.renderComponentTooltip(this.font, z.lines(), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Tab {
        INFO("Info"),
        GROUP("Grupo"),
        ITEMS("Items"),
        COMMANDS("Comandos");

        final String label;

        Tab(final String label) {
            this.label = label;
        }
    }

    private enum CommandOrigin {
        ALL("Todos"),
        VANILLA("Vanilla"),
        OTHER("Mods");

        final String label;

        CommandOrigin(final String label) {
            this.label = label;
        }

        CommandOrigin next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private record Label(String text, int x, int y, int color) {
    }

    private record TooltipZone(int x, int y, int w, int h, List<Component> lines) {
    }
}
