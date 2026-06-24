package com.fspawner.client;

import com.fspawner.config.DropEntry;
import com.fspawner.config.EffectEntry;
import com.fspawner.config.EntityEntry;
import com.fspawner.config.InfernalConfig;
import com.fspawner.config.SpawnerConfig;
import com.fspawner.integration.InfernalModifiers;
import com.fspawner.util.FSAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds the dynamic FSpawner item tooltip (normal + SHIFT detailed view). */
public final class TooltipBuilder {

    private TooltipBuilder() {}

    private static final int MAX_DETAIL_LINES = 24;

    public static List<Component> build(SpawnerConfig cfg, boolean shift) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("\u00A7d\u2726 Fantastic Spawner \u2726"));

        if (!shift) {
            buildSummary(cfg, lines);
            lines.add(Component.empty());
            lines.add(Component.literal("\u00A77\u00A7o")
                    .append(Component.translatable("fspawner.shift_hint")));
        } else {
            buildDetailed(cfg, lines);
        }
        return lines;
    }

    private static void buildSummary(SpawnerConfig cfg, List<Component> lines) {
        String entity = entityName(cfg.primaryEntityId());
        if (cfg.entityMode == SpawnerConfig.EntityMode.POOL && cfg.entities.size() > 1) {
            entity = entity + " \u00A77(+" + (cfg.entities.size() - 1) + ")";
        }
        String suffix = cfg.infernal.isEnabled() ? " \u00A7cInfernal" : "";
        lines.add(Component.literal("\u00A7f" + entity + suffix));

        if (cfg.infernal.mode == InfernalConfig.Mode.RANDOM) {
            lines.add(Component.literal("\u00A77Mods: \u00A7e" + cfg.infernal.min + "-" + cfg.infernal.max));
        } else if (cfg.infernal.isEnabled() && !cfg.infernal.mods.isEmpty()) {
            lines.add(Component.literal("\u00A77Mods: \u00A7e" + cfg.infernal.mods.size()));
        }
    }

    private static void buildDetailed(SpawnerConfig cfg, List<Component> lines) {
        List<Component> detail = new ArrayList<>();

        detail.add(header("Entidad:"));
        if (cfg.entityMode == SpawnerConfig.EntityMode.POOL) {
            for (EntityEntry e : cfg.entities) {
                detail.add(bullet(entityName(e.id) + " \u00A78(x" + e.weight + ")"));
            }
        } else {
            detail.add(bullet(entityName(cfg.primaryEntityId())));
        }

        if (cfg.infernal.isEnabled()) {
            detail.add(header("Infernal:"));
            detail.add(bullet("Activado"));
            switch (cfg.infernal.mode) {
                case ALWAYS -> detail.add(bullet("Siempre Infernal"));
                case RANDOM -> {
                    detail.add(bullet("Modificadores Aleatorios"));
                    detail.add(bullet(cfg.infernal.min + "-" + cfg.infernal.max + " modificadores"));
                }
                case CUSTOM -> detail.add(bullet("Personalizado"));
                default -> {
                }
            }
            List<String> shown = cfg.infernal.mode == InfernalConfig.Mode.RANDOM
                    ? cfg.infernal.pool : cfg.infernal.mods;
            for (String m : shown) {
                detail.add(bullet("\u00A7c" + InfernalModifiers.friendly(m)));
            }
        }

        if (!cfg.attributes.isEmpty()) {
            detail.add(header("Atributos:"));
            for (Map.Entry<String, Double> e : cfg.attributes.entrySet()) {
                detail.add(bullet(FSAttributes.labelFor(e.getKey()) + ": \u00A7f" + trim(e.getValue())));
            }
        }

        boolean anyEquip = cfg.equipment.stream().anyMatch(eq -> !eq.item.isEmpty());
        if (anyEquip) {
            detail.add(header("Equipamiento:"));
            cfg.equipment.forEach(eq -> {
                if (!eq.item.isEmpty()) {
                    detail.add(bullet(eq.item.getHoverName().getString()));
                }
            });
        }

        if (!cfg.effects.isEmpty()) {
            detail.add(header("Efectos:"));
            for (EffectEntry fx : cfg.effects) {
                detail.add(bullet(effectName(fx.id) + " " + (fx.amplifier + 1)
                        + (fx.permanent ? " \u00A7b(permanente)" : "")));
            }
        }

        if (!cfg.drops.isEmpty()) {
            detail.add(header("Drops:"));
            for (DropEntry d : cfg.drops) {
                if (d.item.isEmpty()) {
                    continue;
                }
                int pct = Math.round(d.chance * 100f);
                detail.add(bullet(d.item.getHoverName().getString() + " \u00A77(" + pct + "%)"));
            }
        }

        // cap to keep the tooltip readable
        if (detail.size() <= MAX_DETAIL_LINES) {
            lines.addAll(detail);
        } else {
            int hidden = detail.size() - MAX_DETAIL_LINES;
            lines.addAll(detail.subList(0, MAX_DETAIL_LINES));
            lines.add(Component.literal("\u00A78  +" + hidden + " m\u00e1s..."));
        }
    }

    private static Component header(String text) {
        return Component.literal("\u00A76" + text);
    }

    private static Component bullet(String text) {
        return Component.literal("\u00A77\u2022 ").append(Component.literal("\u00A77" + text));
    }

    private static String entityName(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id);
        if (rl == null) {
            return id == null ? "?" : id;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(rl);
        return type != null ? type.getDescription().getString() : id;
    }

    private static String effectName(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id);
        if (rl == null) {
            return id == null ? "?" : id;
        }
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(rl);
        return effect != null ? effect.getDisplayName().getString() : id;
    }

    private static String trim(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    // kept for callers wanting raw color codes
    public static String color(ChatFormatting fmt) {
        return fmt.toString();
    }
}
