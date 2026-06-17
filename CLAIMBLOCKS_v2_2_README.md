# Claim Blocks 2.2.0 — Minecraft 1.21.1 Fabric

Mod de protección de zonas con 5 tiers, menú GUI completo, 8 flags de
protección, comandos OP, persistencia JSON, contornos 3D en el mundo y
mensajes de entrada/salida con sonido. Todos los textos en **español**.

## Descarga directa

**[claimblocks-2.2.0.jar](https://github.com/pewez267-dot/Rangos/raw/feat/claimblocks-v2_2/claimblocks-2.2.0.jar)**

Pega el archivo en tu carpeta `mods/`. Requiere:
- Minecraft **1.21.1**
- Fabric Loader **>= 0.16.0**
- Fabric API
- Java **21+**

## Tabla de tiers

| Tier | Radio | Cubo protegido |
|------|-------|----------------|
| 1    | 10    | 21×21×21 |
| 2    | 20    | 41×41×41 |
| 3    | 30    | 61×61×61 |
| 4    | 40    | 81×81×81 |
| 5    | 50    | 101×101×101 |

## Comandos (todos requieren OP nivel 2)

| Comando | Descripción |
|---------|-------------|
| `/claim give <jugador> <tier>` | Da una piedra de claim del tier indicado |
| `/claim clear <jugador>` | Borra todas las zonas del jugador |
| `/claim remove` | Elimina la zona donde estás (devuelve la piedra) |
| `/claim menu` | Abre el menú GUI de la zona donde estás (solo dueño) |
| `/claim list` | Lista tus zonas con coords y mundo |
| `/claim info` | Detalles completos de la zona donde estás |
| `/claim ban <jugador>` | Banea al jugador de la zona actual (solo dueño) |
| `/claim unban <jugador>` | Quita el ban del jugador en la zona actual |

## Menú GUI (cofre virtual de 6×9)

- **Slot 4**: Papel — título "📦 Administrar Zona — Tier N de [dueño]"
- **Slots 11/13/15/17**: Información (mapa, libro, estrella nether, reloj)
- **Slots 19-25 + 28**: 8 flags toggle (verde ON / rojo OFF)
- **Slot 38**: Ver miembros — abre lista en chat
- **Slot 42**: ➕ Añadir miembro — cierra GUI y pide nombre por chat
- **Slot 46**: 🗑️ ELIMINAR ZONA — Shift+Click 2 veces para confirmar
- **Slot 49**: ❌ Cerrar
- **Slot 52**: 📋 Ejecuta `/claim list`

## Las 8 flags

| Flag | `true` significa | Implementación |
|------|------------------|----------------|
| `blockBuilding` | Otros NO pueden colocar bloques | `UseBlockCallback` |
| `blockBreaking` | Otros NO pueden romper bloques | `PlayerBlockBreakEvents.BEFORE` + `AttackBlockCallback` |
| `blockExplosions` | Las explosiones no destruyen bloques | Mixin en `Explosion.collectBlocksAndDamageEntities` |
| `blockFire` | El fuego se extingue en la zona | Tick-sweep cada 2s alrededor de jugadores |
| `blockMobSpawn` | Mobs hostiles no spawnean | `ServerEntityEvents.ENTITY_LOAD` |
| `blockPVP` | Jugadores no se atacan | `ServerLivingEntityEvents.ALLOW_DAMAGE` |
| `blockMobDamage` | Mobs no dañan jugadores | `ServerLivingEntityEvents.ALLOW_DAMAGE` |
| `trespasserAlerts` | Avisar al dueño cuando entra un intruso | `PlayerTracker` (cooldown 30s) |

**Siempre protegidos** (sin importar las flags): cofres, hornos, dispensadores,
cajas de shulker, vagonetas con cofre, burros con cofre, botes con cofre.

## Mensajes de entrada/salida

Cuando un jugador cruza la frontera de una zona:
- **Entrar**: ActionBar §a✦ Entrando a la zona de [dueño] (Tier N) + sonido `BLOCK_NOTE_BLOCK_CHIME`
- **Salir**: ActionBar §c✦ Saliendo de la zona de [dueño] + sonido `BLOCK_NOTE_BLOCK_BASS`
- **Baneado**: teleport fuera de la zona + mensaje en chat

## Contornos 3D (cliente)

- Si tienes una piedra de claim en la mano principal y miras a un bloque, se
  dibuja el contorno semitransparente del cubo que protegería la piedra al
  colocarla allí.
- Si estás dentro de una zona, se dibuja el contorno de esa zona.
  El servidor sincroniza la zona activa al cliente vía un paquete custom.

Colores por tier: 1=azul claro, 2=verde, 3=dorado, 4=naranja, 5=rojo.

## Persistencia

Los claims se guardan en JSON en
`<directorio del mundo>/claimblocks_data.json`:

```json
{
  "claims": [{
    "claimId": "uuid",
    "ownerUUID": "uuid",
    "ownerName": "string",
    "tier": 1,
    "world": "minecraft:overworld",
    "x": 100, "y": 64, "z": 100,
    "members": ["uuid"],
    "memberNames": ["nombre"],
    "bannedPlayers": ["uuid"],
    "flags": {
      "blockBuilding": true,
      "blockBreaking": true,
      "blockExplosions": true,
      "blockFire": true,
      "blockMobSpawn": false,
      "blockPVP": true,
      "blockMobDamage": false,
      "trespasserAlerts": false
    }
  }]
}
```

Se guarda automáticamente al crear, eliminar, cambiar flag, añadir/quitar
miembro, banear/desbanear, y al apagar el servidor.

## Texturas

Las 5 texturas son PNGs 16×16 generados de forma programática
(`scripts/GenerateTextures.java`) con `BufferedImage`: relleno sólido, borde
negro, brillo lateral y un dígito centrado pixel-art. Sin ruido ni distorsión.

## Compilar el proyecto

```
JAVA_HOME=/path/to/jdk-21 gradle -p claimblocks-mod build
```

Salida en `claimblocks-mod/build/libs/claimblocks-2.2.0.jar`.

## Estructura

```
claimblocks-mod/src/main/
├── java/com/claimblocks/
│   ├── ClaimBlocksMod.java
│   ├── ClaimBlocksModClient.java
│   ├── block/        ClaimBlock, ModBlocks
│   ├── command/      ClaimCommands
│   ├── data/         Claim, ClaimFlags, ClaimManager
│   ├── event/        BlockProtectionEvents, EntityProtectionEvents, PlayerTracker
│   ├── gui/          ClaimMenuHandler, ClaimMenuScreen
│   ├── client/       ClaimVisualization
│   ├── network/      ClaimNetworking (paquete custom servidor->cliente)
│   ├── mixin/        ExplosionMixin
│   └── item/         ModItems
└── resources/
    ├── fabric.mod.json
    ├── claimblocks.mixins.json
    ├── assets/claimblocks/  (icon, 5 blockstates, 5 block models, 5 item models, 5 textures, lang es/en)
    └── data/claimblocks/loot_table/blocks/  (5 drops)
```
