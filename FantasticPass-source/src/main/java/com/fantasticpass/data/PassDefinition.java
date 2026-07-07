/*
 * Decompiled with CFR 0.152.
 */
package com.fantasticpass.data;

import com.fantasticpass.data.TierDefinition;
import com.fantasticpass.quest.DefaultQuests;
import com.fantasticpass.quest.Quest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

public final class PassDefinition {
    public static final int MAX_TIERS = 999;
    public static final int MAX_WEEKS = 52;
    private String id;
    private String name;
    private final List<TierDefinition> tiers = new ArrayList<TierDefinition>();
    private int minutesPerTierOverride;
    private int tierCount = 100;
    private int dailyFreeCount;
    private int dailyPremiumCount;
    private int weekCountOverride;
    private int weeklyFreeCount;
    private int weeklyPremiumCount;
    private int pointsPerTierOverride;
    private final List<Quest> customDailyFree = new ArrayList<Quest>();
    private final List<Quest> customDailyPremium = new ArrayList<Quest>();
    private final List<List<Quest>> customWeeksFree = new ArrayList<List<Quest>>();
    private final List<List<Quest>> customWeeksPremium = new ArrayList<List<Quest>>();
    private final List<String> musicUrls = new ArrayList<String>();
    private final List<String> backgroundUrls = new ArrayList<String>();
    private int backgroundIntervalSeconds = 12;

    public PassDefinition(String id, String name) {
        this.id = id == null ? "" : id;
        this.name = name == null ? "" : name;
        this.minutesPerTierOverride = 0;
        this.ensureTiers(100);
    }

    private void ensureTiers(int count) {
        while (this.tiers.size() < count) {
            this.tiers.add(new TierDefinition(this.tiers.size() + 1));
        }
    }

    public int getTierCount() {
        return this.tierCount < 1 ? 100 : Math.min(999, this.tierCount);
    }

