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
import com.pewez777.fantastickits.kits.Kit;
import com.pewez777.fantastickits.kits.KitManager;
import com.pewez777.fantastickits.kits.KitService;
import com.pewez777.fantastickits.security.SecurityEventLogger;
import com.pewez777.fantastickits.security.SecurityEventType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client -&gt; Server administrative test request. Delivers a kit temporarily
 * with no claim recorded. Requires operator permission, re-validated here.
 */
public final class TestKitPacket {

    private final String kitName;

    public TestKitPacket(String kitName) {
        this.kitName = kitName == null ? "" : kitName;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(kitName);
    }

    public static TestKitPacket decode(FriendlyByteBuf buf) {
        return new TestKitPacket(buf.readUtf());
    }

    public static void handle(TestKitPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> process(msg, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void process(TestKitPacket msg, ServerPlayer sender) {
        if (sender == null) {
            return;
        }
        if (!sender.hasPermissions(Reference.ADMIN_PERMISSION_LEVEL)) {
            SecurityEventLogger.log(SecurityEventType.FORGED_CLIENT_ACTION, sender.getUUID(),
                    sender.getGameProfile().getName(), "-", "-", msg.kitName, "test-kit",
                    "BLOCKED", "Non-operator attempted to test a kit");
            return;
        }

        Optional<Kit> kit = KitManager.get().getByName(msg.kitName);
        if (kit.isEmpty()) {
            sender.sendSystemMessage(Component.literal(
                            Reference.CHAT_PREFIX + "Kit '" + msg.kitName + "' does not exist.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        KitService.test(sender, kit.get());
        sender.sendSystemMessage(Component.literal(
                        Reference.CHAT_PREFIX + "Test delivery of '" + kit.get().getName()
                                + "' completed (no claim recorded).")
                .withStyle(ChatFormatting.GREEN));
    }
}
