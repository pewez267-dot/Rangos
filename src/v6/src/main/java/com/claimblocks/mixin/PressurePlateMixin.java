/*
 * PressurePlateMixin v6.1.1
 * Impide que los visitantes activen placas de presión (paneles del suelo)
 * dentro de un claim cuando el flag de Puertas/Accesos está activo.
 *
 * Inyecta en AbstractPressurePlateBlock.updatePlateState (method_9433), que es
 * un método CONCRETO (getRedstoneOutput es abstracto y no se puede interceptar).
 * Cuando el que pisa la placa es un jugador no autorizado, se cancela la
 * actualización y la placa nunca se enciende.
 */
package com.claimblocks.mixin;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.minecraft.class_1297;
import net.minecraft.class_1937;
import net.minecraft.class_2231;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_2231.class)
public abstract class PressurePlateMixin {
    @Inject(method = "method_9433", at = @At("HEAD"), cancellable = true)
    private void claimblocks$blockVisitorPlate(class_1297 entity, class_1937 world, class_2338 pos, class_2680 state, int output, CallbackInfo ci) {
        if (world == null || world.field_9236) return;
        // Solo nos interesa cuando quien activa la placa es un jugador.
        if (!(entity instanceof class_3222 player)) return;
        Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
        if (claim == null) return;
        if (!(claim.getFlags().blockDoorsAccess || claim.getFlags().publicMode)) return;
        boolean bypass = player.method_5687(2) && ClaimManager.getInstance().isBypassing(player.method_5667());
        if (claim.canModify(player) || bypass) return; // autorizado: dejar pasar
        // Visitante: la placa no se actualiza (no se enciende).
        ci.cancel();
    }
}
