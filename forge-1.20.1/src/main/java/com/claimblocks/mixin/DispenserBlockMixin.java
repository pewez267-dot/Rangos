package com.claimblocks.mixin;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancela el dispense cuando un dispenser apunta dentro de un claim ajeno protegido.
 */
@Mixin(DispenserBlock.class)
public abstract class DispenserBlockMixin {
    @Inject(method = "dispenseFrom", at = @At("HEAD"), cancellable = true)
    private void claimblocks$blockCrossClaimDispense(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        BlockState state = level.getBlockState(pos);
        Direction facing;
        try {
            facing = state.getValue(DispenserBlock.FACING);
        } catch (Exception e) {
            return;
        }
        BlockPos target = pos.relative(facing);
        ClaimManager mgr = ClaimManager.getInstance();
        Claim self = mgr.getClaimAt(level, pos);
        Claim tgt = mgr.getClaimAt(level, target);
        if (sameClaim(self, tgt)) return;
        if (protectsBuilding(tgt) || protectsBuilding(self)) {
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
