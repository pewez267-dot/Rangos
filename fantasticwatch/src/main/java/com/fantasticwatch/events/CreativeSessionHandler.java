package com.fantasticwatch.events;

import com.fantasticwatch.FantasticWatch;
import com.fantasticwatch.logging.WatchLogger;
import com.fantasticwatch.tracking.ItemTracker;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Detects operators entering and leaving creative mode and bookends each creative session in the
 * operator's log. Only players with permission level 4 (full operator) are tracked.
 */
@Mod.EventBusSubscriber(modid = FantasticWatch.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CreativeSessionHandler {

    private CreativeSessionHandler() {
    }

    private record Session(long startMillis, AtomicInteger itemsSpawned) {
    }

    private record PendingGameModeChange(String operatorName, long issuedAtMillis) {
    }

    private static final ConcurrentHashMap<UUID, Session> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, PendingGameModeChange> PENDING_EXTERNAL = new ConcurrentHashMap<>();
    private static final long CORRELATION_WINDOW_MILLIS = 2000L;

    /** Called by {@link ItemTracker} whenever an item is spawned, to maintain the session counter. */
    public static void incrementSpawned(UUID opUuid) {
        Session session = ACTIVE_SESSIONS.get(opUuid);
        if (session != null) {
            session.itemsSpawned().incrementAndGet();
        }
    }

    /** @return {@code true} if the operator currently has an active tracked creative session. */
    public static boolean hasActiveSession(UUID opUuid) {
        return ACTIVE_SESSIONS.containsKey(opUuid);
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        ParseResults<CommandSourceStack> results = event.getParseResults();
        CommandSourceStack source = results.getContext().getSource();
        String raw = results.getReader().getString();
        detectExternalGameModeChange(source, raw);
    }

    private static void detectExternalGameModeChange(CommandSourceStack source, String raw) {
        String[] tokens = raw.trim().split("\\s+");
        if (tokens.length < 3 || !tokens[0].equalsIgnoreCase("gamemode")) {
            return;
        }
        MinecraftServer server = source.getServer();
        if (server == null) {
            return;
        }
        ServerPlayer target = server.getPlayerList().getPlayerByName(tokens[2]);
        if (target == null) {
            return;
        }
        Entity executor = source.getEntity();
        String operatorName;
        if (executor instanceof ServerPlayer opPlayer) {
            if (opPlayer.getUUID().equals(target.getUUID())) {
                return;
            }
            operatorName = opPlayer.getGameProfile().getName();
        } else {
            operatorName = "Console";
        }
        PENDING_EXTERNAL.put(target.getUUID(),
                new PendingGameModeChange(operatorName, System.currentTimeMillis()));
    }

    @SubscribeEvent
    public static void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        GameType from = event.getCurrentGameMode();
        GameType to = event.getNewGameMode();
        UUID uuid = player.getUUID();

        if (to == GameType.CREATIVE && from != GameType.CREATIVE) {
            // Only operators are tracked.
            if (!ItemTracker.isOp(player)) {
                return;
            }
            ACTIVE_SESSIONS.put(uuid, new Session(System.currentTimeMillis(), new AtomicInteger(0)));
            String triggeredBy = resolveTrigger(uuid);
            String name = player.getGameProfile().getName();
            String payload = "uuid=" + uuid
                    + " by=" + triggeredBy
                    + " @(" + ItemTracker.pos(player) + ") " + ItemTracker.dimShort(player.level());
            WatchLogger.get().record(uuid, name, "SESSION_CREATIVE_START", payload);
        } else if (from == GameType.CREATIVE && to != GameType.CREATIVE) {
            ItemSpawnHandler.clearStackableBaseline(uuid);
            Session session = ACTIVE_SESSIONS.remove(uuid);
            if (session == null) {
                return; // was not a tracked creative session
            }
            long durationSeconds = Math.max(0L, (System.currentTimeMillis() - session.startMillis()) / 1000L);
            String name = player.getGameProfile().getName();
            String payload = "dur=" + durationSeconds + "s"
                    + " spawned=" + session.itemsSpawned().get();
            WatchLogger.get().record(uuid, name, "SESSION_CREATIVE_END", payload);
        }
    }

    private static String resolveTrigger(UUID playerUuid) {
        PendingGameModeChange pending = PENDING_EXTERNAL.remove(playerUuid);
        if (pending != null && (System.currentTimeMillis() - pending.issuedAtMillis()) <= CORRELATION_WINDOW_MILLIS) {
            return "op_externo:" + pending.operatorName();
        }
        return "self";
    }
}
