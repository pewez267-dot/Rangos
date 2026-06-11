package com.gbaminecraft.minecraft.network;

import com.gbaminecraft.GBAMod;
import com.gbaminecraft.minecraft.tileentity.GBATileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Handles all networking between client GBAScreen and server GBATileEntity.
 *
 * Packet IDs:
 *   0 — C->S: Key press/release
 *   1 — C->S: Stop emulator
 *   2 — C->S: Reset emulator
 *   3 — C->S: Set speed multiplier
 *   4 — C->S: Insert cartridge (ROM bytes)
 *   5 — C->S: Eject cartridge
 */
public class GBANetworkHandler {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GBAMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void registerPackets() {
        int id = 0;
        // 0: Key press
        CHANNEL.registerMessage(id++, KeyPacket.class,
                KeyPacket::encode, KeyPacket::decode, KeyPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        // 1: Stop
        CHANNEL.registerMessage(id++, StopPacket.class,
                StopPacket::encode, StopPacket::decode, StopPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        // 2: Reset
        CHANNEL.registerMessage(id++, ResetPacket.class,
                ResetPacket::encode, ResetPacket::decode, ResetPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        // 3: Speed
        CHANNEL.registerMessage(id++, SpeedPacket.class,
                SpeedPacket::encode, SpeedPacket::decode, SpeedPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    // ── Client-side send helpers ───────────────────────────────────────────
    public static void sendKeyPress(BlockPos pos, int key, boolean pressed) {
        CHANNEL.sendToServer(new KeyPacket(pos, key, pressed));
    }

    public static void sendStopEmulator(BlockPos pos) {
        CHANNEL.sendToServer(new StopPacket(pos));
    }

    public static void sendResetEmulator(BlockPos pos) {
        CHANNEL.sendToServer(new ResetPacket(pos));
    }

    public static void sendSetSpeed(BlockPos pos, double speed) {
        CHANNEL.sendToServer(new SpeedPacket(pos, speed));
    }

    // ── Server helper ─────────────────────────────────────────────────────
    private static GBATileEntity getTE(BlockPos pos, NetworkEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return null;
        ServerLevel level = player.serverLevel();
        BlockEntity be = level.getBlockEntity(pos);
        return (be instanceof GBATileEntity) ? (GBATileEntity) be : null;
    }

    // ── Packet: Key press ─────────────────────────────────────────────────
    public record KeyPacket(BlockPos pos, int key, boolean pressed) {
        static void encode(KeyPacket pkt, FriendlyByteBuf buf) {
            buf.writeBlockPos(pkt.pos); buf.writeInt(pkt.key); buf.writeBoolean(pkt.pressed);
        }
        static KeyPacket decode(FriendlyByteBuf buf) {
            return new KeyPacket(buf.readBlockPos(), buf.readInt(), buf.readBoolean());
        }
        static void handle(KeyPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
            NetworkEvent.Context ctx = ctxSup.get();
            ctx.enqueueWork(() -> {
                GBATileEntity te = getTE(pkt.pos, ctx);
                if (te == null) return;
                if (pkt.pressed) te.pressKey(pkt.key);
                else             te.releaseKey(pkt.key);
            });
            ctx.setPacketHandled(true);
        }
    }

    // ── Packet: Stop ──────────────────────────────────────────────────────
    public record StopPacket(BlockPos pos) {
        static void encode(StopPacket pkt, FriendlyByteBuf buf) { buf.writeBlockPos(pkt.pos); }
        static StopPacket decode(FriendlyByteBuf buf) { return new StopPacket(buf.readBlockPos()); }
        static void handle(StopPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
            NetworkEvent.Context ctx = ctxSup.get();
            ctx.enqueueWork(() -> {
                GBATileEntity te = getTE(pkt.pos, ctx);
                if (te != null) te.stopEmulator();
            });
            ctx.setPacketHandled(true);
        }
    }

    // ── Packet: Reset ─────────────────────────────────────────────────────
    public record ResetPacket(BlockPos pos) {
        static void encode(ResetPacket pkt, FriendlyByteBuf buf) { buf.writeBlockPos(pkt.pos); }
        static ResetPacket decode(FriendlyByteBuf buf) { return new ResetPacket(buf.readBlockPos()); }
        static void handle(ResetPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
            NetworkEvent.Context ctx = ctxSup.get();
            ctx.enqueueWork(() -> {
                GBATileEntity te = getTE(pkt.pos, ctx);
                if (te != null) te.getEmulator().reset();
            });
            ctx.setPacketHandled(true);
        }
    }

    // ── Packet: Speed ─────────────────────────────────────────────────────
    public record SpeedPacket(BlockPos pos, double speed) {
        static void encode(SpeedPacket pkt, FriendlyByteBuf buf) {
            buf.writeBlockPos(pkt.pos); buf.writeDouble(pkt.speed);
        }
        static SpeedPacket decode(FriendlyByteBuf buf) {
            return new SpeedPacket(buf.readBlockPos(), buf.readDouble());
        }
        static void handle(SpeedPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
            NetworkEvent.Context ctx = ctxSup.get();
            ctx.enqueueWork(() -> {
                GBATileEntity te = getTE(pkt.pos, ctx);
                if (te != null) te.getEmulator().setSpeedMultiplier(pkt.speed);
            });
            ctx.setPacketHandled(true);
        }
    }
}
