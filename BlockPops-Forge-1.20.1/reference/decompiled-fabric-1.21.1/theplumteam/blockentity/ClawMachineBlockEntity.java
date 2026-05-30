package com.theplumteam.blockentity;

import com.theplumteam.registry.ModBlockEntities;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_2535;
import net.minecraft.class_2586;
import net.minecraft.class_2591;
import net.minecraft.class_2596;
import net.minecraft.class_2602;
import net.minecraft.class_2622;
import net.minecraft.class_2680;
import net.minecraft.class_7225.class_7874;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ClawMachineBlockEntity extends class_2586 implements GeoBlockEntity {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.claw_machine_block.idle");
   private String collectionId = "";

   public ClawMachineBlockEntity(class_2338 pos, class_2680 blockState) {
      super((class_2591)ModBlockEntities.CLAW_MACHINE_BLOCK.get(), pos, blockState);
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController(this, "controller", 0, state -> state.setAndContinue(IDLE_ANIMATION)));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   protected void method_11007(class_2487 tag, class_7874 registries) {
      super.method_11007(tag, registries);
      tag.method_10582("CollectionId", this.collectionId);
   }

   protected void method_11014(class_2487 tag, class_7874 registries) {
      super.method_11014(tag, registries);
      if (tag.method_10545("CollectionId")) {
         this.collectionId = tag.method_10558("CollectionId");
      }
   }

   public class_2487 method_16887(class_7874 registries) {
      class_2487 tag = super.method_16887(registries);
      this.method_11007(tag, registries);
      return tag;
   }

   public void handleUpdateTag(class_2487 tag, class_7874 registries) {
      this.method_11014(tag, registries);
   }

   public class_2596<class_2602> method_38235() {
      return class_2622.method_38585(this);
   }

   public void onDataPacket(class_2535 connection, class_2622 packet, class_7874 registries) {
      class_2487 tag = packet.method_11290();
      if (tag != null) {
         this.method_11014(tag, registries);
         if (this.field_11863 != null && this.field_11863.field_9236) {
            this.field_11863.method_8413(this.method_11016(), this.method_11010(), this.method_11010(), 3);
         }
      }
   }

   public static <T extends class_2586> void tick(class_1937 level, class_2338 pos, class_2680 state, T blockEntity) {
      if (level.field_9236 && blockEntity instanceof ClawMachineBlockEntity) {
      }
   }

   public String getCollectionId() {
      return this.collectionId;
   }

   public void setCollectionId(String collectionId) {
      this.collectionId = collectionId;
      this.method_5431();
   }

   public void loadFromItemNbt(class_2487 tag) {
      if (tag.method_10545("CollectionId")) {
         this.collectionId = tag.method_10558("CollectionId");
      }
   }
}
