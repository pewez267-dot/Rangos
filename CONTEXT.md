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
