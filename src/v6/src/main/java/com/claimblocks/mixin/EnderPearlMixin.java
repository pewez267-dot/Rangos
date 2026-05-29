/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1657
 *  net.minecraft.class_1684
 *  net.minecraft.class_1937
 *  net.minecraft.class_239
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.claimblocks.mixin;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1684;
import net.minecraft.class_1937;
import net.minecraft.class_239;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_1684.class})
public abstract class EnderPearlMixin {
    @Inject(method={"method_7488"}, at={@At(value="HEAD")}, cancellable=true)
    private void claimblocks$blockTeleport(class_239 hit, CallbackInfo ci) {
        class_1684 self = (class_1684)(Object)this;
        class_1937 world = self.method_37908();
        if (world == null || world.field_9236) {
            return;
        }
        class_1297 owner = self.method_24921();
        if (!(owner instanceof class_1657)) {
            return;
        }
        class_1657 player = (class_1657)owner;
        if (player.method_5687(2) && ClaimManager.getInstance().isBypassing(player.method_5667())) {
            return;
        }
        Claim c = ClaimManager.getInstance().getClaimAt(world, self.method_24515());
        if (c == null) {
            return;
        }
        if (c.canModify(player)) {
            return;
        }
        if (c.getFlags().publicMode || c.getFlags().blockEnderPearl) {
            self.method_31472();
            ci.cancel();
        }
    }
}

