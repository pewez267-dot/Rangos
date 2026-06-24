package com.fantasticchest.network;

import com.fantasticchest.block.ChestBlockEntity;
import com.fantasticchest.gui.terminal.ChestTerminalMenu;
import com.fantasticchest.security.PermissionValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/**
 * C-&gt;S: player extracts items. The server validates access/distance, extracts atomically,
 * gives what fits in the inventory, returns any overflow to the chest, and replies with the
 * updated quantity.
 */
public final class TerminalExtractPacket {

    private final BlockPos pos;
    private final String itemId;
    private final long amount;

    public TerminalExtractPacket(final BlockPos pos, final String itemId, final long amount) {
        this.pos = pos;
        this.itemId = itemId;
        this.amount = amount;
    }

    public static void encode(final TerminalExtractPacket m, final FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeUtf(m.itemId);
        buf.writeLong(m.amount);
    }

    public static TerminalExtractPacket decode(final FriendlyByteBuf buf) {
        return new TerminalExtractPacket(buf.readBlockPos(), buf.readUtf(), buf.readLong());
    }

    public static void handle(final TerminalExtractPacket m, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            final ChestBlockEntity chest = PermissionValidator.resolve(player, m.pos);
            if (chest == null || !chest.canAccess(player.getUUID())) {
                return;
            }
            final Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(m.itemId));
            if (item == null || item == Items.AIR) {
                return;
            }
            final long requested = Math.max(0L, m.amount);
            if (requested > 0L) {
                final long taken = chest.extract(item, requested);
                if (taken > 0L) {
                    final long leftover = giveToPlayer(player, item, taken);
                    if (leftover > 0L) {
                        chest.returnToStock(item, leftover);
                        player.sendSystemMessage(Component.literal("§eInventario lleno: se devolvieron " + leftover + " al cofre."));
                    }
                }
            }
            final long newQty = chest.inventory().get(item);
            final int total = ChestTerminalMenu.buildFullList(chest).size();
            PacketHandler.sendToClient(player, new TerminalUpdatePacket(m.itemId, newQty, total));
        });
        context.setPacketHandled(true);
    }

    /** Gives up to {@code count} units to the player; returns the amount that did not fit. */
    private static long giveToPlayer(final ServerPlayer player, final Item item, final long count) {
        long placed = 0L;
        int max = new ItemStack(item).getMaxStackSize();
        if (max <= 0) {
            max = 64;
        }
        while (placed < count) {
            final int n = (int) Math.min((long) max, count - placed);
            final ItemStack stack = new ItemStack(item, n);
            player.getInventory().add(stack);
            final int thisPlaced = n - stack.getCount();
            if (thisPlaced <= 0) {
                break;
            }
            placed += thisPlaced;
            if (stack.getCount() > 0) {
                break;
            }
        }
        return count - placed;
    }
}
