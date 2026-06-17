# PROBLEMA DE AUDIO — estática/siseo residual en el emulador GBA (gbaminecraft / FantasticBoy)

> Este documento describe ÚNICAMENTE el problema y los hechos medidos. No contiene
> solución ni recomendaciones a propósito: la siguiente IA debe diagnosticar y
> resolver por sí misma.

## Repositorio y ubicación
- Repo: `pewez267-dot/Rangos` (público).
- Rama activa: `fix/gba-audio-revert-13z7-to-13z2`.
- Directorio del mod: `gba-minecraft-mod/`.
- ROM de referencia: `Pokemon_Esmeralda-.gba` (raíz del repo).
- Fuente de mGBA (referencia de comportamiento): `.mgba-ref/` (audio.c, gba-audio.c,
  audio-resampler.c, interpolator.h, etc.).
- Documento de traspaso previo: `HANDOFF_AUDIO.md` (raíz).
- Bitácora de los cambios de audio recientes: `FIX_AUDIO.md` (raíz).
- Suite de pruebas headless: `gba-minecraft-mod/emulator-tests/` (`run-tests.sh`,
  `RomAudioCapture.java`, `FbShot.java`, `measure_resampler.py`).

## Qué es este mod
Emulador de GBA dentro de Minecraft Forge 1.20.1 ejecutando Pokémon Esmeralda.
El APU genera audio estéreo de 16 bits a 32768 Hz; `GBAAudioOutput` lo transmite en
tiempo real a la tarjeta de sonido del sistema vía `javax.sound.sampled.SourceDataLine`
(hilo de audio separado, ring buffer SPSC). Minecraft, por su lado, usa OpenAL.

## EL SÍNTOMA EXACTO (reportado por el oído del usuario, única validación válida)
- Un **siseo/ruido de banda ancha constante**, descrito como "cuando se va la señal de
  la TV y queda el hormiguero", "estática".
- **Empieza exactamente cuando arranca el audio del GBA** (el "ding" del logo de
  GameFreak / el primer sonido) y se mantiene **constante para siempre**, sin importar
  qué suene.
- Va por **encima** del audio del juego (música/efectos). En pantallas realmente en
  silencio del GBA no se reporta.
- Estado general del audio por oído del usuario: ~95% bueno; esta estática es el ~5%
  restante.

## HECHOS MEDIDOS
1. **El audio que genera el emulador es limpio.** Captura headless con
   `emulator-tests/RomAudioCapture.java` (graba la salida de `APU.drainInto`, audio
   nativo 32768 Hz): los tramos en silencio son **silencio absoluto** (RMS=0; energía
   en 4–16 kHz = 0.0%). Durante música hay energía de alta frecuencia que sube hacia
   12–16 kHz (flatness ~0.65).
2. **El emulador, los gráficos, el boot y los 103/103 tests funcionan.** Boot a
   gameplay confirmado (`FbShot`: CB2 llega a 0x0802F6B1, `fb_1300` con 36 colores).
   `run-tests.sh`: 103/103 PASARON, 0 FALLARON.
3. **La tubería de audio en tiempo real está sana.** Traza en juego (`[FBA-DIAG]`):
   `dropped=0`, `ringFill` estable ~1126, `submitted≈written` (~65536 shorts/s =
   32768 estéreo), `emuFps=59.7`.
4. Dispositivo de salida del usuario: **Realtek (audio onboard del laptop)** →
   reportado como "Speakers (Realtek(R) Audio)" y como "Headset (EarPods)". Windows 11,
   AMD Ryzen 7 7435HS, RTX 4070, Java 17 (Microsoft 17.0.15).

## CAMBIOS YA APLICADOS Y SU RESULTADO POR OÍDO (todos: "se oye igual")
Cada build fue confirmado en ejecución por su marcador `BUILD` en el log
(`GBAEmulator.BUILD`, línea `[FBA-DIAG] FBA-2026-06-16zNN ...`).
- **z8** — revertir el reload del source del DMA Direct Sound al estado "13z2"
  (`internalSrc[ch] = srcAddr[ch]` en `DMAController.onVBlank`). Resultado: subió a
  ~95% (mejor versión confirmada por el usuario). La estática quedó como residual.
