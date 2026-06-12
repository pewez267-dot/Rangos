# FSCrates — Contexto del Proyecto
> Última actualización: Junio 2026  
> Rama de trabajo: `fix/crate-models-anim-facing`  
> PR: https://github.com/pewez267-dot/Rangos/pull/49

---

## ¿Qué es este proyecto?

**FSCrates** es un mod de Minecraft Forge 1.20.1 que añade cofres con animación de apertura, ruleta de recompensas y sistema de rarezas. Usa modelos 3D del pack **"Crates and Stuff Model Pack Update 4"** (en `Crates and Stuff Model Pack Update 4.zip`).

- **Forge:** 1.20.1-47.2.0  
- **Java:** 17  
- **Gradle:** 8.1.1  
- **Archivo para compilar:** `fscrates-gradle-project-arreglado.zip` (última versión arreglada)

---

## Rarezas y sus modelos de origen (del pack)

| Rareza     | Modelo bbmodel origen  | Escala de render |
|------------|------------------------|------------------|
| COMMON     | common_crate.bbmodel   | 1.0x             |
| RARE       | vote_crate.bbmodel     | 1.0x             |
| EPIC       | rare_crate.bbmodel     | 1.0x             |
| LEGENDARY  | cosmetic_crate.bbmodel | **1.18x**        |
| MYTHIC     | legendary_crate.bbmodel| **1.35x**        |

---

## Problemas originales y soluciones aplicadas

### Bug 1: Solo se veían 2 modelos (common y rare) — RESUELTO
- **Causa:** epic, legendary y mythic tenían 2 texturas en el JSON original. El renderer vanilla solo acepta 1. El converter original además mapeó UVs incorrectamente generando caras degeneradas (área cero) en el 90%+ de faces de epic/mythic.
- **Solución:** 
  - Fusionar las 2 texturas de cada rareza en una sola hoja PNG (tex0 arriba, tex1 abajo) y remapear UVs.
  - Para epic y mythic: re-convertir desde los `.bbmodel` originales con la fórmula correcta: `u_json = (u_pixel / tex_width) × 16`.
  - Texturas re-extraídas desde el base64 embebido en los `.bbmodel`.

### Bug 2: La tapa no se animaba — RESUELTO
- **Causa:** El renderer calculaba `lidAngle` pero nunca lo aplicaba al modelo.
- **Solución:** 
  - Separar cada crate en **base** (`crate_X.json`) + **tapa** (`crate_X_lid.json`) usando los índices del hueso `lid` de los `.bbmodel`.
  - `CrateRenderer` rota la tapa `lidOpen(partialTick) × 62.5°` sobre el eje X en su bisagra (borde trasero-superior del cuerpo).

### Bug 3: Cofres colocados al revés — RESUELTO
- **Causa:** Modelos Blockbench miran a +Z pero el bloque usa `-rot`.
- **Solución:** Añadir **+180°** offset en la rotación Y del renderer.

### Bug 4: Bisagra mal posicionada (tapa "despegaba" del cuerpo) — RESUELTO
- **Causa:** El pivote Y se calculaba como `minY` de la tapa (borde inferior de la tapa), no como `maxY` del cuerpo (donde la tapa se une al cuerpo).
- **Solución:** Recalcular bisagras con `Y = base_max_y` (tope del cuerpo). Valores actuales en `CrateBakedModels.hinge()`.

### Feature 5: legendary/mythic más grandes — IMPLEMENTADO
- `CrateBakedModels.renderScale()` devuelve 1.18x para LEGENDARY y 1.35x para MYTHIC.
- `CrateRenderer` multiplica `sc * baseScale` antes de pintar base y tapa.

### Feature 6: Sin giro del cofre — IMPLEMENTADO
- Eliminado completamente `chestSpin()` de `CrateRenderer`.
- El cofre queda estático; solo la tapa se anima.

### Feature 7: Sin partículas durante la ruleta — IMPLEMENTADO
- `emitAccent()` en `CrateBlockEntity` solo emite partículas en fase FINALE (p ≥ 88%).
- Las partículas giratorias durante REVEAL fueron eliminadas para no tapar la ruleta.

### Feature 8: Timing correcto ruleta→recompensa — IMPLEMENTADO
- Delay de entrega: `animTotal` (100%) en lugar de `Math.round(animTotal × 0.9f)`.
- La recompensa se entrega exactamente cuando termina la animación completa.

