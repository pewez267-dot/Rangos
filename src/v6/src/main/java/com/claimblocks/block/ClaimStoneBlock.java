/*
 * ClaimStoneBlock v6.0.0 - reescrito para integrar Polymer.
 * Permite que el cliente vanilla vea un bloque vanilla "disfrazado"
 * mientras el servidor mantiene la lógica custom.
 */
package com.claimblocks.block;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.gui.ClaimMenuHandler;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.class_124;
import net.minecraft.class_1269;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1935;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2561;
import net.minecraft.class_2680;
import net.minecraft.class_3222;
import net.minecraft.class_3965;
import net.minecraft.class_4970;
import net.minecraft.class_5250;

public class ClaimStoneBlock extends class_2248 implements PolymerBlock {
    private final ClaimTier tier;
    private final int radius;
    private final int height;

    public ClaimStoneBlock(class_4970.class_2251 settings, ClaimTier tier) {
        super(settings);
        this.tier = tier;
        this.radius = tier.radius;
        this.height = tier.height;
    }

    public ClaimTier getTier() { return this.tier; }
    public int getRadius() { return this.radius; }
    public int getHeight() { return this.height; }

    /**
     * Polymer: bloque vanilla que el cliente verá en lugar del custom.
     * Cada tier tiene su propio bloque vanilla para distinguirse.
     */
    @Override
    public class_2680 getPolymerBlockState(class_2680 state) {
        return polymerBlockFor(this.tier).method_9564();
    }

    public static class_2248 polymerBlockFor(ClaimTier tier) {
        return switch (tier.id) {
            case "claimstone_10x10"   -> class_2246.field_10445; // COBBLESTONE
            case "claimstone_25x25"   -> class_2246.field_10115; // ANDESITE
            case "claimstone_40x40"   -> class_2246.field_10093; // POLISHED_ANDESITE
            case "claimstone_64x64"   -> class_2246.field_10360; // SMOOTH_STONE
            case "claimstone_80x80"   -> class_2246.field_10289; // POLISHED_GRANITE
            case "claimstone_100x100" -> class_2246.field_10205; // GOLD_BLOCK
            case "claimstone_150x150" -> class_2246.field_27119; // COPPER_BLOCK
            case "claimstone_250x250" -> class_2246.field_10201; // DIAMOND_BLOCK
            case "claimstone_300x300" -> class_2246.field_22108; // NETHERITE_BLOCK
            case "claimstone_500x500" -> class_2246.field_10327; // BEACON
            default -> class_2246.field_10445;
        };
    }

    @Override
    protected class_1269 method_55766(class_2680 state, class_1937 world, class_2338 pos, class_1657 player, class_3965 hit) {
        if (world.field_9236) return class_1269.field_5812;
        Claim claim = ClaimManager.getInstance().getClaimByCenter(world, pos);
        if (claim == null) {
            player.method_7353(class_2561.method_43470("[x] Esta piedra no tiene zona registrada.").method_27692(class_124.field_1061), false);
            return class_1269.field_21466;
        }
        if (!claim.isOwner(player) && !player.method_5687(2)) {
            player.method_7353(class_2561.method_43470("[x] Solo el dueño puede administrar esta zona.").method_27692(class_124.field_1061), false);
            return class_1269.field_21466;
        }
        if (player instanceof class_3222 sp) {
            ClaimMenuHandler.open(sp, claim, 0);
        }
        return class_1269.field_21466;
    }

    @Override
    public void method_9567(class_1937 world, class_2338 pos, class_2680 state, class_1309 placer, class_1799 stack) {
        super.method_9567(world, pos, state, placer, stack);
        if (world.field_9236) return;
        if (!(placer instanceof class_1657 player)) return;
        ClaimManager mgr = ClaimManager.getInstance();

        // FIX v6: validar límite por jugador antes de wouldOverlap
        int max = ClaimManager.getMaxClaimsPerPlayer();
        if (max > 0 && !player.method_5687(2)) {
            int owned = mgr.getClaimsOf(player.method_5667()).size();
            if (owned >= max) {
                world.method_8501(pos, class_2246.field_10124.method_9564());
                if (!player.method_31549().field_7477) player.method_7270(new class_1799((class_1935) this));
                player.method_7353(class_2561.method_43470("[x] Has alcanzado el límite de zonas (" + max + ").").method_27695(new class_124[]{class_124.field_1061, class_124.field_1067}), true);
                return;
            }
        }

        if (mgr.wouldOverlap(world, pos, this.radius, this.height)) {
            world.method_8501(pos, class_2246.field_10124.method_9564());
            if (!player.method_31549().field_7477) player.method_7270(new class_1799((class_1935) this));
            player.method_7353(class_2561.method_43470("[x] Esta zona se solaparía con otra existente.").method_27695(new class_124[]{class_124.field_1061, class_124.field_1067}), true);
            return;
        }
        mgr.createClaim(world, pos, player, this.tier);
        class_5250 msg = class_2561.method_43470("\u2714 ").method_27695(new class_124[]{class_124.field_1060, class_124.field_1067})
                .method_10852(class_2561.method_43470("Zona creada: ").method_27692(class_124.field_1060))
                .method_10852(class_2561.method_43470(this.tier.label()).method_27695(new class_124[]{class_124.field_1054, class_124.field_1067}))
                .method_10852(class_2561.method_43470(" bloques | Altura: +/-" + this.height).method_27692(class_124.field_1080));
        player.method_7353(msg, false);
    }

    /**
     * NOTA v6: la protección al romper se mueve a PlayerBlockBreakEvents.BEFORE
     * (en BlockProtectionEvents). Aquí solo limpiamos el claim cuando el dueño
     * (o un OP) sí rompe legítimamente la piedra.
     */
    @Override
    public class_2680 method_9576(class_1937 world, class_2338 pos, class_2680 state, class_1657 player) {
        if (!world.field_9236) {
            Claim claim = ClaimManager.getInstance().getClaimByCenter(world, pos);
            if (claim != null && (claim.isOwner(player) || player.method_5687(2))) {
                ClaimManager.getInstance().removeClaim(world, pos);
                player.method_7353(class_2561.method_43470("\u2714 Zona eliminada.").method_27692(class_124.field_1060), false);
            }
        }
        return super.method_9576(world, pos, state, player);
    }
}
