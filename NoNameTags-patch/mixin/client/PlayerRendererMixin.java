package no.name.tags.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import no.name.tags.NoNameTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PlayerRenderer sobreescribe renderNameTag. El mixin de EntityRenderer (base) solo cancela el
 * nombre vanilla; los mods que dibujan lineas extra (ej. FantasticPass) lo hacen en el override de
 * PlayerRenderer (TAIL), que la base no cancela. Cancelando aqui en HEAD se oculta TODO el nametag
 * del jugador (nombre + lineas extra) al ocultar los nombres. Mismos parametros que el metodo real
 * para apuntar al override correcto (no al bridge sintetico).
 */
@Mixin(value = PlayerRenderer.class)
public class PlayerRendererMixin {

    @Inject(method = "renderNameTag", at = @At(value = "HEAD"), cancellable = true)
    private void cancelPlayerNameTag(AbstractClientPlayer player, Component displayName, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (NoNameTags.hideNameTags) {
            ci.cancel();
        }
    }
}
