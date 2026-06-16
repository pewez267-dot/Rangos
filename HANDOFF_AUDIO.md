# HANDOFF — Bug de AUDIO del mod GBA (FantasticBoy / gbaminecraft)

> Documento de traspaso para la próxima IA. Fecha: 2026-06-15.
> **LÉELO COMPLETO ANTES DE TOCAR NADA.**

---

## 0. MANDATO OBLIGATORIO (el usuario lo exige)

Para **cualquier** fix de audio o emulación DEBES:

1. **Emular con la ROM real del repo**: `Pokemon_Esmeralda-.gba` (raíz del repo).
   No inventes, no asumas: corre la ROM y mide.
2. **Leer el código fuente de mGBA** ("MGBAproyecto"), que está en el repo como
   `mgba-master.zip`. Ya extraje los archivos de audio relevantes en `.mgba-ref/`
   (ver sección 7). mGBA es la referencia de CÓMO debe comportarse cada registro.
   Si dudas de un comportamiento, búscalo en mGBA primero.
3. **NO confíes en métricas headless como prueba de calidad** (ver sección 4, es
   la lección más importante de esta sesión).

---

## 1. UBICACIÓN EXACTA DEL TRABAJO

| Cosa | Valor |
|---|---|
| Repo | `pewez267-dot/Rangos` (privado) |
| Rama | `fix/gba-audio-mgba-mix` |
| HEAD actual | `c40cbb6` (build 13z7) |
| Proyecto del mod | `gba-minecraft-mod/` (Forge 1.20.1) |
| Jar compilado | `gbaminecraft-1.5.8.jar` (raíz del repo) |
| ROM de pruebas | `Pokemon_Esmeralda-.gba` (raíz, NO commitear en otros sitios) |
| Fuente mGBA | `mgba-master.zip` (raíz) + `.mgba-ref/` (ya extraído) |
| Link raw del jar | `https://github.com/pewez267-dot/Rangos/raw/refs/heads/fix/gba-audio-mgba-mix/gbaminecraft-1.5.8.jar` |

Archivos del pipeline de audio:
```
gba-minecraft-mod/src/main/java/com/gbaminecraft/emulator/
  apu/APU.java                 <- genera samples 32768 Hz (PSG + Direct Sound mix)
  dma/DMAController.java        <- DMA FIFO del Direct Sound (AQUÍ está el foco actual)
  timer/TimerController.java    <- timers que disparan popFifo
  bios/HleBios.java             <- HLE BIOS (IntrWait/VBlankIntrWait) <- BUG PROBABLE
  GBAEmulator.java              <- loop principal, listener de timer overflow, BUILD marker
  GBAAudioOutput.java           <- salida real-time, ring buffer, resampler 32768->deviceRate
```

---

## 2. QUÉ ES EL MOD

Emulador de Game Boy Advance dentro de Minecraft Forge 1.20.1. Corre
Pokémon Esmeralda. **Todo funciona menos el audio**:
- Boot OK, gráficos OK, velocidad correcta (60 fps), input OK, saves OK.
- 103/103 tests headless pasan.
- **El audio tiene distorsión.** Ese es el único bug abierto.

---

## 3. EL ERROR ACTUAL (descripción exacta)

El usuario describe el audio como **"distorsión a veces"**. Estado por su oído:
fue de "horrible" (pitido/distorsión/borroso constante) a **~75% bueno**. Queda
un **~25% de distorsión intermitente** sin resolver.

Lo que está CONFIRMADO bueno (por el usuario, NO tocar):
- **Fixes de PSG de 13g + 13h** (ver sección 6). Arreglaron el "boop al avanzar
  diálogo" y el "pitido pegado ~10s". El PSG (canales de onda cuadrada/ruido)
  suena limpio. **Están intactos en `APU.java` y NO se deben revertir.**

Lo que sigue mal:
- El **Direct Sound** (canales DMA A/B = la música y voces PCM de Pokémon)
  tiene distorsión intermitente.

---

## 4. ⚠️ LECCIÓN CRÍTICA: LAS MÉTRICAS HEADLESS MIENTEN

En esta sesión perdí MUCHO tiempo guiándome por `spectral_analysis.py` (cuenta
"clicks" = saltos muestra-a-muestra, y reparto de energía por bandas). **ESA
MÉTRICA NO PREDICE LO QUE EL USUARIO OYE.**

