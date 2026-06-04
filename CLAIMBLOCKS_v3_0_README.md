# Claim Blocks 3.0.0 - Minecraft 1.21.1 Fabric

Mod de proteccion de zonas con 10 tiers de tamanos distintos, menu GUI con
2 paginas y 16 flags de proteccion, contorno de previsualizacion mientras
sostienes la piedra, comandos OP y persistencia JSON.

## Descarga directa

**[claimblocks-3.0.0.jar](https://github.com/pewez267-dot/Rangos/raw/feat/claimblocks-v3_0/claimblocks-3.0.0.jar)**

Pega el archivo en tu carpeta `mods/`. Requiere:
- Minecraft 1.21.1
- Fabric Loader >= 0.16.0
- Fabric API
- Java 21+

## 10 Bloques de Claim (radio horizontal | altura)

| ID | Nombre | Radio H | Altura +/- |
|----|--------|---------|-----------|
| `claimstone_10x10`   | Piedra de Claim 10x10   | 10  | 15  |
| `claimstone_25x25`   | Piedra de Claim 25x25   | 25  | 20  |
| `claimstone_40x40`   | Piedra de Claim 40x40   | 40  | 30  |
| `claimstone_64x64`   | Piedra de Claim 64x64   | 64  | 40  |
| `claimstone_80x80`   | Piedra de Claim 80x80   | 80  | 50  |
| `claimstone_100x100` | Piedra de Claim 100x100 | 100 | 60  |
| `claimstone_150x150` | Piedra de Claim 150x150 | 150 | 80  |
| `claimstone_250x250` | Piedra de Claim 250x250 | 250 | 100 |
| `claimstone_300x300` | Piedra de Claim 300x300 | 300 | 120 |
| `claimstone_500x500` | Piedra de Claim 500x500 | 500 | 150 |

Ejemplo: `claimstone_100x100` colocado en X=200,Y=64,Z=200 protege
X=100..300, Y=4..124, Z=100..300.

## Comandos (OP nivel 2)

| Comando | Descripcion |
|---------|-------------|
| `/claim give <jugador> <id>` | Da una piedra (autocompletado de IDs) |
| `/claim clear <jugador>` | Borra todas las zonas del jugador |
| `/claim remove` | Elimina la zona donde estas |
| `/claim menu` | Abre el menu (solo dueno) |
| `/claim list` | Lista tus zonas |
| `/claim info` | Detalles de la zona donde estas |
| `/claim ban <jugador>` | Banea al jugador de la zona |
| `/claim unban <jugador>` | Quita el ban |

Ejemplo: `/claim give Steve claimstone_100x100`

## Las 16 flags

Pagina 1 del menu:
1. **Bloquear construccion** - Intrusos no colocan bloques
2. **Bloquear destruccion** - Intrusos no rompen bloques
3. **Bloquear explosiones** - TNT/creepers no destruyen
4. **Bloquear fuego** - El fuego se apaga aqui
5. **Bloquear spawn mobs** - No spawnean mobs hostiles
6. **PVP entre jugadores** - Jugadores no se atacan
7. **Bloquear dano de mobs** - Mobs no danan jugadores
8. **Alertas de intrusos** - Avisar al entrar intrusos
9. **Modo publico (visita)** - Visitantes no modifican

Pagina 2:
10. **Bloquear uso de items** - Intrusos no usan items
11. **Interac. entidades** - Intrusos no usan mobs
12. **Pisar cultivos** - Intrusos no destruyen tierra
13. **Bloquear fluidos** - No se coloca agua/lava
14. **PVP contra todos** - Cualquiera puede atacar
15. **Talar arboles** - Intrusos no talan logs
16. **Bienvenida personaliz.** - Mensaje custom al entrar (clic izq edita, clic der toggle)

## Visualizacion

**Solo cuando** sostienes una piedra de claim en la mano principal Y miras
a un bloque, se dibuja el contorno de donde quedaria la zona si la
colocaras ahi. En cualquier otro caso (incluso parado dentro de un claim
existente) NO se dibuja nada.

Color del contorno por bloque (mismo color que la textura).

## Persistencia y migracion

Los claims se guardan en `<mundo>/claimblocks_data.json`. El formato
ahora usa `radius` y `height` en vez de `tier`:

```json
{
  "claims": [{
    "claimId": "uuid",
    "ownerUUID": "uuid",
    "ownerName": "Steve",
    "tierId": "claimstone_100x100",
    "radius": 100,
    "height": 60,
    "world": "minecraft:overworld",
    "x": 200, "y": 64, "z": 200,
    "members": ["uuid"],
    "memberNames": ["Alex"],
    "bannedPlayers": ["uuid"],
    "flags": {
      "blockBuilding": true,
      "blockBreaking": true,
      "blockExplosions": true,
      "blockFire": true,
      "blockMobSpawn": false,
      "blockPVP": true,
      "blockMobDamage": false,
      "trespasserAlerts": false,
      "blockItemUse": true,
      "blockEntityInteract": true,
      "blockTrampling": true,
      "blockFluids": true,
      "pvpAll": false,
      "blockTreeChopping": true,
      "publicMode": false,
      "showWelcome": false,
      "welcomeMessage": ""
    }
  }]
}
```

**Migracion automatica**: si encuentra claims viejos con campo `tier`
(numero 1-5), los convierte a `radius`/`height` la primera vez que se
cargan y los persiste asi:

| tier viejo | radius | height |
|-----------|--------|--------|
| 1 | 10 | 15 |
| 2 | 25 | 20 |
| 3 | 40 | 30 |
| 4 | 64 | 40 |
| 5 | 80 | 50 |

## Reglas de texto aplicadas

- Sin emojis Unicode (solo ASCII basico extendido).
- Prefijos: `[ON]`, `[OFF]`, `[OK]`, `[!]`, `[x]`, `[Claim]`.
- Nombres de items <= 30 chars.
- Lore de items <= 2 lineas de 35 chars.
- Titulo del menu <= 40 chars.
- Mensajes de chat / ActionBar <= 60 chars.

## Mixins activos

- `ExplosionMixin` (Explosion.collectBlocksAndDamageEntities) - filtra los
  bloques afectados que esten dentro de claims con `blockExplosions`.
- `FarmlandMixin` (FarmlandBlock.onLandedUpon) - cancela el trampling para
  visitantes en claims con `blockTrampling` o `publicMode`.

## Compilar

```
JAVA_HOME=/path/to/jdk-21 gradle -p claimblocks-mod build
```

Salida en `claimblocks-mod/build/libs/claimblocks-3.0.0.jar`.
