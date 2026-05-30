package com.theplumteam.client.gui;

import com.theplumteam.BlockPopsMod;
import com.theplumteam.client.config.ClientConfig;
import com.theplumteam.client.config.ClientServerConfig;
import com.theplumteam.client.gui.util.ButtonFactory;
import com.theplumteam.client.gui.widget.TabButton;
import com.theplumteam.client.remote.RemoteAssetManager;
import com.theplumteam.figure.CollectionRegistry;
import com.theplumteam.figure.FigureCollection;
import com.theplumteam.network.ReloadTokensPacket;
import com.theplumteam.network.UnlockCollectionPacket;
import com.theplumteam.network.UpdateRemoteCollectionsPacket;
import com.theplumteam.network.UpdateTokenSettingsPacket;
import dev.architectury.platform.Platform;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_339;
import net.minecraft.class_342;
import net.minecraft.class_357;
import net.minecraft.class_4185;
import net.minecraft.class_437;

public class SettingsScreen extends class_437 {
   private final class_437 parent;
   private static final int PANEL_BG = -1342177280;
   private static final int PANEL_OUTLINE = 1627389951;
   private static final int TITLE_COLOR = 16777215;
   private static final int TAB_HEIGHT = 30;
   private static final int TAB_WIDTH = 100;
   private static final int TAB_SPACING = 2;
   private int panelWidth = 650;
   private int panelHeight = 350;
   private int panelX;
   private int panelY;
   private SettingsScreen.Tab activeTab = SettingsScreen.Tab.SERVER;
   private TabButton serverTabButton;
   private TabButton adminTabButton;
   private TabButton remoteTabButton;
   private TabButton developTabButton;
   private TabButton cheatsTabButton;
   private final List<class_339> serverSettingWidgets = new ArrayList<>();
   private final List<class_339> adminSettingWidgets = new ArrayList<>();
   private final List<class_339> remoteSettingWidgets = new ArrayList<>();
   private final List<class_339> developSettingWidgets = new ArrayList<>();
   private final List<class_339> cheatsSettingWidgets = new ArrayList<>();
   private final Set<String> pendingHiddenCollections = new HashSet<>();
   private final Set<String> pendingRemoteCollections = new HashSet<>();
   private final Map<String, String> remoteCollectionNames = new LinkedHashMap<>();
   private class_342 codeInputField;
   private String remoteStatusMessage = null;
   private boolean remoteUpdateChecked = false;
   private String remoteUpdateStatus = null;
   private class_4185 closeButton;
   private class_4185 actionButton;
   private class_4185 forceResyncButton;
   private class_4185 colorTransitionToggle;
   private SettingsScreen.HourSlider resetHourSlider;
   private int loadedServerHourLocal;
   private int pendingServerHourLocal;
   private class_342 regularCooldownBox;
   private class_342 maxRegularBox;
   private int loadedRegularCooldown;
   private int loadedMaxRegular;
   private SettingsScreen.ColorSlider starRedSlider;
   private SettingsScreen.ColorSlider starGreenSlider;
   private SettingsScreen.ColorSlider starBlueSlider;
   private SettingsScreen.OpacitySlider starOpacitySlider;
   private SettingsScreen.ColorSlider bgRedSlider;
   private SettingsScreen.ColorSlider bgGreenSlider;
   private SettingsScreen.ColorSlider bgBlueSlider;
   private SettingsScreen.OpacitySlider panelOpacitySlider;

   public SettingsScreen(class_437 parent) {
      super(class_2561.method_43470("Settings"));
      this.parent = parent;
   }

   protected void method_25426() {
      if (!BlockPopsMod.LOCAL_ADMIN) {
         this.method_25419();
      } else {
         super.method_25426();
         this.serverSettingWidgets.clear();
         this.adminSettingWidgets.clear();
         this.remoteSettingWidgets.clear();
         this.developSettingWidgets.clear();
         this.cheatsSettingWidgets.clear();
         this.panelX = (this.field_22789 - this.panelWidth) / 2;
         this.panelY = (this.field_22790 - this.panelHeight) / 2;
         int tabY = this.panelY;
         int tabStartX = this.panelX;
         this.serverTabButton = (TabButton)ButtonFactory.createTab(
            tabStartX,
            tabY,
            100,
            30,
            class_2561.method_43470(SettingsScreen.Tab.SERVER.getDisplayName()),
            this.activeTab == SettingsScreen.Tab.SERVER,
            btn -> this.switchTab(SettingsScreen.Tab.SERVER)
         );
         this.method_37063(this.serverTabButton);
         int nextTabX = tabStartX + 100 + 2;
         this.adminTabButton = (TabButton)ButtonFactory.createTab(
            nextTabX,
            tabY,
            100,
            30,
            class_2561.method_43470(SettingsScreen.Tab.ADMIN.getDisplayName()),
            this.activeTab == SettingsScreen.Tab.ADMIN,
            btn -> this.switchTab(SettingsScreen.Tab.ADMIN)
         );
         this.method_37063(this.adminTabButton);
         nextTabX += 102;
         if (this.isAdmin()) {
            this.remoteTabButton = (TabButton)ButtonFactory.createTab(
               nextTabX,
               tabY,
               100,
               30,
               class_2561.method_43470(SettingsScreen.Tab.REMOTE.getDisplayName()),
               this.activeTab == SettingsScreen.Tab.REMOTE,
               btn -> this.switchTab(SettingsScreen.Tab.REMOTE)
            );
            this.method_37063(this.remoteTabButton);
            nextTabX += 102;
         }

         if (isDevelopmentMode()) {
            this.developTabButton = (TabButton)ButtonFactory.createTab(
               nextTabX,
               tabY,
               100,
               30,
               class_2561.method_43470(SettingsScreen.Tab.DEVELOP.getDisplayName()),
               this.activeTab == SettingsScreen.Tab.DEVELOP,
               btn -> this.switchTab(SettingsScreen.Tab.DEVELOP)
            );
            this.method_37063(this.developTabButton);
            nextTabX += 102;
         }

         if (this.canAccessCheats()) {
            this.cheatsTabButton = (TabButton)ButtonFactory.createTab(
               nextTabX,
               tabY,
               100,
               30,
               class_2561.method_43470(SettingsScreen.Tab.CHEATS.getDisplayName()),
               this.activeTab == SettingsScreen.Tab.CHEATS,
               btn -> this.switchTab(SettingsScreen.Tab.CHEATS)
            );
            this.method_37063(this.cheatsTabButton);
         }

         int buttonWidth = 100;
         int buttonHeight = 20;
         int buttonY = this.panelY + this.panelHeight - buttonHeight - 20;
         int buttonSpacing = 10;
         int totalButtonWidth = buttonWidth * 2 + buttonSpacing;
         int buttonsStartX = this.panelX + (this.panelWidth - totalButtonWidth) / 2;
         this.closeButton = class_4185.method_46430(class_2561.method_43470("Close"), button -> this.method_25419())
            .method_46434(buttonsStartX, buttonY, buttonWidth, buttonHeight)
            .method_46431();
         this.method_37063(this.closeButton);
         this.actionButton = class_4185.method_46430(class_2561.method_43470("Action"), button -> this.handleActionClick())
            .method_46434(buttonsStartX + buttonWidth + buttonSpacing, buttonY, buttonWidth, buttonHeight)
            .method_46431();
         this.method_37063(this.actionButton);
         this.forceResyncButton = class_4185.method_46430(class_2561.method_43470("Force Resync"), button -> {
            button.method_25355(class_2561.method_43470("Syncing..."));
            button.field_22763 = false;
            RemoteAssetManager.init();
            RemoteAssetManager.invalidateManifest();
            Set<String> enabled = ClientServerConfig.getEnabledRemoteCollections();
            if (!enabled.isEmpty()) {
               RemoteAssetManager.syncEnabledCollections(new HashSet<>(enabled), () -> {
                  String error = RemoteAssetManager.getLastSyncError();
                  if (error != null) {
                     button.method_25355(class_2561.method_43470(error));
                     button.field_22763 = false;
                     this.scheduleButtonReset(button, 3000);
                  } else {
                     button.method_25355(class_2561.method_43470("Synced!"));
                     button.field_22763 = false;
                     this.scheduleButtonReset(button, 2000);
                  }
               });
            } else {
               button.method_25355(class_2561.method_43470("Force Resync"));
               button.field_22763 = true;
            }
         }).method_46434(buttonsStartX, buttonY - buttonHeight - 4, totalButtonWidth, buttonHeight).method_46431();
         this.forceResyncButton.field_22764 = false;
         this.method_37063(this.forceResyncButton);
         this.createServerSettings();
         this.createAdminSettings();
         if (this.isAdmin()) {
            this.createRemoteSettings();
         }

         if (isDevelopmentMode()) {
            this.createDevelopSettings();
         }

         if (this.canAccessCheats()) {
            this.createCheatsSettings();
         }

         this.switchTab(this.activeTab);
      }
   }

