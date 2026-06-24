package com.fspawner.item;

import com.fspawner.config.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;
import java.util.Map;

/**
 * Translates a {@link SpawnerConfig} (for a single entity) into the vanilla
 * entity NBT compound used inside a spawner's SpawnData. Almost all of the
 * advanced behaviour (attributes, equipment, effects, name, glow) is expressed
 * through native entity NBT so the vanilla spawn engine applies it for free.
 * Infernal Mobs and custom drops are carried inside {@code ForgeData.fspawner}.
 */
public final class EntityNbtBuilder {

    private EntityNbtBuilder() {}

    public static final int PERMANENT_DURATION = Integer.MAX_VALUE;

    /**
     * @param includeFullConfig when true the entire SpawnerConfig is embedded in
     *                          ForgeData (used for the spawner's stored SpawnData
     *                          so {@code /fspawner pickup} can rebuild the item).
     */
    public static CompoundTag build(SpawnerConfig cfg, String entityId, boolean includeFullConfig) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", entityId == null ? "minecraft:pig" : entityId);

        applyAttributes(cfg, tag);
        applyEquipment(cfg, tag);
        applyEffects(cfg, tag);
        applyAppearance(cfg, tag);
        applyMarker(cfg, tag, includeFullConfig);

        return tag;
    }

    private static void applyAttributes(SpawnerConfig cfg, CompoundTag tag) {
        if (cfg.attributes.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        for (Map.Entry<String, Double> e : cfg.attributes.entrySet()) {
            CompoundTag a = new CompoundTag();
            a.putString("Name", e.getKey());
            a.putDouble("Base", e.getValue());
            list.add(a);
        }
        tag.put("Attributes", list);

        // Spawn the mob at full (new) health if a max health was specified.
        Double maxHealth = cfg.attributes.get("minecraft:generic.max_health");
        if (maxHealth != null) {
            tag.putFloat("Health", maxHealth.floatValue());
        }
    }

    private static void applyEquipment(SpawnerConfig cfg, CompoundTag tag) {
        if (cfg.equipment.isEmpty()) {
            return;
        }
        ItemStack mainHand = stack(cfg, EquipmentSlot.MAINHAND);
        ItemStack offHand = stack(cfg, EquipmentSlot.OFFHAND);
        ItemStack feet = stack(cfg, EquipmentSlot.FEET);
        ItemStack legs = stack(cfg, EquipmentSlot.LEGS);
        ItemStack chest = stack(cfg, EquipmentSlot.CHEST);
        ItemStack head = stack(cfg, EquipmentSlot.HEAD);

        ListTag handItems = new ListTag();
        handItems.add(itemTag(mainHand));
        handItems.add(itemTag(offHand));
        tag.put("HandItems", handItems);

        ListTag armorItems = new ListTag();
        armorItems.add(itemTag(feet));
        armorItems.add(itemTag(legs));
        armorItems.add(itemTag(chest));
        armorItems.add(itemTag(head));
        tag.put("ArmorItems", armorItems);

        ListTag handDrops = new ListTag();
        handDrops.add(FloatTag.valueOf(dropChance(cfg, EquipmentSlot.MAINHAND)));
        handDrops.add(FloatTag.valueOf(dropChance(cfg, EquipmentSlot.OFFHAND)));
        tag.put("HandDropChances", handDrops);

        ListTag armorDrops = new ListTag();
        armorDrops.add(FloatTag.valueOf(dropChance(cfg, EquipmentSlot.FEET)));
        armorDrops.add(FloatTag.valueOf(dropChance(cfg, EquipmentSlot.LEGS)));
        armorDrops.add(FloatTag.valueOf(dropChance(cfg, EquipmentSlot.CHEST)));
        armorDrops.add(FloatTag.valueOf(dropChance(cfg, EquipmentSlot.HEAD)));
        tag.put("ArmorDropChances", armorDrops);
    }

    private static ItemStack stack(SpawnerConfig cfg, EquipmentSlot slot) {
        EquipmentEntry e = cfg.equipmentFor(slot);
        return e == null ? ItemStack.EMPTY : e.item;
    }

    private static float dropChance(SpawnerConfig cfg, EquipmentSlot slot) {
        EquipmentEntry e = cfg.equipmentFor(slot);
        return e == null ? 0f : e.dropChance;
    }

    private static CompoundTag itemTag(ItemStack stack) {
        CompoundTag t = new CompoundTag();
        if (stack != null && !stack.isEmpty()) {
            stack.save(t);
        }
        return t;
    }

    private static void applyEffects(SpawnerConfig cfg, CompoundTag tag) {
        if (cfg.effects.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        for (EffectEntry fx : cfg.effects) {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(safeRl(fx.id));
            if (effect == null) {
                continue;
            }
            int duration = fx.permanent ? PERMANENT_DURATION : Math.max(1, fx.duration);
            MobEffectInstance instance = new MobEffectInstance(effect, duration, Math.max(0, fx.amplifier),
                    fx.ambient, fx.particles, fx.particles);
            list.add(instance.save(new CompoundTag()));
        }
        if (!list.isEmpty()) {
            // 1.20.1 LivingEntity reads the "ActiveEffects" key.
            tag.put("ActiveEffects", list);
        }
    }

    private static void applyAppearance(SpawnerConfig cfg, CompoundTag tag) {
        if (cfg.mobName != null && !cfg.mobName.isEmpty()) {
            Style style = Style.EMPTY;
            TextColor color = colorOf(cfg.nameColor);
            if (color != null) {
                style = style.withColor(color);
            }
            Component name = Component.literal(cfg.mobName).withStyle(style);
            tag.putString("CustomName", Component.Serializer.toJson(name));
            tag.putBoolean("CustomNameVisible", cfg.mobNameVisible);
        }
        if (cfg.glowing) {
            tag.putBoolean("Glowing", true);
        }
        if (cfg.bossMode) {
            // Boss flavour: persistent, glowing and visible name.
            tag.putBoolean("Glowing", true);
            tag.putBoolean("PersistenceRequired", true);
        }
    }

    private static void applyMarker(SpawnerConfig cfg, CompoundTag tag, boolean includeFullConfig) {
        CompoundTag forgeData = tag.contains(FSKeys.FORGE_DATA)
                ? tag.getCompound(FSKeys.FORGE_DATA) : new CompoundTag();
        CompoundTag marker = new CompoundTag();

        if (includeFullConfig) {
            marker.put(FSKeys.MARKER_CONFIG, cfg.save());
        }

        if (cfg.infernal.isEnabled()) {
            marker.put(FSKeys.MARKER_INFERNAL, cfg.infernal.save());
        }

        if (!cfg.drops.isEmpty()) {
            ListTag dropList = new ListTag();
            for (DropEntry d : cfg.drops) {
                dropList.add(d.save());
            }
            marker.put(FSKeys.MARKER_DROPS, dropList);
        }
        marker.putBoolean(FSKeys.MARKER_KEEP_VANILLA, cfg.keepVanillaDrops);

        // appear chances for equipment slots that may not always show up
        CompoundTag appear = new CompoundTag();
        for (EquipmentEntry e : cfg.equipment) {
            if (!e.item.isEmpty() && e.appearChance < 1.0f) {
                appear.putFloat(e.slot.getName(), e.appearChance);
            }
        }
        if (!appear.isEmpty()) {
            marker.put(FSKeys.MARKER_APPEAR, appear);
        }

        forgeData.put(FSKeys.MARKER, marker);
        tag.put(FSKeys.FORGE_DATA, forgeData);
    }

    private static ResourceLocation safeRl(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id.toLowerCase(Locale.ROOT));
        return rl == null ? new ResourceLocation("minecraft", "strength") : rl;
    }

    private static TextColor colorOf(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (name.startsWith("#")) {
            TextColor parsed = TextColor.parseColor(name);
            return parsed;
        }
        net.minecraft.ChatFormatting fmt = net.minecraft.ChatFormatting.getByName(name.toLowerCase(Locale.ROOT));
        if (fmt != null && fmt.isColor()) {
            return TextColor.fromLegacyFormat(fmt);
        }
        return null;
    }
}
