package com.fscrates.config;

import com.fscrates.animation.AnimationRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * The full, authoritative definition of a crate. Everything the GUI edits lives
 * here and round-trips to/from NBT so it can be embedded in the crate ItemStack,
 * stored in the placed BlockEntity and managed by the server-side registry.
 */
public class CrateConfig {

    // identity
    public String id = "nueva_crate";
    public String displayName = "\u00A7d\u2726 Crate \u2726";
    public Rarity rarity = Rarity.COMMON;

    // rewards
    public final List<RewardEntry> rewards = new ArrayList<>();
    /** how many reward rolls per open (besides guaranteed rewards). */
    public int rolls = 1;

    // animation
    public String animationId = AnimationRegistry.defaultId();

    // appearance
    public boolean glow = true;
    public boolean particles = true;
    public String nameColorHexOverride = ""; // empty = use rarity colour
    public boolean floatingName = true;
    /** Show each reward's drop chance (%) floating above the crate. */
    public boolean showOdds = false;
    /** Free hologram text rendered above the crate (one entry per line, may carry &-color codes). */
    public final List<String> floatingText = new ArrayList<>();
    /** Fully editable particle layers (see the Particles tab). */
    public final List<ParticleLayer> particleLayers = new ArrayList<>();

    // key behaviour (the key itself is a standard per-tier item)
    public boolean consumeKey = true;

    // settings
    public int cooldownSeconds = 0;       // per-player cooldown
    public boolean broadcast = false;     // announce rare rewards to all
    public boolean allowSkip = true;      // SHIFT skips the animation
    public int openDelayTicks = 0;        // anti-spam delay before opening
    public String requiredPermission = "";// extra permission node (optional)

    public CrateConfig() {
        particleLayers.addAll(ParticleLayer.defaults());
    }

    public CrateConfig(String id) {
        this();
        this.id = id;
    }

    // ------------------------------------------------------------------
    // Serialization
    // ------------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("displayName", displayName);
        tag.putString("rarity", rarity.name());
        tag.putInt("rolls", rolls);
        tag.putString("animationId", animationId);

        ListTag rewardList = new ListTag();
        for (RewardEntry r : rewards) {
            rewardList.add(r.save());
        }
        tag.put("rewards", rewardList);

        tag.putBoolean("glow", glow);
        tag.putBoolean("particles", particles);
        tag.putString("nameColorHex", nameColorHexOverride);
        tag.putBoolean("floatingName", floatingName);
        tag.putBoolean("showOdds", showOdds);

        ListTag floatList = new ListTag();
        for (String line : floatingText) {
            floatList.add(StringTag.valueOf(line));
        }
        tag.put("floatingText", floatList);

        ListTag particleList = new ListTag();
        for (ParticleLayer layer : particleLayers) {
            particleList.add(layer.save());
        }
        tag.put("particleLayers", particleList);

        tag.putBoolean("consumeKey", consumeKey);

        tag.putInt("cooldown", cooldownSeconds);
        tag.putBoolean("broadcast", broadcast);
        tag.putBoolean("allowSkip", allowSkip);
        tag.putInt("openDelay", openDelayTicks);
        tag.putString("permission", requiredPermission);
        return tag;
    }

    public static CrateConfig load(CompoundTag tag) {
        CrateConfig c = new CrateConfig();
        c.id = tag.contains("id") ? tag.getString("id") : "nueva_crate";
        c.displayName = tag.contains("displayName") ? tag.getString("displayName") : "\u00A7d\u2726 Crate \u2726";
        c.rarity = Rarity.byName(tag.getString("rarity"));
        c.rolls = tag.contains("rolls") ? Math.max(1, tag.getInt("rolls")) : 1;
        c.animationId = tag.contains("animationId") ? tag.getString("animationId") : AnimationRegistry.defaultId();
        if (!AnimationRegistry.exists(c.animationId)) {
            c.animationId = AnimationRegistry.defaultId();
        }

        c.rewards.clear();
        ListTag rewardList = tag.getList("rewards", Tag.TAG_COMPOUND);
        for (int i = 0; i < rewardList.size(); i++) {
            c.rewards.add(RewardEntry.load(rewardList.getCompound(i)));
        }

        c.glow = !tag.contains("glow") || tag.getBoolean("glow");
        c.particles = !tag.contains("particles") || tag.getBoolean("particles");
        c.nameColorHexOverride = tag.getString("nameColorHex");
        c.floatingName = !tag.contains("floatingName") || tag.getBoolean("floatingName");
        c.showOdds = tag.getBoolean("showOdds");

        c.floatingText.clear();
        ListTag floatList = tag.getList("floatingText", Tag.TAG_STRING);
        for (int i = 0; i < floatList.size(); i++) {
            c.floatingText.add(floatList.getString(i));
        }

        c.particleLayers.clear();
        if (tag.contains("particleLayers")) {
            ListTag particleList = tag.getList("particleLayers", Tag.TAG_COMPOUND);
            for (int i = 0; i < particleList.size(); i++) {
                c.particleLayers.add(ParticleLayer.load(particleList.getCompound(i)));
            }
        } else {
            // older config / fresh crate: seed with nice defaults
            c.particleLayers.addAll(ParticleLayer.defaults());
        }

        c.consumeKey = !tag.contains("consumeKey") || tag.getBoolean("consumeKey");

        c.cooldownSeconds = tag.getInt("cooldown");
        c.broadcast = tag.getBoolean("broadcast");
        c.allowSkip = !tag.contains("allowSkip") || tag.getBoolean("allowSkip");
        c.openDelayTicks = tag.getInt("openDelay");
        c.requiredPermission = tag.getString("permission");
        return c;
    }

    /** Sum of all non-guaranteed reward chances (used to normalise to 100%). */
    public double totalChance() {
        double total = 0;
        for (RewardEntry r : rewards) {
            if (!r.guaranteed) {
                total += Math.max(0, r.chance);
            }
        }
        return total;
    }

    /** The normalised probability (0-100) of a given reward in one roll. */
    public double normalizedPercent(RewardEntry entry) {
        if (entry.guaranteed) {
            return 100.0;
        }
        double total = totalChance();
        return total > 0 ? Math.max(0, entry.chance) * 100.0 / total : 0.0;
    }

    /** Multi-line floating text joined as a single editable string. */
    public String floatingTextJoined() {
        return String.join("\n", floatingText);
    }

    public void setFloatingText(String multiline) {
        floatingText.clear();
        if (multiline == null) {
            return;
        }
        for (String line : multiline.split("\n", -1)) {
            floatingText.add(line);
        }
        // drop a single trailing empty line so an empty editor = no hologram
        while (!floatingText.isEmpty() && floatingText.get(floatingText.size() - 1).isEmpty()) {
            floatingText.remove(floatingText.size() - 1);
        }
    }

    public CrateConfig copy() {
        return load(this.save());
    }
}
