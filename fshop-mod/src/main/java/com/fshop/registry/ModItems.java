package com.fshop.registry;

import com.fshop.FShop;
import com.fshop.item.MarketWandItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
   public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FShop.MOD_ID);

   public static final RegistryObject<Item> MARKET_WAND = ITEMS.register(
         "market_wand",
         () -> new MarketWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

   private ModItems() {
   }

   public static void register(IEventBus bus) {
      ITEMS.register(bus);
   }
}
