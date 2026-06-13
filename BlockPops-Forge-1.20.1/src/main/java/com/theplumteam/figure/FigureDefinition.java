package com.theplumteam.figure;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.theplumteam.block.PopBlockColor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class FigureDefinition {
   private static final String DEFAULT_MODEL_PATH = "geo/figure/box_figure_default.geo.json";
   private final String id;
   private final String name;
   private final ResourceLocation modelPath;
   private final ResourceLocation texturePath;
   private final ResourceLocation animationPath;
   @Nullable
   private final ResourceLocation poseAnimationPath;
   private final FigureType type;
   private final UUID playerUUID;
   private final List<AlternativeSkin> alternatives;
   private final PopBlockColor favoriteColor;
   @Nullable
   private final String authorUrl;
   private List<String> hiddenBones = Collections.emptyList();
   private List<ExtraTexture> extraTextures = Collections.emptyList();
   private float scale = 1.0F;
   private float guiScale = 1.0F;
   private boolean showBoxFace = true;
   @Nullable
   private float[] boxFaceUV = null;
   private float offsetX = 0.0F;
   private float offsetZ = 0.0F;
   private boolean poseLocked = false;

   public FigureDefinition(String id, String name, ResourceLocation modelPath, ResourceLocation texturePath, ResourceLocation animationPath) {
      this(id, name, modelPath, texturePath, animationPath, Collections.emptyList(), null);
   }

   public FigureDefinition(
      String id, String name, ResourceLocation modelPath, ResourceLocation texturePath, ResourceLocation animationPath, List<AlternativeSkin> alternatives
   ) {
      this(id, name, modelPath, texturePath, animationPath, alternatives, null);
   }

   public FigureDefinition(
      String id,
      String name,
      ResourceLocation modelPath,
      ResourceLocation texturePath,
      ResourceLocation animationPath,
      List<AlternativeSkin> alternatives,
      @Nullable String authorUrl
   ) {
      this(id, name, modelPath, texturePath, animationPath, null, alternatives, authorUrl);
   }

   public FigureDefinition(
      String id,
      String name,
      ResourceLocation modelPath,
      ResourceLocation texturePath,
      ResourceLocation animationPath,
      @Nullable ResourceLocation poseAnimationPath,
      List<AlternativeSkin> alternatives,
      @Nullable String authorUrl
   ) {
      this.id = id;
      this.name = name;
      this.modelPath = modelPath;
      this.texturePath = texturePath;
      this.animationPath = animationPath;
      this.poseAnimationPath = poseAnimationPath;
      this.type = FigureType.STATIC;
      this.playerUUID = null;
      this.alternatives = new ArrayList<>(alternatives);
      this.favoriteColor = null;
      this.authorUrl = authorUrl;
   }

   public FigureDefinition(String id, String name, ResourceLocation modelPath, ResourceLocation animationPath, UUID playerUUID, PopBlockColor favoriteColor) {
      this.id = id;
      this.name = name;
      this.modelPath = modelPath;
      this.texturePath = null;
      this.animationPath = animationPath;
      this.poseAnimationPath = null;
      this.type = FigureType.PLAYER;
      this.playerUUID = playerUUID;
      this.alternatives = Collections.emptyList();
      this.favoriteColor = favoriteColor;
      this.authorUrl = null;
   }

   public static FigureDefinition fromJson(JsonObject json) {
      String id = json.get("id").getAsString();
      String name = json.get("name").getAsString();
      ResourceLocation modelPath = ResourceLocation.tryParse(json.get("model").getAsString());
      ResourceLocation animationPath = ResourceLocation.tryParse(json.get("animation").getAsString());
      ResourceLocation poseAnimationPath = json.has("pose_animation") ? ResourceLocation.tryParse(json.get("pose_animation").getAsString()) : null;
      String type = json.has("type") ? json.get("type").getAsString() : "static";
      if ("player".equals(type)) {
         UUID playerUUID = json.has("player_uuid") ? UUID.fromString(json.get("player_uuid").getAsString()) : null;
         PopBlockColor favoriteColor = null;
         if (json.has("favorite_color")) {
            try {
               favoriteColor = PopBlockColor.valueOf(json.get("favorite_color").getAsString().toUpperCase());
            } catch (IllegalArgumentException var16) {
               favoriteColor = PopBlockColor.ORIGINAL;
            }
         }

         float scale = json.has("scale") ? json.get("scale").getAsFloat() : 1.0F;
         FigureDefinition def = new FigureDefinition(id, name, modelPath, animationPath, playerUUID, favoriteColor);
         def.scale = scale;
         def.offsetX = json.has("offset_x") ? json.get("offset_x").getAsFloat() : 0.0F;
         def.offsetZ = json.has("offset_z") ? json.get("offset_z").getAsFloat() : 0.0F;
         def.guiScale = json.has("gui_scale") ? json.get("gui_scale").getAsFloat() : scale;
         boolean isDefaultModel = modelPath != null && modelPath.getPath().equals("geo/figure/box_figure_default.geo.json");
         def.showBoxFace = json.has("show_box_face") ? json.get("show_box_face").getAsBoolean() : isDefaultModel;
         def.boxFaceUV = parseBoxFaceUV(json);
         def.poseLocked = json.has("pose_locked") && json.get("pose_locked").getAsBoolean();
         return def;
      } else {
         ResourceLocation texturePath = ResourceLocation.tryParse(json.get("texture").getAsString());
         List<AlternativeSkin> alternatives = new ArrayList<>();
         if (json.has("alternatives")) {
            JsonArray alternativesArray = json.getAsJsonArray("alternatives");

            for (int i = 0; i < alternativesArray.size(); i++) {
               JsonObject altJson = alternativesArray.get(i).getAsJsonObject();
               alternatives.add(AlternativeSkin.fromJson(altJson));
            }
         }

         String authorUrl = json.has("author_url") ? json.get("author_url").getAsString() : null;
         float scale = json.has("scale") ? json.get("scale").getAsFloat() : 1.0F;
         FigureDefinition def = new FigureDefinition(id, name, modelPath, texturePath, animationPath, poseAnimationPath, alternatives, authorUrl);
         def.scale = scale;
         def.offsetX = json.has("offset_x") ? json.get("offset_x").getAsFloat() : 0.0F;
         def.offsetZ = json.has("offset_z") ? json.get("offset_z").getAsFloat() : 0.0F;
         def.guiScale = json.has("gui_scale") ? json.get("gui_scale").getAsFloat() : scale;
         boolean isDefaultModel = modelPath != null && modelPath.getPath().equals("geo/figure/box_figure_default.geo.json");
         def.showBoxFace = json.has("show_box_face") ? json.get("show_box_face").getAsBoolean() : isDefaultModel;
         def.boxFaceUV = parseBoxFaceUV(json);
         def.poseLocked = json.has("pose_locked") && json.get("pose_locked").getAsBoolean();
         if (json.has("hidden_bones")) {
            List<String> bones = new ArrayList<>();
            JsonArray bonesArray = json.getAsJsonArray("hidden_bones");

            for (int i = 0; i < bonesArray.size(); i++) {
               bones.add(bonesArray.get(i).getAsString());
            }

            def.hiddenBones = bones;
         }

         if (json.has("extra_textures")) {
            List<ExtraTexture> extras = new ArrayList<>();
            JsonArray extrasArray = json.getAsJsonArray("extra_textures");

            for (int i = 0; i < extrasArray.size(); i++) {
               extras.add(ExtraTexture.fromJson(extrasArray.get(i).getAsJsonObject()));
            }

            def.extraTextures = extras;
         }

         return def;
      }
   }

   @Nullable
   private static float[] parseBoxFaceUV(JsonObject json) {
      if (!json.has("box_face_uv")) {
         return null;
      } else {
         JsonObject uv = json.getAsJsonObject("box_face_uv");
         return new float[]{
            uv.get("u").getAsFloat(),
            uv.get("v").getAsFloat(),
            uv.get("w").getAsFloat(),
            uv.get("h").getAsFloat(),
            uv.get("tex_width").getAsFloat(),
            uv.get("tex_height").getAsFloat()
         };
      }
   }

   public String getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public ResourceLocation getModelPath() {
      return this.modelPath;
   }

   public ResourceLocation getTexturePath() {
      return this.texturePath;
   }

   public ResourceLocation getAnimationPath() {
      return this.animationPath;
   }

   @Nullable
   public ResourceLocation getPoseAnimationPath() {
      return this.poseAnimationPath;
   }

   public FigureType getType() {
      return this.type;
   }

   public UUID getPlayerUUID() {
      return this.playerUUID;
   }

   public List<AlternativeSkin> getAlternatives() {
      return Collections.unmodifiableList(this.alternatives);
   }

   public boolean hasAlternatives() {
      return !this.alternatives.isEmpty();
   }

   @Nullable
   public PopBlockColor getFavoriteColor() {
      return this.favoriteColor;
   }

   @Nullable
   public String getAuthorUrl() {
      return this.authorUrl;
   }

   public boolean hasAuthorUrl() {
      return this.authorUrl != null && !this.authorUrl.isEmpty();
   }

   public float getScale() {
      return this.scale;
   }

   public float getGuiScale() {
      return this.guiScale;
   }

   public boolean showBoxFace() {
      return this.showBoxFace;
   }

   @Nullable
   public float[] getBoxFaceUV() {
      return this.boxFaceUV;
   }

   public float getOffsetX() {
      return this.offsetX;
   }

   public float getOffsetZ() {
      return this.offsetZ;
   }

   public boolean isPoseLocked() {
      return this.poseLocked;
   }

   public float getScaleForSkinIndex(int skinIndex) {
      if (skinIndex > 0 && this.hasAlternatives()) {
         int altListIndex = skinIndex - 1;
         if (altListIndex < this.alternatives.size()) {
            Float altScale = this.alternatives.get(altListIndex).scale();
            if (altScale != null) {
               return altScale;
            }
         }
      }

      return this.scale;
   }

   public boolean getShowBoxFaceForSkinIndex(int skinIndex) {
      if (skinIndex > 0 && this.hasAlternatives()) {
         int altListIndex = skinIndex - 1;
         if (altListIndex < this.alternatives.size()) {
            Boolean altVal = this.alternatives.get(altListIndex).showBoxFace();
            if (altVal != null) {
               return altVal;
            }
         }
      }

      return this.showBoxFace;
   }

   @Nullable
   public float[] getBoxFaceUVForSkinIndex(int skinIndex) {
      if (skinIndex > 0 && this.hasAlternatives()) {
         int altListIndex = skinIndex - 1;
         if (altListIndex < this.alternatives.size() && this.alternatives.get(altListIndex).showBoxFace() != null) {
            return this.alternatives.get(altListIndex).boxFaceUV();
         }
      }

      return this.boxFaceUV;
   }

   public List<String> getHiddenBones() {
      return this.hiddenBones;
   }

   public List<ExtraTexture> getExtraTextures() {
      return this.extraTextures;
   }

   public List<String> getHiddenBonesForSkinIndex(int skinIndex) {
      if (skinIndex > 0 && this.hasAlternatives()) {
         int altListIndex = skinIndex - 1;
         if (altListIndex < this.alternatives.size()) {
            List<String> altBones = this.alternatives.get(altListIndex).hiddenBones();
            if (!altBones.isEmpty()) {
               return altBones;
            }
         }
      }

      return this.hiddenBones;
   }

   public ResourceLocation getModelForSkinIndex(int skinIndex) {
      if (skinIndex > 0 && this.hasAlternatives()) {
         int altListIndex = skinIndex - 1;
         if (altListIndex < this.alternatives.size()) {
            ResourceLocation altModel = this.alternatives.get(altListIndex).model();
            if (altModel != null) {
               return altModel;
            }
         }
      }

      return this.modelPath;
   }

   public Set<String> getAllVariantBoneNames() {
      Set<String> allNames = new HashSet<>(this.hiddenBones);

      for (AlternativeSkin alt : this.alternatives) {
         allNames.addAll(alt.hiddenBones());
      }

      return allNames;
   }

   public JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.addProperty("id", this.id);
      json.addProperty("name", this.name);
      json.addProperty("model", this.modelPath.toString());
      json.addProperty("animation", this.animationPath.toString());
      if (this.poseAnimationPath != null) {
         json.addProperty("pose_animation", this.poseAnimationPath.toString());
      }

      if (this.type == FigureType.PLAYER) {
         json.addProperty("type", "player");
         if (this.playerUUID != null) {
            json.addProperty("player_uuid", this.playerUUID.toString());
         }

         if (this.favoriteColor != null) {
            json.addProperty("favorite_color", this.favoriteColor.getSerializedName());
         }
      } else {
         json.addProperty("type", "static");
         if (this.texturePath != null) {
            json.addProperty("texture", this.texturePath.toString());
         }

         if (!this.alternatives.isEmpty()) {
            JsonArray alternativesArray = new JsonArray();

            for (AlternativeSkin alt : this.alternatives) {
               JsonObject altJson = new JsonObject();
               altJson.addProperty("name", alt.name());
               if (alt.model() != null) {
                  altJson.addProperty("model", alt.model().toString());
               }

               altJson.addProperty("texture", alt.texture().toString());
               if (!alt.hiddenBones().isEmpty()) {
                  JsonArray altBonesArray = new JsonArray();

                  for (String bone : alt.hiddenBones()) {
                     altBonesArray.add(bone);
                  }

                  altJson.add("hidden_bones", altBonesArray);
               }

               alternativesArray.add(altJson);
            }

            json.add("alternatives", alternativesArray);
         }

         if (this.authorUrl != null && !this.authorUrl.isEmpty()) {
            json.addProperty("author_url", this.authorUrl);
         }
      }

      if (this.scale != 1.0F) {
         json.addProperty("scale", this.scale);
      }

      if (this.guiScale != 1.0F) {
         json.addProperty("gui_scale", this.guiScale);
      }

      if (!this.showBoxFace) {
         json.addProperty("show_box_face", false);
      }

      if (this.boxFaceUV != null) {
         JsonObject uvObj = new JsonObject();
         uvObj.addProperty("u", this.boxFaceUV[0]);
         uvObj.addProperty("v", this.boxFaceUV[1]);
         uvObj.addProperty("w", this.boxFaceUV[2]);
         uvObj.addProperty("h", this.boxFaceUV[3]);
         uvObj.addProperty("tex_width", this.boxFaceUV[4]);
         uvObj.addProperty("tex_height", this.boxFaceUV[5]);
         json.add("box_face_uv", uvObj);
      }

      if (this.poseLocked) {
         json.addProperty("pose_locked", true);
      }

      if (this.offsetX != 0.0F) {
         json.addProperty("offset_x", this.offsetX);
      }

      if (this.offsetZ != 0.0F) {
         json.addProperty("offset_z", this.offsetZ);
      }

      if (!this.hiddenBones.isEmpty()) {
         JsonArray bonesArray = new JsonArray();

         for (String bone : this.hiddenBones) {
            bonesArray.add(bone);
         }

         json.add("hidden_bones", bonesArray);
      }

      if (!this.extraTextures.isEmpty()) {
         JsonArray extrasArray = new JsonArray();

         for (ExtraTexture extra : this.extraTextures) {
            JsonObject extraJson = new JsonObject();
            extraJson.addProperty("texture", extra.texture().toString());
            JsonArray extraBonesArray = new JsonArray();

            for (String bone : extra.bones()) {
               extraBonesArray.add(bone);
            }

            extraJson.add("bones", extraBonesArray);
            extrasArray.add(extraJson);
         }

         json.add("extra_textures", extrasArray);
      }

      return json;
   }

   @Override
   public String toString() {
      return "FigureDefinition{id='" + this.id + "', name='" + this.name + "', type=" + this.type + "}";
   }

   public static record AlternativeSkin(
      String name,
      @Nullable ResourceLocation model,
      ResourceLocation texture,
      List<String> hiddenBones,
      @Nullable Boolean showBoxFace,
      @Nullable float[] boxFaceUV,
      @Nullable Float scale
   ) {
      public static AlternativeSkin fromJson(JsonObject json) {
         String name = json.get("name").getAsString();
         ResourceLocation model = json.has("model") ? ResourceLocation.tryParse(json.get("model").getAsString()) : null;
         ResourceLocation texture = ResourceLocation.tryParse(json.get("texture").getAsString());
         List<String> hiddenBones = new ArrayList<>();
         if (json.has("hidden_bones")) {
            JsonArray bonesArray = json.getAsJsonArray("hidden_bones");

            for (int i = 0; i < bonesArray.size(); i++) {
               hiddenBones.add(bonesArray.get(i).getAsString());
            }
         }

         Boolean showBoxFace = json.has("show_box_face") ? json.get("show_box_face").getAsBoolean() : null;
         float[] boxFaceUV = FigureDefinition.parseBoxFaceUV(json);
         Float scale = json.has("scale") ? json.get("scale").getAsFloat() : null;
         return new AlternativeSkin(name, model, texture, hiddenBones, showBoxFace, boxFaceUV, scale);
      }
   }

   public static record ExtraTexture(ResourceLocation texture, List<String> bones) {
      public static ExtraTexture fromJson(JsonObject json) {
         ResourceLocation texture = ResourceLocation.tryParse(json.get("texture").getAsString());
         List<String> bones = new ArrayList<>();
         JsonArray bonesArray = json.getAsJsonArray("bones");

         for (int i = 0; i < bonesArray.size(); i++) {
            bones.add(bonesArray.get(i).getAsString());
         }

         return new ExtraTexture(texture, bones);
      }
   }
}
