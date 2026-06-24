package com.fantasticaudit.events;

import com.fantasticaudit.FantasticAudit;
import com.fantasticaudit.config.AuditConfig;
import com.fantasticaudit.logging.AuditLogger;
import com.fantasticaudit.util.ItemSerializer;
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

/**
 * Captures the COMMANDS &amp; GAMEMODE category.
 *
 * <p>Commands are logged verbatim. Gamemode changes are correlated with any preceding
 * {@code /gamemode &lt;mode&gt; &lt;target&gt;} command so the audit can distinguish a player changing
 * their own mode ({@code self}) from an operator changing someone else's ({@code op:{name}}).</p>
 */
@Mod.EventBusSubscriber(modid = FantasticAudit.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommandEventHandler {

    private CommandEventHandler() {
    }

    /** A pending "an operator changed this player's gamemode" record, with the time it was issued. */
    private record PendingGameModeChange(String operatorName, long issuedAtMillis) {
    }

    /** Maps a target player's UUID to the operator who just issued a gamemode change against them. */
    private static final ConcurrentHashMap<UUID, PendingGameModeChange> PENDING_EXTERNAL = new ConcurrentHashMap<>();

    /** A correlation is only honoured if the gamemode change happens within this window. */
    private static final long CORRELATION_WINDOW_MILLIS = 2000L;

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (!AuditConfig.LOG_COMMANDS.get()) {
            return;
        }
        ParseResults<CommandSourceStack> results = event.getParseResults();
        CommandSourceStack source = results.getContext().getSource();
        String raw = results.getReader().getString();

        // Detect operator-driven gamemode changes against another player so the gamemode handler
        // can attribute them correctly. Runs regardless of who issued the command.
        detectExternalGameModeChange(source, raw);

        Entity entity = source.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            // Console / command-block / function sources have no per-player log file.
            return;
        }

        String data = "/" + raw
                + " @(" + ItemSerializer.pos(player) + ") "
                + ItemSerializer.dimShort(player.level());

        AuditLogger.get().record(player.getUUID(), player.getGameProfile().getName(), "COMMAND", data);
    }

    private static void detectExternalGameModeChange(CommandSourceStack source, String raw) {
        String[] tokens = raw.trim().split("\\s+");
        if (tokens.length < 3) {
            return;
        }
        if (!tokens[0].equalsIgnoreCase("gamemode")) {
            return;
        }
        // tokens: [gamemode, <mode>, <target...>]
        String targetSelector = tokens[2];
        MinecraftServer server = source.getServer();
        if (server == null) {
            return;
        }
        ServerPlayer target = server.getPlayerList().getPlayerByName(targetSelector);
        if (target == null) {
            return;
        }
        Entity executor = source.getEntity();
        String operatorName;
        if (executor instanceof ServerPlayer opPlayer) {
            if (opPlayer.getUUID().equals(target.getUUID())) {
                // Self-target: not an external change.
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
        if (!AuditConfig.LOG_COMMANDS.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        GameType from = event.getCurrentGameMode();
        GameType to = event.getNewGameMode();

        String triggeredBy = resolveTrigger(player.getUUID());

        String data = from.getName() + " -> " + to.getName()
                + " by=" + triggeredBy
                + " @(" + ItemSerializer.pos(player) + ")";

        AuditLogger.get().record(player.getUUID(), player.getGameProfile().getName(), "GAMEMODE_CHANGE", data);
    }

    private static String resolveTrigger(UUID playerUuid) {
        PendingGameModeChange pending = PENDING_EXTERNAL.remove(playerUuid);
        if (pending != null && (System.currentTimeMillis() - pending.issuedAtMillis()) <= CORRELATION_WINDOW_MILLIS) {
            return "op:" + pending.operatorName();
        }
        return "self";
    }
}
