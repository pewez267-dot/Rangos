// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.crate;

import java.util.UUID;
import java.util.ArrayList;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.Iterator;
import net.minecraftforge.event.TickEvent;
import net.minecraft.server.level.ServerLevel;
import com.fscrates.config.RewardEntry;
import com.fscrates.config.CrateConfig;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "fscrates")
public final class DelayedDelivery
{
    private static final List<Task> TASKS;
    
    private DelayedDelivery() {
    }
    
    public static void schedule(final ServerPlayer player, final CrateConfig crate, final List<RewardEntry> rewards, final int delayTicks) {
        final ServerLevel level = player.serverLevel();
        final long due = level.getGameTime() + Math.max(1, delayTicks);
        DelayedDelivery.TASKS.add(new Task(player.getUUID(), level, due, crate, rewards));
    }
    
    @SubscribeEvent
    public static void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || DelayedDelivery.TASKS.isEmpty()) {
            return;
        }
        final Iterator<Task> it = DelayedDelivery.TASKS.iterator();
        while (it.hasNext()) {
            final Task t = it.next();
            if (t.level.getGameTime() < t.dueTick) {
                continue;
            }
            it.remove();
            final ServerPlayer player = (t.level.getServer() == null) ? null : t.level.getServer().getPlayerList().getPlayer(t.player);
            if (player == null) {
                continue;
            }
            LootEngine.deliver(player, t.crate, t.rewards);
        }
    }
    
    static {
        TASKS = new ArrayList<Task>();
    }
    
    record Task(UUID player, ServerLevel level, long dueTick, CrateConfig crate, List<RewardEntry> rewards) {}
}
