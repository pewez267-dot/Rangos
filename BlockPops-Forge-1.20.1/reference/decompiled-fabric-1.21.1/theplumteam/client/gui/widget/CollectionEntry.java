package com.theplumteam.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.theplumteam.client.discovery.ClientDiscoveryManager;
import com.theplumteam.client.gui.util.GuiScaleManager;
import com.theplumteam.figure.FigureCollection;
import com.theplumteam.figure.FigureDefinition;
import net.minecraft.class_124;
import net.minecraft.class_2558;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_2558.class_2559;
import net.minecraft.class_4280.class_4281;

public class CollectionEntry extends class_4281<CollectionEntry> {
   private final class_310 mc;
   private final FigureCollection collection;
   private final CollectionListWidget parent;
   private static final int PADDING = 4;
   private static final int TOP_PADDING = 12;
   private static final int LOGO_MAX_SIZE = 56;
   private static final int LINK_BUTTON_SIZE = 14;
   private static final class_2960 COLLECTION_GLOBE_ICON = class_2960.method_60655("blockpops", "textures/gui/search_icon.png");
   private int linkButtonX;
   private int linkButtonY;
   private boolean isLinkHovered;

   public CollectionEntry(CollectionListWidget parent, FigureCollection collection) {
      this.parent = parent;
      this.mc = class_310.method_1551();
      this.collection = collection;
   }

