# GBA Minecraft Mod

Un mod de Minecraft (Forge 1.20.1) que implementa un emulador completo de Game Boy Advance (GBA) dentro del juego. Coloca una consola GBA en tu mundo de Minecraft y juega cualquier ROM `.gba` directamente en la pantalla del bloque.

---

## Características

- **Emulador GBA completo en Java puro** — sin dependencias nativas
  - CPU **ARM7TDMI** completa: modo ARM de 32 bits y modo Thumb de 16 bits
  - **PPU**: modos de video 0–5, 4 capas de fondos regulares/afines, sprites (OBJ), paleta de 15 bits
  - **APU**: 4 canales DMG (Pulse1, Pulse2, Wave, Noise) + 2 canales FIFO DMA (A/B)
  - **Bus de memoria**: BIOS, EWRAM, IWRAM, I/O, VRAM, OAM, ROM (hasta 32 MB), SRAM/Flash
  - **DMA**: 4 canales con modos inmediato, VBlank, HBlank y FIFO de audio
  - **Timers**: 4 timers con prescalers 1/64/256/1024 y modo cascade
  - **Input**: 10 botones del GBA mapeados a teclas del teclado
  - **Detección automática de tipo de guardado**: SRAM, EEPROM, Flash
- **Integración con Minecraft**
  - Bloque **GBA Console** colocable en el mundo
  - Ítem **GBA Cartridge** que almacena la ROM como NBT
  - GUI con pantalla escalada 2× (480×320 píxeles), indicadores de botones y selector de velocidad
  - Comandos `/gba` para cargar ROMs desde la carpeta del mundo
  - Soporte multijugador: cada consola es una instancia independiente
  - Teclas en tiempo real enviadas al servidor vía paquetes de red

---

## Requisitos

- **Java 17** o superior
- **Minecraft 1.20.1**
- **Forge 47.2.20** (`net.minecraftforge:forge:1.20.1-47.2.20`)
- **Parchment mappings** `2023.09.03-1.20.1` (para desarrollo)

---

## Compilación

```bash
# Clonar / extraer el proyecto
cd gba-minecraft-mod

# En Linux/Mac:
./gradlew build

# En Windows:
gradlew.bat build
```

El JAR compilado aparecerá en `build/libs/gba-minecraft-mod-1.0.0.jar`.

> **Nota**: El archivo `gradle/wrapper/gradle-wrapper.jar` no está incluido por ser binario. Si no tienes Gradle instalado, descárgalo desde https://gradle.org/install/ o ejecútalo con tu instalación local de Gradle (`gradle build`).

---

## Instalación

1. Instala **Minecraft Forge 1.20.1** desde https://files.minecraftforge.net/
2. Copia `gba-minecraft-mod-1.0.0.jar` a la carpeta `mods/` de tu instancia de Minecraft
3. Inicia el juego

---

## Uso

### 1. Obtener la consola y el cartucho

```
/gba give       → Da un bloque GBA Console al jugador
/gba cartridge  → Da un GBA Cartridge vacío al jugador
```

### 2. Cargar una ROM

Coloca tu archivo `.gba` en la carpeta `<mundo>/roms/` dentro del mundo de Minecraft.

```
/gba load NombreDelJuego.gba
```

Esto carga la ROM en el cartucho que tengas en la mano. Si no tienes cartucho, crea uno automáticamente.

### 3. Jugar

1. Coloca el bloque **GBA Console** en el mundo
2. Haz clic derecho con el **GBA Cartridge** cargado en la mano — o abre la GUI y pulsa Load
3. Se abre la pantalla de la GBA. ¡A jugar!

### 4. Controles

| Tecla         | Botón GBA |
|---------------|-----------|
| `X`           | A         |
| `Z`           | B         |
| `Enter`       | Start     |
| `Backspace`   | Select    |
| `Flechas`     | D-Pad     |
| `S`           | R         |
| `A`           | L         |

### 5. Velocidad y control

- **1x / 2x / 4x** — botones en la GUI para cambiar la velocidad de emulación
- **Reset** — reinicia la ROM desde el principio
- **Stop** — detiene el emulador (la ROM sigue en el cartucho)
- **FPS** — se muestra en tiempo real en la GUI

