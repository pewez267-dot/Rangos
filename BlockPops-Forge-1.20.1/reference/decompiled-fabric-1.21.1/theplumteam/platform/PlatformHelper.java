package com.theplumteam.platform;

import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.blockentity.ClawMachineBlockEntity;
import com.theplumteam.platform.fabric.PlatformHelperImpl;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.injectables.annotations.ExpectPlatform.Transformed;
import java.nio.file.Path;
import net.minecraft.class_2338;

public class PlatformHelper {
   @ExpectPlatform
   @Transformed
   public static String getPlatformName() {
      return PlatformHelperImpl.getPlatformName();
   }

   @ExpectPlatform
   @Transformed
   public static Path getGameDirectory() {
      return PlatformHelperImpl.getGameDirectory();
   }

   @ExpectPlatform
   @Transformed
   public static Path getConfigDirectory() {
      return PlatformHelperImpl.getConfigDirectory();
   }

   @ExpectPlatform
   @Transformed
   public static boolean isModLoaded(String modId) {
      return PlatformHelperImpl.isModLoaded(modId);
   }

   @ExpectPlatform
   @Transformed
   public static String getModVersion() {
      return PlatformHelperImpl.getModVersion();
   }

   @ExpectPlatform
   @Transformed
   public static boolean isDevelopmentEnvironment() {
      return PlatformHelperImpl.isDevelopmentEnvironment();
   }

   @ExpectPlatform
   @Transformed
   public static void openBoxFigureScreen(class_2338 pos, BoxBlockEntity boxBlockEntity) {
      PlatformHelperImpl.openBoxFigureScreen(pos, boxBlockEntity);
   }

   @ExpectPlatform
   @Transformed
   public static void openClawMachineScreen(class_2338 pos, ClawMachineBlockEntity clawMachineBlockEntity) {
      PlatformHelperImpl.openClawMachineScreen(pos, clawMachineBlockEntity);
   }
}
