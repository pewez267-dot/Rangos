package com.fantasticchest.security;

import com.fantasticchest.block.ChestBlockEntity;
import com.fantasticchest.config.ChestConfig;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Central server-side security checks. Every value here is derived from the authenticated
 * {@link ServerPlayer} and the world — never from packet payloads.
 */
public final class PermissionValidator {

    private PermissionValidator() {
    }

    public static boolean isOp(final ServerPlayer player) {
        return player.hasPermissions(4);
    }

    /** True if the player is within the configured interaction distance of {@code pos}. */
    public static boolean withinDistance(final ServerPlayer player, final BlockPos pos) {
        if (pos == null) {
            return false;
        }
        final double max = ChestConfig.maxInteractionDistance();
        final double dx = player.getX() - (pos.getX() + 0.5);
        final double dy = player.getY() - (pos.getY() + 0.5);
        final double dz = player.getZ() - (pos.getZ() + 0.5);
        return (dx * dx + dy * dy + dz * dz) <= (max * max);
    }

    /**
     * Resolves the chest BlockEntity at {@code pos} for {@code player}, validating that the
     * block exists, is a {@link ChestBlockEntity} and is within interaction distance.
     * Returns {@code null} (interaction rejected) otherwise.
     */
    public static ChestBlockEntity resolve(final ServerPlayer player, final BlockPos pos) {
        if (pos == null || !withinDistance(player, pos)) {
            return null;
        }
        final BlockEntity be = player.level().getBlockEntity(pos);
        return be instanceof ChestBlockEntity chest ? chest : null;
    }

    /** Resolves a player reference (UUID string or username) to a UUID, or {@code null}. */
    public static UUID resolveUuid(final MinecraftServer server, final String reference) {
        if (server == null || reference == null || reference.isBlank()) {
            return null;
        }
        final String trimmed = reference.trim();
        try {
            return UUID.fromString(trimmed);
        } catch (final IllegalArgumentException ignored) {
            // Not a UUID — look the username up in the server profile cache.
            final Optional<GameProfile> profile = server.getProfileCache() == null
                    ? Optional.empty()
                    : server.getProfileCache().get(trimmed);
            return profile.map(GameProfile::getId).orElse(null);
        }
    }
}