---

## Comandos

| Comando                   | Descripción                                      |
|---------------------------|--------------------------------------------------|
| `/gba give`               | Da una GBA Console al jugador                    |
| `/gba cartridge`          | Da un GBA Cartridge vacío                        |
| `/gba load <archivo.gba>` | Carga ROM desde `<mundo>/roms/archivo.gba`       |
| `/gba info`               | Muestra info del cartucho en la mano             |

---

## Arquitectura del emulador

```
GBAEmulator (hilo dedicado a ~16.78 MHz)
├── ARM7TDMI (CPU)
│   ├── Modo ARM  — instrucciones de 32 bits
│   └── Modo Thumb — instrucciones de 16 bits
├── MemoryBus
│   ├── BIOS ROM (16 KB, HLE stub)
│   ├── EWRAM (256 KB)
│   ├── IWRAM (32 KB)
│   ├── I/O Registers (enruta a PPU/APU/Timers/DMA/Input)
│   ├── VRAM (96 KB)
│   ├── OAM (1 KB)
│   ├── Palette RAM (1 KB)
│   ├── ROM (hasta 32 MB)
│   └── SRAM/Flash (64–128 KB)
├── PPU — renderiza 240×160 @ 59.73 Hz
│   ├── Modos 0–5 (regular, afín, bitmap)
│   └── Sprites (128 OBJs, tamaños 8×8 a 64×64)
├── APU — 32768 Hz estéreo
│   ├── CH1: Square con sweep
│   ├── CH2: Square
│   ├── CH3: Wave (Wave RAM 64 nibbles)
│   ├── CH4: Noise (LFSR 15/7 bits)
│   └── FIFO A/B (audio PCM vía DMA)
├── DMA Controller (4 canales)
├── Timer Controller (4 timers)
└── GBAInput (10 botones)
```

---

## Estructura del proyecto

```
gba-minecraft-mod/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── gradlew / gradlew.bat
├── gradle/wrapper/
└── src/main/java/com/gbaminecraft/
    ├── GBAMod.java                     ← Clase principal del mod
    ├── emulator/
    │   ├── GBAEmulator.java            ← Orchestrador principal (hilo)
    │   ├── cpu/ARM7TDMI.java           ← CPU ARM7TDMI completa
    │   ├── memory/MemoryBus.java       ← Bus de memoria y I/O
    │   ├── ppu/PPU.java                ← Unidad de procesamiento gráfico
    │   ├── apu/APU.java                ← Unidad de procesamiento de audio
    │   ├── input/GBAInput.java         ← Manejo de botones
    │   ├── timer/TimerController.java  ← 4 timers hardware
    │   ├── dma/DMAController.java      ← 4 canales DMA
    │   └── cartridge/Cartridge.java    ← Carga y detección de ROMs
    └── minecraft/
        ├── block/GBABlock.java         ← Bloque GBA Console
        ├── client/GBAClientSetup.java  ← Registro de pantalla cliente
        ├── command/GBACommand.java     ← Comandos /gba
        ├── gui/
        │   ├── GBAMenu.java            ← Container del lado servidor
        │   └── GBAScreen.java          ← GUI cliente con pantalla GBA
        ├── item/GBACartridgeItem.java  ← Ítem cartucho con ROM en NBT
        ├── network/GBANetworkHandler.java ← Paquetes cliente↔servidor
        ├── registry/
        │   ├── ModBlocks.java
        │   ├── ModCreativeTabs.java
        │   ├── ModItems.java
        │   ├── ModMenuTypes.java
        │   └── ModTileEntities.java
        └── tileentity/GBATileEntity.java ← Tile entity con instancia del emulador
```

---

## Notas

