# FSCrates — Contexto / Handoff (estado actual)

> Documento de traspaso. Describe QUÉ es el mod, CÓMO está montado y EN QUÉ PUNTO está
> el trabajo. No propone soluciones: solo explica el estado y el historial para que otra
> IA (u otra persona) pueda continuar.

## 0. RUTAS EXACTAS (dónde está todo)

> La fuente que se COMPILA vive fuera de git, en `work6/mod`. En el repo hay una COPIA
> (`FSCrates-source/`). Si trabajas clonando el repo, edita la copia o replica esa carpeta
> a `work6/mod` para compilar. **Mantener ambas sincronizadas.**
>
> **Nota de sandbox:** en un sandbox NUEVO (sesión desde cero) `work6/mod` NO existe todavía
> — solo la copia en git. Recrearla con `cp -r FSCrates-source/. work6/mod/` antes de compilar.

### Fuente del mod (editar + compilar) — NO es git
```
/projects/sandbox/work6/mod/
```
Archivos clave:
```
/projects/sandbox/work6/mod/src/main/java/com/fscrates/client/screen/CrateCinematicScreen.java
/projects/sandbox/work6/mod/src/main/java/com/fscrates/util/CrateSfx.java
/projects/sandbox/work6/mod/src/main/java/com/fscrates/block/CrateBlockEntity.java
/projects/sandbox/work6/mod/src/main/java/com/fscrates/client/ClientPacketHandler.java
/projects/sandbox/work6/mod/src/main/java/com/fscrates/crate/CrateCinematicTiming.java
/projects/sandbox/work6/mod/src/main/java/com/fscrates/mixin/LevelRendererMixin.java
/projects/sandbox/work6/mod/build.gradle                              (version = '2.9.33', linea 20)
/projects/sandbox/work6/mod/src/main/resources/META-INF/mods.toml     (version="2.9.33")
```
Jar de salida del build:
```
/projects/sandbox/work6/mod/build/libs/fscrates-2.9.33.jar
```

### Repo git (lo que se pushea)
```
/projects/sandbox/Rangos/                                 (repo, branch main)
/projects/sandbox/Rangos/FantasticCratesActualizar.jar    (jar entregado)
/projects/sandbox/Rangos/FSCrates-source/                 (COPIA de work6/mod en git)
/projects/sandbox/Rangos/FSCrates-HANDOFF.md              (este documento)
```
- Remoto: https://github.com/pewez267-dot/Rangos (branch `main`)
- Descarga directa del jar: https://github.com/pewez267-dot/Rangos/raw/main/FantasticCratesActualizar.jar
- Java: `/root/.local/share/mise/installs/java/17.0.2`

## 1. Qué es

- **Mod:** FSCrates (Fantastic Crates), Minecraft **Forge 1.20.1**, **Java 17**.
- **Versión actual:** `2.9.40` (en `build.gradle` y `src/main/resources/META-INF/mods.toml`).
- **Feature en la que se ha estado trabajando:** la **cinemática de apertura de crates**
  (cofres), en particular el **sonido**. Todo lo demás del mod (bloques, config, comandos,
  red, etc.) no se ha tocado salvo el fix puntual descrito en la sección 9.

## 2. Repositorio y entrega

- **Repo:** https://github.com/pewez267-dot/Rangos (branch `main`).
- **Fuente del mod (ahora en git):** `FSCrates-source/` en este repo (48 archivos Java +
  gradle wrapper; sin artefactos `build/`/`.gradle/`).
- **Jar entregado:** `FantasticCratesActualizar.jar` en la raíz del repo.
- **Descarga directa (siempre la última):**
  https://github.com/pewez267-dot/Rangos/raw/main/FantasticCratesActualizar.jar
- **Convención de entrega:** compilar → copiar el jar a `FantasticCratesActualizar.jar` →
  verificar md5 (build vs entregado) → `git commit` → push a `main`. El push se hace con el
  **power "github"** (no `git push` directo, falla auth).

### Comando de build (entorno sandbox usado)
```
cd /projects/sandbox/work6/mod && export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 \
 && export PATH="$JAVA_HOME/bin:$PATH" \
 && timeout 500 java -Xmx3G -cp gradle/wrapper/gradle-wrapper.jar \
    org.gradle.wrapper.GradleWrapperMain --no-daemon build -x test
```
(Usar `compileJava` en vez de `build` para iterar rápido, ~8-12s. El primer build en un
sandbox nuevo descarga Gradle 8.1.1 + Forge/MCP (~3 min); usa `--info` si parece colgado,
Gradle no imprime nada mientras descarga en modo normal.)

