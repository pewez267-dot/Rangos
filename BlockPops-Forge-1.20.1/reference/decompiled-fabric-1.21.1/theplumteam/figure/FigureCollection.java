package com.theplumteam.figure;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.class_2960;

public class FigureCollection {
   private final String id;
   private final String name;
   private final String author;
   private final String authorUrl;
   private final class_2960 boxTexture;
   private final FigureCollection.LogoConfig logoConfig;
   private final List<FigureDefinition> figures;
   private final int[] backgroundColor;

   public FigureCollection(
      String id,
      String name,
      String author,
      String authorUrl,
      class_2960 boxTexture,
      FigureCollection.LogoConfig logoConfig,
      List<FigureDefinition> figures,
      int[] backgroundColor
   ) {
      this.id = id;
      this.name = name;
      this.author = author;
      this.authorUrl = authorUrl;
      this.boxTexture = boxTexture;
      this.logoConfig = logoConfig;
      this.figures = new ArrayList<>(figures);
      this.backgroundColor = backgroundColor;
   }

   public static FigureCollection fromJson(JsonObject json) {
      String id = json.get("id").getAsString();
      String name = json.get("name").getAsString();
      String author = json.has("author") ? json.get("author").getAsString() : "Unknown";
      String authorUrl = json.has("author_url") ? json.get("author_url").getAsString() : null;
      class_2960 boxTexture = class_2960.method_12829(json.get("box_texture").getAsString());
      FigureCollection.LogoConfig logoConfig = null;
      if (json.has("logo")) {
         JsonObject logoJson = json.getAsJsonObject("logo");
         class_2960 logoTexture = class_2960.method_12829(logoJson.get("texture").getAsString());
         float positionX = logoJson.has("position_x") ? logoJson.get("position_x").getAsFloat() : -3.5F;
         float positionY = logoJson.has("position_y") ? logoJson.get("position_y").getAsFloat() : 0.8F;
         float positionZ = logoJson.has("position_z") ? logoJson.get("position_z").getAsFloat() : -7.4F;
         float scaleX = logoJson.has("scale_x") ? logoJson.get("scale_x").getAsFloat() : 1.0F;
         float scaleY = logoJson.has("scale_y") ? logoJson.get("scale_y").getAsFloat() : 5.0F;
         float scaleZ = logoJson.has("scale_z") ? logoJson.get("scale_z").getAsFloat() : 5.0F;
         logoConfig = new FigureCollection.LogoConfig(logoTexture, positionX, positionY, positionZ, scaleX, scaleY, scaleZ);
      } else if (json.has("logo_texture")) {
         class_2960 logoTexture = class_2960.method_12829(json.get("logo_texture").getAsString());
         String logoType = json.has("logo_type") ? json.get("logo_type").getAsString() : "square";
         float scaleX = 5.0F;
         float scaleY = 5.0F;
         if ("wide".equals(logoType)) {
            scaleX = 6.0F;
            scaleY = 4.15F;
         } else if ("tall".equals(logoType)) {
            scaleX = 4.0F;
            scaleY = 6.0F;
         }

         logoConfig = new FigureCollection.LogoConfig(logoTexture, -3.5F, 0.8F, -7.4F, 1.0F, scaleY, scaleX);
      }

      List<FigureDefinition> figures = new ArrayList<>();
      JsonArray figuresArray = json.getAsJsonArray("figures");

      for (int i = 0; i < figuresArray.size(); i++) {
         JsonObject figureJson = figuresArray.get(i).getAsJsonObject();
         figures.add(FigureDefinition.fromJson(figureJson));
      }

      int[] backgroundColor = null;
      if (json.has("background_color")) {
         JsonObject bgColorJson = json.getAsJsonObject("background_color");
         int r = bgColorJson.has("r") ? bgColorJson.get("r").getAsInt() : 0;
         int g = bgColorJson.has("g") ? bgColorJson.get("g").getAsInt() : 0;
         int b = bgColorJson.has("b") ? bgColorJson.get("b").getAsInt() : 0;
         backgroundColor = new int[]{r, g, b};
      }

      return new FigureCollection(id, name, author, authorUrl, boxTexture, logoConfig, figures, backgroundColor);
   }

