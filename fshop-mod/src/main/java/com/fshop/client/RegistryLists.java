package com.fshop.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/** Client-side access to the full item registry (vanilla + every mod). */
public final class RegistryLists {
   private RegistryLists() {
   }

   /** Every registered item (air excluded), sorted by id, mods included. */
   public static List<Item> items() {
      List<Item> list = new ArrayList<>();
      for (Item item : ForgeRegistries.ITEMS.getValues()) {
         if (item != Items.AIR) {
            list.add(item);
         }
      }
      list.sort(Comparator.comparing(RegistryLists::itemId));
      return list;
   }

   /**
    * Items shown in a creative tab (used for bulk category adds). Forces the tab
    * contents to build if they are not populated yet, so the buttons always work.
    */
   public static List<Item> itemsOfTab(ResourceKey<CreativeModeTab> key) {
      List<Item> list = new ArrayList<>();
      if (key == null) {
         return list;
      }
      CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(key);
      if (tab == null) {
         return list;
      }
      Collection<ItemStack> display = safeDisplay(tab);
      if (display.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && mc.player != null) {
            try {
               CreativeModeTabs.tryRebuildTabContents(mc.level.enabledFeatures(), true, mc.level.registryAccess());
               display = safeDisplay(tab);
            } catch (Exception ignored) {
               // keep whatever we have
            }
         }
      }
      for (ItemStack stack : display) {
         if (!stack.isEmpty() && stack.getItem() != Items.AIR && !list.contains(stack.getItem())) {
            list.add(stack.getItem());
         }
      }
      return list;
   }

   private static Collection<ItemStack> safeDisplay(CreativeModeTab tab) {
      try {
         return tab.getDisplayItems();
      } catch (Exception e) {
         return new ArrayList<>();
      }
   }

   public static String itemId(Item item) {
      ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
      return rl == null ? "minecraft:air" : rl.toString();
   }

   public static String itemName(Item item) {
      return new ItemStack(item).getHoverName().getString();
   }
}
