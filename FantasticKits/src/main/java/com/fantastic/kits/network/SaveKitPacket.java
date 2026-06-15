package com.fantastic.kits.network;

import com.fantastic.kits.FantasticKits;
import com.fantastic.kits.Reference;
import com.fantastic.kits.audit.AuditEventType;
import com.fantastic.kits.audit.SecurityEventType;
import com.fantastic.kits.kits.Kit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client -> Server. Carries the full edited kit back to the server. The server
 * always re-validates: only operators can mutate kits, the source NBT is
 * deserialised through {@link Kit#load(CompoundTag)} (so any forged tags fall
 * outside the schema and are dropped), and the resulting kit replaces the
 * existing entry atomically through {@link com.fantastic.kits.kits.KitManager}.
 */
public class SaveKitPacket {

    private final CompoundTag kit;

    public SaveKitPacket(CompoundTag kit) {
        this.kit = kit == null ? new CompoundTag() : kit;
    }

    public static void encode(SaveKitPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.kit);
    }

    public static SaveKitPacket decode(FriendlyByteBuf buf) {
        return new SaveKitPacket(buf.readNbt());
    }

    public static void handle(SaveKitPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;

            // Hard server-side gate: only operators can save kits, regardless of
            // what the client claims.
            if (!sender.hasPermissions(Reference.OP_LEVEL)) {
                FantasticKits.security().log(SecurityEventType.FORGED_CLIENT_ACTION,
                        sender, "?", "?", null, "SAVE_KIT", "BLOCKED",
                        "Non-operator attempted to save a kit.");
                return;
            }
            if (msg.kit == null || msg.kit.isEmpty()) {
                FantasticKits.security().log(SecurityEventType.INVALID_PACKET,
                        sender, "?", "?", null, "SAVE_KIT", "BLOCKED",
                        "Empty kit payload.");
                return;
            }

            Kit incoming;
            try {
                incoming = Kit.load(msg.kit);
            } catch (Throwable t) {
                FantasticKits.security().log(SecurityEventType.KIT_DATA_TAMPERING,
                        sender, "?", "?", null, "SAVE_KIT", "BLOCKED",
                        "Malformed NBT payload: " + t.getMessage());
                return;
            }

            UUID uuid = incoming.uuid();
            Kit existing = FantasticKits.kits().byUuid(uuid).orElse(null);
            if (existing == null) {
                // First save - register as a new kit (id may already be live).
                FantasticKits.kits().registerNew(incoming);
                FantasticKits.audit().log(AuditEventType.CREATE_KIT, sender, incoming, "SUCCESS",
                        "Kit registered via editor.");
            } else {
                FantasticKits.kits().replace(existing, incoming);
                FantasticKits.audit().log(AuditEventType.EDIT_KIT, sender, incoming, "SUCCESS",
                        "Kit updated via editor.");
            }
            FantasticKits.luckPerms().syncKitToGroup(incoming.ownerGroup(), incoming.id(), incoming.commands());

            sender.sendSystemMessage(Component.literal(FantasticKits.config().chatPrefix
                    + "\u00A7aKit \u00A7e" + incoming.displayName() + "\u00A7a guardado."));
        });
        context.setPacketHandled(true);
    }
}
