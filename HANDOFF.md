# HANDOFF — GBA Emulator Mod (FantasticBoy / gbaminecraft)

> Documento de traspaso para la próxima sesión de Kiro.
> Fecha: 2026-06-14  |  Última versión compilada: **FBA 13v**  |  Repo: `pewez267-dot/Rangos`  |  Rama: `fix/gba-audio-mgba-mix`

---

## 1. PROMPT EXACTO PARA LA PRÓXIMA SESIÓN

```
Continúa el trabajo en el mod de Minecraft Forge 1.20.1 que emula una GBA (FantasticBoy / gbaminecraft).

Repositorio: pewez267-dot/Rangos
Rama de trabajo: fix/gba-audio-mgba-mix
Último commit: 2ac5299 (FBA 13v)
Jar compilado en raíz del repo: gbaminecraft-1.5.8.jar (168 650 bytes)

=== PROBLEMA ACTIVO (ALTA PRIORIDAD) ===

AUDIO HORRIBLE: distorsión, pitido, borroso — confirmado con build 13v corriendo.
Build marker confirmado en log del usuario: "FBA-2026-06-14v audio-pi-controller + tighter-pacing (input feel)"

DIAGNÓSTICO ACTUAL (hipótesis validada con log):
  - El APU genera audio LIMPIO (captura headless WAV: peak=14270, rms=2390, 0 clips).
  - GBAAudioOutput abre la línea pidiendo 32 768 Hz porque ese es el sample rate del GBA.
  - Windows NO soporta 32 768 Hz de forma nativa en ningún DAC moderno. Acepta la petición
    pero mete un resampler en el kernel: 32 768 → 48 000 Hz.
  - El resampler de Windows para tasas raras (no múltiplos de 48000) es malo: produce
    aliasing de alta frecuencia (pitido), filtro de baja calidad (borroso) y posible
    truncación de bits (distorsión). Esto es exactamente lo que oye el usuario.
  - Nuestro mod YA tiene un resampler lineal propio (GBAAudioOutput.audioLoop, rama resample)
    que convierte 32 768 → deviceRate. Solo hay que hacer que deviceRate sea 48 000 en vez
    de 32 768.

FIX PENDIENTE (una línea):
  Archivo: gba-minecraft-mod/src/main/java/com/gbaminecraft/emulator/GBAAudioOutput.java
  Línea actual:
    private static final int[] CANDIDATE_RATES = { APU.SAMPLE_RATE, 48000, 44100, 22050 };
  Cambio:
    private static final int[] CANDIDATE_RATES = { 48000, 44100, APU.SAMPLE_RATE, 22050 };

  Con esto la línea Java abrirá a 48 000 Hz (nativo en casi todo hardware Windows) y nuestro
  resampler lineal interno hará 32 768 → 48 000 en vez del resampler del kernel de Windows.
  El log deberá mostrar "audio output started at 48000 Hz" en vez de "32768 Hz".

SI EL FIX DE UNA LÍNEA NO RESUELVE:
  - Hipótesis B: el resampler lineal de GBAAudioOutput.audioLoop es de baja calidad
    (interpolación lineal simple, alias en frecuencias >13 kHz). Alternativa: resampler
    sinc de 16 puntos (implementación sencilla, ~50 líneas de Java).
  - Hipótesis C: el problema está más atrás en el APU (mezcla, ganancia, DC offset).
    Capturar WAV con RomAudioCapture.java y comparar con el 13r que funcionaba bien.

=== PROBLEMA SECUNDARIO (LAG RESIDUAL) ===

El usuario nota leve retraso al cambiar de dirección en marcha.

LO QUE YA SE HIZO (13v):
  - sleep granularity floor subido a 6 ms (antes ~4.9 ms) → ventana de busy-spin
    mayor, absorbe hiccups del scheduler de Windows.
  - Resultado esperado: pacing max ~17 ms (antes 21 ms), press->KEYINPUT max ~16.7 ms.

LO QUE NO SE PUEDE REDUCIR MÁS (físico):
  - press->KEYINPUT avg = 8 ms → ÓPTIMO. Pokémon lee KEYINPUT 1 vez/frame en VBlank.
    Media matemática = medio frame = 8 ms. Irreducible sin parchear el cartucho.
  - handoff publicar->recoger avg = 3.5 ms → ÓPTIMO. Minecraft renderiza cada 7 ms
    (143 fps), espera media = 3.5 ms.
  - Sistema de pasos de Pokémon (8 frames/paso = 134 ms/paso, no se puede girar a mitad)
    → comportamiento del juego, idéntico en GBA real.

=== VALIDACIONES QUE DEBEN PASAR ANTES DE ENTREGAR ===

1. cd gba-minecraft-mod/emulator-tests && bash run-tests.sh → 103/103 tests
2. FbShot con la ROM real → cb2 debe transitar por la secuencia completa de boot,
   fb_1300 debe tener ≥30 colores (confirma que el juego llega a gameplay)
3. Log del usuario debe mostrar "audio output started at 48000 Hz"
4. Usuario confirma que el audio ya no suena horrible

=== CÓMO COMPILAR ===

  export JAVA_HOME=/opt/toolchains/.local/share/mise/installs/java/17.0.2
  cd gba-minecraft-mod
  gradle build --no-daemon --console=plain -x test
  cp build/libs/gbaminecraft-1.5.8.jar ../gbaminecraft-1.5.8.jar

El jar compilado va en la raíz del repo. Subir con github power push_to_remote a la misma
rama fix/gba-audio-mgba-mix.

Link raw del jar: https://github.com/pewez267-dot/Rangos/raw/fix/gba-audio-mgba-mix/gbaminecraft-1.5.8.jar

=== ENTORNO DEL USUARIO ===

  - OS: Windows 11 25H2, AMD Ryzen 7 7435HS, RTX 4070 Laptop, 16 GB RAM
  - Monitor: 144 Hz
  - Audio: EarPods (aparece como "Headset (EarPods)" en OpenAL, DAC USB)
  - Prism Launcher 11.0.2, Forge 1.20.1-47.4.10, JDK 17.0.15 Microsoft
  - Instancia TEST (solo mods del servidor + gbaminecraft): la que usa para testear el mod
  - Instancia 1.20.1 (todos los mods del servidor, >130 mods): la de juego real
  - ROM: Pokemon_Esmeralda-.gba (en la raíz del repo, NO subir al commit)
```

