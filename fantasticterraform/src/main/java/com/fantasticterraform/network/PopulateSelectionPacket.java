package com.fantasticterraform.network;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.intelligent.population.PopulationManager;
import com.fantasticterraform.selection.SelectionManager;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C->S: poblamiento inteligente por categorias (mascara de bits) en la seleccion. */
public final class PopulateSelectionPacket {

    private final int categories;
    private final long seed;

    public PopulateSelectionPacket(int categories, long seed) {
        this.categories = categories;
        this.seed = seed;
    }

    public static void encode(PopulateSelectionPacket m, FriendlyByteBuf buf) {
        buf.writeInt(m.categories);
        buf.writeLong(m.seed);
    }

    public static PopulateSelectionPacket decode(FriendlyByteBuf buf) {
        return new PopulateSelectionPacket(buf.readInt(), buf.readLong());
    }

    public static void handle(PopulateSelectionPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            SelectionShape sel = SelectionManager.get(player).getShape();
            if (sel == null) {
                player.sendSystemMessage(Component.literal("\u00a7cNecesitas una seleccion valida para poblar."));
                return;
            }
            long effective = IntelligentSeeds.resolve(m.seed, TerraformConfig.GENERAL.intelligentSeed.get(), player);
            PopulationManager.populate(player, (ServerLevel) player.level(), sel, effective, m.categories);
        });
        c.setPacketHandled(true);
    }
}