   private boolean isAdmin() {
      return BlockPopsMod.LOCAL_ADMIN;
   }

   private static int parseEditBoxInt(class_342 box, int defaultValue) {
      String text = box.method_1882().trim();
      if (text.isEmpty()) {
         return defaultValue;
      } else {
         try {
            return Integer.parseInt(text);
         } catch (NumberFormatException var4) {
            return defaultValue;
         }
      }
   }

   private boolean hasServerSettingsChanged() {
      if (!this.isAdmin()) {
         return false;
      } else if (this.regularCooldownBox != null && this.maxRegularBox != null) {
         int currentRegularCooldown = parseEditBoxInt(this.regularCooldownBox, this.loadedRegularCooldown);
         int currentMaxRegular = parseEditBoxInt(this.maxRegularBox, this.loadedMaxRegular);
         return currentRegularCooldown != this.loadedRegularCooldown
            || currentMaxRegular != this.loadedMaxRegular
            || this.pendingServerHourLocal != this.loadedServerHourLocal;
      } else {
         return false;
      }
   }

   private void scheduleButtonReset(class_4185 btn, int delayMs) {
      CompletableFuture.runAsync(() -> {
         try {
            Thread.sleep((long)delayMs);
         } catch (InterruptedException var3) {
         }

         class_310.method_1551().execute(() -> {
            btn.method_25355(class_2561.method_43470("Force Resync"));
            btn.field_22763 = true;
         });
      });
   }

   private void handleActionClick() {
      if (this.activeTab == SettingsScreen.Tab.SERVER) {
         int regularCooldown = Math.max(1, Math.min(168, parseEditBoxInt(this.regularCooldownBox, this.loadedRegularCooldown)));
         int maxRegular = Math.max(1, Math.min(99, parseEditBoxInt(this.maxRegularBox, this.loadedMaxRegular)));
         int utcValue = convertLocalToUtc(this.pendingServerHourLocal);
         new UpdateTokenSettingsPacket(regularCooldown, maxRegular, utcValue).sendToServer();
         this.loadedRegularCooldown = regularCooldown;
         this.loadedMaxRegular = maxRegular;
         this.loadedServerHourLocal = this.pendingServerHourLocal;
         this.regularCooldownBox.method_1852(String.valueOf(regularCooldown));
         this.maxRegularBox.method_1852(String.valueOf(maxRegular));
         ClientServerConfig.update(regularCooldown, maxRegular, utcValue);
         this.updateActionButtonState();
      } else if (this.activeTab == SettingsScreen.Tab.ADMIN) {
         ClientServerConfig.updateLocalHiddenCollections(new HashSet<>(this.pendingHiddenCollections));
         this.updateActionButtonState();
      } else if (this.activeTab == SettingsScreen.Tab.REMOTE) {
         new UpdateRemoteCollectionsPacket(new ArrayList<>(this.pendingRemoteCollections)).sendToServer();
         ClientServerConfig.updateEnabledRemoteCollections(new ArrayList<>(this.pendingRemoteCollections));
         this.updateActionButtonState();
      } else if (this.activeTab == SettingsScreen.Tab.DEVELOP) {
         ClientConfig.getInstance().resetColors();
         this.starRedSlider.setValue(1.0);
         this.starGreenSlider.setValue(1.0);
         this.starBlueSlider.setValue(1.0);
         this.starOpacitySlider.setValue(0.2);
         this.bgRedSlider.setValue(0.0);
         this.bgGreenSlider.setValue(0.0);
         this.bgBlueSlider.setValue(0.0);
         this.panelOpacitySlider.setValue(1.0);
         this.colorTransitionToggle.method_25355(class_2561.method_43470("Transition: ON"));
      }
   }

