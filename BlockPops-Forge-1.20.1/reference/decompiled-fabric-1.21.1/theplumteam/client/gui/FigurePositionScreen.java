package com.theplumteam.client.gui;

import com.mojang.authlib.GameProfile;
import com.theplumteam.blockentity.BoxBlockEntity;
import com.theplumteam.blockentity.FigureBlockEntity;
import com.theplumteam.figure.FigureDefinition;
import com.theplumteam.figure.FigureType;
import com.theplumteam.network.FigurePositionPacket;
import com.theplumteam.util.SkinModelDetector;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.class_2338;
import net.minecraft.class_2561;
import net.minecraft.class_2586;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_357;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_640;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FigurePositionScreen extends class_437 {
   private static final Logger LOGGER = LoggerFactory.getLogger(FigurePositionScreen.class);
   private static final Map<UUID, GameProfile> profileCache = new ConcurrentHashMap<>();
   private static final Set<UUID> registeredSkins = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private static boolean checkedQuickSkin = false;
   private static boolean quickSkinAvailable = false;
   private static Method getSkinLocationMethod;
   private static Object playerAppearanceServiceInstance;
   private final class_2338 blockPos;
   private double offsetX;
   private double offsetY;
   private double offsetZ;
   private double scale;
   private double hitboxOffsetX;
   private double hitboxOffsetY;
   private double hitboxOffsetZ;
   private double hitboxScaleX;
   private double hitboxScaleY;
   private double hitboxScaleZ;
   private Double logoPositionX;
   private Double logoPositionY;
   private Double logoPositionZ;
   private Double logoScaleX;
   private Double logoScaleY;
   private Double logoScaleZ;

   private static class_2960 getQuickSkinLocation(UUID uuid) {
      if (!checkedQuickSkin) {
         try {
            Class<?> serviceClass = Class.forName("com.quickskin.mod.client.services.PlayerAppearanceService");
            Method getInstanceMethod = serviceClass.getMethod("getInstance");
            playerAppearanceServiceInstance = getInstanceMethod.invoke(null);
            getSkinLocationMethod = serviceClass.getMethod("getSkinLocation", UUID.class);
            quickSkinAvailable = true;
         } catch (Exception var3) {
            quickSkinAvailable = false;
         }

         checkedQuickSkin = true;
      }

      if (quickSkinAvailable && playerAppearanceServiceInstance != null) {
         try {
            return (class_2960)getSkinLocationMethod.invoke(playerAppearanceServiceInstance, uuid);
         } catch (Exception var4) {
         }
      }

      return null;
   }

   public FigurePositionScreen(
      class_2338 blockPos,
      double offsetX,
      double offsetY,
      double offsetZ,
      double scale,
      double hitboxOffsetX,
      double hitboxOffsetY,
      double hitboxOffsetZ,
      double hitboxScaleX,
      double hitboxScaleY,
      double hitboxScaleZ,
      Double logoPositionX,
      Double logoPositionY,
      Double logoPositionZ,
      Double logoScaleX,
      Double logoScaleY,
      Double logoScaleZ
   ) {
      super(class_2561.method_43470("Adjust Figure, Hitbox & Logo"));
      this.blockPos = blockPos;
      this.offsetX = offsetX;
      this.offsetY = offsetY;
      this.offsetZ = offsetZ;
      this.scale = scale;
      this.hitboxOffsetX = hitboxOffsetX;
      this.hitboxOffsetY = hitboxOffsetY;
      this.hitboxOffsetZ = hitboxOffsetZ;
      this.hitboxScaleX = hitboxScaleX;
      this.hitboxScaleY = hitboxScaleY;
      this.hitboxScaleZ = hitboxScaleZ;
      this.logoPositionX = logoPositionX;
      this.logoPositionY = logoPositionY;
      this.logoPositionZ = logoPositionZ;
      this.logoScaleX = logoScaleX;
      this.logoScaleY = logoScaleY;
      this.logoScaleZ = logoScaleZ;
   }

   private void addFineTuneButtons(int x, int y, Runnable decrementAction, Runnable incrementAction) {
      int buttonWidth = 18;
      this.method_37063(class_4185.method_46430(class_2561.method_43470("-"), button -> {
         decrementAction.run();
         this.method_41843();
      }).method_46434(x, y, buttonWidth, 20).method_46431());
      this.method_37063(class_4185.method_46430(class_2561.method_43470("+"), button -> {
         incrementAction.run();
         this.method_41843();
      }).method_46434(x + buttonWidth + 2, y, buttonWidth, 20).method_46431());
   }

   protected void method_25426() {
      super.method_25426();
      int centerX = this.field_22789 / 2;
      int startY = 50;
      int sliderWidth = 140;
      int columnSpacing = 200;
      int col1X = centerX - columnSpacing - 70;
      int col2X = centerX - 70;
      int col3X = centerX + columnSpacing - 70;
      this.method_37063(
         new class_357(col1X, startY, sliderWidth, 20, class_2561.method_43470("X Offset: " + String.format("%.2f", this.offsetX)), (this.offsetX + 1.0) / 2.0) {
            protected void method_25346() {
               FigurePositionScreen.this.offsetX = this.field_22753 * 2.0 - 1.0;
               this.method_25355(class_2561.method_43470("X Offset: " + String.format("%.2f", FigurePositionScreen.this.offsetX)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.offsetX = this.field_22753 * 2.0 - 1.0;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col1X + sliderWidth + 2, startY, () -> {
         this.offsetX = Math.max(-1.0, this.offsetX - 0.001);
         this.sendUpdate();
      }, () -> {
         this.offsetX = Math.min(1.0, this.offsetX + 0.001);
         this.sendUpdate();
      });
      this.method_37063(
         new class_357(
            col1X, startY + 25, sliderWidth, 20, class_2561.method_43470("Y Offset: " + String.format("%.2f", this.offsetY)), (this.offsetY + 1.0) / 2.0
         ) {
            protected void method_25346() {
               FigurePositionScreen.this.offsetY = this.field_22753 * 2.0 - 1.0;
               this.method_25355(class_2561.method_43470("Y Offset: " + String.format("%.2f", FigurePositionScreen.this.offsetY)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.offsetY = this.field_22753 * 2.0 - 1.0;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col1X + sliderWidth + 2, startY + 25, () -> {
         this.offsetY = Math.max(-1.0, this.offsetY - 0.001);
         this.sendUpdate();
      }, () -> {
         this.offsetY = Math.min(1.0, this.offsetY + 0.001);
         this.sendUpdate();
      });
      this.method_37063(
         new class_357(
            col1X, startY + 50, sliderWidth, 20, class_2561.method_43470("Z Offset: " + String.format("%.2f", this.offsetZ)), (this.offsetZ + 1.0) / 2.0
         ) {
            protected void method_25346() {
               FigurePositionScreen.this.offsetZ = this.field_22753 * 2.0 - 1.0;
               this.method_25355(class_2561.method_43470("Z Offset: " + String.format("%.2f", FigurePositionScreen.this.offsetZ)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.offsetZ = this.field_22753 * 2.0 - 1.0;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col1X + sliderWidth + 2, startY + 50, () -> {
         this.offsetZ = Math.max(-1.0, this.offsetZ - 0.001);
         this.sendUpdate();
      }, () -> {
         this.offsetZ = Math.min(1.0, this.offsetZ + 0.001);
         this.sendUpdate();
      });
      this.method_37063(
         new class_357(col1X, startY + 75, sliderWidth, 20, class_2561.method_43470("Scale: " + String.format("%.2f", this.scale)), (this.scale - 0.1) / 1.9) {
            protected void method_25346() {
               FigurePositionScreen.this.scale = 0.1 + this.field_22753 * 1.9;
               this.method_25355(class_2561.method_43470("Scale: " + String.format("%.2f", FigurePositionScreen.this.scale)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.scale = 0.1 + this.field_22753 * 1.9;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col1X + sliderWidth + 2, startY + 75, () -> {
         this.scale = Math.max(0.1, this.scale - 0.001);
         this.sendUpdate();
      }, () -> {
         this.scale = Math.min(2.0, this.scale + 0.001);
         this.sendUpdate();
      });
      this.method_37063(
         new class_357(
            col2X, startY, sliderWidth, 20, class_2561.method_43470("Hitbox X: " + String.format("%.2f", this.hitboxOffsetX)), (this.hitboxOffsetX + 1.0) / 2.0
         ) {
            protected void method_25346() {
               FigurePositionScreen.this.hitboxOffsetX = this.field_22753 * 2.0 - 1.0;
               this.method_25355(class_2561.method_43470("Hitbox X: " + String.format("%.2f", FigurePositionScreen.this.hitboxOffsetX)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.hitboxOffsetX = this.field_22753 * 2.0 - 1.0;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col2X + sliderWidth + 2, startY, () -> {
         this.hitboxOffsetX = Math.max(-1.0, this.hitboxOffsetX - 0.001);
         this.sendUpdate();
      }, () -> {
         this.hitboxOffsetX = Math.min(1.0, this.hitboxOffsetX + 0.001);
         this.sendUpdate();
      });
      this.method_37063(
         new class_357(
            col2X,
            startY + 25,
            sliderWidth,
            20,
            class_2561.method_43470("Hitbox Y: " + String.format("%.2f", this.hitboxOffsetY)),
            (this.hitboxOffsetY + 1.0) / 2.0
         ) {
            protected void method_25346() {
               FigurePositionScreen.this.hitboxOffsetY = this.field_22753 * 2.0 - 1.0;
               this.method_25355(class_2561.method_43470("Hitbox Y: " + String.format("%.2f", FigurePositionScreen.this.hitboxOffsetY)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.hitboxOffsetY = this.field_22753 * 2.0 - 1.0;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col2X + sliderWidth + 2, startY + 25, () -> {
         this.hitboxOffsetY = Math.max(-1.0, this.hitboxOffsetY - 0.001);
         this.sendUpdate();
      }, () -> {
         this.hitboxOffsetY = Math.min(1.0, this.hitboxOffsetY + 0.001);
         this.sendUpdate();
      });
      this.method_37063(
         new class_357(
            col2X,
            startY + 50,
            sliderWidth,
            20,
            class_2561.method_43470("Hitbox Z: " + String.format("%.2f", this.hitboxOffsetZ)),
            (this.hitboxOffsetZ + 1.0) / 2.0
         ) {
            protected void method_25346() {
               FigurePositionScreen.this.hitboxOffsetZ = this.field_22753 * 2.0 - 1.0;
               this.method_25355(class_2561.method_43470("Hitbox Z: " + String.format("%.2f", FigurePositionScreen.this.hitboxOffsetZ)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.hitboxOffsetZ = this.field_22753 * 2.0 - 1.0;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col2X + sliderWidth + 2, startY + 50, () -> {
         this.hitboxOffsetZ = Math.max(-1.0, this.hitboxOffsetZ - 0.001);
         this.sendUpdate();
      }, () -> {
         this.hitboxOffsetZ = Math.min(1.0, this.hitboxOffsetZ + 0.001);
         this.sendUpdate();
      });
      this.method_37063(
         new class_357(
            col2X,
            startY + 75,
            sliderWidth,
            20,
            class_2561.method_43470("Hitbox Scale X: " + String.format("%.2f", this.hitboxScaleX)),
            (this.hitboxScaleX - 0.5) / 1.5
         ) {
            protected void method_25346() {
               FigurePositionScreen.this.hitboxScaleX = 0.5 + this.field_22753 * 1.5;
               this.method_25355(class_2561.method_43470("Hitbox Scale X: " + String.format("%.2f", FigurePositionScreen.this.hitboxScaleX)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.hitboxScaleX = 0.5 + this.field_22753 * 1.5;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col2X + sliderWidth + 2, startY + 75, () -> {
         this.hitboxScaleX = Math.max(0.5, this.hitboxScaleX - 0.01);
         this.sendUpdate();
      }, () -> {
         this.hitboxScaleX = Math.min(2.0, this.hitboxScaleX + 0.01);
         this.sendUpdate();
      });
      this.method_37063(
         new class_357(
            col2X,
            startY + 100,
            sliderWidth,
            20,
            class_2561.method_43470("Hitbox Scale Y: " + String.format("%.2f", this.hitboxScaleY)),
            (this.hitboxScaleY - 0.5) / 1.5
         ) {
            protected void method_25346() {
               FigurePositionScreen.this.hitboxScaleY = 0.5 + this.field_22753 * 1.5;
               this.method_25355(class_2561.method_43470("Hitbox Scale Y: " + String.format("%.2f", FigurePositionScreen.this.hitboxScaleY)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.hitboxScaleY = 0.5 + this.field_22753 * 1.5;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col2X + sliderWidth + 2, startY + 100, () -> {
         this.hitboxScaleY = Math.max(0.5, this.hitboxScaleY - 0.01);
         this.sendUpdate();
      }, () -> {
         this.hitboxScaleY = Math.min(2.0, this.hitboxScaleY + 0.01);
         this.sendUpdate();
      });
      this.method_37063(
         new class_357(
            col2X,
            startY + 125,
            sliderWidth,
            20,
            class_2561.method_43470("Hitbox Scale Z: " + String.format("%.2f", this.hitboxScaleZ)),
            (this.hitboxScaleZ - 0.5) / 1.5
         ) {
            protected void method_25346() {
               FigurePositionScreen.this.hitboxScaleZ = 0.5 + this.field_22753 * 1.5;
               this.method_25355(class_2561.method_43470("Hitbox Scale Z: " + String.format("%.2f", FigurePositionScreen.this.hitboxScaleZ)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.hitboxScaleZ = 0.5 + this.field_22753 * 1.5;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col2X + sliderWidth + 2, startY + 125, () -> {
         this.hitboxScaleZ = Math.max(0.5, this.hitboxScaleZ - 0.01);
         this.sendUpdate();
      }, () -> {
         this.hitboxScaleZ = Math.min(2.0, this.hitboxScaleZ + 0.01);
         this.sendUpdate();
      });
      double logoX = this.logoPositionX != null ? this.logoPositionX : 0.0;
      this.method_37063(
         new class_357(col3X, startY, sliderWidth, 20, class_2561.method_43470("Logo X (Depth): " + String.format("%.2f", logoX)), (logoX + 10.0) / 20.0) {
            protected void method_25346() {
               FigurePositionScreen.this.logoPositionX = this.field_22753 * 20.0 - 10.0;
               this.method_25355(class_2561.method_43470("Logo X (Depth): " + String.format("%.2f", FigurePositionScreen.this.logoPositionX)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.logoPositionX = this.field_22753 * 20.0 - 10.0;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col3X + sliderWidth + 2, startY, () -> {
         this.logoPositionX = Math.max(-10.0, (this.logoPositionX != null ? this.logoPositionX : 0.0) - 0.001);
         this.sendUpdate();
      }, () -> {
         this.logoPositionX = Math.min(10.0, (this.logoPositionX != null ? this.logoPositionX : 0.0) + 0.001);
         this.sendUpdate();
      });
      double logoY = this.logoPositionY != null ? this.logoPositionY : 0.0;
      this.method_37063(
         new class_357(
            col3X, startY + 25, sliderWidth, 20, class_2561.method_43470("Logo Y (Vertical): " + String.format("%.2f", logoY)), (logoY + 10.0) / 20.0
         ) {
            protected void method_25346() {
               FigurePositionScreen.this.logoPositionY = this.field_22753 * 20.0 - 10.0;
               this.method_25355(class_2561.method_43470("Logo Y (Vertical): " + String.format("%.2f", FigurePositionScreen.this.logoPositionY)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.logoPositionY = this.field_22753 * 20.0 - 10.0;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col3X + sliderWidth + 2, startY + 25, () -> {
         this.logoPositionY = Math.max(-10.0, (this.logoPositionY != null ? this.logoPositionY : 0.0) - 0.001);
         this.sendUpdate();
      }, () -> {
         this.logoPositionY = Math.min(10.0, (this.logoPositionY != null ? this.logoPositionY : 0.0) + 0.001);
         this.sendUpdate();
      });
      double logoZ = this.logoPositionZ != null ? this.logoPositionZ : 0.0;
      this.method_37063(
         new class_357(
            col3X, startY + 50, sliderWidth, 20, class_2561.method_43470("Logo Z (Horizontal): " + String.format("%.2f", logoZ)), (logoZ + 10.0) / 20.0
         ) {
            protected void method_25346() {
               FigurePositionScreen.this.logoPositionZ = this.field_22753 * 20.0 - 10.0;
               this.method_25355(class_2561.method_43470("Logo Z (Horizontal): " + String.format("%.2f", FigurePositionScreen.this.logoPositionZ)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.logoPositionZ = this.field_22753 * 20.0 - 10.0;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col3X + sliderWidth + 2, startY + 50, () -> {
         this.logoPositionZ = Math.max(-10.0, (this.logoPositionZ != null ? this.logoPositionZ : 0.0) - 0.001);
         this.sendUpdate();
      }, () -> {
         this.logoPositionZ = Math.min(10.0, (this.logoPositionZ != null ? this.logoPositionZ : 0.0) + 0.001);
         this.sendUpdate();
      });
      double scaleX = this.logoScaleX != null ? this.logoScaleX : 5.0;
      this.method_37063(
         new class_357(col3X, startY + 75, sliderWidth, 20, class_2561.method_43470("Logo Width (X): " + String.format("%.2f", scaleX)), (scaleX - 0.5) / 9.5) {
            protected void method_25346() {
               FigurePositionScreen.this.logoScaleX = 0.5 + this.field_22753 * 9.5;
               this.method_25355(class_2561.method_43470("Logo Width (X): " + String.format("%.2f", FigurePositionScreen.this.logoScaleX)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.logoScaleX = 0.5 + this.field_22753 * 9.5;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col3X + sliderWidth + 2, startY + 75, () -> {
         this.logoScaleX = Math.max(0.5, (this.logoScaleX != null ? this.logoScaleX : 5.0) - 0.001);
         this.sendUpdate();
      }, () -> {
         this.logoScaleX = Math.min(10.0, (this.logoScaleX != null ? this.logoScaleX : 5.0) + 0.001);
         this.sendUpdate();
      });
      double scaleY = this.logoScaleY != null ? this.logoScaleY : 5.0;
      this.method_37063(
         new class_357(col3X, startY + 100, sliderWidth, 20, class_2561.method_43470("Logo Height (Y): " + String.format("%.2f", scaleY)), (scaleY - 0.5) / 9.5) {
            protected void method_25346() {
               FigurePositionScreen.this.logoScaleY = 0.5 + this.field_22753 * 9.5;
               this.method_25355(class_2561.method_43470("Logo Height (Y): " + String.format("%.2f", FigurePositionScreen.this.logoScaleY)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.logoScaleY = 0.5 + this.field_22753 * 9.5;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col3X + sliderWidth + 2, startY + 100, () -> {
         this.logoScaleY = Math.max(0.5, (this.logoScaleY != null ? this.logoScaleY : 5.0) - 0.001);
         this.sendUpdate();
      }, () -> {
         this.logoScaleY = Math.min(10.0, (this.logoScaleY != null ? this.logoScaleY : 5.0) + 0.001);
         this.sendUpdate();
      });
      double scaleZ = this.logoScaleZ != null ? this.logoScaleZ : 1.0;
      this.method_37063(
         new class_357(col3X, startY + 125, sliderWidth, 20, class_2561.method_43470("Logo Depth (Z): " + String.format("%.2f", scaleZ)), (scaleZ - 0.5) / 9.5) {
            protected void method_25346() {
               FigurePositionScreen.this.logoScaleZ = 0.5 + this.field_22753 * 9.5;
               this.method_25355(class_2561.method_43470("Logo Depth (Z): " + String.format("%.2f", FigurePositionScreen.this.logoScaleZ)));
               FigurePositionScreen.this.sendUpdate();
            }

            protected void method_25344() {
               FigurePositionScreen.this.logoScaleZ = 0.5 + this.field_22753 * 9.5;
               FigurePositionScreen.this.sendUpdate();
            }
         }
      );
      this.addFineTuneButtons(col3X + sliderWidth + 2, startY + 125, () -> {
         this.logoScaleZ = Math.max(0.5, (this.logoScaleZ != null ? this.logoScaleZ : 1.0) - 0.001);
         this.sendUpdate();
      }, () -> {
         this.logoScaleZ = Math.min(10.0, (this.logoScaleZ != null ? this.logoScaleZ : 1.0) + 0.001);
         this.sendUpdate();
      });
      int copyButtonY = startY + 160;
      int copyButtonWidth = 95;
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Copy Figure"), button -> {
         String data = String.format("Figure: X=%.3f Y=%.3f Z=%.3f Scale=%.3f", this.offsetX, this.offsetY, this.offsetZ, this.scale);
         this.field_22787.field_1774.method_1455(data);
      }).method_46434(centerX - 100, copyButtonY, copyButtonWidth, 20).method_46431());
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Copy Hitbox"), button -> {
         String data = String.format("Hitbox: X=%.3f Y=%.3f Z=%.3f", this.hitboxOffsetX, this.hitboxOffsetY, this.hitboxOffsetZ);
         this.field_22787.field_1774.method_1455(data);
      }).method_46434(centerX + 5, copyButtonY, copyButtonWidth, 20).method_46431());
      this.method_37063(
         class_4185.method_46430(
               class_2561.method_43470("Copy Logo"),
               button -> {
                  String data = String.format(
                     "Logo: X=%.3f Y=%.3f Z=%.3f Width=%.3f Height=%.3f Depth=%.3f",
                     this.logoPositionX != null ? this.logoPositionX : 0.0,
                     this.logoPositionY != null ? this.logoPositionY : 0.0,
                     this.logoPositionZ != null ? this.logoPositionZ : 0.0,
                     this.logoScaleX != null ? this.logoScaleX : 5.0,
                     this.logoScaleY != null ? this.logoScaleY : 5.0,
                     this.logoScaleZ != null ? this.logoScaleZ : 1.0
                  );
                  this.field_22787.field_1774.method_1455(data);
               }
            )
            .method_46434(centerX - 45, copyButtonY + 25, copyButtonWidth, 20)
            .method_46431()
      );
      this.method_37063(class_4185.method_46430(class_2561.method_43470("Reset All"), button -> {
         this.offsetX = 0.0;
         this.offsetY = 0.1;
         this.offsetZ = 0.0;
         this.scale = 1.0;
         this.hitboxOffsetX = -0.03;
         this.hitboxOffsetY = 0.0;
         this.hitboxOffsetZ = -0.06;
         this.logoPositionX = null;
         this.logoPositionY = null;
         this.logoPositionZ = null;
         this.logoScaleX = null;
         this.logoScaleY = null;
         this.logoScaleZ = null;
         this.method_41843();
         this.sendUpdate();
      }).method_46434(centerX - 100, copyButtonY + 50, 95, 20).method_46431());
      this.method_37063(
         class_4185.method_46430(class_2561.method_43470("Done"), button -> this.method_25419())
            .method_46434(centerX + 5, copyButtonY + 50, 95, 20)
            .method_46431()
      );
   }

   public void method_25394(@NotNull class_332 guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.method_25420(guiGraphics, mouseX, mouseY, partialTick);
      super.method_25394(guiGraphics, mouseX, mouseY, partialTick);
      guiGraphics.method_27534(this.field_22793, this.field_22785, this.field_22789 / 2, 20, 16777215);
      String skinTypeText = "Skin Type: Unknown";
      int skinTypeColor = 11184810;
      if (this.field_22787 != null && this.field_22787.field_1687 != null) {
         FigureDefinition figureDefinition;
         int skinIndex;
         label34: {
            class_2586 blockEntity = this.field_22787.field_1687.method_8321(this.blockPos);
            figureDefinition = null;
            skinIndex = 0;
            if (blockEntity instanceof BoxBlockEntity boxBlockEntity && boxBlockEntity.hasFigure()) {
               figureDefinition = boxBlockEntity.getFigureDefinition();
               skinIndex = boxBlockEntity.getAlternativeSkinIndex();
               break label34;
            }

            if (blockEntity instanceof FigureBlockEntity figureBlockEntity && figureBlockEntity.hasFigure()) {
               figureDefinition = figureBlockEntity.getFigureDefinition();
               skinIndex = figureBlockEntity.getAlternativeSkinIndex();
            }
         }

         if (figureDefinition != null) {
            try {
               class_2960 texture = this.getTextureForFigure(figureDefinition, skinIndex);
               if (texture != null) {
                  SkinModelDetector.SkinModel skinModel = SkinModelDetector.detectSkinModel(texture);
                  if (skinModel == SkinModelDetector.SkinModel.SLIM) {
                     skinTypeText = "Skin Type: SLIM (Alex)";
                     skinTypeColor = 16739229;
                  } else {
                     skinTypeText = "Skin Type: CLASSIC (Steve)";
                     skinTypeColor = 6139362;
                  }
               } else {
                  skinTypeText = "Skin Type: No Texture";
               }
            } catch (Exception var12) {
               skinTypeText = "Skin Type: Detection Error";
               skinTypeColor = 16711680;
            }
         } else {
            skinTypeText = "Skin Type: No Figure";
         }
      }

      guiGraphics.method_25300(this.field_22793, skinTypeText, this.field_22789 / 2, 30, skinTypeColor);
      int centerX = this.field_22789 / 2;
      int columnSpacing = 200;
      int headerY = 35;
      guiGraphics.method_25300(this.field_22793, "FIGURE", centerX - columnSpacing, headerY, 16766720);
      guiGraphics.method_25300(this.field_22793, "HITBOX", centerX, headerY, 65280);
      guiGraphics.method_25300(this.field_22793, "LOGO", centerX + columnSpacing, headerY, 49151);
   }

   private class_2960 getTextureForFigure(FigureDefinition figure, int skinIndex) {
      if (figure == null) {
         return null;
      } else {
         if (skinIndex > 0 && figure.hasAlternatives()) {
            int altListIndex = skinIndex - 1;
            if (altListIndex < figure.getAlternatives().size()) {
               return figure.getAlternatives().get(altListIndex).texture();
            }
         }

         if (figure.getType() == FigureType.PLAYER && figure.getPlayerUUID() != null) {
            class_2960 quickSkinLoc = getQuickSkinLocation(figure.getPlayerUUID());
            if (quickSkinLoc != null) {
               return quickSkinLoc;
            } else {
               if (class_310.method_1551().method_1562() != null) {
                  class_640 playerInfo = class_310.method_1551().method_1562().method_2871(figure.getPlayerUUID());
                  if (playerInfo != null) {
                     return playerInfo.method_52810().comp_1626();
                  }
               }

               return class_2960.method_60656("textures/entity/player/wide/steve.png");
            }
         } else {
            return figure.getTexturePath();
         }
      }
   }

   private void sendUpdate() {
      new FigurePositionPacket(
            this.blockPos,
            this.offsetX,
            this.offsetY,
            this.offsetZ,
            this.scale,
            this.hitboxOffsetX,
            this.hitboxOffsetY,
            this.hitboxOffsetZ,
            this.hitboxScaleX,
            this.hitboxScaleY,
            this.hitboxScaleZ,
            this.logoPositionX,
            this.logoPositionY,
            this.logoPositionZ,
            this.logoScaleX,
            this.logoScaleY,
            this.logoScaleZ
         )
         .sendToServer();
   }

   public void method_57734(float partialTick) {
   }

   public boolean method_25421() {
      return false;
   }
}
