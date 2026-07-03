package com.fscrates.crate;

import com.fscrates.config.CrateConfig;
import com.fscrates.config.RewardEntry;
import com.fscrates.crate.LootEngine;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="fscrates")
public final class DelayedDelivery {
    private static final List<Task> TASKS = new ArrayList<Task>();

    private DelayedDelivery() {
    }

    public static void schedule(ServerPlayer player, CrateConfig crate, List<RewardEntry> rewards, int delayTicks) {
        ServerLevel level = player.serverLevel();
        long due = level.getGameTime() + (long)Math.max(1, delayTicks);
        TASKS.add(new Task(player.getUUID(), level, due, crate, rewards));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !TASKS.isEmpty()) {
            Iterator<Task> it = TASKS.iterator();
            while (it.hasNext()) {
                Task t = it.next();
                if (t.level.getGameTime() < t.dueTick) continue;
                it.remove();
                ServerPlayer player = t.level.getServer() == null ? null : t.level.getServer().getPlayerList().getPlayer(t.player);
                if (player == null) continue;
                LootEngine.deliver(player, t.crate, t.rewards);
            }
        }
    }

    record Task(UUID player, ServerLevel level, long dueTick, CrateConfig crate, List<RewardEntry> rewards) {
    }
}

