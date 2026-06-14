package me.drex.essentials.util;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.function.Predicate;

/**
 * Permission handling for the Forge port.
 *
 * The original Fabric mod used the fabric-permissions-api. On Forge we rely on vanilla
 * operator permission levels, which work on any server and are honoured by permission mods
 * that adjust a player's command permission level. The required level for each node is
 * configurable through the config file.
 */
public final class Permissions {

    private Permissions() {
    }

    public static boolean check(CommandSourceStack source, String node, int defaultLevel) {
        int level = me.drex.essentials.config.Config.get().permissionLevel(node, defaultLevel);
        if (level <= 0) {
            return true;
        }
        return source.hasPermission(level);
    }

    public static Predicate<CommandSourceStack> require(String node, int defaultLevel) {
        return source -> check(source, node, defaultLevel);
    }

    public static <T extends ArgumentBuilder<CommandSourceStack, T>> T requires(T builder, String node, int defaultLevel) {
        return builder.requires(require(node, defaultLevel));
    }
}