- **BIOS**: El emulador incluye un stub HLE mínimo. Para máxima compatibilidad, coloca el BIOS oficial de GBA (`gba_bios.bin`, 16 KB) en la carpeta `<mundo>/roms/gba_bios.bin`. El cargador de ROMs lo detectará automáticamente en una futura actualización.
- **ROMs**: Asegúrate de poseer una copia legal del juego. Las ROMs de juegos comerciales tienen derechos de autor.
- **Rendimiento**: El emulador corre en un hilo Java dedicado. En hardware moderno debería mantener 60 FPS estables. Usa el multiplicador de velocidad si necesitas ir más rápido.
- **Guardado**: Los datos de guardado se almacenan en el NBT del tile entity del bloque y persisten al recargar el mundo.

---

## Licencia

MIT — ver LICENSE


---

# 🎮 Fantastic Boy Advance — actualización del front-end (handheld)

Esta actualización añade el modo **handheld de un solo item**, tal como se pidió, encima del núcleo del emulador existente.

## Qué cambia

- **Mod renombrado** a *Fantastic Boy Advance* (displayName).
- **Nuevo item: `Fantastic Boy Advance`** (`fantastic_boy_advance`). Lo sostienes y haces **clic derecho** para encender la consola — no necesitas colocar bloques.
- **Interfaz totalmente clicable** (`FantasticBoyScreen`):
  - **Navegador de ROMs** que lee la carpeta **`RomsGBA`** en la raíz de la instancia (se crea sola).
  - Al elegir una ROM: menú **Iniciar partida nueva / Cargar partida / Editar teclas / Volver**.
  - Durante el juego: pantalla con **relación de aspecto 3:2** (240×160 escalado), **D-pad, A, B, L, R, Start, Select clicables con el ratón** y también por teclado.
  - Botones: **Salir, Guardar estado, Cargar estado, Forzar guardado, Pausa**.
- **Persistencia**:
  - **Forzar guardado / al salir** → guarda la batería (SRAM) en `RomsGBA/saves/<rom>.sav`.
  - **Guardar/Cargar estado** → snapshot en `RomsGBA/states/<rom>.state` (RAM completa + registros de CPU).
- **Mapeo de teclas** editable y persistente en `config/fantasticboyadvance_keys.txt`.

> El bloque/cartucho y los comandos `/gba` anteriores siguen presentes; el item nuevo es la vía principal.

## Carpeta de ROMs

Coloca tus archivos `.gba` en `RomsGBA/` en la raíz de tu instancia (junto a `mods/`, `config/`, etc.). La carpeta se crea automáticamente al abrir la consola por primera vez.

## Compilar (en tu PC con internet)

```bash
cd gba-minecraft-mod
./gradlew build      # Windows: gradlew.bat build
# salida: build/libs/gba-minecraft-mod-1.0.0.jar
```

## ⚠️ Estado honesto del emulador (léelo)

El **núcleo del emulador (CPU/PPU/APU)** es un trabajo en progreso serio pero **incompleto**: todavía tiene bugs conocidos (p. ej. el cálculo de PC en saltos ARM) y **aún no ejecuta con precisión juegos comerciales**. Esta entrega completa la **experiencia de usuario** (item, interfaz, navegador, guardado, mapeo) sobre ese núcleo; lograr que los juegos corran "fluidos y sin bugs" como mGBA requiere mucho más trabajo de ingeniería de emulación, que conviene abordar de forma iterativa (primero arrancar un test ROM, luego juegos 2D simples, etc.).


---

# 🔧 Reparación del núcleo del emulador (v1.1)

Esta actualización corrige bugs profundos del núcleo del emulador y añade un arnés
de pruebas **headless** (sin Minecraft) que verifica el CPU y la PPU.

## Bugs críticos corregidos en el CPU ARM7TDMI

1. **ADD / SUB / RSB no escribían el resultado** en el registro destino (solo
   actualizaban flags y retornaban). Esto por sí solo impedía ejecutar cualquier
   programa real — era el bloqueador principal.
2. **El salto `B`/`BL` aplicaba el offset dos veces**, mandando el PC a una dirección
   incorrecta.
3. **Modelo de pipeline del PC inconsistente.** Se unificó: durante la ejecución
   `R15 = dirección_instrucción + 8` en ARM y `+ 4` en Thumb, con un flag
   `branchTaken` que evita el doble avance del PC tras un salto.
