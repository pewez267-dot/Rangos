package com.theplumteam.data;

import com.theplumteam.block.PopBlockColor;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.class_2487;
import net.minecraft.class_2499;
import net.minecraft.class_2519;
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

   public class_2487 serializeNBT() {
      class_2487 tag = new class_2487();
      class_2499 listTag = new class_2499();

      for (String figureId : this.discoveredFigures) {
         listTag.add(class_2519.method_23256(figureId));
      }

      tag.method_10566("DiscoveredFigures", listTag);
      tag.method_10569("RegularTokens", this.regularTokens);
      tag.method_10544("NextRegularTokenTime", this.nextRegularTokenTime);
      tag.method_10544("LastSpecialTokenResetTimestamp", this.lastSpecialTokenResetTimestamp);
      tag.method_10556("UsedTodaySpecialToken", this.usedTodaySpecialToken);
      tag.method_10556("HasChosenFavoriteColor", this.hasChosenFavoriteColor);
      if (this.favoriteColor != null) {
         tag.method_10582("FavoriteColor", this.favoriteColor);
      }

      if (!this.figureSkins.isEmpty()) {
         class_2487 skinsTag = new class_2487();

         for (Entry<String, String> entry : this.figureSkins.entrySet()) {
            skinsTag.method_10582(entry.getKey(), entry.getValue());
         }

         tag.method_10566("FigureSkins", skinsTag);
      }

      if (!this.figureQuickSkins.isEmpty()) {
         class_2487 qsTag = new class_2487();

         for (Entry<String, String> entry : this.figureQuickSkins.entrySet()) {
            qsTag.method_10582(entry.getKey(), entry.getValue());
         }

         tag.method_10566("FigureQuickSkins", qsTag);
      }

      return tag;
   }

   public void deserializeNBT(class_2487 tag) {
      this.discoveredFigures.clear();
      if (tag.method_10573("DiscoveredFigures", 9)) {
         class_2499 listTag = tag.method_10554("DiscoveredFigures", 8);

         for (int i = 0; i < listTag.size(); i++) {
            this.discoveredFigures.add(listTag.method_10608(i));
         }
      }

      if (tag.method_10545("RegularTokens")) {
         this.regularTokens = tag.method_10550("RegularTokens");
      }

      if (tag.method_10545("NextRegularTokenTime")) {
         this.nextRegularTokenTime = tag.method_10537("NextRegularTokenTime");
      }

      if (tag.method_10545("LastSpecialTokenResetTimestamp")) {
         this.lastSpecialTokenResetTimestamp = tag.method_10537("LastSpecialTokenResetTimestamp");
      }

      if (tag.method_10545("UsedTodaySpecialToken")) {
         this.usedTodaySpecialToken = tag.method_10577("UsedTodaySpecialToken");
      }

      this.hasChosenFavoriteColor = tag.method_10577("HasChosenFavoriteColor");
      if (tag.method_10573("FavoriteColor", 8)) {
         this.favoriteColor = tag.method_10558("FavoriteColor");
      } else {
         this.favoriteColor = null;
      }

      this.figureSkins.clear();
      if (tag.method_10573("FigureSkins", 10)) {
         class_2487 skinsTag = tag.method_10562("FigureSkins");

         for (String key : skinsTag.method_10541()) {
            this.figureSkins.put(key, skinsTag.method_10558(key));
         }
      }

      this.figureQuickSkins.clear();
      if (tag.method_10573("FigureQuickSkins", 10)) {
         class_2487 qsTag = tag.method_10562("FigureQuickSkins");

         for (String key : qsTag.method_10541()) {
            this.figureQuickSkins.put(key, qsTag.method_10558(key));
         }
      }
   }
}
