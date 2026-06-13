// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.client;

import com.fspawner.config.EquipmentEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceLocation;
import java.util.Iterator;
import java.util.Collection;
import com.fspawner.config.DropEntry;
import com.fspawner.config.EffectEntry;
import com.fspawner.util.FSAttributes;
import java.util.Map;
import com.fspawner.integration.InfernalModifiers;
import com.fspawner.config.EntityEntry;
import com.fspawner.config.InfernalConfig;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import java.util.List;
import com.fspawner.config.SpawnerConfig;

public final class TooltipBuilder
{
    private static final int MAX_DETAIL_LINES = 24;
    
    private TooltipBuilder() {
    }
    
    public static List<Component> build(final SpawnerConfig cfg, final boolean shift) {
        final List<Component> lines = new ArrayList<Component>();
        lines.add((Component)Component.literal("§d\u2726 Fantastic Spawner \u2726"));
        if (!shift) {
            buildSummary(cfg, lines);
            lines.add((Component)Component.empty());
            lines.add((Component)Component.literal("§7§o").append((Component)Component.translatable("fspawner.shift_hint")));
        }
        else {
            buildDetailed(cfg, lines);
        }
        return lines;
    }
    
    private static void buildSummary(final SpawnerConfig cfg, final List<Component> lines) {
        String entity = entityName(cfg.primaryEntityId());
        if (cfg.entityMode == SpawnerConfig.EntityMode.POOL && cfg.entities.size() > 1) {
            entity = entity + " §7(+" + (cfg.entities.size() - 1);
        }
        final String suffix = cfg.infernal.isEnabled() ? " §cInfernal" : "";
        lines.add((Component)Component.literal("§f" + entity + suffix));
        if (cfg.infernal.mode == InfernalConfig.Mode.RANDOM) {
            lines.add((Component)Component.literal("§7Mods: §e" + cfg.infernal.min + "-" + cfg.infernal.max));
        }
        else if (cfg.infernal.isEnabled() && !cfg.infernal.mods.isEmpty()) {
            lines.add((Component)Component.literal("§7Mods: §e" + cfg.infernal.mods.size()));
        }
    }
    
    private static void buildDetailed(final SpawnerConfig cfg, final List<Component> lines) {
        final List<Component> detail = new ArrayList<Component>();
        detail.add(header("Entidad:"));
        if (cfg.entityMode == SpawnerConfig.EntityMode.POOL) {
            for (EntityEntry e : cfg.entities) {
                detail.add(bullet(entityName(e.id) + " §8(x" + e.weight));
            }
        }
        else {
            detail.add(bullet(entityName(cfg.primaryEntityId())));
        }
        if (cfg.infernal.isEnabled()) {
            detail.add(header("Infernal:"));
            detail.add(bullet("Activado"));
            switch (cfg.infernal.mode) {
                case ALWAYS: {
                    detail.add(bullet("Siempre Infernal"));
                    break;
                }
                case RANDOM: {
                    detail.add(bullet("Modificadores Aleatorios"));
                    detail.add(bullet(cfg.infernal.min + "-" + cfg.infernal.max + " modificadores"));
                    break;
                }
                case CUSTOM: {
                    detail.add(bullet("Personalizado"));
                    break;
                }
            }
            final List<String> shown = (cfg.infernal.mode == InfernalConfig.Mode.RANDOM) ? cfg.infernal.pool : cfg.infernal.mods;
            for (String m : shown) {
                detail.add(bullet("§c" + InfernalModifiers.friendly(m)));
            }
        }
        if (!cfg.attributes.isEmpty()) {
            detail.add(header("Atributos:"));
            for (Map.Entry<String, Double> e2 : cfg.attributes.entrySet()) {
                detail.add(bullet(FSAttributes.labelFor(e2.getKey()) + ": §f" + trim(e2.getValue())));
            }
        }
        final boolean anyEquip = cfg.equipment.stream().anyMatch(eq -> !eq.item.isEmpty());
        if (anyEquip) {
            detail.add(header("Equipamiento:"));
            cfg.equipment.forEach(eq -> {
                if (!eq.item.isEmpty()) {
                    detail.add(bullet(eq.item.getHoverName().getString()));
                }
                return;
            });
        }
        if (!cfg.effects.isEmpty()) {
            detail.add(header("Efectos:"));
            for (EffectEntry fx : cfg.effects) {
                detail.add(bullet(effectName(fx.id) + " " + (fx.amplifier + 1) + (fx.permanent ? " §b(permanente)" : "")));
            }
        }
        if (!cfg.drops.isEmpty()) {
            detail.add(header("Drops:"));
            for (DropEntry d : cfg.drops) {
                if (d.item.isEmpty()) {
                    continue;
                }
                final int pct = Math.round(d.chance * 100.0f);
                detail.add(bullet(d.item.getHoverName().getString() + " §7(" + pct + "%)"));
            }
        }
        if (detail.size() <= 24) {
            lines.addAll(detail);
        }
        else {
            final int hidden = detail.size() - 24;
            lines.addAll(detail.subList(0, 24));
            lines.add((Component)Component.literal("§8  +" + hidden + " m\u00e1s..."));
        }
    }
    
    private static Component header(final String text) {
        return (Component)Component.literal("§6" + text);
    }
    
    private static Component bullet(final String text) {
        return (Component)Component.literal("§7\u2022 ").append((Component)Component.literal("§7" + text));
    }
    
    private static String entityName(final String id) {
        final ResourceLocation rl = ResourceLocation.tryParse((id == null) ? "" : id);
        if (rl == null) {
            return (id == null) ? "?" : id;
        }
        final EntityType<?> type = (EntityType<?>)ForgeRegistries.ENTITY_TYPES.getValue(rl);
        return (type != null) ? type.getDescription().getString() : id;
    }
    
    private static String effectName(final String id) {
        final ResourceLocation rl = ResourceLocation.tryParse((id == null) ? "" : id);
        if (rl == null) {
            return (id == null) ? "?" : id;
        }
        final MobEffect effect = (MobEffect)ForgeRegistries.MOB_EFFECTS.getValue(rl);
        return (effect != null) ? effect.getDisplayName().getString() : id;
    }
    
    private static String trim(final double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long)value);
        }
        return String.valueOf(value);
    }
    
    public static String color(final ChatFormatting fmt) {
        return fmt.toString();
    }
}