   private void updateActionButtonState() {
      if (this.activeTab == SettingsScreen.Tab.SERVER) {
         this.actionButton.method_25355(class_2561.method_43470("Save Settings"));
         this.actionButton.field_22764 = this.isAdmin();
         this.actionButton.field_22763 = this.isAdmin() && this.hasServerSettingsChanged();
      } else if (this.activeTab == SettingsScreen.Tab.ADMIN) {
         this.actionButton.method_25355(class_2561.method_43470("Save Visibility"));
         this.actionButton.field_22764 = true;
         this.actionButton.field_22763 = this.hasHiddenCollectionsChanged();
      } else if (this.activeTab == SettingsScreen.Tab.REMOTE) {
         this.actionButton.method_25355(class_2561.method_43470("Save Custom"));
         this.actionButton.field_22764 = true;
         this.actionButton.field_22763 = this.hasRemoteCollectionsChanged();
      } else if (this.activeTab == SettingsScreen.Tab.DEVELOP) {
         this.actionButton.method_25355(class_2561.method_43470("Reset Colors"));
         this.actionButton.field_22763 = true;
         this.actionButton.field_22764 = true;
      } else {
         this.actionButton.field_22764 = false;
      }

      this.forceResyncButton.field_22764 = this.activeTab == SettingsScreen.Tab.REMOTE && !this.pendingRemoteCollections.isEmpty();
   }

