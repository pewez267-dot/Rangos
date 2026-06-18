package com.fantasticchest.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * One row of the terminal: an item (by registry id) and its {@code Long} quantity.
 * A quantity of 0 represents a depleted item (shown greyed-out when {@code hide_empty_items}
 * is false).
 */
public record TerminalEntry(String itemId, long quantity) {

    public void write(final FriendlyByteBuf buf) {
        buf.writeUtf(this.itemId);
        buf.writeLong(this.quantity);
    }

    public static TerminalEntry read(final FriendlyByteBuf buf) {
        final String id = buf.readUtf();
        final long qty = buf.readLong();
        return new TerminalEntry(id, qty);
    }
}
