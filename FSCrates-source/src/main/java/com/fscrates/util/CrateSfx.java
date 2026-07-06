package com.fscrates.util;

import com.fscrates.config.Rarity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

// PALETA DE SONIDO — REWORK v3 "ABISMO" (2.9.34).
//
// POR QUE se rehizo otra vez: la v2 ("boveda ancestral", 2.9.33) apilaba MUCHOS sonidos
// METALICOS y PERCUSIVOS a la vez (IRON_GOLEM_ATTACK = clang metalico, IRON_TRAPDOOR = creak
// metalico, CHAIN_HIT, LODESTONE_HIT). Cuatro golpes de metal simultaneos = mezcla dura,
// sucia y chillona. El usuario la describio como "lo mas horrible que he escuchado".
//
// NUEVA FILOSOFIA: SUAVE y PROFUNDO, no metalico y saturado. Una FAMILIA sonora COHERENTE
// (agua profunda + espectros + ecos etereos) que se MEZCLA en vez de chocar. MAXIMO 2-3
// capas por evento. Sin un solo sonido metalico/percusivo. Los gemidos espectrales de
// almas (lo unico que al usuario le gusta) son el CORAZON emocional.
//
// Capas (TODAS suaves/profundas, ninguna prohibida, ningun bloque musical, ninguna
// vocalizacion de mob agresiva):
//   - DRONE ESPECTRAL grave     = AMBIENT_SOUL_SAND_VALLEY_LOOP (fondo de ultratumba).
//   - GEMIDOS DE ALMAS          = AMBIENT_SOUL_SAND_VALLEY_ADDITIONS/_MOOD (protagonistas).
//   - GOLPE PROFUNDO LIMPIO      = AMBIENT_UNDERWATER_ENTER ("whoomph" grave y redondo; NO
//                                  es un clang, es una onda de presion -> el impacto de la
//                                  tapa y del premio, cinematografico y limpio).
//   - RESACA / DESCENSO          = AMBIENT_UNDERWATER_EXIT ("whoomph" de salida; aterrizaje
//                                  de la caja y cierre).
//   - LECHO DE TENSION           = AMBIENT_UNDERWATER_LOOP (colchon grave sostenido durante
//                                  la carga/ruleta; da cuerpo sin ensuciar).
//   - BRILLO ETEREO / MAGIA      = AMBIENT_UNDERWATER_LOOP_ADDITIONS (ecos raros tipo
//                                  "canto de ballena" -> destellos magicos suaves) y su
//                                  version rara AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE
//                                  para los picos (estallido / reveal).
//   - TICK DE RULETA             = UI_STONECUTTER_SELECT_RECIPE (un "tik" limpio y corto, a
//                                  volumen bajo; nada musical, nada metalico pesado).
//
// Reglas duras de siempre: pitch SIEMPRE >=0.5; volumenes moderados; NO desincronizar
// REVEAL=294; nada de la lista negra (warden, ghast, sculk, lightning/tnt, trident_thunder,
// raid horn, campanas, amatista, xp, totems, cohetes, portal, enderman, bloques musicales,
// yunques, subir de nivel, click de boton UI, y AHORA tampoco metal duro apilado).
public final class CrateSfx {
    private CrateSfx() {
    }

    // Familia sonora (atajos legibles). Soul sand = Holder -> .value(); underwater = directo.
    private static SoundEvent drone()     { return SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP.value(); }
    private static SoundEvent wailA()      { return SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS.value(); }
    private static SoundEvent wailM()      { return SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(); }
    private static SoundEvent swellUp()    { return SoundEvents.AMBIENT_UNDERWATER_ENTER; }
    private static SoundEvent swellDown()  { return SoundEvents.AMBIENT_UNDERWATER_EXIT; }
    private static SoundEvent deepBed()    { return SoundEvents.AMBIENT_UNDERWATER_LOOP; }
    private static SoundEvent eerie()      { return SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS; }
    private static SoundEvent eerieRare()  { return SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE; }

