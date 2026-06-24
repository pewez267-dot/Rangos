package com.fantastickits.security;

import com.fantastickits.FantasticKits;
import com.fantastickits.data.ConfigHandler;
import com.fantastickits.data.KitData;
import com.fantastickits.data.KitDefinition;
import com.fantastickits.data.PlayerData;
import com.fantastickits.integration.LuckPermsIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Central security manager that performs all server-side validations.
 * NEVER trust client data. All checks are authoritative on the server.
 */
public final class SecurityManager {

    private SecurityManager() {}

    /**
     * Result of a claim validation check.
     */
    public static class ClaimResult {
        private final boolean allowed;
        private final String reason;

        private ClaimResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }

        public boolean isAllowed() { return allowed; }
        public String getReason() { return reason; }

        public static ClaimResult allow() { return new ClaimResult(true, ""); }
        public static ClaimResult deny(String reason) { return new ClaimResult(false, reason); }
    }

    /**
     * Check if a player has permission to execute admin commands.
     * Requires OP level as configured in config.toml.
     */
    public static boolean hasAdminPermission(ServerPlayer player) {
        if (!ConfigHandler.REQUIRE_OP_FOR_ADMIN.get()) {
            return true;
        }
        return player.hasPermissions(ConfigHandler.OP_LEVEL_REQUIRED.get());
    }

    /**
     * Validate a kit claim request. Performs all server-side checks:
     * 1. Kit exists
     * 2. Player hasn't already claimed it
     * 3. Player belongs to the correct LuckPerms group
     * 4. Player has enough inventory space
     *
     * @param player   The player attempting to claim
     * @param kitName  The name of the kit to claim
     * @return ClaimResult indicating if the claim is allowed
     */
    public static ClaimResult validateClaim(ServerPlayer player, String kitName) {
        FantasticKits mod = FantasticKits.getInstance();
        KitData kitData = mod.getKitData();
        PlayerData playerData = mod.getPlayerData();
        LuckPermsIntegration luckPerms = mod.getLuckPermsIntegration();

        UUID playerUUID = player.getUUID();

        // 1. Check kit exists
        KitDefinition kit = kitData.getKit(kitName);
        if (kit == null) {
            return ClaimResult.deny("Kit does not exist: " + kitName);
        }

        // 2. Check if already claimed (once per player, forever)
        if (playerData.hasClaimed(playerUUID, kitName)) {
            return ClaimResult.deny("Kit already claimed");
        }

        // 3. Check LuckPerms group
        String requiredGroup = kit.getAssignedGroup();
        if (requiredGroup != null && !requiredGroup.isEmpty()) {
            if (!luckPerms.isAvailable()) {
                return ClaimResult.deny("LuckPerms is not available, cannot verify group");
            }
            if (!luckPerms.playerInGroup(playerUUID, requiredGroup)) {
                return ClaimResult.deny("Player does not belong to required group: " + requiredGroup);
            }
        }

        // 4. Check inventory space (basic check)
        int itemCount = kit.getItemNbtList().size();
        int emptySlots = countEmptySlots(player);
        if (emptySlots < itemCount) {
            return ClaimResult.deny("Not enough inventory space (need " + itemCount + " slots, have " + emptySlots + ")");
        }

        return ClaimResult.allow();
    }

    /**
     * Validate that a GUI action originated from a player who actually has the menu open.
     * This prevents packet spoofing where a player sends menu interactions without having
     * the menu open.
     */
    public static boolean validateMenuInteraction(ServerPlayer player, int containerId) {
        return player.containerMenu != null && player.containerMenu.containerId == containerId;
    }

    /**
     * Validate item stack before placing into kit (prevent invalid/exploit items).
     */
    public static boolean isValidKitItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        // Ensure count is within valid range
        if (stack.getCount() <= 0 || stack.getCount() > stack.getMaxStackSize()) {
            return false;
        }
        return true;
    }

    /**
     * Count empty inventory slots for a player (main inventory only, excluding armor/offhand).
     */
    private static int countEmptySlots(ServerPlayer player) {
        int empty = 0;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if (player.getInventory().items.get(i).isEmpty()) {
                empty++;
            }
        }
        return empty;
    }
}