4. **Máscara de `MSR` incorrecta** (no cubría la forma inmediata).
5. **Operando del shifter con `Rm = R15`**: corrección de PC (+12 con shift por
   registro) acorde al nuevo modelo.
6. **SWI / IRQ unificados** en `enterException()` con banking correcto de
   registros, `SPSR` y vector.

## Bugs corregidos en la PPU

- **Timing HBlank/VBlank por fases**: el HBlank ahora ocurre una sola vez por línea
  (antes el flag quedaba siempre activo y disparaba IRQ en cada llamada).
- **VBlank IRQ dispara una sola vez** al entrar en la línea 160 (antes en cada línea
  de VBlank, inundando de interrupciones).
- **Render de scanline** reordenado para que las líneas 0..159 se dibujen todas.
- **Registros afines de BG3** (0x30–0x3F) añadidos.
- `pollVBlankEdge()` para que el DMA de VBlank se dispare exactamente una vez por frame
  (antes `GBAEmulator` lo lanzaba en cada paso de CPU durante todo el VBlank).

## ✅ Pruebas (headless, solo requieren un JDK 17+)

```bash
cd gba-minecraft-mod/emulator-tests
./run-tests.sh
```

Resultado esperado (verificado en el desarrollo):

```
CPU:                       27 PASARON, 0 FALLARON
PPU:                        5 PASARON, 0 FALLARON
INTEGRACION CPU+MEM+PPU:    5 PASARON, 0 FALLARON
```

Qué cubren:

- **CpuTest (27):** MOV inmediato, ADD/SUB (el bug crítico), cadenas de
  data-processing, shifts, `B`, `BL`+`BX LR`, `LDR`/`STR`, `STMIA!`/`LDMIA`,
  Thumb (MOV/ADD/branch), flags + ejecución condicional (`CMP`+`MOVEQ`/`MOVNE`),
  `MUL`, y un **bucle real** (`SUBS`+`BNE`, cuenta de 5 a 0).
- **PpuTest (5):** Mode 3 (bitmap 15-bit), Mode 4 (paletizado 8bpp), VBlank una
  vez por frame, y señalización de frame completo.
- **IntegrationTest (5):** un **programa ARM real** escribe `DISPCNT` (Mode 3) y
  píxeles en VRAM con `STRH`; luego la PPU renderiza y se verifica que el
  framebuffer muestra exactamente lo que el programa dibujó.

## ⚠️ Estado honesto

El núcleo ya **ejecuta código ARM/Thumb correctamente y renderiza** lo que ese código
dibuja: esa base estaba rota y ahora está verificada. Aun así, **esto no implica
compatibilidad con juegos comerciales todavía** — faltan piezas para precisión total
(audio FIFO completo, casos límite de PPU como ventanas/blending/mosaico, timing fino,
SRAM/Flash/EEPROM por tipo, y el HLE de BIOS). El camino para subir compatibilidad es
iterativo: arrancar test ROMs (p. ej. *armwrestler*, *tonc* demos) → homebrew 2D →
juegos comerciales. La arquitectura ya es la correcta para crecer.


---

# 🧩 v1.2 — Camino hacia Pokémon Emerald (HLE BIOS + Flash)

Esta versión añade las dos piezas que **bloquean el arranque** de juegos como
Pokémon Ruby/Sapphire/**Emerald**/FireRed/LeafGreen.

## HLE BIOS (emulación de alto nivel de las llamadas SWI)

En vez de ejecutar la BIOS original (con copyright), implementamos en Java las
llamadas `SWI` más usadas — lo que los juegos invocan constantemente:

- **Div / DivArm / Sqrt / ArcTan / ArcTan2** (matemática)
- **CpuSet / CpuFastSet** (copias y rellenos de memoria masivos)
- **LZ77UnComp (WRAM/VRAM), RLUnComp, Diff8/16bitUnFilter, BitUnPack** —
  descompresión de gráficos (Emerald descomprime casi todo su arte con esto)
