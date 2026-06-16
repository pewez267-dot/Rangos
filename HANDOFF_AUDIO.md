# HANDOFF — Bug de AUDIO del mod GBA (FantasticBoy / gbaminecraft)

> Documento de traspaso para la próxima IA. Fecha: 2026-06-15.
> **LÉELO COMPLETO ANTES DE TOCAR NADA.**
>
> Este documento SOLO describe el PROBLEMA y los hechos medidos.
> NO trae la solución a propósito: tú debes diagnosticarla y resolverla,
> obligatoriamente con la ROM real y leyendo el código de mGBA (ver §0).

---

## 0. MANDATO OBLIGATORIO (el usuario lo exige)

Para **cualquier** fix de audio o emulación DEBES:

1. **Emular con la ROM real del repo**: `Pokemon_Esmeralda-.gba` (raíz del repo).
   No inventes, no asumas, no parchees a ciegas: corre la ROM y MIDE.
2. **Leer el código fuente de mGBA** ("MGBAproyecto"), que está en el repo como
   `mgba-master.zip`. Ya están extraídos los archivos de audio en `.mgba-ref/`
   (ver §7). mGBA es la referencia de CÓMO debe comportarse cada registro/parte.
   Si dudas de un comportamiento del hardware, búscalo en mGBA primero.
3. **NO confíes en métricas headless como prueba de calidad** (ver §4, es la
   lección más importante de esta sesión).

---

## 1. UBICACIÓN EXACTA DEL TRABAJO

| Cosa | Valor |
|---|---|
| Repo | `pewez267-dot/Rangos` (privado) |
| Rama | `fix/gba-audio-mgba-mix` |
| HEAD actual | build 13z7 |
| Proyecto del mod | `gba-minecraft-mod/` (Forge 1.20.1) |
| Jar compilado | `gbaminecraft-1.5.8.jar` (raíz del repo) |
| ROM de pruebas | `Pokemon_Esmeralda-.gba` (raíz) |
| Fuente mGBA | `mgba-master.zip` (raíz) + `.mgba-ref/` (ya extraído) |
| Link raw del jar | `https://github.com/pewez267-dot/Rangos/raw/refs/heads/fix/gba-audio-mgba-mix/gbaminecraft-1.5.8.jar` |

