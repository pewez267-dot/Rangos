package com.revivemod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Custom payloads between the optional client component and the server. */
public final class Payloads {

    // Client -> server: the downed player wants to surrender / self-revive.
    public static final CustomPayload.Id<SurrenderToggle> SURRENDER_ID =
            new CustomPayload.Id<>(Identifier.of("revivemod", "surrender_toggle"));
    public static final CustomPayload.Id<SelfReviveToggle> SELF_ID =
            new CustomPayload.Id<>(Identifier.of("revivemod", "self_toggle"));

    // Server -> client: you entered / left the downed (crawling) state.
    public static final CustomPayload.Id<DownStart> DOWN_START_ID =
            new CustomPayload.Id<>(Identifier.of("revivemod", "down_start"));
    public static final CustomPayload.Id<DownEnd> DOWN_END_ID =
            new CustomPayload.Id<>(Identifier.of("revivemod", "down_end"));

    public record SurrenderToggle() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, SurrenderToggle> CODEC = PacketCodec.unit(new SurrenderToggle());
        @Override public Id<? extends CustomPayload> getId() { return SURRENDER_ID; }
    }

    public record SelfReviveToggle() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, SelfReviveToggle> CODEC = PacketCodec.unit(new SelfReviveToggle());
        @Override public Id<? extends CustomPayload> getId() { return SELF_ID; }
    }

    public record DownStart() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, DownStart> CODEC = PacketCodec.unit(new DownStart());
        @Override public Id<? extends CustomPayload> getId() { return DOWN_START_ID; }
    }

    public record DownEnd() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, DownEnd> CODEC = PacketCodec.unit(new DownEnd());
        @Override public Id<? extends CustomPayload> getId() { return DOWN_END_ID; }
    }

    private Payloads() {}
}