Prueba: el build 13z5 daba **1195 clicks** (el "mejor" en papel, 4x mejor que la
referencia) y el usuario dijo que **ARRUINÓ el audio**. El build 13z2 daba 4752
clicks (peor en papel) y el usuario dijo **"va mejor, 75%"**.

→ **El único sensor fiable es el oído del usuario.** Haz UN cambio, pídele que lo
pruebe, y créele a él, no al número. No hagas barridos de parámetros optimizando
la métrica: te va a llevar a un óptimo falso.

---

## 5. ESTADO POR-OÍDO DE CADA BUILD (lo que dijo el usuario)

| Build | Cambio | Veredicto del usuario |
|---|---|---|
| 13g/13h | Fixes PSG (ver §6) | **Arreglaron su audio** (PSG limpio) |
| 13z2 (`77f8e01`) | DMA: reload de `internalSrc=srcAddr` cada VBlank | **"va mejor, 75%"** ← mejor confirmado |
| 13z3 (`7e88f14`) | Abrir DAC a 48 kHz nativo | "sigue igual" (neutro) |
| 13z4 (`2521867`) | DMA reload cada 6 frames | (sin feedback claro) |
| 13z5 (`08fc26f`) | DMA ring-buffer auto-ajustado | **"arruinaste el audio"** |
| 13z6 (`1af51d2`) | Revertir DMA exacto a 13z2 | (sin feedback aún) |
| 13z7 (`c40cbb6`) | DMA apunta a 2ª mitad del buffer (anti-race) | **SIN PROBAR por oído** |

**Punto de partida seguro:** si 13z7 suena mal, vuelve a **13z6/13z2** con
`git checkout 1af51d2 -- gba-minecraft-mod/src/main/java/com/gbaminecraft/emulator/dma/DMAController.java`
(es el "75%" confirmado). **Lo PRIMERO que debes hacer es pedirle al usuario que
compare 13z7 vs 13z6 y decirte cuál suena mejor.**

---

## 6. FIXES DE PSG (13g/13h) — CONFIRMADOS BUENOS, NO TOCAR

Replicados de mGBA, ya en `APU.java`:
- **13g**: reset de FIFO en bits 11/15 de SOUNDCNT_H (copia de `GBAAudioWriteSOUNDCNT_HI`).
- **13h** (4 bugs PSG):
  1. Offsets NR13/NR14 separados (trigger y length-enable se leían mal del byte
     de frecuencia; ahora `case 0x65/0x6D/0x75/0x7D` correctos).
  2. Carga de length: `64 - (NRx1&0x3F)`, wave `256 - NR31`.
  3. DAC-off: NRx2 con los 5 bits altos a 0 → canal off.
  4. El trigger ya NO corrompe la frecuencia leyéndola del registro de envelope.

---

## 7. ANÁLISIS DE CAUSA RAÍZ HECHO (datos medidos con la ROM)

Foco: **Direct Sound DMA (DMA1/DMA2 en modo FIFO)**. Medido instrumentando:

- El juego configura el DMA del sonido **UNA sola vez en boot** (32 escrituras a
  registros DMA en los primeros ~100 frames) y luego **0 escrituras** en 60s de
  gameplay. SAD ch1=`0x030066D0`, ch2=`0x03006D00`, `wordCount=0`, REPEAT, 32-bit.
- **Tamaño del buffer PCM = gap entre SAD ch1 y ch2 = `0x630` = 1584 bytes.**
- **Timer0 overflow = 21902 Hz** (reload `0xFD02`, prescaler 1). Pero el gate
  `if (sz<=16)` del FIFO limita el consumo real: **popFifo ≈ 14122/s**.
- El `internalSrc` del DMA avanza **~236 bytes/frame** (dentro del buffer; NO se
  desboca dentro de un frame).
- **El IRQ handler del juego SÍ se despacha (~111/s)** — el dispatch HLE funciona.
- Pero el juego **nunca reescribe el DMA del sonido** durante gameplay → en
  hardware real, `m4aSoundVSync` (rutina de MP2K llamada en VBlank) re-apunta el
  SAD del DMA cada frame. **Esa rutina NO está reanclando en nuestro emulador.**

### Hipótesis de la causa raíz del 25% restante (SIN CONFIRMAR por oído)
1. **`m4aSoundVSync` no corre / no reancla** (probable bug en `HleBios.java`
   IntrWait o en cómo el handler del juego interactúa con INTRCHECK/IF/IME).
   **ÉSTE es el fix de fondo correcto**: hacer que `m4aSoundVSync` corra de verdad
   y reancle el DMA como en hardware. Si se logra, se puede quitar TODO el
   workaround del reload-en-VBlank y emular el DMA exactamente como mGBA.
