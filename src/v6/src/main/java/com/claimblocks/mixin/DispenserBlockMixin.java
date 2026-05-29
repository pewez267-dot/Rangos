/*
 * DispenserBlockMixin v6.0.0
 * Cancela el método dispense cuando un dispenser fuera del claim
 * intenta proyectar contenido sobre/dentro de un claim que protege building.
 */
package com.claimblocks.mixin;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.minecraft.class_2315;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2680;
import net.minecraft.class_2741;
import net.minecraft.class_3218;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_2315.class)
public abstract class DispenserBlockMixin {
    @Inject(method = "method_10012", at = @At("HEAD"), cancellable = true)
    private void claimblocks$blockCrossClaimDispense(class_3218 world, class_2680 state, class_2338 pos, CallbackInfo ci) {
        if (world == null || world.field_9236) return;
        // Posición a la que se dispara el contenido (la celda en frente del dispenser)
        class_2350 facing;
        try {
            facing = state.method_11654(class_2741.field_12525); // FACING
        } catch (Exception e) {
            return;
        }
        class_2338 target = pos.method_10093(facing);

        ClaimManager mgr = ClaimManager.getInstance();
        Claim selfClaim = mgr.getClaimAt(world, pos);
        Claim targetClaim = mgr.getClaimAt(world, target);

        if (sameClaim(selfClaim, targetClaim)) return; // todo dentro del mismo claim (o fuera)
        if (protectsBuilding(targetClaim) || protectsBuilding(selfClaim)) {
            ci.cancel();
        }
    }

    private static boolean sameClaim(Claim a, Claim b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.getClaimId().equals(b.getClaimId());
    }

    private static boolean protectsBuilding(Claim c) {
        if (c == null) return false;
        return c.getFlags().publicMode || c.getFlags().blockBuilding;
    }
}