   public void method_25394(class_332 graphics, int mouseX, int mouseY, float partialTicks) {
      if (this.parent != null) {
         this.parent.method_25394(graphics, -1, -1, partialTicks);
      }

      graphics.method_51452();
      graphics.method_51448().method_22903();
      graphics.method_51448().method_46416(0.0F, 0.0F, 200.0F);
      graphics.method_25294(0, 0, this.field_22789, this.field_22790, 1879048192);
      int contentPanelY = this.panelY + 30;
      int contentPanelHeight = this.panelHeight - 30;
      ClientConfig config = ClientConfig.getInstance();
      int alpha = (int)(config.panelOpacity * 255.0F);
      int panelBgColor = alpha << 24 | 0;
      graphics.method_25294(this.panelX, contentPanelY, this.panelX + this.panelWidth, contentPanelY + contentPanelHeight, panelBgColor);
      graphics.method_25294(this.panelX, contentPanelY, this.panelX + this.panelWidth, contentPanelY + 1, 1627389951);
      graphics.method_25294(this.panelX, contentPanelY + contentPanelHeight - 1, this.panelX + this.panelWidth, contentPanelY + contentPanelHeight, 1627389951);
      graphics.method_25294(this.panelX, contentPanelY, this.panelX + 1, contentPanelY + contentPanelHeight, 1627389951);
      graphics.method_25294(this.panelX + this.panelWidth - 1, contentPanelY, this.panelX + this.panelWidth, contentPanelY + contentPanelHeight, 1627389951);
      if (isDevelopmentMode() && this.activeTab == SettingsScreen.Tab.DEVELOP) {
         int padding = 20;
         int columnSpacing = 15;
         int availableWidth = this.panelWidth - padding * 2 - columnSpacing * 2;
         int columnWidth = availableWidth / 3;
         int col1X = this.panelX + padding;
         int col2X = col1X + columnWidth + columnSpacing;
         int col3X = col2X + columnWidth + columnSpacing;
         int headerY = this.panelY + 30 + 5;
         graphics.method_25303(this.field_22793, "Star Color", col1X, headerY, 16777215);
         graphics.method_25303(this.field_22793, "Background Color", col2X, headerY, 16777215);
         graphics.method_25303(this.field_22793, "Panel & Animation", col3X, headerY, 16777215);
      }

      if (this.activeTab == SettingsScreen.Tab.SERVER) {
         int headerY = this.panelY + 30 + 10;
         graphics.method_25300(this.field_22793, "Token Settings", this.panelX + this.panelWidth / 2, headerY, 16777215);
         int contentWidth = 400;
         int contentX = this.panelX + (this.panelWidth - contentWidth) / 2;
         int fieldWidth = 60;
         int startY = this.panelY + 30 + 30;
         int verticalSpacing = 30;
         boolean admin = this.isAdmin();
         int labelY = startY + 6;
         graphics.method_25303(this.field_22793, "Regular Token Cooldown (hours):", contentX, labelY, 16777215);
         if (!admin) {
            String val = String.valueOf(ClientServerConfig.getRegularTokenCooldownHours());
            graphics.method_25303(this.field_22793, val, contentX + contentWidth - this.field_22793.method_1727(val), labelY, 11184810);
         }

         labelY += verticalSpacing;
         graphics.method_25303(this.field_22793, "Max Regular Tokens:", contentX, labelY, 16777215);
         if (!admin) {
            String val = String.valueOf(ClientServerConfig.getMaxRegularTokens());
            graphics.method_25303(this.field_22793, val, contentX + contentWidth - this.field_22793.method_1727(val), labelY, 11184810);
         }

         int explanationY = startY + verticalSpacing * 2 + 30;
         if (admin && this.resetHourSlider != null && this.resetHourSlider.field_22764) {
            explanationY += 30;
         }

         String[] explanationLines;
         if (admin) {
            explanationLines = new String[]{
               "Configure token generation and reset timing for all players.", "The guaranteed token resets daily at the specified hour."
            };
         } else {
            explanationLines = new String[]{
               "These settings are configured by the server administrator.",
               "Regular tokens regenerate every "
                  + ClientServerConfig.getRegularTokenCooldownHours()
                  + " hour(s), up to "
                  + ClientServerConfig.getMaxRegularTokens()
                  + " max.",
               "The guaranteed token resets daily."
            };
         }

         for (int i = 0; i < explanationLines.length; i++) {
            int lineWidth = this.field_22793.method_1727(explanationLines[i]);
            graphics.method_25303(this.field_22793, explanationLines[i], this.panelX + (this.panelWidth - lineWidth) / 2, explanationY + i * 12, 11184810);
         }
      }

      if (this.activeTab == SettingsScreen.Tab.ADMIN) {
         int headerYx = this.panelY + 30 + 10;
         graphics.method_25300(this.field_22793, "Collection Visibility", this.panelX + this.panelWidth / 2, headerYx, 16777215);
         int explanationYx = this.panelY + 30 + 30;
         String[] explanationLines = new String[]{
            "Toggle which collections are visible in the claw machine menu.", "Hidden collections will not appear for any player on this server."
         };

         for (int i = 0; i < explanationLines.length; i++) {
            int lineWidth = this.field_22793.method_1727(explanationLines[i]);
            graphics.method_25303(this.field_22793, explanationLines[i], this.panelX + (this.panelWidth - lineWidth) / 2, explanationYx + i * 12, 11184810);
         }
      }

      if (this.activeTab == SettingsScreen.Tab.REMOTE) {
         int headerYx = this.panelY + 30 + 10;
         class_2561 headerText;
         if ("update".equals(this.remoteUpdateStatus)) {
            headerText = class_2561.method_43470("Custom Collections  ")
               .method_10852(class_2561.method_43470("[Update Available]").method_27696(class_2583.field_24360.method_36139(5635925).method_10982(true)));
         } else if ("checking".equals(this.remoteUpdateStatus)) {
            headerText = class_2561.method_43470("Custom Collections  ")
               .method_10852(class_2561.method_43470("[Checking...]").method_27696(class_2583.field_24360.method_36139(16777045).method_10978(true)));
         } else {
            headerText = class_2561.method_43470("Custom Collections");
         }

         graphics.method_27534(this.field_22793, headerText, this.panelX + this.panelWidth / 2, headerYx, 16777215);
         int explanationYx = this.panelY + 30 + 30;
         String instruction = "Enter your collection code to load it.";
         int instrWidth = this.field_22793.method_1727(instruction);
         graphics.method_25303(this.field_22793, instruction, this.panelX + (this.panelWidth - instrWidth) / 2, explanationYx, 11184810);
         if (this.remoteStatusMessage != null) {
            int statusY = explanationYx + 14;
            String plainMsg = this.remoteStatusMessage.replaceAll("§.", "");
            int plainWidth = this.field_22793.method_1727(plainMsg);
            graphics.method_25303(this.field_22793, this.remoteStatusMessage, this.panelX + (this.panelWidth - plainWidth) / 2, statusY, 16777215);
         }

         if (!this.pendingRemoteCollections.isEmpty()) {
            int detailStartY = this.panelY + 30 + 90;
            int detailX = this.panelX + this.panelWidth / 2 + 140;
            int entryHeight = 28;

            for (String collectionId : new ArrayList<>(this.pendingRemoteCollections)) {
               FigureCollection loaded = CollectionRegistry.getCollection(collectionId).orElse(null);
               if (loaded != null) {
                  String info = loaded.getFigures().size() + " figures";
                  if (loaded.getAuthor() != null && !loaded.getAuthor().equals("Unknown")) {
                     info = info + " by " + loaded.getAuthor();
                  }

                  graphics.method_25303(this.field_22793, info, detailX, detailStartY + 3, 8978312);
                  int models = RemoteAssetManager.countManifestModelsForCollection(collectionId);
                  int textures = RemoteAssetManager.countManifestTexturesForCollection(collectionId);
                  if (models > 0 || textures > 0) {
                     String assets = models + " models, " + textures + " textures";
                     graphics.method_25303(this.field_22793, assets, detailX, detailStartY + 14, 8947848);
                  }
               } else {
                  graphics.method_25303(this.field_22793, "Not synced yet", detailX, detailStartY + 3, 11184725);
               }

               detailStartY += entryHeight;
            }
         }

         int syncInfoY = this.panelY + this.panelHeight - 75;
         long lastSync = RemoteAssetManager.getLastSyncTimestamp();
         if (lastSync > 0L) {
            long elapsed = System.currentTimeMillis() - lastSync;
            String timeAgo;
            if (elapsed < 60000L) {
               timeAgo = "just now";
            } else if (elapsed < 3600000L) {
               timeAgo = elapsed / 60000L + "m ago";
            } else if (elapsed < 86400000L) {
               timeAgo = elapsed / 3600000L + "h ago";
            } else {
               timeAgo = elapsed / 86400000L + "d ago";
            }

            String syncInfo = "Last sync: " + timeAgo;
            int totalFiles = RemoteAssetManager.getLastSyncTotalFiles();
            if (totalFiles > 0) {
               int downloaded = RemoteAssetManager.getLastSyncDownloaded();
               int cached = RemoteAssetManager.getLastSyncCached();
               int failed = RemoteAssetManager.getLastSyncFailed();
               syncInfo = syncInfo + " | " + totalFiles + " files (" + downloaded + " new, " + cached + " cached";
               if (failed > 0) {
                  syncInfo = syncInfo + ", " + failed + " failed";
               }

               syncInfo = syncInfo + ")";
            }

            int syncInfoWidth = this.field_22793.method_1727(syncInfo);
            graphics.method_25303(this.field_22793, syncInfo, this.panelX + (this.panelWidth - syncInfoWidth) / 2, syncInfoY, 8947848);
         } else if (RemoteAssetManager.isSyncing()) {
            String syncingText = "Syncing...";
            int syncingWidth = this.field_22793.method_1727(syncingText);
            graphics.method_25303(this.field_22793, syncingText, this.panelX + (this.panelWidth - syncingWidth) / 2, syncInfoY, 16777045);
         }

         int manifestVersion = RemoteAssetManager.getCachedManifestVersion();
         if (manifestVersion >= 0) {
            String versionText = "Manifest v" + manifestVersion;
            int remoteVersion = RemoteAssetManager.getRemoteManifestVersion();
            if (remoteVersion >= 0 && remoteVersion != manifestVersion) {
               versionText = versionText + " (remote: v" + remoteVersion + ")";
            }

            int versionWidth = this.field_22793.method_1727(versionText);
            graphics.method_25303(this.field_22793, versionText, this.panelX + (this.panelWidth - versionWidth) / 2, syncInfoY + 12, 6710886);
         }
      }

      if (this.activeTab == SettingsScreen.Tab.CHEATS) {
         int headerYxx = this.panelY + 30 + 10;
         graphics.method_25300(this.field_22793, "Collection Cheats", this.panelX + this.panelWidth / 2, headerYxx, 16777215);
         int explanationYxx = this.panelY + 30 + 30;
         String[] explanationLines = new String[]{
            "Use the token reload buttons to restore your tokens.", "Click a collection button to unlock all figures and receive all boxes."
         };

         for (int i = 0; i < explanationLines.length; i++) {
            int lineWidth = this.field_22793.method_1727(explanationLines[i]);
            graphics.method_25303(this.field_22793, explanationLines[i], this.panelX + (this.panelWidth - lineWidth) / 2, explanationYxx + i * 12, 11184810);
         }
      }

      if (isDevelopmentMode() && this.activeTab == SettingsScreen.Tab.DEVELOP) {
         int previewSize = 35;
         int previewSpacing = 50;
         int previewStartX = this.panelX + (this.panelWidth - (previewSize * 2 + previewSpacing)) / 2;
         int previewY = this.panelY + 210;
         int starRed = (int)(config.starColorR * 255.0F);
         int starGreen = (int)(config.starColorG * 255.0F);
         int starBlue = (int)(config.starColorB * 255.0F);
         int starColor = 0xFF000000 | starRed << 16 | starGreen << 8 | starBlue;
         graphics.method_25294(previewStartX - 1, previewY - 1, previewStartX + previewSize + 1, previewY + previewSize + 1, -1);
         graphics.method_25294(previewStartX, previewY, previewStartX + previewSize, previewY + previewSize, starColor);
         graphics.method_25300(this.field_22793, "Stars", previewStartX + previewSize / 2, previewY + previewSize + 5, 11184810);
         int bgPreviewX = previewStartX + previewSize + previewSpacing;
         int bgRed = (int)(config.backgroundColorR * 255.0F);
         int bgGreen = (int)(config.backgroundColorG * 255.0F);
         int bgBlue = (int)(config.backgroundColorB * 255.0F);
         int bgColor = 0xFF000000 | bgRed << 16 | bgGreen << 8 | bgBlue;
         graphics.method_25294(bgPreviewX - 1, previewY - 1, bgPreviewX + previewSize + 1, previewY + previewSize + 1, -1);
         graphics.method_25294(bgPreviewX, previewY, bgPreviewX + previewSize, previewY + previewSize, bgColor);
         graphics.method_25300(this.field_22793, "Background", bgPreviewX + previewSize / 2, previewY + previewSize + 5, 11184810);
      }

      super.method_25394(graphics, mouseX, mouseY, partialTicks);
      graphics.method_51448().method_22909();
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      if (!(mouseX < (double)this.panelX)
         && !(mouseX > (double)(this.panelX + this.panelWidth))
         && !(mouseY < (double)this.panelY)
         && !(mouseY > (double)(this.panelY + this.panelHeight))) {
         return super.method_25402(mouseX, mouseY, button);
      } else {
         this.method_25419();
         return true;
      }
   }