---

## 2. ESTADO ACTUAL DEL REPO

| Ítem | Valor |
|---|---|
| Rama | `fix/gba-audio-mgba-mix` |
| Último commit | `2ac5299` — FBA 13v |
| Build marker | `FBA-2026-06-14v audio-pi-controller + tighter-pacing (input feel)` |
| Jar en raíz | `gbaminecraft-1.5.8.jar` (168 650 bytes) |
| Tests | 103/103 ✓ |
| ROM | `Pokemon_Esmeralda-.gba` (en raíz, no es código) |

---

## 3. HISTORIAL DE COMMITS RELEVANTES

```
2ac5299  FBA 13v: pacing tighter (sleep granularity floor 6 ms)
1efbddb  FBA 13u: arregla audio horrible (PI controller para buffer drift)
0837183  FBA 13t: cushion A/V tunable in-game (60-180 ms), default 140 ms
1c940c4  FBA 13s: arregla LAG real - prefetch waitstates ROM/EWRAM, elimina *4 hack
ce9d164  FBA 13r: revertir bajada de prioridad de 13p
```

El **lag real de gameplay** (personaje lento, diálogos lentos) fue arreglado en **13s**.
El problema actual es el audio, no el lag de gameplay.

---

## 4. ARQUITECTURA DEL EMULADOR (resumen para contexto)

