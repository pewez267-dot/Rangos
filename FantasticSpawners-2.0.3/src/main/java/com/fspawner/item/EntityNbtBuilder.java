// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.item;

import net.minecraft.ChatFormatting;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import com.fspawner.config.DropEntry;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.effect.MobEffect;
import com.fspawner.config.EffectEntry;
import com.fspawner.config.EquipmentEntry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.FloatTag;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.Iterator;
import net.minecraft.nbt.Tag;
import java.util.Map;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import com.fspawner.config.SpawnerConfig;

public final class EntityNbtBuilder
{
    public static final int PERMANENT_DURATION = Integer.MAX_VALUE;
    
    private EntityNbtBuilder() {
    }
    
    public static CompoundTag build(final SpawnerConfig cfg, final String entityId, final boolean includeFullConfig) {
        final CompoundTag tag = new CompoundTag();
        tag.putString("id", (entityId == null) ? "minecraft:pig" : entityId);
        applyAttributes(cfg, tag);
        applyEquipment(cfg, tag);
        applyEffects(cfg, tag);
        applyAppearance(cfg, tag);
        applyMarker(cfg, tag, includeFullConfig);
        return tag;
    }
    
    private static void applyAttributes(final SpawnerConfig cfg, final CompoundTag tag) {
        if (cfg.attributes.isEmpty()) {
            return;
        }
        final ListTag list = new ListTag();
        for (final Map.Entry<String, Double> e : cfg.attributes.entrySet()) {
            final CompoundTag a = new CompoundTag();
            a.putString("Name", (String)e.getKey());
            a.putDouble("Base", (double)e.getValue());
            list.add(a);
        }
        tag.put("Attributes", (Tag)list);
        final Double maxHealth = cfg.attributes.get("minecraft:generic.max_health");
        if (maxHealth != null) {
            tag.putFloat("Health", maxHealth.floatValue());
        }
    }
    
    private static void applyEquipment(final SpawnerConfig cfg, final CompoundTag tag) {
        if (cfg.equipment.isEmpty()) {
            return;
        }
        final ItemStack mainHand = stack(cfg, EquipmentSlot.MAINHAND);
        final ItemStack offHand = stack(cfg, EquipmentSlot.OFFHAND);
        final ItemStack feet = stack(cfg, EquipmentSlot.FEET);
        final ItemStack legs = stack(cfg, EquipmentSlot.LEGS);
        final ItemStack chest = stack(cfg, EquipmentSlot.CHEST);
        final ItemStack head = stack(cfg, EquipmentSlot.HEAD);
        final ListTag handItems = new ListTag();
        handItems.add(itemTag(mainHand));
        handItems.add(itemTag(offHand));
        tag.put("HandItems", (Tag)handItems);
        final ListTag armorItems = new ListTag();
        armorItems.add(itemTag(feet));
        armorItems.add(itemTag(legs));
        armorItems.add(itemTag(chest));
        armorItems.add(itemTag(head));
        tag.put("ArmorItems", (Tag)armorItems);
        final ListTag handDrops = new ListTag();
        handDrops.add(FloatTag.valueOf(dropChance(cfg, EquipmentSlot.MAINHAND)));
        handDrops.add(FloatTag.valueOf(dropChance(cfg, EquipmentSlot.OFFHAND)));
        tag.put("HandDropChances", (Tag)handDrops);
        final ListTag armorDrops = new ListTag();
        armorDrops.add(FloatTag.valueOf(dropChance(cfg, EquipmentSlot.FEET)));
        armorDrops.add(FloatTag.valueOf(dropChance(cfg, EquipmentSlot.LEGS)));
        armorDrops.add(FloatTag.valueOf(dropChance(cfg, EquipmentSlot.CHEST)));
        armorDrops.add(FloatTag.valueOf(dropChance(cfg, EquipmentSlot.HEAD)));
        tag.put("ArmorDropChances", (Tag)armorDrops);
    }
    
    private static ItemStack stack(final SpawnerConfig cfg, final EquipmentSlot slot) {
        final EquipmentEntry e = cfg.equipmentFor(slot);
        return (e == null) ? ItemStack.EMPTY : e.item;
    }
    
