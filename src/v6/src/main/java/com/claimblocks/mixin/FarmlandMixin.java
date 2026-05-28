/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1657
 *  net.minecraft.class_1937
 *  net.minecraft.class_2338
 *  net.minecraft.class_2344
 *  net.minecraft.class_2680
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
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2344;
import net.minecraft.class_2680;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_2344.class})
public abstract class FarmlandMixin {
    @Inject(method={"method_9554"}, at={@At(value="HEAD")}, cancellable=true)
    private void claimblocks$cancelTrampling(class_1937 world, class_2680 state, class_2338 pos, class_1297 entity, float fallDistance, CallbackInfo ci) {
        if (world == null || world.field_9236) {
            return;
        }
        if (!(entity instanceof class_1657)) {
            return;
        }
        class_1657 player = (class_1657)entity;
        Claim c = ClaimManager.getInstance().getClaimAt(world, pos);
        if (c == null) {
            return;
        }
        if (c.canModify(player)) {
            return;
        }
        if (c.getFlags().publicMode || c.getFlags().blockTrampling) {
            ci.cancel();
        }
    }
}

