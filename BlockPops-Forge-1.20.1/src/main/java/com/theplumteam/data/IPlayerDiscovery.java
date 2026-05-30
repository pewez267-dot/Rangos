package com.theplumteam.data;

import com.theplumteam.block.PopBlockColor;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public interface IPlayerDiscovery {
   boolean isDiscovered(String var1);

   void discover(String var1);

   Set<String> getDiscoveredSet();

   void syncFrom(Set<String> var1);

   int getRegularTokens();

   void setRegularTokens(int var1);

   long getNextRegularTokenTime();

   void setNextRegularTokenTime(long var1);

   long getLastSpecialTokenResetTimestamp();

   void setLastSpecialTokenResetTimestamp(long var1);

   boolean hasUsedTodaySpecialToken();

   void setUsedTodaySpecialToken(boolean var1);

   boolean hasChosenFavoriteColor();

   void setHasChosenFavoriteColor(boolean var1);

   @Nullable
   PopBlockColor getFavoriteColor();

   void setFavoriteColor(@Nullable PopBlockColor var1);

   void saveFigureSkin(String var1, String var2);

   @Nullable
   String getFigureSkin(String var1);

   Map<String, String> getAllFigureSkins();

   void saveFigureQuickSkin(String var1, String var2);

   @Nullable
   String getFigureQuickSkin(String var1);

   Map<String, String> getAllFigureQuickSkins();
}
