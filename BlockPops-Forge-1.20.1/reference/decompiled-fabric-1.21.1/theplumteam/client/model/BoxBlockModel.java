package com.theplumteam.client.model;

import com.theplumteam.block.PopBlockColor;
import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import java.util.Locale;
import net.minecraft.class_2960;
import software.bernie.geckolib.model.GeoModel;

public class BoxBlockModel extends GeoModel<BoxBlockEntity> {
   private static final class_2960 MODEL = class_2960.method_60655("blockpops", "geo/block/box_block.geo.json");
   private static final class_2960 ANIMATION = class_2960.method_60655("blockpops", "animations/block/box_block.animation.json");
   private static final class_2960 DEFAULT_TEXTURE = class_2960.method_60655("blockpops", "textures/block/box/original.png");

   public class_2960 getModelResource(BoxBlockEntity animatable) {
      return MODEL;
   }

   public class_2960 getTextureResource(BoxBlockEntity animatable) {
      PopBlockColor color = animatable.getColor();
      if (color != null) {
         return class_2960.method_60655("blockpops", "textures/block/box/" + color.getTextureName().toLowerCase(Locale.ROOT) + ".png");
      } else {
         String collectionId = animatable.getCollectionId();
         return CollectionRegistry.getCollection(collectionId).map(FigureCollection::getBoxTexture).orElse(DEFAULT_TEXTURE);
      }
   }

   public class_2960 getAnimationResource(BoxBlockEntity animatable) {
      return ANIMATION;
   }
}
