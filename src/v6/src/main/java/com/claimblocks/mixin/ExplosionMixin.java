/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  net.minecraft.class_1927
 *  net.minecraft.class_1937
 *  net.minecraft.class_2338
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.claimblocks.mixin;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.class_1927;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_1927.class})
public abstract class ExplosionMixin {
    @Shadow
    @Final
    private class_1937 field_9187;
    @Shadow
    @Final
    private ObjectArrayList<class_2338> field_9188;

    @Inject(method={"method_8348"}, at={@At(value="RETURN")})
    private void claimblocks$filterAffectedBlocks(CallbackInfo ci) {
        if (this.field_9187 == null || this.field_9187.field_9236) {
            return;
        }
        if (this.field_9188 == null || this.field_9188.isEmpty()) {
            return;
        }
        this.field_9188.removeIf(pos -> {
            Claim c = ClaimManager.getInstance().getClaimAt(this.field_9187, (class_2338)pos);
            return c != null && c.getFlags().blockExplosions;
        });
    }
}

