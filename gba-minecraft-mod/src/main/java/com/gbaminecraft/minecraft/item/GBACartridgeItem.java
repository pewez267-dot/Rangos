package com.gbaminecraft.minecraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.List;

/**
 * GBA Cartridge item.
 * Stores ROM data as Base64-encoded NBT. Supports ROMs up to ~32 MB.
 * On right-click, inserts into the GBA block's cartridge slot.
 */
public class GBACartridgeItem extends Item {

    public static final String TAG_ROM_NAME  = "RomName";
    public static final String TAG_ROM_DATA  = "RomData";
    public static final String TAG_GAME_CODE = "GameCode";
    public static final String TAG_ROM_SIZE  = "RomSize";

    public GBACartridgeItem(Properties props) {
        super(props);
    }

    // ── NBT helpers ────────────────────────────────────────────────────────
    public static void setROM(ItemStack stack, byte[] romData, String name, String gameCode) {
        CompoundTag tag = stack.getOrCreateTag();
        // Store as Base64 string to fit in NBT
        tag.putString(TAG_ROM_NAME,  name);
        tag.putString(TAG_GAME_CODE, gameCode);
        tag.putInt(TAG_ROM_SIZE, romData.length);
        tag.putString(TAG_ROM_DATA, Base64.getEncoder().encodeToString(romData));
    }

    public static byte[] getROM(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ROM_DATA)) return null;
        try {
            return Base64.getDecoder().decode(tag.getString(TAG_ROM_DATA));
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRomName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString(TAG_ROM_NAME) : "Unknown ROM";
    }

    public static String getGameCode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString(TAG_GAME_CODE) : "????";
    }

    public static int getRomSize(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(TAG_ROM_SIZE) : 0;
    }

    public static boolean hasROM(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_ROM_DATA) && !tag.getString(TAG_ROM_DATA).isEmpty();
    }

    // ── Display ────────────────────────────────────────────────────────────
    @Override
    public Component getName(ItemStack stack) {
        if (hasROM(stack)) {
            return Component.literal(getRomName(stack));
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        if (hasROM(stack)) {
            tooltip.add(Component.literal("Game Code: " + getGameCode(stack)));
            int size = getRomSize(stack);
            tooltip.add(Component.literal("ROM Size: " + (size / 1024) + " KB"));
            tooltip.add(Component.literal("Right-click the GBA Console to load"));
        } else {
            tooltip.add(Component.literal("Empty cartridge — no ROM loaded"));
            tooltip.add(Component.literal("Use /gba load <file> to load a ROM"));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasROM(stack); // Glint effect when ROM is loaded
    }
}
