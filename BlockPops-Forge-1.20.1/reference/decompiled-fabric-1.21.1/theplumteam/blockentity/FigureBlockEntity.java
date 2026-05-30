package com.theplumteam.blockentity;

import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureDefinition;
import com.theplumteam.registry.ModBlockEntities;
import net.minecraft.class_1799;
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
import net.minecraft.class_9279;
import net.minecraft.class_9334;
import net.minecraft.class_7225.class_7874;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FigureBlockEntity extends class_2586 implements GeoBlockEntity {
   private static final Logger LOGGER = LoggerFactory.getLogger(FigureBlockEntity.class);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private String figureId = "";
   private String collectionId = "";
   private int alternativeSkinIndex = 0;
   private int poseIndex = 0;
   private String skinSnapshot = null;
   private String quickSkinId = null;
   private double figureOffsetX = -0.6;
   private double figureOffsetY = 0.01;
   private double figureOffsetZ = -0.55;
   private double figureScale = 1.0;

   public FigureBlockEntity(class_2338 pos, class_2680 blockState) {
      super((class_2591)ModBlockEntities.FIGURE_BLOCK.get(), pos, blockState);
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(
         new AnimationController(
            this,
            "pose_controller",
            5,
            state -> this.poseIndex == 1
                  ? state.setAndContinue(RawAnimation.begin().thenLoop("Pose_Sit"))
                  : state.setAndContinue(RawAnimation.begin().thenLoop("Pose_Stand"))
         )
      );
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   public String getCollectionId() {
      return this.collectionId;
   }

   public void setCollectionId(String collectionId) {
      this.collectionId = collectionId != null ? collectionId : "";
      this.method_5431();
      if (this.field_11863 != null && !this.field_11863.field_9236) {
         this.field_11863.method_8413(this.method_11016(), this.method_11010(), this.method_11010(), 3);
      }
   }

   public String getFigureId() {
      return this.figureId;
   }

   public void setFigureId(String figureId) {
      this.figureId = figureId != null ? figureId : "";
      this.method_5431();
      if (this.field_11863 != null && !this.field_11863.field_9236) {
         this.field_11863.method_8413(this.method_11016(), this.method_11010(), this.method_11010(), 3);
      }
   }

   public FigureDefinition getFigureDefinition() {
      return !this.figureId.isEmpty() && !this.collectionId.isEmpty() ? CollectionRegistry.getFigure(this.collectionId, this.figureId).orElse(null) : null;
   }

   public boolean hasFigure() {
      return !this.figureId.isEmpty() && !this.collectionId.isEmpty() && this.getFigureDefinition() != null;
   }

   public double getFigureOffsetX() {
      return this.figureOffsetX;
   }

   public double getFigureOffsetY() {
      return this.figureOffsetY;
   }

   public double getFigureOffsetZ() {
      return this.figureOffsetZ;
   }

   public double getFigureScale() {
      return this.figureScale;
   }

   public void setFigureOffset(double x, double y, double z) {
      this.figureOffsetX = x;
      this.figureOffsetY = y;
      this.figureOffsetZ = z;
      this.method_5431();
      if (this.field_11863 != null && !this.field_11863.field_9236) {
         this.field_11863.method_8413(this.method_11016(), this.method_11010(), this.method_11010(), 3);
      }
   }

   public void setFigureScale(double scale) {
      this.figureScale = scale;
      this.method_5431();
      if (this.field_11863 != null && !this.field_11863.field_9236) {
         this.field_11863.method_8413(this.method_11016(), this.method_11010(), this.method_11010(), 3);
      }
   }

   public int getAlternativeSkinIndex() {
      return this.alternativeSkinIndex;
   }

   public String getSkinSnapshot() {
      return this.skinSnapshot;
   }

   public void setSkinSnapshot(String skinSnapshot) {
      this.skinSnapshot = skinSnapshot;
      this.method_5431();
      if (this.field_11863 != null && !this.field_11863.field_9236) {
         this.field_11863.method_8413(this.method_11016(), this.method_11010(), this.method_11010(), 3);
      }
   }

   public String getQuickSkinId() {
      return this.quickSkinId;
   }

   public void setQuickSkinId(String quickSkinId) {
      this.quickSkinId = quickSkinId;
      this.method_5431();
      if (this.field_11863 != null && !this.field_11863.field_9236) {
         this.field_11863.method_8413(this.method_11016(), this.method_11010(), this.method_11010(), 3);
      }
   }

   public void cycleAlternativeSkin() {
      FigureDefinition def = this.getFigureDefinition();
      if (def != null && def.hasAlternatives()) {
         int totalSkins = 1 + def.getAlternatives().size();
         this.alternativeSkinIndex = (this.alternativeSkinIndex + 1) % totalSkins;
         this.method_5431();
         if (this.field_11863 != null && !this.field_11863.field_9236) {
            this.field_11863.method_8413(this.method_11016(), this.method_11010(), this.method_11010(), 3);
         }
      }
   }

   public int getPoseIndex() {
      return this.poseIndex;
   }

   public void cyclePose() {
      FigureDefinition def = this.getFigureDefinition();
      if (def == null || !def.isPoseLocked()) {
         this.poseIndex = (this.poseIndex + 1) % 2;
         this.method_5431();
         if (this.field_11863 != null && !this.field_11863.field_9236) {
            this.field_11863.method_8413(this.method_11016(), this.method_11010(), this.method_11010(), 3);
         }
      }
   }

   protected void method_11007(class_2487 tag, class_7874 registries) {
      super.method_11007(tag, registries);
      tag.method_10582("FigureId", this.figureId);
      tag.method_10582("CollectionId", this.collectionId);
      tag.method_10569("AlternativeSkinIndex", this.alternativeSkinIndex);
      tag.method_10569("PoseIndex", this.poseIndex);
      if (this.skinSnapshot != null) {
         tag.method_10582("SkinSnapshot", this.skinSnapshot);
      }

      if (this.quickSkinId != null) {
         tag.method_10582("QuickSkinId", this.quickSkinId);
      }

      tag.method_10549("FigureOffsetX", this.figureOffsetX);
      tag.method_10549("FigureOffsetY", this.figureOffsetY);
      tag.method_10549("FigureOffsetZ", this.figureOffsetZ);
      tag.method_10549("FigureScale", this.figureScale);
   }

   protected void method_11014(class_2487 tag, class_7874 registries) {
      super.method_11014(tag, registries);
      if (tag.method_10545("FigureId")) {
         this.figureId = tag.method_10558("FigureId");
      }

      if (tag.method_10545("CollectionId")) {
         this.collectionId = tag.method_10558("CollectionId");
      }

      if (tag.method_10545("AlternativeSkinIndex")) {
         this.alternativeSkinIndex = tag.method_10550("AlternativeSkinIndex");
      }

      if (tag.method_10545("PoseIndex")) {
         this.poseIndex = tag.method_10550("PoseIndex");
      }

      this.skinSnapshot = tag.method_10573("SkinSnapshot", 8) ? tag.method_10558("SkinSnapshot") : null;
      this.quickSkinId = tag.method_10573("QuickSkinId", 8) ? tag.method_10558("QuickSkinId") : null;
      if (tag.method_10545("FigureOffsetX")) {
         this.figureOffsetX = tag.method_10574("FigureOffsetX");
      }

      if (tag.method_10545("FigureOffsetY")) {
         this.figureOffsetY = tag.method_10574("FigureOffsetY");
      }

      if (tag.method_10545("FigureOffsetZ")) {
         this.figureOffsetZ = tag.method_10574("FigureOffsetZ");
      }

      if (tag.method_10545("FigureScale")) {
         this.figureScale = tag.method_10574("FigureScale");
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

   public void saveToItem(class_1799 stack) {
      class_2487 tag = new class_2487();
      if (this.field_11863 != null) {
         this.method_11007(tag, this.field_11863.method_30349());
      }

      tag.method_10582("id", "blockpops:figure_block");
      stack.method_57379(class_9334.field_49611, class_9279.method_57456(tag));
   }

   public void loadFromItemNbt(class_2487 tag) {
      if (tag.method_10545("FigureId")) {
         this.figureId = tag.method_10558("FigureId");
      }

      if (tag.method_10545("CollectionId")) {
         this.collectionId = tag.method_10558("CollectionId");
      }

      if (tag.method_10545("AlternativeSkinIndex")) {
         this.alternativeSkinIndex = tag.method_10550("AlternativeSkinIndex");
      }

      if (tag.method_10545("PoseIndex")) {
         this.poseIndex = tag.method_10550("PoseIndex");
      }

      this.skinSnapshot = tag.method_10573("SkinSnapshot", 8) ? tag.method_10558("SkinSnapshot") : null;
      this.quickSkinId = tag.method_10573("QuickSkinId", 8) ? tag.method_10558("QuickSkinId") : null;
      if (tag.method_10545("FigureOffsetX")) {
         this.figureOffsetX = tag.method_10574("FigureOffsetX");
      }

      if (tag.method_10545("FigureOffsetY")) {
         this.figureOffsetY = tag.method_10574("FigureOffsetY");
      }

      if (tag.method_10545("FigureOffsetZ")) {
         this.figureOffsetZ = tag.method_10574("FigureOffsetZ");
      }

      if (tag.method_10545("FigureScale")) {
         this.figureScale = tag.method_10574("FigureScale");
      }
   }

   public static <T extends class_2586> void tick(class_1937 level, class_2338 pos, class_2680 state, T blockEntity) {
      if (level.field_9236 && blockEntity instanceof FigureBlockEntity var4) {
         ;
      }
   }
}
