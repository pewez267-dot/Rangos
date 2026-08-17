package com.revivemod.event;

import com.revivemod.state.DownManager;
import com.revivemod.state.DownState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ConnectionEvents {
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player2 = (ServerPlayer)player;
        if (DownManager.isDown(player2)) {
            DownState st = DownManager.get(player2);
            if (st != null) {
                st.downDimension = player2.serverLevel().dimension();
                st.downPosition = player2.position();
            }
            DownManager.reattach(player2);
            player2.displayClientMessage((Component)Component.literal((String)"Sigues desangr\u00e1ndote. Otros jugadores pueden revivirte.").withStyle(ChatFormatting.RED), false);
        } else {
            DownManager.clearDownEffects(player2);
            DownManager.clearProne(player2);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player2 = (ServerPlayer)player;
        DownState st = DownManager.get(player2);
        if (st != null) {
            st.bossBar.removePlayer(player2);
        }
    }

    @SubscribeEvent
    public void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player2 = (ServerPlayer)player;
        DownState st = DownManager.get(player2);
        if (st == null) {
            return;
        }
        st.downDimension = player2.serverLevel().dimension();
        st.downPosition = player2.position();
        st.bossBar.removePlayer(player2);
        st.bossBar.addPlayer(player2);
        DownManager.applyDownEffects(player2);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player2 = (ServerPlayer)player;
        if (event.isEndConquered()) {
            if (DownManager.isDown(player2)) {
                DownManager.reattach(player2);
            }
            return;
        }
        DownManager.removeWithoutRevival(player2.getUUID());
        DownManager.clearDownEffects(player2);
        DownManager.clearProne(player2);
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer newPlayer = (ServerPlayer)player;
        Player player2 = event.getOriginal();
        if (!(player2 instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer oldPlayer = (ServerPlayer)player2;
        if (DownManager.isDown(oldPlayer)) {
            DownManager.applyDownEffects(newPlayer);
        }
    }
}