Archivos del pipeline de audio:
```
gba-minecraft-mod/src/main/java/com/gbaminecraft/emulator/
  apu/APU.java                 <- genera samples 32768 Hz (PSG + Direct Sound mix)
  dma/DMAController.java        <- DMA FIFO del Direct Sound
  timer/TimerController.java    <- timers que disparan popFifo
  bios/HleBios.java             <- HLE BIOS (IntrWait / VBlankIntrWait)
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

## 3. EL ERROR ACTUAL (descripción exacta del problema)

El usuario describe el audio como **"distorsión a veces"**. Por su oído, el
audio fue de "horrible" (pitido/distorsión/borroso constante) a **~75% bueno**.
Queda un **~25% de distorsión intermitente** sin resolver.

Qué parte suena mal:
- El **Direct Sound** (canales DMA A/B = la música y las voces/efectos PCM de
  Pokémon) tiene distorsión intermitente.

Qué parte suena bien (CONFIRMADO por el usuario, ver §6, **NO TOCAR**):
- El **PSG** (canales de onda cuadrada / wave / ruido) suena limpio.

---

## 4. ⚠️ LECCIÓN CRÍTICA: LAS MÉTRICAS HEADLESS MIENTEN

En esta sesión se perdió mucho tiempo guiándose por `spectral_analysis.py`
(cuenta "clicks" = saltos muestra-a-muestra, y reparto de energía por bandas).
**ESA MÉTRICA NO PREDICE LO QUE EL USUARIO OYE.**

Prueba real: un build daba **1195 clicks** (el "mejor" en papel, 4x mejor que la
referencia) y el usuario dijo que **ARRUINÓ el audio**. Otro build daba 4752
clicks (peor en papel) y el usuario dijo **"va mejor, 75%"**.

→ **El único sensor fiable es el oído del usuario.** Haz UN cambio, pídele que lo
pruebe, y créele a él, no al número. No hagas barridos de parámetros optimizando
la métrica: te llevan a un óptimo falso. La métrica sirve como PISTA, no prueba.

---

## 5. ESTADO POR-OÍDO DE CADA BUILD (lo que dijo el usuario)

Contexto del problema (no es la solución, es el historial de síntomas):

| Build | Veredicto del usuario |
|---|---|
| 13g/13h | Arreglaron el PSG (boop/pitido). PSG quedó limpio. |
| 13z2 | **"va mejor, 75%"** ← mejor confirmado por oído |
| 13z3 | "sigue igual" (neutro) |
| 13z5 | **"arruinaste el audio"** |
| 13z7 (HEAD) | **SIN PROBAR por oído** |

Lo primero que conviene hacer: pedir al usuario que compare el build actual con
el de "75%" y decir cuál suena mejor, para fijar el punto de partida por SU oído.

---

## 6. LO QUE YA ESTÁ BIEN — PSG (13g/13h) — NO TOCAR

El usuario confirmó que estos fixes (replicados de mGBA) arreglaron el PSG. Ya
están en `APU.java`. **NO revertir, NO modificar.** Es contexto de qué NO es el
problema:
- **13g**: reset de FIFO en bits 11/15 de SOUNDCNT_H (como `GBAAudioWriteSOUNDCNT_HI`
  de mGBA). Arregló el "boop al avanzar diálogo".
- **13h** (4 bugs de PSG que causaban el "pitido pegado ~10s"):
  1. Offsets NR13/NR14 mal mapeados: el trigger y el length-enable de CH1/CH2/CH3
     se leían del byte de frecuencia (NR13) en vez de NR14 → los canales no se
     cortaban → pitido pegado. Ahora NR13/NR14 separados como en hardware.
  2. La longitud nunca se cargaba del registro (ahora `64-(NRx1&0x3F)`, wave
     `256-NR31`, como mGBA).
  3. DAC-off no apagaba el canal (mGBA: NRx2 con los 5 bits altos a 0 → canal off).
  4. El trigger corrompía la frecuencia leyéndola del registro de envelope. Eliminado.

**El bug abierto es el Direct Sound (DMA), NO el PSG.**

---

## 7. HECHOS MEDIDOS SOBRE EL PROBLEMA (con la ROM real)

Estos son DATOS observados instrumentando el emulador con la ROM. Son el punto
de partida del diagnóstico. **NO son la solución** — sácala tú.

- El bug está en el **Direct Sound (DMA1/DMA2 en modo FIFO)**, no en el PSG.
- El juego configura el DMA del sonido **UNA sola vez en boot** (~32 escrituras a
  registros DMA en los primeros ~100 frames) y luego **0 escrituras** durante
  60s de gameplay.
  - SAD ch1 = `0x030066D0`, SAD ch2 = `0x03006D00`, `wordCount=0`, REPEAT, 32-bit.
  - Separación entre SAD ch1 y ch2 = `0x630` = **1584 bytes** (tamaño del buffer PCM).
- **Timer0 overflow ≈ 21902 Hz** (reload `0xFD02`, prescaler 1). El gate
  `if (sz<=16)` del FIFO limita el consumo real a **popFifo ≈ 14122/s**.
- El puntero de lectura del DMA (`internalSrc`) avanza **~236 bytes/frame**.
- El **IRQ handler del juego SÍ se despacha (~111/s)**: el dispatch HLE de IRQ
  funciona.
- Durante el gameplay el juego **NO reescribe** los registros del DMA del sonido.
  (En hardware real, MP2K tiene una rutina de sonido que se ejecuta en VBlank y
  gestiona ese DMA; compara con mGBA cómo debería comportarse.)

Cómo está "tapado" hoy: hay un workaround en `DMAController.onVBlank` que
manipula el puntero del DMA cada VBlank. Es un PARCHE que llega al ~75%; el
defecto de fondo sigue ahí. (Decide tú si lo reemplazas por la emulación correcta
o por otra cosa — pero hazlo con datos de la ROM y referencia mGBA, no a ciegas.)

> Nota: los WAV de referencia en `emulator-tests/samples/` se capturaron con un
> hack viejo de timing (`*4`, cámara lenta) que enmascaraba el bug del Direct
> Sound, así que suenan "suaves" en parte por estar starved, no por ser perfectos.
> Úsalos con cuidado.

---

## 8. CÓMO COMPILAR Y VALIDAR (sandbox Linux)

```bash
export JAVA_HOME=/opt/toolchains/.local/share/mise/installs/java/17.0.2
export PATH=$JAVA_HOME/bin:$PATH