## 3. Arquitectura de la cinemática

Hay **DOS vistas** de la misma apertura:

1. **Pantalla fullscreen del que abre** — `client/screen/CrateCinematicScreen.java`.
   Secuestra la vista del jugador que abre. Dibuja caja 3D cayendo, tapa, ruleta de items,
   reveal del premio, fondo galaxia, partículas, flashes, shake.
2. **Escena in-world para los espectadores** — `block/CrateBlockEntity.java`
   (`sceneLidMode`). Los DEMÁS jugadores ven la crate física del suelo abrir la tapa
   parcialmente + partículas + sonidos. No ven ruleta/hologramas.

Ambas comparten `crate/CrateCinematicTiming.REVEAL_TICK = 294` (tick cosmético en el que
aparece el premio; **crítico no desincronizar** — la recompensa real la da el server aparte).

### Reloj (clave, ya resuelto)
- `render()` usa un **reloj VISUAL de tiempo real**: `float t = (nanoTime - openNanos)/50ms`
  (50ms = 1 tick). Es fluido a los FPS reales, desacoplado del tick-loop del juego.
- **TODO el sonido corre sobre ESE reloj**, no sobre `this.ticks`: en `render()` hay un bucle
  `while (lastSoundTick < floor(t)) { playAtmosphere(++lastSoundTick); advanceRaritySounds(...) }`.
  Antes el sonido iba en `this.ticks` (20 Hz) y NUNCA caía exacto con el flash/animación —
  eso causaba la queja recurrente de "no está sincronizado". Ya está corregido: el estallido
  cae en `t=BURST` y la explosión del premio en `t=REVEAL`, exactos con los efectos.
- `tick()` solo incrementa `this.ticks` y cierra la escena en `TOTAL`.

### Constantes de timing (en `CrateCinematicScreen`, reloj visual)
`TOTAL=400, LAND=24, LID_START=56, BURST=76, LID_END=82, ROLL_START=88, ROLL_END=288, REVEAL=294`.
- 0→24: la caja cae.
- 24→56: **fase de carga ritual** (tapa cerrada; la caja late/pulsa, aura, partículas
  convergen).
- 56→82: la tapa abre (easeIn p²) y REVIENTA; `BURST=76` = estallido + flash de fondo.
- 88→288: ruleta girando; para en 288 (easeOutCubic).
- 294: reveal del premio (burst de partículas + onda de choque).
- In-world espejo: `SCENE_LID_START=56, SCENE_BURST=76, SCENE_LID_END=82, SCENE_CLOSE_START=362`.

## 4. Archivos clave

- `client/screen/CrateCinematicScreen.java` — pantalla fullscreen. Contiene: cálculo de `t`,
  bucle de sonido, `playAtmosphere`, `advanceRaritySounds` (secuencia + "bed" de la ruleta),
  `renderCrate` (curva de caída/tapa + throb), `renderMouthGlow`, `renderChargeFx` (fase de
  carga), `renderSparks`, `renderSceneBackground` (fondo/flashes), `renderRoulette`,
  `renderReveal/Burst/ShockwaveRing`, `updateReelClicks` (tick de la ruleta).
- `util/CrateSfx.java` — sistema de sonido de la pantalla. `Sink.play(ev, VOL, PITCH)`.
  Métodos por rareza: `unlock, spiralCharge, spiralRise, spiralPeak, openAccent, openSustain,
  win, winTail, close`. Ver sección 7 para la paleta actual (2.9.33).
- `block/CrateBlockEntity.java` — escena in-world. **Espeja** las mismas paletas en
  `playUnlock/playSpiralCharge/playSpiralRise/playSpiralPeak/playOpenAccent/playWin/playWinTail/
  playClose`. `advanceSceneSounds` dispara: openAccent@76, win@294, winTail@300, close@362.
  Tiene el campo `muteAudio` (ver sección 9, fix del eco).
- `client/ClientPacketHandler.java` — despacha la cinemática/escena al cliente. Decide si el
  jugador local es el `opener` y llama `be.startSceneLid(color, rareza, muteAudio)`.
