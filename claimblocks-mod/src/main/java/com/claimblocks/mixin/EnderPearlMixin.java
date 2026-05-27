package com.claimblocks.mixin;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels an Ender Pearl's teleport when it lands inside a claim that has
 * {@code blockEnderPearl} on AND the thrower is an intruder. The pearl is
 * silently discarded so the player neither teleports nor takes the usual
 * 5 hearts of fall damage.
 */
@Mixin(EnderPearlEntity.class)
public abstract class EnderPearlMixin {

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void claimblocks$blockTeleport(HitResult hit, CallbackInfo ci) {
        EnderPearlEntity self = (EnderPearlEntity) (Object) this;
        World world = self.getWorld();
        if (world == null || world.isClient) return;
        Entity owner = self.getOwner();
        if (!(owner instanceof PlayerEntity player)) return;

        // Bypass mode for OPs
        if (player.hasPermissionLevel(2)
            && ClaimManager.getInstance().isBypassing(player.getUuid())) return;

        Claim c = ClaimManager.getInstance().getClaimAt(world, self.getBlockPos());
        if (c == null) return;
        if (c.canModify(player)) return;
        if (c.getFlags().publicMode || c.getFlags().blockEnderPearl) {
            self.discard();
            ci.cancel();
        }
    }
}
