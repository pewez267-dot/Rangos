package com.theplumteam.registry;

import com.theplumteam.block.BoxBlock;
import com.theplumteam.block.ClawMachineBlock;
import com.theplumteam.block.FigureBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.class_2248;
import net.minecraft.class_7924;
import net.minecraft.class_4970.class_2251;

public class ModBlocks {
   public static final DeferredRegister<class_2248> BLOCKS = DeferredRegister.create("blockpops", class_7924.field_41254);
   public static final RegistrySupplier<class_2248> BOX_BLOCK = BLOCKS.register(
      "box_block", () -> new BoxBlock(class_2251.method_9637().method_9629(0.5F, 1.0F).method_22488())
   );
   public static final RegistrySupplier<class_2248> CLAW_MACHINE_BLOCK = BLOCKS.register(
      "claw_machine_block", () -> new ClawMachineBlock(class_2251.method_9637().method_9629(1.5F, 6.0F).method_29292().method_22488())
   );
   public static final RegistrySupplier<class_2248> FIGURE_BLOCK = BLOCKS.register(
      "figure_block", () -> new FigureBlock(class_2251.method_9637().method_9629(0.5F, 1.0F).method_22488())
   );

   public static void register() {
      BLOCKS.register();
   }
}
