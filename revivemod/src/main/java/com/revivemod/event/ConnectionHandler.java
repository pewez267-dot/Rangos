package com.revivemod.event;

import com.revivemod.state.DownManager;
import com.revivemod.state.DownState;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Keeps the downed state consistent across logout/login, dimension change,
 * respawn, and any external teleports (like /tpa from another mod).
 */
public final class ConnectionHandler {

    private ConnectionHandler() {}

    public static void register() {
        // Re-attach bossbar + reapply effects when a player joins.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (DownManager.isDown(player)) {
                // Player logged back in while still downed. Reapply everything.
                DownState st = DownManager.get(player);
                if (st != null) {
                    st.downDimension = player.getServerWorld().getRegistryKey();
                    st.downPosition = player.getPos();
                }
                DownManager.reattach(player);
                player.sendMessage(Text.literal("Sigues desangrándote. Otros jugadores pueden revivirte.")
                        .formatted(Formatting.RED), false);
            } else {
                // Defensive: a server restart while a player was downed leaves
                // our infinite effects baked into their saved data with no down
                // state. Strip them and make sure they stand up / aren't flagged.
                DownManager.clearDownEffects(player);
                DownManager.clearProne(player);
            }
        });

        // When the player disconnects, keep the state but detach the bossbar
        // (it'll be re-added on next login via JOIN).
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player == null) return;
            DownState st = DownManager.get(player);
            if (st != null) {
                st.bossBar.removePlayer(player);
            }
        });

        // Dimension change: teleport-safe. Update the lock position so we don't
        // try to drag them back across dimensions, and re-add the bossbar.
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            DownState st = DownManager.get(player);
            if (st == null) return;
            st.downDimension = destination.getRegistryKey();
            st.downPosition = player.getPos();
            // Re-attach the bossbar to the new world's player object (it's the same
            // ServerPlayerEntity instance, but be safe).
            st.bossBar.removePlayer(player);
            st.bossBar.addPlayer(player);
            DownManager.applyDownEffects(player);
        });

        // After actual death (timer expired) and respawn: clear any leftovers.
        // The `alive` parameter is true when the player came back from the End
        // via portal (no real death happened); in that case we KEEP the downed
        // state because they didn't actually die.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) {
                // End portal exit while downed -> reattach bossbar & effects on the new player object.
                if (DownManager.isDown(newPlayer)) {
                    DownManager.reattach(newPlayer);
                }
                return;
            }
            // Real death: cleanup any stale state and our effects.
            DownManager.removeWithoutRevival(newPlayer.getUuid());
            DownManager.clearDownEffects(newPlayer);
            DownManager.clearProne(newPlayer);
        });

        // When a player copies data (end portal travel), reapply.
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (DownManager.isDown(oldPlayer)) {
                DownManager.applyDownEffects(newPlayer);
            }
        });
    }
}
