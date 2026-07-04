package com.fscrates.config;

import com.fscrates.animation.AnimationRegistry;
import com.fscrates.config.ParticleLayer;
import com.fscrates.config.Rarity;
import com.fscrates.config.RewardEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class CrateConfig {
    public String id = "nueva_crate";
    public String displayName = "\u00a7d\u2726 Crate \u2726";
    public Rarity rarity = Rarity.COMMON;
    public String styleId = "";
    public final List<RewardEntry> rewards = new ArrayList<RewardEntry>();
    public int rolls = 1;
    public String animationId = AnimationRegistry.defaultId();
    public boolean glow = true;
    public boolean particles = true;
    public String nameColorHexOverride = "";
    public boolean floatingName = true;
    public boolean showOdds = false;
    public final List<String> floatingText = new ArrayList<String>();
    public final List<ParticleLayer> particleLayers = new ArrayList<ParticleLayer>();
    public boolean consumeKey = true;
    public int cooldownSeconds = 0;
    public boolean broadcast = false;
    public boolean allowSkip = true;
    public int openDelayTicks = 0;
    public String requiredPermission = "";
    public float sizeScale = 1.0f;
    public float yOffset = 0.0f;
    public float yawOffset = 0.0f;
    public boolean openOncePerPlayer = false;

    public CrateConfig() {
        this.particleLayers.addAll(ParticleLayer.defaults());
    }

    public CrateConfig(String id) {
        this();
        this.id = id;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", this.id);
        tag.putString("displayName", this.displayName);
        tag.putString("rarity", this.rarity.name());
        tag.putString("styleId", this.styleId);
        tag.putInt("rolls", this.rolls);
        tag.putString("animationId", this.animationId);
        ListTag rewardList = new ListTag();
        for (RewardEntry rewardEntry : this.rewards) {
            rewardList.add(rewardEntry.save());
        }
        tag.put("rewards", (Tag)rewardList);
        tag.putBoolean("glow", this.glow);
        tag.putBoolean("particles", this.particles);
        tag.putString("nameColorHex", this.nameColorHexOverride);
        tag.putBoolean("floatingName", this.floatingName);
        tag.putBoolean("showOdds", this.showOdds);
        ListTag floatList = new ListTag();
        for (String line : this.floatingText) {
            floatList.add(StringTag.valueOf((String)line));
        }
        tag.put("floatingText", (Tag)floatList);
        ListTag listTag = new ListTag();
        for (ParticleLayer layer : this.particleLayers) {
            listTag.add(layer.save());
        }
        tag.put("particleLayers", (Tag)listTag);
        tag.putBoolean("consumeKey", this.consumeKey);
        tag.putInt("cooldown", this.cooldownSeconds);
        tag.putBoolean("broadcast", this.broadcast);
        tag.putBoolean("allowSkip", this.allowSkip);
        tag.putInt("openDelay", this.openDelayTicks);
        tag.putString("permission", this.requiredPermission);
        tag.putFloat("sizeScale", this.sizeScale);
        tag.putFloat("yOffset", this.yOffset);
        tag.putFloat("yawOffset", this.yawOffset);
        tag.putBoolean("openOncePerPlayer", this.openOncePerPlayer);
        return tag;
    }

    public static CrateConfig load(CompoundTag tag) {
        CrateConfig c = new CrateConfig();
        c.id = tag.contains("id") ? tag.getString("id") : "nueva_crate";
        c.displayName = tag.contains("displayName") ? tag.getString("displayName") : "\u00a7d\u2726 Crate \u2726";
        c.rarity = Rarity.byName(tag.getString("rarity"));
        c.styleId = tag.getString("styleId");
        c.rolls = tag.contains("rolls") ? Math.max(1, tag.getInt("rolls")) : 1;
        String string = c.animationId = tag.contains("animationId") ? tag.getString("animationId") : AnimationRegistry.defaultId();
        if (!AnimationRegistry.exists(c.animationId)) {
            c.animationId = AnimationRegistry.defaultId();
        }
        c.rewards.clear();
        ListTag rewardList = tag.getList("rewards", 10);
        for (int i = 0; i < rewardList.size(); ++i) {
            c.rewards.add(RewardEntry.load(rewardList.getCompound(i)));
        }
        c.glow = !tag.contains("glow") || tag.getBoolean("glow");
        c.particles = !tag.contains("particles") || tag.getBoolean("particles");
        c.nameColorHexOverride = tag.getString("nameColorHex");
        c.floatingName = !tag.contains("floatingName") || tag.getBoolean("floatingName");
        c.showOdds = tag.getBoolean("showOdds");
        c.floatingText.clear();
        ListTag floatList = tag.getList("floatingText", 8);
        for (int j = 0; j < floatList.size(); ++j) {
            c.floatingText.add(floatList.getString(j));
        }
        c.particleLayers.clear();
        if (tag.contains("particleLayers")) {
            ListTag particleList = tag.getList("particleLayers", 10);
            for (int k = 0; k < particleList.size(); ++k) {
                c.particleLayers.add(ParticleLayer.load(particleList.getCompound(k)));
            }
        } else {
            c.particleLayers.addAll(ParticleLayer.defaults());
        }
        c.consumeKey = !tag.contains("consumeKey") || tag.getBoolean("consumeKey");
        c.cooldownSeconds = tag.getInt("cooldown");
        c.broadcast = tag.getBoolean("broadcast");
        c.allowSkip = !tag.contains("allowSkip") || tag.getBoolean("allowSkip");
        c.openDelayTicks = tag.getInt("openDelay");
        c.requiredPermission = tag.getString("permission");
        c.sizeScale = tag.contains("sizeScale") ? tag.getFloat("sizeScale") : 1.0f;
        c.yOffset = tag.getFloat("yOffset");
        c.yawOffset = tag.getFloat("yawOffset");
        c.openOncePerPlayer = tag.getBoolean("openOncePerPlayer");
        return c;
    }

    public double totalChance() {
        double total = 0.0;
        for (RewardEntry r : this.rewards) {
            if (r.guaranteed) continue;
            total += Math.max(0.0, r.chance);
        }
        return total;
    }

    public double normalizedPercent(RewardEntry entry) {
        if (entry.guaranteed) {
            return 100.0;
        }
        double total = this.totalChance();
        return total > 0.0 ? Math.max(0.0, entry.chance) * 100.0 / total : 0.0;
    }

    public String floatingTextJoined() {
        return String.join((CharSequence)"\n", this.floatingText);
    }

    public void setFloatingText(String multiline) {
        this.floatingText.clear();
        if (multiline != null) {
            for (String line : multiline.split("\n", -1)) {
                this.floatingText.add(line);
            }
            while (!this.floatingText.isEmpty() && this.floatingText.get(this.floatingText.size() - 1).isEmpty()) {
                this.floatingText.remove(this.floatingText.size() - 1);
            }
        }
    }

    public CrateConfig copy() {
        return CrateConfig.load(this.save());
    }
}

