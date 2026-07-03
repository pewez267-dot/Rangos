package com.fshop.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

   /** Items belonging to the given creative tab (used for bulk category adds). */
   public static List<Item> itemsOfTab(CreativeModeTab tab) {
      List<Item> list = new ArrayList<>();
      try {
         for (ItemStack stack : tab.getDisplayItems()) {
            if (!stack.isEmpty() && stack.getItem() != Items.AIR && !list.contains(stack.getItem())) {
               list.add(stack.getItem());
            }
         }
      } catch (Exception ignored) {
         // some tabs may not be populated on the client yet
      }
      return list;
   }

   private static CreativeModeTab tab(net.minecraft.resources.ResourceKey<CreativeModeTab> key) {
      return net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.get(key);
   }

   public static CreativeModeTab tabBuildingBlocks() {
      return tab(CreativeModeTabs.BUILDING_BLOCKS);
   }

   public static CreativeModeTab tabCombat() {
      return tab(CreativeModeTabs.COMBAT);
   }

   public static CreativeModeTab tabTools() {
      return tab(CreativeModeTabs.TOOLS_AND_UTILITIES);
   }

   public static CreativeModeTab tabFood() {
      return tab(CreativeModeTabs.FOOD_AND_DRINKS);
   }

   public static CreativeModeTab tabRedstone() {
      return tab(CreativeModeTabs.REDSTONE_BLOCKS);
   }

   public static String itemId(Item item) {
      ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
      return rl == null ? "minecraft:air" : rl.toString();
   }

   public static String itemName(Item item) {
      return new ItemStack(item).getHoverName().getString();
   }
}