   public void method_25343(
      class_332 graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isMouseOver, float partialTick
   ) {
      float scale = GuiScaleManager.isUsingInverseScale() ? GuiScaleManager.getRenderScaleFactor() : 1.0F;
      int effectivePadding = (int)(4.0F * scale);
      int effectiveTopPadding = (int)(12.0F * scale);
      int effectiveLogoMaxSize = (int)(56.0F * scale);
      int effectiveLinkButtonSize = (int)(14.0F * scale);
      boolean isSelected = this.parent.method_25334() == this;
      int highlightPaddingH = (int)(4.0F * scale);
      int highlightPaddingV = (int)(2.0F * scale);
      int highlightLeft = x - highlightPaddingH;
      int highlightRight = x + entryWidth - 10;
      int highlightTop = y - highlightPaddingV;
      int highlightBottom = y + entryHeight + highlightPaddingV;
      if (isSelected) {
         graphics.method_25294(highlightLeft, highlightTop, highlightRight, highlightBottom, -2144301888);
         graphics.method_49601(highlightLeft, highlightTop, highlightRight - highlightLeft, highlightBottom - highlightTop, -12549889);
      } else if (isMouseOver) {
         graphics.method_25294(highlightLeft, highlightTop, highlightRight, highlightBottom, 822083583);
      }

      int logoContainerX = x + effectivePadding;
      class_2960 logoTexture = this.collection.getLogoTexture();
      int textStartX = logoContainerX + effectiveLogoMaxSize + effectivePadding;
      if (logoTexture != null) {
         graphics.method_51452();
         int logoX = logoContainerX + (effectiveLogoMaxSize - effectiveLogoMaxSize) / 2;
         int logoY = y + (entryHeight - effectiveLogoMaxSize) / 2;
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         graphics.method_51448().method_22903();
         graphics.method_51448().method_46416((float)logoX, (float)logoY, 0.0F);
         float logoScale = (float)effectiveLogoMaxSize / 256.0F;
         graphics.method_51448().method_22905(logoScale, logoScale, 1.0F);
         graphics.method_25290(logoTexture, 0, 0, 0.0F, 0.0F, 256, 256, 256, 256);
         graphics.method_51448().method_22909();
         graphics.method_51452();
      }

      int textY = y + effectiveTopPadding;
      int textColor = isSelected ? 16777215 : 14737632;
      graphics.method_51433(this.mc.field_1772, this.collection.getName(), textStartX, textY, textColor, false);
      int totalFigures = this.collection.getFigures().size();
      int discoveredCount = 0;

      for (FigureDefinition figure : this.collection.getFigures()) {
         String figureId = this.collection.getId() + ":" + figure.getId();
         if (ClientDiscoveryManager.isDiscovered(figureId)) {
            discoveredCount++;
         }
      }

      String figureCount = discoveredCount + "/" + totalFigures + " figures";
      int subTextY = textY + 9 + 2;
      int subTextColor = isSelected ? 11184810 : 8421504;
      graphics.method_51433(this.mc.field_1772, figureCount, textStartX, subTextY, subTextColor, false);
      String author = this.collection.getAuthor();
      class_2561 authorText;
      if ("world_players".equals(this.collection.getId())) {
         authorText = class_2561.method_43470("Auto-generated skins").method_27692(class_124.field_1065);
      } else if (author.equals("Unknown")) {
         authorText = class_2561.method_43470("Collection with multiple creators").method_27692(class_124.field_1065);
      } else {
         authorText = class_2561.method_43470("Skin creator: ")
            .method_27692(class_124.field_1065)
            .method_10852(class_2561.method_43470(author).method_27692(class_124.field_1065));
      }

      int authorY = subTextY + 9 + 2;
      graphics.method_51439(this.mc.field_1772, authorText, textStartX, authorY, 16777215, false);
      this.isLinkHovered = false;
      if (isMouseOver && this.collection.getAuthorUrl() != null && !this.collection.getAuthorUrl().isEmpty()) {
         int margin = 4;
         this.linkButtonX = highlightRight - effectiveLinkButtonSize - margin;
         this.linkButtonY = highlightTop + margin;
         boolean linkHovered = mouseX >= this.linkButtonX
            && mouseX < this.linkButtonX + effectiveLinkButtonSize
            && mouseY >= this.linkButtonY
            && mouseY < this.linkButtonY + effectiveLinkButtonSize;
         graphics.method_25294(
            this.linkButtonX,
            this.linkButtonY,
            this.linkButtonX + effectiveLinkButtonSize,
            this.linkButtonY + effectiveLinkButtonSize,
            linkHovered ? -4684277 : -1063686144
         );
         graphics.method_49601(this.linkButtonX, this.linkButtonY, effectiveLinkButtonSize, effectiveLinkButtonSize, linkHovered ? -2448096 : -4684277);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         int iconSize = effectiveLinkButtonSize - 4;
         int iconX = this.linkButtonX + 2;
         int iconY = this.linkButtonY + 2;
         graphics.method_51448().method_22903();
         graphics.method_51448().method_46416((float)iconX, (float)iconY, 0.0F);
         float iconScale = (float)iconSize / 256.0F;
         graphics.method_51448().method_22905(iconScale, iconScale, 1.0F);
         graphics.method_25290(COLLECTION_GLOBE_ICON, 0, 0, 0.0F, 0.0F, 256, 256, 256, 256);
         graphics.method_51448().method_22909();
         RenderSystem.disableBlend();
         this.isLinkHovered = linkHovered;
      }
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (this.isLinkHovered && this.collection.getAuthorUrl() != null && !this.collection.getAuthorUrl().isEmpty()) {
            try {
               this.mc.field_1755.method_25430(class_2583.field_24360.method_10958(new class_2558(class_2559.field_11749, this.collection.getAuthorUrl())));
            } catch (Exception var7) {
               System.err.println("Failed to open URL: " + this.collection.getAuthorUrl());
            }

            return true;
         } else {
            this.parent.method_25313(this);
            this.parent.onCollectionSelected(this);
            return true;
         }
      } else {
         return false;
      }
   }

   public class_2561 method_37006() {
      int totalFigures = this.collection.getFigures().size();
      int discoveredCount = 0;

      for (FigureDefinition figure : this.collection.getFigures()) {
         String figureId = this.collection.getId() + ":" + figure.getId();
         if (ClientDiscoveryManager.isDiscovered(figureId)) {
            discoveredCount++;
         }
      }

      return class_2561.method_43470(this.collection.getName() + " - " + discoveredCount + "/" + totalFigures + " figures");
   }

   public FigureCollection getCollection() {
      return this.collection;
   }
}
