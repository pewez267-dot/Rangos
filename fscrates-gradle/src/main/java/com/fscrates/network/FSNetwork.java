// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.network;

import java.util.function.Predicate;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Function;
import java.util.function.BiConsumer;
import net.minecraftforge.network.simple.SimpleChannel;

public final class FSNetwork
{
    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL;
    
    private FSNetwork() {
    }
    
    public static void register() {
        int id = 0;
        FSNetwork.CHANNEL.registerMessage(id++, OpenEditorPacket.class, OpenEditorPacket::encode, OpenEditorPacket::decode, OpenEditorPacket::handle);
        FSNetwork.CHANNEL.registerMessage(id++, SaveCratePacket.class, SaveCratePacket::encode, SaveCratePacket::decode, SaveCratePacket::handle);
        FSNetwork.CHANNEL.registerMessage(id++, PlayAnimationPacket.class, PlayAnimationPacket::encode, PlayAnimationPacket::decode, PlayAnimationPacket::handle);
    }
    
    public static void sendToClient(final ServerPlayer player, final Object packet) {
        FSNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    public static void sendToNear(final ServerLevel level, final BlockPos pos, final double radius, final Object packet) {
        FSNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, radius, level.dimension())), packet);
    }
    
    public static void sendToServer(final Object packet) {
        FSNetwork.CHANNEL.sendToServer(packet);
    }
    
    static {
        CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation("fscrates", "main")).networkProtocolVersion(() -> "2").clientAcceptedVersions((Predicate)"2"::equals).serverAcceptedVersions((Predicate)"2"::equals).simpleChannel();
    }
}
