package com.fsrecipes.client.screen;

import com.fsrecipes.client.ClientHooks;
import com.fsrecipes.client.RegistryLists;
import com.fsrecipes.client.Sfx;
import com.fsrecipes.client.widget.ScrollSelector;
import com.fsrecipes.network.BulkBanPacket;
import com.fsrecipes.network.Net;
import com.fsrecipes.network.ToggleBanPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI para banear/desbanear recetas. Izquierda: TODOS los items del juego (o tu inventario) con
 * buscador y botones por categoria; clic en un item alterna su baneo. Derecha: lista de items
 * baneados; clic para desbanear. Estilo panel Fantastic.
 */
public final class RecipeBanScreen extends Screen {

    private boolean fromInventory = false;
    private final List<Label> labels = new ArrayList<>();
    private int catalogCount = 0;
    private int gameItemTotal = 0;

    private int leftPos;
    private int topPos;
    private int panelW;
    private int panelH;

    public RecipeBanScreen() {
        super(Component.literal("Fantastic Recipes"));
    }

    public void onBansUpdated() {
        this.rebuildWidgets();
    }

    private boolean isBanned(Item item) {
        ResourceLocation id = RegistryLists.id(item);
        return id != null && ClientHooks.bans().contains(id);
    }

    private void toggle(Item item, boolean ban) {
        ResourceLocation id = RegistryLists.id(item);
        if (id == null) {
            return;
        }
        // UI optimista: reflejamos el cambio ya; el servidor confirmara con SyncBansPacket.
        if (ban) {
            ClientHooks.bans().add(id);
        } else {
            ClientHooks.bans().remove(id);
        }
        Net.CHANNEL.sendToServer(new ToggleBanPacket(id, ban));
    }

    @Override
    protected void init() {
        this.panelW = Math.min(this.width - 16, 460);
        this.panelH = Math.min(this.height - 16, 300);
        this.leftPos = (this.width - this.panelW) / 2;
        this.topPos = (this.height - this.panelH) / 2;
        this.labels.clear();
        // Total de items del juego (vanilla + TODOS los mods instalados), sin contar el aire.
        this.gameItemTotal = RegistryLists.items().size();

        int x = bodyX();
        int y = bodyY();
        int colW = (bodyW() - 8) / 2;
        int rightX = x + colW + 8;

        // ---- Columna izquierda: catalogo de items ----
        this.addRenderableWidget(Button.builder(
                Component.literal(this.fromInventory ? "Fuente: \u00a7bInventario" : "Fuente: \u00a7eRegistro"), b -> {
                    this.fromInventory = !this.fromInventory;
                    Sfx.click();
                    this.rebuildWidgets();
                }).tooltip(Tooltip.create(Component.literal(
                        "Registro = todos los items del juego (todos los mods). Inventario = los items que tienes en la mano/mochila.")))
                .bounds(x, y, colW, 16).build());

        EditBox search = new EditBox(this.font, x, y + 18, colW, 16, Component.empty());
        search.setHint(Component.literal("Buscar item..."));
        this.addRenderableWidget(search);

        int listY = y + 38;
        int catRows = 76;
        int listH = bodyH() - 38 - catRows - 2;

        if (this.fromInventory) {
            List<ItemStack> inv = new ArrayList<>();
            Player p = this.minecraft != null ? this.minecraft.player : null;
            if (p != null) {
                for (ItemStack st : p.getInventory().items) {
                    if (st != null && !st.isEmpty()) {
                        inv.add(st.copy());
                    }
                }
            }
            ScrollSelector<ItemStack> list = new ScrollSelector<ItemStack>(x, listY, colW, listH, 18,
                    st -> st.getHoverName().getString(),
                    st -> st.getHoverName().getString() + " " + RegistryLists.itemId(st.getItem()),
                    st -> st)
                    .withCheckbox(st -> isBanned(st.getItem()))
                    .onSelect(st -> {
                        Item it = st.getItem();
                        boolean nowBan = !isBanned(it);
                        toggle(it, nowBan);
                        if (nowBan) Sfx.select(); else Sfx.click();
                        this.rebuildWidgets();
                    });
            list.setItems(inv);
            this.catalogCount = inv.size();
            search.setResponder(list::setQuery);
            this.addRenderableWidget(list);
            if (inv.isEmpty()) {
                addLabel("\u00a77Tu inventario esta vacio.", x + 2, listY + 4);
            }
        } else {
            ScrollSelector<Item> list = new ScrollSelector<Item>(x, listY, colW, listH, 18,
                    RegistryLists::itemName,
                    it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it),
                    it -> new ItemStack((ItemLike) it))
                    .withCheckbox(this::isBanned)
                    .onSelect(it -> {
                        boolean nowBan = !isBanned(it);
                        toggle(it, nowBan);
                        if (nowBan) Sfx.select(); else Sfx.click();
                        this.rebuildWidgets();
                    });
            List<Item> allItems = RegistryLists.items();
            list.setItems(allItems);
            this.catalogCount = allItems.size();
            search.setResponder(list::setQuery);
            this.addRenderableWidget(list);
        }

