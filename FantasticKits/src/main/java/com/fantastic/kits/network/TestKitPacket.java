package com.fantastic.kits.network;

import com.fantastic.kits.FantasticKits;
import com.fantastic.kits.Reference;
import com.fantastic.kits.audit.SecurityEventType;
import com.fantastic.kits.kits.Kit;
import com.fantastic.kits.kits.KitClaimService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Client -> Server. Sent when an operator clicks a kit while the list screen
 * is in TEST mode. Delivers the kit through {@link KitClaimService#testClaim}
 * (without persisting a claim record).
 */
public class TestKitPacket {

    private final String kitId;

    public TestKitPacket(String kitId) {
        this.kitId = kitId == null ? "" : kitId;
    }

    public static void encode(TestKitPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.kitId, 96);
    }

    public static TestKitPacket decode(FriendlyByteBuf buf) {
        return new TestKitPacket(buf.readUtf(96));
    }

    public static void handle(TestKitPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;
            if (!sender.hasPermissions(Reference.OP_LEVEL)) {
                FantasticKits.security().log(SecurityEventType.FORGED_CLIENT_ACTION,
                        sender, "?", "?", null, "TEST_KIT", "BLOCKED",
                        "Non-operator attempted a test claim.");
                return;
            }
            Optional<Kit> kit = FantasticKits.kits().byId(msg.kitId);
            if (kit.isEmpty()) {
                sender.sendSystemMessage(Component.literal(FantasticKits.config().chatPrefix
                        + "\u00A7cEl kit '" + msg.kitId + "' no existe."));
                return;
            }
            KitClaimService.Outcome outcome = KitClaimService.testClaim(sender, kit.get());
            sender.sendSystemMessage(KitClaimService.outcomeMessage(outcome, kit.get()));
        });
        context.setPacketHandled(true);
    }
}
