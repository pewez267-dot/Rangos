package com.fscrates.crate;

import com.fscrates.config.CrateConfig;
import com.fscrates.config.Rarity;
import com.fscrates.config.RewardEntry;
import com.fscrates.item.CrateItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class LootEngine {
    private LootEngine() {
    }

    public static List<RewardEntry> roll(CrateConfig crate, Random random) {
        ArrayList<RewardEntry> result = new ArrayList<RewardEntry>();
        // 1) las recompensas garantizadas SIEMPRE entran (sin importar la rareza tirada).
        for (RewardEntry r : crate.rewards) {
            if (r.guaranteed) {
                result.add(r);
            }
        }
        for (int i = 0; i < Math.max(1, crate.rolls); ++i) {
            // 2) tira una RAREZA segun la tabla de probabilidad de rarezas de la crate.
            Rarity rolled = crate.rollRarity(random);
            // 3) arma el POOL de esa rareza: recompensas no-garantizadas cuya rareza
            //    efectiva es la rareza tirada. La 'chance' de cada una es su peso DENTRO
            //    del pool de su rareza.
            ArrayList<RewardEntry> pool = new ArrayList<RewardEntry>();
            double total = 0.0;
            for (RewardEntry r : crate.rewards) {
                if (r.guaranteed) continue;
                if (r.effectiveRarity(crate.rarity) == rolled) {
                    pool.add(r);
                    total += Math.max(0.0, r.chance);
                }
            }
            // 4) si el pool de esa rareza esta vacio, cae a CUALQUIER recompensa
            //    no-garantizada (asi nunca se queda sin premio por un pool vacio).
            if (pool.isEmpty()) {
                for (RewardEntry r : crate.rewards) {
                    if (r.guaranteed) continue;
                    pool.add(r);
                    total += Math.max(0.0, r.chance);
                }
            }
            if (pool.isEmpty()) {
                continue;
            }
            // 5) elige una recompensa del pool por peso.
            RewardEntry chosen = null;
            if (total > 0.0) {
                double pick = random.nextDouble() * total;
                double cursor = 0.0;
                for (RewardEntry r : pool) {
                    cursor += Math.max(0.0, r.chance);
                    if (pick < cursor) {
                        chosen = r;
                        break;
                    }
                }
            }
            if (chosen == null) {
                chosen = pool.get(random.nextInt(pool.size()));
            }
            result.add(chosen);
        }
        return result;
    }

    // Lista (NO garantizadas) de recompensas cuya rareza efectiva == la rareza dada. Es el
    // "pool" de esa rareza; alimenta la RULETA (una ruleta distinta por rareza).
    public static List<RewardEntry> poolFor(CrateConfig crate, Rarity rarity) {
        ArrayList<RewardEntry> pool = new ArrayList<RewardEntry>();
        for (RewardEntry r : crate.rewards) {
            if (r.guaranteed) continue;
            if (r.effectiveRarity(crate.rarity) == rarity) {
                pool.add(r);
            }
        }
        return pool;
    }

    // Como poolFor pero INCLUYE las garantizadas: es lo que se MUESTRA en la ruleta, para
    // que SE VEAN todos los items de esa rareza que el usuario configuro (aunque alguno sea
    // garantizado). La entrega sigue su propia logica; esto es solo para el display.
    public static List<RewardEntry> poolForDisplay(CrateConfig crate, Rarity rarity) {
        ArrayList<RewardEntry> pool = new ArrayList<RewardEntry>();
        for (RewardEntry r : crate.rewards) {
            if (r.effectiveRarity(crate.rarity) == rarity) {
                pool.add(r);
            }
        }
        return pool;
    }

    // Devuelve la rareza dada si su pool tiene items; si NO, la rareza con items mas cercana
    // (busca alternando hacia rarezas mayores y menores). Garantiza que la ruleta sea SIEMPRE
    // de UNA sola rareza (nunca mezclada): si sale una rareza sin items configurados, se
    // redirige a la mas proxima que si tenga. Si ninguna tiene items, devuelve la original.
    public static Rarity resolveRarityWithItems(CrateConfig crate, Rarity rarity) {
        if (!LootEngine.poolFor(crate, rarity).isEmpty()) {
            return rarity;
        }
        Rarity[] all = Rarity.values();
        int base = rarity.ordinal();
        for (int d = 1; d < all.length; ++d) {
            int hi = base + d;
            int lo = base - d;
            if (hi < all.length && !LootEngine.poolFor(crate, all[hi]).isEmpty()) {
                return all[hi];
            }
            if (lo >= 0 && !LootEngine.poolFor(crate, all[lo]).isEmpty()) {
                return all[lo];
            }
        }
        return rarity;
    }

    // Elige UNA recompensa (por peso) del pool de la rareza dada. Si ese pool esta vacio,
    // cae a cualquier recompensa no-garantizada. Devuelve null si no hay ninguna.
    public static RewardEntry pickFromPool(CrateConfig crate, Rarity rarity, Random random) {
        ArrayList<RewardEntry> pool = new ArrayList<RewardEntry>();
        double total = 0.0;
        for (RewardEntry r : crate.rewards) {
            if (r.guaranteed) continue;
            if (r.effectiveRarity(crate.rarity) == rarity) {
                pool.add(r);
                total += Math.max(0.0, r.chance);
            }
        }
        if (pool.isEmpty()) {
            for (RewardEntry r : crate.rewards) {
                if (r.guaranteed) continue;
                pool.add(r);
                total += Math.max(0.0, r.chance);
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        if (total > 0.0) {
            double pick = random.nextDouble() * total;
            double cursor = 0.0;
            for (RewardEntry r : pool) {
                cursor += Math.max(0.0, r.chance);
                if (pick < cursor) {
                    return r;
                }
            }
        }
        return pool.get(random.nextInt(pool.size()));
    }

    public static void deliver(ServerPlayer player, CrateConfig crate, List<RewardEntry> rolled) {
        ServerLevel level = player.serverLevel();
        Random random = new Random();
        for (RewardEntry r : rolled) {
            int amount = r.minAmount + (r.maxAmount > r.minAmount ? random.nextInt(r.maxAmount - r.minAmount + 1) : 0);
            amount = Math.max(1, amount);
            switch (r.type) {
                case ITEM: {
                    LootEngine.giveItem(player, r.item, amount);
                    break;
                }
                case KEY: {
                    LootEngine.giveItem(player, CrateItems.buildKey(), amount);
                    break;
                }
                case XP: {
                    player.giveExperiencePoints(r.xp * amount);
                    break;
                }
                case EFFECT: {
                    LootEngine.applyEffect(player, r);
                }
            }
        }
        if (crate.broadcast && level.getServer() != null) {
            String rewards = rolled.isEmpty() ? "nada" : rolled.get(rolled.size() - 1).describe();
            level.getServer().getPlayerList().broadcastSystemMessage((Component)Component.literal((String)("\u00a7d[Crates] \u00a7f" + player.getName().getString() + " abri\u00f3 " + LootEngine.colorize(crate.displayName) + "\u00a7r\u00a7f y obtuvo \u00a7e" + rewards)), false);
        }
    }

    // Convierte los codigos & del nombre a \u00a7 (formato real) para el chat/broadcast. Solo
    // codigos validos; una '&' suelta se deja intacta.
    public static String colorize(String s) {
        if (s == null || s.indexOf(38) < 0) {
            return s;
        }
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length - 1; ++i) {
            if (c[i] == '&' && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(c[i + 1]) >= 0) {
                c[i] = '\u00a7';
            }
        }
        return new String(c);
    }

    private static void giveItem(ServerPlayer player, ItemStack template, int amount) {
        if (template != null && !template.isEmpty()) {
            int take;
            int max = template.getMaxStackSize();
            for (int remaining = amount; remaining > 0; remaining -= take) {
                take = Math.min(remaining, max);
                ItemStack stack = template.copy();
                stack.setCount(take);
                if (player.getInventory().add(stack)) continue;
                player.drop(stack, false);
            }
        }
    }

    private static void applyEffect(ServerPlayer player, RewardEntry r) {
        MobEffect effect = (MobEffect)ForgeRegistries.MOB_EFFECTS.getValue(LootEngine.safe(r.effectId));
        if (effect != null) {
            player.addEffect(new MobEffectInstance(effect, Math.max(1, r.effectDuration), Math.max(0, r.effectAmplifier)));
        }
    }

    private static ResourceLocation safe(String id) {
        ResourceLocation rl = ResourceLocation.tryParse((String)(id == null ? "" : id));
        return rl == null ? new ResourceLocation("minecraft", "luck") : rl;
    }
}

