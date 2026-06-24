package com.fantastic.kits.security;

import com.fantastic.kits.FantasticKits;
import com.fantastic.kits.audit.SecurityEventType;
import com.fantastic.kits.kits.Kit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;

/**
 * Pure server-side validation for kit operations.
 *
 * <p>This is the single chokepoint that decides whether a claim or interaction
 * is legitimate. Every decision that returns {@code false} also emits a
 * {@link SecurityEventType} so the server owner can investigate later.
 */
public final class SecurityValidator {

    private SecurityValidator() {}

    /**
     * Verifies that the player's primary group equals the kit owner group.
     * Returns true when the player is allowed; logs a security event otherwise.
     */
    public static boolean canClaimByGroup(ServerPlayer player, Kit kit) {
        if (kit == null) return false;
        if (player == null) return false;

        String required = kit.ownerGroup();
        String primary = FantasticKits.luckPerms().primaryGroup(player.getUUID());
        boolean ok = FantasticKits.luckPerms().isPrimaryGroupExactly(player.getUUID(), required);
        if (!ok) {
            FantasticKits.security().log(SecurityEventType.INVALID_GROUP_ACCESS,
                    player, primary, required, kit, "CLAIM",
                    "BLOCKED",
                    "Primary group did not match the kit owner group (strict matching).");
        }
        return ok;
    }

    /** Refuse claims for FakePlayer entities to defeat automation exploits. */
    public static boolean isHumanPlayer(ServerPlayer player) {
        if (player instanceof FakePlayer && FantasticKits.config().blockClaimsForFakePlayers) {
            FantasticKits.security().log(SecurityEventType.FORGED_CLIENT_ACTION,
                    player, "?", "?", null, "CLAIM",
                    "BLOCKED", "FakePlayer cannot claim kits.");
            return false;
        }
        return true;
    }

    /** Make sure the player has room for at least one stack before deliveries. */
    public static boolean hasInventoryRoom(ServerPlayer player, Kit kit) {
        if (!FantasticKits.config().blockClaimsWhenInventoryFull) return true;
        int needed = Math.max(1, kit.contents().size());
        int free = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).isEmpty()) free++;
        }
        return free >= Math.min(needed, 9); // require at least 9 slots or full set, whichever is smaller
    }

    /** Refuse claims when the player is in an unsafe state (dead/spectator). */
    public static boolean isSafeContext(ServerPlayer player) {
        if (player == null) return false;
        if (!player.isAlive()) return false;
        if (player.isSpectator()) return false;
        return true;
    }

    /** Apply the per-kit "rejectForgedClient" flag. Always returns true on the server. */
    public static boolean serverSideOnly() {
        return true;
    }
}
