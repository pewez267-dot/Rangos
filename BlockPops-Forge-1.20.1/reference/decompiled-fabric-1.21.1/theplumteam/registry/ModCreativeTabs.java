package com.theplumteam.registry;

import com.theplumteam.block.PopBlockColor;
import com.theplumteam.figure.BuiltInCollections;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.class_1761;
import net.minecraft.class_1792;
import net.minecraft.class_1935;
import net.minecraft.class_2561;
import net.minecraft.class_7924;
import net.minecraft.class_1761.class_7915;

public class ModCreativeTabs {
   public static final DeferredRegister<class_1761> CREATIVE_TABS = DeferredRegister.create("blockpops", class_7924.field_44688);
   public static final RegistrySupplier<class_1761> BLOCKPOPS_TAB = CREATIVE_TABS.register(
      "blockpops_tab",
      () -> class_1761.method_47307(class_7915.field_41049, 0)
            .method_47321(class_2561.method_43471("itemGroup.blockpops.blockpops_tab"))
            .method_47320(() -> ((class_1792)ModItems.DEFAULT_BOX_BLOCK_ITEMS.get(PopBlockColor.ORIGINAL).get()).method_7854())
            .method_47317((parameters, output) -> {
               output.method_45421((class_1935)ModItems.CLAW_MACHINE_BLOCK_ITEM.get());
               output.method_45420(((class_1792)ModItems.DEFAULT_BOX_BLOCK_ITEMS.get(PopBlockColor.ORIGINAL).get()).method_7854());

               for (String collectionId : BuiltInCollections.COLLECTION_IDS) {
                  if (ModItems.BOX_BLOCK_ITEMS.containsKey(collectionId)) {
                     output.method_45420(((class_1792)ModItems.BOX_BLOCK_ITEMS.get(collectionId).get()).method_7854());
                  }
               }
            })
            .method_47324()
   );

   public static void register() {
      CREATIVE_TABS.register();
   }
}
