package com.fantastic.kits.kits;

import com.fantastic.kits.FantasticKits;
import com.fantastic.kits.audit.AuditEventType;
import com.fantastic.kits.audit.SecurityEventType;
import com.fantastic.kits.security.SecurityValidator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.time.Instant;
import java.util.Locale;

/**
 * Centralised kit delivery pipeline.
 * <p>
 * Used by both {@code /fkits get} (real claim) and {@code /fkits test} (admin
 * preview). The {@code persist} flag controls whether the claim is recorded
 * permanently or simulated.
 */
public final class KitClaimService {

    /** Outcome enum so callers can produce coherent feedback. */
    public enum Outcome {
        SUCCESS,
        ALREADY_CLAIMED,
        WRONG_GROUP,
        UNSAFE_CONTEXT,
        INVENTORY_FULL,
        RATE_LIMITED,
        FAKE_PLAYER,
        INTERNAL_ERROR
    }

    private KitClaimService() {}

    /**
     * Real claim. Validates everything, delivers items, runs commands and
     * records the player's claim permanently.
     */
    public static Outcome claim(ServerPlayer player, Kit kit) {
        return run(player, kit, true);
    }

    /**
     * Admin test claim. Runs the validation pipeline AND delivers the items but
     * does NOT register the claim, does NOT consume the use, and does NOT alter
     * statistics.
     */
    public static Outcome testClaim(ServerPlayer admin, Kit kit) {
        if (admin == null || kit == null) return Outcome.INTERNAL_ERROR;
        if (!SecurityValidator.isSafeContext(admin)) return Outcome.UNSAFE_CONTEXT;
        if (kit.security().blockOnFullInventory && !SecurityValidator.hasInventoryRoom(admin, kit)) {
            return Outcome.INVENTORY_FULL;
        }
        deliverItems(admin, kit);
        runKitCommands(admin, kit, /*test=*/true);
        FantasticKits.audit().log(AuditEventType.TEST_KIT, admin, kit, "SUCCESS",
                "Test delivery (no claim recorded).");
        return Outcome.SUCCESS;
    }

