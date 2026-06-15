package com.fantastic.kits.gui;

import com.fantastic.kits.FantasticKits;
import com.fantastic.kits.kits.Kit;
import com.fantastic.kits.luckperms.GroupInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.List;

/**
 * Builds the {@link ListTag} payloads sent over the network to the client when
 * opening a Fantastic Kits screen. Centralised so the wire format is defined
 * in exactly one place and so the editor screens can rely on a stable schema.
 */
public final class GuiPayload {

    private GuiPayload() {}

    /** Serialises every kit in the registry as a list of full kit NBT tags. */
    public static ListTag allKits() {
        ListTag list = new ListTag();
        for (Kit kit : FantasticKits.kits().all()) {
            list.add(kit.save());
        }
        return list;
    }

    /** Serialises every LuckPerms group as a flat NBT compound. */
    public static ListTag groups() {
        ListTag list = new ListTag();
        List<GroupInfo> all = FantasticKits.luckPerms().listGroups();
        for (GroupInfo g : all) {
            CompoundTag t = new CompoundTag();
            t.putString("name", g.name());
            t.putString("displayName", g.displayName());
            t.putInt("weight", g.weight());
            // Inheritance/permissions are sent for inspection only; the server
            // never trusts the client's view of them.
            ListTag inh = new ListTag();
            for (String s : g.inheritance()) inh.add(StringTag.valueOf(s));
            t.put("inherits", inh);
            list.add(t);
        }
        return list;
    }

    /** Serialises the discovered command catalogue. */
    public static ListTag commandCatalogue() {
        ListTag list = new ListTag();
        for (String c : FantasticKits.commands().discoveredCommands()) {
            list.add(StringTag.valueOf(c));
        }
        return list;
    }
}