# Compilar (el gradle-wrapper del repo está roto, usar gradle del sistema)
cd gba-minecraft-mod
gradle build --no-daemon --console=plain -x test
cp build/libs/gbaminecraft-1.5.8.jar ../gbaminecraft-1.5.8.jar

# Tests headless del núcleo (deben dar 103/103)
cd emulator-tests
bash run-tests.sh

# Boot test con la ROM real (cb2 debe llegar a 0802F6B1, fb_1300 >=30 colores)
javac -cp .build/out -d .build/out FbShot.java
java -cp .build/out FbShot ../../Pokemon_Esmeralda-.gba 1500 .audio_test 1300

# Captura de audio del APU (genera WAV en emulator-tests/.audio/)
javac -cp .build/out -d .build/out RomAudioCapture.java H.java
java -cp .build/out RomAudioCapture ../../Pokemon_Esmeralda-.gba 4200 1800

# Análisis espectral (SOLO PISTA, NO PRUEBA DE CALIDAD — ver §4)
pip install numpy
python3 spectral_analysis.py <wav> samples/gameplay_capture_60s.wav
```

### Cómo leer la fuente de mGBA (OBLIGATORIO antes de cualquier fix)
```bash
unzip -o -j mgba-master.zip "mgba-master/src/gba/audio.c" -d .mgba-ref   # Direct Sound GBA
unzip -o -j mgba-master.zip "mgba-master/src/gb/audio.c"  -d .mgba-ref   # PSG
# Ya extraídos en .mgba-ref/: gba-audio.c, gb-audio.c, audio.c,
# audio-resampler.c, interpolator.c, sdl-audio.c y sus .h
```
Funciones de mGBA relevantes para el Direct Sound: `GBAAudioSample`,
`GBAAudioSampleFIFO`, `GBAAudioScheduleFifoDma`, `GBAAudioWriteSOUNDCNT_HI`,
`_applyBias`, y cómo gestiona el DMA de sonido en VBlank.

---

## 9. ENTORNO DEL USUARIO

- Windows 11, Prism Launcher, Forge 1.20.1-47.4.10, JDK 17.0.15.
- Audio: salida a 48000 Hz (EarPods USB / Realtek). El log muestra
  `FBA: audio output started at 48000 Hz`.
- El usuario prueba en instancia TEST (pocos mods). Para ver qué build corre,
  busca en el log la línea `[FBA-DIAG] FBA-2026-06-14...`.

---

## 10. RESUMEN

- **Problema**: distorsión intermitente en el Direct Sound (música/PCM) de
  Pokémon Esmeralda. El PSG ya está bien (no tocar). ~75% resuelto por oído,
  falta ~25%.
- **Cómo trabajar**: con la ROM real + leyendo mGBA. UN cambio a la vez,
  validado por el OÍDO del usuario, no por métricas headless.
- **El diagnóstico fino y la solución los sacas tú** con esas herramientas.
