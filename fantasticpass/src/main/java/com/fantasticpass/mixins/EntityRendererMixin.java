package com.fantasticpass.mixins;

import com.fantasticpass.nametag.ClientNametagCache;
import com.fantasticpass.nametag.NametagBuilder;
import com.fantasticpass.nametag.NametagData;
import com.fantasticpass.nametag.NametagRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Non-invasive injection that draws an additional rank line beneath a player's name.
 * It runs at the TAIL of {@code renderNameTag}, after the vanilla name has been drawn,
 * and never alters the vanilla rendering path.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Shadow
    @Final
    protected EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    public abstract Font getFont();

    @Inject(method = "renderNameTag", at = @At("TAIL"))
    private void fantasticpass$renderRankLine(Entity entity, Component displayName, PoseStack poseStack,
                                              MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer)) {
            return;
        }

        NametagData data = ClientNametagCache.get(entity.getUUID());
        if (data == null || !data.hasLine()) {
            return;
        }

        if (this.entityRenderDispatcher.distanceToSqr(entity) > 4096.0D) {
            return;
        }

        Component line = NametagBuilder.buildLine(data);
        NametagRenderer.render(entity, line, poseStack, buffer, packedLight,
                this.entityRenderDispatcher, this.getFont());
    }
}
