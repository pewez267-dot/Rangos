/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.network.packets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.pewez777.fantastickits.gui.client.ClientPacketHandler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server -&gt; Client packet that opens the kit editor and ships every piece of
 * data the client needs: the serialized kit (NBT), whether this is an edit or a
 * create, the list of LuckPerms groups, the discovered command catalogue and
 * whether LuckPerms is available.
 */
public final class OpenEditorPacket {

    private final boolean editMode;
    private final boolean luckPermsAvailable;
    private final CompoundTag kitTag;
    private final List<String> groups;
    private final List<String> commandCatalog;

    public OpenEditorPacket(boolean editMode, boolean luckPermsAvailable, CompoundTag kitTag,
                            List<String> groups, List<String> commandCatalog) {
        this.editMode = editMode;
        this.luckPermsAvailable = luckPermsAvailable;
        this.kitTag = kitTag == null ? new CompoundTag() : kitTag;
        this.groups = groups == null ? new ArrayList<>() : groups;
        this.commandCatalog = commandCatalog == null ? new ArrayList<>() : commandCatalog;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public boolean isLuckPermsAvailable() {
        return luckPermsAvailable;
    }

    public CompoundTag getKitTag() {
        return kitTag;
    }

    public List<String> getGroups() {
        return groups;
    }

    public List<String> getCommandCatalog() {
        return commandCatalog;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(editMode);
        buf.writeBoolean(luckPermsAvailable);
        buf.writeNbt(kitTag);
        buf.writeCollection(groups, FriendlyByteBuf::writeUtf);
        buf.writeCollection(commandCatalog, FriendlyByteBuf::writeUtf);
    }

    public static OpenEditorPacket decode(FriendlyByteBuf buf) {
        boolean editMode = buf.readBoolean();
        boolean luckPermsAvailable = buf.readBoolean();
        CompoundTag tag = buf.readNbt();
        List<String> groups = buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf);
        List<String> commands = buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf);
        return new OpenEditorPacket(editMode, luckPermsAvailable, tag, groups, commands);
    }

    public static void handle(OpenEditorPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // Open the client screen only on the physical client; the reference to
        // the client handler is isolated inside the DistExecutor so the server
        // never class-loads client-only code.
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.openEditor(msg)));
        context.setPacketHandled(true);
    }
}
