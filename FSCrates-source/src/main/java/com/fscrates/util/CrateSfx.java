package com.fscrates.util;

import com.fscrates.config.Rarity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

// PALETA DE SONIDO — REWORK TOTAL "BOVEDA ANCESTRAL" (2.9.33). El usuario rechazo la paleta
// "ritual oscuro" de bloques musicales (NOTE_BLOCK_BASEDRUM/DIDGERIDOO/BASS/HARP/HAT) por
// sonar "una mierda". Esta paleta es COMPLETAMENTE NUEVA: CERO bloques musicales, CERO
// vocalizaciones de mobs (nada de gruñidos/roars/gemidos de monstruo -> el usuario esta
// cansado de esa categoria entera), solo IMPACTOS, MECANISMOS y MAGIA vanilla + los gemidos
// espectrales que el usuario AMA (se mantienen como protagonistas absolutos).
//
// Identidad de cada capa (todas vanilla, ninguna prohibida):
//   - DRONE OMINOSO SOSTENIDO = AMBIENT_SOUL_SAND_VALLEY_LOOP (el "fondo" de la misma
//                                familia que los gemidos que le gustan -> coherencia total).
//   - GEMIDOS ESPECTRALES      = AMBIENT_SOUL_SAND_VALLEY_ADDITIONS/_MOOD (protagonistas).
//   - PULSO RITUAL (late)      = LODESTONE_HIT (golpe metalico grave y resonante, tipo gong
//                                 de altar magico; reemplaza el tambor de bloque musical).
//   - ACENTO METALICO agudo    = CHAIN_HIT (clank corto y afilado; puntuacion de tension).
//   - IMPACTO EPICO (boom)     = IRON_GOLEM_ATTACK (golpe pesado y grande; el "punch" del
//                                 estallido de la tapa y del premio).
//   - MECANISMO DE TAPA        = IRON_TRAPDOOR_OPEN (creak metalico pesado que vende
//                                 fisicamente la tapa reventando) / IRON_TRAPDOOR_CLOSE.
//   - COFRE MAGICO abriendo    = SHULKER_OPEN / SHULKER_CLOSE (el propio vanilla para un
//                                 contenedor que se abre/cierra; encaja perfecto con un cofre).
//   - DESTELLO MAGICO (warp)   = SHULKER_TELEPORT (shimmer etereo; carga y reveal).
//   - REGALO / FLOURISH        = ALLAY_ITEM_GIVEN (chime magico calido: "recibes un regalo").
//   - OLEADA DE ENERGIA        = TRIDENT_RIPTIDE_2 / TRIDENT_RIPTIDE_3 (silbido magico
//                                 ascendente; NO es el trueno del trident -ese esta prohibido-,
//                                 es el vortice/silbido propio del riptide, distinto timbre).
//   - VIENTO/ENERGIA EN MOVIMIENTO = ELYTRA_FLYING (whoosh sostenido; textura durante el giro).
//   - IMPACTO DE ATERRIZAJE    = DEEPSLATE_HIT (golpe seco y grave al caer la caja).
//   - TICK DE RULETA           = COMPARATOR_CLICK (click mecanico corto, nada musical).
//
// Reglas de siempre: pitch SIEMPRE >=0.5; POCAS capas por evento (3-4 max) para NO clippear;
// NADA de sonidos prohibidos (ver historial: warden, ghast, sculk, vex, lightning/tnt,
// trident_thunder, raid horn, campanas, amatista, xp, totems, cohetes, portal, enderman,
// bloques musicales, yunques, subir de nivel, click de boton UI).
public final class CrateSfx {
    private CrateSfx() {
    }

    // Atajos legibles.
    private static SoundEvent drone() { return SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP.value(); }
    private static SoundEvent wailA() { return SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS.value(); }
    private static SoundEvent wailM() { return SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(); }
    private static SoundEvent pulse() { return SoundEvents.LODESTONE_HIT; }
    private static SoundEvent clank() { return SoundEvents.CHAIN_HIT; }
    private static SoundEvent boom() { return SoundEvents.IRON_GOLEM_ATTACK; }
    private static SoundEvent lidCreakOpen() { return SoundEvents.IRON_TRAPDOOR_OPEN; }
    private static SoundEvent lidCreakClose() { return SoundEvents.IRON_TRAPDOOR_CLOSE; }
    private static SoundEvent boxOpen() { return SoundEvents.SHULKER_OPEN; }
    private static SoundEvent boxClose() { return SoundEvents.SHULKER_CLOSE; }
    private static SoundEvent shimmer() { return SoundEvents.SHULKER_TELEPORT; }
    private static SoundEvent giftChime() { return SoundEvents.ALLAY_ITEM_GIVEN; }
    private static SoundEvent riptide2() { return SoundEvents.TRIDENT_RIPTIDE_2; }
    private static SoundEvent riptide3() { return SoundEvents.TRIDENT_RIPTIDE_3; }
    private static SoundEvent wind() { return SoundEvents.ELYTRA_FLYING; }

