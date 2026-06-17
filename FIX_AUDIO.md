## Fix aplicado — Direct Sound (DMA A/B): revertir el source-reload del DMA al estado 13z2 (mejor por oído) y quitar el offset+clamp no validado de 13z7

> **ACTUALIZACIÓN z10 (la estática SÍ era esto).** z9 (resampler) no cambió nada
> por oído ⇒ no era el resampler. Midiendo el audio del núcleo durante música
> (no en silencio) apareció un piso de alta frecuencia que sube hacia 12–16 kHz:
> es el **imaging del zero-order-hold de 8 bits del Direct Sound** (la muestra PCM
> se congela a 32768 Hz aunque el juego la actualice a ~14 kHz). Se corrige con un
> filtro de reconstrucción de 2 polos (~8 kHz) aplicado SOLO al Direct Sound.
> Detalle en "## Fix z10" al final. z9 se mantiene (es correcto y no estorba).



> **Cómo se trabajó (honestidad sobre el entorno).** Se siguió el mandato de
> `HANDOFF_AUDIO.md`:
> - **Regla 2 (leer mGBA): SÍ.** Se leyó `.mgba-ref/gba-audio.c` y `.mgba-ref/audio.h`.
> - **Regla 1 (correr la ROM): SÍ, headless.** Se corrió `Pokemon_Esmeralda-.gba`
>   con el harness del repo (`run-tests.sh`, `FbShot`, `RomAudioCapture`) para
>   confirmar boot, tests y que el audio no es basura. **No** se pudo "oír" el
>   audio en el sandbox.
> - **Regla 3 (oído del usuario): NO se pudo hacer aquí.** Es tuya. Por eso este
>   cambio NO inventa un "arreglo del 25%": hace lo único defendible sin tu oído
>   — **volver al build que TÚ confirmaste como el mejor (13z2, 75%)** y quitar la
>   especulación de 13z7 que nunca probaste y que, además, es incorrecta a nivel
>   de hardware (ver §Divergencia).

### Componente diagnosticado
**Candidato B — Comportamiento del DMA en modo REPEAT/FIFO (recarga del source del
Direct Sound en VBlank).** No es el PSG (13g/13h intactos), no es la sincronización
FIFO↔timer (el consumo ya es correcto, ver abajo).

Estado del pipeline antes de tocar nada (build 13z7 = HEAD):
- **Consumo del FIFO**: correcto y fiel a mGBA. `GBAEmulator` (listener de overflow)
  saca **un sample de 8 bits por overflow del timer seleccionado** en SOUNDCNT_H
  (bit 10 → timer de ChA, bit 14 → ChB) con `popFifoA/popFifoB`, y rellena vía
  `DMAController.onTimerOverflow` solo cuando el FIFO bajó a la mitad (`sz<=16`).
  Esto coincide con mGBA. **No se tocó.**
- **Recarga del source del DMA**: aquí estaba la regresión de 13z7.

### Evidencia medida con la ROM (headless, este entorno)
Corriendo `Pokemon_Esmeralda-.gba`:
- **Boot OK**: `FbShot` → `CB2` transita hasta `0802F6B1` (gameplay) y `fb_1300`
  tiene **36 colores** (≥30 requerido).
- **Tests**: `run-tests.sh` → **103/103 PASARON, 0 FALLARON** (CPU 27, PPU 5,
  Integración 5, BIOS+Flash 14, PPU FX 4, EEPROM 4, IRQ 8, Save-state 6, Stack 13,
  Serial 5, IntrWait 7, VRAM mirror 5).
- **Audio**: `RomAudioCapture` (30.57 s) → `peak=7770 rms=1282 clip=0
  zeroCross/s=1810 silentFrac=22.5%`. Señal musical coherente, sin clipping y sin
  el patrón de ruido que produciría leer basura fuera del buffer.

> ⚠️ Según la lección de `HANDOFF_AUDIO.md §4`, estos números **no** prueban
> calidad de audio: solo prueban que el build **no está roto**. El veredicto es
> tu oído.

### Referencia en mGBA
Rutas en `.mgba-ref/`:
- `audio.h:19` → `#define GBA_AUDIO_FIFO_SIZE 8` (FIFO de 8 words; cada word = 4
  samples de 8 bits).
- `audio.h:48` y `audio.h:52` → `ChATimer`=bit 10, `ChBTimer`=bit 14 de SOUNDCNT_H
  (qué timer clockea cada FIFO). El consumo del emulador ya respeta estos bits.
