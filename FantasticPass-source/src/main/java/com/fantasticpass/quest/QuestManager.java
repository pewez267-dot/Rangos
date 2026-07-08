/*
 * Decompiled with CFR 0.152.
 */
package com.fantasticpass.quest;

import com.fantasticpass.config.PassConfig;
import com.fantasticpass.data.PassDefinition;
import com.fantasticpass.data.PassSavedData;
import com.fantasticpass.data.PlayerPassData;
import com.fantasticpass.network.PacketHandler;
import com.fantasticpass.network.QuestCompletePacket;
import com.fantasticpass.quest.DefaultQuests;
import com.fantasticpass.quest.Quest;
import com.fantasticpass.quest.QuestType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class QuestManager {
    private QuestManager() {
    }

    public static int pointsPerTier() {
        int v = (Integer)PassConfig.POINTS_PER_TIER.get();
        return v <= 0 ? 100 : v;
    }

    public static int pointsPerTier(PassDefinition pass) {
        int override = pass == null ? 0 : pass.getPointsPerTierOverride();
        return override > 0 ? override : QuestManager.pointsPerTier();
    }

    public static long today() {
        return System.currentTimeMillis() / 86400000L;
    }

    private static PassDefinition activePass() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : PassSavedData.get(server).getActivePass();
    }

    public static int dailyFreeCount() {
        PassDefinition pass = QuestManager.activePass();
        int override = pass == null ? 0 : pass.getDailyFreeCount();
        return override > 0 ? override : (Integer)PassConfig.DAILY_FREE_COUNT.get();
    }

    public static int dailyPremiumCount() {
        PassDefinition pass = QuestManager.activePass();
        int override = pass == null ? 0 : pass.getDailyPremiumCount();
        return override > 0 ? override : (Integer)PassConfig.DAILY_PREMIUM_COUNT.get();
    }

    public static void ensureDaily(UUID uuid, PlayerPassData data) {
        long today = QuestManager.today();
        PassDefinition pass = QuestManager.activePass();
        boolean stale = data.getDailyQuestIds().isEmpty();
        if (!stale) {
            for (String id : data.getDailyQuestIds()) {
                if (QuestManager.resolve(pass, id) != null) continue;
                stale = true;
                break;
            }
        }
        // Numero de diarias que DEBEN existir segun la config actual (acotado al tamano del pool).
        List<Quest> freePool = pass != null ? pass.dailyFreePool() : DefaultQuests.DAILY_FREE_POOL;
        int expected = Math.min(QuestManager.dailyFreeCount(), freePool.size());
        if (data.isPremium()) {
            List<Quest> premPool = pass != null ? pass.dailyPremiumPool() : DefaultQuests.DAILY_PREMIUM_POOL;
            expected += Math.min(QuestManager.dailyPremiumCount(), premPool.size());
        }
        // Auto-arreglo: si el jugador tiene MAS diarias de las que corresponden (por el bug de inflado
        // de versiones anteriores, o si bajaste el numero), re-sorteamos al numero correcto.
        boolean wrongCount = data.getDailyQuestIds().size() != expected;
        if (data.getDailyResetDay() != today || stale || wrongCount) {
            data.resetDaily(QuestManager.rollDaily(uuid, today, data.isPremium()), today);
        }
    }

    private static Quest resolve(PassDefinition pass, String id) {
        return pass != null ? pass.resolveQuest(id) : DefaultQuests.byId(id);
    }

    public static void rerollDaily(UUID uuid, PlayerPassData data) {
        data.resetDaily(QuestManager.rollDaily(uuid, QuestManager.today(), data.isPremium()), QuestManager.today());
    }

    private static List<String> rollDaily(UUID uuid, long day, boolean premium) {
        PassDefinition pass = QuestManager.activePass();
        List<Quest> freePool = pass != null ? pass.dailyFreePool() : DefaultQuests.DAILY_FREE_POOL;
        List<Quest> premPool = pass != null ? pass.dailyPremiumPool() : DefaultQuests.DAILY_PREMIUM_POOL;
        ArrayList<String> ids = new ArrayList<String>();
        QuestManager.pickInto(ids, new ArrayList<Quest>(freePool), uuid, day, QuestManager.dailyFreeCount(), 1L);
        if (premium) {
            QuestManager.pickInto(ids, new ArrayList<Quest>(premPool), uuid, day, QuestManager.dailyPremiumCount(), 31L);
        }
        return ids;
    }

    public static List<Quest> previewPremiumDaily(PassDefinition pass, UUID uuid, int count) {
        List<Quest> pool = pass != null ? pass.dailyPremiumPool() : DefaultQuests.DAILY_PREMIUM_POOL;
        ArrayList<String> ids = new ArrayList<String>();
        QuestManager.pickInto(ids, new ArrayList<Quest>(pool), uuid, QuestManager.today(), count, 31L);
        ArrayList<Quest> out = new ArrayList<Quest>();
        for (String id : ids) {
            Quest q = QuestManager.resolve(pass, id);
            if (q == null) continue;
            out.add(q);
        }
        return out;
    }

    private static void pickInto(List<String> out, List<Quest> pool, UUID uuid, long day, int count, long salt) {
        Random rng = new Random(day * 1099511628211L ^ uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits() ^ salt * 2654435761L);
        Collections.shuffle(pool, rng);
        for (int i = 0; i < Math.min(Math.max(0, count), pool.size()); ++i) {
            out.add(pool.get(i).getId());
        }
    }

    public static List<Quest> activeDaily(PlayerPassData data) {
        return QuestManager.activeDaily(QuestManager.activePass(), data);
    }

    public static List<Quest> activeDaily(PassDefinition pass, PlayerPassData data) {
        ArrayList<Quest> list = new ArrayList<Quest>();
        for (String id : data.getDailyQuestIds()) {
            Quest q = QuestManager.resolve(pass, id);
            if (q == null) continue;
            list.add(q);
        }
        return list;
    }

    public static boolean recomputeTier(PlayerPassData data) {
        PassDefinition pass = QuestManager.activePass();
        int maxTier = pass == null ? 100 : pass.getTierCount();
        int newTier = Math.max(0, Math.min(maxTier, data.getPoints() / QuestManager.pointsPerTier(pass)));
        if (newTier > data.getCurrentTier()) {
            data.setCurrentTier(newTier);
            return true;
        }
        return false;
    }

    public static boolean track(ServerPlayer player, PlayerPassData data, QuestType type, int amount) {
        return QuestManager.track(player, data, type, "", amount);
    }

    public static boolean track(ServerPlayer player, PlayerPassData data, QuestType type, String param, int amount) {
        if (amount <= 0) {
            return false;
        }
        QuestManager.ensureDaily(player.getUUID(), data);
        PassDefinition pass = QuestManager.activePass();
        boolean tierChanged = false;
        for (Quest q : QuestManager.activeDaily(pass, data)) {
            tierChanged |= QuestManager.progress(player, data, q, type, param, amount);
        }
        int w = data.getCurrentWeek();
        ArrayList<Quest> week = new ArrayList<Quest>(pass != null ? pass.weekFreeQuests(w) : DefaultQuests.weekQuestsCyclic(w));
        if (data.isPremium()) {
            week.addAll(pass != null ? pass.weekPremiumQuests(w) : DefaultQuests.premiumWeekQuestsCyclic(w));
        }
        for (Quest q : week) {
            tierChanged |= QuestManager.progress(player, data, q, type, param, amount);
        }
        boolean allDone = true;
        for (Quest q : week) {
            if (data.isQuestClaimed(q.getId())) continue;
            allDone = false;
            break;
        }
        if (allDone && data.getCurrentWeek() < DefaultQuests.effectiveWeekCount(pass)) {
            data.setCurrentWeek(data.getCurrentWeek() + 1);
            player.sendSystemMessage((Component)Component.translatable((String)"fantasticpass.msg.week_unlocked", (Object[])new Object[]{data.getCurrentWeek()}).withStyle(ChatFormatting.AQUA));
        }
        return tierChanged;
    }

    private static boolean progress(ServerPlayer player, PlayerPassData data, Quest quest, QuestType type, String param, int amount) {
        if (quest.getType() != type || data.isQuestClaimed(quest.getId())) {
            return false;
        }
        if (!quest.getParam().isEmpty() && !quest.getParam().equalsIgnoreCase(param)) {
            return false;
        }
        int p = data.addQuestProgress(quest.getId(), amount);
        if (p >= quest.getTarget()) {
            data.markQuestClaimed(quest.getId());
            data.addPoints(quest.getPoints());
            boolean premiumQuest = quest.getId().startsWith("dp_") || quest.getId().startsWith("wp");
            PacketHandler.sendToPlayer(player, new QuestCompletePacket(quest.getType(), quest.getTarget(), quest.getPoints(), premiumQuest));
            return QuestManager.recomputeTier(data);
        }
        return false;
    }
}

