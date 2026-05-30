package com.theplumteam.server.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.block.PopBlockColor;
import com.theplumteam.platform.PlatformHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ServerConfig {
   private static ServerConfig instance;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   public int guaranteedTokenResetHour = 18;
   public String defaultPlayerColor = "original";
   public boolean debugLogging = false;
   public int regularTokenCooldownHours = 3;
   public int maxRegularTokens = 3;
   public boolean showColorSelectionOnJoin = true;
   public List<String> hiddenCollections = new ArrayList<>();

   private ServerConfig() {
   }

   public static ServerConfig getInstance() {
      if (instance == null) {
         instance = load();
      }

      return instance;
   }

   public int getGuaranteedTokenResetHour() {
      return this.guaranteedTokenResetHour;
   }

   public void setGuaranteedTokenResetHour(int hour) {
      this.guaranteedTokenResetHour = Math.max(0, Math.min(23, hour));
      this.save();
   }

   public PopBlockColor getDefaultPlayerColor() {
      try {
         return PopBlockColor.valueOf(this.defaultPlayerColor.toUpperCase());
      } catch (NullPointerException | IllegalArgumentException var2) {
         return PopBlockColor.ORIGINAL;
      }
   }

   public void setDefaultPlayerColor(PopBlockColor color) {
      this.defaultPlayerColor = color.name().toLowerCase();
      this.save();
   }

   public boolean isDebugLogging() {
      return this.debugLogging;
   }

   public void setDebugLogging(boolean enabled) {
      this.debugLogging = enabled;
      this.save();
   }

   public int getRegularTokenCooldownHours() {
      return this.regularTokenCooldownHours;
   }

   public void setRegularTokenCooldownHours(int hours) {
      this.regularTokenCooldownHours = Math.max(1, Math.min(168, hours));
      this.save();
   }

   public int getMaxRegularTokens() {
      return this.maxRegularTokens;
   }

   public void setMaxRegularTokens(int max) {
      this.maxRegularTokens = Math.max(1, Math.min(99, max));
      this.save();
   }

   public boolean isShowColorSelectionOnJoin() {
      return this.showColorSelectionOnJoin;
   }

   public void setShowColorSelectionOnJoin(boolean show) {
      this.showColorSelectionOnJoin = show;
      this.save();
   }

   public List<String> getHiddenCollections() {
      if (this.hiddenCollections == null) {
         this.hiddenCollections = new ArrayList<>();
      }

      return this.hiddenCollections;
   }

   public void setHiddenCollections(List<String> hidden) {
      this.hiddenCollections = hidden != null ? new ArrayList<>(hidden) : new ArrayList<>();
      this.save();
   }

   private static ServerConfig load() {
      Path configPath = getConfigPath();
      if (Files.exists(configPath)) {
         try {
            String json = Files.readString(configPath);
            ServerConfig config = GSON.fromJson(json, ServerConfig.class);
            BlockPopsMod.LOGGER.debug("Loaded server configuration");
            return config;
         } catch (Exception var3) {
            BlockPopsMod.LOGGER.error("Failed to load server configuration, using defaults", var3);
         }
      }

      ServerConfig config = new ServerConfig();
      config.save();
      return config;
   }

   public void save() {
      Path configPath = getConfigPath();

      try {
         Files.createDirectories(configPath.getParent());
         String json = GSON.toJson(this);
         Files.writeString(configPath, json);
         BlockPopsMod.LOGGER.debug("Saved server configuration");
      } catch (IOException var3) {
         BlockPopsMod.LOGGER.error("Failed to save server configuration", var3);
      }
   }

   private static Path getConfigPath() {
      return PlatformHelper.getConfigDirectory().resolve("blockpops-server.json");
   }

   public static void reload() {
      instance = load();
   }
}
