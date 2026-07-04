# FantasticCrates (FSCrates) — Contexto del proyecto para IA

> Este documento resume el estado exacto del proyecto para que otra IA (o tú mismo en
> otra sesión) pueda continuar sin perder contexto. Todo lo aquí descrito ya está
> pusheado a GitHub (branch `main` y branch `fuente-y-ci-build`).

## Identidad del proyecto

- **Repo**: `pewez267-dot/Rangos` — branch principal `main`.
- **Mod**: FantasticCrates (id interno `fscrates`), mod de **Minecraft Forge 1.20.1**,
  **Java 17**. Sistema de crates/cofres con rarezas, GUI editable en juego, recompensas
  con NBT, cooldown por jugador y un motor de animaciones modular.
- **Versión actual**: **2.9.6** (ver `build.gradle` línea `version = '2.9.6'` y
  `META-INF/mods.toml` línea `version="2.9.6"`).
- **Jar entregado**: `/FantasticCratesActualizar.jar` en la raíz del repo (branch `main`).
- **Descarga directa**: https://github.com/pewez267-dot/Rangos/raw/main/FantasticCratesActualizar.jar
- **Checksum**: siempre se verifica que `md5sum` del jar compilado == el jar commiteado
  antes de dar la entrega por buena.

## ⚠️ Dato crítico: dónde vive la fuente ahora

- La fuente COMPILABLE completa del mod vive en:
  **`/projects/sandbox/work6/mod/`** (este es el path de trabajo activo, fuera del repo git).
- Una COPIA idéntica de esa fuente está commiteada en el repo, en la branch
  **`fuente-y-ci-build`**, carpeta **`fscrates-src/`**, a través del
  **Pull Request #76** (abierto, sin mergear — `pewez267-dot/Rangos#76`,
  `fuente-y-ci-build` → `main`).
- **Por qué existen dos copias**: GitHub bloquea pushear archivos de configuración de
  CI (`.github/workflows/*.yml`) directo a `main` sin PR (por seguridad, ya que un
  workflow corre con acceso a secrets). Como el PR que sube `fscrates-src/` también
  incluye un GitHub Action (`build.yml`), quedó pendiente de merge.
- **IMPORTANTE**: la fuente en `work6/mod/` y en `fscrates-src/` (rama `fuente-y-ci-build`)
  están sincronizadas byte a byte a la versión 2.9.5 (verificado con `filecmp` justo antes
  de escribir este documento). Cualquiera de las dos sirve como punto de partida.
- Anteriormente (antes de 2.7.2) el código fuente del mod se había PERDIDO en un reset
  del sandbox y tuvo que reconstruirse decompilando el jar con CFR + remapeo SRG→oficial
  usando las mappings de ForgeGradle. Ya no debería volver a pasar mientras la fuente
  siga viva en `work6/mod/` Y en la rama `fuente-y-ci-build`, pero si ambas se perdieran,
  ese es el método de recuperación (documentado en los commits de la v2.7.1/2.7.2).

## Cómo compilar (comando probado y funcional)

El wrapper `./gradlew` tiene un bug de quoting en este entorno bash — **no usarlo
directo**. Usar el wrapper jar invocado manualmente:

```bash
cd /projects/sandbox/work6/mod
export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2
export PATH="$JAVA_HOME/bin:$PATH"
java -Xmx3G -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain --no-daemon build
```

- Jar de salida: `build/libs/fscrates-<version>.jar`
- La primera build tarda ~1-2 min (usa cache de ForgeGradle ya existente en
  `~/.gradle/caches/forge_gradle/`); builds siguientes son más rápidas.
- Para solo compilar sin empaquetar: cambiar `build` por `compileJava` (más rápido para
  iterar mientras se depura un error).

## Verificación OBLIGATORIA antes de entregar un jar

Después de cada build, verificar SIEMPRE:

```bash
J=build/libs/fscrates-<version>.jar
unzip -p $J META-INF/mods.toml | grep "^version"          # version correcta
unzip -p $J fscrates.refmap.json | grep -o "m_109599_"     # refmap del Mixin OK
unzip -l $J | grep -E "CinematicDiag|LevelRendererMixin"   # clases del mixin presentes
```

