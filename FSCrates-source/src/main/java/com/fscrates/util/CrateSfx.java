package com.fscrates.util;

import com.fscrates.config.Rarity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

// PALETA DE SONIDO — REWORK TOTAL "RITUAL OSCURO" (2.9.32). El usuario borro toda la paleta
// anterior (warden emerge/heartbeat/charge, beacon, conduit, evoker, respawn, cuerno...) por
// sonar chafa/distorsionada/desincronizada. Identidad NUEVA, LIMPIA y afinable:
//   - TAMBOR RITUAL / BOOM  = NOTE_BLOCK_BASEDRUM (kick grave y limpio, no satura).
//   - DRONE OMINOSO         = NOTE_BLOCK_DIDGERIDOO (fondo de ultratumba).
//   - BAJO DE TENSION       = NOTE_BLOCK_BASS (nota grave que sube en el build).
//   - DESTELLO DE PREMIO    = NOTE_BLOCK_HARP (flourish magico limpio, NO campana).
//   - GEMIDOS ESPECTRALES   = AMBIENT_SOUL_SAND_VALLEY_ADDITIONS/_MOOD (almas en pena; el
//                             usuario los AMA -> se MANTIENEN como protagonistas).
// Reglas: pitch SIEMPRE >=0.5; POCAS capas balanceadas por evento (3-4) para NO clippear;
// NADA de sonidos prohibidos (warden boom/roar/nearby, ghast, sculk, vex, lightning/tnt,
// trident thunder, raid horn, campanas, amatista, xp, totems, cohetes, portal, enderman).
// Todos los NOTE_BLOCK_* y AMBIENT_* son Holder<SoundEvent> -> se usan con .value().
public final class CrateSfx {
    private CrateSfx() {
    }

    // Atajos legibles (todos Holder -> .value()).
    private static SoundEvent drum() { return SoundEvents.NOTE_BLOCK_BASEDRUM.value(); }
    private static SoundEvent drone() { return SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(); }
    private static SoundEvent bass() { return SoundEvents.NOTE_BLOCK_BASS.value(); }
    private static SoundEvent harp() { return SoundEvents.NOTE_BLOCK_HARP.value(); }
    private static SoundEvent wailA() { return SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS.value(); }
    private static SoundEvent wailM() { return SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(); }

