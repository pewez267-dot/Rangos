package com.pewez.fantasticshortcuts.network;

import com.pewez.fantasticshortcuts.FantasticShortcutsMod;
import com.pewez.fantasticshortcuts.shortcuts.Shortcut;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import com.pewez.fantasticshortcuts.util.ChatPrefix;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * Client -> Server. Save edits to an existing shortcut. Supports renaming the alias (original alias
 * is carried separately so the server can rename safely).
 */
public class SaveShortcutPacket {

    public final String originalAlias;
    public final Shortcut shortcut;

    public SaveShortcutPacket(String originalAlias, Shortcut shortcut) {
        this.originalAlias = originalAlias;
        this.shortcut = shortcut;
    }

    public static void encode(SaveShortcutPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.originalAlias == null ? "" : packet.originalAlias, 64);
        packet.shortcut.encode(buf);
    }

    public static SaveShortcutPacket decode(FriendlyByteBuf buf) {
        String original = buf.readUtf(64);
        return new SaveShortcutPacket(original, Shortcut.decode(buf));
    }

    public static void handle(SaveShortcutPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null || !sender.hasPermissions(4)) {
                return;
            }
            String actor = sender.getGameProfile().getName();
            String original = packet.originalAlias == null ? "" : packet.originalAlias.trim();
            String newAlias = packet.shortcut.alias == null ? "" : packet.shortcut.alias.trim();

            ShortcutManager.Result result;
            if (!original.equalsIgnoreCase(newAlias)) {
                // Rename: create the new alias, then delete the old one.
                result = ShortcutManager.get().create(newAlias, packet.shortcut.command, actor);
                if (result.success()) {
                    Shortcut created = ShortcutManager.get().get(newAlias);
                    if (created != null) {
                        created.allowArguments = packet.shortcut.allowArguments;
                        created.replaceOriginal = packet.shortcut.replaceOriginal;
                        created.description = packet.shortcut.description == null ? "" : packet.shortcut.description;
                    }
                    ShortcutManager.get().delete(original, actor);
                    ShortcutManager.get().save();
                }
            } else {
                result = ShortcutManager.get().edit(newAlias, packet.shortcut.command, actor);
                if (result.success()) {
                    ShortcutManager.get().setReplaceOriginal(newAlias, packet.shortcut.replaceOriginal, actor);
                    Shortcut existing = ShortcutManager.get().get(newAlias);
                    if (existing != null) {
                        existing.allowArguments = packet.shortcut.allowArguments;
                        existing.description = packet.shortcut.description == null ? "" : packet.shortcut.description;
                    }
                    ShortcutManager.get().save();
                }
            }

            sender.sendSystemMessage(result.success()
                    ? ChatPrefix.success(result.message())
                    : ChatPrefix.error(result.message()));
            FantasticShortcutsMod.liveSync(sender.getServer());
            FSShortcutsNetwork.sendToClient(sender,
                    new OpenEditorPacket(new ArrayList<>(ShortcutManager.get().all()), "lista"));
        });
        ctx.setPacketHandled(true);
    }
}
