package com.fantasticpass.gui.admin;

import com.fantasticpass.data.NametagStyle;
import com.fantasticpass.gui.GuiTheme;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ColorEditorScreen extends Screen {
   @Nullable
   private final Screen parent;
   private final BiConsumer<NametagStyle, String> onDone;
   private final NametagStyle initialStyle;
   private final String initialText;
   private ColorEditorWidget editor;
   private EditBox textField;

   public ColorEditorScreen(@Nullable Screen parent, NametagStyle style, String text, BiConsumer<NametagStyle, String> onDone) {
      super(Component.literal("Nametag Style Editor"));
      this.parent = parent;
      this.initialStyle = style == null ? new NametagStyle() : style.copy();
      this.initialText = text == null ? "" : text;
      this.onDone = onDone;
   }

   private int leftX() {
      return this.width / 2 - 120;
   }

   protected void init() {
      int leftX = this.leftX();
      this.textField = (EditBox)this.addRenderableWidget(new EditBox(this.font, leftX, 42, 240, 16, Component.translatable("fantasticpass.gui.rank_text")));
      this.textField.setMaxLength(64);
      this.textField.setValue(this.initialText);
      this.editor = new ColorEditorWidget(this.initialStyle, this.initialText);
      this.editor.setPreviewContext(Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getGameProfile().getName() : "Player", 100);
      this.textField.setResponder(this.editor::setRankText);
      this.editor.build(this::addRenderableWidget, this.font, leftX, 64);
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.confirm()).bounds(this.width - 174, 8, 80, 18).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose()).bounds(this.width - 90, 8, 80, 18).build());
   }

   private void confirm() {
      if (this.onDone != null) {
         this.onDone.accept(this.editor.getStyle(), this.textField.getValue());
      }

      this.onClose();
   }

   public void onClose() {
      Minecraft.getInstance().setScreen(this.parent);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      GuiTheme.drawBackground(graphics, this.width, this.height);
      graphics.drawString(this.font, this.title, this.leftX(), 14, -16718337, false);
      graphics.drawString(this.font, Component.translatable("fantasticpass.gui.rank_text"), this.leftX(), 32, -5592406, false);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.editor.renderPalette(graphics);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return button == 0 && this.editor.handlePaletteClick(mouseX, mouseY) ? true : super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean isPauseScreen() {
      return false;
   }
}
