# Claim Blocks 4.0.0 - Minecraft 1.21.1 Fabric

Parche v4 sobre el mod existente. Aplica las 6 correcciones del prompt:
formato de textos con colores, alturas verificadas, contorno visual
siempre encendido y centrado en el jugador, 3 nuevas flags de efectos
pasivos solo en bloques de pago, textos de flags rediseñados, y nuevo
flujo de eliminación con un solo clic más confirmación.

## Descarga

**[claimblocks-4.0.0.jar](https://github.com/pewez267-dot/Rangos/raw/feat/claimblocks-v4_0/claimblocks-4.0.0.jar)**

## Cambios v3 → v4 al detalle

### 1. Formato de textos
- Restaurados los colores con `Formatting.GREEN/RED/YELLOW/AQUA/GRAY/GOLD/WHITE+BOLD`
  en todos los mensajes de chat, ActionBar y nombres de items del menú.
- Tildes y eñe corregidas: "Daño", "protección", "construcción", "dueño",
  "árboles", "está", "configuración", "información", "administración".
- Prefijos puro-ASCII: `[OK]`, `[!]`, `[x]`, `[+]`, `[-]`, `[i]`, `[Claim]`.
- Nombres de items en el menú con color verde/rojo según ON/OFF.
- **Bug crítico del título arreglado**: `Claim.sizeLabel()` ahora deriva
  el "NxN" del id del bloque (`claimstone_NxN`) en vez de `radius * 2`.
  Antes: claim de 500x500 mostraba "Zona 1000x1000". Ahora: "Zona 500x500".

### 2. Alturas verificadas (estrictamente crecientes)

| Bloque | Radio | Altura ±Y |
|--------|-------|-----------|
| 10x10   | 10  | **15**  |
| 25x25   | 25  | **20**  |
| 40x40   | 40  | **30**  |
| 64x64   | 64  | **40**  |
| 80x80   | 80  | **50**  |
| 100x100 | 100 | **60**  |
| 150x150 | 150 | **80**  |
| 250x250 | 250 | **100** |
| 300x300 | 300 | **120** |
| 500x500 | 500 | **150** |

Verifié `data/ClaimTier.java`: la lista ya estaba en orden estricto. Sin cambios.

### 3. Contorno visual rediseñado
`client/ClaimVisualization.java` reescrito:
- **Siempre** se dibuja mientras tengas una piedra de claim en la mano
  principal, mires a donde mires.
- Centrado en `client.player.getBlockPos()` (sigue al jugador en cada frame).
- Se eliminó por completo el chequeo de `crosshairTarget`.
- Alpha 0.85, color exacto del bloque por tier.

### 4. 3 flags de efectos pasivos (solo bloques de pago)

| Flag | Campo JSON | Default | Efecto aplicado a dueño/miembros |
|------|-----------|---------|----------------------------------|
| Regeneración pasiva | `effectRegeneration` | false | `REGENERATION I` cada 2s |
| Resistencia pasiva  | `effectResistance`   | false | `RESISTANCE I`   cada 2s |
| Velocidad pasiva    | `effectSpeed`        | false | `SPEED I`        cada 2s |

- Solo aparecen activables en claims `claimstone_250x250`, `300x300`, `500x500`.
- En tiers menores se muestran como vidrio gris bloqueado con lore
  "Solo disponible en zonas de pago".
- Los efectos se aplican con `showParticles=false` para que sean sutiles
  y nunca se aplican a intrusos.
- Implementado en `event/PassiveEffectsManager.java` (tick cada 40 ticks).

### 5. Textos de flags clarificados (16 + 3 = 19 flags totales)

Cada flag ahora muestra texto distinto según ON/OFF, p. ej.:
- `Construir: BLOQUEADO [ON]` (verde, vidrio lima) ↔ `Construir: permitido [OFF]` (rojo, vidrio rojo)
- `Cultivos: PROTEGIDOS [ON]` ↔ `Cultivos: sin protec. [OFF]`
- `Mobs hostiles: BLOQUEADOS [ON]` ↔ `Mobs hostiles: permit. [OFF]`
- `PVP: BLOQUEADO [ON]` ↔ `PVP: permitido [OFF]`
- ... etc para las 19

El lore es siempre 2 líneas en gris con la descripción y el estado dinámico
"Estado: ACTIVO - Clic para cambiar" / "Estado: INACTIVO - Clic para cambiar".

### 6. Nuevo flujo de eliminación

| Estado | Slot 46 | Slot 47 |
|--------|---------|---------|
| Normal | Barrera roja "Eliminar zona" | (vidrio negro de fondo) |
| Confirmando | TNT "Confirmar eliminación" | Lana verde "Cancelar" |

- Primer clic en slot 46 → entra en estado de confirmación; el menú se queda
  abierto, el botón cambia a TNT.
- Clic en TNT (slot 46) → elimina el claim, devuelve la piedra al inventario,
  cierra el menú: `[OK] Zona eliminada. Piedra devuelta a tu inventario.`
- Clic en lana verde (slot 47) → cancela; ActionBar `[i] Eliminación cancelada.`
- Clic en cualquier otra cosa también cancela el estado.
- Cerrar el menú implícitamente cancela (handler instance pierde el estado).

## Estructura nueva

```
claimblocks-mod/src/main/java/com/claimblocks/
├── ClaimBlocksMod.java           (registra PassiveEffectsManager.tick)
├── ClaimBlocksModClient.java
├── block/
│   ├── ClaimStoneBlock.java      (mensajes con color)
│   └── ModBlocks.java
├── command/
│   └── ClaimCommands.java        (todos los mensajes con color, info muestra effect flags)
├── data/
│   ├── Claim.java                (sizeLabel() corregido)
│   ├── ClaimFlags.java           (+3 effect flags + 3 entries en FlagId)
│   ├── ClaimManager.java
│   └── ClaimTier.java            (+isPaid() helper)
├── event/
│   ├── BlockProtectionEvents.java
│   ├── EntityProtectionEvents.java
│   ├── PassiveEffectsManager.java   ← NUEVO
│   └── PlayerTracker.java
├── gui/
│   ├── ClaimMenuHandler.java     (rediseñado: 19 flags, 2 páginas, nuevo delete flow, colores)
│   └── ClaimMenuScreen.java
├── client/
│   └── ClaimVisualization.java   (always-on, player-centered)
├── item/
│   └── ModItems.java
└── mixin/
    ├── ExplosionMixin.java
    └── FarmlandMixin.java
```

## Notas

- **No se tocó nada que no estuviera en el prompt**: 10 tiers, comandos,
  persistencia JSON, mixins (Explosion, Farmland), tracker, sonidos,
  estructura de carpetas, build, etc. Se mantienen.
- Versión `4.0.0`, archivo de salida `claimblocks-4.0.0.jar`.
- El JSON ahora guarda los 3 nuevos campos `effectRegeneration`,
  `effectResistance`, `effectSpeed` (default `false`).
- Compatibilidad: claims v3 (con `radius`/`height`) cargan sin problema;
  los nuevos campos se asumen `false` si no están en el JSON.
