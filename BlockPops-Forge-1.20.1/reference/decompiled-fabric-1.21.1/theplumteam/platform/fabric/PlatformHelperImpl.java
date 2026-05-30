package com.theplumteam.platform.fabric;

import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.blockentity.ClawMachineBlockEntity;
import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_2338;

public class PlatformHelperImpl {
   public static String getPlatformName() {
      return "Fabric";
   }

   public static Path getGameDirectory() {
      return FabricLoader.getInstance().getGameDir();
   }

   public static Path getConfigDirectory() {
      return FabricLoader.getInstance().getConfigDir();
   }

   public static boolean isModLoaded(String modId) {
      return FabricLoader.getInstance().isModLoaded(modId);
   }

   public static String getModVersion() {
      return FabricLoader.getInstance()
         .getModContainer("blockpops")
         .map(container -> container.getMetadata().getVersion().getFriendlyString())
         .orElse("UNKNOWN");
   }

   public static boolean isDevelopmentEnvironment() {
      return FabricLoader.getInstance().isDevelopmentEnvironment();
   }

   public static void openBoxFigureScreen(class_2338 pos, BoxBlockEntity boxBlockEntity) {
      if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
         ClientPlatformHelperImpl.openBoxFigureScreen(pos, boxBlockEntity);
      }
   }

   public static void openClawMachineScreen(class_2338 pos, ClawMachineBlockEntity clawMachineBlockEntity) {
      if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
         ClientPlatformHelperImpl.openClawMachineScreen(pos, clawMachineBlockEntity);
      }
   }
}
