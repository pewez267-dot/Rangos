# FSCrates — Contexto / Handoff (estado actual)

> Documento de traspaso. Describe QUÉ es el mod, CÓMO está montado y EN QUÉ PUNTO está
> el trabajo. No propone soluciones: solo explica el estado y el historial para que otra
> IA (u otra persona) pueda continuar.

## 0. RUTAS EXACTAS (dónde está todo)

> La fuente que se COMPILA vive fuera de git, en `work6/mod`. En el repo hay una COPIA
> (`FSCrates-source/`). Si trabajas clonando el repo, edita la copia o replica esa carpeta
> a `work6/mod` para compilar. **Mantener ambas sincronizadas.**

### Fuente del mod (editar + compilar) — NO es git
```
/projects/sandbox/work6/mod/
```
Archivos clave:
```
/projects/sandbox/work6/mod/src/main/java/com/fscrates/client/screen/CrateCinematicScreen.java
/projects/sandbox/work6/mod/src/main/java/com/fscrates/util/CrateSfx.java
/projects/sandbox/work6/mod/src/main/java/com/fscrates/block/CrateBlockEntity.java
/projects/sandbox/work6/mod/src/main/java/com/fscrates/crate/CrateCinematicTiming.java
/projects/sandbox/work6/mod/src/main/java/com/fscrates/mixin/LevelRendererMixin.java
/projects/sandbox/work6/mod/build.gradle                              (version = '2.9.32', linea 20)
/projects/sandbox/work6/mod/src/main/resources/META-INF/mods.toml     (version="2.9.32")
```
Jar de salida del build:
```
/projects/sandbox/work6/mod/build/libs/fscrates-2.9.32.jar
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
- **Versión actual:** `2.9.32` (en `build.gradle` y `src/main/resources/META-INF/mods.toml`).
- **Feature en la que se ha estado trabajando:** la **cinemática de apertura de crates**
  (cofres). Todo lo demás del mod (bloques, config, comandos, red, etc.) no se ha tocado.

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
(Usar `compileJava` en vez de `build` para iterar rápido, ~8-12s.)

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
  convergen — añadido en 2.9.32 como "más animación antes de abrir la tapa").
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
  win, winTail, close`. Los `NOTE_BLOCK_*` y `AMBIENT_*` son `Holder<SoundEvent>` → usan `.value()`.
- `block/CrateBlockEntity.java` — escena in-world. **Espeja** las mismas paletas en
  `playUnlock/playSpiralCharge/playSpiralRise/playSpiralPeak/playOpenAccent/playWin/playWinTail/
  playClose`. Aquí `this.play` tiene overloads para `SoundEvent` Y `Holder<SoundEvent>` (por eso
  los holders se pasan SIN `.value()`). `advanceSceneSounds` dispara: openAccent@76, win@294,
  winTail@300, close@362.
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
custom**). Se han hecho MUCHAS iteraciones de la mezcla y el usuario ha ido **rechazando**
casi todo. Lista de lo RECHAZADO explícitamente (NO volver a usar):

- Warden: `WARDEN_SONIC_BOOM`, `WARDEN_ROAR`, `WARDEN_NEARBY_*` (quejidos), `WARDEN_EMERGE`,
  `WARDEN_HEARTBEAT`, `WARDEN_SONIC_CHARGE` (el usuario está cansado del warden en general).
- `GHAST`, `SCULK_*` (chillador/sensor), `WARDEN_TENDRIL_CLICKS`, `VEX_CHARGE`,
  `ELDER_GUARDIAN_CURSE`, `SOUL_ESCAPE`.
- `LIGHTNING_BOLT_IMPACT`/`_THUNDER` (los oye como **TNT**), `TRIDENT_THUNDER`, `RAID_HORN`
  (lo oye distorsionado/horrible).
- `ENDER_CHEST_OPEN`, `BEACON_*`, `CONDUIT_*`, `EVOKER_CAST_SPELL`, `RESPAWN_ANCHOR_*`,
  `ENCHANTMENT_TABLE_USE`, `GENERIC_EXPLODE`, `GENERIC_BIG_FALL`, `WOOD_HIT`, `AMBIENT_CAVE`,
  ambientes de bioma del **Nether** (basalt/crimson/warped/nether_wastes = "chillido agudo").
- Prohibidos de siempre: portal, ender dragon, wither, campana (BELL), amatista, xp/subir de
  nivel, totems, cohetes (firework), enderman, click de botón UI.
- **NUEVO rechazo (2.9.32):** **bloques musicales** (`NOTE_BLOCK_BASEDRUM/DIDGERIDOO/BASS/HARP`),
  totems, yunques, xp, subir de nivel.

**Lo ÚNICO que al usuario le gusta y quiere mantener:** los **gemidos espectrales / almas en
pena** = `AMBIENT_SOUL_SAND_VALLEY_ADDITIONS` y `AMBIENT_SOUL_SAND_VALLEY_MOOD`.

### Paleta ACTUAL en el código (2.9.32) — el usuario la RECHAZÓ
"Ritual oscuro": tambor `NOTE_BLOCK_BASEDRUM` + drone `NOTE_BLOCK_DIDGERIDOO` + bajo
`NOTE_BLOCK_BASS` + arpa `NOTE_BLOCK_HARP` + gemidos soul sand valley. El tick de la ruleta usa
`NOTE_BLOCK_HAT`. El usuario dijo textualmente que el sonido de bloque musical "es una mierda"
y que no lo quiere.

## 8. Problema abierto AHORA MISMO

- El usuario quiere un sonido **"épico espectacular"** para la apertura de la tapa y el
  recibimiento del premio. Ha rechazado prácticamente todas las combinaciones vanilla posibles
  (ver lista). La paleta vanilla útil para "épico" está esencialmente **agotada**.
- Última pregunta que se le hizo (SIN responder aún): elegir entre (1) añadir **sonidos custom**
  (.ogg propios) al mod — requiere montar `sounds.json` + registro de `SoundEvent` + reproducción,
  y que el usuario aporte los archivos; o (2) seguir 100% vanilla pero con una **referencia
  concreta** de un sonido que él considere épico. Está pendiente su decisión.

## 9. Otro punto no resuelto (detectado en review, fuera de los 3 archivos de sonido)

- **Posible audio doblado para el que abre:** la pantalla reproduce con `SimpleSoundInstance.forUI`
  (canal master, sin atenuación) y la escena in-world reproduce con `level.playLocalSound` en la
  posición de la crate. Si el cliente del que abre corre AMBAS (la pantalla y el tick de la
  block-entity), oiría el ritual DOS veces (posible eco/engrosado). El disparo de la escena
  (`startSceneLid`) está en el código de red/despacho, no en los 3 archivos de sonido. No
  verificado a fondo. Es comportamiento **pre-existente** (no introducido en el rework de sonido).

## 10. Estado de entrega

- Última versión compilada, verificada y pusheada: **2.9.32**, commit `9ee4a6c`.
- Compila limpio; refmap OK; sin sonidos prohibidos en código; sin pitch<0.5.
- El usuario **NO está conforme** con el sonido de 2.9.32 (rechazó los bloques musicales).