    // t=2 — INSERCION DE LA LLAVE / el ritual DESPIERTA: drone grave que se enciende + el
    // primer lamento lejano de las almas. Sin dings brillantes.
    public static void unlock(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(drone(), 0.7f, 0.5f);
                s.play(wailM(), 0.85f, 1.0f);
                break;
            }
            case RARE: {
                s.play(drone(), 0.75f, 0.5f);
                s.play(wailM(), 0.9f, 1.0f);
                break;
            }
            case EPIC: {
                s.play(drone(), 0.8f, 0.5f);
                s.play(wailA(), 0.9f, 1.0f);
                break;
            }
            case LEGENDARY: {
                s.play(drone(), 0.85f, 0.5f);
                s.play(wailA(), 0.95f, 0.95f);
                s.play(bass(), 0.6f, 0.5f);
                break;
            }
            case MYTHIC: {
                s.play(drone(), 0.9f, 0.5f);
                s.play(wailA(), 0.95f, 0.9f);
                s.play(bass(), 0.7f, 0.5f);
                break;
            }
        }
    }

    // t=6 — arranca el RITUAL: drone + primer golpe de tambor grave + lamento de fondo.
    public static void spiralCharge(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(drone(), 0.8f, 0.5f);
                s.play(drum(), 0.65f, 0.5f);
                s.play(wailM(), 0.85f, 1.0f);
                break;
            }
            case RARE: {
                s.play(drone(), 0.85f, 0.5f);
                s.play(drum(), 0.7f, 0.5f);
                s.play(wailM(), 0.9f, 1.0f);
                break;
            }
            case EPIC: {
                s.play(drone(), 0.9f, 0.5f);
                s.play(drum(), 0.75f, 0.5f);
                s.play(wailA(), 0.9f, 0.95f);
                break;
            }
            case LEGENDARY: {
                s.play(drone(), 0.95f, 0.5f);
                s.play(drum(), 0.8f, 0.5f);
                s.play(wailA(), 0.95f, 0.9f);
                s.play(bass(), 0.6f, 0.5f);
                break;
            }
            case MYTHIC: {
                s.play(drone(), 1.0f, 0.5f);
                s.play(drum(), 0.85f, 0.5f);
                s.play(wailA(), 0.95f, 0.85f);
                s.play(bass(), 0.7f, 0.5f);
                break;
            }
        }
    }

    // EL TAMBOR RITUAL. El caller la dispara en un intervalo que se ACORTA (acelera) de t=6
    // a t=64: cada llamada es un golpe de tambor grave, con el tono subiendo levemente con p
    // (tension), un bajo que sube y el lamento creciendo. Cada golpe empuja un PULSO VISUAL
    // de la caja (ver lastPulseTick en la pantalla) -> imagen y sonido laten juntos.
    public static void spiralRise(Sink s, Rarity r, float p) {
        float vol = 0.55f + p * 0.4f;
        float drumPitch = 0.5f + p * 0.28f;   // 0.5 -> 0.78, sube la tension
        float bassPitch = 0.5f + p * 0.25f;
        switch (r) {
            case COMMON: {
                s.play(drum(), vol, drumPitch);
                if (p > 0.5f) {
                    s.play(wailM(), 0.4f + p * 0.45f, 0.95f);
                }
                break;
            }
            case RARE: {
                s.play(drum(), vol, drumPitch);
                s.play(bass(), vol * 0.6f, bassPitch);
                if (p > 0.45f) {
                    s.play(wailM(), 0.45f + p * 0.45f, 0.95f);
                }
                break;
            }
            case EPIC: {
                s.play(drum(), vol, drumPitch);
                s.play(bass(), vol * 0.65f, bassPitch);
                s.play(wailA(), 0.5f + p * 0.45f, 0.9f);
                break;
            }
            case LEGENDARY: {
                s.play(drum(), vol, drumPitch);
                s.play(bass(), vol * 0.7f, bassPitch);
                s.play(wailA(), 0.55f + p * 0.45f, 0.85f);
                if (p > 0.6f) {
                    s.play(drone(), 0.55f, 0.5f);
                }
                break;
            }
            case MYTHIC: {
                s.play(drum(), vol, drumPitch);
                s.play(bass(), vol * 0.75f, bassPitch);
                s.play(wailA(), 0.6f + p * 0.4f, 0.8f);
                if (p > 0.5f) {
                    s.play(drone(), 0.6f, 0.5f);
                }
                break;
            }
        }
    }

    // ~t=64 — INHALACION/WINDUP: acorde tenso sostenido (drone grave + bajo + lamento a
    // tope) justo antes del estallido. El silencio del tambor + esta tension = anticipacion.
    public static void spiralPeak(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(drone(), 1.0f, 0.5f);
                s.play(wailM(), 1.0f, 1.0f);
                break;
            }
            case RARE: {
                s.play(drone(), 1.0f, 0.5f);
                s.play(wailA(), 1.0f, 0.95f);
                s.play(bass(), 0.6f, 0.5f);
                break;
            }
            case EPIC: {
                s.play(drone(), 1.0f, 0.5f);
                s.play(wailA(), 1.0f, 0.9f);
                s.play(bass(), 0.7f, 0.5f);
                break;
            }
            case LEGENDARY: {
                s.play(drone(), 1.0f, 0.5f);
                s.play(wailA(), 1.0f, 0.85f);
                s.play(wailM(), 0.7f, 1.0f);
                s.play(bass(), 0.75f, 0.5f);
                break;
            }
            case MYTHIC: {
                s.play(drone(), 1.0f, 0.5f);
                s.play(wailA(), 1.0f, 0.8f);
                s.play(wailM(), 0.75f, 0.95f);
                s.play(bass(), 0.8f, 0.5f);
                break;
            }
        }
    }

    // t=76 — EL ESTALLIDO (la tapa revienta; cae EXACTO con el fogonazo del fondo). BOOM
    // limpio y grande: tambor grave a tope (doble = octava en rarezas altas) + el AULLIDO de
    // las almas + drone. Pocas capas -> pega fuerte sin distorsionar.
    public static void openAccent(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(drum(), 1.0f, 0.5f);
                s.play(wailA(), 0.9f, 1.0f);
                s.play(drone(), 0.7f, 0.5f);
                break;
            }
            case RARE: {
                s.play(drum(), 1.0f, 0.5f);
                s.play(drum(), 0.75f, 0.6f);
                s.play(wailA(), 0.9f, 0.95f);
                s.play(drone(), 0.75f, 0.5f);
                break;
            }
            case EPIC: {
                s.play(drum(), 1.0f, 0.5f);
                s.play(drum(), 0.8f, 0.6f);
                s.play(wailA(), 0.9f, 0.9f);
                s.play(wailM(), 0.65f, 0.75f);
                s.play(bass(), 0.7f, 0.5f);
                break;
            }
            case LEGENDARY: {
                s.play(drum(), 1.0f, 0.5f);
                s.play(drum(), 0.85f, 0.6f);
                s.play(wailA(), 0.9f, 0.85f);
                s.play(wailM(), 0.7f, 0.72f);
                s.play(bass(), 0.75f, 0.5f);
                break;
            }
            case MYTHIC: {
                s.play(drum(), 1.0f, 0.5f);
                s.play(drum(), 0.9f, 0.6f);
                s.play(wailA(), 0.95f, 0.9f);
                s.play(wailM(), 0.7f, 0.68f);
                s.play(bass(), 0.8f, 0.5f);
                break;
            }
        }
    }

    // Lecho sostenido opcional (no lo usa el fullscreen; disponible por compatibilidad).
    public static void openSustain(Sink s, Rarity r, float p) {
        float v = 0.5f + p * 0.4f;
        s.play(drone(), v * 0.6f, 0.5f);
        switch (r) {
            case COMMON: {
                break;
            }
            case RARE: {
                s.play(wailM(), v * 0.6f, 0.95f);
                break;
            }
            case EPIC: {
                s.play(wailM(), v * 0.72f, 0.9f);
                break;
            }
            case LEGENDARY: {
                s.play(wailA(), v * 0.78f, 0.85f);
                break;
            }
            case MYTHIC: {
                s.play(wailA(), v * 0.88f, 0.8f);
                break;
            }
        }
    }

    // t=294 — EXPLOSION DEL PREMIO (cae EXACTO cuando aparece el premio). MISMA identidad
    // que la apertura (tambor + almas) = CONGRUENTE, pero TRIUNFAL: tambor un pelin mas
    // agudo + CORO de almas + un DESTELLO DE ARPA ascendente (magia/recompensa, NO campana).
    public static void win(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(drum(), 1.0f, 0.6f);
                s.play(wailA(), 0.9f, 1.1f);
                s.play(harp(), 0.4f, 1.05f);
                break;
            }
            case RARE: {
                s.play(drum(), 1.0f, 0.6f);
                s.play(wailA(), 0.9f, 1.0f);
                s.play(wailM(), 0.65f, 0.85f);
                s.play(harp(), 0.45f, 1.1f);
                break;
            }
            case EPIC: {
                s.play(drum(), 1.0f, 0.58f);
                s.play(wailA(), 0.9f, 0.95f);
                s.play(wailM(), 0.7f, 0.8f);
                s.play(harp(), 0.5f, 1.15f);
                s.play(bass(), 0.6f, 0.55f);
                break;
            }
            case LEGENDARY: {
                s.play(drum(), 1.0f, 0.55f);
                s.play(wailA(), 0.9f, 0.9f);
                s.play(wailM(), 0.7f, 0.75f);
                s.play(harp(), 0.5f, 1.2f);
                s.play(bass(), 0.7f, 0.5f);
                break;
            }
            case MYTHIC: {
                s.play(drum(), 1.0f, 0.5f);
                s.play(drum(), 0.85f, 0.6f);
                s.play(wailA(), 0.95f, 0.9f);
                s.play(wailM(), 0.7f, 0.72f);
                s.play(harp(), 0.55f, 1.25f);
                s.play(bass(), 0.75f, 0.5f);
                break;
            }
        }
    }

    // Cola/resonancia tras el premio: el lamento de las almas se desvanece sobre el drone.
    public static void winTail(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(drone(), 0.5f, 0.5f);
                s.play(wailM(), 0.55f, 1.1f);
                break;
            }
            case RARE: {
                s.play(drone(), 0.5f, 0.5f);
                s.play(wailM(), 0.6f, 1.05f);
                break;
            }
            case EPIC: {
                s.play(drone(), 0.55f, 0.5f);
                s.play(wailM(), 0.65f, 1.0f);
                break;
            }
            case LEGENDARY: {
                s.play(drone(), 0.55f, 0.5f);
                s.play(wailA(), 0.7f, 0.95f);
                break;
            }
            case MYTHIC: {
                s.play(drone(), 0.6f, 0.5f);
                s.play(wailA(), 0.75f, 0.9f);
                s.play(wailM(), 0.55f, 1.1f);
                break;
            }
        }
    }

    // Cierre (in-world): el drone y el bajo se apagan.
    public static void close(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(drone(), 0.5f, 0.5f);
                break;
            }
            case RARE: {
                s.play(drone(), 0.5f, 0.5f);
                s.play(bass(), 0.4f, 0.5f);
                break;
            }
            case EPIC: {
                s.play(drone(), 0.55f, 0.5f);
                s.play(bass(), 0.45f, 0.5f);
                break;
            }
            case LEGENDARY: {
                s.play(drone(), 0.55f, 0.5f);
                s.play(bass(), 0.5f, 0.5f);
                break;
            }
            case MYTHIC: {
                s.play(drone(), 0.6f, 0.5f);
                s.play(bass(), 0.5f, 0.5f);
                break;
            }
        }
    }

    public static interface Sink {
        public void play(SoundEvent var1, float var2, float var3);
    }
}