   public void method_25419() {
      this.field_22787.method_1507(this.parent);
   }

   public boolean method_25422() {
      return true;
   }

   public void method_57734(float partialTick) {
   }

   public boolean method_25421() {
      return false;
   }

   private static boolean isDevelopmentMode() {
      return Platform.isDevelopmentEnvironment();
   }

   private boolean canAccessCheats() {
      return BlockPopsMod.LOCAL_ADMIN;
   }

   private void createServerSettings() {
      boolean admin = this.isAdmin();
      int fieldHeight = 20;
      int fieldWidth = 60;
      int labelFieldSpacing = 8;
      int verticalSpacing = 30;
      int contentWidth = 400;
      int contentX = this.panelX + (this.panelWidth - contentWidth) / 2;
      int startY = this.panelY + 30 + 30;
      this.loadedRegularCooldown = ClientServerConfig.getRegularTokenCooldownHours();
      this.loadedMaxRegular = ClientServerConfig.getMaxRegularTokens();
      ZoneId localZone = ZoneId.systemDefault();
      String timezoneName = localZone.getDisplayName(TextStyle.SHORT, Locale.getDefault());
      int utcHour = ClientServerConfig.getGuaranteedTokenResetHour();
      int localHour = convertUtcToLocal(utcHour);
      this.loadedServerHourLocal = localHour;
      this.pendingServerHourLocal = localHour;
      int fieldX = contentX + contentWidth - fieldWidth;
      if (admin) {
         this.regularCooldownBox = new class_342(this.field_22793, fieldX, startY, fieldWidth, fieldHeight, class_2561.method_43470("Regular Cooldown"));
         this.regularCooldownBox.method_1852(String.valueOf(this.loadedRegularCooldown));
         this.regularCooldownBox.method_1890(s -> s.matches("\\d*"));
         this.regularCooldownBox.method_1880(3);
         this.regularCooldownBox.method_1863(s -> this.updateActionButtonState());
         this.serverSettingWidgets.add(this.regularCooldownBox);
      }

      int currentY = startY + verticalSpacing;
      if (admin) {
         this.maxRegularBox = new class_342(this.field_22793, fieldX, currentY, fieldWidth, fieldHeight, class_2561.method_43470("Max Regular"));
         this.maxRegularBox.method_1852(String.valueOf(this.loadedMaxRegular));
         this.maxRegularBox.method_1890(s -> s.matches("\\d*"));
         this.maxRegularBox.method_1880(2);
         this.maxRegularBox.method_1863(s -> this.updateActionButtonState());
         this.serverSettingWidgets.add(this.maxRegularBox);
      }

      currentY += verticalSpacing;
      this.resetHourSlider = new SettingsScreen.HourSlider(
         contentX,
         currentY,
         contentWidth,
         fieldHeight,
         class_2561.method_43470("Guaranteed Token Reset Hour (" + timezoneName + "): "),
         localHour,
         localValue -> {
            this.pendingServerHourLocal = localValue;
            this.updateActionButtonState();
         }
      );
      if (admin) {
         this.serverSettingWidgets.add(this.resetHourSlider);
      }
   }

   private static int convertUtcToLocal(int utcHour) {
      ZonedDateTime utcTime = ZonedDateTime.now(ZoneId.of("UTC")).withHour(utcHour).withMinute(0).withSecond(0).withNano(0);
      ZonedDateTime localTime = utcTime.withZoneSameInstant(ZoneId.systemDefault());
      return localTime.getHour();
   }

   private static int convertLocalToUtc(int localHour) {
      ZonedDateTime localTime = ZonedDateTime.now(ZoneId.systemDefault()).withHour(localHour).withMinute(0).withSecond(0).withNano(0);
      ZonedDateTime utcTime = localTime.withZoneSameInstant(ZoneId.of("UTC"));
      return utcTime.getHour();
   }