    public void setTierCount(int tierCount) {
        this.tierCount = Math.max(1, Math.min(999, tierCount));
        this.ensureTiers(this.tierCount);
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id == null ? "" : id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public int getMinutesPerTierOverride() {
        return this.minutesPerTierOverride;
    }

    public void setMinutesPerTierOverride(int minutesPerTierOverride) {
        this.minutesPerTierOverride = minutesPerTierOverride;
    }

    public int getDailyFreeCount() {
        return this.dailyFreeCount;
    }

    public void setDailyFreeCount(int count) {
        this.dailyFreeCount = Math.max(0, Math.min(12, count));
    }

    public int getDailyPremiumCount() {
        return this.dailyPremiumCount;
    }

    public void setDailyPremiumCount(int count) {
        this.dailyPremiumCount = Math.max(0, Math.min(12, count));
    }

    public int getWeekCountOverride() {
        return this.weekCountOverride;
    }

    public void setWeekCountOverride(int count) {
        this.weekCountOverride = Math.max(0, Math.min(52, count));
    }

    public int getWeeklyFreeCount() {
        return this.weeklyFreeCount;
    }

    public void setWeeklyFreeCount(int count) {
        this.weeklyFreeCount = Math.max(0, Math.min(25, count));
    }

    public int getWeeklyPremiumCount() {
        return this.weeklyPremiumCount;
    }

    public void setWeeklyPremiumCount(int count) {
        this.weeklyPremiumCount = Math.max(0, Math.min(25, count));
    }

    public int getPointsPerTierOverride() {
        return this.pointsPerTierOverride;
    }

    public void setPointsPerTierOverride(int points) {
        this.pointsPerTierOverride = Math.max(0, Math.min(1000000, points));
    }

    public List<TierDefinition> getTiers() {
        return this.tiers;
    }

    public TierDefinition getTier(int tierNumber) {
        if (tierNumber < 1 || tierNumber > 999) {
            return null;
        }
        this.ensureTiers(tierNumber);
        return this.tiers.get(tierNumber - 1);
    }

    public void setTier(int tierNumber, TierDefinition definition) {
        if (tierNumber >= 1 && tierNumber <= 999 && definition != null) {
            this.ensureTiers(tierNumber);
            this.tiers.set(tierNumber - 1, definition);
        }
    }

    public List<Quest> getCustomDailyFree() {
        return this.customDailyFree;
    }

    public List<String> getMusicUrls() {
        return this.musicUrls;
    }

    public List<String> getBackgroundUrls() {
        return this.backgroundUrls;
    }

    public int getBackgroundIntervalSeconds() {
        return this.backgroundIntervalSeconds;
    }

    public void setBackgroundIntervalSeconds(int seconds) {
        this.backgroundIntervalSeconds = Math.max(2, Math.min(3600, seconds));
    }

    public List<Quest> getCustomDailyPremium() {
        return this.customDailyPremium;
    }

    public List<Quest> getCustomWeekFree(int week) {
        while (this.customWeeksFree.size() < week) {
            this.customWeeksFree.add(new ArrayList());
        }
        return this.customWeeksFree.get(week - 1);
    }

    public List<Quest> getCustomWeekPremium(int week) {
        while (this.customWeeksPremium.size() < week) {
            this.customWeeksPremium.add(new ArrayList());
        }
        return this.customWeeksPremium.get(week - 1);
    }

    public List<Quest> dailyFreePool() {
        return this.customDailyFree.isEmpty() ? DefaultQuests.DAILY_FREE_POOL : this.customDailyFree;
    }

    public List<Quest> dailyPremiumPool() {
        return this.customDailyPremium.isEmpty() ? DefaultQuests.DAILY_PREMIUM_POOL : this.customDailyPremium;
    }

    public List<Quest> weekFreeQuests(int week) {
        List<Quest> base = week >= 1 && week <= this.customWeeksFree.size() && !this.customWeeksFree.get(week - 1).isEmpty() ? this.customWeeksFree.get(week - 1) : DefaultQuests.weekQuestsCyclic(week);
        return PassDefinition.trimWeekly(base, this.weeklyFreeCount);
    }

    public List<Quest> weekPremiumQuests(int week) {
        List<Quest> base = week >= 1 && week <= this.customWeeksPremium.size() && !this.customWeeksPremium.get(week - 1).isEmpty() ? this.customWeeksPremium.get(week - 1) : DefaultQuests.premiumWeekQuestsCyclic(week);
        return PassDefinition.trimWeekly(base, this.weeklyPremiumCount);
    }

    private static List<Quest> trimWeekly(List<Quest> base, int count) {
        int n = count > 0 ? Math.min(count, base.size()) : base.size();
        return new ArrayList<Quest>(base.subList(0, n));
    }

    public Quest resolveQuest(String questId) {
        if (questId == null) {
            return null;
        }
        Quest q = PassDefinition.find(this.customDailyFree, questId);
        if (q == null) {
            q = PassDefinition.find(this.customDailyPremium, questId);
        }
        for (List<Quest> w : this.customWeeksFree) {
            if (q != null) break;
            q = PassDefinition.find(w, questId);
        }
        for (List<Quest> w : this.customWeeksPremium) {
            if (q != null) break;
            q = PassDefinition.find(w, questId);
        }
        return q != null ? q : DefaultQuests.byId(questId);
    }

    private static Quest find(List<Quest> list, String id) {
        for (Quest q : list) {
            if (!q.getId().equals(id)) continue;
            return q;
        }
        return null;
    }

    public PassDefinition copy() {
        PassDefinition copy = new PassDefinition(this.id, this.name);
        copy.minutesPerTierOverride = this.minutesPerTierOverride;
        copy.tierCount = this.tierCount;
        copy.dailyFreeCount = this.dailyFreeCount;
        copy.dailyPremiumCount = this.dailyPremiumCount;
        copy.weekCountOverride = this.weekCountOverride;
        copy.weeklyFreeCount = this.weeklyFreeCount;
        copy.weeklyPremiumCount = this.weeklyPremiumCount;
        copy.pointsPerTierOverride = this.pointsPerTierOverride;
        copy.tiers.clear();
        for (TierDefinition tier : this.tiers) {
            copy.tiers.add(tier.copy());
        }
        PassDefinition.copyQuests(this.customDailyFree, copy.customDailyFree);
        PassDefinition.copyQuests(this.customDailyPremium, copy.customDailyPremium);
        PassDefinition.copyWeeks(this.customWeeksFree, copy.customWeeksFree);
        PassDefinition.copyWeeks(this.customWeeksPremium, copy.customWeeksPremium);
        copy.musicUrls.clear();
        copy.musicUrls.addAll(this.musicUrls);
        copy.backgroundUrls.clear();
        copy.backgroundUrls.addAll(this.backgroundUrls);
        copy.backgroundIntervalSeconds = this.backgroundIntervalSeconds;
        return copy;
    }

    private static void copyQuests(List<Quest> src, List<Quest> dst) {
        dst.clear();
        dst.addAll(src);
    }

    private static void copyWeeks(List<List<Quest>> src, List<List<Quest>> dst) {
        dst.clear();
        for (List<Quest> w : src) {
            dst.add(new ArrayList<Quest>(w));
        }
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", this.id);
        tag.putString("name", this.name);
        tag.putInt("minutesPerTierOverride", this.minutesPerTierOverride);
        tag.putInt("tierCount", this.tierCount);
        tag.putInt("dailyFreeCount", this.dailyFreeCount);
        tag.putInt("dailyPremiumCount", this.dailyPremiumCount);
        tag.putInt("weekCountOverride", this.weekCountOverride);
        tag.putInt("weeklyFreeCount", this.weeklyFreeCount);
        tag.putInt("weeklyPremiumCount", this.weeklyPremiumCount);
        tag.putInt("pointsPerTier", this.pointsPerTierOverride);
        ListTag list = new ListTag();
        for (TierDefinition tierDefinition : this.tiers) {
            list.add(tierDefinition.toNbt());
        }
        tag.put("tiers", (Tag)list);
        tag.put("cDailyFree", (Tag)PassDefinition.questsToNbt(this.customDailyFree));
        tag.put("cDailyPrem", (Tag)PassDefinition.questsToNbt(this.customDailyPremium));
        tag.put("cWeeksFree", (Tag)PassDefinition.weeksToNbt(this.customWeeksFree));
        tag.put("cWeeksPrem", (Tag)PassDefinition.weeksToNbt(this.customWeeksPremium));
        ListTag music = new ListTag();
        for (String url : this.musicUrls) {
            music.add(StringTag.valueOf((String)url));
        }
        tag.put("musicUrls", (Tag)music);
        ListTag listTag = new ListTag();
        for (String url : this.backgroundUrls) {
            listTag.add(StringTag.valueOf((String)url));
        }
        tag.put("backgroundUrls", (Tag)listTag);
        tag.putInt("bgInterval", this.backgroundIntervalSeconds);
        return tag;
    }

    public static PassDefinition fromNbt(CompoundTag tag) {
        PassDefinition pass = new PassDefinition(tag.getString("id"), tag.getString("name"));
        pass.minutesPerTierOverride = tag.getInt("minutesPerTierOverride");
        pass.tierCount = tag.contains("tierCount") ? tag.getInt("tierCount") : 100;
        pass.dailyFreeCount = tag.getInt("dailyFreeCount");
        pass.dailyPremiumCount = tag.getInt("dailyPremiumCount");
        pass.weekCountOverride = tag.getInt("weekCountOverride");
        pass.weeklyFreeCount = tag.getInt("weeklyFreeCount");
        pass.weeklyPremiumCount = tag.getInt("weeklyPremiumCount");
        pass.pointsPerTierOverride = tag.getInt("pointsPerTier");
        ListTag list = tag.getList("tiers", 10);
        for (int i = 0; i < list.size(); ++i) {
            TierDefinition tier = TierDefinition.fromNbt(list.getCompound(i));
            int idx = tier.getTierNumber() - 1;
            if (idx < 0 || idx >= 999) continue;
            pass.ensureTiers(idx + 1);
            pass.tiers.set(idx, tier);
        }
        pass.ensureTiers(pass.getTierCount());
        PassDefinition.questsFromNbt(tag.getList("cDailyFree", 10), pass.customDailyFree);
        PassDefinition.questsFromNbt(tag.getList("cDailyPrem", 10), pass.customDailyPremium);
        PassDefinition.weeksFromNbt(tag.getList("cWeeksFree", 9), pass.customWeeksFree);
        PassDefinition.weeksFromNbt(tag.getList("cWeeksPrem", 9), pass.customWeeksPremium);
        ListTag music = tag.getList("musicUrls", 8);
        pass.musicUrls.clear();
        for (int i = 0; i < music.size(); ++i) {
            pass.musicUrls.add(music.getString(i));
        }
        ListTag backgrounds = tag.getList("backgroundUrls", 8);
        pass.backgroundUrls.clear();
        for (int i = 0; i < backgrounds.size(); ++i) {
            pass.backgroundUrls.add(backgrounds.getString(i));
        }
        pass.setBackgroundIntervalSeconds(tag.contains("bgInterval") ? tag.getInt("bgInterval") : 12);
        return pass;
    }

    private static ListTag questsToNbt(List<Quest> quests) {
        ListTag list = new ListTag();
        for (Quest q : quests) {
            list.add(q.toNbt());
        }
        return list;
    }

    private static void questsFromNbt(ListTag list, List<Quest> target) {
        target.clear();
        for (int i = 0; i < list.size(); ++i) {
            target.add(Quest.fromNbt(list.getCompound(i)));
        }
    }

    private static ListTag weeksToNbt(List<List<Quest>> weeks) {
        ListTag list = new ListTag();
        for (List<Quest> week : weeks) {
            list.add(PassDefinition.questsToNbt(week));
        }
        return list;
    }

    private static void weeksFromNbt(ListTag list, List<List<Quest>> target) {
        target.clear();
        for (int i = 0; i < list.size(); ++i) {
            Tag t = list.get(i);
            ArrayList<Quest> week = new ArrayList<Quest>();
            if (t instanceof ListTag) {
                ListTag inner = (ListTag)t;
                PassDefinition.questsFromNbt(inner, week);
            }
            target.add(week);
        }
    }

    public void toBuf(FriendlyByteBuf buf) {
        buf.writeUtf(this.id);
        buf.writeUtf(this.name);
        buf.writeVarInt(this.minutesPerTierOverride);
        buf.writeVarInt(this.tierCount);
        buf.writeVarInt(this.dailyFreeCount);
        buf.writeVarInt(this.dailyPremiumCount);
        buf.writeVarInt(this.weekCountOverride);
        buf.writeVarInt(this.weeklyFreeCount);
        buf.writeVarInt(this.weeklyPremiumCount);
        buf.writeVarInt(this.pointsPerTierOverride);
        buf.writeVarInt(this.tiers.size());
        for (TierDefinition tier : this.tiers) {
            tier.toBuf(buf);
        }
        PassDefinition.questsToBuf(buf, this.customDailyFree);
        PassDefinition.questsToBuf(buf, this.customDailyPremium);
        PassDefinition.weeksToBuf(buf, this.customWeeksFree);
        PassDefinition.weeksToBuf(buf, this.customWeeksPremium);
        buf.writeVarInt(this.musicUrls.size());
        for (String url : this.musicUrls) {
            buf.writeUtf(url);
        }
        buf.writeVarInt(this.backgroundUrls.size());
        for (String url : this.backgroundUrls) {
            buf.writeUtf(url);
        }
        buf.writeVarInt(this.backgroundIntervalSeconds);
    }

    public static PassDefinition fromBuf(FriendlyByteBuf buf) {
        PassDefinition pass = new PassDefinition(buf.readUtf(), buf.readUtf());
        pass.minutesPerTierOverride = buf.readVarInt();
        pass.tierCount = buf.readVarInt();
        pass.dailyFreeCount = buf.readVarInt();
        pass.dailyPremiumCount = buf.readVarInt();
        pass.weekCountOverride = buf.readVarInt();
        pass.weeklyFreeCount = buf.readVarInt();
        pass.weeklyPremiumCount = buf.readVarInt();
        pass.pointsPerTierOverride = buf.readVarInt();
        int tierN = buf.readVarInt();
        pass.tiers.clear();
        for (int i = 0; i < tierN; ++i) {
            TierDefinition tier = TierDefinition.fromBuf(buf);
            pass.tiers.add(tier);
        }
        pass.ensureTiers(pass.getTierCount());
        PassDefinition.questsFromBuf(buf, pass.customDailyFree);
        PassDefinition.questsFromBuf(buf, pass.customDailyPremium);
        PassDefinition.weeksFromBuf(buf, pass.customWeeksFree);
        PassDefinition.weeksFromBuf(buf, pass.customWeeksPremium);
        int musicN = buf.readVarInt();
        pass.musicUrls.clear();
        for (int i = 0; i < musicN; ++i) {
            pass.musicUrls.add(buf.readUtf());
        }
        int bgN = buf.readVarInt();
        pass.backgroundUrls.clear();
        for (int i = 0; i < bgN; ++i) {
            pass.backgroundUrls.add(buf.readUtf());
        }
        pass.setBackgroundIntervalSeconds(buf.readVarInt());
        return pass;
    }

    private static void questsToBuf(FriendlyByteBuf buf, List<Quest> quests) {
        buf.writeVarInt(quests.size());
        for (Quest q : quests) {
            q.toBuf(buf);
        }
    }

    private static void questsFromBuf(FriendlyByteBuf buf, List<Quest> target) {
        target.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; ++i) {
            target.add(Quest.fromBuf(buf));
        }
    }

    private static void weeksToBuf(FriendlyByteBuf buf, List<List<Quest>> weeks) {
        buf.writeVarInt(weeks.size());
        for (List<Quest> week : weeks) {
            PassDefinition.questsToBuf(buf, week);
        }
    }

    private static void weeksFromBuf(FriendlyByteBuf buf, List<List<Quest>> target) {
        target.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; ++i) {
            ArrayList<Quest> week = new ArrayList<Quest>();
            PassDefinition.questsFromBuf(buf, week);
            target.add(week);
        }
    }
}

