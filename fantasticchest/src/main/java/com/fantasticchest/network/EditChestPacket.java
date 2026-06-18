package com.fantasticchest.network;

import com.fantasticchest.block.ChestBlockEntity;
import com.fantasticchest.inventory.CompressedInventory;
import com.fantasticchest.security.PermissionValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.function.Supplier;

/**
 * C-&gt;S: OP confirms an edit of a placed chest's items. Bulk fill overwrites every item;
 * otherwise the provided overrides are merged into the existing inventory. The resulting
 * stock also becomes the new "original" used by Refresh Stock.
 */
public final class EditChestPacket {

    private final BlockPos pos;
    private final boolean doBulk;
    private final long bulkValue;
    private final Map<String, Long> overrides;

    public EditChestPacket(final BlockPos pos, final boolean doBulk, final long bulkValue, final Map<String, Long> overrides) {
        this.pos = pos;
        this.doBulk = doBulk;
        this.bulkValue = bulkValue;
        this.overrides = overrides;
    }

    public static void encode(final EditChestPacket m, final FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeBoolean(m.doBulk);
        buf.writeLong(m.bulkValue);
        PacketHandler.writeLongMap(buf, m.overrides);
    }

    public static EditChestPacket decode(final FriendlyByteBuf buf) {
        return new EditChestPacket(buf.readBlockPos(), buf.readBoolean(), buf.readLong(), PacketHandler.readLongMap(buf));
    }

    public static void handle(final EditChestPacket m, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null || !PermissionValidator.isOp(player)) {
                return;
            }
            final ChestBlockEntity chest = PermissionValidator.resolve(player, m.pos);
            if (chest == null) {
                player.sendSystemMessage(Component.literal("§cNo se encontro el cofre o estas demasiado lejos."));
                return;
            }
            final CompressedInventory result;
            if (m.doBulk) {
                result = CreateChestPacket.buildInventory(true, m.bulkValue, m.overrides);
            } else {
                result = chest.inventory().copy();
                if (m.overrides != null) {
                    for (final Map.Entry<String, Long> e : m.overrides.entrySet()) {
                        final Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(e.getKey()));
                        if (item != null) {
                            result.set(item, e.getValue() == null ? 0L : e.getValue());
                        }
                    }
                }
            }
            chest.applyStock(result, result.copy());
            player.sendSystemMessage(Component.literal("§aCofre actualizado (" + result.distinctCount() + " tipos de items)."));
        });
        context.setPacketHandled(true);
    }
}
