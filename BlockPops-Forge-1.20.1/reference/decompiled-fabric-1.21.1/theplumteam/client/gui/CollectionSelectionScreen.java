package com.theplumteam.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.theplumteam.BlockPopsMod;
import com.theplumteam.client.config.ClientConfig;
import com.theplumteam.client.config.ClientServerConfig;
import com.theplumteam.client.discovery.ClientDiscoveryManager;
import com.theplumteam.client.gui.util.GuiScaleManager;
import com.theplumteam.client.gui.widget.CollectionEntry;
import com.theplumteam.client.gui.widget.CollectionListWidget;
import com.theplumteam.client.gui.widget.FigureListWidget;
import com.theplumteam.client.gui.widget.LinkButton;
import com.theplumteam.client.renderer.FigureWidgetRenderer;
import com.theplumteam.client.token.ClientTokenManager;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import com.theplumteam.figure.FigureDefinition;
import com.theplumteam.network.ClawMachineCollectionPacket;
import com.theplumteam.network.DropBoxPacket;
import com.theplumteam.network.TokenType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.class_2338;
import net.minecraft.class_2561;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import net.minecraft.class_293.class_5596;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectionSelectionScreen extends class_437 {
   private static final Logger LOGGER = LoggerFactory.getLogger(CollectionSelectionScreen.class);
   private final class_2338 blockPos;
   private String selectedCollectionId;
   private final List<FigureCollection> collections;
   @Nullable
   private CollectionListWidget collectionListWidget;
   @Nullable
   private FigureListWidget figureListWidget;
   private class_4185 useRegularButton;
   private class_4185 useSpecialButton;
   private class_4185 doneButton;
   private int panelX;
   private int panelY;
   private int panelWidth;
   private int panelHeight;
   private static final int MIN_PANEL_WIDTH = 500;
   private static final int MAX_PANEL_WIDTH = 1200;
   private static final int MIN_PANEL_HEIGHT = 400;
   private float currentColorR;
   private float currentColorG;
   private float currentColorB;
   private float targetColorR;
   private float targetColorG;
   private float targetColorB;
   private float colorTransitionProgress = 1.0F;
   private static final float COLOR_TRANSITION_SPEED = 0.05F;
   private static final float COLOR_SNAP_THRESHOLD = 0.95F;
   private boolean guiScaleForced = false;
   private boolean isClosing = false;
   private static final class_2960 DISCORD_ICON = class_2960.method_60655("blockpops", "textures/gui/discord_icon.png");
   private static final class_2960 CURSEFORGE_ICON = class_2960.method_60655("blockpops", "textures/gui/curseforge_icon.png");
   private static final class_2960 MODRINTH_ICON = class_2960.method_60655("blockpops", "textures/gui/modrinth_icon.png");
   private static final class_2960 SETTINGS_ICON = class_2960.method_60655("blockpops", "textures/gui/settings_icon.png");
   private static final class_2960 PALETTE_ICON = class_2960.method_60655("blockpops", "textures/gui/palette_icon.png");
   private static final String DISCORD_URL = "https://discord.gg/yGxdvA7qej";
   private static final String CURSEFORGE_URL = "https://www.curseforge.com/minecraft/mc-mods/blockpops";
   private static final String MODRINTH_URL = "https://modrinth.com/mod/blockpops";

   public CollectionSelectionScreen(class_2338 blockPos, String currentCollectionId) {
      super(class_2561.method_43470("Claw Machine Configuration"));
      this.blockPos = blockPos;
      this.selectedCollectionId = currentCollectionId;
      this.collections = new ArrayList<>();
      ClientConfig config = ClientConfig.getInstance();
      this.currentColorR = config.backgroundColorR;
      this.currentColorG = config.backgroundColorG;
      this.currentColorB = config.backgroundColorB;
      this.targetColorR = config.backgroundColorR;
      this.targetColorG = config.backgroundColorG;
      this.targetColorB = config.backgroundColorB;
      this.colorTransitionProgress = 1.0F;
      BlockPopsMod.logDebug("CollectionSelectionScreen opened at {} with current collection: {}", blockPos, currentCollectionId);
   }

   protected void method_25426() {
      FigureWidgetRenderer.ensureInitialized();
      if (!this.guiScaleForced && !this.isClosing) {
         this.guiScaleForced = true;
         int optimalScale = GuiScaleManager.getOptimalMenuScale();
         if (GuiScaleManager.setMenuGuiScale(optimalScale)) {
            return;
         }
      }

      super.method_25426();
      this.method_37067();
      this.collections.clear();
      CollectionRegistry.getAllCollections()
         .stream()
         .filter(c -> !"default".equals(c.getId()))
         .filter(c -> !ClientServerConfig.isCollectionHidden(c.getId()))
         .forEach(this.collections::add);
      Set<String> remoteIds = ClientServerConfig.getEnabledRemoteCollections();
      this.collections.sort((c1, c2) -> {
         boolean c1IsPlayers = "world_players".equals(c1.getId());
         boolean c2IsPlayers = "world_players".equals(c2.getId());
         boolean c1IsRemote = remoteIds.contains(c1.getId());
         boolean c2IsRemote = remoteIds.contains(c2.getId());
         int priority1 = c1IsPlayers ? 0 : (c1IsRemote ? 1 : 2);
         int priority2 = c2IsPlayers ? 0 : (c2IsRemote ? 1 : 2);
         return Integer.compare(priority1, priority2);
      });
      this.calculatePanelDimensions();
      int scaledPadding = 10;
      int scaledSpacing = 6;
      int scaledComponentHeight = 20;
      int leftPanelWidth = (int)((float)this.panelWidth * 0.45F);
      int rightPanelWidth = (int)((float)this.panelWidth * 0.5F);
      int componentX = this.panelX + scaledPadding;
      int yPos = this.panelY + scaledPadding + scaledComponentHeight + scaledPadding;
      int topSectionHeight = scaledPadding + scaledComponentHeight + scaledPadding;
      int tokenInfoHeight = 9 + scaledSpacing;
      int extraBottomSpacing = 5;
      int bottomSectionHeight = tokenInfoHeight + scaledComponentHeight * 2 + scaledSpacing + scaledPadding + extraBottomSpacing;
      int listHeight = this.panelHeight - topSectionHeight - bottomSectionHeight;
      int collectionHeaderHeight = 30;
      this.collectionListWidget = new CollectionListWidget(this, this.field_22787, leftPanelWidth, listHeight, yPos, 55);
      this.collectionListWidget.setXPosition(componentX);
      this.method_25429(this.collectionListWidget);
      this.loadCollections();
      int previewX = this.panelX + this.panelWidth - rightPanelWidth - scaledPadding;
      int headerHeight = 50;
      this.figureListWidget = new FigureListWidget(this.field_22787, rightPanelWidth, listHeight, yPos, 90);
      this.figureListWidget.setXPosition(previewX);
      this.method_25429(this.figureListWidget);
      if (this.selectedCollectionId != null && !this.selectedCollectionId.isEmpty()) {
         CollectionRegistry.getCollection(this.selectedCollectionId).ifPresent(collection -> {
            if (this.figureListWidget != null) {
               this.figureListWidget.setCollection(collection);
            }
         });
      }

      int bottomY = this.panelY + this.panelHeight - scaledPadding;
      int fullWidthX = this.panelX + scaledPadding;
      int fullComponentWidth = this.panelWidth - scaledPadding * 2;
      bottomY -= scaledComponentHeight;
      this.doneButton = class_4185.method_46430(class_2561.method_43470("Done"), button -> this.method_25419())
         .method_46434(fullWidthX, bottomY, fullComponentWidth, scaledComponentHeight)
         .method_46431();
      this.method_37063(this.doneButton);
      bottomY -= scaledComponentHeight + scaledSpacing;
      int buttonWidth = (fullComponentWidth - scaledSpacing) / 2;
      this.useRegularButton = class_4185.method_46430(class_2561.method_43470("Use Regular Token"), button -> {
         if (this.selectedCollectionId != null && !this.selectedCollectionId.isEmpty()) {
            BlockPopsMod.logDebug("Using regular token for collection: {}", this.selectedCollectionId);
            DropBoxPacket packet = new DropBoxPacket(this.blockPos, this.selectedCollectionId, TokenType.REGULAR);
            packet.sendToServer();
            this.method_25419();
         }
      }).method_46434(fullWidthX, bottomY, buttonWidth, scaledComponentHeight).method_46431();
      this.method_37063(this.useRegularButton);
      this.useSpecialButton = class_4185.method_46430(class_2561.method_43470("Use Guaranteed Token"), button -> {
         if (this.selectedCollectionId != null && !this.selectedCollectionId.isEmpty()) {
            BlockPopsMod.logDebug("Using guaranteed token for collection: {}", this.selectedCollectionId);
            DropBoxPacket packet = new DropBoxPacket(this.blockPos, this.selectedCollectionId, TokenType.GUARANTEED);
            packet.sendToServer();
            this.method_25419();
         }
      }).method_46434(fullWidthX + buttonWidth + scaledSpacing, bottomY, buttonWidth, scaledComponentHeight).method_46431();
      this.method_37063(this.useSpecialButton);
      this.updateTokenButtonStates();
      int buttonSize = 24;
      int linkButtonY = this.panelY + scaledPadding;
      int settingsButtonX = this.panelX + this.panelWidth - buttonSize - scaledPadding;
      if (BlockPopsMod.LOCAL_ADMIN) {
         this.method_37063(new LinkButton(settingsButtonX, linkButtonY, buttonSize, buttonSize, SETTINGS_ICON, null, class_2561.method_43470("Settings")) {
            public void method_25306() {
               CollectionSelectionScreen.this.openSettingsScreen();
            }
         });
      }

      int discordButtonX = settingsButtonX - buttonSize - scaledSpacing;
      this.method_37063(
         new LinkButton(
            discordButtonX, linkButtonY, buttonSize, buttonSize, DISCORD_ICON, "https://discord.gg/yGxdvA7qej", class_2561.method_43470("Join our Discord!")
         )
      );
      int curseforgeButtonX = discordButtonX - buttonSize - scaledSpacing;
      this.method_37063(
         new LinkButton(
            curseforgeButtonX,
            linkButtonY,
            buttonSize,
            buttonSize,
            CURSEFORGE_ICON,
            "https://www.curseforge.com/minecraft/mc-mods/blockpops",
            class_2561.method_43470("Visit our CurseForge page")
         )
      );
      int modrinthButtonX = curseforgeButtonX - buttonSize - scaledSpacing;
      this.method_37063(
         new LinkButton(
            modrinthButtonX,
            linkButtonY,
            buttonSize,
            buttonSize,
            MODRINTH_ICON,
            "https://modrinth.com/mod/blockpops",
            class_2561.method_43470("Visit our Modrinth page")
         )
      );
      int paletteButtonX = modrinthButtonX - buttonSize - scaledSpacing;
      this.method_37063(
         new LinkButton(paletteButtonX, linkButtonY, buttonSize, buttonSize, PALETTE_ICON, null, class_2561.method_43470("Change Favorite Color")) {
            public void method_25306() {
               CollectionSelectionScreen.this.openFavoriteColorScreen();
            }
         }
      );
   }

   private void calculatePanelDimensions() {
      int screenWidth = GuiScaleManager.isUsingInverseScale() ? GuiScaleManager.getVirtualWidth() : this.field_22789;
      int screenHeight = GuiScaleManager.isUsingInverseScale() ? GuiScaleManager.getVirtualHeight() : this.field_22790;
      int desiredWidth = (int)((float)screenWidth * 0.8F);
      int desiredHeight = (int)((float)screenHeight * 0.85F);
      this.panelWidth = class_3532.method_15340(desiredWidth, 500, Math.min(1200, screenWidth - 60));
      this.panelHeight = class_3532.method_15340(desiredHeight, 400, screenHeight - 60);
      this.panelX = (screenWidth - this.panelWidth) / 2;
      this.panelY = (screenHeight - this.panelHeight) / 2;
   }

   public void method_25432() {
      super.method_25432();
      if (this.isClosing) {
         this.restoreGuiScaleIfNeeded();
      }
   }

   public void method_25419() {
      this.isClosing = true;
      this.restoreGuiScaleIfNeeded();
      super.method_25419();
   }

   private void restoreGuiScaleIfNeeded() {
      if (this.guiScaleForced) {
         this.guiScaleForced = false;
         GuiScaleManager.restoreOriginalGuiScale();
      }
   }

   public void method_25394(@NotNull class_332 graphics, int mouseX, int mouseY, float partialTick) {
      int adjustedMouseX = mouseX;
      int adjustedMouseY = mouseY;
      if (GuiScaleManager.isUsingInverseScale()) {
         adjustedMouseX = (int)GuiScaleManager.transformMouseX((double)mouseX);
         adjustedMouseY = (int)GuiScaleManager.transformMouseY((double)mouseY);
      }

      if (GuiScaleManager.isUsingInverseScale()) {
         graphics.method_51448().method_22903();
         float scale = GuiScaleManager.getRenderScaleFactor();
         graphics.method_51448().method_22905(scale, scale, 1.0F);
      }

      this.renderBackgroundEffects(graphics, partialTick);
      graphics.method_51452();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      this.renderPanel(graphics);
      super.method_25394(graphics, adjustedMouseX, adjustedMouseY, partialTick);
      this.renderTokenInfo(graphics);
      this.renderCollectionListHeader(graphics);
      this.renderFigurePanelHeader(graphics);
      if (GuiScaleManager.isUsingInverseScale()) {
         graphics.method_51448().method_22909();
      }

      if (this.collectionListWidget != null) {
         this.collectionListWidget.method_25394(graphics, adjustedMouseX, adjustedMouseY, partialTick);
      }

      if (this.figureListWidget != null) {
         this.figureListWidget.method_25394(graphics, adjustedMouseX, adjustedMouseY, partialTick);
      }
   }

   private void renderCollectionListHeader(class_332 graphics) {
      if (this.collectionListWidget != null) {
         int scaledPadding = 10;
         int scaledComponentHeight = 20;
         int leftPanelWidth = (int)((float)this.panelWidth * 0.45F);
         int componentX = this.panelX + scaledPadding;
         int headerStartY = this.panelY + scaledPadding;
         int headerHeight = scaledComponentHeight + scaledPadding;
         int currentY = headerStartY + 4;
         graphics.method_51433(this.field_22793, "Collections", componentX + 8, currentY, 16777215, false);
         currentY += 9 + 4;
         String collectionCount = this.collections.size() + " collections available";
         graphics.method_51433(this.field_22793, collectionCount, componentX + 8, currentY, 11184810, false);
         currentY += 9 + 4;
         graphics.method_25294(componentX + 8, currentY, componentX + leftPanelWidth - 8, currentY + 1, 1090519039);
      }
   }

   private void renderFigurePanelHeader(class_332 graphics) {
      if (this.figureListWidget != null) {
         FigureCollection collection = this.figureListWidget.getCurrentCollection();
         if (collection != null) {
            int scaledPadding = 10;
            int scaledComponentHeight = 20;
            int rightPanelWidth = (int)((float)this.panelWidth * 0.5F);
            int previewX = this.panelX + this.panelWidth - rightPanelWidth - scaledPadding;
            int headerStartY = this.panelY + scaledPadding;
            int currentY = headerStartY + 4;
            graphics.method_51433(this.field_22793, collection.getName(), previewX + 8, currentY, 16777215, false);
            currentY += 9 + 4;
            String figureCount = collection.getFigures().size() + " figures in this collection";
            graphics.method_51433(this.field_22793, figureCount, previewX + 8, currentY, 11184810, false);
            currentY += 9 + 4;
            graphics.method_25294(previewX + 8, currentY, previewX + rightPanelWidth - 8, currentY + 1, 1090519039);
         }
      }
   }

   private void renderBackgroundEffects(class_332 graphics, float partialTick) {
      ClientConfig config = ClientConfig.getInstance();
      if (this.colorTransitionProgress < 1.0F) {
         this.colorTransitionProgress = Math.min(1.0F, this.colorTransitionProgress + 0.05F);
         if (this.colorTransitionProgress >= 0.95F) {
            this.colorTransitionProgress = 1.0F;
         }

         if (this.colorTransitionProgress >= 1.0F) {
            config.backgroundColorR = this.targetColorR;
            config.backgroundColorG = this.targetColorG;
            config.backgroundColorB = this.targetColorB;
         }
      }

      float lerpedR;
      float lerpedG;
      float lerpedB;
      if (this.colorTransitionProgress >= 1.0F) {
         lerpedR = config.backgroundColorR;
         lerpedG = config.backgroundColorG;
         lerpedB = config.backgroundColorB;
      } else {
         lerpedR = class_3532.method_16439(this.colorTransitionProgress, this.currentColorR, this.targetColorR);
         lerpedG = class_3532.method_16439(this.colorTransitionProgress, this.currentColorG, this.targetColorG);
         lerpedB = class_3532.method_16439(this.colorTransitionProgress, this.currentColorB, this.targetColorB);
      }

      int bgRed = (int)(lerpedR * 255.0F);
      int bgGreen = (int)(lerpedG * 255.0F);
      int bgBlue = (int)(lerpedB * 255.0F);
      int bgColor = 0xFF000000 | bgRed << 16 | bgGreen << 8 | bgBlue;
      int bgWidth = GuiScaleManager.isUsingInverseScale() ? GuiScaleManager.getVirtualWidth() : this.field_22789;
      int bgHeight = GuiScaleManager.isUsingInverseScale() ? GuiScaleManager.getVirtualHeight() : this.field_22790;
      graphics.method_25294(0, 0, bgWidth, bgHeight, bgColor);
      this.renderStarPattern(graphics, partialTick);
   }

   private void renderStarPattern(class_332 graphics, float partialTick) {
      double pixelsPerSecond = 5.0;
      int tileSize = StarPatternCache.getTileSize();
      int tickCount = this.field_22787 != null ? this.field_22787.field_1705.method_1738() : 0;
      double smoothTime = (double)((float)tickCount + partialTick) / 20.0;
      double offsetX = smoothTime * pixelsPerSecond % (double)tileSize;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      ClientConfig config = ClientConfig.getInstance();
      RenderSystem.setShaderColor(config.starColorR, config.starColorG, config.starColorB, config.starOpacity);
      class_2960 cacheTexture = StarPatternCache.getTextureLocation();
      int cacheWidth = StarPatternCache.getTextureWidth();
      int cacheHeight = StarPatternCache.getTextureHeight();
      float u0 = (float)offsetX / (float)cacheWidth;
      float v0 = 0.0F;
      float u1 = u0 + (float)(GuiScaleManager.isUsingInverseScale() ? GuiScaleManager.getVirtualWidth() : this.field_22789) / (float)cacheWidth;
      float v1 = (float)(GuiScaleManager.isUsingInverseScale() ? GuiScaleManager.getVirtualHeight() : this.field_22790) / (float)cacheHeight;
      class_4587 pose = graphics.method_51448();
      pose.method_22903();
      RenderSystem.setShaderTexture(0, cacheTexture);
      RenderSystem.setShader(class_757::method_34542);
      int starHeight = GuiScaleManager.isUsingInverseScale() ? GuiScaleManager.getVirtualHeight() : this.field_22790;
      int starWidth = GuiScaleManager.isUsingInverseScale() ? GuiScaleManager.getVirtualWidth() : this.field_22789;
      class_287 bufferBuilder = class_289.method_1348().method_60827(class_5596.field_27382, class_290.field_1585);
      bufferBuilder.method_22918(pose.method_23760().method_23761(), 0.0F, (float)starHeight, 0.0F).method_22913(u0, v1);
      bufferBuilder.method_22918(pose.method_23760().method_23761(), (float)starWidth, (float)starHeight, 0.0F).method_22913(u1, v1);
      bufferBuilder.method_22918(pose.method_23760().method_23761(), (float)starWidth, 0.0F, 0.0F).method_22913(u1, v0);
      bufferBuilder.method_22918(pose.method_23760().method_23761(), 0.0F, 0.0F, 0.0F).method_22913(u0, v0);
      class_286.method_43433(bufferBuilder.method_60800());
      pose.method_22909();
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderPanel(class_332 graphics) {
      ClientConfig config = ClientConfig.getInstance();
      int alpha = (int)(config.panelOpacity * 255.0F);
      int panelBgColor = alpha << 24 | 0;
      graphics.method_25294(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, panelBgColor);
      graphics.method_25294(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + 1, 1627389951);
      graphics.method_25294(this.panelX, this.panelY + this.panelHeight - 1, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 1627389951);
      graphics.method_25294(this.panelX, this.panelY + 1, this.panelX + 1, this.panelY + this.panelHeight - 1, 1627389951);
      graphics.method_25294(this.panelX + this.panelWidth - 1, this.panelY + 1, this.panelX + this.panelWidth, this.panelY + this.panelHeight - 1, 1627389951);
   }

   private void loadCollections() {
      if (this.collectionListWidget != null) {
         for (FigureCollection collection : this.collections) {
            this.collectionListWidget.addCollectionEntry(collection);
         }

         if (this.selectedCollectionId != null && !this.selectedCollectionId.isEmpty()) {
            this.collectionListWidget.selectByCollectionId(this.selectedCollectionId);
         }
      }
   }

   public void onCollectionSelected(CollectionEntry entry) {
      if (entry != null) {
         FigureCollection collection = entry.getCollection();
         this.selectedCollectionId = collection.getId();
         if (collection.hasBackgroundColor()) {
            int[] bgColor = collection.getBackgroundColor();
            ClientConfig config = ClientConfig.getInstance();
            this.currentColorR = config.backgroundColorR;
            this.currentColorG = config.backgroundColorG;
            this.currentColorB = config.backgroundColorB;
            this.targetColorR = (float)bgColor[0] / 255.0F;
            this.targetColorG = (float)bgColor[1] / 255.0F;
            this.targetColorB = (float)bgColor[2] / 255.0F;
            if (config.enableColorTransition) {
               this.colorTransitionProgress = 0.0F;
            } else {
               this.colorTransitionProgress = 1.0F;
               config.backgroundColorR = this.targetColorR;
               config.backgroundColorG = this.targetColorG;
               config.backgroundColorB = this.targetColorB;
            }
         }

         if (this.figureListWidget != null) {
            this.figureListWidget.setCollection(collection);
         }

         this.sendUpdate();
         this.updateTokenButtonStates();
      }
   }

   private void updateTokenButtonStates() {
      boolean hasSelection = this.selectedCollectionId != null && !this.selectedCollectionId.isEmpty();
      if (this.useRegularButton != null) {
         this.useRegularButton.field_22763 = hasSelection && ClientTokenManager.getRegularTokens() > 0;
      }

      if (this.useSpecialButton != null) {
         boolean collectionComplete = hasSelection && this.isCollectionComplete();
         this.useSpecialButton.field_22763 = hasSelection && ClientTokenManager.hasSpecialToken() && !collectionComplete;
      }
   }

   private boolean isCollectionComplete() {
      return this.selectedCollectionId != null && !this.selectedCollectionId.isEmpty()
         ? CollectionRegistry.getCollection(this.selectedCollectionId).map(collection -> {
            for (FigureDefinition figure : collection.getFigures()) {
               String figureId = collection.getId() + ":" + figure.getId();
               if (!ClientDiscoveryManager.isDiscovered(figureId)) {
                  return false;
               }
            }

            return true;
         }).orElse(false)
         : false;
   }

   private void renderTokenInfo(class_332 graphics) {
      int scaledPadding = 10;
      int scaledSpacing = 6;
      int scaledComponentHeight = 20;
      int bottomY = this.panelY + this.panelHeight - scaledPadding;
      bottomY -= scaledComponentHeight;
      bottomY -= scaledComponentHeight + scaledSpacing;
      int tokenInfoY = bottomY - 9 - scaledSpacing;
      int blueColor = 5609983;
      int goldColor = 16766720;
      int fullWidthX = this.panelX + scaledPadding;
      int fullComponentWidth = this.panelWidth - scaledPadding * 2;
      int buttonWidth = (fullComponentWidth - scaledSpacing) / 2;
      int regularTokens = ClientTokenManager.getRegularTokens();
      int maxRegularTokens = ClientServerConfig.getMaxRegularTokens();
      String regularText = "Regular Tokens: " + regularTokens + "/" + maxRegularTokens;
      if (regularTokens < maxRegularTokens) {
         String nextRegularTime = ClientTokenManager.formatNextRegularTime();
         regularText = regularText + " - Next: " + nextRegularTime;
      }

      int regularButtonCenterX = fullWidthX + buttonWidth / 2;
      int regularTextWidth = this.field_22793.method_1727(regularText);
      int regularTextX = regularButtonCenterX - regularTextWidth / 2;
      graphics.method_51433(this.field_22793, regularText, regularTextX, tokenInfoY, blueColor, false);
      boolean hasSpecial = ClientTokenManager.hasSpecialToken();
      String specialText = "Guaranteed Token: " + (hasSpecial ? "Available" : "Used");
      if (!hasSpecial) {
         String nextSpecialTime = ClientTokenManager.formatNextSpecialResetTime();
         specialText = specialText + " - Resets: " + nextSpecialTime;
      }

      int specialButtonCenterX = fullWidthX + buttonWidth + scaledSpacing + buttonWidth / 2;
      int specialTextWidth = this.field_22793.method_1727(specialText);
      int specialTextX = specialButtonCenterX - specialTextWidth / 2;
      graphics.method_51433(this.field_22793, specialText, specialTextX, tokenInfoY, goldColor, false);
   }

   public void method_25393() {
      super.method_25393();
      this.updateTokenButtonStates();
      int currentCount = (int)CollectionRegistry.getAllCollections()
         .stream()
         .filter(c -> !"default".equals(c.getId()))
         .filter(c -> !ClientServerConfig.isCollectionHidden(c.getId()))
         .count();
      if (currentCount != this.collections.size()) {
         this.method_41843();
      }
   }

   private void sendUpdate() {
      BlockPopsMod.logDebug("Sending collection update - Position: {}, Collection ID: {}", this.blockPos, this.selectedCollectionId);
      ClawMachineCollectionPacket packet = new ClawMachineCollectionPacket(this.blockPos, this.selectedCollectionId);
      packet.sendToServer();
   }

   private void openSettingsScreen() {
      if (BlockPopsMod.LOCAL_ADMIN) {
         this.field_22787.method_1507(new SettingsScreen(this));
      }
   }

   private void openFavoriteColorScreen() {
      this.field_22787.method_1507(new FavoriteColorSelectionScreen());
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      if (GuiScaleManager.isUsingInverseScale()) {
         mouseX = GuiScaleManager.transformMouseX(mouseX);
         mouseY = GuiScaleManager.transformMouseY(mouseY);
      }

      return super.method_25402(mouseX, mouseY, button);
   }

   public boolean method_25406(double mouseX, double mouseY, int button) {
      if (GuiScaleManager.isUsingInverseScale()) {
         mouseX = GuiScaleManager.transformMouseX(mouseX);
         mouseY = GuiScaleManager.transformMouseY(mouseY);
      }

      return super.method_25406(mouseX, mouseY, button);
   }

   public boolean method_25403(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (GuiScaleManager.isUsingInverseScale()) {
         mouseX = GuiScaleManager.transformMouseX(mouseX);
         mouseY = GuiScaleManager.transformMouseY(mouseY);
         float scale = GuiScaleManager.getMouseScaleFactor();
         dragX *= (double)scale;
         dragY *= (double)scale;
      }

      return super.method_25403(mouseX, mouseY, button, dragX, dragY);
   }

   public boolean method_25401(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (GuiScaleManager.isUsingInverseScale()) {
         mouseX = GuiScaleManager.transformMouseX(mouseX);
         mouseY = GuiScaleManager.transformMouseY(mouseY);
      }

      return super.method_25401(mouseX, mouseY, scrollX, scrollY);
   }

   public void method_16014(double mouseX, double mouseY) {
      if (GuiScaleManager.isUsingInverseScale()) {
         mouseX = GuiScaleManager.transformMouseX(mouseX);
         mouseY = GuiScaleManager.transformMouseY(mouseY);
      }

      super.method_16014(mouseX, mouseY);
   }

   public boolean method_25421() {
      return false;
   }

   public boolean method_25404(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.method_25419();
         return true;
      } else {
         return super.method_25404(keyCode, scanCode, modifiers);
      }
   }

   public void method_57734(float partialTick) {
   }
}