- `crate/CrateCinematicTiming.java` — solo `REVEAL_TICK=294`.
- `mixin/LevelRendererMixin.java` — corta el render del mundo 3D mientras la cinemática está
  abierta (ahorro de GPU). Requiere el plugin mixin + annotationProcessor en `build.gradle`
  para que se genere el **refmap** (`fscrates.refmap.json`) que mapea `renderLevel` → nombre
  SRG `m_109599_`. Sin ese refmap el mixin no aplica en producción.

## 5. Restricciones DURAS (siempre se han respetado)

- **Pitch SIEMPRE ≥ 0.5** (Minecraft clampa por debajo de 0.5). OJO al orden de args:
  `CrateSfx.Sink.play`/`CrateBlockEntity.play` = `(ev, VOL, PITCH)`; pero
  `CrateCinematicScreen.playUi` = `(ev, PITCH, VOL)`. El `sfxSink` invierte a propósito.
- **Evitar clipping:** pocas capas por evento (3-4), volúmenes moderados. Apilar 5-7 sonidos
  a vol 1.0 satura y suena a "distorsión" (fue una queja real).
- **No desincronizar** `REVEAL_TICK=294` entre pantalla e in-world.
- **Refmap** debe seguir existiendo y mapeando `m_109599_` (verificar en cada jar con
  `unzip -p <jar> fscrates.refmap.json`).

## 6. Verificación usada en cada entrega
```
grep de sonidos prohibidos (ver lista abajo) → debe estar vacío
grep de pitch<0.5 en play() (arg3) y playUi() (arg2) → vacío
unzip -p <jar> META-INF/mods.toml | grep version   → versión correcta
unzip -p <jar> fscrates.refmap.json | grep m_109599_ → presente
md5sum build/libs/<jar> y el copiado a Rangos → deben coincidir
```

## 7. Historial de SONIDO (lo importante para continuar)

El mod usa **solo sonidos vanilla** de Minecraft (el usuario dijo que **no tiene sonidos
custom**). Lista de lo RECHAZADO explícitamente (NO volver a usar NUNCA):

- Warden: `WARDEN_SONIC_BOOM`, `WARDEN_ROAR`, `WARDEN_NEARBY_*` (quejidos), `WARDEN_EMERGE`,
  `WARDEN_HEARTBEAT`, `WARDEN_SONIC_CHARGE` (el usuario está cansado del warden en general).
- `GHAST`, `SCULK_*` (chillador/sensor), `WARDEN_TENDRIL_CLICKS`, `VEX_CHARGE`,
  `ELDER_GUARDIAN_CURSE`, `SOUL_ESCAPE`.
- `LIGHTNING_BOLT_IMPACT`/`_THUNDER` (los oye como **TNT**), `TRIDENT_THUNDER` (el trueno, NO
  el riptide — ver más abajo, se usa `TRIDENT_RIPTIDE_2/3` que es un silbido distinto), `RAID_HORN`
  (lo oye distorsionado/horrible).
- `ENDER_CHEST_OPEN`, `BEACON_*`, `CONDUIT_*`, `EVOKER_CAST_SPELL`, `RESPAWN_ANCHOR_*`,
  `ENCHANTMENT_TABLE_USE`, `GENERIC_EXPLODE`, `GENERIC_BIG_FALL`, `WOOD_HIT`, `AMBIENT_CAVE`,
  ambientes de bioma del **Nether** (basalt/crimson/warped/nether_wastes = "chillido agudo").
- Prohibidos de siempre: portal, ender dragon, wither, campana (BELL), amatista, xp/subir de
  nivel, totems, cohetes (firework), enderman, click de botón UI.
- **Rechazo 2.9.32:** **bloques musicales** (`NOTE_BLOCK_BASEDRUM/DIDGERIDOO/BASS/HARP/HAT`),
  totems, yunques, xp, subir de nivel. El usuario dijo textualmente que el sonido de bloque
  musical "es una mierda" y que no lo quiere. **NO reintroducir NINGÚN `NOTE_BLOCK_*`.**
- El usuario también expresó estar **cansado de vocalizaciones de mobs en general**
  (gruñidos/roars/gemidos de monstruo tipo warden/ghast/elder guardian) — por eso la paleta
  2.9.33 evita CUALQUIER sonido de "voz de mob" salvo los gemidos de almas que le gustan.

