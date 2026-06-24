package com.fscrates.crate;

import com.fscrates.FSCrates;
import com.fscrates.config.CrateConfig;
import com.fscrates.config.RewardEntry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Server-side timer that hands out a crate's rewards AFTER the client animation
 * has played, so the item the player receives is the very item the roulette
 * lands on — never before the reel even starts.
 */
@Mod.EventBusSubscriber(modid = FSCrates.MOD_ID)
public final class DelayedDelivery {

    private DelayedDelivery() {}

    private record Task(UUID player, ServerLevel level, long dueTick,
                        CrateConfig crate, List<RewardEntry> rewards) {}

    private static final List<Task> TASKS = new ArrayList<>();

    public static void schedule(ServerPlayer player, CrateConfig crate,
                                List<RewardEntry> rewards, int delayTicks) {
        ServerLevel level = player.serverLevel();
        long due = level.getGameTime() + Math.max(1, delayTicks);
        TASKS.add(new Task(player.getUUID(), level, due, crate, rewards));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TASKS.isEmpty()) {
            return;
        }
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.level.getGameTime() < t.dueTick) {
                continue;
            }
            it.remove();
            ServerPlayer player = t.level.getServer() == null ? null
                    : t.level.getServer().getPlayerList().getPlayer(t.player);
            if (player != null) {
                LootEngine.deliver(player, t.crate, t.rewards);
            }
        }
    }
}
