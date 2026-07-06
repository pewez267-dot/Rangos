package com.fscrates.util;

import com.fscrates.config.Rarity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

// PALETA DE SONIDO — REIMPLANTE EXACTO de la build FantasticCratesSONG.jar (FSCrates 2.9.12)
// (version 2.9.35). El usuario pidio EXPRESA y NO NEGOCIABLEMENTE que se sacaran los sonidos
// de esa build TAL CUAL y se adaptaran a este contexto (mismo timing actual: BURST=76,
// REVEAL=294). Esta build usa una paleta "arcana/energetica" con beacon, conduit, respawn
// anchor, warden sonic boom/charge, lightning thunder, trident thunder, wither spawn, end
// portal y enchantment table. SI, varios estaban en la antigua "lista negra"; el usuario la
// ANULO al pedir explicitamente esta build de referencia (ver handoff).
//
// AÑADIDO por peticion del usuario: capas ESPECTRALES (gemidos de almas del valle de arena
// de almas = AMBIENT_SOUL_SAND_VALLEY_ADDITIONS/_MOOD) mezcladas sobre la paleta 2.9.12 en
// los momentos clave (despertar, windup, estallido, reveal, cola). La paleta 2.9.12 se
// mantiene byte-a-byte; los espectrales son capas EXTRA a volumen moderado.
//
// Valores (vol, pitch) copiados EXACTAMENTE de 2.9.12. Sink.play(ev, VOL, PITCH). Los soul
// sand valley son Holder<SoundEvent> -> .value().
public final class CrateSfx {
    private CrateSfx() {
    }

    // Capas espectrales AÑADIDAS (no estaban en 2.9.12).
    private static SoundEvent wailA() { return SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS.value(); }
    private static SoundEvent wailM() { return SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(); }
    // Capas de TERROR/ESPECTRO añadidas en 2.9.36 (pedido del usuario: mas epico y aterrador
    // al abrir la tapa y en el premio). Todas SoundEvent directo.
    private static SoundEvent witherGroan() { return SoundEvents.WITHER_AMBIENT; }   // gemido/gruñido grave del wither
    private static SoundEvent witherDeath() { return SoundEvents.WITHER_DEATH; }     // gemido largo y dramatico
    private static SoundEvent soulEscape()  { return SoundEvents.SOUL_ESCAPE; }      // alma que escapa (espectral)

