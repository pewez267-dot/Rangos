package com.theplumteam.platform.fabric;

import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.blockentity.ClawMachineBlockEntity;
import com.theplumteam.client.gui.CollectionSelectionScreen;
import com.theplumteam.client.gui.FigurePositionScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2338;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public class ClientPlatformHelperImpl {
   public static void openBoxFigureScreen(class_2338 pos, BoxBlockEntity boxBlockEntity) {
      class_310.method_1551()
         .method_1507(
            new FigurePositionScreen(
               pos,
               boxBlockEntity.getFigureOffsetX(),
               boxBlockEntity.getFigureOffsetY(),
               boxBlockEntity.getFigureOffsetZ(),
               boxBlockEntity.getFigureScale(),
               boxBlockEntity.getHitboxOffsetX(),
               boxBlockEntity.getHitboxOffsetY(),
               boxBlockEntity.getHitboxOffsetZ(),
               boxBlockEntity.getHitboxScaleX(),
               boxBlockEntity.getHitboxScaleY(),
               boxBlockEntity.getHitboxScaleZ(),
               boxBlockEntity.getLogoPositionX(),
               boxBlockEntity.getLogoPositionY(),
               boxBlockEntity.getLogoPositionZ(),
               boxBlockEntity.getLogoScaleX(),
               boxBlockEntity.getLogoScaleY(),
               boxBlockEntity.getLogoScaleZ()
            )
         );
   }

   public static void openClawMachineScreen(class_2338 pos, ClawMachineBlockEntity clawMachineBlockEntity) {
      class_310.method_1551().method_1507(new CollectionSelectionScreen(pos, clawMachineBlockEntity.getCollectionId()));
   }
}
