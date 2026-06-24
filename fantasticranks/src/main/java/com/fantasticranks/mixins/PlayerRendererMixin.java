package com.fantasticranks.mixins;

import com.fantasticranks.interop.FantasticPassInterop;
import com.fantasticranks.nametag.ClientNametagCache;
import com.fantasticranks.nametag.NametagBuilder;
import com.fantasticranks.nametag.NametagData;
import com.fantasticranks.nametag.NametagRenderer;
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

/**
 * Non-invasive injection that draws the rank line beneath a player's name.
 *
 * <p>It targets {@link PlayerRenderer#renderNameTag} (not the base {@code EntityRenderer})
 * so the line is drawn exactly once per player even when a "belowName" scoreboard objective
 * makes the vanilla method call {@code super} multiple times. The injection is at
 * {@code TAIL} and only adds rendering. If Fantastic Pass is installed and is already
 * rendering a line for this player, Fantastic Ranks yields the slot.
 */
@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {

    @Inject(method = "renderNameTag", at = @At("TAIL"))
    private void fantasticranks$renderRankLine(AbstractClientPlayer player, Component displayName, PoseStack poseStack,
                                               MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        NametagData data = ClientNametagCache.get(player.getUUID());
        if (data == null || !data.hasLine()) {
            return;
        }

        // Yield to Fantastic Pass if it is currently rendering this player's line.
        if (FantasticPassInterop.shouldCedeNametag(player.getUUID())) {
            return;
        }

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (dispatcher.distanceToSqr(player) > 4096.0D) {
            return;
        }

        Component line = NametagBuilder.buildLine(data);
        NametagRenderer.render(player, line, poseStack, buffer, packedLight,
                dispatcher, Minecraft.getInstance().font);
    }
}