    // t=2 — EL ABISMO DESPIERTA: el drone espectral se enciende grave + el primer lamento
    // lejano de un alma. Nada mas: espacio, profundidad, misterio.
    public static void unlock(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(drone(), 0.6f, 0.6f);
                s.play(wailM(), 0.8f, 1.0f);
                break;
            }
            case RARE: {
                s.play(drone(), 0.65f, 0.6f);
                s.play(wailM(), 0.85f, 1.0f);
                break;
            }
            case EPIC: {
                s.play(drone(), 0.7f, 0.55f);
                s.play(wailA(), 0.85f, 1.0f);
                break;
            }
            case LEGENDARY: {
                s.play(drone(), 0.75f, 0.55f);
                s.play(wailA(), 0.9f, 0.95f);
                s.play(deepBed(), 0.4f, 0.7f);
                break;
            }
            case MYTHIC: {
                s.play(drone(), 0.8f, 0.5f);
                s.play(wailA(), 0.9f, 0.9f);
                s.play(deepBed(), 0.45f, 0.7f);
                break;
            }
        }
    }

    // t=6 — ARRANCA LA ATRACCION: drone + lecho de tension grave (deepBed) que empieza a
    // crecer + gemido. Sin golpes: solo presion que se acumula.
    public static void spiralCharge(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(drone(), 0.7f, 0.6f);
                s.play(deepBed(), 0.45f, 0.7f);
                s.play(wailM(), 0.8f, 1.0f);
                break;
            }
            case RARE: {
                s.play(drone(), 0.75f, 0.6f);
                s.play(deepBed(), 0.5f, 0.7f);
                s.play(wailM(), 0.85f, 1.0f);
                break;
            }
            case EPIC: {
                s.play(drone(), 0.8f, 0.55f);
                s.play(deepBed(), 0.55f, 0.65f);
                s.play(wailA(), 0.85f, 0.95f);
                break;
            }
            case LEGENDARY: {
                s.play(drone(), 0.85f, 0.55f);
                s.play(deepBed(), 0.6f, 0.65f);
                s.play(wailA(), 0.9f, 0.9f);
                break;
            }
            case MYTHIC: {
                s.play(drone(), 0.9f, 0.5f);
                s.play(deepBed(), 0.65f, 0.6f);
                s.play(wailA(), 0.9f, 0.85f);
                break;
            }
        }
    }

    // LA MAREA QUE SUBE. El caller la dispara en un intervalo que se ACORTA (acelera) de
    // t=6 a t=64. Cada llamada NO es un golpe: es una ONDA DE PRESION suave (swellUp) que
    // sube en tono y volumen con p -> la energia inflandose. Los gemidos crecen debajo, y en
    // tension alta aparecen ECOS ETEREOS (eerie) como destellos magicos. Cero metal, cero
    // percusion: la sensacion es de algo enorme cargandose bajo el agua.
    public static void spiralRise(Sink s, Rarity r, float p) {
        float swellVol = 0.35f + p * 0.4f;
        float swellPitch = 0.5f + p * 0.35f;
        switch (r) {
            case COMMON: {
                s.play(swellUp(), swellVol * 0.8f, swellPitch);
                if (p > 0.55f) {
                    s.play(wailM(), 0.4f + p * 0.4f, 0.95f);
                }
                break;
            }
            case RARE: {
                s.play(swellUp(), swellVol * 0.85f, swellPitch);
                if (p > 0.45f) {
                    s.play(wailM(), 0.45f + p * 0.4f, 0.95f);
                }
                break;
            }
            case EPIC: {
                s.play(swellUp(), swellVol * 0.9f, swellPitch);
                s.play(wailA(), 0.45f + p * 0.4f, 0.9f);
                break;
            }
            case LEGENDARY: {
                s.play(swellUp(), swellVol, swellPitch);
                s.play(wailA(), 0.5f + p * 0.4f, 0.85f);
                if (p > 0.6f) {
                    s.play(eerie(), 0.5f, 0.7f);
                }
                break;
            }
            case MYTHIC: {
                s.play(swellUp(), swellVol, swellPitch);
                s.play(wailA(), 0.55f + p * 0.4f, 0.8f);
                if (p > 0.5f) {
                    s.play(eerie(), 0.55f, 0.6f);
                }
                break;
            }
        }
    }

    // ~t=64 — INHALACION: el lecho grave y el gemido llegan a su cima + un eco etereo que
    // anuncia el estallido. El silencio-lleno justo antes de reventar.
    public static void spiralPeak(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(deepBed(), 0.7f, 0.6f);
                s.play(wailM(), 0.95f, 1.0f);
                break;
            }
            case RARE: {
                s.play(deepBed(), 0.75f, 0.6f);
                s.play(wailA(), 0.95f, 0.95f);
                break;
            }
            case EPIC: {
                s.play(deepBed(), 0.8f, 0.55f);
                s.play(wailA(), 0.95f, 0.9f);
                s.play(eerie(), 0.5f, 0.7f);
                break;
            }
            case LEGENDARY: {
                s.play(deepBed(), 0.85f, 0.55f);
                s.play(wailA(), 1.0f, 0.85f);
                s.play(eerie(), 0.55f, 0.65f);
                break;
            }
            case MYTHIC: {
                s.play(deepBed(), 0.9f, 0.5f);
                s.play(wailA(), 1.0f, 0.8f);
                s.play(eerieRare(), 0.6f, 0.6f);
                break;
            }
        }
    }

    // t=76 — EL ESTALLIDO (la tapa revienta; EXACTO con el fogonazo del fondo). El nucleo es
    // un GOLPE PROFUNDO LIMPIO (swellUp = onda de presion grave, NO un clang) + el gemido en
    // su punto mas alto. Mas rareza = mas cuerpo (segunda onda una octava arriba + eco
    // etereo raro). Cinematografico y REDONDO, sin dureza metalica.
    public static void openAccent(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(swellUp(), 0.9f, 0.5f);
                s.play(wailM(), 0.85f, 1.0f);
                break;
            }
            case RARE: {
                s.play(swellUp(), 0.95f, 0.5f);
                s.play(wailA(), 0.85f, 0.95f);
                break;
            }
            case EPIC: {
                s.play(swellUp(), 1.0f, 0.5f);
                s.play(swellUp(), 0.6f, 0.75f);
                s.play(wailA(), 0.9f, 0.9f);
                break;
            }
            case LEGENDARY: {
                s.play(swellUp(), 1.0f, 0.5f);
                s.play(swellUp(), 0.65f, 0.8f);
                s.play(wailA(), 0.9f, 0.85f);
                s.play(eerieRare(), 0.55f, 0.7f);
                break;
            }
            case MYTHIC: {
                s.play(swellUp(), 1.0f, 0.5f);
                s.play(swellUp(), 0.7f, 0.85f);
                s.play(wailA(), 0.95f, 0.9f);
                s.play(eerieRare(), 0.6f, 0.6f);
                break;
            }
        }
    }

    // Lecho sostenido opcional (no lo usa el fullscreen; disponible por compatibilidad).
    public static void openSustain(Sink s, Rarity r, float p) {
        float v = 0.4f + p * 0.35f;
        s.play(deepBed(), v * 0.7f, 0.6f);
        switch (r) {
            case COMMON: {
                break;
            }
            case RARE: {
                s.play(wailM(), v * 0.6f, 0.95f);
                break;
            }
            case EPIC: {
                s.play(wailM(), v * 0.7f, 0.9f);
                break;
            }
            case LEGENDARY: {
                s.play(wailA(), v * 0.75f, 0.85f);
                break;
            }
            case MYTHIC: {
                s.play(wailA(), v * 0.85f, 0.8f);
                break;
            }
        }
    }

    // t=294 — EL PREMIO EMERGE (EXACTO con la aparicion del premio). Onda de presion grande
    // (swellUp) + gemido BRILLANTE (pitch alto -> se siente triunfal, no lugubre) + destello
    // etereo. Misma familia que el estallido pero mas luminoso: "algo maravilloso sube del
    // abismo".
    public static void win(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(swellUp(), 0.9f, 0.65f);
                s.play(wailA(), 0.85f, 1.15f);
                s.play(eerie(), 0.5f, 1.0f);
                break;
            }
            case RARE: {
                s.play(swellUp(), 0.95f, 0.65f);
                s.play(wailA(), 0.85f, 1.2f);
                s.play(eerie(), 0.55f, 1.0f);
                break;
            }
            case EPIC: {
                s.play(swellUp(), 1.0f, 0.6f);
                s.play(wailA(), 0.9f, 1.25f);
                s.play(eerie(), 0.6f, 0.95f);
                break;
            }
            case LEGENDARY: {
                s.play(swellUp(), 1.0f, 0.55f);
                s.play(wailA(), 0.9f, 1.3f);
                s.play(eerieRare(), 0.6f, 0.9f);
                break;
            }
            case MYTHIC: {
                s.play(swellUp(), 1.0f, 0.5f);
                s.play(wailA(), 0.95f, 1.3f);
                s.play(eerieRare(), 0.65f, 0.8f);
                break;
            }
        }
    }

    // Cola: el eco del premio se disuelve en el abismo (resaca suave + gemido que se apaga).
    public static void winTail(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(swellDown(), 0.45f, 0.7f);
                s.play(wailM(), 0.55f, 1.1f);
                break;
            }
            case RARE: {
                s.play(swellDown(), 0.5f, 0.7f);
                s.play(wailM(), 0.6f, 1.05f);
                break;
            }
            case EPIC: {
                s.play(swellDown(), 0.5f, 0.65f);
                s.play(wailM(), 0.65f, 1.0f);
                break;
            }
            case LEGENDARY: {
                s.play(swellDown(), 0.55f, 0.65f);
                s.play(wailA(), 0.7f, 0.95f);
                break;
            }
            case MYTHIC: {
                s.play(swellDown(), 0.6f, 0.6f);
                s.play(wailA(), 0.75f, 0.9f);
                s.play(wailM(), 0.5f, 1.1f);
                break;
            }
        }
    }

    // Cierre (in-world): la resaca del abismo (swellDown) + el drone que se apaga.
    public static void close(Sink s, Rarity r) {
        switch (r) {
            case COMMON: {
                s.play(swellDown(), 0.5f, 0.7f);
                break;
            }
            case RARE: {
                s.play(swellDown(), 0.5f, 0.65f);
                s.play(drone(), 0.4f, 0.5f);
                break;
            }
            case EPIC: {
                s.play(swellDown(), 0.55f, 0.65f);
                s.play(drone(), 0.45f, 0.5f);
                break;
            }
            case LEGENDARY: {
                s.play(swellDown(), 0.55f, 0.6f);
                s.play(drone(), 0.5f, 0.5f);
                break;
            }
            case MYTHIC: {
                s.play(swellDown(), 0.6f, 0.6f);
                s.play(drone(), 0.5f, 0.5f);
                break;
            }
        }
    }

    // Textura de lecho grave para la ruleta (opcional, disponible para llamadores que
    // quieran una capa extra de ambiente durante el giro).
    public static void spinWind(Sink s, float vol) {
        s.play(deepBed(), vol, 0.6f);
    }

    public static interface Sink {
        public void play(SoundEvent var1, float var2, float var3);
    }
}
