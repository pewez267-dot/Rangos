package com.fantasticpass.mixins;

import com.fantasticpass.nametag.ClientNametagCache;
import com.fantasticpass.nametag.NametagBuilder;
import com.fantasticpass.nametag.NametagData;
import com.fantasticpass.nametag.NametagRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerRenderer.class})
public class PlayerRendererMixin {
   @Inject(
      method = {"renderNameTag"},
      at = {@At("TAIL")}
   )
   private void fantasticpass$renderRankLine(
      AbstractClientPlayer player, Component displayName, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci
   ) {
      NametagData data = ClientNametagCache.get(player.getUUID());
      if (data != null && data.hasLine()) {
         EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
         if (!(dispatcher.distanceToSqr(player) > 4096.0)) {
            Component line = NametagBuilder.buildLine(data);
            NametagRenderer.render(player, line, poseStack, buffer, packedLight, dispatcher, Minecraft.getInstance().font);
         }
      }
   }
}
