package com.fantastickits.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single kit definition with its items and assigned LuckPerms group.
 */
public class KitDefinition {

    private String name;
    private String assignedGroup;
    private List<String> itemNbtList; // Each item stored as SNBT string

    public KitDefinition(String name, String assignedGroup) {
        this.name = name;
        this.assignedGroup = assignedGroup;
        this.itemNbtList = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAssignedGroup() {
        return assignedGroup;
    }

    public void setAssignedGroup(String assignedGroup) {
        this.assignedGroup = assignedGroup;
    }

    public List<String> getItemNbtList() {
        return itemNbtList;
    }

    public void setItemNbtList(List<String> itemNbtList) {
        this.itemNbtList = itemNbtList != null ? itemNbtList : new ArrayList<>();
    }

    public void addItem(CompoundTag itemTag) {
        this.itemNbtList.add(itemTag.toString());
    }

    public void removeItem(int index) {
        if (index >= 0 && index < itemNbtList.size()) {
            itemNbtList.remove(index);
        }
    }

    public void setItem(int index, CompoundTag itemTag) {
        if (index >= 0 && index < itemNbtList.size()) {
            itemNbtList.set(index, itemTag.toString());
        }
    }

    public List<CompoundTag> getItemsAsNbt() {
        List<CompoundTag> tags = new ArrayList<>();
        for (String snbt : itemNbtList) {
            try {
                tags.add(TagParser.parseTag(snbt));
            } catch (Exception e) {
                // Skip malformed entries
            }
        }
        return tags;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("assignedGroup", assignedGroup);
        JsonArray items = new JsonArray();
        for (String snbt : itemNbtList) {
            items.add(snbt);
        }
        obj.add("items", items);
        return obj;
    }

    public static KitDefinition fromJson(JsonObject obj) {
        String name = obj.get("name").getAsString();
        String group = obj.has("assignedGroup") ? obj.get("assignedGroup").getAsString() : "";
        KitDefinition kit = new KitDefinition(name, group);
        if (obj.has("items")) {
            JsonArray items = obj.getAsJsonArray("items");
            List<String> nbtList = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                nbtList.add(items.get(i).getAsString());
            }
            kit.setItemNbtList(nbtList);
        }
        return kit;
    }
}
