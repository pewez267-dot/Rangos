# FSCrates — Handoff para otra IA (actualización 2.9.9)

Documento de traspaso enfocado en la **última actualización** del mod. Para el historial
completo y las lecciones de la cinemática, ver también `CONTEXTO_PROYECTO.md` en este
mismo repo.

---

## 1. Direcciones exactas del proyecto

- **Repositorio GitHub**: `pewez267-dot/Rangos` — https://github.com/pewez267-dot/Rangos
  - Branch principal: **`main`** (aquí va el jar entregable).
  - Branch de fuente + CI: **`fuente-y-ci-build`** (aquí va el código fuente en
    `fscrates-src/`; hay un PR #76 abierto sin mergear).
- **Jar entregable** (lo que descarga el usuario):
  `https://github.com/pewez267-dot/Rangos/raw/main/FantasticCratesActualizar.jar`
- **Código fuente de trabajo (en el sandbox)**: `/projects/sandbox/work6/mod/`
  - Java: `/projects/sandbox/work6/mod/src/main/java/com/fscrates/`
  - Recursos: `/projects/sandbox/work6/mod/src/main/resources/assets/fscrates/`
  - Copia espejo del fuente en el repo: `/projects/sandbox/Rangos/fscrates-src/`
    (rama `fuente-y-ci-build`).
- **Mod**: FantasticCrates, id interno `fscrates`, **Minecraft Forge 1.20.1, Java 17**.
- **Versión actual**: **2.9.9** (`build.gradle` → `version = '2.9.9'`;
  `META-INF/mods.toml` → `version="2.9.9"`).

---

## 2. Cómo compilar, verificar y entregar

### Compilar (usar SIEMPRE el wrapper por classpath, NUNCA `./gradlew` directo)
```bash
cd /projects/sandbox/work6/mod && \
export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2 && \
export PATH="$JAVA_HOME/bin:$PATH" && \
java -Xmx3G -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain --no-daemon build
```
- Para iterar rápido: cambiar `build` por `compileJava`.
- Jar de salida: `build/libs/fscrates-<version>.jar`.

### Verificar (NO asumir que "compiló" = "funciona")
```bash
JAR=build/libs/fscrates-2.9.9.jar
unzip -p $JAR META-INF/mods.toml | grep "^version"          # versión correcta
unzip -p $JAR fscrates.refmap.json | grep -o "m_109599_"    # Mixin de FPS mapeado (SRG)
unzip -l $JAR | grep fantastic_key                          # item empaquetado
```
- **Mixin de FPS (crítico)**: `LevelRendererMixin` corta el render del mundo 3D mientras
  la cinemática está abierta. Requiere el plugin `org.spongepowered:mixin` en `build.gradle`
  + annotationProcessor explícito para que se genere el **refmap**. Verificar SIEMPRE que
  `fscrates.refmap.json` exista y mapee `renderLevel → m_109599_`.

### Convención de entrega
1. Copiar el jar compilado a `/projects/sandbox/Rangos/FantasticCratesActualizar.jar`.
2. Verificar checksum: `md5sum build/libs/fscrates-<v>.jar FantasticCratesActualizar.jar`
   (deben coincidir).
3. Commit en `main`, luego **`pull_repository`** (power `github`) para sincronizar, y
   **`push_to_remote`** a `main`. **NUNCA `git push` directo por bash** (falla auth) — usar
   el power `github` (`kiro_powers` → `github` → `push_to_remote`).
4. Sincronizar el fuente en la rama `fuente-y-ci-build`: `git checkout fuente-y-ci-build`,
   `rm -rf fscrates-src/src && cp -r work6/mod/src fscrates-src/src`,
   `cp work6/mod/build.gradle fscrates-src/build.gradle`, commit, y push SECUENCIAL
   (checkout → copiar → commit → push; no en paralelo, o se pushea la rama equivocada).
5. Actualizar `CONTEXTO_PROYECTO.md`.

---

## 3. QUÉ CAMBIÓ EN 2.9.9 (esta actualización) — el cambio grande

### Fantastic Key universal (reemplaza las 5 llaves por rareza)
- Antes: 5 items `key_common`…`key_mythic`, y cada crate abría solo con la llave de su tier.
  **Eso se eliminó por completo.**
- Ahora: **una sola llave universal "Fantastic Key"** abre CUALQUIER crate.
  - Item: `ModRegistry.FANTASTIC_KEY` (id `fscrates:fantastic_key`), clase `KeyItem` SIN
    campo `rarity`. Nombre "✦ Fantastic Key ✦" (LIGHT_PURPLE), foil.
  - **Textura/modelo = `ultimate_key` del pack "W6 - Cinematic Crates"** (oro + gema
    morada), copiados EXACTOS:
    - `assets/fscrates/textures/item/fantastic_key.png` (32x32, md5
      `33ff6e2b9cb68c717a81268421475d41`).
    - `assets/fscrates/models/item/fantastic_key.json` (modelo 3D con `elements`,
      `texture_size:[32,32]`, bloque `display`). Lo ÚNICO cambiado respecto al original es
      la ref de textura → `fscrates:item/fantastic_key`.
  - Se BORRARON los 5 `key_*.json` y `key_*.png`.

### Rarezas por crate + pools de recompensa (todo configurable en GUI)
- `CrateConfig` ahora tiene `LinkedHashMap<Rarity,Double> rarityChances` = **tabla de
  probabilidad por rareza** (pesos que se normalizan; default nuevo 60/25/10/4/1).
- **Flujo al abrir**: `CrateConfig.rollRarity(random)` tira una rareza según la tabla →
  `LootEngine.roll` arma el **pool de esa rareza** (recompensas cuya
  `effectiveRarity(crate.rarity)` == la rareza tirada) y elige una por peso
  (`RewardEntry.chance` = peso DENTRO de su pool). Si el pool está vacío, cae a cualquier
  recompensa. Las `guaranteed` siempre entran.
- La **cinemática muestra la rareza que salió** (color/sonido/partículas), porque
  `CrateOpeningService` usa `effectRarity = headline.effectiveRarity(crate.rarity)` y el
  headline salió del pool de la rareza tirada.
- `crate.rarity` sigue existiendo como **"rareza base"**: color/nombre del item de crate y
  pool por defecto de los items marcados "Auto" (rareza en blanco).

### GUI (CrateEditorScreen)
- La pestaña "Llave" ahora se llama **"Rarezas"** (`initKey()`): edita los % de cada rareza
  (5 campos), botones "Igualar rarezas" y "Preset 60/25/10/4/1", y el toggle de consumir
  llave.
- Los items se asignan a cada rareza en la pestaña **"Premios"** con el botón **Rareza** de
  cada item (Auto = usa la rareza base). La lista muestra `[Rareza]` y el % DENTRO de su
  pool (`normalizedPercentInPool`). La pestaña **"Prob."** ajusta el peso de cada item.
- INFO: el botón "Tier" pasó a llamarse **"Rareza base"**.

### Migración (importante para no romper mundos existentes)
- `CrateConfig.load`: si el NBT NO trae `rarityChances` (crate guardada antes de 2.9.9),
  se migra a `{rarity: 100}` (100% su rareza actual) → conserva EXACTAMENTE el
  comportamiento previo hasta que el admin reparta los % en la GUI. Las crates nuevas
  nacen con 60/25/10/4/1.

### Comando
- `/fscrate key give <jugador> [cantidad]` da la Fantastic Key (ya NO hay `<tier>`).

### Archivos tocados en 2.9.9
`KeyItem`, `ModRegistry` (FANTASTIC_KEY + `key()`), `CrateItems` (`isKey`/`buildKey()`
no-arg, lore), `CrateBlock.use` (acepta cualquier `isKey`), `RewardEntry.describe`,
`LootEngine.roll` + `deliver`, `CrateOpeningService.iconFor`, `CrateConfig` (rarityChances
+ helpers + save/load + migración), `FSCrateCommand` (comando key), `CrateEditorScreen`
(tab Rarezas + INFO + lista Premios). Recursos: `fantastic_key.json`/`.png` nuevos, 5
`key_*` borrados, `lang/en_us.json` + `lang/es_es.json`.

---

## 4. Contexto reciente de la cinemática (2.9.7 / 2.9.8) — resumen

- **2.9.7**: la tapa de las crates "cine" (estilos `cine_*`, pack W6) abre HACIA el jugador
  (pivote reflejado `1-hinge[2]` + `XP(-lid)`, SOLO cuando `cIsCineStyle`); fondo teñido
  con el color de rareza + resplandor ambiental visible; se quitó el haz vertical de la
  tapa (→ ondas de halo + abanico de rayos); partículas con envolvente `smoothstep`;
  reveal/shockwave pulidos; se quitó el sonido de yunque al aterrizar; apertura más épica.
- **2.9.8**: se quitó el **punto luminoso duro** de la tapa (ahora glow ancho/plano/suave,
  sin núcleo blanco que traspasaba la textura); se **quitó el texto "[ESC] saltar
  (operador)"** de la pantalla (el operador SIGUE pudiendo saltar, solo se ocultó el
  texto); **rework de sonidos**: se eliminaron TODOS los `FIREWORK_ROCKET_*` y los de
  experiencia (`EXPERIENCE_ORB_PICKUP`, `PLAYER_LEVELUP`) y se subieron volúmenes de la
  apertura.