   public String getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public String getAuthor() {
      return this.author;
   }

   public String getAuthorUrl() {
      return this.authorUrl;
   }

   public class_2960 getBoxTexture() {
      return this.boxTexture;
   }

   public FigureCollection.LogoConfig getLogoConfig() {
      return this.logoConfig;
   }

   @Deprecated
   public class_2960 getLogoTexture() {
      return this.logoConfig != null ? this.logoConfig.getTexture() : null;
   }

   public List<FigureDefinition> getFigures() {
      return Collections.unmodifiableList(this.figures);
   }

   public int[] getBackgroundColor() {
      return this.backgroundColor;
   }

   public boolean hasBackgroundColor() {
      return this.backgroundColor != null;
   }

   public Optional<FigureDefinition> getFigure(String figureId) {
      return this.figures.stream().filter(f -> f.getId().equals(figureId)).findFirst();
   }

   public boolean hasFigures() {
      return !this.figures.isEmpty();
   }

   public JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.addProperty("id", this.id);
      json.addProperty("name", this.name);
      json.addProperty("author", this.author);
      if (this.authorUrl != null) {
         json.addProperty("author_url", this.authorUrl);
      }

      json.addProperty("box_texture", this.boxTexture.toString());
      if (this.logoConfig != null) {
         JsonObject logoJson = new JsonObject();
         logoJson.addProperty("texture", this.logoConfig.getTexture().toString());
         logoJson.addProperty("position_x", this.logoConfig.getPositionX());
         logoJson.addProperty("position_y", this.logoConfig.getPositionY());
         logoJson.addProperty("position_z", this.logoConfig.getPositionZ());
         logoJson.addProperty("scale_x", this.logoConfig.getScaleX());
         logoJson.addProperty("scale_y", this.logoConfig.getScaleY());
         logoJson.addProperty("scale_z", this.logoConfig.getScaleZ());
         json.add("logo", logoJson);
      }

      JsonArray figuresArray = new JsonArray();

      for (FigureDefinition figure : this.figures) {
         figuresArray.add(figure.toJson());
      }

      json.add("figures", figuresArray);
      if (this.backgroundColor != null) {
         JsonObject bgColorJson = new JsonObject();
         bgColorJson.addProperty("r", this.backgroundColor[0]);
         bgColorJson.addProperty("g", this.backgroundColor[1]);
         bgColorJson.addProperty("b", this.backgroundColor[2]);
         json.add("background_color", bgColorJson);
      }

      return json;
   }

   @Override
   public String toString() {
      return "FigureCollection{id='" + this.id + "', name='" + this.name + "', figures=" + this.figures.size() + "}";
   }

   public static class LogoConfig {
      private final class_2960 texture;
      private final float positionX;
      private final float positionY;
      private final float positionZ;
      private final float scaleX;
      private final float scaleY;
      private final float scaleZ;

      public LogoConfig(class_2960 texture, float positionX, float positionY, float positionZ, float scaleX, float scaleY, float scaleZ) {
         this.texture = texture;
         this.positionX = positionX;
         this.positionY = positionY;
         this.positionZ = positionZ;
         this.scaleX = scaleX;
         this.scaleY = scaleY;
         this.scaleZ = scaleZ;
      }

      public class_2960 getTexture() {
         return this.texture;
      }

      public float getPositionX() {
         return this.positionX;
      }

      public float getPositionY() {
         return this.positionY;
      }

      public float getPositionZ() {
         return this.positionZ;
      }

      public float getScaleX() {
         return this.scaleX;
      }

      public float getScaleY() {
         return this.scaleY;
      }

      public float getScaleZ() {
         return this.scaleZ;
      }
   }
}