El **Mixin `LevelRendererMixin`** corta el render del mundo 3D mientras la cinemática
está abierta (evita gastar GPU dibujando el mundo detrás de la pantalla opaca). El
refmap DEBE mapear `renderLevel` → `m_109599_` (nombre SRG de Forge 1.20.1). Si ese
mapeo no aparece, el Mixin no se aplicará en producción (falla en silencio, `required:
false` en `fscrates.mixins.json`).

## Convención de entrega (checklist paso a paso)

1. Compilar en `/projects/sandbox/work6/mod/` con el comando de arriba.
2. Verificar refmap + versión + checksum (ver arriba).
3. Copiar el jar: `cp build/libs/fscrates-X.Y.Z.jar /projects/sandbox/Rangos/FantasticCratesActualizar.jar`
4. `git checkout main` en `/projects/sandbox/Rangos` (SIEMPRE pararse en la rama antes
   de cualquier operación git — evita mezclar cambios de otra rama).
5. `git add FantasticCratesActualizar.jar && git commit -m "..."`
6. Push con el power **github** (`kiro_powers activate/use`, tool `push_to_remote`).
   **NUNCA usar `git push` directo por bash** (falla auth en este sandbox).
7. Sincronizar la fuente en la rama del PR:
   `git checkout fuente-y-ci-build` → copiar `build.gradle` + `src/` desde
   `work6/mod/` a `fscrates-src/` → commit → push (con el power github).
8. Volver a `git checkout main` al terminar.

⚠️ **Cuidado con el orden de operaciones git en paralelo**: si se ejecuta `git checkout`
seguido de `push_to_remote` en llamadas paralelas (mismo bloque de tool calls), hay una
condición de carrera y se puede pushear la rama equivocada al remoto equivocado. Hacerlo
**secuencial**: primero `checkout` + esperar confirmación, LUEGO `push_to_remote`. Si se
detecta que se pusheó mal, corregir con `push_to_remote` usando `force_with_lease: true`
desde la rama correcta (nunca a `main`, solo a la rama de feature).

## Repos oficiales GitHub — reglas del sandbox

- Usar el power `github` (`kiro_powers activate powerName="github"`) para
  push/pull/PR. Nunca `git push` por bash (sin auth).
- Push siempre a una rama nueva, nunca directo a `main`, EXCEPTO el jar binario
  `FantasticCratesActualizar.jar` que el usuario pidió explícitamente que se pushee
  directo a `main` (es la convención de entrega de este proyecto — la URL de descarga
  apunta a `main`).
- Archivos de config de CI (`.github/workflows/*.yml`) NO se pueden pushear directo a
  `main` (bloqueado por seguridad) — requieren PR. Por eso el PR #76 sigue abierto.

## Feature en desarrollo: Cinemática de apertura de crates

Archivo principal: `src/main/java/com/fscrates/client/screen/CrateCinematicScreen.java`

### Qué es
Pantalla fullscreen que secuestra la vista del jugador que abre el cofre (los demás
jugadores ven la animación normal in-world, sin cambios). Flujo: caja cae desde arriba
→ tiembla → tapa se abre un poco → ruleta de ítems aparece y gira → frena y aterriza en
el premio → CAJA DESAPARECE y el premio sale al frente → reveal con texto.

### Timing actual (constantes en el archivo, todas en ticks, 20 ticks = 1s)
```
TOTAL = 360        (18s total, incluye ~3s extra de reveal para leer el premio)
LAND = 30          (caida de la caja, ~1.5s, con peso)
LID_START = 46
LID_END = 78        (apertura de tapa mas lenta y con mas tension, 32 ticks)
ROLL_START = 80
ROLL_END = 248
REVEAL = 254         (compartido con servidor via CrateCinematicTiming.REVEAL_TICK —
                       CRÍTICO no desincronizar nunca entre cliente/servidor)
```

### Orientación 3D del cofre — HISTORIAL DE BUGS (leer antes de tocar `renderCrate`)

