/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_1293
 *  net.minecraft.class_1294
 *  net.minecraft.class_1657
 *  net.minecraft.class_1934
 *  net.minecraft.class_1937
 *  net.minecraft.class_2561
 *  net.minecraft.class_3218
 *  net.minecraft.class_3222
 *  net.minecraft.server.MinecraftServer
 */
package com.claimblocks.event;

import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.class_124;
import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_1657;
import net.minecraft.class_1934;
import net.minecraft.class_1937;
import net.minecraft.class_2561;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.server.MinecraftServer;

public final class PassiveEffectsManager {
    private static final int RUN_EVERY_TICKS = 20;
    private static int counter = 0;
    private static final Set<UUID> grantedFlight = new HashSet<UUID>();

    private PassiveEffectsManager() {
    }

    public static void tick(MinecraftServer server) {
        if (++counter % 20 != 0) {
            return;
        }
        boolean runEffects = counter % 40 == 0;
        for (class_3218 world : server.method_3738()) {
            String dim = world.method_27983().method_29177().toString();
            for (class_3222 player : world.method_18456()) {
                Claim claim = ClaimManager.getInstance().getClaimAt((class_1937)world, player.method_24515());
                PassiveEffectsManager.handleFlight(player, claim);
                if (!runEffects) continue;
                PassiveEffectsManager.applyEffects(player, claim);
            }
        }
    }

    private static void handleFlight(class_3222 player, Claim claim) {
        boolean inClaimWithFlight = claim != null && claim.canModify((class_1657)player) && claim.getTier() != null && claim.getTier().isPaid() && claim.getFlags().allowFlight;
        boolean creativeOrSpectator = player.field_13974.method_14257() == class_1934.field_9220 || player.field_13974.method_14257() == class_1934.field_9219;
        UUID id = player.method_5667();
        if (inClaimWithFlight) {
            if (!player.method_31549().field_7478 && !creativeOrSpectator) {
                player.method_31549().field_7478 = true;
                grantedFlight.add(id);
                player.method_7355();
                player.method_7353((class_2561)class_2561.method_43470((String)"\u2714 Vuelo activado en esta zona.").method_27692(class_124.field_1060), true);
            } else if (creativeOrSpectator) {
                grantedFlight.remove(id);
            }
        } else if (grantedFlight.contains(id)) {
            if (!creativeOrSpectator) {
                player.method_31549().field_7478 = false;
                player.method_31549().field_7479 = false;
                player.method_7355();
                player.method_7353((class_2561)class_2561.method_43470((String)"[i] Saliste de la zona de vuelo.").method_27692(class_124.field_1075), true);
            }
            grantedFlight.remove(id);
        }
    }

    private static void applyEffects(class_3222 player, Claim claim) {
        if (claim == null) {
            return;
        }
        ClaimTier tier = claim.getTier();
        if (tier == null || !tier.isPaid()) {
            return;
        }
        if (!claim.canModify((class_1657)player)) {
            return;
        }
        if (claim.getFlags().effectRegeneration) {
            player.method_6092(new class_1293(class_1294.field_5924, 60, 0, true, false, true));
        }
        if (claim.getFlags().effectResistance) {
            player.method_6092(new class_1293(class_1294.field_5907, 60, 0, true, false, true));
        }
        if (claim.getFlags().effectSpeed) {
            player.method_6092(new class_1293(class_1294.field_5904, 60, 0, true, false, true));
        }
    }
}

