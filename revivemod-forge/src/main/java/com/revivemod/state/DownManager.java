package com.revivemod.state;

import com.revivemod.RevivemodForge;
import com.revivemod.config.ReviveConfig;
import com.revivemod.network.DownEndPacket;
import com.revivemod.network.DownStartPacket;
import com.revivemod.network.ReviveNetwork;
import com.revivemod.state.DownState;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public final class DownManager {
    private static final Map<UUID, DownState> DOWNED = new HashMap<UUID, DownState>();
    private static final Set<UUID> ACTIVE_REVIVERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> FORCE_KILLING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> PENDING_SURRENDER = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> PENDING_SELF = ConcurrentHashMap.newKeySet();

    private DownManager() {
    }

    public static void requestSurrenderToggle(UUID u) {
        if (DownManager.isDown(u)) {
            PENDING_SURRENDER.add(u);
        }
    }

    public static void requestSelfToggle(UUID u) {
        if (DownManager.isDown(u)) {
            PENDING_SELF.add(u);
        }
    }

    public static boolean consumeSurrenderToggle(UUID u) {
        return PENDING_SURRENDER.remove(u);
    }

    public static boolean consumeSelfToggle(UUID u) {
        return PENDING_SELF.remove(u);
    }

    public static boolean isDown(ServerPlayer player) {
        return DOWNED.containsKey(player.getUUID());
    }

    public static boolean isDown(UUID uuid) {
        return DOWNED.containsKey(uuid);
    }

    public static DownState get(ServerPlayer player) {
        return DOWNED.get(player.getUUID());
    }

    public static DownState get(UUID uuid) {
        return DOWNED.get(uuid);
    }

    public static Collection<DownState> all() {
        return Collections.unmodifiableCollection(DOWNED.values());
    }

    public static void clearActiveRevivers() {
        ACTIVE_REVIVERS.clear();
    }

    public static void markReviving(UUID uuid) {
        ACTIVE_REVIVERS.add(uuid);
    }

    public static boolean isReviving(UUID uuid) {
        return ACTIVE_REVIVERS.contains(uuid);
    }

    public static boolean isForceKilling(UUID uuid) {
        return FORCE_KILLING.contains(uuid);
    }

    public static void knockDown(ServerPlayer player, DamageSource cause) {
        ReviveConfig cfg = RevivemodForge.getConfig();
        int totalTicks = Math.max(20, cfg.downTimeSeconds * 20);
        DownState state = new DownState(player.getUUID(), totalTicks, (ResourceKey<Level>)player.serverLevel().dimension(), player.position());
        state.snapshotFood = player.getFoodData().getFoodLevel();
        state.snapshotSaturation = player.getFoodData().getSaturationLevel();
        state.lockedSlot = player.getInventory().selected;
        for (MobEffectInstance eff : player.getActiveEffects()) {
            state.snapshotEffects.add(new MobEffectInstance(eff));
        }
        DOWNED.put(player.getUUID(), state);
        float maxHealth = player.getMaxHealth();
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            maxHealth = (float)attr.getValue();
        }
        player.setHealth(Math.max(1.0f, maxHealth));
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);
        player.clearFire();
        player.fallDistance = 0.0f;
        player.setSharedFlagOnFire(false);
        player.invulnerableTime = 40;
        player.setSprinting(false);
        DownManager.applyDownEffects(player);
        player.setPose(Pose.SWIMMING);
        DownManager.notifyClient(player, true);
        DownManager.enforceLockedSlot(player);
        state.bossBar.addPlayer(player);
        state.bossBar.setName((Component)Component.literal((String)"Desangr\u00e1ndose").withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}));
        state.bossBar.setProgress(1.0f);
        ServerLevel world = player.serverLevel();
        for (int i = 0; i < Math.max(1, cfg.knockdownSoundLayers); ++i) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, cfg.knockdownVolume, 0.7f);
        }
        MutableComponent msg = Component.literal((String)player.getGameProfile().getName()).withStyle(ChatFormatting.YELLOW).append((Component)Component.literal((String)" se est\u00e1 desangrando.").withStyle(ChatFormatting.RED));
        for (ServerPlayer p : world.getServer().getPlayerList().getPlayers()) {
            p.displayClientMessage((Component)msg, false);
        }
        RevivemodForge.LOGGER.info("[revivemod] {} knocked down (cause={})", (Object)player.getGameProfile().getName(), (Object)(cause == null ? "unknown" : cause.getMsgId()));
        if (cfg.clearMobAggroOnDown) {
            world.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48.0), mob -> mob.getTarget() == player).forEach(mob -> mob.setTarget(null));
        }
    }

    private static void notifyClient(ServerPlayer player, boolean down) {
        if (down) {
            ReviveNetwork.sendToPlayer(player, new DownStartPacket(RevivemodForge.getConfig().selfReviveLevelCost));
        } else {
            ReviveNetwork.sendToPlayer(player, new DownEndPacket());
        }
    }

    public static void applyDownEffects(ServerPlayer player) {
        ReviveConfig cfg = RevivemodForge.getConfig();
        int slow = Math.max(0, Math.min(15, cfg.crawlSlowness));
        player.addEffect(DownManager.infinite(MobEffects.MOVEMENT_SLOWDOWN, slow));
        if (cfg.glowingWhileDown) {
            player.addEffect(DownManager.infinite(MobEffects.GLOWING, 0));
        }
    }

    public static void enforceLockedSlot(ServerPlayer player) {
        DownState st = DOWNED.get(player.getUUID());
        if (st != null && player.getInventory().selected != st.lockedSlot) {
            player.getInventory().selected = st.lockedSlot;
            player.connection.send((Packet)new ClientboundSetCarriedItemPacket(st.lockedSlot));
        }
    }

    private static MobEffectInstance infinite(MobEffect effect, int amplifier) {
        return new MobEffectInstance(effect, -1, amplifier, false, false, false);
    }

    public static void revive(ServerPlayer player) {
        DownState state = DOWNED.remove(player.getUUID());
        if (state == null) {
            return;
        }
        ReviveConfig cfg = RevivemodForge.getConfig();
        state.bossBar.removeAllPlayers();
        ACTIVE_REVIVERS.remove(player.getUUID());
        DownManager.clearDownEffects(player);
        DownManager.clearProne(player);
        for (MobEffectInstance eff : state.snapshotEffects) {
            MobEffect e = eff.getEffect();
            if (e == MobEffects.MOVEMENT_SLOWDOWN || e == MobEffects.DIG_SLOWDOWN || e == MobEffects.WEAKNESS || e == MobEffects.BLINDNESS || e == MobEffects.GLOWING) continue;
            player.addEffect(new MobEffectInstance(eff));
        }
        player.setHealth(Math.max(1.0f, Math.min(player.getMaxHealth(), cfg.reviveHealth)));
        player.getFoodData().setFoodLevel(Math.max(player.getFoodData().getFoodLevel(), cfg.reviveFood));
        player.getFoodData().setSaturation(2.0f);
        player.invulnerableTime = 60;
        player.fallDistance = 0.0f;
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1, false, true, true));
        ServerLevel world = player.serverLevel();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        for (int i = 0; i < Math.max(1, cfg.reviveSoundLayers); ++i) {
            world.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, cfg.reviveVolume, 1.2f);
            world.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, cfg.reviveVolume, 1.6f);
        }
        world.sendParticles((ParticleOptions)ParticleTypes.HEART, x, y + 1.2, z, 14, 0.5, 0.7, 0.5, 0.02);
        world.sendParticles((ParticleOptions)ParticleTypes.HAPPY_VILLAGER, x, y + 1.0, z, 18, 0.6, 0.9, 0.6, 0.04);
    }

    public static void clearDownEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.GLOWING);
    }

    public static void clearProne(ServerPlayer player) {
        player.setSwimming(false);
        player.setPose(Pose.STANDING);
        DownManager.notifyClient(player, false);
    }

    public static boolean selfRevive(ServerPlayer player) {
        ReviveConfig cfg = RevivemodForge.getConfig();
        if (!cfg.allowSelfRevive) {
            return false;
        }
        if (!DownManager.isDown(player)) {
            return false;
        }
        if (player.experienceLevel < cfg.selfReviveLevelCost) {
            return false;
        }
        player.giveExperienceLevels(-cfg.selfReviveLevelCost);
        DownManager.revive(player);
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void forceDeath(ServerPlayer player, DamageSource source) {
        DownState state = DOWNED.remove(player.getUUID());
        if (state != null) {
            state.bossBar.removeAllPlayers();
        }
        ACTIVE_REVIVERS.remove(player.getUUID());
        ReviveConfig cfg = RevivemodForge.getConfig();
        ServerLevel dw = player.serverLevel();
        for (int i = 0; i < Math.max(1, cfg.deathSoundLayers); ++i) {
            dw.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, cfg.deathVolume, 0.65f);
            dw.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, cfg.deathVolume, 0.5f);
        }
        DownManager.clearDownEffects(player);
        DownManager.clearProne(player);
        DamageSource finalSrc = player.damageSources().genericKill();
        FORCE_KILLING.add(player.getUUID());
        try {
            player.invulnerableTime = 0;
            player.hurt(finalSrc, Float.MAX_VALUE);
            if (player.isAlive()) {
                player.setHealth(0.0f);
            }
        }
        finally {
            FORCE_KILLING.remove(player.getUUID());
        }
    }

    public static void clearAll(MinecraftServer server) {
        for (DownState state : DOWNED.values()) {
            state.bossBar.removeAllPlayers();
        }
        DOWNED.clear();
        ACTIVE_REVIVERS.clear();
    }

    public static DownState removeWithoutRevival(UUID uuid) {
        DownState st = DOWNED.remove(uuid);
        if (st != null) {
            st.bossBar.removeAllPlayers();
        }
        ACTIVE_REVIVERS.remove(uuid);
        return st;
    }

    public static void reattach(ServerPlayer player) {
        DownState state = DOWNED.get(player.getUUID());
        if (state == null) {
            return;
        }
        state.bossBar.addPlayer(player);
        DownManager.applyDownEffects(player);
        player.setPose(Pose.SWIMMING);
        DownManager.notifyClient(player, true);
        DownManager.enforceLockedSlot(player);
    }

    public static void enforcePosition(ServerPlayer player) {
        DownState state = DOWNED.get(player.getUUID());
        if (state == null) {
            return;
        }
        state.downDimension = player.serverLevel().dimension();
        state.downPosition = player.position();
    }
}

