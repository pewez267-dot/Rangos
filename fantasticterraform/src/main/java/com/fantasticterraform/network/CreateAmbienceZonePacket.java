package com.fantasticterraform.network;

import com.fantasticterraform.ambience.AmbienceManager;
import com.fantasticterraform.ambience.AmbienceZone;
import com.fantasticterraform.selection.SelectionManager;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C->S: crea una zona de ambiente usando el bounding box de la seleccion activa
 * (definida con la varita) y los parametros de sonido del HUD.
 */
public final class CreateAmbienceZonePacket {

    private final String sound;
    private final float volume;
    private final float pitch;
    private final boolean loop;
    private final double fadeSeconds;

    public CreateAmbienceZonePacket(String sound, float volume, float pitch, boolean loop, double fadeSeconds) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.loop = loop;
        this.fadeSeconds = fadeSeconds;
    }

    public static void encode(CreateAmbienceZonePacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.sound);
        buf.writeFloat(m.volume);
        buf.writeFloat(m.pitch);
        buf.writeBoolean(m.loop);
        buf.writeDouble(m.fadeSeconds);
    }

    public static CreateAmbienceZonePacket decode(FriendlyByteBuf buf) {
        return new CreateAmbienceZonePacket(buf.readUtf(), buf.readFloat(), buf.readFloat(),
                buf.readBoolean(), buf.readDouble());
    }

    public static void handle(CreateAmbienceZonePacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            SelectionShape sel = SelectionManager.get(player).getShape();
            if (sel == null) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7cNecesitas una seleccion valida para definir la region de ambiente."));
                return;
            }
            BlockPos min = sel.getMin();
            BlockPos max = sel.getMax();
            AmbienceZone zone = new AmbienceZone();
            zone.dimension = player.level().dimension().location().toString();
            zone.minX = min.getX();
            zone.minY = min.getY();
            zone.minZ = min.getZ();
            zone.maxX = max.getX();
            zone.maxY = max.getY();
            zone.maxZ = max.getZ();
            zone.sound = m.sound;
            zone.volume = m.volume;
            zone.pitch = m.pitch;
            zone.loop = m.loop;
            zone.fadeSeconds = m.fadeSeconds;
            AmbienceManager.get().add(zone);
            player.sendSystemMessage(Component.literal("\u00a7aZona de ambiente creada (" + m.sound + ")."));
        });
        c.setPacketHandled(true);
    }
}
