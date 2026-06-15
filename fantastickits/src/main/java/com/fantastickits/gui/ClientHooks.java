package com.fantastickits.gui;

import com.fantastickits.data.Kit;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * Client-only entry point invoked from {@code OpenKitEditorPacket} (guarded by
 * {@code DistExecutor}, so this class is never loaded on a dedicated server).
 */
public final class ClientHooks {

    private ClientHooks() {
    }

    public static void openEditor(final CompoundTag kitNbt, final List<String> groups, final List<String> assignedCommands) {
        final Kit kit = kitNbt == null ? new Kit() : Kit.fromNbt(kitNbt);
        Minecraft.getInstance().setScreen(new KitEditorScreen(kit, groups, assignedCommands));
    }
}
