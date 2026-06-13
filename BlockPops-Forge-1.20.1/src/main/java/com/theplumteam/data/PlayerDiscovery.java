package com.theplumteam.data;

import com.theplumteam.block.PopBlockColor;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public class PlayerDiscovery implements IPlayerDiscovery {
   private final Set<String> discoveredFigures = new HashSet<>();
   private static final String NBT_KEY = "DiscoveredFigures";
   private int regularTokens = 0;
   private long nextRegularTokenTime = 0L;
   private long lastSpecialTokenResetTimestamp = 0L;
   private boolean usedTodaySpecialToken = false;
   private boolean hasChosenFavoriteColor = false;
   private String favoriteColor = null;
   private final Map<String, String> figureSkins = new HashMap<>();
   private static final String NBT_FIGURE_SKINS_KEY = "FigureSkins";
   private final Map<String, String> figureQuickSkins = new HashMap<>();
   private static final String NBT_FIGURE_QUICK_SKINS_KEY = "FigureQuickSkins";

   @Override
   public boolean isDiscovered(String figureId) {
      return this.discoveredFigures.contains(figureId);
   }

   @Override
   public void discover(String figureId) {
      this.discoveredFigures.add(figureId);
   }

   @Override
   public Set<String> getDiscoveredSet() {
      return Collections.unmodifiableSet(this.discoveredFigures);
   }

   @Override
   public void syncFrom(Set<String> discovered) {
      this.discoveredFigures.clear();
      this.discoveredFigures.addAll(discovered);
   }

   @Override
   public int getRegularTokens() {
      return this.regularTokens;
   }

   @Override
   public void setRegularTokens(int count) {
      this.regularTokens = count;
   }

   @Override
   public long getNextRegularTokenTime() {
      return this.nextRegularTokenTime;
   }

   @Override
   public void setNextRegularTokenTime(long worldTimeTicks) {
      this.nextRegularTokenTime = worldTimeTicks;
   }

   @Override
   public long getLastSpecialTokenResetTimestamp() {
      return this.lastSpecialTokenResetTimestamp;
   }

   @Override
   public void setLastSpecialTokenResetTimestamp(long timestamp) {
      this.lastSpecialTokenResetTimestamp = timestamp;
   }

   @Override
   public boolean hasUsedTodaySpecialToken() {
      return this.usedTodaySpecialToken;
   }

   @Override
   public void setUsedTodaySpecialToken(boolean used) {
      this.usedTodaySpecialToken = used;
   }

   @Override
   public boolean hasChosenFavoriteColor() {
      return this.hasChosenFavoriteColor;
   }

   @Override
   public void setHasChosenFavoriteColor(boolean hasChosen) {
      this.hasChosenFavoriteColor = hasChosen;
   }

   @Nullable
   @Override
   public PopBlockColor getFavoriteColor() {
      if (this.favoriteColor == null) {
         return null;
      } else {
         try {
            return PopBlockColor.valueOf(this.favoriteColor.toUpperCase());
         } catch (IllegalArgumentException var2) {
            return null;
         }
      }
   }

   @Override
   public void setFavoriteColor(@Nullable PopBlockColor color) {
      this.favoriteColor = color != null ? color.name() : null;
   }

   @Override
   public void saveFigureSkin(String figureId, String skinUrl) {
      this.figureSkins.put(figureId, skinUrl);
   }

   @Nullable
   @Override
   public String getFigureSkin(String figureId) {
      return this.figureSkins.get(figureId);
   }

   @Override
   public Map<String, String> getAllFigureSkins() {
      return Collections.unmodifiableMap(this.figureSkins);
   }

   @Override
   public void saveFigureQuickSkin(String figureId, String quickSkinId) {
      this.figureQuickSkins.put(figureId, quickSkinId);
   }

   @Nullable
   @Override
   public String getFigureQuickSkin(String figureId) {
      return this.figureQuickSkins.get(figureId);
   }

   @Override
   public Map<String, String> getAllFigureQuickSkins() {
      return Collections.unmodifiableMap(this.figureQuickSkins);
   }

   public CompoundTag serializeNBT() {
      CompoundTag tag = new CompoundTag();
      ListTag listTag = new ListTag();

      for (String figureId : this.discoveredFigures) {
         listTag.add(StringTag.valueOf(figureId));
      }

      tag.put("DiscoveredFigures", listTag);
      tag.putInt("RegularTokens", this.regularTokens);
      tag.putLong("NextRegularTokenTime", this.nextRegularTokenTime);
      tag.putLong("LastSpecialTokenResetTimestamp", this.lastSpecialTokenResetTimestamp);
      tag.putBoolean("UsedTodaySpecialToken", this.usedTodaySpecialToken);
      tag.putBoolean("HasChosenFavoriteColor", this.hasChosenFavoriteColor);
      if (this.favoriteColor != null) {
         tag.putString("FavoriteColor", this.favoriteColor);
      }

      if (!this.figureSkins.isEmpty()) {
         CompoundTag skinsTag = new CompoundTag();

         for (Entry<String, String> entry : this.figureSkins.entrySet()) {
            skinsTag.putString(entry.getKey(), entry.getValue());
         }

         tag.put("FigureSkins", skinsTag);
      }

      if (!this.figureQuickSkins.isEmpty()) {
         CompoundTag qsTag = new CompoundTag();

         for (Entry<String, String> entry : this.figureQuickSkins.entrySet()) {
            qsTag.putString(entry.getKey(), entry.getValue());
         }

         tag.put("FigureQuickSkins", qsTag);
      }

      return tag;
   }

   public void deserializeNBT(CompoundTag tag) {
      this.discoveredFigures.clear();
      if (tag.contains("DiscoveredFigures", Tag.TAG_LIST)) {
         ListTag listTag = tag.getList("DiscoveredFigures", Tag.TAG_STRING);

         for (int i = 0; i < listTag.size(); i++) {
            this.discoveredFigures.add(listTag.getString(i));
         }
      }

      if (tag.contains("RegularTokens")) {
         this.regularTokens = tag.getInt("RegularTokens");
      }

      if (tag.contains("NextRegularTokenTime")) {
         this.nextRegularTokenTime = tag.getLong("NextRegularTokenTime");
      }

      if (tag.contains("LastSpecialTokenResetTimestamp")) {
         this.lastSpecialTokenResetTimestamp = tag.getLong("LastSpecialTokenResetTimestamp");
      }

      if (tag.contains("UsedTodaySpecialToken")) {
         this.usedTodaySpecialToken = tag.getBoolean("UsedTodaySpecialToken");
      }

      this.hasChosenFavoriteColor = tag.getBoolean("HasChosenFavoriteColor");
      if (tag.contains("FavoriteColor", Tag.TAG_STRING)) {
         this.favoriteColor = tag.getString("FavoriteColor");
      } else {
         this.favoriteColor = null;
      }

      this.figureSkins.clear();
      if (tag.contains("FigureSkins", Tag.TAG_COMPOUND)) {
         CompoundTag skinsTag = tag.getCompound("FigureSkins");

         for (String key : skinsTag.getAllKeys()) {
            this.figureSkins.put(key, skinsTag.getString(key));
         }
      }

      this.figureQuickSkins.clear();
      if (tag.contains("FigureQuickSkins", Tag.TAG_COMPOUND)) {
         CompoundTag qsTag = tag.getCompound("FigureQuickSkins");

         for (String key : qsTag.getAllKeys()) {
            this.figureQuickSkins.put(key, qsTag.getString(key));
         }
      }
   }
}