### Feature 9: Rework de sonidos — IMPLEMENTADO
- **Reveal:** arpa que acelera + tono sube; RARE+ agrega bajo; sin totem, sin sonidos recargados.
- **Impacto (FINALE):** Bell + levelup (todos); EPIC+ xylofono+chime; LEGENDARY+ bajo+flauta; MYTHIC trueno suave + campana grave.
- **Arpegio:** Escala mayor completa, 3 ticks/nota, más notas para rarezas altas.
- **Flourish:** EPIC+ cohete; LEGENDARY+ cohete grande+flauta; MYTHIC doble cohete+xylofono+flauta. **SIN TOTEM_USE**.

---

## Archivos clave modificados

```
_mod/fscrates-gradle/src/main/java/com/fscrates/
├── client/
│   └── render/
│       ├── CrateBakedModels.java   ← registra modelos, bisagras, renderScale()
│       └── CrateRenderer.java      ← render base+tapa, escala, sin spin, +180 orient.
├── block/
│   └── CrateBlockEntity.java       ← sonidos, partículas, timing
└── crate/
    └── CrateOpeningService.java    ← delay de entrega = animTotal (100%)

_mod/fscrates-gradle/src/main/resources/assets/fscrates/
├── models/block/
│   ├── crate_common.json / crate_common_lid.json
│   ├── crate_rare.json   / crate_rare_lid.json
│   ├── crate_epic.json   / crate_epic_lid.json
│   ├── crate_legendary.json / crate_legendary_lid.json
│   └── crate_mythic.json / crate_mythic_lid.json
└── textures/block/
    ├── crate_common.png        (64×64, single texture)
    ├── crate_rare.png          (128×128, single texture)
    ├── crate_epic.png          (64×80, merged: body 64×64 + lock 16×16)
    ├── crate_legendary.png     (128×160, merged: body 128×128 + hat 32×32)
    └── crate_mythic.png        (64×80, merged: body 64×64 + lock 16×16)
```

---

## Scripts de análisis (no trackeados, pueden eliminarse)

| Script | Función |
|--------|---------|
| `_analyze.py`  | Mapeo bbmodel → JSON con transformada lineal |
| `_analyze2.py` | Identificar hueso `lid` y pivote real |
| `_analyze3.py` | Leer keyframes de animación del bbmodel |
| `_analyze4.py` | Bbox de lid/lock por rareza |
| `_convert.py`  | Conversión base+lid, fusión texturas |
| `_preview.py`  | Render isométrico de preview |

---

## Estado del repo

```
branch: fix/crate-models-anim-facing
commits:
  806ba3a - FSCrates: escala legendary/mythic, sin giro, sin particulas en ruleta, timing y sonidos
  6fc7f99 - Fix UV conversion for epic/mythic + fix all hinges (base_max_y)
  610e7ea - Fix FSCrates: modelos por rareza visibles, animacion de tapa y orientacion
  074990b - Add files via upload (original del usuario)
  00559ea - Add files via upload (original del usuario)
```

---

## Pendiente / Notas

- Las "zonas negras" visibles en los previews PNG son artefactos del renderizador isométrico simplificado (`_preview.py`), **no errores reales del modelo**. En Minecraft el z-buffer y backface culling los eliminan.
- Si el sonido `NOTE_BLOCK_IRON_XYLOPHONE` no compila en 1.20.1, reemplazar por `NOTE_BLOCK_XYLOPHONE`.
- Si `FIREWORK_ROCKET_LAUNCH` no compila, reemplazar por `FIREWORK_ROCKET_BLAST`.
- Los valores de escala 1.18x (LEGENDARY) y 1.35x (MYTHIC) son ajustables en `CrateBakedModels.renderScale()`.
- Los valores de bisagra son ajustables en `CrateBakedModels.hinge()`.



---

## Iteración v3 — Rework grande (tamaños, sonidos/partículas por rareza, ruleta CS:GO, luz, item 3D)

Archivo listo para compilar: **fscrates-gradle-project-arreglado-v3.zip**

### Cambios

1. **Tamaño legendary**: `renderScale` legendary `1.50 → 1.90` (mythic sigue 1.80). El
   CUERPO del modelo legendary es el más pequeño de todos (~6.8×6.8px), por eso se
   veía chica; ahora queda a nivel de mythic pero un poco por debajo. El escalado
   sigue anclado al suelo (no se hunde).

2. **Ruleta estilo CS:GO**: `REEL_LOOPS = 10` (más vueltas = más rápida) y la curva
   compartida `easeOutReel` pasó de quartica a **cúbica** (mucha velocidad al inicio,
   desaceleración gradual hasta el premio). Render del carrusel y sonido usan la MISMA
   fórmula/constante (sincronizados).