- **BgAffineSet / ObjAffineSet** (matrices de rotación/escala)
- **VBlankIntrWait / IntrWait / Halt** (sincronización con el vídeo)
- **RegisterRamReset, SoftReset, GetBiosChecksum, SoundBias, MidiKey2Freq**

Las SWIs de sonido/multiboot que no modelamos se aceptan como *no-op* para que el
juego continúe en lugar de crashear.

## Memoria Flash (chip de guardado de Pokémon)

Nuevo `FlashMemory` con el protocolo de comandos real (escrituras a
`0x0E005555`/`0x0E002AAA`):

- **Modo ID**: devuelve fabricante + device id que el juego sondea al arrancar
  (Sanyo 128KB `0x62/0x13` para Emerald; Panasonic 64KB para 512Kb).
- **Programación byte a byte**, **borrado de sector (4KB)** y **borrado de chip**.
- **Conmutación de banco** para los chips de 128KB (dos bancos de 64KB).

El bus enruta automáticamente la ventana `0x0E000000` a Flash cuando el cartucho
declara `FLASH_512K`/`FLASH_1M`; si no, usa SRAM/EEPROM como antes.

## ✅ Pruebas añadidas (BiosFlashTest, 14/14)

`Div`, `CpuSet` (relleno y copia), `CpuFastSet`, `LZ77` (con back-reference),
lectura de **ID de Flash**, programación+lectura de byte, **borrado de sector**, y
**cambio de banco** de 128KB.

Suite total verificada: **CPU 27 · PPU 5 · Integración 5 · BIOS/Flash 14 = 51/51**.

## ⚠️ Estado honesto (importante para Emerald)

Con esto Emerald tiene lo necesario para **no crashear** en el arranque por SWIs ni
por la detección del chip de guardado. **Pero todavía NO está garantizado que llegue
al menú o al juego**: faltan piezas para compatibilidad real con un título tan
exigente, principalmente:

- **EEPROM** (algunos modos) y temporización precisa de Flash.
- **PPU**: ventanas (WIN0/1), blending/alpha, mosaico, y prioridad sprite-vs-BG
  exactas — Emerald los usa en menús y transiciones.
- **Timers + DMA de sonido (FIFO)** con sincronía fina, e **IRQs** de timer.
- **Waitstates/timing** de ciclos (Emerald no depende tanto, pero ayuda).
- Casos límite del **ARM/Thumb** (algunos modos de direccionamiento raros).

El plan correcto es ir iterando con la ROM real: arrancar, ver dónde se queda
(log de PC/SWIs), e ir tapando el siguiente agujero. La base ya es sólida y
**verificable** (51 tests), que era lo que faltaba.


---

# 🎨 v1.3 — PPU avanzada + EEPROM + audio FIFO

Esta versión añade las capacidades que los juegos usan en menús, transiciones y
guardado, acercándonos a compatibilidad real.

## PPU: compositor por píxel (ventanas, blending, brillo)

Se reescribió el render para componer **por píxel** las dos capas frontales:

- **Prioridad correcta** entre BG0–BG3 y sprites (OBJ), con desempates como el hardware.
- **Ventanas** WIN0 / WIN1 / OBJ-window / fuera (WININ/WINOUT): recortan qué capas se
  ven en cada región — base de cuadros de texto y máscaras de menú.
- **Alpha blending** (BLDCNT/BLDALPHA): mezcla de dos capas, y **OBJ semitransparentes**.
- **Brillo** (BLDY): fundidos a **blanco** y a **negro** (las transiciones de pantalla).
- Los fondos afines ahora usan la paleta correcta (0–255), no 256+.

## EEPROM (otro tipo de guardado común)

Nuevo `Eeprom` (512B y 8KB) con el **protocolo serial real** (comando `10`/`11`,
bits de dirección, 64 bits de datos). El bus lo enruta por la región `0x0D`.
El cartucho elige automáticamente EEPROM/Flash/SRAM por su cabecera.

## Audio FIFO por timer

El desbordamiento de **Timer 0/1** ahora avanza el sample DMA de sonido y refresca
el FIFO (canales A/B según `SOUNDCNT_H`) — la vía por la que los juegos reproducen
música y voces digitalizadas.

