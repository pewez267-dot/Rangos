# Claim Blocks 5.0.0 - Server-side Minecraft 1.21.1 Fabric

Parche v4.1 (renombrado a 5.0.0 por consistencia con el nombre de JAR pedido).
Convierte el mod a server-side only, restaura el símbolo `✔` para mensajes
de éxito, añade 7 flags nuevas, y un panel administrativo completo.

## Descarga

**[claimblocks-5.0.0.jar](https://github.com/pewez267-dot/Rangos/raw/feat/claimblocks-v5_0/claimblocks-5.0.0.jar)** (124 KB)

> Nota sobre versionado: el spec mencionaba simultáneamente "4.1.0" en
> fabric.mod.json y "5.0.0" en el filename y en las notas finales OP. Elegí
> **5.0.0** consistente con el filename + 2/3 referencias a 5.0.0.

## Cambios v4.0 → v5.0

### 1. `[OK]` → `✔` (heavy check mark)
Reemplazado en TODOS los mensajes de chat / ActionBar / nombres de items
de menú. `sed -i 's/\[OK\]/✔/g'` aplicado al árbol completo. Resto de
prefijos (`[!]`, `[x]`, `[+]`, `[-]`, `[i]`, `[Claim]`) sin cambios.

### 2. Server-side only
- `fabric.mod.json`: `"environment": "server"`, eliminado entrypoint `"client"`.
- **Eliminados** `client/ClaimVisualization.java` y `ClaimBlocksModClient.java`.
- El contorno 3D de previsualización **ya no existe** (era la única
  feature que necesitaba código cliente). El menú GUI sigue funcionando
  porque usa `ScreenHandlerType.GENERIC_9X6` que el cliente vanilla
  ya sabe renderizar.
- Los mixins server-side (`ExplosionMixin`, `FarmlandMixin`,
  `EnderPearlMixin`) se mantienen.
- Los jugadores pueden conectarse con cliente vanilla; no necesitan el mod.

### 3. 7 flags nuevas (1 cancelada por Corrección 5)

| Flag | JSON | Default | Página menú | Implementación |
|------|------|---------|-------------|----------------|
| `blockAnimalKilling` | `blockAnimalKilling` | true  | 1 | `ServerLivingEntityEvents.ALLOW_DAMAGE` |
| `blockChestAccess`   | `blockChestAccess`   | true  | 1 | `UseBlockCallback` (chest/barrel/shulker/ender/furnace) |
| `blockCropHarvest`   | `blockCropHarvest`   | true  | 1 | `PlayerBlockBreakEvents.BEFORE` (CropBlock + NetherWart maduros) |
| `blockAnvilUse`      | `blockAnvilUse`      | true  | 2 | `UseBlockCallback` (`AnvilBlock`) |
| `blockEnderPearl`    | `blockEnderPearl`    | true  | 2 | Mixin `EnderPearlEntity#onCollision` |
| `blockSignEditing`   | `blockSignEditing`   | true  | 2 | `UseBlockCallback` (`AbstractSignBlock`) |
| `allowFlight`        | `allowFlight`        | false | 2 (paid) | `PassiveEffectsManager` cada 20 ticks toggles `getAbilities().allowFlying` |
| ~~`blockCommandsInClaim`~~ | — | — | — | **CANCELADA** por Corrección 5 |

`allowFlight` solo aparece activable en claims `claimstone_250x250/300x300/500x500`.
En tiers menores se muestra como vidrio gris bloqueado, igual que los 3 effects.

### 4. Acentos y símbolos restaurados
Verificado: "protección", "administración", "configuración", "eliminación",
"confirmación", "regeneración", "interacción", "Daño", "dueño", "está",
"árboles" — todos los textos del código y lang files.

### 5. (cancelada — `blockCommandsInClaim` no implementada)

### 6. Panel de Administración para OPs (`/claimadmin`)

Comandos:

| Comando | Función |
|---------|---------|
| `/claimadmin` | Abre panel GUI con lista de todos los claims |
| `/claimadmin list` | Lista en chat con prefijo `✔` y formato definido |
| `/claimadmin bypass` | Toggle del modo bypass (ignora protecciones) |
| `/claimadmin stats` | Estadísticas globales (total, dueños únicos, más grande, más antigua, paid) |
| `/claimadmin globalflag <flag> <on\|off>` | Cambia `globalPVP`/`globalMobGriefing`/`globalFireSpread` (con autocompletado, broadcast a todos) |

Panel principal (54 slots):
- 0..44: claims paginados, item = la piedra de claim correspondiente, lore con coords/dim
- 45/53: prev/next page
- 46: Estadísticas (libro)
- 47: Flags Globales (palanca → submenú)
- 48: Modo Bypass ON/OFF (espada de oro)
- 49: Cerrar (barrera)

Sub-menú por claim (27 slots útiles + filler):
- 11: **Teleportar** (brújula) — top-Y al `getTopY` para no enterrarse
- 12: **Ver/editar flags** (palanca) — abre `ClaimMenuHandler` con título `[Admin] Flags de [dueño]`
- 13: **Eliminar claim** (barrera/TNT) — flujo de confirmación con notificación al dueño (queue si offline)
- 15: **Transferir claim** (papel) — pide nombre por chat, busca online o `UserCache`, limpia miembros
- 22: Volver al panel

Bypass mode:
- `Set<UUID>` en memoria, perdido al reiniciar (per spec)
- Recordatorio en ActionBar `[!] BYPASS ACTIVO` cada 60 ticks (3 s)
- Aplicado en `BlockProtectionEvents` y `EntityProtectionEvents` y `EnderPearlMixin`

Flags globales:
- Persistencia: `<world>/global_flags.json` (separado de `claimblocks_data.json`)
- Implementadas mediante gamerules vanilla: `pvp` server flag, `doMobGriefing`, `doFireTick`
- Aplicadas al iniciar el servidor + en cada cambio
- Broadcast `[!] Un administrador cambió una configuración global del servidor.` a todos

Estadísticas:
- Campo nuevo `createdAt` en `Claim` (timestamp); fallback `0` para claims pre-v5.0
- "Zona más antigua" muestra fecha humana o `(legacy)`

Mensajes pendientes:
- `Map<UUID, List<Text>> pendingMessages` en memoria
- Entregados en `ServerPlayConnectionEvents.JOIN`
- Usado por `adminDelete` y transferencia cuando el dueño está offline

## Estructura nueva

```
claimblocks-mod/src/main/java/com/claimblocks/
├── ClaimBlocksMod.java               (server-side only entrypoint)
├── block/                            (sin cambios estructurales)
├── command/
│   ├── ClaimCommands.java            (✔ y acentos)
│   └── ClaimAdminCommands.java       ← NUEVO (5 subcomandos)
├── data/
│   ├── Claim.java                    (+createdAt, +7 flag fields serialization)
│   ├── ClaimFlags.java               (+7 flags + ALLOW_FLIGHT enum + isPaidOnly())
│   ├── ClaimManager.java             (+bypass set, +pendingMessages)
│   ├── ClaimTier.java                (sin cambios)
│   └── GlobalFlags.java              ← NUEVO (3 flags + persistencia + gamerules)
├── event/
│   ├── BlockProtectionEvents.java    (+chest/anvil/sign/crop, +bypass)
│   ├── EntityProtectionEvents.java   (+animal killing, +chest container, +globalPVP)
│   ├── PassiveEffectsManager.java    (+flight management cada 20t)
│   └── PlayerTracker.java            (+bypass reminder cada 60t)
├── gui/
│   ├── ClaimMenuHandler.java         (12 + 14 flags, admin-view title, +OP transfer chat)
│   ├── ClaimMenuScreen.java          (sin cambios)
│   ├── AdminPanelHandler.java        ← NUEVO (panel principal con paginación)
│   ├── AdminClaimSubMenuHandler.java ← NUEVO (3x3 útil dentro de 9x6)
│   └── AdminGlobalFlagsHandler.java  ← NUEVO (sub-menú flags globales)
├── item/                             (sin cambios)
└── mixin/
    ├── ExplosionMixin.java           (sin cambios)
    ├── FarmlandMixin.java            (sin cambios)
    └── EnderPearlMixin.java          ← NUEVO (cancela teleporte si destino en claim protegido)
```

**Eliminados:**
- `client/ClaimVisualization.java`
- `ClaimBlocksModClient.java`

## Total de flags: 26
- 16 flags v3.0
- 3 effect flags v4.0 (REGEN/RESIST/SPEED)
- 6 flags nuevas v5.0 (ANIMAL_KILLING/CHEST_ACCESS/CROP_HARVEST/ANVIL_USE/ENDER_PEARL/SIGN_EDITING)
- 1 paid-only nuevo v5.0 (ALLOW_FLIGHT)

3 flags globales server-wide (no por claim):
- `globalPVP`, `globalMobGriefing`, `globalFireSpread`

## Limitaciones conocidas

- **Sin contorno 3D**: era cliente-only. Si lo necesitan, se podría empaquetar como mod opcional separado para clientes.
- **`pendingMessages` en memoria**: si el servidor cae antes de que el dueño se conecte, el aviso se pierde.
- **Bypass se pierde al cerrar sesión**: per spec.
- **Transferencia a jugador offline**: usa `UserCache`, así que solo funciona si el jugador se ha conectado al servidor antes.