2. **Race condition** (lo que intenta 13z7): el mixer del juego escribe el buffer
   desde el inicio cada frame mientras el DMA lee desde el inicio → lee datos a
   medio escribir. 13z7 apunta el DMA a la 2ª mitad (doble-buffer manual). SIN
   validar por oído.

### Workaround actual (13z2..13z7, en `DMAController.onVBlank`)
Recargar `internalSrc[ch] = srcAddr[ch]` (o `+bufLen/2` en 13z7) cada VBlank para
los canales FIFO. Es un PARCHE. El fix de fondo es el punto 1.

---

## 8. CÓMO COMPILAR Y VALIDAR (sandbox Linux)

```bash
export JAVA_HOME=/opt/toolchains/.local/share/mise/installs/java/17.0.2
export PATH=$JAVA_HOME/bin:$PATH

# Compilar el mod (el gradle-wrapper del repo está roto, usar gradle del sistema)
cd gba-minecraft-mod
gradle build --no-daemon --console=plain -x test
cp build/libs/gbaminecraft-1.5.8.jar ../gbaminecraft-1.5.8.jar

# Tests headless del núcleo (deben dar 103/103)
cd emulator-tests
bash run-tests.sh

# Boot test con la ROM real (cb2 debe llegar a 0802F6B1, fb_1300 >=30 colores)
javac -cp .build/out -d .build/out FbShot.java
java -cp .build/out FbShot ../../Pokemon_Esmeralda-.gba 1500 .audio_test 1300

# Captura de audio del APU (genera WAV; OJO: escribe en emulator-tests/.audio/)
javac -cp .build/out -d .build/out RomAudioCapture.java H.java
java -cp .build/out RomAudioCapture ../../Pokemon_Esmeralda-.gba 4200 1800

# Análisis espectral (ÚSALO SOLO COMO PISTA, NO COMO PRUEBA DE CALIDAD)
pip install numpy
python3 spectral_analysis.py <wav> samples/gameplay_capture_60s.wav
```

WAVs de referencia "buenos" en `gba-minecraft-mod/emulator-tests/samples/`
(`baseline_capture.wav`, `gameplay_capture_60s.wav`). OJO: se capturaron con el
hack `*4` (cámara lenta) que enmascaraba el bug del Direct Sound, así que suenan
"suaves" en parte por estar starved, no por estar perfectos.

### Cómo leer la fuente de mGBA (OBLIGATORIO para fixes)
```bash
unzip -o -j mgba-master.zip "mgba-master/src/gba/audio.c" -d .mgba-ref   # Direct Sound GBA
unzip -o -j mgba-master.zip "mgba-master/src/gb/audio.c"  -d .mgba-ref   # PSG
# Ya extraídos en .mgba-ref/: gba-audio.c, gb-audio.c, audio.c, audio-resampler.c,
# interpolator.c, sdl-audio.c y sus .h
```
Funciones clave en `gba-audio.c`: `GBAAudioSample`, `GBAAudioSampleFIFO`,
`GBAAudioScheduleFifoDma`, `GBAAudioWriteSOUNDCNT_HI`, `_applyBias`.

---

## 9. ENTORNO DEL USUARIO

- Windows 11, Prism Launcher, Forge 1.20.1-47.4.10, JDK 17.0.15.
- Audio: salida a 48000 Hz (EarPods USB / Realtek). El log muestra
  `FBA: audio output started at 48000 Hz` desde 13z3.
- El usuario prueba en instancia TEST (pocos mods). Para ver qué build corre,
  busca en el log la línea `[FBA-DIAG] FBA-2026-06-14...`.

---

## 10. RECOMENDACIÓN DE PRIMER PASO PARA LA PRÓXIMA IA

1. Pídele al usuario que pruebe el jar actual (13z7) y compare con 13z6. Decide
   el punto de partida por SU oído.
2. Si quieres el fix de fondo de verdad: arregla `HleBios.java` para que el
   despacho de IRQ + IntrWait haga correr `m4aSoundVSync` del juego, que reancle
   el DMA del Direct Sound solo, y entonces ELIMINA el workaround de
   `DMAController.onVBlank`. Valida con la ROM y compara contra mGBA.
3. NO optimices `spectral_analysis.py`. Es solo pista. El oído del usuario manda.
```
```