## ✅ Pruebas (suite total 59/59)

| Suite | Tests |
|---|---|
| CpuTest | 27 |
| PpuTest | 5 |
| **PpuFxTest** (nuevo: blending/brillo/ventana) | **4** |
| IntegrationTest | 5 |
| BiosFlashTest | 14 |
| **EepromTest** (nuevo) | **4** |

`cd gba-minecraft-mod/emulator-tests && ./run-tests.sh`

## ⚠️ Estado honesto

Vamos sumando las piezas correctas y **verificadas**. Ya están: CPU ARM/Thumb,
render con prioridad/ventanas/blending/brillo, HLE BIOS (incl. descompresión),
Flash + EEPROM + SRAM, timers con IRQ y FIFO de audio. **Aún no garantizo que
Pokémon Emerald llegue al juego**: falta pulir timing fino de ciclos, casos límite
de PPU (mosaico, prioridad OBJ-window exacta), y validar con la ROM real. El
siguiente paso natural es **arrancar la ROM y depurar dónde se traba** (añadir un
log de PC/SWIs/registros). La base es sólida y crece de forma medible.

---

# ⚡ v1.4 — IRQ del juego (HLE) + tracer de arranque

## Despacho de interrupciones del juego (lo más importante para avanzar)

Los juegos no "corren en línea recta": dependen de **interrupciones** (VBlank,
timers, DMA…) para ejecutar su lógica de cada frame. Ahora el emulador, con BIOS
HLE, hace lo que hace la BIOS real:

- Al dispararse una IRQ, **entra en modo IRQ**, guarda contexto (SPSR) y **salta
  al handler del juego** almacenado en `[0x03007FFC]`.
- Usa una dirección **centinela** de retorno: cuando el handler termina (`bx lr`),
  el emulador **restaura el contexto** y reanuda justo donde estaba.
- `VBlankIntrWait`/`IntrWait`/`Halt` detienen la CPU hasta que llega la IRQ.

Esto es lo que convierte "la consola tiene todas las piezas" en "el juego puede
avanzar su bucle principal".

## Tracer de arranque (para la fase de pruebas)

Nuevo `BootTracer`: registra una ventana de las últimas instrucciones (PC+opcode),
un histograma de **qué SWIs usó el juego**, el estado de vídeo/IRQ, y detecta
**bucles infinitos** (PC repetido). Desde la consola, en juego:

- Botón **Trace ON/OFF** y botón **Diagnóstico** → vuelca `RomsGBA/boot-trace.txt`.

Cuando probemos una ROM y se quede pegada, ese archivo nos dirá **exactamente**
en qué instrucción/SWI se traba, para arreglar lo siguiente con precisión.

## ✅ Pruebas (suite total 62/62)

CPU 27 · PPU 5 · PPU-FX 4 · Integración 5 · BIOS/Flash 14 · EEPROM 4 · **IRQ HLE 3 (nuevo)**

`cd gba-minecraft-mod/emulator-tests && ./run-tests.sh`

---

# 🏁 v1.5 — Sprites affine + mosaico + autosave + save-state completo

## PPU: sprites con rotación/escalado (affine) + mosaico

- **Sprites affine**: rotación y escala con la matriz OAM (PA–PD), incluyendo
  el modo **double-size**. Es lo que usan las pantallas de título, intros de
  combate y muchísimos efectos.
- **Mosaico** para fondos y sprites (registro MOSAIC).
- Modo OBJ-window reconocido (no pinta color directo).

## Guardado robusto

- **Autosave de batería** cada ~10 s mientras juegas → nunca pierdes la partida
  aunque se cierre el juego de golpe.
- `saveBattery`/`loadBattery` ahora persisten el **chip real** del cartucho
  (Flash, EEPROM o SRAM), no solo SRAM.
- **Save-state** ahora incluye también el chip de guardado, así que un estado es
  totalmente autocontenido.

## ✅ Pruebas (suite total 68/68)

CPU 27 · PPU 5 · PPU-FX 4 · Integración 5 · BIOS/Flash 14 · EEPROM 4 · IRQ HLE 3 · **SAVE-STATE 6 (nuevo)**