Esto costó **muchísimas iteraciones fallidas** por no poder verificar visualmente el
render. El estado ACTUAL (2.9.5) está verificado por geometría (coordenadas reales del
modelo + matrices), no por adivinar. Reglas aprendidas:

1. El render usa `pose.scale(px, -px, px)` (flip en Y para mapear el mundo Y-up al
   espacio GUI Y-down). **Este flip NO invierte el sentido de apertura/cierre de la
   tapa** — solo mapea "arriba del modelo" a "arriba en pantalla". Un error repetido fue
   pensar que había que negar la rotación de la tapa para "compensar" el flip; eso
   estaba mal y hacía que la tapa se azotara hacia adentro del cofre.
2. `crate.json` (el modelo clásico) declara `"north": "crate_front"` → la convención del
   proyecto es **frente = cara norte = -Z**.
3. Estado final verificado que SÍ funciona (2.9.5):
   - `float yaw = 180.0f;` `float pitch = 26.0f;` — muestra la cara frontal decorada.
   - La tapa usa la **bisagra natural** (`this.cHinge`, sin hacks de pivote reflejado) y
     `Axis.XP.rotationDegrees(lid)` (positivo) — exactamente como el render in-world
     (`CrateRenderer.java`, que siempre fue correcto y sirve de referencia). Con yaw=180
     esto abre la tapa HACIA el jugador (verificado numéricamente: el borde de la cara
     decorada sube en pantalla al abrir).
4. **Si en el futuro se vuelve a reportar mala orientación**: NO adivinar con
   prueba-y-error. Usar un sub-agente (`invoke_sub_agent`, agente
   `general-task-execution`) con un prompt que le pida analizar las coordenadas reales
   de los JSON de modelo (`assets/fscrates/models/block/cine_*.json` y `*_lid.json`),
   comparar contra `CrateRenderer.java` (referencia correcta conocida), y hacer el
   álgebra de la matriz de transformación paso a paso ANTES de tocar código. Ese método
   sí funcionó (fix de 2.9.3/2.9.4/2.9.5); adivinar signos no funcionó en 3 intentos
   previos.
5. **(2.9.6) Excepción confirmada por el usuario**: las cajas del pack "W6 - Cinematic
   Crates" (`https://github.com/pewez267-dot/Rangos/blob/main/W6%20-%20Cinematic%20Crates.zip`,
   estilos registrados via `CrateStyles.regCine`: `cine_common`, `cine_rare`,
   `cine_epic`, `cine_legendary`, `cine_mythical`, `cine_ultimate`) tienen su UV de cara
   frontal rotada 180° respecto al resto de modelos del mod. Se detecta con
   `CrateStyles.Style.isCinematic()` (campo cacheado en `CrateCinematicScreen.cIsCineStyle`,
   calculado una vez en `ensureGeom()`) y se les suma **+180° extra** de yaw SOLO a
   ellas (`yaw = 180.0f + (cIsCineStyle ? 180.0f : 0.0f)` en `renderCrate`). El resto de
   estilos (clásicos, dedou, greek, toro, pirate, etc.) NO se tocan — su orientación con
   yaw=180 base sigue siendo la correcta verificada en 2.9.3-2.9.5.

### Bug crítico ya corregido: TODAS las crates deben usar la MISMA cinemática

En `src/main/java/com/fscrates/client/ClientPacketHandler.java`, método
`playAnimation`: antes, el jugador que abre solo veía la cinemática si
`animationId != "instant"`; si no, caía en la **animación in-world** (haz de luz +
ruleta 3D + hologramas en el mundo), que es pesada y visualmente distinta → causaba lag
horrible y "otra animación" en las crates clásicas (comunes, raras, etc.) vs las
"cinematic". **Ya corregido**: ahora el opener SIEMPRE ve la cinemática si hay premios,
sin excepción de rareza/estilo:
```java
boolean bl = cinematic = isOpener && cands != null && !cands.isEmpty();
```
Si se reporta lag o "animación distinta" otra vez, revisar primero este archivo.

### Sistema de sonido

