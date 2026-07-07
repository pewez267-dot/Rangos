/*
 * Decompiled with CFR 0.152.
 */
package com.fantasticpass.network;

import com.fantasticpass.capability.PassCapability;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PassSavedData;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.quest.QuestManager;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public final class SavePassPacket {
    private final PassDefinition pass;

    public SavePassPacket(PassDefinition pass) {
        this.pass = pass;
    }

    public static void encode(SavePassPacket packet, FriendlyByteBuf buf) {
        packet.pass.toBuf(buf);
    }

    public static SavePassPacket decode(FriendlyByteBuf buf) {
        return new SavePassPacket(PassDefinition.fromBuf(buf));
    }

    public static void handle(SavePassPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || !sender.hasPermissions(4)) {
                return;
            }
            String id = packet.pass.getId();
            if (id == null || id.isEmpty()) {
                sender.sendSystemMessage((Component)Component.translatable((String)"fantasticpass.msg.pass_id_required").withStyle(ChatFormatting.RED));
                return;
            }
            MinecraftServer server = sender.getServer();
            if (server == null) {
                return;
            }
            PassSavedData saved = PassSavedData.get(server);
            saved.putPass(packet.pass);
            sender.sendSystemMessage((Component)Component.translatable((String)"fantasticpass.msg.pass_saved", (Object[])new Object[]{id}).withStyle(ChatFormatting.GREEN));
            String activeId = saved.getActivePassId();
            if (activeId == null || activeId.isEmpty()) {
                saved.setActivePassId(id);
                sender.sendSystemMessage((Component)Component.translatable((String)"fantasticpass.msg.pass_activated", (Object[])new Object[]{id}).withStyle(ChatFormatting.GOLD));
            } else if (!activeId.equals(id)) {
                sender.sendSystemMessage((Component)Component.translatable((String)"fantasticpass.msg.pass_saved_not_active", (Object[])new Object[]{id}).withStyle(ChatFormatting.YELLOW));
            }
            PassDefinition active = saved.getActivePass();
            if (active != null && id.equals(active.getId())) {
                PlayerPassData data = PassCapability.getData((Player)sender);
                if (data != null) {
                    QuestManager.ensureDaily(sender.getUUID(), data);
                    PacketHandler.sendToPlayer(sender, (Object)new OpenViewScreenPacket(active, data, QuestManager.pointsPerTier(active)));
                }
            }
        });
        context.setPacketHandled(true);
    }
}