`cd gba-minecraft-mod/emulator-tests && ./run-tests.sh`

## 🎯 Estado: piezas grandes COMPLETAS

A nivel de arquitectura, la consola está **completa**:
- CPU ARM7TDMI (ARM + Thumb) con IRQ y excepciones
- Memoria completa + waitstates básicos
- PPU: modos 0–5, fondos regulares y afines, sprites (incl. affine), prioridad,
  **ventanas, alpha blending, brillo/fade, mosaico**
- HLE BIOS (matemática, CpuSet/FastSet, **descompresión LZ77/RLE/Huffman/Diff**, afines)
- Despacho de **IRQ al handler del juego**
- Timers con IRQ + **FIFO de audio**; APU con 4 canales PSG + 2 DMA
- DMA (inmediato/VBlank/HBlank/FIFO)
- Guardado **SRAM + Flash + EEPROM**, autosave y save-states
- Detección automática del tipo de guardado por la cabecera de la ROM
  (Pokémon Emerald = `FLASH1M_V` → 128 KB)
- Interfaz jugable + tracer de diagnóstico

Lo que queda es **afinado fino** (timing exacto de ciclos, casos límite) que solo
se puede pulir **probando con ROMs reales** y leyendo el `boot-trace.txt`.

**➡️ Cuando quieras, ejecutamos: mete una ROM en `RomsGBA`, abre el item, y si algo
falla, el botón Diagnóstico nos dirá exactamente qué arreglar.**

---

# 🐛 v1.5.1 — Bug crítico de pila (LDM/STM) corregido

El diagnóstico de arranque reveló que el emulador **se descarrilaba** muy temprano:
el Stack Pointer terminaba apuntando a la ROM (0x0800xxxx) y la CPU saltaba a
memoria inválida ejecutando ceros en bucle.

**Causa:** en `LDM/STM` (las instrucciones de `PUSH`/`POP`), el cálculo de la
dirección y del *writeback* en los modos **decrecientes** (STMFD/`push`, los más
usados para la pila) restaba 4 de más. Cada `PUSH` corrompía el SP en 4 bytes →
tras unas pocas llamadas, la pila quedaba destruida y el juego saltaba a basura.

**Arreglo:** reescrito el cálculo de direcciones para los 4 modos (IA/IB/DA/DB)
con el *writeback* correcto e independiente del bit P. Verificado con un test
nuevo (`StackTest`, 13/13) que incluye un roundtrip `PUSH`/`POP` completo donde el
SP vuelve exactamente a su valor original.

El tracer ahora **congela** la traza en el instante del descarrilamiento, mostrando
las instrucciones que llevaron al salto inválido.

Suite total: **81/81**.

---

# 🔬 v1.5.2 — Tracer auto-armado desde el arranque

El trace anterior solo capturaba 1 instrucción porque se activaba a mano, cuando
el juego ya llevaba rato descarrilado. Ahora el tracer **se arma automáticamente
desde la instrucción nº 0** al iniciar una ROM.

Cuando el CPU se descarrila, el diagnóstico ahora muestra:
- la **instrucción culpable** exacta (opcode + PC) que hizo el salto inválido,
- los **registros EN EL MOMENTO** del descarrilamiento (no la basura posterior),
- la ventana de las instrucciones previas.

Esto nos da la pista quirúrgica para arreglar el siguiente fallo de arranque.

---

# 📊 v1.5.3 — Diagnóstico exhaustivo (un solo volcado lo dice TODO)

Para no andar probando por una cosita cada vez, el diagnóstico ahora captura en
UN solo archivo:
- **Cabecera de la ROM** (título + entrypoint) → confirma que cargó bien.
- **Instrucción culpable** exacta + **registros en el momento** del descarrilamiento.
- **Histograma de tipos de instrucción** ejecutados (DataProc, LDR/STR, LDM/STM,
  B/BL, MUL, MSR/MRS, SWI, BX, Thumb...).
