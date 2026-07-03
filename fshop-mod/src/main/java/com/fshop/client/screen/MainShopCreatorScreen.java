package com.fshop.client.screen;

import com.fshop.client.RegistryLists;
import com.fshop.client.Sfx;
import com.fshop.client.widget.ScrollSelector;
import com.fshop.economy.CoinEconomy;
import com.fshop.network.PacketHandler;
import com.fshop.network.SaveMainShopPacket;
import com.fshop.shop.PlayerShop;
import com.fshop.shop.ShopOffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Admin creator/editor for the main server shop ("La Moneda de Oro"), styled
 * like the Fantastic Crates editor. Browses the WHOLE item registry (vanilla +
 * mods), bulk-adds full creative categories with one click, sets per-offer
 * price / currency / stock (with an infinite option) and a sale bundle size
 * ("sell by the stack"), edits full NBT of custom items and sets a configurable
 * icon. Laid out on a grid so nothing overlaps.
 */
public final class MainShopCreatorScreen extends Screen {
   private enum Tab {
      ITEMS("Items"), SETTINGS("Ajustes");

      final String label;

      Tab(String label) {
         this.label = label;
      }
   }

   private record Label(String text, int x, int y) {
   }

   private final PlayerShop shop;
   private final List<ShopOffer> offers;
   private String name;
   private ItemStack icon;
   private Tab activeTab = Tab.ITEMS;
   private ShopOffer selected;

   private final List<Label> labels = new ArrayList<>();
   private String help = "";

   private int leftPos;
   private int topPos;
   private int panelW;
   private int panelH;

   public MainShopCreatorScreen(PlayerShop shop) {
      super(Component.literal("Creador de tienda"));
      this.shop = shop;
      this.offers = new ArrayList<>(shop.getOffers());
      this.name = shop.getName() == null || shop.getName().isBlank() ? "La Moneda de Oro" : shop.getName();
      this.icon = shop.getIcon().isEmpty() ? new ItemStack(Items.GOLD_INGOT) : shop.getIcon().copy();
   }

   @Override
   protected void init() {
      this.panelW = Math.min(this.width - 16, 460);
      this.panelH = Math.min(this.height - 16, 300);
      this.leftPos = (this.width - this.panelW) / 2;
      this.topPos = (this.height - this.panelH) / 2;
      this.labels.clear();
      initHeader();
      initFooter();
      if (this.activeTab == Tab.ITEMS) {
         initItems();
      } else {
         initSettings();
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
         addRenderableWidget(Button.builder(Component.literal(text), b -> {
            this.activeTab = tab;
            Sfx.click();
            rebuildWidgets();
         }).bounds(x, y, tabW, 16).build());
         x += tabW + gap;
      }
   }