### Reglas duras de la cinemática (NO romper)
- Timing compartido con el servidor: `REVEAL=254` (y `CrateCinematicTiming.REVEAL_TICK`)
  NUNCA se desincroniza.
- Efectos SOLO por blits de textura suave (`drawGlowTex`/`drawSoftDot`); **NUNCA** dibujar
  partículas/brillos con `g.fill()` (el usuario odia las "partículas cuadradas y sucias" y
  causa lag).
- Sonido: **NUNCA** `GENERIC_EXPLODE`, `TOTEM_USE`, `FIREWORK_ROCKET_*`,
  `EXPERIENCE_ORB_PICKUP`, `PLAYER_LEVELUP`. La paleta épica permitida está en
  `CrateSfx.java` (warden/thunder/wither/dragon/raid_horn/goat_horn/bells/amethyst/
  conduit/beacon/enchant). El sistema de sonido es por rareza (`switch(r)` por método).
- La animación usa un reloj de **tiempo real** (`System.nanoTime()`), no `this.ticks`, para
  no heredar el tartamudeo del tick-loop.
- La cinemática es OBLIGATORIA salvo para operadores (permiso nivel 2). Ver `canSkip()`.

---

## 5. Estilo del usuario y forma de trabajar

- Habla español, informal, directo. Se frustra si el trabajo es mediocre, si se recortan
  cosas sin pedirlo, o si se repiten errores ya reportados.
