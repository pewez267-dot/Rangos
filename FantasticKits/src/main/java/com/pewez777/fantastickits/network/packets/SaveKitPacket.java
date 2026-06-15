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
import com.pewez777.fantastickits.items.ItemEditorService;
import com.pewez777.fantastickits.kits.Kit;
import com.pewez777.fantastickits.kits.KitManager;
import com.pewez777.fantastickits.luckperms.LuckPermsHook;
import com.pewez777.fantastickits.security.AuditAction;
import com.pewez777.fantastickits.security.AuditLogger;
import com.pewez777.fantastickits.security.NetworkAddressUtil;
import com.pewez777.fantastickits.security.SecurityEventLogger;
import com.pewez777.fantastickits.security.SecurityEventType;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client -&gt; Server packet carrying a fully serialized kit to persist.
 *
 * <p>The server NEVER trusts this payload: it re-checks operator permission,
 * sanitizes the NBT (item count, name, owner group), preserves immutable fields
 * on edit, persists, and republishes the LuckPerms relationship. Tampering is
 * recorded as a security event and rejected.</p>
 */
public final class SaveKitPacket {

    private final CompoundTag kitTag;

    public SaveKitPacket(CompoundTag kitTag) {
        this.kitTag = kitTag == null ? new CompoundTag() : kitTag;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(kitTag);
    }

    public static SaveKitPacket decode(FriendlyByteBuf buf) {
        return new SaveKitPacket(buf.readNbt());
    }

    public static void handle(SaveKitPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> process(msg, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void process(SaveKitPacket msg, ServerPlayer sender) {
        if (sender == null) {
            return;
        }
        final String name = sender.getGameProfile().getName();
        final String ip = NetworkAddressUtil.getIp(sender);

        // 1) Authorization: must be an operator.
        if (!sender.hasPermissions(Reference.ADMIN_PERMISSION_LEVEL)) {
            SecurityEventLogger.log(SecurityEventType.FORGED_CLIENT_ACTION, sender.getUUID(), name,
                    "-", "-", "-", "save-kit", "BLOCKED", "Non-operator attempted to save a kit");
            deny(sender);
            return;
        }

        // 2) Parse + sanitize.
        Kit incoming;
        try {
            incoming = Kit.fromNbt(msg.kitTag);
        } catch (Throwable t) {
            SecurityEventLogger.log(SecurityEventType.KIT_DATA_TAMPERING, sender.getUUID(), name,
                    "-", "-", "-", "save-kit", "BLOCKED", "Malformed kit NBT");
            deny(sender);
            return;
        }

        if (incoming.getName().isBlank()) {
            SecurityEventLogger.log(SecurityEventType.KIT_DATA_TAMPERING, sender.getUUID(), name,
                    "-", "-", "-", "save-kit", "BLOCKED", "Kit name was blank");
            deny(sender);
            return;
        }

        if (incoming.getItems().size() > ItemEditorService.MAX_ITEMS) {
            SecurityEventLogger.log(SecurityEventType.NBT_MANIPULATION_ATTEMPT, sender.getUUID(), name,
                    "-", incoming.getOwnerGroup(), incoming.getName(), "save-kit",
                    "TRIMMED", "Item count exceeded maximum; list trimmed");
            incoming.setItems(ItemEditorService.sanitized(incoming.getItems())
                    .subList(0, ItemEditorService.MAX_ITEMS));
        } else {
            incoming.setItems(ItemEditorService.sanitized(incoming.getItems()));
        }

        // 3) Preserve immutable fields on edit; detect changes for auditing.
        Optional<Kit> existingOpt = KitManager.get().getByName(incoming.getName());
        boolean isNew = existingOpt.isEmpty();
        String previousGroup = null;
        if (existingOpt.isPresent()) {
            Kit existing = existingOpt.get();
            incoming.setId(existing.getId());
            incoming.setCreatedAt(existing.getCreatedAt());
            previousGroup = existing.getOwnerGroup();

            if (!existing.getOwnerGroup().equalsIgnoreCase(incoming.getOwnerGroup())) {
                AuditLogger.log(AuditAction.CHANGE_GROUP, sender.getUUID(), name, ip,
                        incoming.getName(), incoming.getOwnerGroup(), "SUCCESS",
                        "Group changed from '" + existing.getOwnerGroup() + "'");
            }
            if (!existing.getCommands().equals(incoming.getCommands())) {
                AuditLogger.log(AuditAction.CHANGE_COMMANDS, sender.getUUID(), name, ip,
                        incoming.getName(), incoming.getOwnerGroup(), "SUCCESS",
                        "Commands updated (" + incoming.getCommands().size() + " total)");
            }
            AuditLogger.log(AuditAction.CHANGE_NBT, sender.getUUID(), name, ip,
                    incoming.getName(), incoming.getOwnerGroup(), "SUCCESS",
                    "Item/NBT contents updated (" + incoming.getItems().size() + " items)");
        }

        // 4) Persist.
        boolean saved = KitManager.get().save(incoming);
        if (!saved) {
            sender.sendSystemMessage(Component.literal(
                            Reference.CHAT_PREFIX + "Failed to save the kit.")
                    .withStyle(ChatFormatting.RED));
            AuditLogger.log(isNew ? AuditAction.CREATE_KIT : AuditAction.EDIT_KIT,
                    sender.getUUID(), name, ip, incoming.getName(), incoming.getOwnerGroup(),
                    "FAILURE", "Disk write failed");
            return;
        }

        // 5) Republish LuckPerms relationship (group -> kit -> commands).
        if (FantasticKitsConfig.PUBLISH_LUCKPERMS_NODES.get() && LuckPermsHook.isAvailable()) {
            if (previousGroup != null && !previousGroup.equalsIgnoreCase(incoming.getOwnerGroup())) {
                LuckPermsHook.revokeKitNodes(previousGroup, incoming.getId());
            }
            LuckPermsHook.publishKitNodes(incoming.getOwnerGroup(), incoming.getId(),
                    incoming.getCommands());
        }

        AuditLogger.log(isNew ? AuditAction.CREATE_KIT : AuditAction.EDIT_KIT,
                sender.getUUID(), name, ip, incoming.getName(), incoming.getOwnerGroup(),
                "SUCCESS", isNew ? "Kit created" : "Kit edited");

        sender.sendSystemMessage(Component.literal(
                        Reference.CHAT_PREFIX + "Kit '" + incoming.getName() + "' saved.")
                .withStyle(ChatFormatting.GREEN));
    }

    private static void deny(ServerPlayer sender) {
        sender.sendSystemMessage(Component.literal(
                        Reference.CHAT_PREFIX + "Your kit save request was rejected by the server.")
                .withStyle(ChatFormatting.RED));
    }
}
