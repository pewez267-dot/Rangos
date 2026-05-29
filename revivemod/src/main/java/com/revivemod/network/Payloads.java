package com.revivemod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Custom client -> server payloads used by the optional client component. */
public final class Payloads {

    public static final CustomPayload.Id<SurrenderToggle> SURRENDER_ID =
            new CustomPayload.Id<>(Identifier.of("revivemod", "surrender_toggle"));
    public static final CustomPayload.Id<SelfReviveToggle> SELF_ID =
            new CustomPayload.Id<>(Identifier.of("revivemod", "self_toggle"));

    public record SurrenderToggle() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, SurrenderToggle> CODEC = PacketCodec.unit(new SurrenderToggle());
        @Override public Id<? extends CustomPayload> getId() { return SURRENDER_ID; }
    }

    public record SelfReviveToggle() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, SelfReviveToggle> CODEC = PacketCodec.unit(new SelfReviveToggle());
        @Override public Id<? extends CustomPayload> getId() { return SELF_ID; }
    }

    private Payloads() {}
}