    public static void unlock(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.8f, 1.35f);
                s.play(SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.25f);
                s.play(wailM(), 0.7f, 1.0f);
                break;
            }
            case RARE: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.85f, 1.2f);
                s.play(SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.15f);
                s.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.6f, 1.1f);
                s.play(wailM(), 0.75f, 1.0f);
                break;
            }
            case EPIC: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.9f, 1.05f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.7f, 1.15f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.6f, 1.2f);
                s.play(wailA(), 0.75f, 1.0f);
                break;
            }
            case LEGENDARY: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.9f, 0.95f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.8f, 1.0f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.7f, 1.1f);
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.5f, 1.3f);
                s.play(wailA(), 0.8f, 0.95f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.9f, 0.8f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.8f, 0.9f);
                s.play(SoundEvents.END_PORTAL_FRAME_FILL, 0.8f, 0.9f);
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.6f, 1.1f);
                s.play(wailA(), 0.85f, 0.9f);
            }
        }
    }

    public static void spiralCharge(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.55f, 1.4f);
                break;
            }
            case RARE: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6f, 1.25f);
                s.play(SoundEvents.BEACON_AMBIENT, 0.5f, 1.2f);
                break;
            }
            case EPIC: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.65f, 1.1f);
                s.play(SoundEvents.CONDUIT_AMBIENT, 0.55f, 1.1f);
                break;
            }
            case LEGENDARY: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.7f, 1.0f);
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.45f, 1.0f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.7f, 0.85f);
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.55f, 0.9f);
                s.play(SoundEvents.CONDUIT_AMBIENT, 0.5f, 0.9f);
            }
        }
    }

    public static void spiralRise(Sink s, Rarity r, float p) {
        float vol = Math.min(1.0f, 0.5f + p * 0.5f);
        switch (r) {
            case COMMON: {
                s.play(SoundEvents.BEACON_POWER_SELECT, vol * 0.85f, Math.min(1.6f, 0.6f + p * 1.0f));
                break;
            }
            case RARE: {
                s.play(SoundEvents.BEACON_POWER_SELECT, vol * 0.85f, Math.min(1.55f, 0.6f + p * 0.95f));
                if (!(p > 0.55f)) break;
                s.play(SoundEvents.CONDUIT_AMBIENT, vol * 0.35f, 0.9f + p * 0.4f);
                break;
            }
            case EPIC: {
                s.play(SoundEvents.BEACON_POWER_SELECT, vol, Math.min(1.5f, 0.55f + p * 1.0f));
                if (!(p > 0.4f)) break;
                s.play(SoundEvents.CONDUIT_ACTIVATE, vol * 0.3f, 0.7f + p * 0.5f);
                break;
            }
            case LEGENDARY: {
                s.play(SoundEvents.BEACON_POWER_SELECT, vol, Math.min(1.4f, 0.45f + p * 0.9f));
                if (!(p > 0.4f)) break;
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.2f + p * 0.3f, 0.7f + p * 0.45f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.BEACON_POWER_SELECT, vol, Math.min(1.25f, 0.35f + p * 0.85f));
                if (p > 0.3f) {
                    s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.25f + p * 0.35f, 0.6f + p * 0.5f);
                }
                if (!(p > 0.75f)) break;
                s.play(SoundEvents.CONDUIT_ACTIVATE, vol * 0.4f, 0.9f + p * 0.3f);
            }
        }
    }

    public static void spiralPeak(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(SoundEvents.BEACON_ACTIVATE, 0.8f, 1.5f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.6f, 1.6f);
                break;
            }
            case RARE: {
                s.play(SoundEvents.BEACON_ACTIVATE, 0.85f, 1.35f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.7f, 1.4f);
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.5f, 1.4f);
                break;
            }
            case EPIC: {
                s.play(SoundEvents.BEACON_ACTIVATE, 0.9f, 1.2f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.75f, 1.3f);
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.6f, 1.3f);
                s.play(wailA(), 0.7f, 0.9f);
                break;
            }
            case LEGENDARY: {
                s.play(SoundEvents.BEACON_ACTIVATE, 0.9f, 1.05f);
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.75f, 1.15f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.7f, 1.2f);
                s.play(wailA(), 0.75f, 0.85f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, 0.85f, 1.0f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.8f, 0.95f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.7f, 1.1f);
                s.play(wailA(), 0.8f, 0.8f);
            }
        }
    }

    public static void openAccent(Sink s, Rarity r) {
        // ESTALLIDO DE LA TAPA. Base = 2.9.12 (warden boom, lightning, trident thunder, end
        // portal, beacon/conduit). AÑADIDO 2.9.36: gemido grave del WITHER (aterrador, pitch
        // bajo = mas siniestro) + ALMA QUE ESCAPA (espectral) + gemido de almas extra. Escala
        // con la rareza: mas rareza = gemido mas grave/fuerte.
        switch (r) {
            case COMMON: {
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 0.9f, 1.5f);
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.05f);
                s.play(SoundEvents.BEACON_ACTIVATE, 1.0f, 1.2f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.9f, 1.3f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, 1.4f);
                s.play(wailM(), 0.85f, 1.0f);
                // 2.9.37: wither MUY bajo (rumor grave lejano, no protagonista) + mas almas.
                s.play(witherGroan(), 0.28f, 0.7f);
                s.play(soulEscape(), 0.6f, 1.05f);
                s.play(wailA(), 0.7f, 1.1f);
                break;
            }
            case RARE: {
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 1.3f);
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 0.7f, 0.9f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.9f, 1.25f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.95f, 1.15f);
                s.play(SoundEvents.TRIDENT_THUNDER, 0.85f, 1.3f);
                s.play(SoundEvents.END_PORTAL_SPAWN, 0.55f, 1.35f);
                s.play(wailA(), 0.85f, 0.95f);
                s.play(witherGroan(), 0.3f, 0.68f);
                s.play(soulEscape(), 0.65f, 1.0f);
                s.play(wailM(), 0.65f, 1.15f);
                break;
            }
            case EPIC: {
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 1.1f);
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 0.78f, 0.82f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 1.1f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.7f, 0.8f);
                s.play(SoundEvents.TRIDENT_THUNDER, 0.9f, 1.2f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.9f, 1.25f);
                s.play(SoundEvents.END_PORTAL_SPAWN, 0.72f, 1.3f);
                s.play(wailA(), 0.9f, 0.9f);
                s.play(witherGroan(), 0.32f, 0.66f);
                s.play(soulEscape(), 0.7f, 0.95f);
                s.play(wailM(), 0.7f, 1.2f);
                break;
            }
            case LEGENDARY: {
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 0.95f);
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 0.85f, 0.68f);
                s.play(SoundEvents.WITHER_SPAWN, 0.9f, 0.95f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 0.72f);
                s.play(SoundEvents.TRIDENT_THUNDER, 0.95f, 1.1f);
                s.play(SoundEvents.END_PORTAL_SPAWN, 0.82f, 1.1f);
                s.play(wailA(), 0.9f, 0.85f);
                s.play(witherGroan(), 0.35f, 0.64f);
                s.play(soulEscape(), 0.72f, 0.9f);
                s.play(wailM(), 0.75f, 1.2f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 0.75f);
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 0.92f, 0.55f);
                s.play(SoundEvents.WITHER_SPAWN, 1.0f, 0.9f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 0.85f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.85f, 0.6f);
                s.play(SoundEvents.END_PORTAL_SPAWN, 0.9f, 0.9f);
                s.play(SoundEvents.TRIDENT_THUNDER, 0.9f, 1.0f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.85f, 1.1f);
                s.play(wailA(), 0.95f, 0.9f);
                s.play(witherGroan(), 0.4f, 0.62f);
                s.play(soulEscape(), 0.75f, 0.85f);
                s.play(wailM(), 0.8f, 1.25f);
            }
        }
    }

    public static void openSustain(Sink s, Rarity r, float p) {
        float v = 0.55f + p * 0.4f;
        s.play(SoundEvents.CONDUIT_AMBIENT, v * 0.7f, 1.1f + p * 0.5f);
        switch (r) {
            case COMMON: {
                break;
            }
            case RARE: {
                s.play(SoundEvents.BEACON_AMBIENT, v * 0.5f, 1.1f);
                break;
            }
            case EPIC: {
                s.play(SoundEvents.BEACON_AMBIENT, v * 0.5f, 1.0f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, v * 0.35f, 1.2f);
                break;
            }
            case LEGENDARY: {
                s.play(SoundEvents.BEACON_AMBIENT, v * 0.55f, 0.95f);
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, v * 0.35f, 1.0f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.BEACON_AMBIENT, v * 0.55f, 0.9f);
                s.play(SoundEvents.WARDEN_SONIC_CHARGE, v * 0.45f, 0.9f);
            }
        }
    }

    public static void win(Sink s, Rarity r) {
        // EL PREMIO EMERGE. Base = 2.9.12. AÑADIDO 2.9.36: gemido del WITHER (aterrador) +
        // ALMA QUE ESCAPA + gemido de almas extra. En LEGENDARY/MYTHIC se usa el gemido LARGO
        // y dramatico del wither (WITHER_DEATH) para un reveal imponente. Mas rareza = mas
        // grave y epico.
        switch (r) {
            case COMMON: {
                s.play(SoundEvents.BEACON_POWER_SELECT, 0.85f, 1.5f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.7f, 1.3f);
                s.play(wailA(), 0.7f, 1.15f);
                // 2.9.37: fuera WITHER_DEATH (se robaba el show); wither solo un rumor grave
                // suave + mas almas espectrales de protagonistas.
                s.play(witherGroan(), 0.25f, 0.72f);
                s.play(soulEscape(), 0.62f, 1.1f);
                s.play(wailM(), 0.6f, 1.2f);
                break;
            }
            case RARE: {
                s.play(SoundEvents.BEACON_ACTIVATE, 0.85f, 1.2f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.8f, 1.3f);
                s.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.65f, 1.2f);
                s.play(wailA(), 0.75f, 1.15f);
                s.play(witherGroan(), 0.28f, 0.7f);
                s.play(soulEscape(), 0.66f, 1.05f);
                s.play(wailM(), 0.62f, 1.2f);
                break;
            }
            case EPIC: {
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.9f, 1.15f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.85f, 1.25f);
                s.play(SoundEvents.TRIDENT_THUNDER, 0.75f, 1.2f);
                s.play(SoundEvents.END_PORTAL_SPAWN, 0.6f, 1.2f);
                s.play(wailA(), 0.8f, 1.2f);
                s.play(witherGroan(), 0.3f, 0.68f);
                s.play(soulEscape(), 0.7f, 1.0f);
                s.play(wailM(), 0.68f, 1.25f);
                break;
            }
            case LEGENDARY: {
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 0.85f, 1.1f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.9f, 1.05f);
                s.play(SoundEvents.TRIDENT_THUNDER, 0.85f, 1.15f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.8f, 1.2f);
                s.play(wailA(), 0.85f, 1.2f);
                s.play(witherGroan(), 0.33f, 0.66f);
                s.play(soulEscape(), 0.72f, 0.95f);
                s.play(wailM(), 0.72f, 1.2f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.WITHER_SPAWN, 0.9f, 0.95f);
                s.play(SoundEvents.WARDEN_SONIC_BOOM, 0.9f, 0.9f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.9f, 1.05f);
                s.play(SoundEvents.END_PORTAL_SPAWN, 0.8f, 1.0f);
                s.play(SoundEvents.CONDUIT_ACTIVATE, 0.8f, 1.1f);
                s.play(wailA(), 0.9f, 1.2f);
                s.play(witherGroan(), 0.36f, 0.64f);
                s.play(soulEscape(), 0.75f, 0.9f);
                s.play(wailM(), 0.76f, 1.25f);
            }
        }
    }

    public static void winTail(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(SoundEvents.BEACON_POWER_SELECT, 0.55f, 1.8f);
                s.play(wailM(), 0.5f, 1.1f);
                break;
            }
            case RARE: {
                s.play(SoundEvents.CONDUIT_AMBIENT, 0.55f, 1.4f);
                s.play(SoundEvents.BEACON_POWER_SELECT, 0.5f, 1.7f);
                s.play(wailM(), 0.55f, 1.1f);
                break;
            }
            case EPIC: {
                s.play(SoundEvents.CONDUIT_AMBIENT, 0.6f, 1.25f);
                s.play(SoundEvents.ENCHANTMENT_TABLE_USE, 0.55f, 1.3f);
                s.play(wailM(), 0.6f, 1.05f);
                break;
            }
            case LEGENDARY: {
                s.play(SoundEvents.TRIDENT_RETURN, 0.6f, 1.1f);
                s.play(SoundEvents.CONDUIT_AMBIENT, 0.55f, 1.1f);
                s.play(SoundEvents.BEACON_ACTIVATE, 0.55f, 1.4f);
                s.play(wailA(), 0.6f, 1.0f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.END_PORTAL_SPAWN, 0.6f, 1.2f);
                s.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.55f, 1.2f);
                s.play(SoundEvents.CONDUIT_AMBIENT, 0.55f, 0.95f);
                s.play(wailA(), 0.65f, 0.95f);
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
                s.play(SoundEvents.CONDUIT_DEACTIVATE, 0.5f, 1.0f);
                s.play(SoundEvents.BEACON_DEACTIVATE, 0.45f, 0.9f);
                break;
            }
            case LEGENDARY: {
                s.play(SoundEvents.CONDUIT_DEACTIVATE, 0.55f, 0.95f);
                s.play(SoundEvents.BEACON_DEACTIVATE, 0.5f, 0.85f);
                break;
            }
            case MYTHIC: {
                s.play(SoundEvents.CONDUIT_DEACTIVATE, 0.55f, 0.9f);
                s.play(SoundEvents.BEACON_DEACTIVATE, 0.5f, 0.8f);
            }
        }
    }

    public static interface Sink {
        public void play(SoundEvent var1, float var2, float var3);
    }
}
