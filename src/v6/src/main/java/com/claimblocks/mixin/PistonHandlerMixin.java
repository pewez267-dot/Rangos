/*
 * PistonHandlerMixin v6.0.0
 * Cancela el push/pull cuando una operación de pistón cruzaría límites
 * entre claims (entre fuera-claim y dentro-claim, o entre dos claims distintos).
 */
package com.claimblocks.mixin;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import java.util.List;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2674;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_2674.class)
public abstract class PistonHandlerMixin {
    @Shadow @Final private class_1937 field_12249;          // world
    @Shadow @Final private class_2338 field_12250;          // posFrom (pistón)
    @Shadow @Final private class_2350 field_12243;          // motionDirection
    @Shadow @Final private List<class_2338> field_12245;    // movedBlocks

    @Inject(method = "method_11537", at = @At("RETURN"), cancellable = true)
    private void claimblocks$blockCrossClaimPiston(CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (this.field_12249 == null || this.field_12249.field_9236) return;
        if (this.field_12245 == null || this.field_12245.isEmpty()) return;

        ClaimManager mgr = ClaimManager.getInstance();
        Claim pistonClaim = mgr.getClaimAt(this.field_12249, this.field_12250);

        for (class_2338 origin : this.field_12245) {
            class_2338 dest = origin.method_10093(this.field_12243);
            Claim originClaim = mgr.getClaimAt(this.field_12249, origin);
            Claim destClaim = mgr.getClaimAt(this.field_12249, dest);
            if (sameClaimRef(originClaim, destClaim) && sameClaimRef(pistonClaim, originClaim)) continue;
            // Hay desalineación: bloquear si alguno de los claims involucrados protege building
            if (protectsBuilding(originClaim) || protectsBuilding(destClaim) || protectsBuilding(pistonClaim)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    private static boolean sameClaimRef(Claim a, Claim b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.getClaimId().equals(b.getClaimId());
    }

    private static boolean protectsBuilding(Claim c) {
        if (c == null) return false;
        return c.getFlags().publicMode || c.getFlags().blockBuilding;
    }
}
