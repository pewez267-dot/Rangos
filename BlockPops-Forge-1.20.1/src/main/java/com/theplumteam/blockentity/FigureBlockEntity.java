package com.theplumteam.blockentity;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FigureBlockEntity extends BlockEntity implements GeoBlockEntity {
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

   public FigureBlockEntity(BlockPos pos, BlockState blockState) {
      super(ModBlockEntities.FIGURE_BLOCK.get(), pos, blockState);
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(
         new AnimationController<>(
            this,
            "pose_controller",
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

   private void notifyUpdate() {
      this.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public String getCollectionId() {
      return this.collectionId;
   }

   public void setCollectionId(String collectionId) {
      this.collectionId = collectionId != null ? collectionId : "";
      this.notifyUpdate();
   }

   public String getFigureId() {
      return this.figureId;
   }

   public void setFigureId(String figureId) {
      this.figureId = figureId != null ? figureId : "";
      this.notifyUpdate();
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
      this.notifyUpdate();
   }

   public void setFigureScale(double scale) {
      this.figureScale = scale;
      this.notifyUpdate();
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

   @Override
   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putString("FigureId", this.figureId);
      tag.putString("CollectionId", this.collectionId);
      tag.putInt("AlternativeSkinIndex", this.alternativeSkinIndex);
      tag.putInt("PoseIndex", this.poseIndex);
      if (this.skinSnapshot != null) {
         tag.putString("SkinSnapshot", this.skinSnapshot);
      }

      if (this.quickSkinId != null) {
         tag.putString("QuickSkinId", this.quickSkinId);
      }

      tag.putDouble("FigureOffsetX", this.figureOffsetX);
      tag.putDouble("FigureOffsetY", this.figureOffsetY);
      tag.putDouble("FigureOffsetZ", this.figureOffsetZ);
      tag.putDouble("FigureScale", this.figureScale);
   }

   @Override
   public void load(CompoundTag tag) {
      super.load(tag);
      this.loadFromItemNbt(tag);
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
         if (this.level != null && this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
         }
      }
   }

   public void saveToItem(ItemStack stack) {
      CompoundTag tag = new CompoundTag();
      this.saveAdditional(tag);
      BlockItem.setBlockEntityData(stack, this.getType(), tag);
   }

   public void loadFromItemNbt(CompoundTag tag) {
      if (tag.contains("FigureId")) {
         this.figureId = tag.getString("FigureId");
      }

      if (tag.contains("CollectionId")) {
         this.collectionId = tag.getString("CollectionId");
      }

      if (tag.contains("AlternativeSkinIndex")) {
         this.alternativeSkinIndex = tag.getInt("AlternativeSkinIndex");
      }

      if (tag.contains("PoseIndex")) {
         this.poseIndex = tag.getInt("PoseIndex");
      }

      this.skinSnapshot = tag.contains("SkinSnapshot", Tag.TAG_STRING) ? tag.getString("SkinSnapshot") : null;
      this.quickSkinId = tag.contains("QuickSkinId", Tag.TAG_STRING) ? tag.getString("QuickSkinId") : null;
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
   }

   public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
      if (level.isClientSide && blockEntity instanceof FigureBlockEntity) {
      }
   }
}
