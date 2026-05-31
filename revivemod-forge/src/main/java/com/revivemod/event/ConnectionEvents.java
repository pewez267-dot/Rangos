package com.revivemod.event;

import com.revivemod.state.DownManager;
import com.revivemod.state.DownState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Keeps the downed state consistent across login, logout, dimension change and respawn/clone.
 * Replaces Fabric's ServerPlayConnectionEvents / ServerEntityWorldChangeEvents / ServerPlayerEvents.
 */
public final class ConnectionEvents {

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (DownManager.isDown(player)) {
            DownState st = DownManager.get(player);
            if (st != null) {
                st.downDimension = player.serverLevel().dimension();
                st.downPosition = player.position();
            }
            DownManager.reattach(player);
            player.displayClientMessage(Component.literal("Sigues desangr\u00e1ndote. Otros jugadores pueden revivirte.")
                    .withStyle(ChatFormatting.RED), false);
        } else {
            DownManager.clearDownEffects(player);
            DownManager.clearProne(player);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DownState st = DownManager.get(player);
        if (st != null) {
            st.bossBar.removePlayer(player);
        }
    }

    @SubscribeEvent
    public void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DownState st = DownManager.get(player);
        if (st == null) {
            return;
        }
        st.downDimension = player.serverLevel().dimension();
        st.downPosition = player.position();
        st.bossBar.removePlayer(player);
        st.bossBar.addPlayer(player);
        DownManager.applyDownEffects(player);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.isEndConquered()) {
            if (DownManager.isDown(player)) {
                DownManager.reattach(player);
            }
            return;
        }
        DownManager.removeWithoutRevival(player.getUUID());
        DownManager.clearDownEffects(player);
        DownManager.clearProne(player);
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
            return;
        }
        if (DownManager.isDown(oldPlayer)) {
            DownManager.applyDownEffects(newPlayer);
        }
    }
}
