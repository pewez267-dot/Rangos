package com.fantastic.kits.network;

import com.fantastic.kits.FantasticKits;
import com.fantastic.kits.Reference;
import com.fantastic.kits.audit.SecurityEventType;
import com.fantastic.kits.kits.Kit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Client -> Server. Sent after the user confirms a deletion in the GUI. The
 * server validates operator level + kit existence, then delegates to
 * {@link com.fantastic.kits.kits.KitManager#delete(Kit, String)} which also
 * revokes the corresponding LuckPerms nodes.
 */
public class DeleteKitPacket {

    private final String kitId;

    public DeleteKitPacket(String kitId) {
        this.kitId = kitId == null ? "" : kitId;
    }

    public static void encode(DeleteKitPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.kitId, 96);
    }

    public static DeleteKitPacket decode(FriendlyByteBuf buf) {
        return new DeleteKitPacket(buf.readUtf(96));
    }

    public static void handle(DeleteKitPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;
            if (!sender.hasPermissions(Reference.OP_LEVEL)) {
                FantasticKits.security().log(SecurityEventType.FORGED_CLIENT_ACTION,
                        sender, "?", "?", null, "DELETE_KIT", "BLOCKED",
                        "Non-operator attempted to delete a kit.");
                return;
            }
            Optional<Kit> kit = FantasticKits.kits().byId(msg.kitId);
            if (kit.isEmpty()) {
                sender.sendSystemMessage(Component.literal(FantasticKits.config().chatPrefix
                        + "\u00A7cEl kit '" + msg.kitId + "' no existe."));
                return;
            }
            FantasticKits.kits().delete(kit.get(), "Confirmed via GUI by " + sender.getGameProfile().getName());
            sender.sendSystemMessage(Component.literal(FantasticKits.config().chatPrefix
                    + "\u00A7aKit \u00A7e" + kit.get().displayName() + "\u00A7a eliminado."));
        });
        context.setPacketHandled(true);
    }
}
