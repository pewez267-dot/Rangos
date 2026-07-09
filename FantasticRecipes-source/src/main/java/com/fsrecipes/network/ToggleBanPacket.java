package com.fsrecipes.network;

import com.fsrecipes.RecipeBans;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.function.Supplier;

/** Cliente -> servidor: banea/desbanea un item. El servidor valida que sea OP. */
public class ToggleBanPacket {

    private final ResourceLocation itemId;
    private final boolean ban;

    public ToggleBanPacket(ResourceLocation itemId, boolean ban) {
        this.itemId = itemId;
        this.ban = ban;
    }

    public static void encode(ToggleBanPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.itemId);
        buf.writeBoolean(msg.ban);
    }

    public static ToggleBanPacket decode(FriendlyByteBuf buf) {
        return new ToggleBanPacket(buf.readResourceLocation(), buf.readBoolean());
    }

    public static void handle(ToggleBanPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp == null || !sp.hasPermissions(2)) {
                return;
            }
            RecipeBans.setBanned(sp.server, msg.itemId, msg.ban);
            Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new SyncBansPacket(new HashSet<>(RecipeBans.banned())));
        });
        c.setPacketHandled(true);
    }
}