   private void createDevelopSettings() {
      ClientConfig config = ClientConfig.getInstance();
      config.backgroundColorR = 0.0F;
      config.backgroundColorG = 0.0F;
      config.backgroundColorB = 0.0F;
      int padding = 20;
      int columnSpacing = 15;
      int sliderHeight = 20;
      int verticalSpacing = 28;
      int availableWidth = this.panelWidth - padding * 2 - columnSpacing * 2;
      int columnWidth = availableWidth / 3;
      int col1X = this.panelX + padding;
      int col2X = col1X + columnWidth + columnSpacing;
      int col3X = col2X + columnWidth + columnSpacing;
      int startY = this.panelY + (isDevelopmentMode() ? 50 : 50);
      this.starRedSlider = new SettingsScreen.ColorSlider(
         col1X, startY, columnWidth, sliderHeight, class_2561.method_43470("Red: "), (double)config.starColorR, value -> config.starColorR = value.floatValue()
      );
      this.developSettingWidgets.add(this.starRedSlider);
      int col1Y = startY + verticalSpacing;
      this.starGreenSlider = new SettingsScreen.ColorSlider(
         col1X,
         col1Y,
         columnWidth,
         sliderHeight,
         class_2561.method_43470("Green: "),
         (double)config.starColorG,
         value -> config.starColorG = value.floatValue()
      );
      this.developSettingWidgets.add(this.starGreenSlider);
      col1Y += verticalSpacing;
      this.starBlueSlider = new SettingsScreen.ColorSlider(
         col1X, col1Y, columnWidth, sliderHeight, class_2561.method_43470("Blue: "), (double)config.starColorB, value -> config.starColorB = value.floatValue()
      );
      this.developSettingWidgets.add(this.starBlueSlider);
      col1Y += verticalSpacing;
      this.starOpacitySlider = new SettingsScreen.OpacitySlider(
         col1X,
         col1Y,
         columnWidth,
         sliderHeight,
         class_2561.method_43470("Opacity: "),
         (double)config.starOpacity,
         value -> config.starOpacity = value.floatValue()
      );
      this.developSettingWidgets.add(this.starOpacitySlider);
      this.bgRedSlider = new SettingsScreen.ColorSlider(
         col2X,
         startY,
         columnWidth,
         sliderHeight,
         class_2561.method_43470("Red: "),
         (double)config.backgroundColorR,
         value -> config.backgroundColorR = value.floatValue()
      );
      this.developSettingWidgets.add(this.bgRedSlider);
      int col2Y = startY + verticalSpacing;
      this.bgGreenSlider = new SettingsScreen.ColorSlider(
         col2X,
         col2Y,
         columnWidth,
         sliderHeight,
         class_2561.method_43470("Green: "),
         (double)config.backgroundColorG,
         value -> config.backgroundColorG = value.floatValue()
      );
      this.developSettingWidgets.add(this.bgGreenSlider);
      col2Y += verticalSpacing;
      this.bgBlueSlider = new SettingsScreen.ColorSlider(
         col2X,
         col2Y,
         columnWidth,
         sliderHeight,
         class_2561.method_43470("Blue: "),
         (double)config.backgroundColorB,
         value -> config.backgroundColorB = value.floatValue()
      );
      this.developSettingWidgets.add(this.bgBlueSlider);
      this.panelOpacitySlider = new SettingsScreen.OpacitySlider(
         col3X,
         startY,
         columnWidth,
         sliderHeight,
         class_2561.method_43470("Panel Opacity: "),
         (double)config.panelOpacity,
         value -> config.panelOpacity = value.floatValue()
      );
      this.developSettingWidgets.add(this.panelOpacitySlider);
      int col3Y = startY + verticalSpacing;
      this.colorTransitionToggle = class_4185.method_46430(class_2561.method_43470("Transition: " + (config.enableColorTransition ? "ON" : "OFF")), button -> {
         config.enableColorTransition = !config.enableColorTransition;
         button.method_25355(class_2561.method_43470("Transition: " + (config.enableColorTransition ? "ON" : "OFF")));
      }).method_46434(col3X, col3Y, columnWidth, sliderHeight).method_46431();
      this.developSettingWidgets.add(this.colorTransitionToggle);
   }

   private boolean hasHiddenCollectionsChanged() {
      Set<String> serverHidden = ClientServerConfig.getLocalHiddenCollections();
      return !this.pendingHiddenCollections.equals(serverHidden);
   }

   private void createAdminSettings() {
      this.pendingHiddenCollections.clear();
      this.pendingHiddenCollections.addAll(ClientServerConfig.getLocalHiddenCollections());
      int padding = 20;
      int buttonWidth = 180;
      int buttonHeight = 24;
      int verticalSpacing = 30;
      int horizontalSpacing = 15;
      int buttonsPerRow = 3;
      int startY = this.panelY + 30 + 60;
      int startX = this.panelX + padding;
      Collection<FigureCollection> allCollections = CollectionRegistry.getAllCollections();
      List<FigureCollection> filteredCollections = allCollections.stream()
         .filter(collectionx -> !collectionx.getId().equals("default"))
         .collect(Collectors.toList());
      int row = 0;
      int col = 0;

      for (FigureCollection collection : filteredCollections) {
         int buttonX = startX + col * (buttonWidth + horizontalSpacing);
         int buttonY = startY + row * verticalSpacing;
         boolean isHidden = this.pendingHiddenCollections.contains(collection.getId());
         String label = (isHidden ? "§c✖ " : "§a✔ ") + collection.getName();
         class_4185 toggleButton = class_4185.method_46430(class_2561.method_43470(label), button -> {
            String id = collection.getId();
            if (this.pendingHiddenCollections.contains(id)) {
               this.pendingHiddenCollections.remove(id);
               button.method_25355(class_2561.method_43470("§a✔ " + collection.getName()));
            } else {
               this.pendingHiddenCollections.add(id);
               button.method_25355(class_2561.method_43470("§c✖ " + collection.getName()));
            }

            this.updateActionButtonState();
         }).method_46434(buttonX, buttonY, buttonWidth, buttonHeight).method_46431();
         this.adminSettingWidgets.add(toggleButton);
         if (++col >= buttonsPerRow) {
            col = 0;
            row++;
         }
      }
   }

   private boolean hasRemoteCollectionsChanged() {
      Set<String> serverEnabled = ClientServerConfig.getEnabledRemoteCollections();
      return !this.pendingRemoteCollections.equals(serverEnabled);
   }

   private void createRemoteSettings() {
      this.pendingRemoteCollections.clear();
      this.pendingRemoteCollections.addAll(ClientServerConfig.getEnabledRemoteCollections());

      for (String id : this.pendingRemoteCollections) {
         if (!this.remoteCollectionNames.containsKey(id)) {
            this.remoteCollectionNames.put(id, id.substring(0, 1).toUpperCase() + id.substring(1));
         }
      }

      this.rebuildRemoteWidgets();
   }

