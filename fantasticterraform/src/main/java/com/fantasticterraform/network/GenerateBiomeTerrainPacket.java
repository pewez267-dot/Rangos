package com.fantasticterraform.network;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.intelligent.biome.BiomeTerrainGenerator;
import com.fantasticterraform.schematics.BlockStateCodec;
import com.fantasticterraform.selection.SelectionManager;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C->S: genera terreno por capas de ruido (biomas) personalizable en la seleccion activa. */
public final class GenerateBiomeTerrainPacket {

    private final int style;
    private final double featureScale;
    private final double amplitude;
    private final double seaFraction;
    private final boolean useCustom;
    private final String surface;
    private final String sub;
    private final String stone;
    private final long seed;
    private final int forcedBiome;
    private final boolean autoPopulate;

    public GenerateBiomeTerrainPacket(int style, double featureScale, double amplitude, double seaFraction,
                                      boolean useCustom, String surface, String sub, String stone, long seed,
                                      int forcedBiome, boolean autoPopulate) {
        this.style = style;
        this.featureScale = featureScale;
        this.amplitude = amplitude;
        this.seaFraction = seaFraction;
        this.useCustom = useCustom;
        this.surface = surface;
        this.sub = sub;
        this.stone = stone;
        this.seed = seed;
        this.forcedBiome = forcedBiome;
        this.autoPopulate = autoPopulate;
    }

    public static void encode(GenerateBiomeTerrainPacket m, FriendlyByteBuf buf) {
        buf.writeInt(m.style);
        buf.writeDouble(m.featureScale);
        buf.writeDouble(m.amplitude);
        buf.writeDouble(m.seaFraction);
        buf.writeBoolean(m.useCustom);
        buf.writeUtf(m.surface);
        buf.writeUtf(m.sub);
        buf.writeUtf(m.stone);
        buf.writeLong(m.seed);
        buf.writeInt(m.forcedBiome);
        buf.writeBoolean(m.autoPopulate);
    }

    public static GenerateBiomeTerrainPacket decode(FriendlyByteBuf buf) {
        return new GenerateBiomeTerrainPacket(buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readBoolean(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readLong(),
                buf.readInt(), buf.readBoolean());
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
            HolderLookup<Block> lookup = player.server.registryAccess().lookupOrThrow(Registries.BLOCK);
            long effective = IntelligentSeeds.resolve(m.seed, TerraformConfig.GENERAL.intelligentSeed.get(), player);
            BiomeTerrainGenerator.generate(player, (ServerLevel) player.level(), sel, effective,
                    m.style, m.featureScale, m.amplitude, m.seaFraction, m.useCustom,
                    BlockStateCodec.parse(lookup, m.surface),
                    BlockStateCodec.parse(lookup, m.sub),
                    BlockStateCodec.parse(lookup, m.stone),
                    m.forcedBiome, m.autoPopulate);
        });
        c.setPacketHandled(true);
    }
}
