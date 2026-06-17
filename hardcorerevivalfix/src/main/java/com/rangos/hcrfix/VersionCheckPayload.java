package com.rangos.hcrfix;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Empty S2C "version probe" payload. The mere fact that a client has registered
 * a receiver for this ID is what proves it has a compatible version of this mod
 * installed. Modeled after BlockPops' VersionCheckPacket.
 */
public record VersionCheckPayload() implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("hardcorerevivalfix", "version_check_v1");

    public static final Type<VersionCheckPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, VersionCheckPayload> CODEC =
            StreamCodec.unit(new VersionCheckPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