- **z9** — reemplazar la interpolación lineal del resampler de salida 32768→deviceRate
  por un kernel windowed-sinc polyphase (16 taps, 512 fases). Medido: imaging
  (16.5–24 kHz) 3.22% → 0.0157%. Resultado por oído: **sin cambio**.
- **z10** — filtro paso-bajo de 2 polos (~8 kHz) aplicado SOLO al Direct Sound en
  `APU.generateSample` (para atacar el imaging del zero-order-hold de 8 bits; el PSG no
  se filtra). Medido: estática 10–16 kHz 1.13% → 0.499%. Resultado por oído:
  **sin cambio**.
- **z11** — abrir la línea de salida a la **tasa nativa del dispositivo** (detección +
  fallback 44100 antes que 48000), para que el SO no resamplee el stream. El log
  confirma `FBA: audio output started at 44100 Hz` (antes 48000). Resultado por oído:
  **sin cambio**.

(Build actual en la rama = z11. La estática persiste idéntica en z8, z9, z10 y z11.)

## RESTRICCIONES DEL PROYECTO (hechos, no recomendaciones)
- El **PSG** (canales 1–4: cuadradas, wave, ruido) y los fixes **13g** (reset FIFO
  bits 11/15 de SOUNDCNT_H) y **13h** (offsets NR13/NR14, length, DAC-off, trigger)
  están marcados como correctos e intocables en este proyecto.
- La **única métrica de validación válida es el oído del usuario**; las métricas
  espectrales/headless han demostrado NO predecir lo que se oye (z9/z10/z11 mejoraron
  números sin cambiar el oído).
- El diagnóstico debe hacerse corriendo la **ROM real** y leyendo el código de
  **mGBA** (`.mgba-ref/`).
- Los 103 tests headless no deben romperse.

## MAPA DE ARCHIVOS RELEVANTES (rama fix/gba-audio-revert-13z7-to-13z2)
- `gba-minecraft-mod/src/main/java/com/gbaminecraft/emulator/apu/APU.java`
  — generación PSG + Direct Sound, FIFO A/B, `generateSample` (mezcla y
  zero-order-hold de `dmaASample`/`dmaBSample`), `drainInto`.
- `gba-minecraft-mod/src/main/java/com/gbaminecraft/emulator/GBAAudioOutput.java`
  — ring buffer SPSC, hilo de audio `audioLoop` (resampler windowed-sinc), apertura de
  línea (`CANDIDATE_RATES`/`buildRateCandidates`), cushion, diagnósticos (`status`).
- `gba-minecraft-mod/src/main/java/com/gbaminecraft/emulator/dma/DMAController.java`
  — Direct Sound FIFO (`requestSoundFifoDMA`/`executeFIFO`) y `onVBlank` (reload del
  source = estado 13z2).
- `gba-minecraft-mod/src/main/java/com/gbaminecraft/emulator/timer/TimerController.java`
  — overflow de timers que clockea el consumo del FIFO.
- `gba-minecraft-mod/src/main/java/com/gbaminecraft/emulator/GBAEmulator.java`
  — bucle principal, submit de audio, `BUILD` (marcador), traza `[FBA-DIAG]`.

## CÓMO COMPILAR Y MEDIR (el usuario compila; tú no compilas)
- Compilar el mod: `cd gba-minecraft-mod && ./gradlew build` (Forge 1.20.1, Java 17).
- Tests headless: `cd gba-minecraft-mod/emulator-tests && bash run-tests.sh`.
- Captura de audio de la ROM: `RomAudioCapture.java` (genera WAV a 32768 Hz).
- Análisis espectral del resampleo: `measure_resampler.py`.
