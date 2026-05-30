package com.theplumteam.client.model;

import com.theplumteam.blockentity.ClawMachineBlockEntity;
import net.minecraft.class_2960;
import software.bernie.geckolib.model.GeoModel;

public class ClawMachineBlockModel extends GeoModel<ClawMachineBlockEntity> {
   private static final class_2960 MODEL = class_2960.method_60655("blockpops", "geo/block/claw_machine_block.geo.json");
   private static final class_2960 TEXTURE = class_2960.method_60655("blockpops", "textures/block/claw_machine_block.png");
   private static final class_2960 ANIMATION = class_2960.method_60655("blockpops", "animations/block/claw_machine_block.animation.json");

   public class_2960 getModelResource(ClawMachineBlockEntity animatable) {
      return MODEL;
   }

   public class_2960 getTextureResource(ClawMachineBlockEntity animatable) {
      return TEXTURE;
   }

   public class_2960 getAnimationResource(ClawMachineBlockEntity animatable) {
      return ANIMATION;
   }
}
