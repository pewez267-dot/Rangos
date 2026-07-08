/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fshop.client.Sfx
 *  com.fshop.client.screen.NbtEditorScreen
 *  com.fshop.economy.CoinEconomy
 *  com.fshop.network.CollectMainShopPacket
 *  com.fshop.network.PacketHandler
 *  com.fshop.shop.PlayerShop
 *  com.fshop.shop.ShopOffer
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.Tooltip
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.item.CreativeModeTab
 *  net.minecraft.world.item.CreativeModeTabs
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 */
package com.fshop.client.screen;

import com.fshop.client.RegistryLists;
import com.fshop.client.Sfx;
import com.fshop.client.screen.NbtEditorScreen;
import com.fshop.client.widget.ScrollSelector;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.CollectMainShopPacket;
import com.fshop.network.PacketHandler;
import com.fshop.network.SaveMainShopPacket;
import com.fshop.shop.PlayerShop;
import com.fshop.shop.ShopOffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class MainShopCreatorScreen
extends Screen {
    private final PlayerShop shop;
    private final List<ShopOffer> offers;
    private final long[] pending;
    private String name;
    private ItemStack icon;
    private Tab activeTab = Tab.ITEMS;
    private ShopOffer selected;
    // Fuente de items para las ofertas: false = registro (todos los items del juego),
    // true = inventario del jugador (para items con NBT unico: llaves de crates, cabezas, items custom).
    private boolean fromInventory = false;
    private final List<Label> labels = new ArrayList<Label>();
    private String help = "";
    private int leftPos;
    private int topPos;
    private int panelW;
    private int panelH;

    public MainShopCreatorScreen(PlayerShop shop) {
        super((Component)Component.literal((String)"Creador de tienda"));
        this.shop = shop;
        this.offers = new ArrayList<ShopOffer>(shop.getOffers());
        this.pending = new long[]{shop.getPendingEarnings(0), shop.getPendingEarnings(1), shop.getPendingEarnings(2)};
        this.name = shop.getName() == null || shop.getName().isBlank() ? "La Moneda de Oro" : shop.getName();
        this.icon = shop.getIcon().isEmpty() ? new ItemStack((ItemLike)Items.GOLD_INGOT) : shop.getIcon().copy();
    }

    protected void init() {
        this.panelW = Math.min(this.width - 16, 460);
        this.panelH = Math.min(this.height - 16, 300);
        this.leftPos = (this.width - this.panelW) / 2;
        this.topPos = (this.height - this.panelH) / 2;
        this.labels.clear();
        this.initHeader();
        this.initFooter();
        if (this.activeTab == Tab.ITEMS) {
            this.initItems();
        } else {
            this.initSettings();
        }
    }

    private int bodyX() {
        return this.leftPos + 8;
    }

    private int bodyY() {
        return this.topPos + 58;
    }

    private int bodyW() {
        return this.panelW - 16;
    }

    private int bodyH() {
        return this.panelH - 58 - 28;
    }

    private void initHeader() {
        Tab[] tabs = Tab.values();
        int gap = 2;
        int tabW = (this.panelW - 16 - gap * (tabs.length - 1)) / tabs.length;
        int x = this.leftPos + 8;
        int y = this.topPos + 22;
        for (Tab tab : tabs) {
            boolean active = tab == this.activeTab;
            String text = (active ? "\u00a7f\u00a7l" : "\u00a77") + tab.label;
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)text), b -> {
                this.activeTab = tab;
                Sfx.click();
                this.rebuildWidgets();
            }).bounds(x, y, tabW, 16).build());
            x += tabW + gap;
        }
    }

    private void initFooter() {
        int w = 150;
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7aGuardar y publicar"), b -> {
            this.save();
            Sfx.success();
            this.onClose();
        }).bounds(this.leftPos + this.panelW - w - 8, this.topPos + this.panelH - 24, w, 18).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Cerrar"), b -> this.onClose()).bounds(this.leftPos + 8, this.topPos + this.panelH - 24, 80, 18).build());
        long sum = this.pending[0] + this.pending[1] + this.pending[2];
        String cobrar = sum > 0L ? "\u00a7aCobrar: " + this.pending[2] + "o " + this.pending[1] + "p " + this.pending[0] + "b" : "\u00a77Sin ganancias";
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)cobrar), b -> {
            if (this.pending[0] + this.pending[1] + this.pending[2] > 0L) {
                PacketHandler.sendToServer((Object)new CollectMainShopPacket(false));
                this.pending[0] = 0L;
                this.pending[1] = 0L;
                this.pending[2] = 0L;
                Sfx.success();
                this.rebuildWidgets();
            }
        }).tooltip(Tooltip.create((Component)Component.literal((String)"Cobra las ganancias que la tienda del servidor ha recaudado (se depositan en tus monedas)."))).bounds(this.leftPos + 92, this.topPos + this.panelH - 24, 128, 18).build());
    }

    private void initItems() {
        this.help = "Clic en un item para a\u00f1adirlo. Pasa el rat\u00f3n por cada control para ver qu\u00e9 hace.";
        int x = this.bodyX();
        int y = this.bodyY();
        int colW = (this.bodyW() - 8) / 2;
        int rightX = x + colW + 8;
        int catRows = 76;
        int listBottom = y + this.bodyH() - catRows - 2;
        // Toggle de fuente: Registro (todos los items del juego) o Inventario del jugador (items con NBT
        // unico como llaves de crates, cabezas, items custom con nombre/lore/encantamientos, que no
        // existen en el registro). Mismo comportamiento que el picker de drops de FantasticSpawner.
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)(this.fromInventory ? "Fuente: \u00a7bInventario" : "Fuente: \u00a7eRegistro")), b -> {
            this.fromInventory = !this.fromInventory;
            Sfx.click();
            this.rebuildWidgets();
        }).tooltip(Tooltip.create((Component)Component.literal((String)"Registro = todos los items del juego. Inventario = tus items REALES con su NBT (llaves de crates, cabezas, items custom con nombre/lore/encantamientos)."))).bounds(x, y, colW, 16).build());
        EditBox search = new EditBox(this.font, x, y + 18, colW, 16, (Component)Component.empty());
        search.setHint((Component)Component.literal((String)"Buscar item..."));
        this.addRenderableWidget(search);
        int listY = y + 38;
        int listH = listBottom - listY;
        if (this.fromInventory) {
            java.util.List<ItemStack> invItems = new ArrayList<ItemStack>();
            net.minecraft.world.entity.player.Player p = net.minecraft.client.Minecraft.getInstance().player;
            if (p != null) {
                for (ItemStack st : p.getInventory().items) {
                    if (st != null && !st.isEmpty()) {
                        invItems.add(st.copy());
                    }
                }
            }
            ScrollSelector<ItemStack> invList = new ScrollSelector<ItemStack>(x, listY, colW, listH, 18, st -> st.getHoverName().getString(), st -> st.getHoverName().getString() + " " + RegistryLists.itemId(st.getItem()), st -> st);
            invList.setItems(invItems);
            invList.onSelect(st -> {
                this.addOfferStack((ItemStack)st);
                Sfx.select();
                this.rebuildWidgets();
            });
            search.setResponder(invList::setQuery);
            this.addRenderableWidget(invList);
            if (invItems.isEmpty()) {
                this.addLabel("\u00a77Tu inventario esta vacio.", x, listY + 4);
            }
        } else {
            ScrollSelector<Item> items = new ScrollSelector<Item>(x, listY, colW, listH, 18, RegistryLists::itemName, it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it), ItemStack::new);
            items.setItems(RegistryLists.items());
            items.onSelect(it -> {
                this.addOffer((Item)it);
                Sfx.select();
                this.rebuildWidgets();
            });
            search.setResponder(items::setQuery);
            this.addRenderableWidget(items);
        }
        int bw = colW / 3 - 2;
        int r1 = y + this.bodyH() - 72;
        int r2 = y + this.bodyH() - 54;
        int r3 = y + this.bodyH() - 36;
        int r4 = y + this.bodyH() - 18;
        this.addRenderableWidget(this.catButton("Bloques", x, r1, bw, (ResourceKey<CreativeModeTab>)CreativeModeTabs.BUILDING_BLOCKS));
        this.addRenderableWidget(this.catButton("Naturales", x + bw + 2, r1, bw, (ResourceKey<CreativeModeTab>)CreativeModeTabs.NATURAL_BLOCKS));
        this.addRenderableWidget(this.catButton("Colores", x + 2 * (bw + 2), r1, bw, (ResourceKey<CreativeModeTab>)CreativeModeTabs.COLORED_BLOCKS));
        this.addRenderableWidget(this.catButton("Funcional", x, r2, bw, (ResourceKey<CreativeModeTab>)CreativeModeTabs.FUNCTIONAL_BLOCKS));
        this.addRenderableWidget(this.catButton("Combate", x + bw + 2, r2, bw, (ResourceKey<CreativeModeTab>)CreativeModeTabs.COMBAT));
        this.addRenderableWidget(this.catButton("Herram.", x + 2 * (bw + 2), r2, bw, (ResourceKey<CreativeModeTab>)CreativeModeTabs.TOOLS_AND_UTILITIES));
        this.addRenderableWidget(this.catButton("Comida", x, r3, bw, (ResourceKey<CreativeModeTab>)CreativeModeTabs.FOOD_AND_DRINKS));
        this.addRenderableWidget(this.catButton("Redstone", x + bw + 2, r3, bw, (ResourceKey<CreativeModeTab>)CreativeModeTabs.REDSTONE_BLOCKS));
        this.addRenderableWidget(this.catButton("Ingred.", x + 2 * (bw + 2), r3, bw, (ResourceKey<CreativeModeTab>)CreativeModeTabs.INGREDIENTS));
        this.addRenderableWidget(this.catButton("Huevos", x, r4, bw, (ResourceKey<CreativeModeTab>)CreativeModeTabs.SPAWN_EGGS));
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7e+ TODO"), b -> {
            for (Item it : RegistryLists.items()) {
                this.addOfferIfNew(it);
            }
            Sfx.select();
            this.rebuildWidgets();
        }).tooltip(Tooltip.create((Component)Component.literal((String)"Agrega TODOS los items del juego (\u00a1son muchos!). \u00dasalo con cuidado."))).bounds(x + bw + 2, r4, bw, 16).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7cLimpiar"), b -> {
            this.offers.clear();
            this.selected = null;
            Sfx.click();
            this.rebuildWidgets();
        }).tooltip(Tooltip.create((Component)Component.literal((String)"Quita TODAS las ofertas de la tienda."))).bounds(x + 2 * (bw + 2), r4, bw, 16).build());
        int editorH = 116;
        ScrollSelector<ShopOffer> list = new ScrollSelector<ShopOffer>(rightX, y, colW, this.bodyH() - editorH, 16, o -> (o == this.selected ? "\u00a7e\u25b6 " : "\u00a7f") + o.getItem().getHoverName().getString() + (String)(o.getBundle() > 1 ? " \u00a78x" + o.getBundle() : "") + " " + CoinEconomy.coinColorCode((int)o.getCoin()) + o.getUnitPrice() + MainShopCreatorScreen.coinShort(o.getCoin()), o -> o.getItem().getHoverName().getString(), ShopOffer::getItem);
        list.setItems(new ArrayList<ShopOffer>(this.offers));
        list.onSelect(o -> {
            this.selected = o;
            this.rebuildWidgets();
        });
        this.addRenderableWidget(list);
        if (this.selected != null && this.offers.contains(this.selected)) {
            ShopOffer o2 = this.selected;
            int ey = y + this.bodyH() - 110;
            this.addLongField(rightX + 46, ey, 70, o2.getUnitPrice(), v -> o2.setUnitPrice(Math.max(1L, v)), "Precio:", rightX, ey + 4, "Precio por CADA venta (por el 'Vender de a'). Ej: si vendes de a 64 y el precio es 10, el jugador paga 10 por 64 items.");
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)("Moneda: " + CoinEconomy.coinColorCode((int)o2.getCoin()) + MainShopCreatorScreen.coinName(o2.getCoin()))), b -> {
                o2.setCoin((o2.getCoin() + 1) % 3);
                Sfx.click();
                this.rebuildWidgets();
            }).tooltip(Tooltip.create((Component)Component.literal((String)"Moneda del precio: bronce (naranja), plata o oro. Clic para cambiar."))).bounds(rightX + 122, ey, colW - 122, 16).build());
            int sy = ey + 22;
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)(o2.isInfinite() ? "\u00a7bStock: \u221e" : "\u00a7fStock: limitado")), b -> {
                o2.setInfinite(!o2.isInfinite());
                Sfx.click();
                this.rebuildWidgets();
            }).tooltip(Tooltip.create((Component)Component.literal((String)"\u221e = nunca se agota (ideal para la tienda del servidor). 'Limitado' = defines una cantidad exacta."))).bounds(rightX, sy, colW / 2 - 2, 16).build());
            if (!o2.isInfinite()) {
                this.addIntField(rightX + colW / 2 + 30, sy, colW / 2 - 32, o2.getStock(), v -> o2.setStock(Math.max(0, v)), "Cant.", rightX + colW / 2, sy + 4, "Cantidad total de items disponibles para vender.");
            }
            int by = ey + 44;
            this.addIntField(rightX + 74, by, 40, o2.getBundle(), v -> o2.setBundle(Math.max(1, v)), "Vender de a:", rightX, by + 4, "Cuantos items entrega CADA compra y a los que aplica el precio. 1 = de a uno. 64 = vende de a un stack. El stock no cambia esto.");
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7b\u270e Editar NBT"), b -> this.minecraft.setScreen((Screen)new NbtEditorScreen((Screen)this, o2.getItem()))).tooltip(Tooltip.create((Component)Component.literal((String)"Personaliza el item: nombre y lore con color, encantamientos, atributos... (items custom)."))).bounds(rightX + 120, by, colW - 120, 16).build());
            int ry = ey + 66;
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Precio a TODAS"), b -> {
                for (ShopOffer x2 : this.offers) {
                    x2.setUnitPrice(o2.getUnitPrice());
                    x2.setCoin(o2.getCoin());
                }
                Sfx.success();
                this.rebuildWidgets();
            }).tooltip(Tooltip.create((Component)Component.literal((String)"Aplica ESTE precio y moneda a TODAS las ofertas de golpe (precios por bultos)."))).bounds(rightX, ry, colW / 2 - 2, 16).build());
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Stack a TODAS"), b -> {
                for (ShopOffer x2 : this.offers) {
                    x2.setBundle(o2.getBundle());
                }
                Sfx.success();
                this.rebuildWidgets();
            }).tooltip(Tooltip.create((Component)Component.literal((String)"Aplica ESTE 'Vender de a' (tama\u00f1o de venta por stack) a TODAS las ofertas."))).bounds(rightX + colW / 2, ry, colW / 2, 16).build());
            int ry2 = ey + 88;
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u00a7cQuitar este item"), b -> {
                this.offers.remove(o2);
                this.selected = null;
                Sfx.click();
                this.rebuildWidgets();
            }).tooltip(Tooltip.create((Component)Component.literal((String)"Quita este item de la tienda."))).bounds(rightX, ry2, colW, 16).build());
        } else {
            this.addLabel("\u00a77Selecciona un item de la lista de la derecha", rightX, y + this.bodyH() - 82);
            this.addLabel("\u00a77para editar su precio, moneda, stock y NBT.", rightX, y + this.bodyH() - 70);
        }
    }

    private Button catButton(String label, int x, int y, int w, ResourceKey<CreativeModeTab> key) {
        return Button.builder((Component)Component.literal((String)label), b -> {
            int before = this.offers.size();
            for (Item it : RegistryLists.itemsOfTab(key)) {
                this.addOfferIfNew(it);
            }
            if (this.offers.size() > before) {
                Sfx.select();
            } else {
                Sfx.click();
            }
            this.rebuildWidgets();
        }).tooltip(Tooltip.create((Component)Component.literal((String)"Agrega DE GOLPE todos los items de esta categor\u00eda (precio 1 bronce, stock \u221e). Luego ajusta los que quieras."))).bounds(x, y, w, 16).build();
    }

    private void initSettings() {
        this.help = "Nombre de la tienda e icono que se muestra en el primer slot del mercado.";
        int x = this.bodyX();
        int y = this.bodyY();
        int colW = (this.bodyW() - 8) / 2;
        int rightX = x + colW + 8;
        EditBox nameBox = new EditBox(this.font, x + 70, y, colW - 70, 16, (Component)Component.empty());
        nameBox.setMaxLength(48);
        nameBox.setValue(this.name);
        nameBox.setResponder(s -> {
            this.name = s;
        });
        this.addRenderableWidget(nameBox);
        this.addLabel("Nombre:", x, y + 4);
        this.addLabel("\u00a76Icono actual:", x, y + 34);
        this.addLabel("\u00a7eElige el icono \u2192", rightX, y - 2);
        EditBox iconSearch = new EditBox(this.font, rightX, y + 12, colW, 16, (Component)Component.empty());
        iconSearch.setHint((Component)Component.literal((String)"Buscar icono..."));
        this.addRenderableWidget(iconSearch);
        ScrollSelector<Item> iconList = new ScrollSelector<Item>(rightX, y + 32, colW, this.bodyH() - 34, 18, RegistryLists::itemName, it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it), ItemStack::new);
        iconList.setItems(RegistryLists.items());
        iconList.onSelect(it -> {
            this.icon = new ItemStack((ItemLike)it);
            Sfx.select();
            this.rebuildWidgets();
        });
        iconSearch.setResponder(iconList::setQuery);
        this.addRenderableWidget(iconList);
    }

    private void addOffer(Item item) {
        ShopOffer offer = new ShopOffer(new ItemStack((ItemLike)item), 1L, 0, 0);
        offer.setInfinite(true);
        this.offers.add(offer);
        this.selected = offer;
    }

    private void addOfferStack(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        ShopOffer offer = new ShopOffer(copy, 1L, 0, 0);
        offer.setInfinite(true);
        this.offers.add(offer);
        this.selected = offer;
    }

    private void addOfferIfNew(Item item) {
        for (ShopOffer o : this.offers) {
            if (o.getItem().getItem() != item) continue;
            return;
        }
        ShopOffer offer = new ShopOffer(new ItemStack((ItemLike)item), 1L, 0, 0);
        offer.setInfinite(true);
        this.offers.add(offer);
    }

    private void save() {
        PlayerShop out = new PlayerShop(this.shop.getId(), this.shop.getOwner(), this.shop.getOwnerName(), this.name == null || this.name.isBlank() ? "La Moneda de Oro" : this.name);
        out.setMain(true);
        out.setIcon(this.icon);
        out.getOffers().addAll(this.offers);
        PacketHandler.sendToServer((Object)new SaveMainShopPacket(out));
    }

    private static String coinName(int coin) {
        return switch (coin) {
            case 2 -> "Oro";
            case 1 -> "Plata";
            default -> "Bronce";
        };
    }

    private static String coinShort(int coin) {
        return switch (coin) {
            case 2 -> "o";
            case 1 -> "p";
            default -> "b";
        };
    }

    private void addLabel(String text, int x, int y) {
        this.labels.add(new Label(text, x, y));
    }

    private void addIntField(int x, int y, int w, int value, IntConsumer setter, String label, int lx, int ly, String tip) {
        EditBox box = new EditBox(this.font, x, y, w, 16, (Component)Component.empty());
        box.setMaxLength(10);
        box.setValue(Integer.toString(value));
        box.setResponder(s -> {
            try {
                setter.accept(Integer.parseInt(s.trim()));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        });
        if (tip != null) {
            box.setTooltip(Tooltip.create((Component)Component.literal((String)tip)));
        }
        this.addRenderableWidget(box);
        if (label != null) {
            this.addLabel(label, lx, ly);
        }
    }

    private void addLongField(int x, int y, int w, long value, LongConsumer setter, String label, int lx, int ly, String tip) {
        EditBox box = new EditBox(this.font, x, y, w, 16, (Component)Component.empty());
        box.setMaxLength(12);
        box.setValue(Long.toString(value));
        box.setResponder(s -> {
            try {
                setter.accept(Long.parseLong(s.trim()));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        });
        if (tip != null) {
            box.setTooltip(Tooltip.create((Component)Component.literal((String)tip)));
        }
        this.addRenderableWidget(box);
        if (label != null) {
            this.addLabel(label, lx, ly);
        }
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelW, this.topPos + this.panelH, -535160294);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.panelW, this.topPos + 18, -14013910);
        g.fill(this.leftPos, this.topPos + this.panelH - 1, this.leftPos + this.panelW, this.topPos + this.panelH, -12961222);
        g.fill(this.leftPos + 6, this.topPos + 40, this.leftPos + this.panelW - 6, this.topPos + 41, -12961222);
        g.drawString(this.font, "\u00a76\u2726 La Moneda de Oro \u00a77- \u00a7f" + this.offers.size() + " items", this.leftPos + 8, this.topPos + 5, 0xFFFFFF, false);
        g.renderFakeItem(this.icon, this.leftPos + this.panelW - 24, this.topPos + 2);
        if (!this.help.isEmpty()) {
            String trimmed = this.font.plainSubstrByWidth("\u00a77" + this.help, this.panelW - 16);
            g.drawString(this.font, trimmed, this.leftPos + 8, this.topPos + 45, 10141936, false);
        }
        if (this.activeTab == Tab.SETTINGS) {
            g.renderFakeItem(this.icon, this.bodyX() + 70, this.topPos + 88);
        }
        super.render(g, mouseX, mouseY, partial);
        for (Label l : this.labels) {
            g.drawString(this.font, l.text(), l.x(), l.y(), 0xE0E0E0, false);
        }
    }

    public boolean isPauseScreen() {
        return false;
    }

    private static enum Tab {
        ITEMS("Items"),
        SETTINGS("Ajustes");

        final String label;

        private Tab(String label) {
            this.label = label;
        }
    }

    private record Label(String text, int x, int y) {
    }
}
