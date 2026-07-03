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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Admin creator/editor for the main server shop ("La Moneda de Oro"), styled
 * like the Fantastic Crates editor: dark panel, tabs, a searchable browser of
 * the WHOLE item registry (vanilla + mods), per-offer price/coin/stock with an
 * infinite option, bulk category adds and a "price to all" bulk action, plus a
 * configurable shop icon. Everything is laid out on a grid so nothing overlaps.
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

   private record TooltipZone(int x, int y, int w, int h, List<Component> lines) {
   }

   private final PlayerShop shop;
   private final List<ShopOffer> offers;
   private String name;
   private ItemStack icon;
   private Tab activeTab = Tab.ITEMS;
   private ShopOffer selected;

   private final List<Label> labels = new ArrayList<>();
   private final List<TooltipZone> zones = new ArrayList<>();
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
      this.zones.clear();
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
      this.help = "Izquierda: busca cualquier item (incluidos mods) y da clic para anadirlo. Derecha: edita precio, moneda y stock.";
      int x = bodyX();
      int y = bodyY();
      int colW = (bodyW() - 8) / 2;
      int rightX = x + colW + 8;

      // --- left: search + full registry list + category bulk-adds ---
      EditBox search = new EditBox(this.font, x, y, colW, 16, Component.empty());
      search.setHint(Component.literal("Buscar item..."));
      addRenderableWidget(search);

      int catRows = 40;
      ScrollSelector<Item> items = new ScrollSelector<>(x, y + 20, colW, bodyH() - 22 - catRows, 18,
            RegistryLists::itemName, it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it),
            it -> new ItemStack(it));
      items.setItems(RegistryLists.items());
      items.onSelect(it -> {
         addOffer(it);
         Sfx.select();
         rebuildWidgets();
      });
      search.setResponder(items::setQuery);
      addRenderableWidget(items);

      // category quick-adds (bulk) + clear
      int cy1 = y + bodyH() - 38;
      int cy2 = y + bodyH() - 18;
      int bw = colW / 3 - 2;
      addRenderableWidget(catButton("+ Bloques", x, cy1, bw, RegistryLists.tabBuildingBlocks()));
      addRenderableWidget(catButton("+ Combate", x + bw + 2, cy1, bw, RegistryLists.tabCombat()));
      addRenderableWidget(catButton("+ Herram.", x + 2 * (bw + 2), cy1, bw, RegistryLists.tabTools()));
      addRenderableWidget(catButton("+ Comida", x, cy2, bw, RegistryLists.tabFood()));
      addRenderableWidget(catButton("+ Redstone", x + bw + 2, cy2, bw, RegistryLists.tabRedstone()));
      addRenderableWidget(Button.builder(Component.literal("\u00a7cLimpiar"), b -> {
         this.offers.clear();
         this.selected = null;
         Sfx.click();
         rebuildWidgets();
      }).bounds(x + 2 * (bw + 2), cy2, bw, 16).build());

      // --- right: current offers list + selected editor ---
      int editorH = 82;
      ScrollSelector<ShopOffer> list = new ScrollSelector<>(rightX, y, colW, bodyH() - editorH, 16,
            o -> (o == this.selected ? "\u00a7e\u25b6 " : "\u00a7f") + o.getItem().getHoverName().getString()
                  + " \u00a77" + o.getUnitPrice() + coinShort(o.getCoin()),
            o -> o.getItem().getHoverName().getString(), ShopOffer::getItem);
      list.setItems(new ArrayList<>(this.offers));
      list.onSelect(o -> {
         this.selected = o;
         rebuildWidgets();
      });
      addRenderableWidget(list);

      addLabel("\u00a77Items en la tienda: \u00a7f" + this.offers.size(), rightX, y + bodyH() - editorH + 2, null);

      if (this.selected != null && this.offers.contains(this.selected)) {
         ShopOffer o = this.selected;
         int ey = y + bodyH() - 64;
         // price + coin
         addIntFieldLong(rightX + 46, ey, 70, o.getUnitPrice(), v -> o.setUnitPrice(Math.max(1L, v)),
               "Precio:", rightX, ey + 4);
         addRenderableWidget(Button.builder(Component.literal("Moneda: " + coinName(o.getCoin())), b -> {
            o.setCoin((o.getCoin() + 1) % 3);
            Sfx.click();
            rebuildWidgets();
         }).bounds(rightX + 122, ey, colW - 122, 16).build());
         // stock: infinite toggle + finite field
         int sy = ey + 22;
         addRenderableWidget(Button.builder(
               Component.literal(o.isInfinite() ? "\u00a7bStock: \u221e (infinito)" : "\u00a7fStock: limitado"), b -> {
                  o.setInfinite(!o.isInfinite());
                  Sfx.click();
                  rebuildWidgets();
               }).bounds(rightX, sy, colW / 2 - 2, 16).build());
         if (!o.isInfinite()) {
            addIntField(rightX + colW / 2 + 30, sy, colW / 2 - 32, o.getStock(),
                  v -> o.setStock(Math.max(0, v)), "Cant.", rightX + colW / 2, sy + 4);
         }
         // remove + bulk price-to-all
         int ry = ey + 44;
         addRenderableWidget(Button.builder(Component.literal("\u00a7cQuitar"), b -> {
            this.offers.remove(o);
            this.selected = null;
            Sfx.click();
            rebuildWidgets();
         }).bounds(rightX, ry, colW / 2 - 2, 16).build());
         Button all = Button.builder(Component.literal("Precio a TODAS"), b -> {
            for (ShopOffer x2 : this.offers) {
               x2.setUnitPrice(o.getUnitPrice());
               x2.setCoin(o.getCoin());
            }
            Sfx.success();
            rebuildWidgets();
         }).bounds(rightX + colW / 2, ry, colW / 2, 16).build();
         addRenderableWidget(all);
         this.zones.add(new TooltipZone(rightX + colW / 2, ry, colW / 2, 16,
               desc("Aplica ESTE precio y moneda a TODAS las ofertas.", "Precio por bultos rapido.")));
      } else {
         addLabel("\u00a77Selecciona un item de la derecha", rightX, y + bodyH() - 44, null);
         addLabel("\u00a77para editar su precio y stock.", rightX, y + bodyH() - 32, null);
      }
   }

   private Button catButton(String label, int x, int y, int w, CreativeModeTab tab) {
      return Button.builder(Component.literal(label), b -> {
         addCategory(tab);
         Sfx.select();
         rebuildWidgets();
      }).bounds(x, y, w, 16).build();
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
      addLabel("Nombre:", x, y + 4, desc("Nombre visible de la tienda principal."));

      addLabel("\u00a76Icono actual:", x, y + 30, null);
      addLabel("\u00a77(destaca en el slot 0)", x, y + 54, null);

      addLabel("\u00a7eElige el icono \u2192", rightX, y - 2, desc("Busca y da clic en cualquier item", "para usarlo como icono de la tienda."));
      EditBox iconSearch = new EditBox(this.font, rightX, y + 12, colW, 16, Component.empty());
      iconSearch.setHint(Component.literal("Buscar icono..."));
      addRenderableWidget(iconSearch);
      ScrollSelector<Item> iconList = new ScrollSelector<>(rightX, y + 32, colW, bodyH() - 34, 18,
            RegistryLists::itemName, it -> RegistryLists.itemName(it) + " " + RegistryLists.itemId(it),
            it -> new ItemStack(it));
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

   private void addCategory(CreativeModeTab tab) {
      if (tab == null) {
         return;
      }
      for (Item item : RegistryLists.itemsOfTab(tab)) {
         boolean exists = false;
         for (ShopOffer o : this.offers) {
            if (o.getItem().getItem() == item) {
               exists = true;
               break;
            }
         }
         if (!exists) {
            ShopOffer offer = new ShopOffer(new ItemStack(item), 1L, CoinEconomy.BRONZE, 0);
            offer.setInfinite(true);
            this.offers.add(offer);
         }
      }
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

   private static List<Component> desc(String... lines) {
      List<Component> out = new ArrayList<>();
      for (String s : lines) {
         out.add(Component.literal(s));
      }
      return out;
   }

   private void addLabel(String text, int x, int y, List<Component> tooltip) {
      this.labels.add(new Label(text, x, y));
      if (tooltip != null) {
         this.zones.add(new TooltipZone(x, y - 2, Math.max(120, this.font.width(text) + 8), 14, tooltip));
      }
   }

   private void addIntField(int x, int y, int w, int value, IntConsumer setter, String label, int lx, int ly) {
      EditBox box = new EditBox(this.font, x, y, w, 16, Component.empty());
      box.setMaxLength(10);
      box.setValue(Integer.toString(value));
      box.setResponder(s -> {
         try {
            setter.accept(Integer.parseInt(s.trim()));
         } catch (NumberFormatException ignored) {
         }
      });
      addRenderableWidget(box);
      if (label != null) {
         this.labels.add(new Label(label, lx, ly));
      }
   }

   private void addIntFieldLong(int x, int y, int w, long value, java.util.function.LongConsumer setter,
         String label, int lx, int ly) {
      EditBox box = new EditBox(this.font, x, y, w, 16, Component.empty());
      box.setMaxLength(12);
      box.setValue(Long.toString(value));
      box.setResponder(s -> {
         try {
            setter.accept(Long.parseLong(s.trim()));
         } catch (NumberFormatException ignored) {
         }
      });
      addRenderableWidget(box);
      if (label != null) {
         this.labels.add(new Label(label, lx, ly));
      }
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelW, this.topPos + this.panelH, 0xE01A1A1A);
      g.fill(this.leftPos, this.topPos, this.leftPos + this.panelW, this.topPos + 18, 0xFF2A2A2A);
      g.fill(this.leftPos, this.topPos + this.panelH - 1, this.leftPos + this.panelW, this.topPos + this.panelH, 0xFF3A3A3A);
      g.fill(this.leftPos + 6, this.topPos + 40, this.leftPos + this.panelW - 6, this.topPos + 41, 0xFF3A3A3A);
      g.drawString(this.font, "\u00a76\u2726 La Moneda de Oro \u00a76\u2726 \u00a77- creador de tienda del servidor",
            this.leftPos + 8, this.topPos + 5, 0xFFFFFF, false);
      g.renderFakeItem(this.icon, this.leftPos + this.panelW - 24, this.topPos + 2);
      if (!this.help.isEmpty()) {
         String trimmed = this.font.plainSubstrByWidth("\u00a77" + this.help, this.panelW - 16);
         g.drawString(this.font, trimmed, this.leftPos + 8, this.topPos + 45, 0x9AC0F0, false);
      }
      // settings tab: draw the current icon preview big-ish
      if (this.activeTab == Tab.SETTINGS) {
         g.renderFakeItem(this.icon, bodyX() + 70, this.topPos + 84);
      }
      super.render(g, mouseX, mouseY, partial);
      for (Label l : this.labels) {
         g.drawString(this.font, l.text(), l.x(), l.y(), 0xE0E0E0, false);
      }
      for (TooltipZone z : this.zones) {
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
}
