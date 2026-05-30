package com.theplumteam.blockentity;

import com.theplumteam.block.PopBlockColor;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureDefinition;
import com.theplumteam.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BoxBlockEntity extends BlockEntity implements GeoBlockEntity {
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

   public BoxBlockEntity(BlockPos pos, BlockState blockState) {
      super(ModBlockEntities.BOX_BLOCK.get(), pos, blockState);
   }

   /** Mark changed and push a block update to clients (server side only). */
   private void notifyUpdate() {
      this.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
      }
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "box_controller", 0, state -> {
         if (this.getBlockPos().equals(BlockPos.ZERO)) {
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
         new AnimationController<>(
            this,
            "figure_pose_controller",
            5,
            state -> this.poseIndex == 1
                  ? state.setAndContinue(RawAnimation.begin().thenLoop("Pose_Sit"))
                  : state.setAndContinue(RawAnimation.begin().thenLoop("Pose_Stand"))
         )
      );
   }

   @Override
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
      this.notifyUpdate();
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
      this.notifyUpdate();
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
      this.notifyUpdate();
   }

   public void setFigureScale(double scale) {
      this.figureScale = scale;
      this.notifyUpdate();
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
      this.notifyUpdate();
   }

   public void setHitboxScale(double scaleX, double scaleY, double scaleZ) {
      this.hitboxScaleX = scaleX;
      this.hitboxScaleY = scaleY;
      this.hitboxScaleZ = scaleZ;
      this.notifyUpdate();
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
      this.notifyUpdate();
   }

   public void setLogoScale(Double scaleX, Double scaleY, Double scaleZ) {
      this.logoScaleX = scaleX;
      this.logoScaleY = scaleY;
      this.logoScaleZ = scaleZ;
      this.notifyUpdate();
   }

   public void setCollectionIdOverride(String collectionId) {
      this.collectionIdOverride = collectionId;
      this.setChanged();
   }

   public void setColorOverride(String color) {
      this.colorOverride = color;
      this.setChanged();
   }

   public int getAlternativeSkinIndex() {
      return this.alternativeSkinIndex;
   }

   public String getSkinSnapshot() {
      return this.skinSnapshot;
   }

   public void setSkinSnapshot(String skinSnapshot) {
      this.skinSnapshot = skinSnapshot;
      this.notifyUpdate();
   }

   public String getQuickSkinId() {
      return this.quickSkinId;
   }

   public void setQuickSkinId(String quickSkinId) {
      this.quickSkinId = quickSkinId;
      this.notifyUpdate();
   }

   public void cycleAlternativeSkin() {
      FigureDefinition def = this.getFigureDefinition();
      if (def != null && def.hasAlternatives()) {
         int totalSkins = 1 + def.getAlternatives().size();
         this.alternativeSkinIndex = (this.alternativeSkinIndex + 1) % totalSkins;
         this.notifyUpdate();
      }
   }

   public int getPoseIndex() {
      return this.poseIndex;
   }

   public void cyclePose() {
      FigureDefinition def = this.getFigureDefinition();
      if (def == null || !def.isPoseLocked()) {
         this.poseIndex = (this.poseIndex + 1) % 2;
         this.notifyUpdate();
      }
   }

   public boolean isOpen() {
      return this.isOpen;
   }

   public void toggleOpen() {
      if (this.level != null && !this.level.isClientSide) {
         this.isOpen = !this.isOpen;
         this.setChanged();
         this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
      }
   }

   @Deprecated
   public void triggerOpenAnimation() {
      if (this.level != null && !this.level.isClientSide && !this.isOpen) {
         this.toggleOpen();
      }
   }

   @Override
   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putBoolean("IsOpen", this.isOpen);
      tag.putString("FigureId", this.figureId);
      tag.putBoolean("IsFigureExtracted", this.isFigureExtracted);
      tag.putInt("AlternativeSkinIndex", this.alternativeSkinIndex);
      tag.putInt("PoseIndex", this.poseIndex);
      if (this.skinSnapshot != null) {
         tag.putString("SkinSnapshot", this.skinSnapshot);
      }

      if (this.quickSkinId != null) {
         tag.putString("QuickSkinId", this.quickSkinId);
      }

      if (this.collectionIdOverride != null) {
         tag.putString("CollectionId", this.collectionIdOverride);
      }

      if (this.colorOverride != null) {
         tag.putString("Color", this.colorOverride);
      }

      tag.putDouble("FigureOffsetX", this.figureOffsetX);
      tag.putDouble("FigureOffsetY", this.figureOffsetY);
      tag.putDouble("FigureOffsetZ", this.figureOffsetZ);
      tag.putDouble("FigureScale", this.figureScale);
      tag.putDouble("HitboxOffsetX", this.hitboxOffsetX);
      tag.putDouble("HitboxOffsetY", this.hitboxOffsetY);
      tag.putDouble("HitboxOffsetZ", this.hitboxOffsetZ);
      tag.putDouble("HitboxScaleX", this.hitboxScaleX);
      tag.putDouble("HitboxScaleY", this.hitboxScaleY);
      tag.putDouble("HitboxScaleZ", this.hitboxScaleZ);
      if (this.logoPositionX != null) {
         tag.putDouble("LogoPositionX", this.logoPositionX);
      }

      if (this.logoPositionY != null) {
         tag.putDouble("LogoPositionY", this.logoPositionY);
      }

      if (this.logoPositionZ != null) {
         tag.putDouble("LogoPositionZ", this.logoPositionZ);
      }

      if (this.logoScaleX != null) {
         tag.putDouble("LogoScaleX", this.logoScaleX);
      }

      if (this.logoScaleY != null) {
         tag.putDouble("LogoScaleY", this.logoScaleY);
      }

      tag.putBoolean("HideLogo", this.hideLogo);
   }

   @Override
   public void load(CompoundTag tag) {
      super.load(tag);
      this.isOpen = tag.contains("IsOpen") && tag.getBoolean("IsOpen");
      if (tag.contains("FigureId")) {
         this.figureId = tag.getString("FigureId");
      }

      if (tag.contains("IsFigureExtracted")) {
         this.isFigureExtracted = tag.getBoolean("IsFigureExtracted");
      }

      if (tag.contains("AlternativeSkinIndex")) {
         this.alternativeSkinIndex = tag.getInt("AlternativeSkinIndex");
      }

      if (tag.contains("PoseIndex")) {
         this.poseIndex = tag.getInt("PoseIndex");
      }

      this.skinSnapshot = tag.contains("SkinSnapshot", Tag.TAG_STRING) ? tag.getString("SkinSnapshot") : null;
      this.quickSkinId = tag.contains("QuickSkinId", Tag.TAG_STRING) ? tag.getString("QuickSkinId") : null;
      if (tag.contains("CollectionId")) {
         this.collectionIdOverride = tag.getString("CollectionId");
      }

      if (tag.contains("Color")) {
         this.colorOverride = tag.getString("Color");
      }

      if (tag.contains("FigureOffsetX")) {
         this.figureOffsetX = tag.getDouble("FigureOffsetX");
      }

      if (tag.contains("FigureOffsetY")) {
         this.figureOffsetY = tag.getDouble("FigureOffsetY");
      }

      if (tag.contains("FigureOffsetZ")) {
         this.figureOffsetZ = tag.getDouble("FigureOffsetZ");
      }

      if (tag.contains("FigureScale")) {
         this.figureScale = tag.getDouble("FigureScale");
      }

      if (tag.contains("HitboxOffsetX")) {
         this.hitboxOffsetX = tag.getDouble("HitboxOffsetX");
      }

      if (tag.contains("HitboxOffsetY")) {
         this.hitboxOffsetY = tag.getDouble("HitboxOffsetY");
      }

      if (tag.contains("HitboxOffsetZ")) {
         this.hitboxOffsetZ = tag.getDouble("HitboxOffsetZ");
      }

      if (tag.contains("HitboxScaleX")) {
         this.hitboxScaleX = tag.getDouble("HitboxScaleX");
      }

      if (tag.contains("HitboxScaleY")) {
         this.hitboxScaleY = tag.getDouble("HitboxScaleY");
      }

      if (tag.contains("HitboxScaleZ")) {
         this.hitboxScaleZ = tag.getDouble("HitboxScaleZ");
      }

      this.logoPositionX = tag.contains("LogoPositionX") ? tag.getDouble("LogoPositionX") : null;
      this.logoPositionY = tag.contains("LogoPositionY") ? tag.getDouble("LogoPositionY") : null;
      this.logoPositionZ = tag.contains("LogoPositionZ") ? tag.getDouble("LogoPositionZ") : null;
      this.logoScaleX = tag.contains("LogoScaleX") ? tag.getDouble("LogoScaleX") : null;
      this.logoScaleY = tag.contains("LogoScaleY") ? tag.getDouble("LogoScaleY") : null;
      if (tag.contains("HideLogo")) {
         this.hideLogo = tag.getBoolean("HideLogo");
      }

      this.applyDefinitionDefaults();
   }

   private void applyDefinitionDefaults() {
      FigureDefinition def = this.getFigureDefinition();
      if (def != null) {
         if (def.getOffsetX() != 0.0F && this.figureOffsetX == -0.53) {
            this.figureOffsetX = def.getOffsetX();
         }

         if (def.getOffsetZ() != 0.0F && this.figureOffsetZ == -0.55) {
            this.figureOffsetZ = def.getOffsetZ();
         }
      }
   }

   public void loadFromItemNbt(CompoundTag tag) {
      this.isOpen = tag.contains("IsOpen") && tag.getBoolean("IsOpen");
      if (tag.contains("FigureId")) {
         this.figureId = tag.getString("FigureId");
      }

      if (tag.contains("IsFigureExtracted")) {
         this.isFigureExtracted = tag.getBoolean("IsFigureExtracted");
      }

      if (tag.contains("AlternativeSkinIndex")) {
         this.alternativeSkinIndex = tag.getInt("AlternativeSkinIndex");
      }

      if (tag.contains("PoseIndex")) {
         this.poseIndex = tag.getInt("PoseIndex");
      }

      this.skinSnapshot = tag.contains("SkinSnapshot", Tag.TAG_STRING) ? tag.getString("SkinSnapshot") : null;
      this.quickSkinId = tag.contains("QuickSkinId", Tag.TAG_STRING) ? tag.getString("QuickSkinId") : null;
      if (tag.contains("CollectionId")) {
         this.collectionIdOverride = tag.getString("CollectionId");
      }

      if (tag.contains("Color")) {
         this.colorOverride = tag.getString("Color");
      }

      if (tag.contains("FigureOffsetX")) {
         this.figureOffsetX = tag.getDouble("FigureOffsetX");
      }

      if (tag.contains("FigureOffsetY")) {
         this.figureOffsetY = tag.getDouble("FigureOffsetY");
      }

      if (tag.contains("FigureOffsetZ")) {
         this.figureOffsetZ = tag.getDouble("FigureOffsetZ");
      }

      if (tag.contains("FigureScale")) {
         this.figureScale = tag.getDouble("FigureScale");
      }

      if (tag.contains("HideLogo")) {
         this.hideLogo = tag.getBoolean("HideLogo");
      }

      this.applyDefinitionDefaults();
   }

   @Override
   public CompoundTag getUpdateTag() {
      CompoundTag tag = super.getUpdateTag();
      this.saveAdditional(tag);
      return tag;
   }

   @Override
   public void handleUpdateTag(CompoundTag tag) {
      this.load(tag);
   }

   @Override
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
      CompoundTag tag = packet.getTag();
      if (tag != null) {
         this.load(tag);
      }
   }

   public void saveToItem(ItemStack stack) {
      CompoundTag tag = new CompoundTag();
      this.saveAdditional(tag);
      tag.putBoolean("IsOpen", false);
      BlockItem.setBlockEntityData(stack, this.getType(), tag);
   }

   public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
      if (level.isClientSide && blockEntity instanceof BoxBlockEntity boxBlockEntity && boxBlockEntity.transitionTicks > 0) {
         boxBlockEntity.transitionTicks--;
         if (boxBlockEntity.transitionTicks == 0) {
            boxBlockEntity.isTransitioning = false;
         }
      }
   }
}
