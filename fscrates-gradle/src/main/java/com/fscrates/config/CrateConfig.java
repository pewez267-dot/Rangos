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
        tag.m_128359_("id", this.id);
        tag.m_128359_("displayName", this.displayName);
        tag.m_128359_("rarity", this.rarity.name());
        tag.m_128405_("rolls", this.rolls);
        tag.m_128359_("animationId", this.animationId);
        final ListTag rewardList = new ListTag();
        for (final RewardEntry r : this.rewards) {
            rewardList.add((Object)r.save());
        }
        tag.m_128365_("rewards", (Tag)rewardList);
        tag.m_128379_("glow", this.glow);
        tag.m_128379_("particles", this.particles);
        tag.m_128359_("nameColorHex", this.nameColorHexOverride);
        tag.m_128379_("floatingName", this.floatingName);
        tag.m_128379_("showOdds", this.showOdds);
        final ListTag floatList = new ListTag();
        for (final String line : this.floatingText) {
            floatList.add((Object)StringTag.m_129297_(line));
        }
        tag.m_128365_("floatingText", (Tag)floatList);
        final ListTag particleList = new ListTag();
        for (final ParticleLayer layer : this.particleLayers) {
            particleList.add((Object)layer.save());
        }
        tag.m_128365_("particleLayers", (Tag)particleList);
        tag.m_128379_("consumeKey", this.consumeKey);
        tag.m_128405_("cooldown", this.cooldownSeconds);
        tag.m_128379_("broadcast", this.broadcast);
        tag.m_128379_("allowSkip", this.allowSkip);
        tag.m_128405_("openDelay", this.openDelayTicks);
        tag.m_128359_("permission", this.requiredPermission);
        return tag;
    }
    
    public static CrateConfig load(final CompoundTag tag) {
        final CrateConfig c = new CrateConfig();
        c.id = (tag.m_128441_("id") ? tag.m_128461_("id") : "nueva_crate");
        c.displayName = (tag.m_128441_("displayName") ? tag.m_128461_("displayName") : "§d\u2726 Crate \u2726");
        c.rarity = Rarity.byName(tag.m_128461_("rarity"));
        c.rolls = (tag.m_128441_("rolls") ? Math.max(1, tag.m_128451_("rolls")) : 1);
        c.animationId = (tag.m_128441_("animationId") ? tag.m_128461_("animationId") : AnimationRegistry.defaultId());
        if (!AnimationRegistry.exists(c.animationId)) {
            c.animationId = AnimationRegistry.defaultId();
        }
        c.rewards.clear();
        final ListTag rewardList = tag.m_128437_("rewards", 10);
        for (int i = 0; i < rewardList.size(); ++i) {
            c.rewards.add(RewardEntry.load(rewardList.m_128728_(i)));
        }
        c.glow = (!tag.m_128441_("glow") || tag.m_128471_("glow"));
        c.particles = (!tag.m_128441_("particles") || tag.m_128471_("particles"));
        c.nameColorHexOverride = tag.m_128461_("nameColorHex");
        c.floatingName = (!tag.m_128441_("floatingName") || tag.m_128471_("floatingName"));
        c.showOdds = tag.m_128471_("showOdds");
        c.floatingText.clear();
        final ListTag floatList = tag.m_128437_("floatingText", 8);
        for (int j = 0; j < floatList.size(); ++j) {
            c.floatingText.add(floatList.m_128778_(j));
        }
        c.particleLayers.clear();
        if (tag.m_128441_("particleLayers")) {
            final ListTag particleList = tag.m_128437_("particleLayers", 10);
            for (int k = 0; k < particleList.size(); ++k) {
                c.particleLayers.add(ParticleLayer.load(particleList.m_128728_(k)));
            }
        }
        else {
            c.particleLayers.addAll(ParticleLayer.defaults());
        }
        c.consumeKey = (!tag.m_128441_("consumeKey") || tag.m_128471_("consumeKey"));
        c.cooldownSeconds = tag.m_128451_("cooldown");
        c.broadcast = tag.m_128471_("broadcast");
        c.allowSkip = (!tag.m_128441_("allowSkip") || tag.m_128471_("allowSkip"));
        c.openDelayTicks = tag.m_128451_("openDelay");
        c.requiredPermission = tag.m_128461_("permission");
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
