package com.pewez.fantasticessentials.storage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * A serializable world location (dimension + position + rotation).
 */
public class Location {

    public String dimension;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;

    public Location() {
    }

    public Location(String dimension, double x, double y, double z, float yaw, float pitch) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static Location of(ServerPlayer player) {
        return new Location(
                player.level().dimension().location().toString(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()
        );
    }

    public ServerLevel level(MinecraftServer server) {
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        if (id == null) {
            return server.overworld();
        }
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
        ServerLevel level = server.getLevel(key);
        return level != null ? level : server.overworld();
    }

    /**
     * Teleport a player to this location. Returns false if the dimension no longer exists.
     */
    public boolean teleport(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        if (id == null) {
            return false;
        }
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
        ServerLevel target = server.getLevel(key);
        if (target == null) {
            return false;
        }
        player.teleportTo(target, x, y, z, yaw, pitch);
        return true;
    }
}
