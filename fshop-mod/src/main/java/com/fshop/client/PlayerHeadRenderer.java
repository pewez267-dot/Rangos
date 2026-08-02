package com.fshop.client;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders a player's real face/head, matching how vanilla identifies shop
 * owners: the tab list skin if the player is currently online, otherwise the
 * skin fetched (and cached) via the skin manager for their profile. Works for
 * both premium accounts (their real skin loads once fetched) and offline/
 * cracked accounts, which fall back to the vanilla default Steve/Alex skin
 * derived from their UUID -- exactly like the game itself would show.
 */
public final class PlayerHeadRenderer {
   private PlayerHeadRenderer() {
   }

   public static void draw(GuiGraphics g, UUID ownerId, String ownerName, int x, int y, int size) {
      PlayerFaceRenderer.draw(g, resolveSkin(ownerId, ownerName), x, y, size);
   }

   private static ResourceLocation resolveSkin(UUID ownerId, String ownerName) {
      Minecraft mc = Minecraft.getInstance();
      if (ownerId == null) {
         return DefaultPlayerSkin.getDefaultSkin();
      }
      if (mc.getConnection() != null) {
         PlayerInfo info = mc.getConnection().getPlayerInfo(ownerId);
         if (info != null) {
            return info.getSkinLocation();
         }
      }
      try {
         String safeName = (ownerName == null || ownerName.isEmpty()) ? "Player" : ownerName;
         GameProfile profile = new GameProfile(ownerId, safeName);
         return mc.getSkinManager().getInsecureSkinLocation(profile);
      } catch (Exception e) {
         return DefaultPlayerSkin.getDefaultSkin(ownerId);
      }
   }
}
