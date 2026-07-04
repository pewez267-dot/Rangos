package com.fscrates.util;

import com.fscrates.config.Rarity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

// Paleta de sonido por rareza para la apertura de cofres. Construida en capas
// (varios s.play() con distinto pitch/volumen) para lograr una sensacion "epica y
// profesional" sin nunca usar SoundEvents.GENERIC_EXPLODE (explosion tipo TNT) ni
// SoundEvents.TOTEM_USE - ambos RECHAZADOS EXPLICITAMENTE por el usuario. Todo el
// resto son SoundEvents vanilla de Minecraft 1.20.1.
//
// Arco de intensidad por metodo (se dispara en este orden durante la cinematica):
//   unlock -> spiralCharge -> spiralRise(p) [repetido, p:0..1] -> spiralPeak ->
//   openAccent -> [ruleta] -> win -> winTail -> close (al cerrar/limpiar)
// A mas rareza, mas capas, mas graves/loudness y mas "instrumentos" épicos
// (cuerno de invasion, cuerno de cabra, dragon, warden) se suman encima de la base
// magica (beacon/conduit/encantamiento/amatista/campanas).
public final class CrateSfx {
    private CrateSfx() {
    }

    public static void unlock(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.5f, 1.4f);
                s.play(SoundEvents.BEACON_POWER_SELECT, 0.45f, 1.25f);
                break;
            }
            case RARE: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.55f, 1.2f);
                s.play(SoundEvents.CONDUIT_AMBIENT, 0.5f, 1.15f);
                s.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.4f, 1.1f);
                break;
            }
            case EPIC: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6f, 1.1f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.5f, 1.0f);
                s.play(SoundEvents.EVOKER_PREPARE_ATTACK, 0.5f, 1.0f);
                break;
            }
            case LEGENDARY: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6f, 0.9f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.55f, 0.9f);
                s.play((SoundEvent)SoundEvents.RAID_HORN.value(), 0.45f, 1.1f);
                s.play(SoundEvents.TRIDENT_THUNDER, 0.35f, 1.4f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6f, 0.7f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.55f, 0.8f);
                s.play(SoundEvents.WARDEN_HEARTBEAT, 0.55f, 0.85f);
                s.play(SoundEvents.ENDER_DRAGON_GROWL, 0.4f, 0.9f);
            }
        }
    }

    public static void spiralCharge(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.45f, 1.5f);
                break;
            }
            case RARE: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.5f, 1.35f);
                s.play(SoundEvents.BEACON_POWER_SELECT, 0.4f, 1.3f);
                break;
            }
            case EPIC: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.55f, 1.2f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.5f, 1.1f);
                s.play(SoundEvents.EVOKER_CAST_SPELL, 0.4f, 1.1f);
                break;
            }
            case LEGENDARY: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6f, 1.0f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.55f, 1.0f);
                s.play(SoundEvents.EVOKER_CAST_SPELL, 0.45f, 0.9f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6f, 0.85f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.6f, 1.0f);
                s.play(SoundEvents.ENDER_DRAGON_GROWL, 0.4f, 1.2f);
                s.play(SoundEvents.WARDEN_HEARTBEAT, 0.45f, 0.9f);
            }
        }
    }

    public static void spiralRise(Sink s, Rarity r, float p) {
        float vol = Math.min(1.0f, 0.45f + p * 0.55f);
        switch (r) {
            case COMMON: {
                s.play(SoundEvents.BEACON_POWER_SELECT, vol * 0.85f, Math.min(1.55f, 0.55f + p * 1.0f));
                break;
            }
            case RARE: {
                s.play(SoundEvents.ENCHANTMENT_TABLE_USE, vol, Math.min(1.5f, 0.55f + p * 0.95f));
                if (!(p > 0.55f)) break;
                s.play(SoundEvents.CONDUIT_AMBIENT, vol * 0.3f, 0.85f + p * 0.4f);
                break;
            }
            case EPIC: {
                s.play(SoundEvents.ENCHANTMENT_TABLE_USE, vol, Math.min(1.5f, 0.5f + p * 1.0f));
                if (p > 0.3f) {
                    s.play(SoundEvents.EVOKER_CAST_SPELL, vol * (0.1f + p * 0.35f), 0.6f + p * 0.6f);
                }
                if (!(p > 0.8f)) break;
                s.play(SoundEvents.CONDUIT_ACTIVATE, vol * 0.4f, 0.85f + p * 0.3f);
                break;
            }
            case LEGENDARY: {
                s.play(SoundEvents.BEACON_POWER_SELECT, vol, Math.min(1.25f, 0.4f + p * 0.85f));
                if (p > 0.35f) {
                    s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.15f + p * 0.25f, 0.7f + p * 0.45f);
                }
                if (p > 0.75f) {
                    s.play((SoundEvent)SoundEvents.RAID_HORN.value(), vol * 0.28f, 0.72f + p * 0.32f);
                }
                if (!(p > 0.9f)) break;
                s.play(SoundEvents.GOAT_HORN_PLAY, vol * 0.18f, 0.85f + p * 0.2f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.BEACON_POWER_SELECT, vol, Math.min(1.05f, 0.3f + p * 0.75f));
                if (p > 0.2f) {
                    s.play(SoundEvents.WARDEN_HEARTBEAT, vol * (0.2f + p * 0.4f), 0.55f + p * 0.5f);
                }
                if (p > 0.5f) {
                    s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.2f + p * 0.25f, 0.65f + p * 0.45f);
                }
                if (!(p > 0.88f)) break;
                s.play(SoundEvents.ENDER_DRAGON_GROWL, 0.18f, 1.2f);
            }
        }
    }

    public static void spiralPeak(Sink s, Rarity r) {
        // "Inhalacion" ANTES del impacto (openAccent): un swell brillante ascendente de
        // campanas/amatista + un charge grave que crece, que deja al oyente en tension
        // justo cuando entra el golpe del cielo abriendose. Se le sumo brillo agudo
        // (amatista/twinkle) y cuerpo grave por rareza para que el pico se sienta mas alto.
        switch (r) {
            case COMMON: {
                s.play(SoundEvents.BELL_BLOCK, 0.85f, 1.5f);
                s.play(SoundEvents.BELL_BLOCK, 0.75f, 1.78f);
                s.play(SoundEvents.BELL_BLOCK, 0.62f, 2.0f);
                s.play(SoundEvents.BELL_RESONATE, 0.5f, 1.6f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.6f, 1.6f);
                s.play(SoundEvents.BEACON_POWER_SELECT, 0.55f, 1.85f);
                s.play(SoundEvents.CONDUIT_AMBIENT, 0.4f, 1.7f);
                s.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.55f, 1.9f);
                break;
            }
            case RARE: {
                s.play(SoundEvents.BELL_BLOCK, 0.88f, 1.33f);
                s.play(SoundEvents.BELL_BLOCK, 0.76f, 1.68f);
                s.play(SoundEvents.BELL_BLOCK, 0.62f, 2.0f);
                s.play(SoundEvents.BELL_RESONATE, 0.55f, 1.4f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.65f, 1.45f);
                s.play(SoundEvents.CONDUIT_AMBIENT, 0.5f, 1.3f);
                s.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.55f, 1.5f);
                s.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.75f);
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.35f, 1.4f);
                break;
            }
            case EPIC: {
                s.play(SoundEvents.BELL_BLOCK, 0.9f, 1.18f);
                s.play(SoundEvents.BELL_BLOCK, 0.78f, 1.5f);
                s.play(SoundEvents.BELL_BLOCK, 0.64f, 1.78f);
                s.play(SoundEvents.BELL_RESONATE, 0.55f, 1.2f);
                s.play(SoundEvents.EVOKER_CAST_SPELL, 0.62f, 1.2f);
                s.play(SoundEvents.EVOKER_PREPARE_ATTACK, 0.5f, 1.0f);
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.5f, 1.45f);
                s.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.65f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.55f, 1.35f);
                break;
            }
            case LEGENDARY: {
                s.play(SoundEvents.BELL_BLOCK, 0.95f, 1.5f);
                s.play(SoundEvents.BELL_BLOCK, 0.88f, 1.15f);
                s.play(SoundEvents.BELL_BLOCK, 0.8f, 0.85f);
                s.play(SoundEvents.BELL_RESONATE, 0.6f, 1.0f);
                s.play((SoundEvent)SoundEvents.RAID_HORN.value(), 0.8f, 1.1f);
                s.play((SoundEvent)SoundEvents.RAID_HORN.value(), 0.62f, 0.82f);
                s.play(SoundEvents.GOAT_HORN_PLAY, 0.4f, 0.9f);
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.7f, 1.2f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.6f, 1.25f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.6f, 1.4f);
                s.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.7f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.BELL_BLOCK, 0.95f, 1.4f);
                s.play(SoundEvents.BELL_BLOCK, 0.9f, 1.0f);
                s.play(SoundEvents.BELL_BLOCK, 0.75f, 0.75f);
                s.play(SoundEvents.BELL_RESONATE, 0.6f, 0.85f);
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.75f, 1.1f);
                s.play(SoundEvents.WARDEN_HEARTBEAT, 0.6f, 1.2f);
                s.play(SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 1.2f);
                s.play(SoundEvents.GOAT_HORN_PLAY, 0.4f, 0.75f);
                s.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.6f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.55f, 1.15f);
            }
        }
    }

    public static void openAccent(Sink s, Rarity r) {
        // Apertura EPICA "el cielo se abre", SIN explosion TNT y SIN totem. Construida en
        // TRES bandas deliberadas para que suene grande y limpio en vez de un amasijo:
        //   LOW  (cuerpo/impacto profundo): sonic boom / trueno / wither-spawn a pitch bajo.
        //   MID  (magia + fanfarria): cuernos, evoker, conduit, tridente.
        //   HIGH (brillo/destello): campanas agudas, amatista, twinkle de fuego artificial.
        // La progresion COMMON->MYTHIC suma bandas y baja el pitch del cuerpo (mas masa).
        switch (r) {
            case COMMON: {
                // LOW suave
                s.play(SoundEvents.BEACON_ACTIVATE, 0.9f, 0.95f);
                // HIGH brillante (protagonista en comun)
                s.play(SoundEvents.BELL_BLOCK, 0.9f, 1.4f);
                s.play(SoundEvents.BELL_BLOCK, 0.7f, 1.78f);
                s.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.85f, 1.35f);
                s.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.55f, 1.95f);
                s.play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.4f, 1.5f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.6f, 1.4f);
                s.play(SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.55f, 1.3f);
                break;
            }
            case RARE: {
                // LOW impacto (nuevo sonic boom suave) + rumor de trueno lejano
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 0.55f, 1.35f);
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.6f, 1.3f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.45f, 1.3f);
                // MID magia
                s.play(SoundEvents.CONDUIT_ACTIVATE, 1.0f, 1.1f);
                s.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.7f, 1.2f);
                // HIGH brillo
                s.play(SoundEvents.BELL_BLOCK, 0.88f, 1.25f);
                s.play(SoundEvents.BELL_BLOCK, 0.72f, 1.6f);
                s.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.15f);
                s.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5f, 1.9f);
                s.play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.45f, 1.35f);
                s.play(SoundEvents.FIREWORK_ROCKET_BLAST, 0.5f, 1.4f);
                break;
            }
            case EPIC: {
                // LOW doble sonic boom (uno grave sub) + trueno + tridente
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 1.1f);
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 0.82f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.9f, 1.1f);
                s.play(SoundEvents.TRIDENT_THUNDER, 0.6f, 1.25f);
                // MID magia + primer cuerno (empieza a "abrirse el cielo")
                s.play(SoundEvents.EVOKER_CAST_SPELL, 0.7f, 1.1f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.75f, 1.3f);
                s.play((SoundEvent)SoundEvents.RAID_HORN.value(), 0.55f, 1.05f);
                // HIGH brillo
                s.play(SoundEvents.BELL_BLOCK, 0.85f, 1.1f);
                s.play(SoundEvents.BELL_BLOCK, 0.7f, 1.45f);
                s.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.85f, 1.0f);
                s.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.55f, 1.7f);
                s.play(SoundEvents.FIREWORK_ROCKET_BLAST, 0.65f, 1.15f);
                break;
            }
            case LEGENDARY: {
                // LOW muralla de graves: boom + sub-boom + trueno doble
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 0.9f);
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 0.65f, 0.68f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, 0.78f);
                // MID fanfarria de cuernos + tridente + magia
                s.play((SoundEvent)SoundEvents.RAID_HORN.value(), 1.0f, 1.0f);
                s.play((SoundEvent)SoundEvents.RAID_HORN.value(), 0.7f, 0.78f);
                s.play(SoundEvents.GOAT_HORN_PLAY, 0.6f, 0.85f);
                s.play(SoundEvents.TRIDENT_THUNDER, 0.85f, 1.15f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.7f, 1.1f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.8f, 1.25f);
                // HIGH brillo + gran fuego artificial
                s.play(SoundEvents.BELL_BLOCK, 0.9f, 0.95f);
                s.play(SoundEvents.BELL_BLOCK, 0.6f, 1.5f);
                s.play(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.7f, 1.0f);
                s.play(SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.55f, 1.3f);
                break;
            }
            case MYTHIC: {
                // LOW cataclismo: boom grave + WITHER_SPAWN (el cielo se rasga) + dragon +
                // doble trueno (uno sub). WITHER_SPAWN aporta ese "GONG" cosmico enorme.
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 0.72f);
                s.play(SoundEvents.WITHER_SPAWN, 0.75f, 1.0f);
                s.play(SoundEvents.WARDEN_ROAR, 0.95f, 1.0f);
                s.play(SoundEvents.ENDER_DRAGON_GROWL, 0.95f, 0.9f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 0.85f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, 0.68f);
                // MID cuernos de guerra + portal
                s.play((SoundEvent)SoundEvents.RAID_HORN.value(), 0.95f, 0.82f);
                s.play(SoundEvents.GOAT_HORN_PLAY, 0.7f, 0.72f);
                s.play(SoundEvents.END_PORTAL_SPAWN, 0.7f, 0.85f);
                // HIGH campanas graves imponentes + agudo brillante + gran fuego + twinkle
                s.play(SoundEvents.BELL_BLOCK, 0.9f, 0.85f);
                s.play(SoundEvents.BELL_BLOCK, 0.6f, 1.45f);
                s.play(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.8f, 0.9f);
                s.play(SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.6f, 1.2f);
                break;
            }
        }
    }

    public static void win(Sink s, Rarity r) {
        // Sting de victoria: escala de "ding sutil" (comun) a fanfarria multi-capa
        // (mitico). Cada capa suma volumen/timbre sin repetir instrumentos usados en
        // openAccent en el mismo tick, para que se sienta como un momento nuevo.
        switch (r) {
            case COMMON: {
                s.play(SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.5f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.5f, 1.3f);
                s.play(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.55f, 1.3f);
                break;
            }
            case RARE: {
                s.play(SoundEvents.BEACON_ACTIVATE, 0.65f, 1.2f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.55f, 1.3f);
                s.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.5f, 1.2f);
                s.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5f, 1.3f);
                break;
            }
            case EPIC: {
                s.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.7f, 1.0f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.6f, 1.2f);
                s.play(SoundEvents.EVOKER_CAST_SPELL, 0.55f, 1.1f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.55f, 1.3f);
                s.play(SoundEvents.PLAYER_LEVELUP, 0.55f, 1.1f);
                break;
            }
            case LEGENDARY: {
                s.play((SoundEvent)SoundEvents.RAID_HORN.value(), 0.85f, 0.95f);
                s.play(SoundEvents.GOAT_HORN_PLAY, 0.55f, 0.95f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 1.0f);
                s.play(SoundEvents.TRIDENT_THUNDER, 0.7f, 1.2f);
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 0.55f, 1.1f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.75f, 1.3f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.6f, 1.1f);
                s.play(SoundEvents.PLAYER_LEVELUP, 0.5f, 1.0f);
                s.play(SoundEvents.FIREWORK_ROCKET_BLAST, 0.5f, 1.0f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.ENDER_DRAGON_GROWL, 0.95f, 1.0f);
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 0.85f, 0.95f);
                s.play(SoundEvents.WARDEN_ROAR, 0.7f, 1.0f);
                s.play((SoundEvent)SoundEvents.RAID_HORN.value(), 0.8f, 0.85f);
                s.play(SoundEvents.GOAT_HORN_PLAY, 0.6f, 0.8f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 1.1f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.8f, 1.1f);
                s.play(SoundEvents.PLAYER_LEVELUP, 0.6f, 0.9f);
                s.play(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.65f, 0.95f);
                s.play(SoundEvents.BELL_BLOCK, 0.55f, 0.8f);
            }
        }
    }

    public static void winTail(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(SoundEvents.BEACON_POWER_SELECT, 0.45f, 1.8f);
                s.play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.3f, 1.6f);
                break;
            }
            case RARE: {
                s.play(SoundEvents.BEACON_POWER_SELECT, 0.5f, 1.7f);
                s.play(SoundEvents.CONDUIT_AMBIENT, 0.4f, 1.3f);
                s.play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.35f, 1.4f);
                break;
            }
            case EPIC: {
                s.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.5f, 1.3f);
                s.play(SoundEvents.CONDUIT_AMBIENT, 0.45f, 1.2f);
                s.play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.35f, 1.25f);
                break;
            }
            case LEGENDARY: {
                s.play(SoundEvents.TRIDENT_THUNDER, 0.6f, 1.2f);
                s.play((SoundEvent)SoundEvents.RAID_HORN.value(), 0.5f, 1.05f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.5f, 1.5f);
                s.play(SoundEvents.BELL_RESONATE, 0.4f, 1.1f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.ENDER_DRAGON_GROWL, 0.65f, 1.3f);
                s.play(SoundEvents.WARDEN_ROAR, 0.5f, 1.1f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.55f, 1.25f);
                s.play(SoundEvents.BELL_RESONATE, 0.45f, 0.9f);
                s.play(SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.4f, 1.0f);
            }
        }
    }

    public static void close(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(SoundEvents.BEACON_DEACTIVATE, 0.5f, 1.1f);
                break;
            }
            case RARE: {
                s.play(SoundEvents.BEACON_DEACTIVATE, 0.5f, 1.0f);
                s.play(SoundEvents.CONDUIT_DEACTIVATE, 0.4f, 1.2f);
                break;
            }
            case EPIC: {
                s.play(SoundEvents.BEACON_DEACTIVATE, 0.55f, 1.0f);
                s.play(SoundEvents.CONDUIT_DEACTIVATE, 0.45f, 0.9f);
                break;
            }
            case LEGENDARY: {
                s.play(SoundEvents.CONDUIT_DEACTIVATE, 0.55f, 0.95f);
                s.play(SoundEvents.BEACON_DEACTIVATE, 0.5f, 0.85f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.CONDUIT_DEACTIVATE, 0.55f, 0.9f);
                s.play(SoundEvents.WARDEN_HEARTBEAT, 0.45f, 0.8f);
                s.play(SoundEvents.ENDER_DRAGON_GROWL, 0.4f, 0.85f);
            }
        }
    }

    public static interface Sink {
        public void play(SoundEvent var1, float var2, float var3);
    }
}
