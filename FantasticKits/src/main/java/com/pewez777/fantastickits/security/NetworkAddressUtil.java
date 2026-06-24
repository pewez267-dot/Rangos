/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.security;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;

/**
 * Safely resolves a player's remote IP address for audit/security logging.
 *
 * <p>The {@link Connection} reference is located reflectively so the helper is
 * resilient to mapping field-visibility differences, and the whole operation is
 * guarded so it can never throw - it simply returns {@code null} when the
 * address cannot be determined.</p>
 */
public final class NetworkAddressUtil {

    private NetworkAddressUtil() {
    }

    public static String getIp(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return null;
        }
        try {
            Connection connection = extractConnection(player.connection);
            if (connection == null) {
                return null;
            }
            SocketAddress address = connection.getRemoteAddress();
            if (address instanceof InetSocketAddress inet) {
                return inet.getAddress() != null
                        ? inet.getAddress().getHostAddress()
                        : inet.getHostString();
            }
            return address == null ? null : address.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static Connection extractConnection(Object listener) throws IllegalAccessException {
        for (Class<?> clazz = listener.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Connection.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(listener);
                    if (value instanceof Connection connection) {
                        return connection;
                    }
                }
            }
        }
        return null;
    }
}
