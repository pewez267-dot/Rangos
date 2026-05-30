package com.theplumteam.registry;

import com.theplumteam.block.PopBlockColor;
import com.theplumteam.figure.BuiltInCollections;
import com.theplumteam.item.BoxBlockItem;
import com.theplumteam.item.GeoBlockItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_1792;
import net.minecraft.class_2248;
import net.minecraft.class_7924;
import net.minecraft.class_1792.class_1793;

public class ModItems {
   public static final DeferredRegister<class_1792> ITEMS = DeferredRegister.create("blockpops", class_7924.field_41197);
   public static final Map<String, RegistrySupplier<class_1792>> BOX_BLOCK_ITEMS = new HashMap<>();
   public static final Map<PopBlockColor, RegistrySupplier<class_1792>> DEFAULT_BOX_BLOCK_ITEMS = new HashMap<>();
   public static final RegistrySupplier<class_1792> CLAW_MACHINE_BLOCK_ITEM = ITEMS.register(
      "claw_machine_block", () -> new GeoBlockItem((class_2248)ModBlocks.CLAW_MACHINE_BLOCK.get(), new class_1793())
   );
   public static final RegistrySupplier<class_1792> FIGURE_BLOCK_ITEM = ITEMS.register(
      "figure_block", () -> new GeoBlockItem((class_2248)ModBlocks.FIGURE_BLOCK.get(), new class_1793())
   );

   public static void register() {
      ITEMS.register();
   }

   static {
      for (PopBlockColor color : PopBlockColor.values()) {
         DEFAULT_BOX_BLOCK_ITEMS.put(
            color, ITEMS.register("box_block_" + color.method_15434(), () -> new BoxBlockItem((class_2248)ModBlocks.BOX_BLOCK.get(), new class_1793(), color))
         );
      }

      for (String collectionId : BuiltInCollections.COLLECTION_IDS) {
         BOX_BLOCK_ITEMS.put(
            collectionId,
            ITEMS.register("box_block_" + collectionId, () -> new BoxBlockItem((class_2248)ModBlocks.BOX_BLOCK.get(), new class_1793(), collectionId))
         );
      }
   }
}