    private static Outcome run(ServerPlayer player, Kit kit, boolean persist) {
        if (player == null || kit == null) return Outcome.INTERNAL_ERROR;

        // 1. Anti-bot / fake player.
        if (!SecurityValidator.isHumanPlayer(player)) return Outcome.FAKE_PLAYER;

        // 2. Per-player cooldown (anti macro / double click).
        int cooldown = Math.max(0, FantasticKits.config().claimCooldownMillis);
        if (!FantasticKits.antiExploit().tryConsume(player.getUUID(), "claim:" + kit.id(), cooldown)) {
            FantasticKits.security().log(SecurityEventType.REPEATED_REQUEST_SPAM,
                    player, FantasticKits.luckPerms().primaryGroup(player.getUUID()),
                    kit.ownerGroup(), kit, "CLAIM",
                    "BLOCKED", "Cooldown still active.");
            return Outcome.RATE_LIMITED;
        }

        // 3. Strict group match.
        if (!SecurityValidator.canClaimByGroup(player, kit)) {
            if (persist) {
                FantasticKits.players().recordDenied(player.getUUID(),
                        player.getGameProfile().getName(), kit.id(), Instant.now().toEpochMilli());
                FantasticKits.audit().log(AuditEventType.CLAIM_DENIED, player, kit, "DENIED",
                        "Strict group mismatch.");
            }
            return Outcome.WRONG_GROUP;
        }

        // 4. Context.
        if (kit.security().blockUnsafeContexts && !SecurityValidator.isSafeContext(player)) {
            FantasticKits.security().log(SecurityEventType.INVALID_GUI_INTERACTION,
                    player, FantasticKits.luckPerms().primaryGroup(player.getUUID()),
                    kit.ownerGroup(), kit, "CLAIM",
                    "BLOCKED", "Unsafe context (dead/spectator).");
            return Outcome.UNSAFE_CONTEXT;
        }

        // 5. Single permanent claim.
        if (persist && FantasticKits.players().hasClaimed(player.getUUID(), kit.id())) {
            FantasticKits.security().log(SecurityEventType.DUPLICATE_CLAIM_ATTEMPT,
                    player, FantasticKits.luckPerms().primaryGroup(player.getUUID()),
                    kit.ownerGroup(), kit, "CLAIM",
                    "BLOCKED", "Player already claimed this kit.");
            FantasticKits.audit().log(AuditEventType.CLAIM_DENIED, player, kit, "DENIED",
                    "Already claimed.");
            return Outcome.ALREADY_CLAIMED;
        }

        // 6. Inventory room.
        if (kit.security().blockOnFullInventory && !SecurityValidator.hasInventoryRoom(player, kit)) {
            return Outcome.INVENTORY_FULL;
        }

        // 7. Deliver.
        deliverItems(player, kit);
        runKitCommands(player, kit, /*test=*/false);

        if (persist) {
            boolean recorded = FantasticKits.players().recordClaim(player.getUUID(),
                    player.getGameProfile().getName(), kit.id(), Instant.now().toEpochMilli());
            if (!recorded) {
                FantasticKits.security().log(SecurityEventType.DUPLICATE_CLAIM_ATTEMPT,
                        player, FantasticKits.luckPerms().primaryGroup(player.getUUID()),
                        kit.ownerGroup(), kit, "CLAIM",
                        "BLOCKED", "Race condition during recordClaim().");
            }
            FantasticKits.audit().log(AuditEventType.CLAIM_KIT, player, kit, "SUCCESS",
                    "Kit delivered.");
        }
        return Outcome.SUCCESS;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void deliverItems(ServerPlayer player, Kit kit) {
        for (ItemStack template : kit.contents()) {
            if (template == null || template.isEmpty()) continue;
            ItemStack copy = template.copy();
            if (!player.getInventory().add(copy)) {
                ItemEntity drop = player.drop(copy, false);
                if (drop != null) {
                    drop.setNoPickUpDelay();
                    drop.setTarget(player.getUUID());
                    drop.setOwner(player.getUUID());
                }
            }
        }
        player.containerMenu.broadcastChanges();
    }

    /**
     * Executes the kit-bound commands as the server (operator level), but only
     * after re-validating the primary group at run time. This ensures that
     * commands granted via a kit cannot be "carried over" by a player who
     * loses the rank between the claim and the execution.
     */
    private static void runKitCommands(ServerPlayer player, Kit kit, boolean test) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        Commands dispatcher = server.getCommands();
        CommandSourceStack source = server.createCommandSourceStack()
                .withEntity(player)
                .withPosition(player.position())
                .withRotation(player.getRotationVector())
                .withLevel(player.serverLevel())
                .withPermission(2)
                .withSuppressedOutput();

        for (String cmd : kit.commands()) {
            String resolved = cmd
                    .replace("{player}", player.getGameProfile().getName())
                    .replace("{uuid}", player.getUUID().toString())
                    .replace("{kit}", kit.id());

            if (!test && !SecurityValidator.canClaimByGroup(player, kit)) {
                FantasticKits.security().log(SecurityEventType.COMMAND_ACCESS_DENIED,
                        player, FantasticKits.luckPerms().primaryGroup(player.getUUID()),
                        kit.ownerGroup(), kit, "/" + resolved,
                        "BLOCKED", "Group changed mid-claim - command refused.");
                return;
            }
            try {
                dispatcher.performPrefixedCommand(source, resolved);
            } catch (Throwable t) {
                FantasticKits.LOGGER.error("Failed to execute kit command '/{}'", resolved, t);
            }
        }
    }

    public static Component outcomeMessage(Outcome o, Kit kit) {
        String name = kit == null ? "?" : kit.displayName();
        return switch (o) {
            case SUCCESS -> Component.literal(FantasticKits.config().chatPrefix +
                    "\u00A7aHas reclamado el kit \u00A7e" + name + "\u00A7a.");
            case ALREADY_CLAIMED -> Component.literal(FantasticKits.config().chatPrefix +
                    "\u00A7cYa has reclamado este kit anteriormente.");
            case WRONG_GROUP -> Component.literal(FantasticKits.config().chatPrefix +
                    "\u00A7cTu grupo primario no permite reclamar este kit.");
            case UNSAFE_CONTEXT -> Component.literal(FantasticKits.config().chatPrefix +
                    "\u00A7cNo puedes reclamar el kit en este estado.");
            case INVENTORY_FULL -> Component.literal(FantasticKits.config().chatPrefix +
                    "\u00A7cTu inventario no tiene suficiente espacio.");
            case RATE_LIMITED -> Component.literal(FantasticKits.config().chatPrefix +
                    "\u00A7cEspera unos segundos antes de intentarlo de nuevo.");
            case FAKE_PLAYER -> Component.literal(FantasticKits.config().chatPrefix +
                    "\u00A7cAccion no permitida.");
            case INTERNAL_ERROR -> Component.literal(FantasticKits.config().chatPrefix +
                    "\u00A7cError interno. Revisa la consola.");
        };
    }

    /** Lower-cased, slash-stripped variant - used by the runtime command gate. */
    public static String normalizeCommandKey(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("/")) t = t.substring(1);
        int sp = t.indexOf(' ');
        if (sp > 0) t = t.substring(0, sp);
        return t.toLowerCase(Locale.ROOT);
    }
}
