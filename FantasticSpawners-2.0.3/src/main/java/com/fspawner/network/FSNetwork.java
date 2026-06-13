// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.network;

import java.util.function.Predicate;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Function;
import java.util.function.BiConsumer;
import net.minecraftforge.network.simple.SimpleChannel;

public final class FSNetwork
{
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL;
    
    private FSNetwork() {
    }
    
    public static void register() {
        int id = 0;
        FSNetwork.CHANNEL.registerMessage(id++, OpenScreenPacket.class, OpenScreenPacket::encode, OpenScreenPacket::decode, OpenScreenPacket::handle);
        FSNetwork.CHANNEL.registerMessage(id++, SaveConfigPacket.class, SaveConfigPacket::encode, SaveConfigPacket::decode, SaveConfigPacket::handle);
    }
    
    public static void sendToClient(final ServerPlayer player, final Object packet) {
        FSNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    public static void sendToServer(final Object packet) {
        FSNetwork.CHANNEL.sendToServer(packet);
    }
    
    static {
        CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation("fspawner", "main")).networkProtocolVersion(() -> "1").clientAcceptedVersions((Predicate)"1"::equals).serverAcceptedVersions((Predicate)"1"::equals).simpleChannel();
    }
}