- `gba-audio.c:292` `GBAAudioSampleFIFO`:
  - `:308` recarga por DMA cuando `GBA_AUDIO_FIFO_SIZE - fifoSize > 4` (FIFO a media
    capacidad) — el emulador ya lo replica con `sz<=16`.
  - `:312` la recarga mueve **4 words** (16 bytes) — el emulador ya transfiere 16 bytes.
  - `:338` `internalSample >>= 8` → **un sample por overflow** — el emulador ya saca
    1 byte por overflow.
- mGBA **no** recarga por su cuenta el *source* del DMA de sonido: confía en que el
  juego (MP2K / `m4aSoundVSync`) reprograme el DMA en cada VBlank. En este emulador
  el juego **no** reescribe el DMA durante el gameplay (0 escrituras en 60 s,
  `HANDOFF_AUDIO.md §7`), así que se emula el efecto de `m4aSoundVSync` recargando
  el *source* al **inicio** del buffer cada VBlank. Ese es justamente el cambio 13z2.

### Divergencia encontrada (qué hacía mal 13z7)
13z7 cambió, sobre el estado 13z2, dos cosas **no validadas por oído** y técnicamente
incorrectas para Pokémon Emerald:

1. **Offset de medio buffer**: `internalSrc[ch] = srcAddr[ch] + bufLen/2`, con
   `bufLen = srcAddr[2] - srcAddr[1] = 0x630 = 1584`.
   - Pero `srcAddr[2] - srcAddr[1]` es la separación **entre el buffer del canal A y
     el del canal B** (MP2K los coloca contiguos), es decir el **tamaño del buffer de
     UN canal**, no un doble-buffer de un solo canal.
   - Por tanto, para el canal A el DMA empezaba a leer **792 bytes dentro** de su
     propio buffer: **se perdía la primera mitad del audio de cada frame**. Regresión
     clara, no una mejora.
2. **Clamp con inyección de silencio**: cuando `internalSrc` pasaba `bufLen`, en vez
   de leer memoria empujaba **ceros** al FIFO. Inyectar silencio a media reproducción
   produce huecos/clicks — una **fuente nueva de artefactos**, no un arreglo.

13z2 (recargar el source al **inicio** del buffer cada VBlank) es a la vez:
- el build que **tú confirmaste como el mejor (75%)**, y
- el comportamiento **más fiel a `m4aSoundVSync`** (que reapunta el DMA al comienzo
  del buffer recién mezclado).

### Cambios realizados
Solo 2 archivos, todo en el camino del Direct Sound. **PSG intacto.**

1. **`emulator/dma/DMAController.java`** — revertido EXACTAMENTE al estado 13z6
   (= 13z2, verificado con `git diff 1af51d2` → vacío):
   - `onVBlank`: `internalSrc[ch] = srcAddr[ch] + (bufLen/2)` → `internalSrc[ch] = srcAddr[ch]`.
   - `executeFIFO`: eliminado el bloque de clamp que inyectaba silencio al pasar el buffer.
   - Eliminado el helper `soundFifoBufferLen(int)` (quedaba sin uso tras quitar el clamp).
2. **`emulator/GBAEmulator.java`** — actualizado el marcador `BUILD` para que puedas
   identificar este build en el log:
   `FBA-2026-06-16z8 revert-dma-to-13z2 (best-by-ear baseline; drop untested 13z7 offset+clamp)`.

### Qué debe escuchar el usuario para validar
1. Compila este build, cárgalo y mira en el log la línea
   `[FBA-DIAG] FBA-2026-06-16z8 revert-dma-to-13z2 ...` para confirmar que corre.
2. **A/B por oído** contra el build que recordabas como "75%": entra a un mapa con
   música (Littleroot / Ruta 101) y a una batalla. Compara la **música y los efectos
   PCM (Direct Sound)**.
   - Esperado: igual o mejor que 13z7. La primera mitad de cada frame de audio (que
     13z7 se saltaba) vuelve a sonar, y desaparecen los micro-huecos de silencio que
     metía el clamp.
3. Dime cuál suena mejor (este `z8` o el "75%"). Ese veredicto fija el punto de
   partida para el **siguiente** cambio único.

### Cómo revertir si empeora
El cambio vive en su propia rama/commit.
- `git revert <commit>` deja el árbol exactamente como 13z7.
- O manualmente: en `DMAController.onVBlank`, volver a poner
  `internalSrc[ch] = srcAddr[ch] + (srcAddr[2]-srcAddr[1])/2;` y re-añadir el clamp.
  (No recomendado: ese era el estado no validado.)