`src/main/java/com/fscrates/util/CrateSfx.java` — sonido por rareza, reworkeado varias
veces según feedback:
- **NO usar `SoundEvents.GENERIC_EXPLODE`** (explosión tipo TNT) — el usuario lo pidió
  quitar explícitamente.
- **NO usar `SoundEvents.TOTEM_USE`** — el usuario también lo rechazó explícitamente
  ("no pongas sonidos de totems").
- Paleta actual: campanas (`BELL_BLOCK`), amatista (`AMETHYST_BLOCK_CHIME`), sonic boom
  del Warden, truenos, cuerno de invasión (`RAID_HORN`), conduit/beacon/encantamiento
  para sabor mágico. Ajustar volumen/pitch por rareza (ver el patrón `switch(r)` en cada
  método).
- El "tick" de sonido de la ruleta se dispara en `render()` (a la tasa de FPS real, no
  en `tick()` a 20Hz) para que el sonido cuadre exacto con el movimiento visual a
  cualquier framerate — ver método `updateReelClicks`.

### Rendimiento — lecciones aprendidas

- Los efectos de brillo/partículas se dibujaban originalmente con cientos de `g.fill()`
  por frame (scanlines para simular círculos) → esto causaba lag Y se veía con
  bandas/anillos feos. **Se reemplazó por una textura pre-generada**
  (`assets/fscrates/textures/gui/glow.png`, 128x128 RGBA, gradiente radial suave
  generado con un script Python que escribe el PNG directo con `zlib`, sin depender de
  Pillow) + un solo `GuiGraphics.blit()` por brillo/partícula (método `drawGlowTex` en
  `CrateCinematicScreen.java`). Esto resolvió lag Y estética a la vez.
- La animación se mueve con un **reloj de tiempo real puro** (`System.nanoTime()` desde
  que se abre la pantalla), NO atado a `this.ticks` del tick-loop del juego. Esto se
  hizo porque el tick-loop del cliente puede tartamudear en modpacks aunque los FPS
  estén altos, y la animación heredaba ese tartamudeo. Ver el inicio de `render()`.
- Hubo un overlay de diagnóstico en pantalla (FPS, ms/frame, desglose de costo por
  sección, estado del Mixin) usado temporalmente para depurar — **ya se removió por
  completo** (método `renderDiagnostics` eliminado, clase `CinematicDiag` sigue
  existiendo como puente de estado del Mixin pero sin overlay visual).

### Rework de sonidos/partículas/brillos (2.9.6)

Hecho vía sub-agente (`general-task-execution`) con instrucciones detalladas de diseño
+ restricciones duras, luego verificado por el orquestador (grep de sonidos prohibidos,
compilación, build completo, refmap) antes de entregar. Cambios:

- **Sonido** (`CrateSfx.java`): capas nuevas en `spiralPeak`/`openAccent`/`win`/
  `winTail` (bell resonate, amatista resonate, goat horn, fireworks escalando por
  rareza, experience orb, player levelup). El impacto de aterrizaje (`playAtmosphere`,
  tick 30) ahora es RAVAGER_STEP + GENERIC_BIG_FALL + ANVIL_LAND (más peso). El cue de
  mitad de apertura (tick 64) ya no usa CHICKEN_EGG (fuera de tema) — ahora usa
  AMETHYST_BLOCK_CHIME/RESONATE. También se encontró y corrigió un SEGUNDO lugar con
  `GENERIC_EXPLODE`: `CrateBlockEntity.playOpenAccent` (el sonido in-world que escuchan
  los DEMÁS jugadores mientras el opener ve la cinemática) — reescrito con la misma
  paleta sin explosión/totem.
- **Partículas/brillos** (`CrateCinematicScreen.java`): se agregó `rarityIntensity()`
  (0=COMMON..1=MYTHIC) para escalar intensidad sin tocar timing. Brasas de la boca en
  espiral suave (antes subían derecho), doble-pulso de luz, chispas ambientales con
  arco balístico + caída (antes rectas), shockwave/burst del reveal con ease-out, capa
  extra de "polvo de estrellas" en el reveal solo para EPIC+. Sigue usando
  EXCLUSIVAMENTE `drawGlowTex`/`drawSoftDot`/`drawRadialGlow` (textura, 1 blit por
  efecto) — cero `g.fill()` cuadrados nuevos para partículas/brillos. Conteo de
  partículas se mantuvo bajo (~14-20 máx por capa) para no reintroducir lag.