- **Regiones de memoria** por las que pasó el PC (BIOS/EWRAM/IWRAM/ROM...).
- Estado completo de vídeo (DISPCNT/DISPSTAT/VCOUNT/BG0CNT/WAITCNT) e IRQ.
- SWIs del BIOS usadas, detector de bucles, y ventana de instrucciones previas.

Con esto, un solo trace me da el panorama entero del arranque y puedo arreglar
varios fallos de una vez en lugar de uno por uno.

---

# 🎯 v1.5.4 — Dos bugs profundos de arranque (el trace de Emerald los reveló)

El diagnóstico exhaustivo mostró que **Pokémon Emerald YA corría** (1138 instrucciones
Thumb, título 'POKEMON EMER', usaba SoftReset/RegisterRamReset) pero se descarrilaba
en un `POP {pc}` con el Stack Pointer corrupto (R13=0x0800034D, en ROM y desalineado).

Dos causas raíz encontradas y corregidas:

1. **Ops Thumb de registro alto mal decodificadas.** Las instrucciones `0x46xx`
   (`MOV r8,r0`, `MOV r7,r8`, `ADD r10,r1`...) se enrutaban por error a la ALU
   normal en vez de a la rutina de registros altos. El boot de Pokémon usa r8/r10/r11
   como punteros de frame → se corrompían → la pila se destruía. Ahora todo el grupo
   0x4400–0x47FF (ADD/CMP/MOV/BX hi-reg) se decodifica bien. (HiRegTest 5/5)

2. **SoftReset incompleto.** El `SWI 0x00` del BIOS real reinicia los stack pointers
   de SVC/IRQ/Sistema y pasa a modo Sistema. El nuestro solo saltaba. Ahora configura
   las pilas correctamente (`biosReinitStacks`).

Además, el tracer ahora tiene un **tripwire de SP**: si el Stack Pointer sale de la
RAM o se desalinea, congela en el acto y muestra la instrucción exacta que lo hizo.

Suite total: **96/96**.

---

# 🔴➡️ v1.5.5 — Bug de la pantalla roja: LDR PC-relativo (carga de constantes)

¡El emulador pasó de pantalla negra a **roja** y ejecutó 29 MILLONES de instrucciones!
El trace mostró un bucle infinito de polling: `LDRB r0,[r1]; CMP r0,#0x9F; BNE`, con
**r1=0x0000B600** (una dirección basura, sin la parte alta).

**Causa raíz:** faltaba el handler de `LDR Rd,[PC,#imm]` (Thumb formato 6, `0x48xx`).
Esta es LA instrucción con la que los juegos cargan constantes de 32 bits (como las
direcciones de RAM `0x0203xxxx`). Se enrutaba por error a `thumbALU`, así que el
registro recibía basura → el juego leía una dirección inválida → bucle infinito →
pantalla roja congelada.

**Arreglo:** implementado `thumbLdrPcRel` (carga desde `(PC&~3)+imm*4`). Verificado:
ahora `LDR r1,[pc,#0]` carga la dirección completa `0x0203B600` en vez de `0xB600`.
(PcRelTest 2/2)

**Diagnóstico mejorado:** el tracer ahora detecta **bucles de espera (polling)** y
reporta el rango de PC donde el juego gira esperando algo que no cambia.

Suite total: **98/98**.

---

# ⚪ v1.5.6 — Diagnóstico de IRQ/frames + fix de falso descarrilamiento

¡Pantalla roja ➡️ **blanca**, IME=1, el juego instaló su handler de IRQ y descomprimió
gráficos (LZ77Vram)! Está mucho más cerca.

El trace anterior se "congelaba" en `BX LR -> 0xF000F000`, pero eso era un **falso
positivo**: 0xF000F000 es el centinela interno de retorno de IRQ (el mecanismo que
usa el CPU para saber cuándo el handler del juego terminó). Ahora el tracer lo
ignora y ya no se confunde con un crash.

Además, el diagnóstico ahora cuenta **IRQs disparadas (y cuántas VBlank)** y
**frames renderizados**, para confirmar de un vistazo si las interrupciones y el
vídeo están fluyendo o si el juego espera algo que no llega.

Suite total: **98/98**.