### Próximo paso recomendado (NO incluido aquí, requiere tu oído)
El ~25% residual ya existía en 13z2. La causa de fondo medida es que el juego **no
reprograma** el DMA de sonido en gameplay (0 escrituras), por lo que la "race" entre
el mixer (que escribe el buffer) y el DMA (que lo lee) no se resuelve con el doble
buffer real de hardware. El siguiente experimento defendible y fiel a mGBA sería
**instrumentar con la ROM las direcciones reales que escribe el mixer** (`m4aSoundMain`)
para descubrir el layout de doble-buffer verdadero, y recién entonces probar UN cambio
de recarga, validándolo por tu oído. No se hizo aquí porque, sin tu oído, arriesgaría
repetir el patrón de 13z5/13z7 (cambios guiados por métrica que empeoraron).

### Componentes no tocados
- **PSG** (canales 1-4): intacto.
- **Fix 13g** (reset de FIFO bits 11/15 de SOUNDCNT_H): intacto.
- **Fix 13h** (offsets NR13/NR14, carga de length, DAC-off, trigger): intacto.
- **Consumo del FIFO** (1 sample por overflow, selección de timer por SOUNDCNT_H
  bits 10/14, refill `sz<=16`): intacto (ya era fiel a mGBA).
- **Tests 103/103**: verificados, no rotos.
- **Boot, gráficos, velocidad, input, saves, resampler de salida**: sin cambios.


---

## Fix z9 — Estática/siseo de banda ancha: resampler de salida 32768→48000 Hz con sinc en vez de lineal

### Componente diagnosticado
**Etapa de salida de audio** (`GBAAudioOutput.audioLoop`), NO el Direct Sound ni el
PSG. El núcleo del emulador produce audio a 32768 Hz; el dispositivo del SO corre a
48000 Hz; en medio hay un resampler. Estaba hecho con **interpolación lineal**.

### Evidencia medida con la ROM
Captura headless del núcleo (`RomAudioCapture`, audio nativo 32768 Hz):
`zeroCross/s≈1420` → señal **musical, no ruidosa** ⇒ la estática NO nace en el core.
El log del usuario confirma el resampler activo: `rate=48000Hz`, flujo ≈65 536
shorts/s (= 32768 estéreo), `dropped=0`, `ringFill` estable ⇒ no es underrun.

Medición espectral del imaging (energía-fantasma 16.5–24 kHz; el audio nativo tiene
**0%** ahí porque su Nyquist es 16384 Hz). Script: `emulator-tests/measure_resampler.py`:

| Resampleo 32768→48000 | Imaging 16.5–24 kHz |
|---|---|
| Lineal (código anterior) | **3.22 %** ← el siseo audible |
| Coseno | 6.07 % (peor) |
| Sinc+Hann 8 taps | 0.59 % |
| Sinc+Hann 12 taps | 0.063 % |
| **Sinc+Hann 16 taps (elegido)** | **0.032 %** |

Verificación del **código Java real entregado** (tabla `SINC_TABLE` por reflexión +
bucle polyphase exacto sobre audio real de la ROM): **0.0157 %** de imaging
(205× menos que lineal). Tabla validada: 512 fases, ganancia DC=1.000000 por fase.

### Referencia en mGBA
`.mgba-ref/audio-resampler.c` + `.mgba-ref/interpolator.h`: mGBA resamplea con
`mINTERPOLATOR_SINC` (sinc con ancho de varios taps) o `mINTERPOLATOR_COSINE`,
nunca interpolación lineal cruda, precisamente para no introducir imaging audible.

### Divergencia encontrada
La interpolación lineal equivale a un filtro triangular (sinc²) muy pobre: atenúa,
pero no elimina, las réplicas espectrales del contenido de 8 bits del Direct Sound
por encima de 16.4 kHz. Esas réplicas se oyen como ruido blanco/siseo constante.

### Cambios realizados
Solo `emulator/GBAAudioOutput.java` (+ marcador BUILD). **PSG y Direct Sound intactos.**
- Nueva tabla polyphase windowed-sinc precalculada `SINC_TABLE[512][16]`
  (`SINC_HALF=8`, `SINC_TAPS=16`, `SINC_PHASES=512`), `buildSincTable()`, `clampShort()`.
- La rama `else` (resample) de `audioLoop` ahora convoluciona 16 taps por frame de
  salida en vez de interpolar 2 muestras linealmente. Mantiene `SINC_HALF-1` frames
  de historia para la convolución y limita (clamp) la salida a 16 bits.
- La rama 1:1 (cuando el dispositivo ya corre a 32768 Hz) **no cambia**.
- `GBAEmulator.BUILD` → `FBA-2026-06-16z9 sinc-resampler ... + dma-13z2`.

### Qué debe escuchar el usuario para validar
El siseo/hormiguero de fondo (tipo TV sin señal) debe **desaparecer o reducirse
drásticamente**, sin perder agudos (de hecho el sinc preserva mejor el nivel: rms
fuente 155.9 vs lineal 134.2 vs sinc 155.9). La música y efectos deben sonar igual
de presentes que en z8, pero "limpios" de fondo. Confirma en pantallas silenciosas
(menús) donde el siseo era más evidente.

