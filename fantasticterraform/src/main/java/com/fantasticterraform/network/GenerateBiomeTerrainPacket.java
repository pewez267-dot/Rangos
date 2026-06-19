package com.fantasticterraform.network;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.intelligent.biome.BiomeTerrainGenerator;
import com.fantasticterraform.selection.SelectionManager;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C->S: genera terreno por capas de ruido (biomas) en la seleccion activa. */
public final class GenerateBiomeTerrainPacket {

    private final double contScale;
    private final double eroScale;
    private final double moistScale;
    private final double tempScale;
    private final long seed;

    public GenerateBiomeTerrainPacket(double contScale, double eroScale, double moistScale, double tempScale, long seed) {
        this.contScale = contScale;
        this.eroScale = eroScale;
        this.moistScale = moistScale;
        this.tempScale = tempScale;
        this.seed = seed;
    }

    public static void encode(GenerateBiomeTerrainPacket m, FriendlyByteBuf buf) {
        buf.writeDouble(m.contScale);
        buf.writeDouble(m.eroScale);
        buf.writeDouble(m.moistScale);
        buf.writeDouble(m.tempScale);
        buf.writeLong(m.seed);
    }

    public static GenerateBiomeTerrainPacket decode(FriendlyByteBuf buf) {
        return new GenerateBiomeTerrainPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readLong());
    }

    public static void handle(GenerateBiomeTerrainPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            SelectionShape sel = SelectionManager.get(player).getShape();
            if (sel == null) {
                player.sendSystemMessage(Component.literal("\u00a7cNecesitas una seleccion valida para generar biomas."));
                return;
            }
            long effective = IntelligentSeeds.resolve(m.seed, TerraformConfig.GENERAL.intelligentSeed.get(), player);
            BiomeTerrainGenerator.generate(player, (ServerLevel) player.level(), sel, effective,
                    m.contScale, m.eroScale, m.moistScale, m.tempScale);
        });
        c.setPacketHandled(true);
    }
}
