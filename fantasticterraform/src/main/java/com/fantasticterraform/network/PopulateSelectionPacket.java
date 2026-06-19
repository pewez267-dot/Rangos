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

/** C->S: poblamiento inteligente (arboles/rocas/vegetacion/cristales) en la seleccion. */
public final class PopulateSelectionPacket {

    private final boolean trees;
    private final boolean rocks;
    private final boolean vegetation;
    private final boolean crystals;
    private final long seed;

    public PopulateSelectionPacket(boolean trees, boolean rocks, boolean vegetation, boolean crystals, long seed) {
        this.trees = trees;
        this.rocks = rocks;
        this.vegetation = vegetation;
        this.crystals = crystals;
        this.seed = seed;
    }

    public static void encode(PopulateSelectionPacket m, FriendlyByteBuf buf) {
        buf.writeBoolean(m.trees);
        buf.writeBoolean(m.rocks);
        buf.writeBoolean(m.vegetation);
        buf.writeBoolean(m.crystals);
        buf.writeLong(m.seed);
    }

    public static PopulateSelectionPacket decode(FriendlyByteBuf buf) {
        return new PopulateSelectionPacket(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readLong());
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
            PopulationManager.populate(player, (ServerLevel) player.level(), sel, effective,
                    m.trees, m.rocks, m.vegetation, m.crystals);
        });
        c.setPacketHandled(true);
    }
}
