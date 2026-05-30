package com.theplumteam.platform;

import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.blockentity.ClawMachineBlockEntity;
import com.theplumteam.platform.forge.ClientPlatformHelperImpl;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Forge implementation of the platform abstraction. The original (Architectury multiloader)
 * mod used {@code @ExpectPlatform}; on a single-target Forge build the redirection is inlined here.
 */
public class PlatformHelper {
   public static String getPlatformName() {
      return "Forge";
   }

   public static Path getGameDirectory() {
      return FMLPaths.GAMEDIR.get();
   }

   public static Path getConfigDirectory() {
      return FMLPaths.CONFIGDIR.get();
   }

   public static boolean isModLoaded(String modId) {
      return ModList.get().isLoaded(modId);
   }

   public static String getModVersion() {
      return ModList.get()
         .getModContainerById("blockpops")
         .map(container -> container.getModInfo().getVersion().toString())
         .orElse("UNKNOWN");
   }

   public static boolean isDevelopmentEnvironment() {
      return !FMLEnvironment.production;
   }

   public static void openBoxFigureScreen(BlockPos pos, BoxBlockEntity boxBlockEntity) {
      DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPlatformHelperImpl.openBoxFigureScreen(pos, boxBlockEntity));
   }

   public static void openClawMachineScreen(BlockPos pos, ClawMachineBlockEntity clawMachineBlockEntity) {
      DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPlatformHelperImpl.openClawMachineScreen(pos, clawMachineBlockEntity));
   }
}
