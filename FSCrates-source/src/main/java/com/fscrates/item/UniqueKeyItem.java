package com.fscrates.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

// Llave UNICA por crate. A diferencia de la Fantastic Key (universal), esta llave lleva en
// su NBT (bajo CrateItems.TAG_ROOT):
//   - keyModel : id del modelo elegido (KeyModels.Entry.id) -> tambien fija CustomModelData
//   - crateId  : id de la crate a la que esta enlazada (solo abre ESA crate)
//   - keyName  : nombre editable (con codigos & de color). Si vacio, usa el nombre por defecto.
// El modelo se renderiza via CustomModelData sobre models/item/unique_key.json (overrides).
public class UniqueKeyItem
extends Item {
    public UniqueKeyItem() {
        super(new Item.Properties().stacksTo(64));
    }

    public static String keyName(ItemStack stack) {
        if (stack != null && stack.hasTag()) {
            CompoundTag root = stack.getTag().getCompound(CrateItems.TAG_ROOT);
            if (root.contains("keyName")) {
                return root.getString("keyName");
            }
        }
        return "";
    }

    public Component getName(ItemStack stack) {
        String n = UniqueKeyItem.keyName(stack);
        if (n != null && !n.isBlank()) {
            // soporta codigos '&' de color/estilo escritos por el admin
            return Component.literal((String)n.replace('&', '\u00a7'));
        }
        return Component.literal((String)"\u2726 Llave de Crate \u2726").withStyle(ChatFormatting.AQUA);
    }

    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String crateId = "";
        if (stack != null && stack.hasTag()) {
            crateId = stack.getTag().getCompound(CrateItems.TAG_ROOT).getString("crateId");
        }
        if (crateId != null && !crateId.isBlank()) {
            tooltip.add((Component)Component.literal((String)("Abre solo la crate: \u00a7f" + crateId)).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add((Component)Component.literal((String)"Llave unica de crate.").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add((Component)Component.literal((String)"Enlazada a su caja.").withStyle(new ChatFormatting[]{ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC}));
    }
}