**Lo ÚNICO que al usuario le gusta y quiere mantener:** los **gemidos espectrales / almas en
pena** = `AMBIENT_SOUL_SAND_VALLEY_ADDITIONS` y `AMBIENT_SOUL_SAND_VALLEY_MOOD`.

### Paleta ANTERIOR (2.9.32) — RECHAZADA por el usuario
"Ritual oscuro": tambor `NOTE_BLOCK_BASEDRUM` + drone `NOTE_BLOCK_DIDGERIDOO` + bajo
`NOTE_BLOCK_BASS` + arpa `NOTE_BLOCK_HARP` + gemidos soul sand valley. Tick de ruleta con
`NOTE_BLOCK_HAT`. Ver arriba: bloques musicales = rechazo explícito.

### AJUSTES 2.9.40 — 7 correcciones
1. **Sonido residual (viento) eliminado**: `AMBIENT_SOUL_SAND_VALLEY_LOOP` (el viento continuo
   del valle de almas, ~30s) que se añadió en 2.9.39 como "drone épico" se reproducía por UI
   y quedaba de fondo tras la escena. Se QUITÓ de openAccent/win (CrateSfx) y playOpenAccent/
   playWin (CrateBlockEntity). En su lugar, coro de almas one-shot (no deja residuo).
   **REGLA: NO usar `_LOOP` de ambientes en sonidos forUI/one-shot; dejan cola.**
2. **GUI partículas (solapado)**: los 6 campos del panel de capa ahora se alinean al borde
   derecho de su media-columna (`f1x`/`f2x`) → la etiqueta de la 2a columna ya no se pisa con
   el campo de la 1a en paneles estrechos.
3. **+26 partículas (presets fscrates)**: `RegistryLists.FS_PRESETS` + `CrateBlockEntity.
   resolveFsPreset` añaden 26 "partículas" propias (`fscrates:fs_*`): 12 polvos de color fijo,
   polvo diminuto/enorme, 5 degradados (fuego/hielo/vóid/tóxico/real), 4 esquirlas de gema
   (oro/diamante/amatista/esmeralda), estrella del Nether, gema, espiral de alma. Cada una
   mapea a una partícula vanilla que funciona (dust/dust_color_transition/block/item/
   sculk_charge) → sin texturas custom ni registro cliente. Nombres en `ParticleNames`.
4. **Gemidos más aterradores**: el coro espectral de openAccent/win usa moans MUY graves
   (pitch ~0.55, demoníaco) + grave + etéreo alto (one-shot).
5. **Galaxia reworkeada** (`renderSceneBackground`): NÚCLEO LUMINOSO definido (3 capas, centro
   casi blanco que late) + 2 brazos LIMPIOS y ORDENADOS (espiral apretada con muchos puntos
   PEQUEÑOS y densos que forman líneas suaves, no manchas dispersas). Detrás del cofre.
6. **Tick de ruleta suave**: `UI_BUTTON_CLICK` (tosco/ruidoso) → `UI_STONECUTTER_SELECT_RECIPE`
   a volumen bajo (0.28 pantalla / 0.22 in-world), pitch gentil. Pantalla e in-world.
7. **Marco de ruleta con efecto de aparición** (`renderRoulettePanel`): resplandor que revienta
   detrás del marco + barrido de luz que cruza al abrirse + bordes que laten a blanco + destello
   en la aguja central.

### AJUSTES 2.9.39 — destello starburst, spectral épico, centrado de ESCENA, GUI partículas
1. **Más destello al abrir (fondo)**: en `renderSceneBackground` (DIVINE LIGHT) se añadió un
   **STARBURST** de rayos radiales brillantes + fogonazo central que revienta en el estallido
   (divFlash), todo DETRÁS del cofre. Mejora los flashes existentes sin tocar la textura.
2. **Gemidos espectrales más épicos**: en `CrateSfx.openAccent`/`win` (y espejado en
   `CrateBlockEntity`) se añadió una BASE espectral común a todas las rarezas: drone GRAVE de
   almas (`AMBIENT_SOUL_SAND_VALLEY_LOOP` a pitch ~0.6) + wail ETÉREO alto. El wither sigue
   como rumor sutil.
