package com.pewez.fantasticessentials.command;

import com.pewez.fantasticessentials.config.Config;
import com.pewez.fantasticessentials.storage.DataStorage;
import com.pewez.fantasticessentials.storage.Location;
import com.pewez.fantasticessentials.storage.PlayerData;
import com.pewez.fantasticessentials.text.Messages;
import com.pewez.fantasticessentials.util.TeleportManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared helpers for command implementations.
 */
public final class CommandHelper {

    private CommandHelper() {
    }

    /** Store the player's current location as the /back destination. */
    public static void saveBack(ServerPlayer player) {
        PlayerData data = DataStorage.playerData(player);
        data.lastLocation = Location.of(player);
        DataStorage.savePlayerData(player.getUUID());
    }

    /**
     * Run a teleport, saving the back location first and honouring the configured warmup.
     */
    public static void teleport(ServerPlayer player, Location target, Component success) {
        TeleportManager.scheduleTeleport(player, () -> {
            saveBack(player);
            if (target.teleport(player)) {
                if (success != null) {
                    player.sendSystemMessage(success);
                }
            } else {
                player.sendSystemMessage(Messages.prefixed("teleport.failed",
                        "&cTeleport failed: the target dimension is not loaded."));
            }
        });
    }

    /** Check + announce a cooldown. Returns true if the player must wait. */
    public static boolean onCooldown(ServerPlayer player, String key, int seconds) {
        int remaining = TeleportManager.remainingCooldown(player, key);
        if (remaining > 0) {
            player.sendSystemMessage(Messages.prefixed("cooldown.active",
                    "&cYou must wait &e{seconds}&c seconds before using this again.",
                    Messages.of("seconds", String.valueOf(remaining))));
            return true;
        }
        if (seconds > 0) {
            TeleportManager.setCooldown(player, key, seconds);
        }
        return false;
    }

    public static int homeLimit(ServerPlayer player) {
        if (player.hasPermissions(Config.get().homeLimitBypassLevel)) {
            return Integer.MAX_VALUE;
        }
        return Config.get().defaultHomeLimit;
    }
}
