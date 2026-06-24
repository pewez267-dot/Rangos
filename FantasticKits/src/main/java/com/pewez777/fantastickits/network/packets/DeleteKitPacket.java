/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.network.packets;

import java.util.Optional;
import java.util.function.Supplier;

import com.pewez777.fantastickits.Reference;
import com.pewez777.fantastickits.config.FantasticKitsConfig;
import com.pewez777.fantastickits.kits.Kit;
import com.pewez777.fantastickits.kits.KitManager;
import com.pewez777.fantastickits.luckperms.LuckPermsHook;
import com.pewez777.fantastickits.security.AuditAction;
import com.pewez777.fantastickits.security.AuditLogger;
import com.pewez777.fantastickits.security.NetworkAddressUtil;
import com.pewez777.fantastickits.security.SecurityEventLogger;
import com.pewez777.fantastickits.security.SecurityEventType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client -&gt; Server confirmed deletion request.
 *
 * <p>Re-validated server-side: requires operator permission, removes the kit
 * and all of its records/associations, revokes the published LuckPerms nodes
 * and purges every per-player claim record for the kit.</p>
 */
public final class DeleteKitPacket {

    private final String kitName;

    public DeleteKitPacket(String kitName) {
        this.kitName = kitName == null ? "" : kitName;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(kitName);
    }

    public static DeleteKitPacket decode(FriendlyByteBuf buf) {
        return new DeleteKitPacket(buf.readUtf());
    }

    public static void handle(DeleteKitPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> process(msg, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void process(DeleteKitPacket msg, ServerPlayer sender) {
        if (sender == null) {
            return;
        }
        final String name = sender.getGameProfile().getName();
        final String ip = NetworkAddressUtil.getIp(sender);

        if (!sender.hasPermissions(Reference.ADMIN_PERMISSION_LEVEL)) {
            SecurityEventLogger.log(SecurityEventType.FORGED_CLIENT_ACTION, sender.getUUID(), name,
                    "-", "-", msg.kitName, "delete-kit", "BLOCKED",
                    "Non-operator attempted to delete a kit");
            return;
        }

        Optional<Kit> existing = KitManager.get().getByName(msg.kitName);
        if (existing.isEmpty()) {
            sender.sendSystemMessage(Component.literal(
                            Reference.CHAT_PREFIX + "Kit '" + msg.kitName + "' does not exist.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        Kit kit = existing.get();
        Optional<Kit> removed = KitManager.get().delete(kit.getName());
        if (removed.isEmpty()) {
            sender.sendSystemMessage(Component.literal(
                            Reference.CHAT_PREFIX + "Failed to delete the kit.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // Revoke LuckPerms nodes and purge per-player claim records.
        if (FantasticKitsConfig.PUBLISH_LUCKPERMS_NODES.get() && LuckPermsHook.isAvailable()) {
            LuckPermsHook.revokeKitNodes(kit.getOwnerGroup(), kit.getId());
        }
        int purged = KitManager.get().players().purgeKitClaims(kit.getId());

        AuditLogger.log(AuditAction.DELETE_KIT, sender.getUUID(), name, ip, kit.getName(),
                kit.getOwnerGroup(), "SUCCESS", "Kit deleted; purged " + purged + " claim record(s)");

        sender.sendSystemMessage(Component.literal(
                        Reference.CHAT_PREFIX + "Kit '" + kit.getName() + "' deleted.")
                .withStyle(ChatFormatting.GREEN));
    }
}