    // t=2 — LA BOVEDA DESPIERTA: drone grave que se enciende + el primer lamento lejano de
    // las almas. Sin metal todavia (eso llega al arrancar el ritual, spiralCharge).
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
                s.play(clank(), 0.5f, 0.5f);
                break;
            }
            case MYTHIC: {
                s.play(drone(), 0.9f, 0.5f);
                s.play(wailA(), 0.95f, 0.9f);
                s.play(clank(), 0.55f, 0.5f);
                break;
            }
        }
    }

    // t=6 — ARRANCA EL RITUAL: drone + primer golpe metalico grave (pulse) + lamento de fondo.
    public static void spiralCharge(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(drone(), 0.8f, 0.5f);
                s.play(pulse(), 0.6f, 0.5f);
                s.play(wailM(), 0.85f, 1.0f);
                break;
            }
            case RARE: {
                s.play(drone(), 0.85f, 0.5f);
                s.play(pulse(), 0.65f, 0.5f);
                s.play(wailM(), 0.9f, 1.0f);
                break;
            }
            case EPIC: {
                s.play(drone(), 0.9f, 0.5f);
                s.play(pulse(), 0.7f, 0.5f);
                s.play(wailA(), 0.9f, 0.95f);
                break;
            }
            case LEGENDARY: {
                s.play(drone(), 0.95f, 0.5f);
                s.play(pulse(), 0.75f, 0.5f);
                s.play(wailA(), 0.95f, 0.9f);
                s.play(clank(), 0.55f, 0.5f);
                break;
            }
            case MYTHIC: {
                s.play(drone(), 1.0f, 0.5f);
                s.play(pulse(), 0.8f, 0.5f);
                s.play(wailA(), 0.95f, 0.85f);
                s.play(clank(), 0.6f, 0.5f);
                break;
            }
        }
    }

    // EL PULSO RITUAL. El caller la dispara en un intervalo que se ACORTA (acelera) de t=6
    // a t=64: cada llamada es un golpe metalico grave (pulse), con el tono subiendo levemente
    // con p (tension), un acento metalico agudo (clank) y el lamento creciendo. En tension
    // alta se suma una OLEADA DE ENERGIA (riptide) ascendente -> sensacion de poder
    // acumulandose. Cada golpe empuja un PULSO VISUAL de la caja (lastPulseTick en pantalla).
    public static void spiralRise(Sink s, Rarity r, float p) {
        float vol = 0.55f + p * 0.4f;
        float pulsePitch = 0.5f + p * 0.24f;
        float clankPitch = 0.5f + p * 0.22f;
        switch (r) {
            case COMMON: {
                s.play(pulse(), vol, pulsePitch);
                if (p > 0.5f) {
                    s.play(wailM(), 0.4f + p * 0.45f, 0.95f);
                }
                break;
            }
            case RARE: {
                s.play(pulse(), vol, pulsePitch);
                s.play(clank(), vol * 0.55f, clankPitch);
                if (p > 0.45f) {
                    s.play(wailM(), 0.45f + p * 0.45f, 0.95f);
                }
                break;
            }
            case EPIC: {
                s.play(pulse(), vol, pulsePitch);
                s.play(clank(), vol * 0.6f, clankPitch);
                s.play(wailA(), 0.5f + p * 0.45f, 0.9f);
                break;
            }
            case LEGENDARY: {
                s.play(pulse(), vol, pulsePitch);
                s.play(clank(), vol * 0.65f, clankPitch);
                s.play(wailA(), 0.55f + p * 0.45f, 0.85f);
                if (p > 0.6f) {
                    s.play(riptide2(), 0.55f, 0.5f);
                }
                break;
            }
            case MYTHIC: {
                s.play(pulse(), vol, pulsePitch);
                s.play(clank(), vol * 0.7f, clankPitch);
                s.play(wailA(), 0.6f + p * 0.4f, 0.8f);
                if (p > 0.45f) {
                    s.play(riptide3(), 0.6f, 0.5f);
                }
                break;
            }
        }
    }

    // ~t=64 — INHALACION/WINDUP: drone a tope + lamento a tope + un DESTELLO MAGICO
    // (shimmer) que anuncia que la energia esta a punto de reventar. Anticipacion pura,
    // justo antes del estallido.
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
                s.play(shimmer(), 0.5f, 0.6f);
                break;
            }
            case EPIC: {
                s.play(drone(), 1.0f, 0.5f);
                s.play(wailA(), 1.0f, 0.9f);
                s.play(shimmer(), 0.55f, 0.6f);
                break;
            }
            case LEGENDARY: {
                s.play(drone(), 1.0f, 0.5f);
                s.play(wailA(), 1.0f, 0.85f);
                s.play(wailM(), 0.7f, 1.0f);
                s.play(shimmer(), 0.6f, 0.55f);
                break;
            }
            case MYTHIC: {
                s.play(drone(), 1.0f, 0.5f);
                s.play(wailA(), 1.0f, 0.8f);
                s.play(wailM(), 0.75f, 0.95f);
                s.play(shimmer(), 0.65f, 0.5f);
                break;
            }
        }
    }

    // t=76 — EL ESTALLIDO (la tapa revienta; cae EXACTO con el fogonazo del fondo). Capas
    // FIJAS para TODAS las rarezas (representan la accion fisica de la tapa): el CREAK de
    // mecanismo pesado (lidCreakOpen) + el IMPACTO grande (boom). Encima, mas rareza = mas
    // riqueza (doble impacto en octava / acento de cofre magico) y el lamento crece.
    public static void openAccent(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(lidCreakOpen(), 0.7f, 0.55f);
                s.play(boom(), 1.0f, 0.5f);
                s.play(wailM(), 0.85f, 1.0f);
                break;
            }
            case RARE: {
                s.play(lidCreakOpen(), 0.72f, 0.55f);
                s.play(boom(), 1.0f, 0.5f);
                s.play(wailA(), 0.85f, 0.95f);
                break;
            }
            case EPIC: {
                s.play(lidCreakOpen(), 0.75f, 0.55f);
                s.play(boom(), 1.0f, 0.5f);
                s.play(boxOpen(), 0.55f, 0.6f);
                s.play(wailA(), 0.9f, 0.9f);
                break;
            }
            case LEGENDARY: {
                s.play(lidCreakOpen(), 0.78f, 0.52f);
                s.play(boom(), 1.0f, 0.5f);
                s.play(boom(), 0.8f, 0.62f);
                s.play(wailA(), 0.9f, 0.85f);
                break;
            }
            case MYTHIC: {
                s.play(lidCreakOpen(), 0.8f, 0.5f);
                s.play(boom(), 1.0f, 0.5f);
                s.play(boom(), 0.88f, 0.62f);
                s.play(wailA(), 0.95f, 0.9f);
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

    // t=294 — EXPLOSION DEL PREMIO (cae EXACTO cuando aparece el premio). IMPACTO final +
    // DESTELLO MAGICO (shimmer) + CHIME DE REGALO (giftChime, "recibes algo") + lamento.
    // Firma sonora CONGRUENTE con la apertura (mismo impacto) pero mas brillante/triunfal.
    public static void win(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(boom(), 1.0f, 0.6f);
                s.play(wailA(), 0.9f, 1.1f);
                s.play(giftChime(), 0.5f, 1.0f);
                break;
            }
            case RARE: {
                s.play(boom(), 1.0f, 0.6f);
                s.play(wailA(), 0.9f, 1.0f);
                s.play(giftChime(), 0.55f, 1.05f);
                s.play(shimmer(), 0.45f, 0.6f);
                break;
            }
            case EPIC: {
                s.play(boom(), 1.0f, 0.58f);
                s.play(wailA(), 0.9f, 0.95f);
                s.play(giftChime(), 0.55f, 1.1f);
                s.play(shimmer(), 0.5f, 0.58f);
                break;
            }
            case LEGENDARY: {
                s.play(boom(), 1.0f, 0.55f);
                s.play(wailA(), 0.9f, 0.9f);
                s.play(giftChime(), 0.58f, 1.15f);
                s.play(shimmer(), 0.55f, 0.55f);
                break;
            }
            case MYTHIC: {
                s.play(boom(), 1.0f, 0.5f);
                s.play(wailA(), 0.95f, 0.9f);
                s.play(giftChime(), 0.62f, 1.2f);
                s.play(shimmer(), 0.6f, 0.5f);
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

    // Cierre (in-world): el mecanismo del cofre se cierra + el drone se apaga.
    public static void close(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(drone(), 0.5f, 0.5f);
                break;
            }
            case RARE: {
                s.play(drone(), 0.5f, 0.5f);
                s.play(boxClose(), 0.4f, 0.55f);
                break;
            }
            case EPIC: {
                s.play(drone(), 0.55f, 0.5f);
                s.play(boxClose(), 0.45f, 0.55f);
                break;
            }
            case LEGENDARY: {
                s.play(drone(), 0.55f, 0.5f);
                s.play(lidCreakClose(), 0.5f, 0.52f);
                break;
            }
            case MYTHIC: {
                s.play(drone(), 0.6f, 0.5f);
                s.play(lidCreakClose(), 0.5f, 0.5f);
                break;
            }
        }
    }

    // Textura de "viento/energia en movimiento" para la ruleta (opcional, disponible para
    // llamadores que quieran una capa extra de ambiente durante el giro).
    public static void spinWind(Sink s, float vol) {
        s.play(wind(), vol, 0.6f);
    }

    public static interface Sink {
        public void play(SoundEvent var1, float var2, float var3);
    }
}