   private void rebuildRemoteWidgets() {
      for (class_339 widget : this.remoteSettingWidgets) {
         this.method_37066(widget);
      }

      this.remoteSettingWidgets.clear();
      int padding = 20;
      int startY = this.panelY + 30 + 55;
      int contentWidth = this.panelWidth - padding * 2;
      int inputWidth = 180;
      int addBtnWidth = 60;
      int inputSpacing = 5;
      int totalInputWidth = inputWidth + inputSpacing + addBtnWidth;
      int inputX = this.panelX + (this.panelWidth - totalInputWidth) / 2;
      this.codeInputField = new class_342(this.field_22793, inputX, startY, inputWidth, 20, class_2561.method_43470("Collection Code"));
      this.codeInputField.method_47404(class_2561.method_43470("Enter code...").method_27696(class_2583.field_24360.method_36139(8421504)));
      this.codeInputField.method_1880(64);
      this.codeInputField.method_1863(text -> this.remoteStatusMessage = null);
      this.remoteSettingWidgets.add(this.codeInputField);
      class_4185 addButton = class_4185.method_46430(class_2561.method_43470("Add"), button -> {
         String code = this.codeInputField.method_1882().trim();
         if (!code.isEmpty()) {
            button.field_22763 = false;
            button.method_25355(class_2561.method_43470("..."));
            this.remoteStatusMessage = "§eLoading...";
            RemoteAssetManager.init();
            RemoteAssetManager.fetchCollectionByCode(code).thenAccept(result -> class_310.method_1551().execute(() -> {
                  if (result == null) {
                     this.remoteStatusMessage = "§cInvalid code. Collection not found.";
                  } else if (result.isError()) {
                     this.remoteStatusMessage = "§c" + result.error();
                  } else if (this.pendingRemoteCollections.contains(result.id())) {
                     this.remoteStatusMessage = "§e" + result.name() + " is already added.";
                  } else {
                     this.pendingRemoteCollections.add(result.id());
                     this.remoteCollectionNames.put(result.id(), result.name());
                     this.remoteStatusMessage = "§aAdded: " + result.name();
                     this.codeInputField.method_1852("");
                     this.rebuildRemoteWidgets();
                     if (this.activeTab == SettingsScreen.Tab.REMOTE) {
                        this.switchTab(SettingsScreen.Tab.REMOTE);
                     }
                  }

                  button.field_22763 = true;
                  button.method_25355(class_2561.method_43470("Add"));
                  this.updateActionButtonState();
               }));
         }
      }).method_46434(inputX + inputWidth + inputSpacing, startY, addBtnWidth, 20).method_46431();
      this.remoteSettingWidgets.add(addButton);
      startY += 35;
      if (!this.pendingRemoteCollections.isEmpty()) {
         int buttonHeight = 24;
         int removeWidth = 24;
         int entrySpacing = 4;
         int entryWidth = 250;
         int entryX = this.panelX + (this.panelWidth - entryWidth) / 2;

         for (String collectionId : new ArrayList<>(this.pendingRemoteCollections)) {
            String displayName = this.remoteCollectionNames.getOrDefault(collectionId, collectionId.substring(0, 1).toUpperCase() + collectionId.substring(1));
            class_4185 nameButton = class_4185.method_46430(class_2561.method_43470("§a✔ " + displayName), button -> {
            }).method_46434(entryX, startY, entryWidth - removeWidth - entrySpacing, buttonHeight).method_46431();
            nameButton.field_22763 = false;
            this.remoteSettingWidgets.add(nameButton);
            class_4185 removeButton = class_4185.method_46430(class_2561.method_43470("§cX"), button -> {
               this.pendingRemoteCollections.remove(collectionId);
               this.remoteCollectionNames.remove(collectionId);
               this.remoteStatusMessage = null;
               this.rebuildRemoteWidgets();
               if (this.activeTab == SettingsScreen.Tab.REMOTE) {
                  this.switchTab(SettingsScreen.Tab.REMOTE);
               }

               this.updateActionButtonState();
            }).method_46434(entryX + entryWidth - removeWidth, startY, removeWidth, buttonHeight).method_46431();
            this.remoteSettingWidgets.add(removeButton);
            startY += buttonHeight + entrySpacing;
         }
      }

      if (this.activeTab == SettingsScreen.Tab.REMOTE) {
         for (class_339 widget : this.remoteSettingWidgets) {
            this.method_37063(widget);
         }
      }
   }

   private void createCheatsSettings() {
      int padding = 20;
      int buttonWidth = 180;
      int buttonHeight = 24;
      int verticalSpacing = 30;
      int horizontalSpacing = 15;
      int buttonsPerRow = 3;
      int startY = this.panelY + 30 + 50;
      int startX = this.panelX + padding;
      int tokenButtonWidth = 200;
      int tokenButtonSpacing = 15;
      int tokenButtonsStartX = this.panelX + (this.panelWidth - (tokenButtonWidth * 2 + tokenButtonSpacing)) / 2;
      class_4185 reloadRegularButton = class_4185.method_46430(class_2561.method_43470("Reload Regular Tokens"), button -> {
         ReloadTokensPacket packet = new ReloadTokensPacket(true, false);
         packet.sendToServer();
         button.method_25355(class_2561.method_43470("Reloading..."));
         button.field_22763 = false;
         new Thread(() -> {
            try {
               Thread.sleep(500L);
               this.field_22787.execute(() -> {
                  button.method_25355(class_2561.method_43470("Reload Regular Tokens"));
                  button.field_22763 = true;
               });
            } catch (InterruptedException var3x) {
               var3x.printStackTrace();
            }
         }).start();
      }).method_46434(tokenButtonsStartX, startY, tokenButtonWidth, buttonHeight).method_46431();
      this.cheatsSettingWidgets.add(reloadRegularButton);
      class_4185 reloadGuaranteedButton = class_4185.method_46430(class_2561.method_43470("Reload Guaranteed Token"), button -> {
         ReloadTokensPacket packet = new ReloadTokensPacket(false, true);
         packet.sendToServer();
         button.method_25355(class_2561.method_43470("Reloading..."));
         button.field_22763 = false;
         new Thread(() -> {
            try {
               Thread.sleep(500L);
               this.field_22787.execute(() -> {
                  button.method_25355(class_2561.method_43470("Reload Guaranteed Token"));
                  button.field_22763 = true;
               });
            } catch (InterruptedException var3x) {
               var3x.printStackTrace();
            }
         }).start();
      }).method_46434(tokenButtonsStartX + tokenButtonWidth + tokenButtonSpacing, startY, tokenButtonWidth, buttonHeight).method_46431();
      this.cheatsSettingWidgets.add(reloadGuaranteedButton);
      startY += verticalSpacing + 20;
      Collection<FigureCollection> collections = CollectionRegistry.getAllCollections();
      List<FigureCollection> filteredCollections = collections.stream()
         .filter(collectionx -> !collectionx.getId().equals("default"))
         .collect(Collectors.toList());
      int row = 0;
      int col = 0;

      for (FigureCollection collection : filteredCollections) {
         int buttonX = startX + col * (buttonWidth + horizontalSpacing);
         int buttonY = startY + row * verticalSpacing;
         class_4185 unlockButton = class_4185.method_46430(class_2561.method_43470("Unlock " + collection.getName()), button -> {
            UnlockCollectionPacket packet = new UnlockCollectionPacket(collection.getId());
            packet.sendToServer();
            button.method_25355(class_2561.method_43470("Unlocking..."));
            button.field_22763 = false;
            new Thread(() -> {
               try {
                  Thread.sleep(1000L);
                  this.field_22787.execute(() -> {
                     button.method_25355(class_2561.method_43470("Unlock " + collection.getName()));
                     button.field_22763 = true;
                  });
               } catch (InterruptedException var4x) {
                  var4x.printStackTrace();
               }
            }).start();
         }).method_46434(buttonX, buttonY, buttonWidth, buttonHeight).method_46431();
         this.cheatsSettingWidgets.add(unlockButton);
         if (++col >= buttonsPerRow) {
            col = 0;
            row++;
         }
      }
   }

