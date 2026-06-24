/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.commandsystem;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.mojang.brigadier.ParseResults;
import com.pewez777.fantastickits.Reference;
import com.pewez777.fantastickits.config.FantasticKitsConfig;
import com.pewez777.fantastickits.kits.Kit;
import com.pewez777.fantastickits.kits.KitManager;
import com.pewez777.fantastickits.kits.KitService;
import com.pewez777.fantastickits.luckperms.LuckPermsHook;
import com.pewez777.fantastickits.security.SecurityEventLogger;
import com.pewez777.fantastickits.security.SecurityEventType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Runtime barrier that gates kit-owned commands behind exact primary-group
 * matching.
 *
 * <p>If a kit owns {@code /feed} and {@code /repair}, only players whose primary
 * group exactly matches that kit's owner group may run them - regardless of
 * inherited permissions, weights or hierarchy. Server operators are exempt so
 * they can always administer the server. Every block is recorded as a security
 * event and the action is cancelled cleanly (never crashing).</p>
 */
public final class CommandBarrier {

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (!FantasticKitsConfig.ENFORCE_COMMAND_BARRIER.get()) {
            return;
        }

        ParseResults<CommandSourceStack> results = event.getParseResults();
        CommandSourceStack source = results.getContext().getSource();

        // Only players are gated; console and command blocks are unaffected.
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // Operators administer the server and bypass the barrier.
        if (source.hasPermission(Reference.ADMIN_PERMISSION_LEVEL)) {
            return;
        }

        String literal = firstLiteral(results.getReader().getString());
        if (literal.isEmpty()) {
            return;
        }

        List<Kit> owningKits = KitManager.get().kitsOwningCommand(literal);
        if (owningKits.isEmpty()) {
            return; // command is not owned by any kit - nothing to enforce
        }

        final UUID id = player.getUUID();
        boolean allowed = false;
        String requiredGroups = "";
        for (Kit kit : owningKits) {
            if (!requiredGroups.isEmpty()) {
                requiredGroups += ", ";
            }
            requiredGroups += kit.getOwnerGroup();
            if (KitService.primaryGroupMatchesExactly(id, kit.getOwnerGroup())) {
                allowed = true;
                break;
            }
        }

        if (allowed) {
            return;
        }

        // Block: cancel, notify, and record the security event.
        event.setCanceled(true);

        String detected = LuckPermsHook.getPrimaryGroup(id).orElse("(none)");
        player.sendSystemMessage(Component.literal(
                        Reference.CHAT_PREFIX + "This command is reserved for another rank.")
                .withStyle(ChatFormatting.RED));

        SecurityEventLogger.log(SecurityEventType.COMMAND_ACCESS_DENIED, id,
                player.getGameProfile().getName(), detected, requiredGroups,
                "(command:/" + literal + ")", "/" + literal,
                "BLOCKED", "Primary group does not match any kit owning this command");
    }

    private static String firstLiteral(String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int space = trimmed.indexOf(' ');
        if (space > 0) {
            trimmed = trimmed.substring(0, space);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
