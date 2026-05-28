/*
 * ClaimBlocks v6.0.0 - helper 100% server-side, sin bloques custom.
 *
 * Mapea cada ClaimTier a un bloque vanilla de concreto (color distinto)
 * y marca los items con un NBT custom para identificarlos al colocarlos.
 *
 * Esto evita registrar bloques/items en el namespace claimblocks, lo que
 * a su vez resuelve el error "Received N registry entries unknown to this client".
 */
package com.claimblocks;

import com.claimblocks.data.ClaimTier;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2487;
import net.minecraft.class_9279;
import net.minecraft.class_9334;

public final class ClaimBlocks {
    /** Marcador NBT para identificar nuestros items aunque sean concreto vanilla. */
    public static final String NBT_KEY = "claimblocks";
    public static final String NBT_TIER_FIELD = "tier";

    private ClaimBlocks() {}

    /** Bloque vanilla a usar para cada tier (concreto de colores). */
    public static class_2248 blockForTier(ClaimTier tier) {
        if (tier == null) return class_2246.field_10107;
        return switch (tier.id) {
            case "claimstone_10x10"   -> class_2246.field_10107; // WHITE_CONCRETE
            case "claimstone_25x25"   -> class_2246.field_10172; // LIGHT_GRAY_CONCRETE
            case "claimstone_40x40"   -> class_2246.field_10308; // CYAN_CONCRETE
            case "claimstone_64x64"   -> class_2246.field_10242; // LIGHT_BLUE_CONCRETE
            case "claimstone_80x80"   -> class_2246.field_10421; // LIME_CONCRETE
            case "claimstone_100x100" -> class_2246.field_10542; // YELLOW_CONCRETE
            case "claimstone_150x150" -> class_2246.field_10210; // ORANGE_CONCRETE
            case "claimstone_250x250" -> class_2246.field_10434; // PINK_CONCRETE
            case "claimstone_300x300" -> class_2246.field_10585; // MAGENTA_CONCRETE
            case "claimstone_500x500" -> class_2246.field_10206; // PURPLE_CONCRETE
            default -> class_2246.field_10107;
        };
    }

    public static class_1792 itemForTier(ClaimTier tier) {
        return blockForTier(tier).method_8389();
    }

    /** Verifica si un BLOQUE colocado coincide con el tier esperado del claim. */
    public static boolean isClaimConcreteForTier(class_2248 block, ClaimTier tier) {
        return block == blockForTier(tier);
    }

    /** Verifica si un BLOQUE colocado podría ser cualquiera de nuestros 10 concretos. */
    public static boolean isAnyClaimConcrete(class_2248 block) {
        for (ClaimTier t : ClaimTier.VALUES) {
            if (block == blockForTier(t)) return true;
        }
        return false;
    }

    /** Crea un ItemStack del concreto correspondiente al tier, con NBT marker y nombre custom. */
    public static class_1799 createTierItem(ClaimTier tier, int amount) {
        class_1799 stack = new class_1799(itemForTier(tier), amount);
        // Marcar con NBT custom
        class_2487 nbt = new class_2487();
        class_2487 root = new class_2487();
        root.method_10582(NBT_TIER_FIELD, tier.id);
        nbt.method_10566(NBT_KEY, root);
        stack.method_57379(class_9334.field_49628, class_9279.method_57456(nbt));
        // Nombre visible al cliente
        stack.method_57379(class_9334.field_49631,
                net.minecraft.class_2561.method_43470("Piedra de Claim " + tier.label())
                        .method_27695(new net.minecraft.class_124[]{net.minecraft.class_124.field_1054, net.minecraft.class_124.field_1067})
                        .method_27694(s -> s.method_10978(false))); // sin cursiva
        // Lore informativo
        java.util.List<net.minecraft.class_2561> lore = new java.util.ArrayList<>();
        lore.add(net.minecraft.class_2561.method_43470("Tier: " + tier.id).method_27692(net.minecraft.class_124.field_1080));
        lore.add(net.minecraft.class_2561.method_43470("Radio: " + tier.radius + " | Altura: +/-" + tier.height).method_27692(net.minecraft.class_124.field_1063));
        lore.add(net.minecraft.class_2561.method_43470("Colocala para crear una zona").method_27692(net.minecraft.class_124.field_1060));
        stack.method_57379(class_9334.field_49632, new net.minecraft.class_9290(lore));
        return stack;
    }

    /** Lee el tier de un ItemStack. Retorna null si no tiene nuestro NBT marker. */
    public static String readTierId(class_1799 stack) {
        if (stack == null || stack.method_7960()) return null;
        class_9279 comp = stack.method_57824(class_9334.field_49628);
        if (comp == null) return null;
        class_2487 nbt = comp.method_57461();
        if (nbt == null || !nbt.method_10573(NBT_KEY, 10)) return null; // 10 = compound
        class_2487 root = nbt.method_10562(NBT_KEY);
        if (!root.method_10573(NBT_TIER_FIELD, 8)) return null; // 8 = string
        return root.method_10558(NBT_TIER_FIELD);
    }

    /** Resuelve el ClaimTier de un ItemStack o null. */
    public static ClaimTier readTier(class_1799 stack) {
        String id = readTierId(stack);
        if (id == null) return null;
        return ClaimTier.byId(id);
    }
}