   private void switchTab(SettingsScreen.Tab tab) {
      this.activeTab = tab;

      for (class_339 widget : this.serverSettingWidgets) {
         this.method_37066(widget);
      }

      for (class_339 widget : this.adminSettingWidgets) {
         this.method_37066(widget);
      }

      for (class_339 widget : this.remoteSettingWidgets) {
         this.method_37066(widget);
      }

      for (class_339 widget : this.developSettingWidgets) {
         this.method_37066(widget);
      }

      for (class_339 widget : this.cheatsSettingWidgets) {
         this.method_37066(widget);
      }

      if (tab == SettingsScreen.Tab.REMOTE && !this.remoteUpdateChecked && !this.pendingRemoteCollections.isEmpty()) {
         this.remoteUpdateChecked = true;
         this.remoteUpdateStatus = "checking";
         RemoteAssetManager.init();
         RemoteAssetManager.checkForUpdates(result -> {
            if (result == null) {
               this.remoteUpdateStatus = null;
            } else if (result) {
               this.remoteUpdateStatus = "update";
            } else {
               this.remoteUpdateStatus = "current";
            }
         });
      }

      List<class_339> activeWidgets;
      if (tab == SettingsScreen.Tab.SERVER) {
         activeWidgets = this.serverSettingWidgets;
      } else if (tab == SettingsScreen.Tab.ADMIN) {
         activeWidgets = this.adminSettingWidgets;
      } else if (tab == SettingsScreen.Tab.REMOTE) {
         activeWidgets = this.remoteSettingWidgets;
      } else if (tab == SettingsScreen.Tab.DEVELOP) {
         activeWidgets = this.developSettingWidgets;
      } else if (tab == SettingsScreen.Tab.CHEATS) {
         activeWidgets = this.cheatsSettingWidgets;
      } else {
         activeWidgets = new ArrayList<>();
      }

      for (class_339 widget : activeWidgets) {
         this.method_37063(widget);
      }

      this.serverTabButton.setSelected(tab == SettingsScreen.Tab.SERVER);
      if (this.adminTabButton != null) {
         this.adminTabButton.setSelected(tab == SettingsScreen.Tab.ADMIN);
      }

      if (this.remoteTabButton != null) {
         this.remoteTabButton.setSelected(tab == SettingsScreen.Tab.REMOTE);
      }

      if (this.developTabButton != null) {
         this.developTabButton.setSelected(tab == SettingsScreen.Tab.DEVELOP);
      }

      if (this.cheatsTabButton != null) {
         this.cheatsTabButton.setSelected(tab == SettingsScreen.Tab.CHEATS);
      }

      this.updateActionButtonState();
   }

   private static class ColorSlider extends class_357 {
      private final class_2561 prefix;
      private final Consumer<Double> onValueChange;

      public ColorSlider(int x, int y, int width, int height, class_2561 prefix, double initialValue, Consumer<Double> onValueChange) {
         super(x, y, width, height, class_2561.method_43473(), initialValue);
         this.prefix = prefix;
         this.onValueChange = onValueChange;
         this.method_25346();
      }

      protected void method_25346() {
         int intValue = (int)(this.field_22753 * 255.0);
         this.method_25355(class_2561.method_43470(this.prefix.getString() + intValue));
      }

      protected void method_25344() {
         this.onValueChange.accept(this.field_22753);
      }

      public void setValue(double newValue) {
         this.field_22753 = newValue;
         this.method_25346();
      }
   }

   private static class HourSlider extends class_357 {
      private final class_2561 prefix;
      private final Consumer<Integer> onValueChange;

      public HourSlider(int x, int y, int width, int height, class_2561 prefix, int initialValue, Consumer<Integer> onValueChange) {
         super(x, y, width, height, class_2561.method_43473(), (double)initialValue / 23.0);
         this.prefix = prefix;
         this.onValueChange = onValueChange;
         this.method_25346();
      }

      protected void method_25346() {
         int hour = (int)(this.field_22753 * 23.0);
         this.method_25355(class_2561.method_43470(this.prefix.getString() + String.format("%02d:00", hour)));
      }

      protected void method_25344() {
         int hour = (int)(this.field_22753 * 23.0);
         this.onValueChange.accept(hour);
      }

      public void setValue(int newValue) {
         this.field_22753 = (double)newValue / 23.0;
         this.method_25346();
      }
   }

   private static class OpacitySlider extends class_357 {
      private final class_2561 prefix;
      private final Consumer<Double> onValueChange;

      public OpacitySlider(int x, int y, int width, int height, class_2561 prefix, double initialValue, Consumer<Double> onValueChange) {
         super(x, y, width, height, class_2561.method_43473(), initialValue);
         this.prefix = prefix;
         this.onValueChange = onValueChange;
         this.method_25346();
      }

      protected void method_25346() {
         int percentage = (int)(this.field_22753 * 100.0);
         this.method_25355(class_2561.method_43470(this.prefix.getString() + percentage + "%"));
      }

      protected void method_25344() {
         this.onValueChange.accept(this.field_22753);
      }

      public void setValue(double newValue) {
         this.field_22753 = newValue;
         this.method_25346();
      }
   }

   private static enum Tab {
      SERVER("Server"),
      ADMIN("Hide"),
      REMOTE("Custom"),
      DEVELOP("Develop"),
      CHEATS("Cheats");

      private final String displayName;

      private Tab(String displayName) {
         this.displayName = displayName;
      }

      public String getDisplayName() {
         return this.displayName;
      }
   }
}
