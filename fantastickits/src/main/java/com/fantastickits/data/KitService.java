package com.fantastickits.data;

import com.fantastickits.config.FKConfig;
import com.fantastickits.integration.LuckPermsIntegration;
import com.fantastickits.security.AuditLog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Server-side business logic for delivering, claiming and testing kits.
 *
 * <p>This is the single authoritative place where claim rules are enforced. The order
 * of operations in {@link #claim(ServerPlayer, String)} matters: membership is checked
 * first, then the claim is recorded <em>atomically</em> via
 * {@link PlayerClaimStore#tryClaim}, and only then are items delivered. Recording the
 * claim before delivery is what makes duplicate/spoofed claim attempts impossible to
 * exploit for item duplication.</p>
 */
public final class KitService {

    private KitService() {
    }

    /** Adds detached copies of the kit's items to the player, dropping any overflow. */
    public static void deliver(final ServerPlayer player, final Kit kit) {
        for (final ItemStack stack : kit.items) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            final ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
        }
    }

    /**
     * Full player-initiated claim flow: validates kit existence, group membership and
     * the one-claim-per-UUID rule, then delivers the items. Every outcome is audited and
     * the player is informed.
     */
    public static void claim(final ServerPlayer player, final String rawKitId) {
        final String kitId = Kit.normalizeId(rawKitId);
        final UUID uuid = player.getUUID();
        final String name = player.getGameProfile().getName();

        final Kit kit = KitRegistry.get().get(kitId);
        if (kit == null) {
            AuditLog.claimDenied(uuid, name, kitId, AuditLog.REASON_NO_KIT);
            player.sendSystemMessage(Component.literal("§cEl kit §e" + kitId + " §cno existe."));
            return;
        }

        // Group gate.
        if (!kit.hasGroup()) {
            if (!FKConfig.allowClaimWithoutGroup()) {
                AuditLog.claimDenied(uuid, name, kitId, AuditLog.REASON_NO_GROUP_ASSIGNED);
                player.sendSystemMessage(Component.literal("§cEste kit no tiene un grupo asignado, por lo que no puede reclamarse."));
                return;
            }
        } else if (!LuckPermsIntegration.isMemberOf(uuid, kit.group)) {
            AuditLog.claimDenied(uuid, name, kitId, AuditLog.REASON_WRONG_GROUP);
            player.sendSystemMessage(Component.literal("§cNo perteneces al grupo §e" + kit.group + " §crequerido para reclamar este kit."));
            return;
        }

        // Atomic one-time claim: this is the duplication / race-condition guard.
        if (!PlayerClaimStore.get().tryClaim(uuid, name, kitId)) {
            AuditLog.claimDenied(uuid, name, kitId, AuditLog.REASON_ALREADY_CLAIMED);
            player.sendSystemMessage(Component.literal("§cYa reclamaste este kit. Solo se puede reclamar una vez."));
            return;
        }

        deliver(player, kit);
        AuditLog.claimSuccess(uuid, name, kitId);

        final String display = (kit.displayName == null || kit.displayName.isBlank()) ? kitId : kit.displayName;
        player.sendSystemMessage(Component.literal("§aReclamaste el kit §r" + display + "§a."));
        if (FKConfig.broadcastOnClaim() && player.getServer() != null) {
            player.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("§d\u2726 §f" + name + " §7reclamo el kit §r" + display), false);
        }
    }

    /**
     * Administrative delivery / restock to {@code target}. Bypasses the group check and
     * the one-time claim rule (and does not consume the player's claim record), exactly
     * as required for manual replacement of lost items. The acting source may be a player
     * or the server console.
     */
    public static boolean adminGet(final CommandSourceStack source, final ServerPlayer target, final String rawKitId) {
        final String kitId = Kit.normalizeId(rawKitId);
        final Kit kit = KitRegistry.get().get(kitId);
        if (kit == null) {
            source.sendSystemMessage(Component.literal("§cEl kit §e" + kitId + " §cno existe."));
            return false;
        }
        deliver(target, kit);

        final ServerPlayer adminPlayer = source.getPlayer();
        final UUID adminUuid = adminPlayer != null ? adminPlayer.getUUID() : new UUID(0L, 0L);
        AuditLog.adminGet(adminUuid, source.getTextName(), target.getGameProfile().getName(), kitId);

        final String display = (kit.displayName == null || kit.displayName.isBlank()) ? kitId : kit.displayName;
        source.sendSystemMessage(Component.literal("§aEntregaste el kit §r" + display + " §aa §e" + target.getGameProfile().getName() + "§a."));
        target.sendSystemMessage(Component.literal("§aUn administrador te entrego el kit §r" + display + "§a."));
        return true;
    }

    /**
     * Test delivery to the executing admin. Items are delivered but no claim is recorded
     * and no group check is performed, so the kit can be inspected freely.
     */
    public static boolean test(final ServerPlayer admin, final String rawKitId) {
        final String kitId = Kit.normalizeId(rawKitId);
        final Kit kit = KitRegistry.get().get(kitId);
        if (kit == null) {
            admin.sendSystemMessage(Component.literal("§cEl kit §e" + kitId + " §cno existe."));
            return false;
        }
        deliver(admin, kit);
        AuditLog.kitTested(admin.getUUID(), admin.getGameProfile().getName(), kitId);

        final String groupInfo = kit.hasGroup() ? ("§7grupo: §e" + kit.group) : "§7sin grupo asignado";
        admin.sendSystemMessage(Component.literal("§aPrueba del kit §e" + kitId + " §a(" + kit.items.size() + " items, " + groupInfo + "§a). No se registro ningun reclamo."));
        return true;
    }
}
