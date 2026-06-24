package com.fantasticshortcuts.network;

import com.fantasticshortcuts.data.Shortcut;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Buffer (de)serialisation for {@link Shortcut} instances used by menus and packets.
 * Only the fields needed by the GUI are transmitted; server-managed metadata
 * (created_by / timestamps) is preserved server-side on save.
 */
public final class ShortcutCodec {

    private ShortcutCodec() {
    }

    public static void write(final FriendlyByteBuf buf, final Shortcut s) {
        buf.writeUtf(s.getId());
        buf.writeUtf(s.getName());
        buf.writeUtf(s.getDescription());
        buf.writeUtf(s.getOriginalCommand());
        buf.writeUtf(s.getAlias());
        buf.writeBoolean(s.isReplaceOriginal());
    }

    public static Shortcut read(final FriendlyByteBuf buf) {
        final Shortcut s = new Shortcut();
        s.setId(buf.readUtf());
        s.setName(buf.readUtf());
        s.setDescription(buf.readUtf());
        s.setOriginalCommand(buf.readUtf());
        s.setAlias(buf.readUtf());
        s.setReplaceOriginal(buf.readBoolean());
        return s;
    }

    public static void writeList(final FriendlyByteBuf buf, final List<Shortcut> list) {
        final List<Shortcut> safe = list == null ? List.of() : list;
        buf.writeVarInt(safe.size());
        for (final Shortcut s : safe) {
            write(buf, s);
        }
    }

    public static List<Shortcut> readList(final FriendlyByteBuf buf) {
        final int size = buf.readVarInt();
        final List<Shortcut> out = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            out.add(read(buf));
        }
        return out;
    }
}
