package com.theplumteam.registry;

import com.theplumteam.block.PopBlockColor;
import com.theplumteam.figure.BuiltInCollections;
import com.theplumteam.item.BoxBlockItem;
import com.theplumteam.item.GeoBlockItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class ModItems {
   public static final DeferredRegister<Item> ITEMS = DeferredRegister.create("blockpops", Registries.ITEM);
   public static final Map<String, RegistrySupplier<Item>> BOX_BLOCK_ITEMS = new HashMap<>();
   public static final Map<PopBlockColor, RegistrySupplier<Item>> DEFAULT_BOX_BLOCK_ITEMS = new HashMap<>();

   public static final RegistrySupplier<Item> CLAW_MACHINE_BLOCK_ITEM = ITEMS.register(
      "claw_machine_block", () -> new GeoBlockItem(ModBlocks.CLAW_MACHINE_BLOCK.get(), new Item.Properties())
   );
   public static final RegistrySupplier<Item> FIGURE_BLOCK_ITEM = ITEMS.register(
      "figure_block", () -> new GeoBlockItem(ModBlocks.FIGURE_BLOCK.get(), new Item.Properties())
   );

   public static void register() {
      ITEMS.register();
   }

   static {
      for (PopBlockColor color : PopBlockColor.values()) {
         DEFAULT_BOX_BLOCK_ITEMS.put(
            color, ITEMS.register("box_block_" + color.getSerializedName(), () -> new BoxBlockItem(ModBlocks.BOX_BLOCK.get(), new Item.Properties(), color))
         );
      }

      for (String collectionId : BuiltInCollections.COLLECTION_IDS) {
         BOX_BLOCK_ITEMS.put(
            collectionId,
            ITEMS.register("box_block_" + collectionId, () -> new BoxBlockItem(ModBlocks.BOX_BLOCK.get(), new Item.Properties(), collectionId))
         );
      }
   }
}