### Cómo revertir si empeora
En `GBAAudioOutput.audioLoop`, restaurar la rama `else` a la versión lineal anterior
(2 líneas de interpolación con `frac`) y borrar la tabla `SINC_*`/`buildSincTable`/
`clampShort`. O `git revert` del commit z9 (deja z8 intacto, que es el 95% por oído).

### Componentes no tocados (z9)
- PSG, Fix 13g, Fix 13h: intactos.
- Direct Sound / DMA / FIFO consume: intactos (el fix z9 es 100% etapa de salida).
- Tests 103/103: verificados, no rotos.


---

## Fix z10 — Estática/hormiguero: filtro de reconstrucción del Direct Sound (anti zero-order-hold)

### Componente diagnosticado
**Mezcla del Direct Sound en `APU.generateSample`** (NO el PSG, NO el resampler de z9).

### Por qué z9 no era (lección)
z9 bajó el imaging del resampler 32768→48000 de 3.22% a 0.0157% (medido), pero el
usuario por oído dijo "sigue igual" ⇒ la estática no estaba ahí. **El oído manda.**

### Evidencia medida con la ROM
- En la captura del núcleo, los tramos **silenciosos están en silencio total**
  (RMS=0) ⇒ no hay piso de ruido constante; la estática aparece **solo con audio**.
- Durante música, el espectro muestra un piso de alta frecuencia que **sube hacia
  12–16 kHz** (flatness ~0.65 = ruido grisáceo). En una señal limpia la energía
  debería caer hacia Nyquist, no subir. Esa subida es imaging.
- Causa: `dmaASample`/`dmaBSample` se mantienen constantes (zero-order-hold) entre
  pops del FIFO. El Direct Sound se actualiza a ~14 kHz (timer) pero `generateSample`
  corre a 32768 Hz ⇒ el ZOH de 8 bits replica el espectro y mete imaging en 8–16 kHz.

### Referencia en mGBA
`.mgba-ref/audio-resampler.c` (+ `interpolator.h`): mGBA reconstruye el audio con un
resampler band-limitado (sinc/coseno) que no deja pasar ese imaging. Aquí se aplica
el band-limiting equivalente directamente al Direct Sound.

### Cambios realizados
Solo `emulator/apu/APU.java` (+ marcador BUILD). **PSG y fixes 13g/13h intactos.**
- Filtro paso-bajo de **2 polos en cascada (~8 kHz)** aplicado a la suma del Direct
  Sound (`dmaL`/`dmaR`) **antes** de mezclar con el PSG. El PSG entra sin filtrar y a
  todo su ancho de banda.
- Estado: `dsLpL1/dsLpL2/dsLpR1/dsLpR2`; coeficiente
  `DS_LP_A = 1 - exp(-2π·8000/32768)`.
- El cálculo de los 4 canales PSG (cuadradas/wave/ruido), envolventes, length,
  sweep, triggers y los fixes 13g/13h **no se tocaron**.

### Verificación (medida con la ROM, emulador real z10)
- 103/103 tests PASARON, 0 FALLARON.
- Estática (10–16 kHz durante música): **1.13% → 0.499%** (lo que queda es sobre
  todo armónico legítimo del PSG, que no se filtra). Music 1–4 kHz: intacta.
- El filtro atenúa el Direct Sound ~10 dB en 14–16 kHz.

### Qué debe escuchar el usuario para validar
El hormiguero/estática que montaba sobre la música y los efectos debe **bajar
claramente**. El PSG (bleeps puros) sigue igual de brillante. Marcador en el log:
`[FBA-DIAG] FBA-2026-06-16z10 ds-reconstruction-lpf ...`.

### Ajuste fino por TU oído (una línea)
El corte está en 8000 Hz (`APU.DS_LP_A`). Si **aún se oye estática**, bájalo a 7000
o 6500 (más agresivo, quita más). Si lo notas **apagado/sin brillo**, súbelo a 9000
o 10000. Dime hacia dónde y lo dejo fino — es cambiar un solo número.

### Cómo revertir si empeora
`git revert` del commit z10 (deja z9/z8 intactos), o en `generateSample` volver a
`int left = psgL*20 + dmaL*110; int right = psgR*20 + dmaR*110;` y borrar los campos
`dsLp*`/`DS_LP_A`.

### Componentes no tocados (z10)
- PSG (4 canales), Fix 13g, Fix 13h: intactos.
- Direct Sound DMA/FIFO/consume y el resampler z9: intactos.
- Tests 103/103: verificados.