   private void initFooter() {
      int w = 150;
      addRenderableWidget(Button.builder(Component.literal("\u00a7aGuardar y publicar"), b -> {
         save();
         Sfx.success();
         this.onClose();
      }).bounds(this.leftPos + this.panelW - w - 8, this.topPos + this.panelH - 24, w, 18).build());
      addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> this.onClose())
            .bounds(this.leftPos + 8, this.topPos + this.panelH - 24, 80, 18).build());
   }

   private void initItems() {
      this.help = "Clic en un item para a\u00f1adirlo. Pasa el rat\u00f3n por cada control para ver qu\u00e9 hace.";
      int x = bodyX();
      int y = bodyY();
      int colW = (bodyW() - 8) / 2;
      int rightX = x + colW + 8;

      // --- left: search + full registry list + category bulk-adds (3 rows) ---
      EditBox search = new EditBox(this.font, x, y, colW, 16, Component.empty());
      search.setHint(Component.literal("Buscar item..."));
      addRenderableWidget(search);

      int catRows = 56;
      ScrollSelector<Item> items = new ScrollSelector<>(x, y + 20, colW, bodyH() - 22 - catRows, 18,
            RegistryLists::itemName, it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it),
            ItemStack::new);
      items.setItems(RegistryLists.items());
      items.onSelect(it -> {
         addOffer(it);
         Sfx.select();
         rebuildWidgets();
      });
      search.setResponder(items::setQuery);
      addRenderableWidget(items);

      int bw = colW / 3 - 2;
      int r1 = y + bodyH() - 54;
      int r2 = y + bodyH() - 36;
      int r3 = y + bodyH() - 18;
      addRenderableWidget(catButton("Bloques", x, r1, bw, CreativeModeTabs.BUILDING_BLOCKS));
      addRenderableWidget(catButton("Naturales", x + bw + 2, r1, bw, CreativeModeTabs.NATURAL_BLOCKS));
      addRenderableWidget(catButton("Colores", x + 2 * (bw + 2), r1, bw, CreativeModeTabs.COLORED_BLOCKS));
      addRenderableWidget(catButton("Combate", x, r2, bw, CreativeModeTabs.COMBAT));
      addRenderableWidget(catButton("Herram.", x + bw + 2, r2, bw, CreativeModeTabs.TOOLS_AND_UTILITIES));
      addRenderableWidget(catButton("Comida", x + 2 * (bw + 2), r2, bw, CreativeModeTabs.FOOD_AND_DRINKS));
      addRenderableWidget(catButton("Redstone", x, r3, bw, CreativeModeTabs.REDSTONE_BLOCKS));
      addRenderableWidget(Button.builder(Component.literal("\u00a7e+ TODO"), b -> {
         for (Item it : RegistryLists.items()) {
            addOfferIfNew(it);
         }
         Sfx.select();
         rebuildWidgets();
      }).tooltip(Tooltip.create(Component.literal(
            "Agrega TODOS los items del juego (\u00a1son muchos!). \u00da\u0073alo con cuidado.")))
            .bounds(x + bw + 2, r3, bw, 16).build());
      addRenderableWidget(Button.builder(Component.literal("\u00a7cLimpiar"), b -> {
         this.offers.clear();
         this.selected = null;
         Sfx.click();
         rebuildWidgets();
      }).tooltip(Tooltip.create(Component.literal("Quita TODAS las ofertas de la tienda.")))
            .bounds(x + 2 * (bw + 2), r3, bw, 16).build());

      // --- right: current offers list + selected editor ---
      int editorH = 94;
      ScrollSelector<ShopOffer> list = new ScrollSelector<>(rightX, y, colW, bodyH() - editorH, 16,
            o -> (o == this.selected ? "\u00a7e\u25b6 " : "\u00a7f") + o.getItem().getHoverName().getString()
                  + (o.getBundle() > 1 ? " \u00a78x" + o.getBundle() : "")
                  + " " + CoinEconomy.coinColorCode(o.getCoin()) + o.getUnitPrice() + coinShort(o.getCoin()),
            o -> o.getItem().getHoverName().getString(), ShopOffer::getItem);
      list.setItems(new ArrayList<>(this.offers));
      list.onSelect(o -> {
         this.selected = o;
         rebuildWidgets();
      });
      addRenderableWidget(list);

      addLabel("\u00a77Items: \u00a7f" + this.offers.size(), rightX, y + bodyH() - editorH + 2);

      if (this.selected != null && this.offers.contains(this.selected)) {
         ShopOffer o = this.selected;
         int ey = y + bodyH() - 88;
         // row 1: price (per sale unit) + coin
         addLongField(rightX + 46, ey, 70, o.getUnitPrice(), v -> o.setUnitPrice(Math.max(1L, v)), "Precio:", rightX, ey + 4,
               "Precio por CADA venta (por el 'Vender de a'). Ej: si vendes de a 64 y el precio es 10, el jugador paga 10 por 64 items.");
         addRenderableWidget(Button.builder(
               Component.literal("Moneda: " + CoinEconomy.coinColorCode(o.getCoin()) + coinName(o.getCoin())), b -> {
                  o.setCoin((o.getCoin() + 1) % 3);
                  Sfx.click();
                  rebuildWidgets();
               }).tooltip(Tooltip.create(Component.literal("Moneda del precio: bronce (naranja), plata o oro. Clic para cambiar.")))
               .bounds(rightX + 122, ey, colW - 122, 16).build());
         // row 2: stock infinite toggle + finite field
         int sy = ey + 22;
         addRenderableWidget(Button.builder(
               Component.literal(o.isInfinite() ? "\u00a7bStock: \u221e" : "\u00a7fStock: limitado"), b -> {
                  o.setInfinite(!o.isInfinite());
                  Sfx.click();
                  rebuildWidgets();
               }).tooltip(Tooltip.create(Component.literal(
                     "\u221e = nunca se agota (ideal para la tienda del servidor). 'Limitado' = defines una cantidad exacta.")))
               .bounds(rightX, sy, colW / 2 - 2, 16).build());
         if (!o.isInfinite()) {
            addIntField(rightX + colW / 2 + 30, sy, colW / 2 - 32, o.getStock(), v -> o.setStock(Math.max(0, v)),
                  "Cant.", rightX + colW / 2, sy + 4, "Cantidad total de items disponibles para vender.");
         }
         // row 3: bundle (sell by the stack) + NBT editor
         int by = ey + 44;
         addIntField(rightX + 74, by, 40, o.getBundle(), v -> o.setBundle(Math.max(1, v)), "Vender de a:", rightX, by + 4,
               "Cuantos items entrega CADA compra y a los que aplica el precio. 1 = de a uno. 64 = vende de a un stack. El stock no cambia esto.");
         addRenderableWidget(Button.builder(Component.literal("\u00a7b\u270e Editar NBT"), b -> {
            this.minecraft.setScreen(new NbtEditorScreen(this, o.getItem()));
         }).tooltip(Tooltip.create(Component.literal(
               "Personaliza el item: nombre y lore con color, encantamientos, atributos... (items custom).")))
               .bounds(rightX + 120, by, colW - 120, 16).build());
         // row 4: remove + bulk price-to-all
         int ry = ey + 66;
         addRenderableWidget(Button.builder(Component.literal("\u00a7cQuitar"), b -> {
            this.offers.remove(o);
            this.selected = null;
            Sfx.click();
            rebuildWidgets();
         }).tooltip(Tooltip.create(Component.literal("Quita este item de la tienda.")))
               .bounds(rightX, ry, colW / 2 - 2, 16).build());
         addRenderableWidget(Button.builder(Component.literal("Precio a TODAS"), b -> {
            for (ShopOffer x2 : this.offers) {
               x2.setUnitPrice(o.getUnitPrice());
               x2.setCoin(o.getCoin());
            }
            Sfx.success();
            rebuildWidgets();
         }).tooltip(Tooltip.create(Component.literal(
               "Aplica ESTE precio y moneda a TODAS las ofertas de golpe (precios por bultos).")))
               .bounds(rightX + colW / 2, ry, colW / 2, 16).build());
      } else {
         addLabel("\u00a77Selecciona un item de la lista de la derecha", rightX, y + bodyH() - 60);
         addLabel("\u00a77para editar su precio, moneda, stock y NBT.", rightX, y + bodyH() - 48);
      }
   }

   private Button catButton(String label, int x, int y, int w, ResourceKey<CreativeModeTab> key) {
      return Button.builder(Component.literal(label), b -> {
         int before = this.offers.size();
         for (Item it : RegistryLists.itemsOfTab(key)) {
            addOfferIfNew(it);
         }
         if (this.offers.size() > before) {
            Sfx.select();
         } else {
            Sfx.click();
         }
         rebuildWidgets();
      }).tooltip(Tooltip.create(Component.literal(
            "Agrega DE GOLPE todos los items de esta categor\u00eda (precio 1 bronce, stock \u221e). Luego ajusta los que quieras.")))
            .bounds(x, y, w, 16).build();
   }

   private void initSettings() {
      this.help = "Nombre de la tienda e icono que se muestra en el primer slot del mercado.";
      int x = bodyX();
      int y = bodyY();
      int colW = (bodyW() - 8) / 2;
      int rightX = x + colW + 8;

      EditBox nameBox = new EditBox(this.font, x + 70, y, colW - 70, 16, Component.empty());
      nameBox.setMaxLength(48);
      nameBox.setValue(this.name);
      nameBox.setResponder(s -> this.name = s);
      addRenderableWidget(nameBox);
      addLabel("Nombre:", x, y + 4);
      addLabel("\u00a76Icono actual:", x, y + 34);

      addLabel("\u00a7eElige el icono \u2192", rightX, y - 2);
      EditBox iconSearch = new EditBox(this.font, rightX, y + 12, colW, 16, Component.empty());
      iconSearch.setHint(Component.literal("Buscar icono..."));
      addRenderableWidget(iconSearch);
      ScrollSelector<Item> iconList = new ScrollSelector<>(rightX, y + 32, colW, bodyH() - 34, 18,
            RegistryLists::itemName, it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it),
            ItemStack::new);
      iconList.setItems(RegistryLists.items());
      iconList.onSelect(it -> {
         this.icon = new ItemStack(it);
         Sfx.select();
         rebuildWidgets();
      });
      iconSearch.setResponder(iconList::setQuery);
      addRenderableWidget(iconList);
   }

   private void addOffer(Item item) {
      ShopOffer offer = new ShopOffer(new ItemStack(item), 1L, CoinEconomy.BRONZE, 0);
      offer.setInfinite(true);
      this.offers.add(offer);
      this.selected = offer;
   }

   private void addOfferIfNew(Item item) {
      for (ShopOffer o : this.offers) {
         if (o.getItem().getItem() == item) {
            return;
         }
      }
      ShopOffer offer = new ShopOffer(new ItemStack(item), 1L, CoinEconomy.BRONZE, 0);
      offer.setInfinite(true);
      this.offers.add(offer);
   }

   private void save() {
      PlayerShop out = new PlayerShop(this.shop.getId(), this.shop.getOwner(), this.shop.getOwnerName(),
            this.name == null || this.name.isBlank() ? "La Moneda de Oro" : this.name);
      out.setMain(true);
      out.setIcon(this.icon);
      out.getOffers().addAll(this.offers);
      PacketHandler.sendToServer(new SaveMainShopPacket(out));
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
      EditBox box = new EditBox(this.font, x, y, w, 16, Component.empty());
      box.setMaxLength(10);
      box.setValue(Integer.toString(value));
      box.setResponder(s -> {
         try {
            setter.accept(Integer.parseInt(s.trim()));
         } catch (NumberFormatException ignored) {
         }
      });
      if (tip != null) {
         box.setTooltip(Tooltip.create(Component.literal(tip)));
      }
      addRenderableWidget(box);
      if (label != null) {
         addLabel(label, lx, ly);
      }
   }

   private void addLongField(int x, int y, int w, long value, java.util.function.LongConsumer setter,
         String label, int lx, int ly, String tip) {
      EditBox box = new EditBox(this.font, x, y, w, 16, Component.empty());
      box.setMaxLength(12);
      box.setValue(Long.toString(value));
      box.setResponder(s -> {
         try {
            setter.accept(Long.parseLong(s.trim()));
         } catch (NumberFormatException ignored) {
         }
      });
      if (tip != null) {
         box.setTooltip(Tooltip.create(Component.literal(tip)));
      }
      addRenderableWidget(box);
      if (label != null) {
         addLabel(label, lx, ly);
      }
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelW, this.topPos + this.panelH, 0xE01A1A1A);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelW, this.topPos + 18, 0xFF2A2A2A);
      g.fill(this.leftPos, this.topPos + this.panelH - 1, this.leftPos + this.panelW, this.topPos + this.panelH, 0xFF3A3A3A);
      g.fill(this.leftPos + 6, this.topPos + 40, this.leftPos + this.panelW - 6, this.topPos + 41, 0xFF3A3A3A);
      g.drawString(this.font, "\u00a76\u2726 La Moneda de Oro \u00a76\u2726 \u00a77- creador del servidor",
            this.leftPos + 8, this.topPos + 5, 0xFFFFFF, false);
      g.renderFakeItem(this.icon, this.leftPos + this.panelW - 24, this.topPos + 2);
      if (!this.help.isEmpty()) {
         String trimmed = this.font.plainSubstrByWidth("\u00a77" + this.help, this.panelW - 16);
         g.drawString(this.font, trimmed, this.leftPos + 8, this.topPos + 45, 0x9AC0F0, false);
      }
      if (this.activeTab == Tab.SETTINGS) {
         g.renderFakeItem(this.icon, bodyX() + 70, this.topPos + 88);
      }
      super.render(g, mouseX, mouseY, partial);
      for (Label l : this.labels) {
         g.drawString(this.font, l.text(), l.x(), l.y(), 0xE0E0E0, false);
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
