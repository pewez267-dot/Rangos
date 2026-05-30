package com.theplumteam.registry;

import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.blockentity.ClawMachineBlockEntity;
import com.theplumteam.blockentity.FigureBlockEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.class_2248;
import net.minecraft.class_2591;
import net.minecraft.class_7924;
import net.minecraft.class_2591.class_2592;

public class ModBlockEntities {
   public static final DeferredRegister<class_2591<?>> BLOCK_ENTITIES = DeferredRegister.create("blockpops", class_7924.field_41255);
   public static final RegistrySupplier<class_2591<BoxBlockEntity>> BOX_BLOCK = BLOCK_ENTITIES.register(
      "box_block", () -> class_2592.method_20528(BoxBlockEntity::new, new class_2248[]{(class_2248)ModBlocks.BOX_BLOCK.get()}).method_11034(null)
   );
   public static final RegistrySupplier<class_2591<ClawMachineBlockEntity>> CLAW_MACHINE_BLOCK = BLOCK_ENTITIES.register(
      "claw_machine_block",
      () -> class_2592.method_20528(ClawMachineBlockEntity::new, new class_2248[]{(class_2248)ModBlocks.CLAW_MACHINE_BLOCK.get()}).method_11034(null)
   );
   public static final RegistrySupplier<class_2591<FigureBlockEntity>> FIGURE_BLOCK = BLOCK_ENTITIES.register(
      "figure_block", () -> class_2592.method_20528(FigureBlockEntity::new, new class_2248[]{(class_2248)ModBlocks.FIGURE_BLOCK.get()}).method_11034(null)
   );

   public static void register() {
      BLOCK_ENTITIES.register();
   }
}