    private static float dropChance(final SpawnerConfig cfg, final EquipmentSlot slot) {
        final EquipmentEntry e = cfg.equipmentFor(slot);
        return (e == null) ? 0.0f : e.dropChance;
    }
    
    private static CompoundTag itemTag(final ItemStack stack) {
        final CompoundTag t = new CompoundTag();
        if (stack != null && !stack.isEmpty()) {
            stack.save(t);
        }
        return t;
    }
    
    private static void applyEffects(final SpawnerConfig cfg, final CompoundTag tag) {
        if (cfg.effects.isEmpty()) {
            return;
        }
        final ListTag list = new ListTag();
        for (final EffectEntry fx : cfg.effects) {
            final MobEffect effect = (MobEffect)ForgeRegistries.MOB_EFFECTS.getValue(safeRl(fx.id));
            if (effect == null) {
                continue;
            }
            final int duration = fx.permanent ? Integer.MAX_VALUE : Math.max(1, fx.duration);
            final MobEffectInstance instance = new MobEffectInstance(effect, duration, Math.max(0, fx.amplifier), fx.ambient, fx.particles, fx.particles);
            list.add(instance.save(new CompoundTag()));
        }
        if (!list.isEmpty()) {
            tag.put("ActiveEffects", (Tag)list);
        }
    }
    
    private static void applyAppearance(final SpawnerConfig cfg, final CompoundTag tag) {
        if (cfg.mobName != null && !cfg.mobName.isEmpty()) {
            Style style = Style.EMPTY;
            final TextColor color = colorOf(cfg.nameColor);
            if (color != null) {
                style = style.withColor(color);
            }
            final Component name = (Component)Component.literal(cfg.mobName).withStyle(style);
            tag.putString("CustomName", Component.Serializer.toJson(name));
            tag.putBoolean("CustomNameVisible", cfg.mobNameVisible);
        }
        if (cfg.glowing) {
            tag.putBoolean("Glowing", true);
        }
        if (cfg.bossMode) {
            tag.putBoolean("Glowing", true);
            tag.putBoolean("PersistenceRequired", true);
        }
    }
    
    private static void applyMarker(final SpawnerConfig cfg, final CompoundTag tag, final boolean includeFullConfig) {
        final CompoundTag forgeData = tag.contains("ForgeData") ? tag.getCompound("ForgeData") : new CompoundTag();
        final CompoundTag marker = new CompoundTag();
        if (includeFullConfig) {
            marker.put("cfg", (Tag)cfg.save());
        }
        if (cfg.infernal.isEnabled()) {
            marker.put("infernal", (Tag)cfg.infernal.save());
        }
        if (!cfg.drops.isEmpty()) {
            final ListTag dropList = new ListTag();
            for (final DropEntry d : cfg.drops) {
                dropList.add(d.save());
            }
            marker.put("drops", (Tag)dropList);
        }
        marker.putBoolean("keepVanillaDrops", cfg.keepVanillaDrops);
        final CompoundTag appear = new CompoundTag();
        for (final EquipmentEntry e : cfg.equipment) {
            if (!e.item.isEmpty() && e.appearChance < 1.0f) {
                appear.putFloat(e.slot.getName(), e.appearChance);
            }
        }
        if (!appear.isEmpty()) {
            marker.put("appearChances", (Tag)appear);
        }
        forgeData.put("fspawner", (Tag)marker);
        tag.put("ForgeData", (Tag)forgeData);
    }
    
    private static ResourceLocation safeRl(final String id) {
        final ResourceLocation rl = ResourceLocation.tryParse((id == null) ? "" : id.toLowerCase(Locale.ROOT));
        return (rl == null) ? new ResourceLocation("minecraft", "strength") : rl;
    }
    
    private static TextColor colorOf(final String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (name.startsWith("#")) {
            final TextColor parsed = TextColor.parseColor(name);
            return parsed;
        }
        final ChatFormatting fmt = ChatFormatting.getByName(name.toLowerCase(Locale.ROOT));
        if (fmt != null && fmt.isColor()) {
            return TextColor.fromLegacyFormat(fmt);
        }
        return null;
    }
}
