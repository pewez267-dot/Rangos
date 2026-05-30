package com.theplumteam.blockentity;

import com.theplumteam.block.PopBlockColor;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BoxBlockEntity extends class_2586 implements GeoBlockEntity {
   private static final Logger LOGGER = LoggerFactory.getLogger(BoxBlockEntity.class);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.box_block.idle");
   private static final RawAnimation OPEN_ANIMATION = RawAnimation.begin().thenPlay("animation.box_block.open").thenLoop("animation.box_block.open_state");
   private static final RawAnimation OPEN_STATE_ANIMATION = RawAnimation.begin().thenLoop("animation.box_block.open_state");
   private static final RawAnimation CLOSE_ANIMATION = RawAnimation.begin().thenPlay("animation.box_block.close").thenLoop("animation.box_block.idle");
   private boolean isOpen = false;
   private transient boolean wasOpen = false;
   private transient boolean isTransitioning = false;
   private transient int transitionTicks = 0;
   private static final int TRANSITION_DURATION = 11;
   private String figureId = "";
   private String collectionIdOverride = null;
   private String colorOverride = null;
   private boolean isFigureExtracted = false;
   private int alternativeSkinIndex = 0;
   private String skinSnapshot = null;
   private String quickSkinId = null;
   private int poseIndex = 0;
   private double figureOffsetX = -0.53;
   private double figureOffsetY = 0.01;
   private double figureOffsetZ = -0.55;
   private double figureScale = 1.0;
   private double hitboxOffsetX = 0.0;
   private double hitboxOffsetY = 0.006;
   private double hitboxOffsetZ = 0.0;
   private double hitboxScaleX = 1.1;
   private double hitboxScaleY = 1.0;
   private double hitboxScaleZ = 0.9;
   private Double logoPositionX = null;
   private Double logoPositionY = null;
   private Double logoPositionZ = null;
   private Double logoScaleX = null;
   private Double logoScaleY = null;
   private Double logoScaleZ = null;
   private boolean hideLogo = false;

   public BoxBlockEntity(class_2338 pos, class_2680 blockState) {
      super((class_2591)ModBlockEntities.BOX_BLOCK.get(), pos, blockState);
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController(this, "box_controller", 0, state -> {
         if (this.method_11016().equals(class_2338.field_10980)) {
            return PlayState.STOP;
         } else if (this.isTransitioning) {
            return PlayState.CONTINUE;
         } else if (this.isOpen != this.wasOpen) {
            this.wasOpen = this.isOpen;
            this.isTransitioning = true;
            this.transitionTicks = 11;
            return this.isOpen ? state.setAndContinue(OPEN_ANIMATION) : state.setAndContinue(CLOSE_ANIMATION);
         } else {
            return this.isOpen ? state.setAndContinue(OPEN_STATE_ANIMATION) : state.setAndContinue(IDLE_ANIMATION);
         }
      }).setAnimationSpeed(1.2));
      controllers.add(
         new AnimationController(
            this,
            "figure_pose_controller",
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
      return this.collectionIdOverride != null && !this.collectionIdOverride.isEmpty()
         ? this.collectionIdOverride
         : CollectionRegistry.getDefaultCollection().map(collection -> collection.getId()).orElse("");
   }

   @Nullable
   public PopBlockColor getColor() {
      if (this.colorOverride != null && !this.colorOverride.isEmpty()) {
         try {
            return PopBlockColor.valueOf(this.colorOverride.toUpperCase());
         } catch (IllegalArgumentException var2) {
            return null;
         }
      } else {
         return null;
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
      return this.figureId.isEmpty() ? null : CollectionRegistry.getFigure(this.getCollectionId(), this.figureId).orElse(null);
   }

   public boolean hasFigure() {
      return !this.figureId.isEmpty() && this.getFigureDefinition() != null;
   }

   public boolean isFigureExtracted() {
      return this.isFigureExtracted;
   }

   public void setFigureExtracted(boolean extracted) {
      this.isFigureExtracted = extracted;
      this.method_5431();
      if (this.field_11863 != null && !this.field_11863.field_9236) {
         this.field_11863.method_8413(this.method_11016(), this.method_11010(), this.method_11010(), 3);
      }
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

   public double getHitboxOffsetX() {
      return this.hitboxOffsetX;
   }

   public double getHitboxOffsetY() {
      return this.hitboxOffsetY;
   }

   public double getHitboxOffsetZ() {
      return this.hitboxOffsetZ;
   }

   public double getHitboxScaleX() {
      return this.hitboxScaleX;
   }

   public double getHitboxScaleY() {
      return this.hitboxScaleY;
   }

   public double getHitboxScaleZ() {
      return this.hitboxScaleZ;
   }

   public void setHitboxOffset(double x, double y, double z) {
      this.hitboxOffsetX = x;
      this.hitboxOffsetY = y;
      this.hitboxOffsetZ = z;
      this.method_5431();
      if (this.field_11863 != null && !this.field_11863.field_9236) {
         this.field_11863.method_8413(this.method_11016(), this.method_11010(), this.method_11010(), 3);
      }
   }

   public void setHitboxScale(double scaleX, double scaleY, double scaleZ) {
      this.hitboxScaleX = scaleX;
      this.hitboxScaleY = scaleY;
      this.hitboxScaleZ = scaleZ;
      this.method_5431();
      if (this.field_11863 != null && !this.field_11863.field_9236) {
         this.field_11863.method_8413(this.method_11016(), this.method_11010(), this.method_11010(), 3);
      }
   }

   public Double getLogoPositionX() {
      return this.logoPositionX;
   }

   public Double getLogoPositionY() {
      return this.logoPositionY;
   }

   public Double getLogoPositionZ() {
      return this.logoPositionZ;
   }

   public Double getLogoScaleX() {
      return this.logoScaleX;
   }

   public Double getLogoScaleY() {
      return this.logoScaleY;
   }

   public Double getLogoScaleZ() {
      return this.logoScaleZ;
   }

   public boolean isHideLogo() {
      return this.hideLogo;
   }

   public void setLogoPosition(Double x, Double y, Double z) {
      this.logoPositionX = x;
      this.logoPositionY = y;
      this.logoPositionZ = z;
      this.method_5431();
      if (this.field_11863 != null && !this.field_11863.field_9236) {
         this.field_11863.method_8413(this.method_11016(), this.method_11010(), this.method_11010(), 3);
      }
   }

   public void setLogoScale(Double scaleX, Double scaleY, Double scaleZ) {
      this.logoScaleX = scaleX;
      this.logoScaleY = scaleY;
      this.logoScaleZ = scaleZ;
      this.method_5431();
      if (this.field_11863 != null && !this.field_11863.field_9236) {
         this.field_11863.method_8413(this.method_11016(), this.method_11010(), this.method_11010(), 3);
      }
   }

   public void setCollectionIdOverride(String collectionId) {
      this.collectionIdOverride = collectionId;
      this.method_5431();
   }

   public void setColorOverride(String color) {
      this.colorOverride = color;
      this.method_5431();
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

   public boolean isOpen() {
      return this.isOpen;
   }

   public void toggleOpen() {
      if (this.field_11863 != null && !this.field_11863.field_9236) {
         this.isOpen = !this.isOpen;
         this.method_5431();
         this.field_11863.method_8413(this.method_11016(), this.method_11010(), this.method_11010(), 3);
      }
   }

   @Deprecated
   public void triggerOpenAnimation() {
      if (this.field_11863 != null && !this.field_11863.field_9236 && !this.isOpen) {
         this.toggleOpen();
      }
   }

   protected void method_11007(class_2487 tag, class_7874 registries) {
      super.method_11007(tag, registries);
      tag.method_10556("IsOpen", this.isOpen);
      tag.method_10582("FigureId", this.figureId);
      tag.method_10556("IsFigureExtracted", this.isFigureExtracted);
      tag.method_10569("AlternativeSkinIndex", this.alternativeSkinIndex);
      tag.method_10569("PoseIndex", this.poseIndex);
      if (this.skinSnapshot != null) {
         tag.method_10582("SkinSnapshot", this.skinSnapshot);
      }

      if (this.quickSkinId != null) {
         tag.method_10582("QuickSkinId", this.quickSkinId);
      }

      if (this.collectionIdOverride != null) {
         tag.method_10582("CollectionId", this.collectionIdOverride);
      }

      if (this.colorOverride != null) {
         tag.method_10582("Color", this.colorOverride);
      }

      tag.method_10549("FigureOffsetX", this.figureOffsetX);
      tag.method_10549("FigureOffsetY", this.figureOffsetY);
      tag.method_10549("FigureOffsetZ", this.figureOffsetZ);
      tag.method_10549("FigureScale", this.figureScale);
      tag.method_10549("HitboxOffsetX", this.hitboxOffsetX);
      tag.method_10549("HitboxOffsetY", this.hitboxOffsetY);
      tag.method_10549("HitboxOffsetZ", this.hitboxOffsetZ);
      tag.method_10549("HitboxScaleX", this.hitboxScaleX);
      tag.method_10549("HitboxScaleY", this.hitboxScaleY);
      tag.method_10549("HitboxScaleZ", this.hitboxScaleZ);
      if (this.logoPositionX != null) {
         tag.method_10549("LogoPositionX", this.logoPositionX);
      }

      if (this.logoPositionY != null) {
         tag.method_10549("LogoPositionY", this.logoPositionY);
      }

      if (this.logoPositionZ != null) {
         tag.method_10549("LogoPositionZ", this.logoPositionZ);
      }

      if (this.logoScaleX != null) {
         tag.method_10549("LogoScaleX", this.logoScaleX);
      }

      if (this.logoScaleY != null) {
         tag.method_10549("LogoScaleY", this.logoScaleY);
      }

      tag.method_10556("HideLogo", this.hideLogo);
   }

   protected void method_11014(class_2487 tag, class_7874 registries) {
      super.method_11014(tag, registries);
      this.isOpen = tag.method_10545("IsOpen") ? tag.method_10577("IsOpen") : false;
      if (tag.method_10545("FigureId")) {
         this.figureId = tag.method_10558("FigureId");
      }

      if (tag.method_10545("IsFigureExtracted")) {
         this.isFigureExtracted = tag.method_10577("IsFigureExtracted");
      }

      if (tag.method_10545("AlternativeSkinIndex")) {
         this.alternativeSkinIndex = tag.method_10550("AlternativeSkinIndex");
      }

      if (tag.method_10545("PoseIndex")) {
         this.poseIndex = tag.method_10550("PoseIndex");
      }

      this.skinSnapshot = tag.method_10573("SkinSnapshot", 8) ? tag.method_10558("SkinSnapshot") : null;
      this.quickSkinId = tag.method_10573("QuickSkinId", 8) ? tag.method_10558("QuickSkinId") : null;
      if (tag.method_10545("CollectionId")) {
         this.collectionIdOverride = tag.method_10558("CollectionId");
      }

      if (tag.method_10545("Color")) {
         this.colorOverride = tag.method_10558("Color");
      }

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

      if (tag.method_10545("HitboxOffsetX")) {
         this.hitboxOffsetX = tag.method_10574("HitboxOffsetX");
      }

      if (tag.method_10545("HitboxOffsetY")) {
         this.hitboxOffsetY = tag.method_10574("HitboxOffsetY");
      }

      if (tag.method_10545("HitboxOffsetZ")) {
         this.hitboxOffsetZ = tag.method_10574("HitboxOffsetZ");
      }

      if (tag.method_10545("HitboxScaleX")) {
         this.hitboxScaleX = tag.method_10574("HitboxScaleX");
      }

      if (tag.method_10545("HitboxScaleY")) {
         this.hitboxScaleY = tag.method_10574("HitboxScaleY");
      }

      if (tag.method_10545("HitboxScaleZ")) {
         this.hitboxScaleZ = tag.method_10574("HitboxScaleZ");
      }

      this.logoPositionX = tag.method_10545("LogoPositionX") ? tag.method_10574("LogoPositionX") : null;
      this.logoPositionY = tag.method_10545("LogoPositionY") ? tag.method_10574("LogoPositionY") : null;
      this.logoPositionZ = tag.method_10545("LogoPositionZ") ? tag.method_10574("LogoPositionZ") : null;
      this.logoScaleX = tag.method_10545("LogoScaleX") ? tag.method_10574("LogoScaleX") : null;
      this.logoScaleY = tag.method_10545("LogoScaleY") ? tag.method_10574("LogoScaleY") : null;
      if (tag.method_10545("HideLogo")) {
         this.hideLogo = tag.method_10577("HideLogo");
      }

      this.applyDefinitionDefaults();
   }

   private void applyDefinitionDefaults() {
      FigureDefinition def = this.getFigureDefinition();
      if (def != null) {
         if (def.getOffsetX() != 0.0F && this.figureOffsetX == -0.53) {
            this.figureOffsetX = (double)def.getOffsetX();
         }

         if (def.getOffsetZ() != 0.0F && this.figureOffsetZ == -0.55) {
            this.figureOffsetZ = (double)def.getOffsetZ();
         }
      }
   }

   public void loadFromItemNbt(class_2487 tag) {
      this.isOpen = tag.method_10545("IsOpen") ? tag.method_10577("IsOpen") : false;
      if (tag.method_10545("FigureId")) {
         this.figureId = tag.method_10558("FigureId");
      }

      if (tag.method_10545("IsFigureExtracted")) {
         this.isFigureExtracted = tag.method_10577("IsFigureExtracted");
      }

      if (tag.method_10545("AlternativeSkinIndex")) {
         this.alternativeSkinIndex = tag.method_10550("AlternativeSkinIndex");
      }

      if (tag.method_10545("PoseIndex")) {
         this.poseIndex = tag.method_10550("PoseIndex");
      }

      this.skinSnapshot = tag.method_10573("SkinSnapshot", 8) ? tag.method_10558("SkinSnapshot") : null;
      this.quickSkinId = tag.method_10573("QuickSkinId", 8) ? tag.method_10558("QuickSkinId") : null;
      if (tag.method_10545("CollectionId")) {
         this.collectionIdOverride = tag.method_10558("CollectionId");
      }

      if (tag.method_10545("Color")) {
         this.colorOverride = tag.method_10558("Color");
      }

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

      if (tag.method_10545("HideLogo")) {
         this.hideLogo = tag.method_10577("HideLogo");
      }

      this.applyDefinitionDefaults();
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
      }
   }

   public void saveToItem(class_1799 stack) {
      class_2487 tag = new class_2487();
      if (this.field_11863 != null) {
         this.method_11007(tag, this.field_11863.method_30349());
      }

      tag.method_10556("IsOpen", false);
      tag.method_10582("id", "blockpops:box_block");
      stack.method_57379(class_9334.field_49611, class_9279.method_57456(tag));
   }

   public static <T extends class_2586> void tick(class_1937 level, class_2338 pos, class_2680 state, T blockEntity) {
      if (level.field_9236 && blockEntity instanceof BoxBlockEntity boxBlockEntity && boxBlockEntity.transitionTicks > 0) {
         boxBlockEntity.transitionTicks--;
         if (boxBlockEntity.transitionTicks == 0) {
            boxBlockEntity.isTransitioning = false;
         }
      }
   }
}