```
GBAEmulator.java       → hilo GBA-Emulator, runFrame(), loop de pacing
  ARM7TDMI.java        → CPU, step() devuelve ciclos con waitstates incluidos
  PPU.java             → gráficos, CYCLES_PER_DOT=4, 308×228×4=280896 ciclos/frame
  APU.java             → audio, SAMPLE_RATE=32768, BUFFER_SIZE=2048 stereo samples
  TimerController.java → timers TM0-TM3, maneja overflow para Direct Sound
  DMAController.java   → DMA, onTimerOverflow() para replenish de FIFO A/B
  MemoryBus.java       → bus, waitstates WAITCNT, IRQ
GBAAudioOutput.java    → salida de audio real-time, SourceDataLine, resampler lineal
FantasticBoyScreen.java→ pantalla de juego, input, textura doble buffer
```

**Pipeline de audio**:
```
APU.tick() → audioBuffer[] (32768 Hz)
           → drainInto() → GBAAudioOutput.submit() → ring buffer
                         → audioLoop() → SourceDataLine (deviceRate Hz)
                                       ↑ resampler lineal si deviceRate ≠ 32768
```

**Por qué 13s fue crítico**:
- Antes de 13s: `cycles = cpu.step() * 4` → el juego corría al 25% de la velocidad real.
- 13s: eliminó el `*4` y añadió waitstates reales por región (ROM/EWRAM vs IWRAM) según WAITCNT,
  igual que mGBA. El juego ahora ejecuta ~105k instrucciones/frame (rango real: 110-170k).

---

## 5. DIAGNÓSTICOS CLAVE (logs del usuario)

### Log instancia TEST con 13v (CONFIRMADO — este es el build corriendo)
```
[FBA-DIAG] FBA-2026-06-14v audio-pi-controller + tighter-pacing (input feel)
  FBA: audio output started at 32768 Hz.   ← ← ← PROBLEMA AQUÍ
  buffer=140ms  rate=32768Hz  minBufFill=129ms  underruns=0
```
El buffer PI controller funcionó (se quedó en ~140 ms estable). Pero el audio sigue
horrible → la causa NO era el controlador, era el sample rate que se pide a Windows.

### Log instancia TEST con 13t (VIEJO, solo referencia)
```
buffer oscilaba 50-170 ms cada ~95 s → eso YA se arregló en 13u con PI controller
```

### Captura headless WAV (APU directo, sin pasar por Windows)
```
dur=30.57s  peak=14270  rms=2390  clips=0  L/R corr=1.000
jumps >24000: solo 6 (naturales de la música)
→ APU produce audio LIMPIO
```

---

## 6. ARCHIVOS MODIFICADOS EN ESTA SESIÓN

| Archivo | Qué se hizo |
|---|---|
| `GBAEmulator.java` | PI controller (Kp=1.0, Ki=0.05, deadband 3ms, clamp ±1%, anti-windup) para buffer drift; sleep granularity floor 6ms |
| `gbaminecraft-1.5.8.jar` | Jar compilado actualizado |

**NO se tocó** `GBAAudioOutput.java` `APU.java` `ARM7TDMI.java` `MemoryBus.java` en esta sesión.

---

## 7. PRÓXIMOS PASOS EN ORDEN

### Paso 1 — FIX DEL AUDIO (urgente)

Cambiar en `GBAAudioOutput.java`:
```java
// ANTES:
private static final int[] CANDIDATE_RATES = { APU.SAMPLE_RATE, 48000, 44100, 22050 };

// DESPUÉS:
private static final int[] CANDIDATE_RATES = { 48000, 44100, APU.SAMPLE_RATE, 22050 };
```
Compilar → tests → FbShot → push → entregar al usuario.
El usuario verá "audio output started at 48000 Hz" y el audio debería sonar limpio.

### Paso 2 — SI EL AUDIO SIGUE MAL con 48000 Hz

Mejorar el resampler en `GBAAudioOutput.audioLoop()`:
- El resampler lineal actual hace interpolación 1er orden entre muestras adyacentes.
- Para 32768→48000 (ratio 0.6827) esto introduce aliasing por encima de ~11 kHz.
- Fix: resampler sinc de N=16 puntos con ventana Kaiser (bien documentado, ~50 líneas Java).

