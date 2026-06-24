package com.fantasticterraform.network;

import com.fantasticterraform.intelligent.dungeon.DungeonSizeRequirement;
import com.fantasticterraform.intelligent.dungeon.DungeonSizeValidator;
import com.fantasticterraform.intelligent.dungeon.DungeonTier;
import com.fantasticterraform.selection.SelectionManager;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C->S: pide validar si la seleccion cumple el tier elegido antes de generar. */
public final class ValidateDungeonSelectionPacket {

    private final int tier;

    public ValidateDungeonSelectionPacket(int tier) {
        this.tier = tier;
    }

    public static void encode(ValidateDungeonSelectionPacket m, FriendlyByteBuf buf) {
        buf.writeInt(m.tier);
    }

    public static ValidateDungeonSelectionPacket decode(FriendlyByteBuf buf) {
        return new ValidateDungeonSelectionPacket(buf.readInt());
    }

    public static void handle(ValidateDungeonSelectionPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            DungeonTier[] tiers = DungeonTier.values();
            DungeonTier tier = m.tier >= 0 && m.tier < tiers.length ? tiers[m.tier] : DungeonTier.SMALL;
            SelectionShape sel = SelectionManager.get(player).getShape();
            DungeonSizeValidator.Result result = DungeonSizeValidator.validate(sel, DungeonSizeRequirement.forTier(tier));
            PacketHandler.sendToClient(player, new DungeonSelectionValidationResultPacket(result.ok, result.message));
        });
        c.setPacketHandled(true);
    }
}