        // Botones de categoria (banear de golpe)
        int bw = colW / 3 - 2;
        int r1 = y + bodyH() - 72;
        int r2 = y + bodyH() - 54;
        int r3 = y + bodyH() - 36;
        int r4 = y + bodyH() - 18;
        addRenderableWidget(catButton("Bloques", x, r1, bw, CreativeModeTabs.BUILDING_BLOCKS));
        addRenderableWidget(catButton("Naturales", x + bw + 2, r1, bw, CreativeModeTabs.NATURAL_BLOCKS));
        addRenderableWidget(catButton("Funcional", x + 2 * (bw + 2), r1, bw, CreativeModeTabs.FUNCTIONAL_BLOCKS));
        addRenderableWidget(catButton("Combate", x, r2, bw, CreativeModeTabs.COMBAT));
        addRenderableWidget(catButton("Herram.", x + bw + 2, r2, bw, CreativeModeTabs.TOOLS_AND_UTILITIES));
        addRenderableWidget(catButton("Redstone", x + 2 * (bw + 2), r2, bw, CreativeModeTabs.REDSTONE_BLOCKS));
        addRenderableWidget(catButton("Comida", x, r3, bw, CreativeModeTabs.FOOD_AND_DRINKS));
        addRenderableWidget(catButton("Ingred.", x + bw + 2, r3, bw, CreativeModeTabs.INGREDIENTS));
        addRenderableWidget(catButton("Deco", x + 2 * (bw + 2), r3, bw, CreativeModeTabs.COLORED_BLOCKS));

        // ---- Columna derecha: baneados ----
        List<Item> bannedItems = new ArrayList<>();
        for (ResourceLocation id : ClientHooks.bans()) {
            Item it = ForgeRegistries.ITEMS.getValue(id);
            if (it != null) {
                bannedItems.add(it);
            }
        }
        bannedItems.sort((a, b) -> RegistryLists.itemId(a).compareTo(RegistryLists.itemId(b)));

        EditBox bannedSearch = new EditBox(this.font, rightX, y + 18, colW, 16, Component.empty());
        bannedSearch.setHint(Component.literal("Buscar baneado..."));
        this.addRenderableWidget(bannedSearch);

        ScrollSelector<Item> banned = new ScrollSelector<Item>(rightX, listY, colW, listH, 18,
                RegistryLists::itemName,
                it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it),
                it -> new ItemStack((ItemLike) it))
                .onSelect(it -> {
                    toggle(it, false);
                    Sfx.click();
                    this.rebuildWidgets();
                });
        banned.setItems(bannedItems);
        bannedSearch.setResponder(banned::setQuery);
        this.addRenderableWidget(banned);

        // Desbanear todo
        this.addRenderableWidget(Button.builder(Component.literal("\u00a7aDesbanear TODO"), b -> {
            Net.CHANNEL.sendToServer(new BulkBanPacket(new ArrayList<>(), false, true));
            ClientHooks.bans().clear();
            Sfx.success();
            this.rebuildWidgets();
        }).tooltip(Tooltip.create(Component.literal("Quita el baneo de TODAS las recetas.")))
                .bounds(rightX, y + bodyH() - 18, colW, 16).build());

        // ---- Footer ----
        this.addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> this.onClose())
                .bounds(this.leftPos + this.panelW - 88, this.topPos + this.panelH - 24, 80, 18).build());

        String fuente = this.fromInventory ? "inventario" : "todos los mods";
        addLabel("\u00a7eCatalogo \u00a7f" + this.catalogCount + " items \u00a77(" + fuente + ")", x + 2, y + bodyH() - 88);
        addLabel("\u00a7cBaneados \u00a77(clic = desbanear)", rightX + 2, y - 12);
    }

    private Button catButton(String label, int x, int y, int w, ResourceKey<CreativeModeTab> key) {
        return Button.builder(Component.literal(label), b -> {
            List<ResourceLocation> ids = new ArrayList<>();
            for (Item it : RegistryLists.itemsOfTab(key)) {
                ResourceLocation id = RegistryLists.id(it);
                if (id != null) {
                    ids.add(id);
                    ClientHooks.bans().add(id);
                }
            }
            if (!ids.isEmpty()) {
                Net.CHANNEL.sendToServer(new BulkBanPacket(ids, true, false));
                Sfx.select();
            }
            this.rebuildWidgets();
        }).tooltip(Tooltip.create(Component.literal("Banea DE GOLPE todas las recetas de esta categoria.")))
                .bounds(x, y, w, 16).build();
    }

    private int bodyX() {
        return this.leftPos + 8;
    }

    private int bodyY() {
        return this.topPos + 40;
    }

    private int bodyW() {
        return this.panelW - 16;
    }

    private int bodyH() {
        return this.panelH - 40 - 28;
    }

    private void addLabel(String text, int x, int y) {
        this.labels.add(new Label(text, x, y));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelW, this.topPos + this.panelH, -535160294);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelW, this.topPos + 18, -14013910);
        g.fill(this.leftPos, this.topPos + this.panelH - 1, this.leftPos + this.panelW, this.topPos + this.panelH, -12961222);
        g.fill(this.leftPos + 6, this.topPos + 34, this.leftPos + this.panelW - 6, this.topPos + 35, -12961222);
        g.drawString(this.font, "\u00a76\u2726 Fantastic Recipes \u00a77- \u00a7f" + this.gameItemTotal
                        + " items \u00a77- \u00a7c" + ClientHooks.bans().size() + " baneadas",
                this.leftPos + 8, this.topPos + 5, 0xFFFFFF, false);
        super.render(g, mouseX, mouseY, partial);
        for (Label l : this.labels) {
            g.drawString(this.font, l.text, l.x, l.y, 0xE0E0E0, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Label(String text, int x, int y) {
    }
}