3. **Rareza por item del pool**: `RewardEntry.rarity` (String, vacío = hereda la de la
   crate) + `effectiveRarity(fallback)`. En el editor (pestaña Premios, item) hay botón
   **"Rareza: Auto/Común/…/Mítica"**. La rareza efectiva del premio ganado viaja al
   cliente en `PlayAnimationPacket.winnerRarity` y define luz, sonido y partículas.

4. **Luz de faro por rareza**: el haz (`renderBeam`) ahora sale en TODAS las animaciones
   al abrir la tapa, con el COLOR de la rareza del item ganado, más notorio que antes.

5. **Rework de sonidos por rareza** (sin totems / logros / amatista de picar):
   - Tick de ruleta = `UI_BUTTON_CLICK` limpio (sube de tono, sincronizado al giro).
   - Llave/desbloqueo = `CHAIN_PLACE` + `UI_BUTTON_CLICK` (antes una nota de bajo fea).
   - Golpe de victoria `playWin(Rarity)` + cola `playWinTail(Rarity)` que cambian según
     la rareza del item: campanas/XP (común) … rugido de dragón + trueno + gong + faro
     (mítico). Suena justo cuando para la ruleta y se entrega el premio.
   - Overload `play(Holder<SoundEvent>)` para referir sonidos sin `.value()`.

6. **Partículas por rareza**: FINALE usa `finaleParticle(rareza)` + chispas con el color
   de la rareza; además aura ambiental por TEMA (`themeParticle`) baja alrededor del
   cofre (no tapa la ruleta) para que cada animación se vea distinta.

7. **Item en mano = cofre 3D real (no barril)**: nuevo `CrateItemRenderer` (BEWLR) +
   `CrateBlockItem.initializeClient`; `models/item/crate.json` ahora `builtin/entity` con
   display transforms. Renderiza el modelo 3D por rareza en mano/inventario/GUI.

### Archivos nuevos
- `client/render/CrateItemRenderer.java`
- `item/CrateBlockItem.java`

### Notas de compilación (no se compiló aquí; el usuario compila)
- Todos los sonidos usados existen en 1.20.1 oficial. El overload `play(Holder<SoundEvent>)`
  hace que compile sin importar si un `SoundEvents.*` es `SoundEvent` o `Holder`.
- Si algún `ParticleTypes.*` (GLOW, WITCH, END_ROD, FIREWORK, FLAME, ENCHANT,
  HAPPY_VILLAGER) faltara en el mapping, sustituir por uno equivalente.



---

## Iteración v4 — Pulido (sonidos más épicos, tapa, velocidad constante, ruleta horizontal, puntero, haz cambiante, item centrado)

Archivo: **fscrates-gradle-project-arreglado-v4.zip**

1. **Sonidos por rareza más épicos** (`playWin`/`playWinTail`), énfasis legendary/mythic:
   legendary = gong + brillo + level up + cohete grande + `TRIDENT_THUNDER` + faro;
   mythic = `ENDER_DRAGON_GROWL` + `WARDEN_SONIC_BOOM` + trueno + cohete + gong profundo + faro.
2. **Tapa abre más** (`OPEN_ANGLE_DEG` 62.5 → 100°) para que el haz no choque con la tapa.
3. **Velocidad de ruleta CONSTANTE** sin importar el tamaño del pool: se reemplazó
   `n*loops` por `REEL_STEPS` (80) fijo vía `reelTravel(n,winner)` (usado por render y
   sonido). Antes con más items iba más rápido.
4. **Ruleta siempre HORIZONTAL** (se eliminó el modo vertical/tragamonedas en el render).
5. **Item centrado en el slot**: el BEWLR hacía un `translate(-0.5)` extra además del que
   ya aplica el ItemRenderer de Forge → se iba al rincón inferior. Se quitó.
6. **Sonido de apertura PROPIO por cofre/tier** (`playUnlock(rarity)`): cada tier tiene su
   desbloqueo (cadena/trampilla/puerta de hierro + flair: faro, conducto, rugido…).
7. **Haz de luz CAMBIANTE**: el color del haz sigue al item que pasa por el centro de la
   ruleta (no se queda fijo en el del premio). Para ello viajan las rarezas de TODOS los
   candidatos (`poolRarities` → NBT "rar" → `candidateRarities`).
8. **Puntero/indicador central** (dos flechitas blancas) que marca el item en el centro,
   tipo ruleta real (`triangle()` en `renderReel`).
9. **Legendary aún más grande** (1.90 → **2.10**), cerca de mythic pero por debajo (su
   modelo/cuerpo es el más pequeño de todos).
