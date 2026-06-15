package com.pewez.fantasticshortcuts.network;

import com.pewez.fantasticshortcuts.FantasticShortcutsMod;
import com.pewez.fantasticshortcuts.shortcuts.Shortcut;
import com.pewez.fantasticshortcuts.shortcuts.ShortcutManager;
import com.pewez.fantasticshortcuts.util.ChatPrefix;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server. Save edits made to an existing shortcut (target command, options).
 */
public class SaveShortcutPacket {

    public final Shortcut shortcut;

    public SaveShortcutPacket(Shortcut shortcut) {
        this.shortcut = shortcut;
    }

    public static void encode(SaveShortcutPacket packet, FriendlyByteBuf buf) {
        packet.shortcut.encode(buf);
    }

    public static SaveShortcutPacket decode(FriendlyByteBuf buf) {
        return new SaveShortcutPacket(Shortcut.decode(buf));
    }

    public static void handle(SaveShortcutPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null || !sender.hasPermissions(4)) {
                return;
            }
            String actor = sender.getGameProfile().getName();
            Shortcut existing = ShortcutManager.get().get(packet.shortcut.alias);
            if (existing == null) {
                sender.sendSystemMessage(ChatPrefix.error(
                        "No shortcut named '" + packet.shortcut.alias + "'. Use Create instead."));
                return;
            }
            // Update target command (validates inside).
            ShortcutManager.Result editResult = ShortcutManager.get()
                    .edit(packet.shortcut.alias, packet.shortcut.command, actor);
            if (!editResult.success()) {
                sender.sendSystemMessage(ChatPrefix.error(editResult.message()));
                return;
            }
            // Update toggles.
            ShortcutManager.get().setReplaceOriginal(packet.shortcut.alias, packet.shortcut.replaceOriginal, actor);
            existing.allowArguments = packet.shortcut.allowArguments;
            existing.description = packet.shortcut.description == null ? "" : packet.shortcut.description;
            ShortcutManager.get().save();

            sender.sendSystemMessage(ChatPrefix.success(
                    "Saved /" + packet.shortcut.alias + " -> /" + existing.command));
            FantasticShortcutsMod.liveSync(sender.getServer());
            FSShortcutsNetwork.sendToClient(sender,
                    new OpenEditorPacket(new java.util.ArrayList<>(ShortcutManager.get().all()), "lista"));
        });
        ctx.setPacketHandled(true);
    }
}