- Exige **verificación real** antes de dar algo por hecho: comparar checksums, verificar el
  refmap, compilar limpio, leer bytecode si hay dudas de firmas — nunca asumir.
- No inventar valores/sonidos/mecánicas sin revisar antes si ya existe un sistema real en
  el código para reusar.
- Pide usar **sub-agentes** para tareas subjetivas (visual/audio) o de geometría 3D que no
  se pueden verificar visualmente desde el sandbox. Darles TODA la evidencia y restringir
  el alcance ("modifica SOLO este método, no toques timing/REVEAL/etc.").
- El sandbox NO puede ejecutar Minecraft; declarar explícitamente lo que no se puede
  verificar en vez de afirmar que "está arreglado".

---

## 6. Estado actual y pendiente

- **main** HEAD `b4a6a56` — jar 2.9.9 entregado y verificado (md5
  `6e1b1dc4c5bce80fae8a3345e4b51c9b`, item `fantastic_key` registrado, modelo+textura
  empaquetados, 0 llaves viejas, refmap OK).
- **fuente-y-ci-build** HEAD `949bf75` — fuente 2.9.9 sincronizada + `CONTEXTO_PROYECTO.md`.
- **PENDIENTE**: el usuario aún NO ha probado 2.9.8/2.9.9 en el juego. A validar en 2.9.9:
  1. la Fantastic Key sale con el diseño correcto (oro + gema morada) y abre cualquier crate;
  2. la tabla de rarezas reparte bien los premios por rareza;
  3. las crates viejas ya colocadas siguen funcionando (migración a 100% su rareza).
- Recordatorio operativo: como se eliminaron las 5 llaves viejas, hay que repartir
  **Fantastic Keys** con `/fscrate key give`.
