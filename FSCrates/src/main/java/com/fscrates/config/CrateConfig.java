package com.fscrates.config;

import com.fscrates.animation.AnimationRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * The full, authoritative definition of a crate. Everything the GUI edits lives
 * here and round-trips to/from NBT so it can be embedded in the crate ItemStack
 * and managed by the server-side registry.
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

    // key
    public String keyName = "\u00A7e\u2726 Llave \u2726";
    public boolean keyGlint = true;
    public String keyLore = "Usa esta llave en su crate para abrirla.";
    public boolean consumeKey = true;

    // settings
    public int cooldownSeconds = 0;       // per-player cooldown
    public boolean broadcast = false;     // announce rare rewards to all
    public boolean allowSkip = true;      // SHIFT skips the animation
    public int openDelayTicks = 0;        // anti-spam delay before opening
    public String requiredPermission = "";// extra permission node (optional)

    public CrateConfig() {}

    public CrateConfig(String id) {
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

        tag.putString("keyName", keyName);
        tag.putBoolean("keyGlint", keyGlint);
        tag.putString("keyLore", keyLore);
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

        c.keyName = tag.contains("keyName") ? tag.getString("keyName") : "\u00A7e\u2726 Llave \u2726";
        c.keyGlint = !tag.contains("keyGlint") || tag.getBoolean("keyGlint");
        c.keyLore = tag.contains("keyLore") ? tag.getString("keyLore") : "Usa esta llave en su crate para abrirla.";
        c.consumeKey = !tag.contains("consumeKey") || tag.getBoolean("consumeKey");

        c.cooldownSeconds = tag.getInt("cooldown");
        c.broadcast = tag.getBoolean("broadcast");
        c.allowSkip = !tag.contains("allowSkip") || tag.getBoolean("allowSkip");
        c.openDelayTicks = tag.getInt("openDelay");
        c.requiredPermission = tag.getString("permission");
        return c;
    }

    public int totalWeight() {
        int total = 0;
        for (RewardEntry r : rewards) {
            if (!r.guaranteed) {
                total += Math.max(0, r.weight);
            }
        }
        return total;
    }

    public CrateConfig copy() {
        return load(this.save());
    }
}
