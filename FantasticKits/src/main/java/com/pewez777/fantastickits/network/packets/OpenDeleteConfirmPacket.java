/*
 * Fantastic Kits
 * Copyright (c) 2026 Pewez777. All Rights Reserved.
 *
 * Proprietary software. Unauthorized copying, distribution, modification,
 * reverse engineering, modpack inclusion or AI training is prohibited.
 * See LICENSE.txt for the full proprietary license terms.
 */
package com.pewez777.fantastickits.network.packets;

import java.util.function.Supplier;

import com.pewez777.fantastickits.gui.client.ClientPacketHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server -&gt; Client packet that opens the deletion confirmation screen,
 * preventing accidental deletions. The actual deletion is only performed when
 * the client confirms and the resulting {@link DeleteKitPacket} is re-validated
 * server-side.
 */
public final class OpenDeleteConfirmPacket {

    private final String kitName;
    private final String ownerGroup;

    public OpenDeleteConfirmPacket(String kitName, String ownerGroup) {
        this.kitName = kitName == null ? "" : kitName;
        this.ownerGroup = ownerGroup == null ? "" : ownerGroup;
    }

    public String getKitName() {
        return kitName;
    }

    public String getOwnerGroup() {
        return ownerGroup;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(kitName);
        buf.writeUtf(ownerGroup);
    }

    public static OpenDeleteConfirmPacket decode(FriendlyByteBuf buf) {
        return new OpenDeleteConfirmPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(OpenDeleteConfirmPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.openDeleteConfirm(msg)));
        context.setPacketHandled(true);
    }
}
