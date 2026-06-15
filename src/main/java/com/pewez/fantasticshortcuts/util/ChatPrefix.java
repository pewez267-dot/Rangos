package com.pewez.fantasticshortcuts.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Builds the {@code [F-Shortcuts]} chat prefix and helper messages.
 */
public final class ChatPrefix {

    private ChatPrefix() {
    }

    public static MutableComponent prefix() {
        return Component.empty()
                .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("F-Shortcuts").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static MutableComponent info(String text) {
        return prefix().append(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent success(String text) {
        return prefix().append(Component.literal(text).withStyle(ChatFormatting.GREEN));
    }

    public static MutableComponent error(String text) {
        return prefix().append(Component.literal(text).withStyle(ChatFormatting.RED));
    }
}
