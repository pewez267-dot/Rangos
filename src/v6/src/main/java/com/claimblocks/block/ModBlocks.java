/*
 * ModBlocks v6.0.0 - registra los bloques + items como PolymerBlockItem
 * para que clientes vanilla los vean disfrazados como items vanilla.
 */
package com.claimblocks.block;

import com.claimblocks.ClaimBlocksMod;
import com.claimblocks.data.ClaimTier;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1935;
import net.minecraft.class_2248;
import net.minecraft.class_2378;
import net.minecraft.class_2498;
import net.minecraft.class_2960;
import net.minecraft.class_3620;
import net.minecraft.class_4970;
import net.minecraft.class_7706;
import net.minecraft.class_7923;

public final class ModBlocks {
    private static final Map<String, class_3620> MAP_COLORS = new LinkedHashMap<>();
    private static final Map<String, class_2248> BY_ID = new LinkedHashMap<>();

    public static void register() {
        ClaimBlocksMod.LOGGER.info("Registrando 10 bloques claimstone (Polymer)");
        for (ClaimTier t : ClaimTier.VALUES) {
            class_2248 block = registerBlock(t);
            BY_ID.put(t.id, block);
        }
        ItemGroupEvents.modifyEntriesEvent(class_7706.field_40197).register(g -> {
            for (class_2248 b : BY_ID.values()) g.method_45421((class_1935) b);
        });
    }

    private static class_2248 registerBlock(ClaimTier tier) {
        class_4970.class_2251 s = class_4970.class_2251.method_9637()
                .method_9629(50.0f, 1200.0f)
                .method_31710(MAP_COLORS.getOrDefault(tier.id, class_3620.field_16023))
                .method_9631(state -> 8)
                .method_9626(class_2498.field_11533)
                .method_29292();
        ClaimStoneBlock block = new ClaimStoneBlock(s, tier);
        class_2960 id = class_2960.method_60655("claimblocks", tier.id);
        class_2248 reg = class_2378.method_10230(class_7923.field_41175, id, block);

        // Polymer: el item virtual mostrado al cliente es el item del bloque vanilla equivalente.
        class_2248 vanilla = ClaimStoneBlock.polymerBlockFor(tier);
        class_1792 virtualItem = vanilla.method_8389();
        PolymerBlockItem polyItem = new PolymerBlockItem(reg, new class_1792.class_1793(), virtualItem);
        class_2378.method_10230(class_7923.field_41178, id, polyItem);
        return reg;
    }

    public static class_2248 byId(String id) { return BY_ID.get(id); }
    public static Map<String, class_2248> all() { return BY_ID; }

    public static ClaimTier tierForBlock(class_2248 block) {
        if (block instanceof ClaimStoneBlock cb) return cb.getTier();
        return null;
    }

    static {
        MAP_COLORS.put("claimstone_10x10", class_3620.field_16023);
        MAP_COLORS.put("claimstone_25x25", class_3620.field_16024);
        MAP_COLORS.put("claimstone_40x40", class_3620.field_16026);
        MAP_COLORS.put("claimstone_64x64", class_3620.field_15997);
        MAP_COLORS.put("claimstone_80x80", class_3620.field_15995);
        MAP_COLORS.put("claimstone_100x100", class_3620.field_16010);
        MAP_COLORS.put("claimstone_150x150", class_3620.field_15987);
        MAP_COLORS.put("claimstone_250x250", class_3620.field_16020);
        MAP_COLORS.put("claimstone_300x300", class_3620.field_16012);
        MAP_COLORS.put("claimstone_500x500", class_3620.field_16014);
    }
}
