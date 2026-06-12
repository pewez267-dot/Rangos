// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.config;

import java.util.Iterator;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import java.util.Collection;
import com.fscrates.animation.AnimationRegistry;
import java.util.ArrayList;
import java.util.List;

public class CrateConfig
{
    public String id;
    public String displayName;
    public Rarity rarity;
    public final List<RewardEntry> rewards;
    public int rolls;
    public String animationId;
    public boolean glow;
    public boolean particles;
    public String nameColorHexOverride;
    public boolean floatingName;
    public boolean showOdds;
    public final List<String> floatingText;
    public final List<ParticleLayer> particleLayers;
    public boolean consumeKey;
    public int cooldownSeconds;
    public boolean broadcast;
    public boolean allowSkip;
    public int openDelayTicks;
    public String requiredPermission;
    
    public CrateConfig() {
        this.id = "nueva_crate";
        this.displayName = "§d\u2726 Crate \u2726";
        this.rarity = Rarity.COMMON;
        this.rewards = new ArrayList<RewardEntry>();
        this.rolls = 1;
        this.animationId = AnimationRegistry.defaultId();
        this.glow = true;
        this.particles = true;
        this.nameColorHexOverride = "";
        this.floatingName = true;
        this.showOdds = false;
        this.floatingText = new ArrayList<String>();
        this.particleLayers = new ArrayList<ParticleLayer>();
        this.consumeKey = true;
        this.cooldownSeconds = 0;
        this.broadcast = false;
        this.allowSkip = true;
        this.openDelayTicks = 0;
        this.requiredPermission = "";
        this.particleLayers.addAll(ParticleLayer.defaults());
    }
    
    public CrateConfig(final String id) {
        this();
        this.id = id;
    }
    
    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("id", this.id);
        tag.putString("displayName", this.displayName);
        tag.putString("rarity", this.rarity.name());
        tag.putInt("rolls", this.rolls);
        tag.putString("animationId", this.animationId);
        final ListTag rewardList = new ListTag();
        for (final RewardEntry r : this.rewards) {
            rewardList.add((Object)r.save());
        }
        tag.put("rewards", (Tag)rewardList);
        tag.putBoolean("glow", this.glow);
        tag.putBoolean("particles", this.particles);
        tag.putString("nameColorHex", this.nameColorHexOverride);
        tag.putBoolean("floatingName", this.floatingName);
        tag.putBoolean("showOdds", this.showOdds);
        final ListTag floatList = new ListTag();
        for (final String line : this.floatingText) {
            floatList.add((Object)StringTag.valueOf(line));
        }
        tag.put("floatingText", (Tag)floatList);
        final ListTag particleList = new ListTag();
        for (final ParticleLayer layer : this.particleLayers) {
            particleList.add((Object)layer.save());
        }
        tag.put("particleLayers", (Tag)particleList);
        tag.putBoolean("consumeKey", this.consumeKey);
        tag.putInt("cooldown", this.cooldownSeconds);
        tag.putBoolean("broadcast", this.broadcast);
        tag.putBoolean("allowSkip", this.allowSkip);
        tag.putInt("openDelay", this.openDelayTicks);
        tag.putString("permission", this.requiredPermission);
        return tag;
    }
    
    public static CrateConfig load(final CompoundTag tag) {
        final CrateConfig c = new CrateConfig();
        c.id = (tag.contains("id") ? tag.getString("id") : "nueva_crate");
        c.displayName = (tag.contains("displayName") ? tag.getString("displayName") : "§d\u2726 Crate \u2726");
        c.rarity = Rarity.byName(tag.getString("rarity"));
        c.rolls = (tag.contains("rolls") ? Math.max(1, tag.getInt("rolls")) : 1);
        c.animationId = (tag.contains("animationId") ? tag.getString("animationId") : AnimationRegistry.defaultId());
        if (!AnimationRegistry.exists(c.animationId)) {
            c.animationId = AnimationRegistry.defaultId();
        }
        c.rewards.clear();
        final ListTag rewardList = tag.getList("rewards", 10);
        for (int i = 0; i < rewardList.size(); ++i) {
            c.rewards.add(RewardEntry.load(rewardList.getCompound(i)));
        }
        c.glow = (!tag.contains("glow") || tag.getBoolean("glow"));
        c.particles = (!tag.contains("particles") || tag.getBoolean("particles"));
        c.nameColorHexOverride = tag.getString("nameColorHex");
        c.floatingName = (!tag.contains("floatingName") || tag.getBoolean("floatingName"));
        c.showOdds = tag.getBoolean("showOdds");
        c.floatingText.clear();
        final ListTag floatList = tag.getList("floatingText", 8);
        for (int j = 0; j < floatList.size(); ++j) {
            c.floatingText.add(floatList.getString(j));
        }
        c.particleLayers.clear();
        if (tag.contains("particleLayers")) {
            final ListTag particleList = tag.getList("particleLayers", 10);
            for (int k = 0; k < particleList.size(); ++k) {
                c.particleLayers.add(ParticleLayer.load(particleList.getCompound(k)));
            }
        }
        else {
            c.particleLayers.addAll(ParticleLayer.defaults());
        }
        c.consumeKey = (!tag.contains("consumeKey") || tag.getBoolean("consumeKey"));
        c.cooldownSeconds = tag.getInt("cooldown");
        c.broadcast = tag.getBoolean("broadcast");
        c.allowSkip = (!tag.contains("allowSkip") || tag.getBoolean("allowSkip"));
        c.openDelayTicks = tag.getInt("openDelay");
        c.requiredPermission = tag.getString("permission");
        return c;
    }
    
    public double totalChance() {
        double total = 0.0;
        for (final RewardEntry r : this.rewards) {
            if (!r.guaranteed) {
                total += Math.max(0.0, r.chance);
            }
        }
        return total;
    }
    
    public double normalizedPercent(final RewardEntry entry) {
        if (entry.guaranteed) {
            return 100.0;
        }
        final double total = this.totalChance();
        return (total > 0.0) ? (Math.max(0.0, entry.chance) * 100.0 / total) : 0.0;
    }
    
    public String floatingTextJoined() {
        return String.join("\n", this.floatingText);
    }
    
    public void setFloatingText(final String multiline) {
        this.floatingText.clear();
        if (multiline == null) {
            return;
        }
        for (final String line : multiline.split("\n", -1)) {
            this.floatingText.add(line);
        }
        while (!this.floatingText.isEmpty() && this.floatingText.get(this.floatingText.size() - 1).isEmpty()) {
            this.floatingText.remove(this.floatingText.size() - 1);
        }
    }
    
    public CrateConfig copy() {
        return load(this.save());
    }
}