### Paso 3 — Documentar/cerrar el lag

Si el usuario sigue notando lag mínimo después del fix de audio, explicarle que:
- El lag de gameplay real ya se arregló en 13s.
- El lag residual al cambiar dirección es 8 ms (medio frame, óptimo) + pipeline Minecraft
  (~25-50 ms) + sistema de pasos de Pokémon (134 ms/paso).
- Para comparar: GBA real tiene exactamente el mismo lag de input del juego.

---

## 8. COMANDOS ÚTILES (sandbox Linux)

```bash
# Compilar
export JAVA_HOME=/opt/toolchains/.local/share/mise/installs/java/17.0.2
cd /projects/sandbox/Rangos/gba-minecraft-mod
gradle build --no-daemon --console=plain -x test

# Tests headless (deben dar 103/103)
cd /projects/sandbox/Rangos/gba-minecraft-mod/emulator-tests
bash run-tests.sh

# Verificar build marker en el jar
unzip -p /projects/sandbox/Rangos/gbaminecraft-1.5.8.jar \
  com/gbaminecraft/emulator/GBAEmulator.class | strings | grep "FBA-2026"

# FbShot — boot test con ROM real
cd /projects/sandbox/Rangos/gba-minecraft-mod/emulator-tests
rm -rf .audio_test && mkdir -p .audio_test
javac -cp .build/out -d .build/out FbShot.java
java -cp .build/out FbShot ../../Pokemon_Esmeralda-.gba 1500 .audio_test 1300
# Debe mostrar cb2->0802F6B1 y fb_1300 con ≥30 colores

# Captura de audio headless
cd /projects/sandbox/Rangos/gba-minecraft-mod/emulator-tests
javac -cp .build/out -d .build/out RomAudioCapture.java H.java
java -cp .build/out RomAudioCapture ../../Pokemon_Esmeralda-.gba 4200 1800
# WAV en emulator-tests/emulator-tests/.audio/rom_capture.wav
```

---

## 9. NOTAS IMPORTANTES

- **NO subir** `mgba-master.zip`, `mGBAproyecto.zip`, ni la ROM `Pokemon_Esmeralda-.gba`
  en commits. Están en .gitignore implícito pero vigilar.
- El **gradle-wrapper.jar del repo está corrupto**. Usar siempre `gradle` del sistema
  con `JAVA_HOME` apuntando al JDK 17.0.2 de mise.
- El usuario trabaja en **instancia TEST** (pocos mods) para testear y en **instancia 1.20.1**
  (todos los mods) para jugar en el servidor `tierrasfantasibilias.mnt.li`.
- Para verificar qué build está corriendo buscar en el log la línea:
  `[FBA-DIAG] FBA-2026-06-14...`
  Si no aparece, el usuario no abrió el emulador durante esa sesión.
- El jar siempre va con nombre `gbaminecraft-1.5.8.jar` (Forge lo carga por ID del mod,
  no por nombre de archivo, pero mantener ese nombre evita confusión).

---

## 10. RESUMEN EJECUTIVO

**¿Qué funciona bien?**
- Gameplay a velocidad real (no más lag de personaje/diálogos) — arreglado en 13s
- emuFps estable en 59.7 Hz
- Audio sin underruns, buffer estable en ~140 ms
- 103/103 tests headless pasan
- Input lag óptimo (8 ms media, óptimo físico)
- Boot de Pokemon Esmeralda estable, sin pantalla blanca
- Autosave asíncrono (sin congelar renders)
- Triple buffer de video (sin costura horizontal)

**¿Qué sigue roto?**
- **Audio suena horrible** (distorsión/pitido/borroso) cuando la línea de audio abre a 32768 Hz
  y Windows mete su resampler de kernel. Fix trivial: cambiar el orden de CANDIDATE_RATES
  para pedir 48000 Hz primero.
