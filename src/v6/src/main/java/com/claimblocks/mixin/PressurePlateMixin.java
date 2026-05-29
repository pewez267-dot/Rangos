/*
 * PressurePlateMixin v6.1.0
 * Impide que los visitantes activen placas de presión (paneles del suelo)
 * dentro de un claim cuando el flag de Puertas/Accesos está activo.
 *
 * Las placas se activan al PISARLAS, no con click derecho, así que no hay
 * forma de cubrir esto con UseBlockCallback; se necesita este mixin.
 */
package com.claimblocks.mixin;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import java.util.List;
import net.minecraft.class_1937;
import net.minecraft.class_2231;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_2231.class)
public abstract class PressurePlateMixin {
    @Inject(method = "method_9434", at = @At("HEAD"), cancellable = true)
    private void claimblocks$blockVisitorPlate(class_1937 world, class_2338 pos, CallbackInfoReturnable<Integer> cir) {
        if (!(world instanceof class_3218 sw)) return;
        Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
        if (claim == null) return;
        if (!(claim.getFlags().blockDoorsAccess || claim.getFlags().publicMode)) return;

        // Caja justo encima de la placa para detectar quién la está pisando.
        class_238 box = new class_238(
                pos.method_10263(), pos.method_10264(), pos.method_10260(),
                pos.method_10263() + 1, pos.method_10264() + 1, pos.method_10260() + 1
        ).method_1014(0.05);

        List<class_3222> players = sw.method_18467(class_3222.class, box);
        if (players.isEmpty()) return; // sin jugadores: dejar que vanilla maneje (mobs/items)

        boolean anyAuthorized = false;
        for (class_3222 p : players) {
            boolean bypass = p.method_5687(2) && ClaimManager.getInstance().isBypassing(p.method_5667());
            if (claim.canModify(p) || bypass) {
                anyAuthorized = true;
                break;
            }
        }
        // Si solo hay visitantes parados, la placa no emite señal (no abre puertas).
        if (!anyAuthorized) {
            cir.setReturnValue(0);
        }
    }
}
