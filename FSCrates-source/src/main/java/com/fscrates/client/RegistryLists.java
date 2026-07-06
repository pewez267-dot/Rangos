package com.fscrates.client;

import com.fscrates.config.EsNames;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

public final class RegistryLists {
    private RegistryLists() {
    }

    public static List<Item> items() {
        ArrayList<Item> list = new ArrayList<Item>(ForgeRegistries.ITEMS.getValues());
        list.sort(Comparator.comparing(RegistryLists::itemId));
        return list;
    }

    public static String itemId(Item item) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        return rl == null ? "minecraft:air" : rl.toString();
    }

    public static String itemName(Item item) {
        return new ItemStack((ItemLike)item).getHoverName().getString();
    }

    public static List<ResourceLocation> effects() {
        ArrayList<ResourceLocation> list = new ArrayList<ResourceLocation>(ForgeRegistries.MOB_EFFECTS.getKeys());
        list.sort(Comparator.comparing(ResourceLocation::toString));
        return list;
    }

    public static String effectName(ResourceLocation rl) {
        return EsNames.effect(rl);
    }

    // Presets propios de FSCrates (paths). Cada uno se resuelve en CrateBlockEntity.resolveFsPreset.
    public static final String[] FS_PRESETS = new String[]{
        "fs_dust_red", "fs_dust_orange", "fs_dust_gold", "fs_dust_yellow", "fs_dust_lime", "fs_dust_green",
        "fs_dust_aqua", "fs_dust_blue", "fs_dust_purple", "fs_dust_magenta", "fs_dust_pink", "fs_dust_white",
        "fs_dust_tiny", "fs_dust_huge",
        "fs_fade_fire", "fs_fade_ice", "fs_fade_void", "fs_fade_toxic", "fs_fade_royal",
        "fs_shard_gold", "fs_shard_diamond", "fs_shard_amethyst", "fs_shard_emerald",
        "fs_burst_star", "fs_burst_gem", "fs_soul_swirl"
    };

    public static List<ResourceLocation> particles() {
        ArrayList<ResourceLocation> list = new ArrayList<ResourceLocation>();
        // Todas las particulas SIMPLES (sin parametros) del registro.
        list.add(new ResourceLocation("minecraft", "dust"));
        for (Map.Entry e : ForgeRegistries.PARTICLE_TYPES.getEntries()) {
            ResourceLocation key;
            ParticleType type = (ParticleType)e.getValue();
            if (!(type instanceof SimpleParticleType) || (key = ((ResourceKey)e.getKey()).location()).toString().equals("minecraft:dust")) continue;
            list.add(key);
        }
        // Particulas PARAMETRICAS soportadas manualmente en CrateBlockEntity.resolve (2.9.39):
        // antes se excluian por no ser SimpleParticleType; ahora se emiten con valores por
        // defecto sensatos para dar mucha mas variedad visual.
        String[] parametric = new String[]{"dust_color_transition", "block", "block_marker", "falling_dust", "item", "sculk_charge", "shriek"};
        for (String p : parametric) {
            ResourceLocation rl = new ResourceLocation("minecraft", p);
            if (ForgeRegistries.PARTICLE_TYPES.containsKey(rl) && !list.contains(rl)) {
                list.add(rl);
            }
        }
        // PRESETS FSCRATES (2.9.40): 26 "particulas" nuevas listas para usar (colores fijos,
        // degradados, esquirlas de gema, estrella, espiral de alma...). Cada una mapea a una
        // particula vanilla que SI funciona (ver CrateBlockEntity.resolveFsPreset), asi que no
        // requieren texturas propias ni registro cliente. Namespace fscrates -> salen arriba.
        for (String p : RegistryLists.FS_PRESETS) {
            list.add(new ResourceLocation("fscrates", p));
        }
        list.sort(Comparator.comparing(ResourceLocation::toString));
        return list;
    }
}

