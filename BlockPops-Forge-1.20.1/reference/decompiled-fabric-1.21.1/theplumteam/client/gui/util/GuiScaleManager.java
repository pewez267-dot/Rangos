package com.theplumteam.client.gui.util;

import com.theplumteam.BlockPopsMod;
import dev.architectury.platform.Platform;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.class_310;
import net.minecraft.class_7172;

public class GuiScaleManager {
   private static Integer originalGuiScale = null;
   private static boolean scaleChanged = false;
   private static boolean usingInverseScale = false;
   private static final int TARGET_GUI_SCALE = 2;

   public static boolean setMenuGuiScale(int targetScale) {
      return setMenuGuiScale(targetScale, false);
   }

   public static boolean setMenuGuiScale(int targetScale, boolean force) {
      try {
         class_310 mc = class_310.method_1551();
         class_7172<Integer> guiScaleOption = mc.field_1690.method_42474();
         int currentScale = (Integer)guiScaleOption.method_41753();
         if (currentScale == targetScale) {
            usingInverseScale = false;
            return false;
         } else if (!force && areShadersActive() && isDistantHorizonsInstalled()) {
            BlockPopsMod.logDebug("Shaders + Distant Horizons detected, skipping GUI scale change");
            usingInverseScale = false;
            return false;
         } else {
            usingInverseScale = false;
            if (originalGuiScale == null) {
               originalGuiScale = currentScale;
            }

            guiScaleOption.method_41748(targetScale);
            scaleChanged = true;
            mc.method_15993();
            return true;
         }
      } catch (Exception var5) {
         BlockPopsMod.LOGGER.error("Failed to set menu GUI scale", var5);
         return false;
      }
   }

   public static void restoreOriginalGuiScale() {
      try {
         usingInverseScale = false;
         if (originalGuiScale == null || !scaleChanged) {
            BlockPopsMod.LOGGER.debug("No GUI scale to restore (originalGuiScale={}, scaleChanged={})", originalGuiScale, scaleChanged);
            return;
         }

         class_310 mc = class_310.method_1551();
         class_7172<Integer> guiScaleOption = mc.field_1690.method_42474();
         int currentScale = (Integer)guiScaleOption.method_41753();
         int targetScale = originalGuiScale;
         BlockPopsMod.logDebug("Restoring GUI scale from {} to {}", currentScale, targetScale);
         originalGuiScale = null;
         scaleChanged = false;
         if (currentScale != targetScale) {
            guiScaleOption.method_41748(targetScale);
            mc.method_15993();
            BlockPopsMod.logDebug("GUI scale restored successfully to {}", targetScale);
         } else {
            BlockPopsMod.logDebug("GUI scale already at target {}, no change needed", targetScale);
         }
      } catch (Exception var4) {
         BlockPopsMod.LOGGER.error("Failed to restore original GUI scale", var4);
      }
   }

   public static boolean isUsingInverseScale() {
      return usingInverseScale;
   }

   public static float getRenderScaleFactor() {
      class_310 mc = class_310.method_1551();
      double currentGuiScale = mc.method_22683().method_4495();
      if (currentGuiScale <= 0.0) {
         currentGuiScale = 1.0;
      }

      return (float)(2.0 / currentGuiScale);
   }

   public static int getVirtualWidth() {
      class_310 mc = class_310.method_1551();
      return mc.method_22683().method_4489() / 2;
   }

   public static int getVirtualHeight() {
      class_310 mc = class_310.method_1551();
      return mc.method_22683().method_4506() / 2;
   }

   public static double transformMouseX(double mouseX) {
      if (!usingInverseScale) {
         return mouseX;
      } else {
         class_310 mc = class_310.method_1551();
         double currentGuiScale = mc.method_22683().method_4495();
         if (currentGuiScale <= 0.0) {
            currentGuiScale = 1.0;
         }

         return mouseX * (currentGuiScale / 2.0);
      }
   }

   public static double transformMouseY(double mouseY) {
      if (!usingInverseScale) {
         return mouseY;
      } else {
         class_310 mc = class_310.method_1551();
         double currentGuiScale = mc.method_22683().method_4495();
         if (currentGuiScale <= 0.0) {
            currentGuiScale = 1.0;
         }

         return mouseY * (currentGuiScale / 2.0);
      }
   }

   public static float getMouseScaleFactor() {
      class_310 mc = class_310.method_1551();
      double currentGuiScale = mc.method_22683().method_4495();
      if (currentGuiScale <= 0.0) {
         currentGuiScale = 1.0;
      }

      return (float)(currentGuiScale / 2.0);
   }

   public static int getOptimalMenuScale() {
      return 2;
   }

   public static boolean areShadersActive() {
      try {
         if (isOptifineShaderActive()) {
            return true;
         }

         if (isIrisShaderActive()) {
            return true;
         }
      } catch (Exception var1) {
         BlockPopsMod.LOGGER.debug("Error checking for shaders: {}", var1.getMessage());
      }

      return false;
   }

   private static boolean isOptifineShaderActive() {
      try {
         Class<?> shadersClass = Class.forName("net.optifine.shaders.Shaders");
         Field shaderPackLoadedField = shadersClass.getDeclaredField("shaderPackLoaded");
         shaderPackLoadedField.setAccessible(true);
         return shaderPackLoadedField.getBoolean(null);
      } catch (ClassNotFoundException var4) {
         return false;
      } catch (Exception var5) {
         try {
            Class<?> shadersClassx = Class.forName("net.optifine.shaders.Shaders");
            Method isShaderPackLoaded = shadersClassx.getMethod("isShaderPackLoaded");
            return (Boolean)isShaderPackLoaded.invoke(null);
         } catch (Exception var3) {
            return false;
         }
      }
   }

   private static boolean isIrisShaderActive() {
      try {
         Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
         Method getInstance = irisApiClass.getMethod("getInstance");
         Object instance = getInstance.invoke(null);
         Method isShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");
         return (Boolean)isShaderPackInUse.invoke(instance);
      } catch (ClassNotFoundException var4) {
         return false;
      } catch (Exception var5) {
         return false;
      }
   }

   private static boolean isDistantHorizonsInstalled() {
      return Platform.isModLoaded("distanthorizons");
   }
}