- **Timing NO tocado**: `REVEAL=254` sigue igual, sincronizado con
  `CrateCinematicTiming.REVEAL_TICK`.

### Reveal / textos (estado 2.9.5, sin cambios en 2.9.6)

- Ya NO dice "RECOMPENSA ENTREGADA". Ahora: `"Has recibido"` → nombre del ítem → solo
  el nombre de la rareza (ej. `"Mítico"`), sin la palabra "RAREZA" delante.
- El reveal dura más (`TOTAL=360`) para dar tiempo a leer el premio.

### Skip de la escena — OBLIGATORIA salvo para operadores

La cinemática NO se puede saltar por jugadores normales. Solo un operador (permiso
nivel 2, `player.hasPermissions(2)`) puede cerrarla con ESC/SPACE/ENTER — ver método
`canSkip()` y `keyPressed()`/`shouldCloseOnEsc()` en `CrateCinematicScreen.java`. El
texto "[ESC] saltar" solo se muestra si el jugador es operador.

### Editor de crates

`src/main/java/com/fscrates/client/screen/CrateEditorScreen.java` — la pestaña
**"Anim." (animaciones de la ruleta) fue ELIMINADA** del enum `Tab` por pedido del
usuario (no la usa). Si se vuelve a agregar algo similar, no reintroducir esa pestaña
sin confirmar primero.

## Sub-agentes: cuándo usarlos en este proyecto

El usuario pidió explícitamente usar sub-agentes (`invoke_sub_agent`,
`general-task-execution`) para tareas de **geometría 3D / orientación** que no se pueden
verificar visualmente desde este entorno (no hay forma de ver el render en vivo). Los
prompts que funcionaron bien:
- Dieron TODA la evidencia acumulada (coordenadas de los JSON de modelo, código de
  referencia que sí funciona, reportes empíricos del usuario con capturas).
- Pidieron explícitamente "no adivines, verifica con números" y mostrar el álgebra.
- Restringieron el alcance ("modifica SOLO este método, no toques timing/sonido/etc.").
- Exigieron compilar y reportar el resultado antes de considerarlo terminado.

Para cualquier tarea futura de orientación 3D, ángulos, o transformaciones de matrices
en este mod, preferir este patrón sobre iterar a ciegas con el usuario.

## Estilo del usuario (para cualquier IA que continúe)

- Habla español, informal, directo. Se frustra (con razón) si el trabajo es mediocre,
  si se recortan cosas sin pedirlo, o si se repiten errores ya reportados.
- Exige verificación real antes de dar algo por "hecho": comparar checksums, verificar
  refmap, compilar limpio — nunca asumir que "compiló" = "funciona".
- No inventar valores/sonidos/mecánicas sin revisar primero si ya existe un sistema real
  en el código para reusar (ej.: el sistema de sonido por rareza, los estilos de crate).
- Prefiere que se declare explícitamente cuando algo no se puede verificar (esta sesión
  no tiene forma de ejecutar Minecraft y ver el render), en vez de afirmar que algo
  "está arreglado" sin evidencia.

## Estado al momento de escribir este documento

- **main**: HEAD en `eb93210` — "FantasticCrates 2.9.6: rotacion +180 solo cajas cine +
  rework completo de sonidos/particulas/brillos". Jar entregado y verificado (refmap OK,
  checksum build==entregado).
- **fuente-y-ci-build** (PR #76 abierto, sin mergear): HEAD en `f6e2cae` — fuente
  sincronizada a 2.9.6, idéntica a `/projects/sandbox/work6/mod/`.
- Todo pusheado a GitHub, sin cambios locales pendientes en ninguna rama.
- Próximo paso sugerido: esperar feedback del usuario tras probar 2.9.6 en el juego
  (orientación correcta de las cajas "cine" específicamente, y la sensación
  nueva de sonidos/partículas/brillos en la apertura).