3. **CENTRADO EN LA ESCENA (el fix real)**: la petición previa se había resuelto solo en el
   render in-world (`CrateRenderer`), pero la CINEMÁTICA usa `CrateCinematicScreen.renderCrate`,
   que también asumía centro (0.5,0.5). Ahora `ensureGeom` calcula `cCenterX/cCenterZ` con
   `modelXZCenter` (bbox del footprint del base) y `renderCrate` pivota sobre ese centro real
   → todas las crates quedan centradas EN LA ESCENA aunque el modelo esté autorado descentrado.
4. **GUI de partículas** (`CrateEditorScreen` / `ParticleLayer` / `RegistryLists` /
   `CrateBlockEntity.resolve`):
   - `helpLine` del tab acortado para que NO se corte ("Capas ilimitadas. Anillo/Halo/Espiral/
     Vórtice se adaptan al cofre.").
   - `ParticleLayer.shortLabel()` ahora es COMPACTO (nombre + inicial de fase/forma) para que
     no se corte en la lista estrecha de capas.
   - **Más partículas**: `RegistryLists.particles()` ya incluía TODAS las simples (~50). Se
     añadieron las PARAMÉTRICAS que antes se excluían, con handling en `resolve()`:
     `dust_color_transition`, `block`, `block_marker`, `falling_dust`, `item` (nether star),
     `sculk_charge`, `shriek`. (Vanilla no tiene más tipos; `vibration` se omitió por complejo.)

### AJUSTES 2.9.38 — más almas + auto-centrado de crates + glow celestial movido al fondo
1. **Más sonidos espectrales** en `openAccent` (estallido) y `win` (premio), tanto en
   `CrateSfx` (pantalla) como en `CrateBlockEntity` (in-world): coro fantasmal en varios
   tonos (capas extra de `AMBIENT_SOUL_SAND_VALLEY_ADDITIONS/_MOOD` a distintos pitch) +
   más `SOUL_ESCAPE`. El wither sigue como rumor grave sutil (sin cambios).
2. **AUTO-CENTRADO de todas las crates en el bloque** (`CrateRenderer`): antes el render
   asumía que cada modelo estaba centrado en (0.5,0.5), y varios modelos venían descentrados.
   Ahora se calcula el centro real del footprint XZ del modelo base (bounding box de sus
   quads, cacheado por identidad en `CENTER_CACHE`, método `footprintCenter`) y se traslada
   por `-centro` en vez de `-0.5`. La tapa usa el mismo frame → sigue alineada. Resultado:
   todas las crates quedan centradas como la dorada.
3. **El "brillo celestial" de la boca (2.9.37) SE QUITÓ** de `renderMouthGlow` porque lavaba
   la textura del cofre (queja del usuario). Se **reinventó como DIVINE LIGHT en el FONDO**
   (`renderSceneBackground`, sección "3.8 DIVINE LIGHT"): un resplandor celestial (bloom
   radial nucleo blanco + halo de rareza + abanico de rayos anchos difusos) que crece al
   abrir la tapa (t=56), revienta en el estallido (76) y se mantiene en el reveal, dibujado
   DETRÁS del cofre → enmarca sin tapar la textura.

### AJUSTES 2.9.37 — wither reworkeado + brillo celestial de la boca (Zelda)
El usuario dijo que el wither de 2.9.36 quedó mal ("muy molesto y se roba protagonismo").
1. **Wither reducido a un rumor grave lejano**: en `openAccent`/`win` (CrateSfx) y
   `playOpenAccent`/`playWin` (CrateBlockEntity), `WITHER_AMBIENT` bajó a vol ~0.2-0.4 con
   pitch ~0.62-0.72 (rumor de fondo, ya no gruñido protagonista). Se **quitó `WITHER_DEATH`**
   del `win` (era el que más robaba foco; el helper `witherDeath()` queda sin usar, inofensivo).
   A cambio se metieron **más almas espectrales** (más `SOUL_ESCAPE` + capas extra de
   `AMBIENT_SOUL_SAND_VALLEY_ADDITIONS/_MOOD`) como protagonistas.
2. **BRILLO CELESTIAL tipo cofre de Zelda** al abrir la tapa (`CrateCinematicScreen.renderMouthGlow`,
   bloque "BRILLO CELESTIAL"): bloom radial de 3 capas (halo rareza → mezcla → núcleo casi
   blanco) que emana de la BOCA del cofre, crece con la apertura (t=56→82) y revienta de
   brillo en el estallido (t=76, `burstFlash`), + abanico de god-rays ANCHOS y DIFUSOS hacia
   arriba (blobs suaves, NO la "raya" recta que odiaba). Todo con `drawGlowTex`. Se dibuja a
   z=300 (delante del cofre) junto a las partículas de `renderMouthGlow`.

### AJUSTES 2.9.36 (el usuario APROBÓ 2.9.35: "quedaron perfectas") — 3 cambios pedidos
Partiendo de 2.9.35 (que le encantó), el usuario pidió exactamente:
1. **QUITAR** el `ENDER_CHEST_OPEN` al abrir la tapa → eliminado de `CrateCinematicScreen.playAtmosphere`
   caso t=56 (queda solo `WOOD_HIT`). Nada más se tocó de la paleta 2.9.12 base.
2. **MÁS ÉPICO/ATERRADOR** en el estallido de la tapa (`openAccent`, t=76) y el premio
   (`win`, t=294): se AÑADIERON capas de **gemido de wither** (`WITHER_AMBIENT`, pitch bajo =
   más siniestro; en LEGENDARY/MYTHIC del `win` se usa el gemido LARGO `WITHER_DEATH`) +
   **alma que escapa** (`SOUL_ESCAPE`, espectral) + **gemido de almas extra** (soul sand
   valley). Escala con la rareza (más rareza = gemido más grave/fuerte). Aplicado tanto en
   `CrateSfx` (pantalla) como en `CrateBlockEntity.playOpenAccent/playWin` (in-world, algo más bajo).
3. **FONDO DE GALAXIA más épico** (`CrateCinematicScreen.renderSceneBackground`): NÚCLEO
   GALÁCTICO brillante y pulsante (doble halo) detrás del cofre, brazos espirales 2→3 más
   largos/densos/brillantes, nebulosas con más opacidad/tamaño, y más estrellas (44→60+) más
   brillantes.
Todo lo demás de 2.9.35 se mantiene intacto.

### ⚠️ CAMBIO DE CRITERIO (2.9.35) — la LISTA NEGRA de sonidos quedó ANULADA
El usuario, de forma EXPLÍCITA y NO NEGOCIABLE, eligió como referencia la build
`FantasticCratesSONG.jar` (= FSCrates **2.9.12**, en la raíz del repo) y pidió sacar sus
sonidos TAL CUAL y adaptarlos a este contexto. Esa build usa MUCHOS sonidos que antes estaban
"prohibidos" (warden sonic boom/charge, beacon, conduit, respawn anchor, lightning thunder,
trident thunder, wither spawn, end portal, enchantment table, ender chest, generic big fall,
wood hit, UI button click). **Ya NO están prohibidos**: son la paleta oficial ahora. La
sección 7 de "rechazos" de abajo es HISTÓRICA; sólo aplicar si el usuario lo vuelve a pedir.

### Paleta ACTUAL (2.9.35) — REIMPLANTE EXACTO de 2.9.12 + espectrales
Se decompiló `FantasticCratesSONG.jar` (CFR) y se mapearon los SoundEvents SRG→oficial. Se
copiaron BYTE-A-BYTE los métodos de sonido de 2.9.12 (`CrateSfx` de la pantalla; `play*` del
`CrateBlockEntity`; `playAtmosphere`/bed/tick del `CrateCinematicScreen`) a este mod,
**manteniendo el timing actual** (LAND=24, LID_START=56, BURST=76, ROLL 88→288, REVEAL=294;
2.9.12 usaba LAND=30, burst=46, REVEAL=254 — NO se copió el timing, sólo los sonidos).
- **CrateSfx** (pantalla): unlock/spiralCharge/spiralRise/spiralPeak/openAccent/openSustain/
  win/winTail/close = valores exactos de 2.9.12.
- **CrateBlockEntity** (in-world, más bajo): mismos métodos, valores exactos de 2.9.12.
- **playAtmosphere** (pantalla): @2 beacon+respawn, @24(LAND) warden_sonic_boom+generic_big_fall,
  @56(LID) ender_chest_open+wood_hit, @68 conduit+beacon (mapeo de los @2/@30/@44/@64 de 2.9.12).
- **Bed de la ruleta**: openSustain en t=92/108/124 (secuela del estallido, como 2.9.12) +
  acentos espectrales ligeros en 156/196/236/272.
- **Tick de ruleta**: `UI_BUTTON_CLICK` (pantalla vol 0.5, in-world vol 0.4) como 2.9.12.
- **AÑADIDO** (pedido del usuario): capas ESPECTRALES `AMBIENT_SOUL_SAND_VALLEY_ADDITIONS/_MOOD`
  en unlock, spiralPeak(EPIC+), openAccent, win, winTail y en el bed. Son las ÚNICAS 2 líneas
  nuevas respecto a 2.9.12 (verificado: el jar tiene los 17 SoundEvents de 2.9.12 + esas 2).
- El fix de audio doblado del opener (sección 9) sigue vigente.
- Copia de trabajo del análisis: se decompiló con CFR (`cfr-0.152.jar`) y se mapeó con la tsrg
  `srg_to_official_1.20.1.tsrg` del cache de ForgeGradle. (dirs temporales ya borrados).

### Paleta v2 (2.9.33) — "BÓVEDA ANCESTRAL" — RECHAZADA ("lo más horrible que he escuchado")
Apilaba demasiados sonidos METÁLICOS/PERCUSIVOS a la vez (`IRON_GOLEM_ATTACK` = clang,
`IRON_TRAPDOOR` = creak metálico, `CHAIN_HIT`, `LODESTONE_HIT`). Cuatro golpes de metal
simultáneos = mezcla dura, sucia y chillona. **Lección: no apilar metal/percusión.**

### Paleta ACTUAL (2.9.34) — "ABISMO" — pendiente de feedback del usuario
Filosofía nueva: **suave y profundo, no metálico y saturado.** Una FAMILIA sonora coherente
(agua profunda + espectros) que se MEZCLA en vez de chocar. Máximo 2-3 capas por evento.

| Capa | SoundEvent | Uso |
|---|---|---|
| Gemidos espectrales | `AMBIENT_SOUL_SAND_VALLEY_ADDITIONS/_MOOD` | Protagonistas (lo que le gusta) |
| Drone espectral | `AMBIENT_SOUL_SAND_VALLEY_LOOP` | Fondo grave de ultratumba |
| Golpe profundo limpio | `AMBIENT_UNDERWATER_ENTER` | "Whoomph" grave/redondo → estallido y reveal (NO un clang) |
| Resaca / descenso | `AMBIENT_UNDERWATER_EXIT` | Aterrizaje de la caja + cierre |
| Lecho de tensión | `AMBIENT_UNDERWATER_LOOP` | Colchón grave sostenido en carga/ruleta |
| Brillo etéreo / magia | `AMBIENT_UNDERWATER_LOOP_ADDITIONS` / `_ULTRA_RARE` | Destellos mágicos suaves en picos |
| Tick de ruleta | `UI_STONECUTTER_SELECT_RECIPE` | "Tik" limpio y suave, vol bajo (no metálico) |

**Cero metal, cero percusión, cero bloques musicales, cero vocalizaciones de mob** (salvo
los gemidos de almas). Verificado contra mappings oficiales 1.20.1.

### (histórico) Paleta v2 (2.9.33) — "BÓVEDA ANCESTRAL"
Rework total, pedido explícitamente por el usuario: "sonido vanilla, creado por ti... haz un
completo rework, sincronizando tiempos, ruleta, apertura de tapa, flash, etc.". Se le avisó
que se aplicaría criterio propio de diseño de sonido; el usuario NO ha escuchado esta versión
todavía — **la siguiente sesión debe recoger su feedback y iterar**.

**Cero bloques musicales. Cero vocalizaciones de mob** (salvo los gemidos que ama). Todo
impacto/mecanismo/magia vanilla:

| Capa | SoundEvent | Uso |
|---|---|---|
| Drone sostenido | `AMBIENT_SOUL_SAND_VALLEY_LOOP` | Fondo ominoso (reemplaza el `DIDGERIDOO`) |
| Gemidos espectrales | `AMBIENT_SOUL_SAND_VALLEY_ADDITIONS` / `_MOOD` | Protagonistas, sin cambios |
| Pulso ritual grave | `LODESTONE_HIT` | Golpe metálico resonante tipo "gong de altar" (reemplaza el tambor) |
| Acento metálico agudo | `CHAIN_HIT` | Clank corto, puntuación de tensión |
| Impacto épico (boom) | `IRON_GOLEM_ATTACK` | El "punch" del estallido de tapa y del premio |
| Mecanismo de tapa | `IRON_TRAPDOOR_OPEN` / `IRON_TRAPDOOR_CLOSE` | Creak metálico pesado, vende la tapa físicamente |
| Cofre mágico | `SHULKER_OPEN` / `SHULKER_CLOSE` | Acento de contenedor abriendo/cerrando (rarezas altas) |
| Destello mágico | `SHULKER_TELEPORT` | Shimmer etéreo en windup y reveal |
| Regalo / flourish | `ALLAY_ITEM_GIVEN` | Chime cálido "recibes algo" en el reveal |
| Oleada de energía | `TRIDENT_RIPTIDE_2` / `_3` | Silbido mágico ascendente (NO es el trueno prohibido) |
| Impacto de aterrizaje | `DEEPSLATE_HIT` | Golpe seco al caer la caja (t=24) |
| Tick de ruleta | `COMPARATOR_CLICK` | Click mecánico corto, nada musical |

Todo verificado contra las mappings oficiales 1.20.1 (`javap` sobre
`forge-1.20.1-47.2.0_mapped_official_1.20.1.jar`) para confirmar existencia y tipo de campo
(`Holder<SoundEvent>` vs `SoundEvent` directo) antes de usarlos.

## 8. Estado de la petición "épico espectacular"

El usuario pidió explícitamente que la IA actuara como "ingeniero veterano de sonidos" y
diseñara su propia paleta épica 100% vanilla, sin más preguntas de referencia. Eso es lo que
se implementó en 2.9.33 (sección 7). **No está confirmado si le gusta** — la próxima sesión
debe preguntar/recoger feedback y, si hace falta, iterar sobre esta paleta o (si el usuario
lo pide) pasar a sonidos custom `.ogg` (requeriría `sounds.json` + registro de `SoundEvent`).

## 9. Fix aplicado: audio doblado/eco para el opener (RESUELTO en 2.9.33)

Detectado en review de la sesión anterior, ahora corregido:
- La pantalla fullscreen reproduce con `SimpleSoundInstance.forUI` (canal master, sin
  atenuación). La escena in-world (`CrateBlockEntity.sceneLidMode`) reproducía la MISMA
  secuencia con `level.playLocalSound` en la posición de la crate. El cliente del opener
  corría AMBAS → oía todo el ritual DOBLADO/con eco.
- **Fix:** se añadió el campo `CrateBlockEntity.muteAudio` y una sobrecarga
  `startSceneLid(rarityColor, winnerRarity, boolean muteAudio)`. `ClientPacketHandler` llama
  con `muteAudio=true` SOLO cuando quien dispara la escena es el propio opener (que ya tiene
  su pantalla fullscreen con el audio completo); los bystanders siguen recibiendo
  `muteAudio=false` y oyen el audio in-world normalmente. El método privado `play(SoundEvent,
  float, float)` chequea `!this.muteAudio` antes de reproducir. La tapa/partículas físicas
  NO se ven afectadas (siguen su curso igual), solo se silencia el audio duplicado.
- `startAnimation` (modo in-world completo, sin pantalla) resetea `muteAudio=false` de forma
  defensiva por si la instancia del block entity se reutiliza.

## 10. Estado de entrega

- Última versión compilada, verificada y pusheada: **2.9.35** (REIMPLANTE EXACTO de la build
  de referencia 2.9.12 + espectrales; ver arriba "CAMBIO DE CRITERIO"). Anula versiones previas.
- Compila limpio; refmap OK; sin sonidos prohibidos en código (cero `NOTE_BLOCK_*`, cero
  metal/percusión); sin pitch<0.5 (verificado por archivo respetando el orden de args).
- Historial de intentos: 2.9.32 "ritual oscuro" (bloques musicales) RECHAZADO → 2.9.33
  "bóveda ancestral" (metal apilado) RECHAZADO como "lo más horrible que he escuchado" →
  2.9.34 "abismo" (agua profunda + espectros, suave/cohesionado). El fix de audio doblado
  del opener (sección 9) sigue vigente desde 2.9.33.
- **Pendiente:** feedback del usuario sobre la paleta "Abismo". Aún no la ha escuchado en
  juego. NOTA para la próxima IA: se ha fallado 2 veces a ciegas; si "Abismo" tampoco
  convence, la recomendación honesta es pasar a sonidos custom `.ogg` (casi todo lo vanilla
  "épico" está en la lista negra), montando `sounds.json` + registro de `SoundEvent`.
