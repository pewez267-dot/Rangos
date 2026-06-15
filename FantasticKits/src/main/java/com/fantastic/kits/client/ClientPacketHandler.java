package com.fantastic.kits.client;

import com.fantastic.kits.client.screen.KitEditorScreen;
import com.fantastic.kits.client.screen.KitListScreen;
import com.fantastic.kits.kits.Kit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Single client-side dispatcher used by every server-bound packet that needs
 * to open a {@link Screen}.
 *
 * <p>Lives in the client classpath but is invoked only through
 * {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} so it never reaches
 * the dedicated server.
 */
public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void openKitList(int mode, ListTag kitsNbt, ListTag groupsNbt, ListTag commandsNbt) {
        List<Kit> kits = new ArrayList<>();
        for (int i = 0; i < kitsNbt.size(); i++) {
            try {
                kits.add(Kit.load(kitsNbt.getCompound(i)));
            } catch (Throwable ignored) {
                // Skip malformed entries instead of crashing the client.
            }
        }
        List<KitListScreen.GroupView> groups = decodeGroups(groupsNbt);
        List<String> commands = decodeStrings(commandsNbt);
        Minecraft.getInstance().setScreen(new KitListScreen(KitListScreen.Mode.fromWire(mode), kits, groups, commands));
    }

    public static void openKitEditor(CompoundTag kitNbt, ListTag groupsNbt, ListTag commandsNbt) {
        Kit kit;
        try {
            kit = Kit.load(kitNbt);
        } catch (Throwable t) {
            return;
        }
        List<KitListScreen.GroupView> groups = decodeGroups(groupsNbt);
        List<String> commands = decodeStrings(commandsNbt);
        Minecraft.getInstance().setScreen(new KitEditorScreen(null, kit, groups, commands));
    }

    // ------------------------------------------------------------------
    // Decoders
    // ------------------------------------------------------------------

    public static List<KitListScreen.GroupView> decodeGroups(ListTag groupsNbt) {
        List<KitListScreen.GroupView> out = new ArrayList<>();
        if (groupsNbt == null) return out;
        for (int i = 0; i < groupsNbt.size(); i++) {
            CompoundTag t = groupsNbt.getCompound(i);
            String name = t.getString("name");
            if (name.isEmpty()) continue;
            String display = t.contains("displayName") ? t.getString("displayName") : name;
            int weight = t.getInt("weight");
            out.add(new KitListScreen.GroupView(name, display, weight));
        }
        return out;
    }

    public static List<String> decodeStrings(ListTag list) {
        List<String> out = new ArrayList<>();
        if (list == null) return out;
        for (int i = 0; i < list.size(); i++) {
            Tag t = list.get(i);
            String s = t.getAsString();
            if (s != null && !s.isEmpty()) out.add(s);
        }
        return out;
    }
}
