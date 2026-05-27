package com.claimblocks.network;

import com.claimblocks.ClaimBlocksMod;
import com.claimblocks.client.ClaimVisualization;
import com.claimblocks.data.Claim;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Server -> Client sync for "current claim outline".  When a player enters or
 * leaves a claim, the server sends a small packet so the client can draw the
 * 3D outline of the claim they are currently inside.
 *
 * The packet is also used to clear the outline (radius = -1).
 */
public final class ClaimNetworking {
    public static final Identifier CLAIM_SYNC_ID =
        Identifier.of(ClaimBlocksMod.MOD_ID, "claim_sync");

    public record ClaimSyncPayload(int x, int y, int z, int radius, int tier, String ownerName)
        implements CustomPayload {

        public static final CustomPayload.Id<ClaimSyncPayload> ID = new CustomPayload.Id<>(CLAIM_SYNC_ID);
        public static final PacketCodec<PacketByteBuf, ClaimSyncPayload> CODEC =
            PacketCodec.of(
                (value, buf) -> {
                    buf.writeInt(value.x);
                    buf.writeInt(value.y);
                    buf.writeInt(value.z);
                    buf.writeInt(value.radius);
                    buf.writeInt(value.tier);
                    buf.writeString(value.ownerName == null ? "" : value.ownerName);
                },
                buf -> new ClaimSyncPayload(
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readString())
            );

        @Override public Id<? extends CustomPayload> getId() { return ID; }

        public static ClaimSyncPayload clear() {
            return new ClaimSyncPayload(0, 0, 0, -1, 0, "");
        }

        public static ClaimSyncPayload fromClaim(Claim c) {
            return new ClaimSyncPayload(c.getX(), c.getY(), c.getZ(),
                    c.getRadius(), c.getTier(), c.getOwnerName());
        }
    }

    /** Run on both physical sides during ModInitializer.onInitialize. */
    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(ClaimSyncPayload.ID, ClaimSyncPayload.CODEC);
    }

    @Environment(EnvType.CLIENT)
    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(ClaimSyncPayload.ID, (payload, ctx) -> {
            ctx.client().execute(() -> ClaimVisualization.handleSync(payload));
        });
    }

    public static void sendClaimSync(ServerPlayerEntity player, Claim claim) {
        ServerPlayNetworking.send(player,
            claim == null ? ClaimSyncPayload.clear() : ClaimSyncPayload.fromClaim(claim));
    }
}
