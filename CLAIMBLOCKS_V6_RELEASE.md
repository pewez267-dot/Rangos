# ClaimBlocks v6.0.0 - Release Notes

## ⬇️ Descarga directa

| Archivo | Descripción |
|---|---|
| **[`claimblocks-6.0.0.jar`](./claimblocks-6.0.0.jar)** | El mod listo para soltar en `mods/` del servidor |
| [`claimblocks-6.0.0-sources.jar`](./claimblocks-6.0.0-sources.jar) | Código fuente completo |
| [`src/v6/`](./src/v6/) | Proyecto Gradle completo (para abrir en IntelliJ/VSCode) |

> **SHA-256**: `be02b17d109a3ca10243ebf83c1bf7bad65d7967fe3016c997a6a1c2e5edd797`

---

## 🟢 100% server-side, 100% vanilla

A diferencia de la primera v6 (que usaba Polymer), esta versión **no registra ningún bloque ni item custom**. Las "Piedras de Claim" son simplemente bloques de **concreto vanilla de colores**, identificados por un NBT marker (`custom_data`).

### ✅ Resuelve definitivamente el error de "registry entries unknown"

```
Received 20 registry entries that are unknown to this client.
namespace: claimblocks
```

Ya no se registra **nada** en el namespace `claimblocks` (ni bloques ni items). El cliente vanilla no recibe ningún ID custom y se conecta sin error. **Sin Polymer, sin librerías extra, sin resourcepacks.**

### 🎨 Mapeo de tiers a concretos

| Tier | Color de concreto |
|---|---|
| 10x10 | White Concrete |
| 25x25 | Light Gray Concrete |
| 40x40 | Cyan Concrete |
| 64x64 | Light Blue Concrete |
| 80x80 | Lime Concrete |
| 100x100 | Yellow Concrete |
| 150x150 | Orange Concrete |
| 250x250 | Pink Concrete |
| 300x300 | Magenta Concrete |
| 500x500 | Purple Concrete |

---

## 📋 Requisitos

| Dependencia | Versión |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.16.0+ |
| Java | 21+ |
| [Fabric API](https://modrinth.com/mod/fabric-api) | 0.102.0+1.21.1 |

> ✅ **El cliente NO necesita instalar nada**. Solo Minecraft 1.21.1 vanilla.

---

## 🔧 Cómo funciona la "Piedra de Claim"

1. Un OP ejecuta `/claim give <jugador> <claimstone_25x25>`.
2. El jugador recibe un **`ItemStack` de Light Gray Concrete** con:
   - `display.Name` = "Piedra de Claim 25x25" (color amarillo)
   - `lore` = info del tier
   - `custom_data` = `{claimblocks: {tier: "claimstone_25x25"}}` ← marcador NBT
3. Al click derecho con el item:
   - `UseBlockCallback` lee el NBT, identifica el tier
   - Valida overlap, límite por jugador, permisos
   - Coloca el concreto vanilla y registra el claim
4. Al click derecho sobre el bloque-centro de un claim existente:
   - Si eres dueño u OP, abre el menú GUI de gestión.
5. Al romper el bloque-centro:
   - Si eres dueño u OP, elimina el claim y devuelve un nuevo item con NBT.
   - Si no, cancela el break.

> **Nota**: si rompes manualmente el concreto y lo recoges, vuelve a ser un concreto normal sin NBT. El NBT solo lo regenera el mod cuando elimina un claim oficialmente.

---

## 🐛 El bug de "20 registry entries unknown to this client" — RESUELTO

### Causa

`20 = 10 bloques + 10 BlockItems`. Tener `"environment": "server"` en `fabric.mod.json` **NO impide** que los IDs custom se envíen al cliente — solo evita que Fabric Loader cargue el mod en una instancia cliente. La sincronización de registries vive en otra capa.

### Solución v6 (definitiva)

**No registrar nada custom.** Usar concretos vanilla + `class_9279` (NbtComponent / `CUSTOM_DATA` data component) en el ItemStack para identificar a nuestros items. El servidor mantiene toda la metadata de claims en su `claimblocks_data.json`, mapeando posición de bloque → claim.

---

## 🔧 Correcciones críticas aplicadas (revisión completa de v5)

### Bugs corregidos
1. **Protección de break del centro** ahora se cancela en `PlayerBlockBreakEvents.BEFORE` (en v5 intentaba "deshacer" el break dentro del callback `onBreak`, no funciona).
2. **`clearClaimsOf` seguro**: solo borra el bloque del centro si todavía es el concreto correcto del tier (no pisa otras construcciones del dueño).
3. **`pushOutOfClaim` Y-safe**: recalcula la `Y` con `Heightmap.MOTION_BLOCKING_NO_LEAVES` y carga el chunk antes. Ya no atasca jugadores en piedra/lava.
4. **`Claim.overlapsWith`**: `<=` → `<`, permite zonas adyacentes hombro a hombro.
5. **`isInteractiveBlock`**: usa `BlockTags.BUTTONS/DOORS/TRAPDOORS/FENCE_GATES/PRESSURE_PLATES` (antes comparaba `translation keys`, frágil).
6. **`isMatureCrop`**: cobertura ampliada (cualquier bloque con propiedad `AGE` al máximo).
7. **`ConcurrentHashMap`** en `pending`, `lastClaim`, `lastAlert`, `bypass`.
8. **Cleanup en `DISCONNECT`**: `lastClaim`, `lastAlert`, `bypass` ya no quedan colgados.
9. **`en_us.json`**: traducido a inglés real.

## 🛡️ Nuevos mixins anti-cross-claim

- **`PistonHandlerMixin`**: cancela `calculatePush()` cuando el push cruza fronteras de claims protegidos.
- **`DispenserBlockMixin`**: cancela `dispense()` cuando un dispenser apunta dentro de un claim ajeno.

## ✨ Nuevas funcionalidades

- `/claim transfer <jugador>` — transferir propiedad de la zona donde estás parado.
- `/claim removemember <jugador>` — eliminar miembro.
- **Límite por jugador** configurable: archivo `world/data/claimblocks_config.json` → `maxClaimsPerPlayer` (`0` = sin límite). Validado al colocar.
- **Bypass admin con cleanup automático** en disconnect.

---

## 📝 Comandos completos

### Operador (`level 2`)
- `/claim give <jugador> <tier>` — da un concreto-piedra con NBT (tier = `claimstone_10x10`, `claimstone_25x25`, etc.)
- `/claim clear <jugador>` — borra todas las zonas de un jugador
- `/claim remove` — borra la zona donde estás parado
- `/claim menu` — abre el menú de la zona donde estás parado
- `/claim list` — lista zonas
- `/claim info` — info de la zona donde estás
- `/claim ban <jugador>` / `/claim unban <jugador>`
- `/claim transfer <jugador>` ← **v6**
- `/claim removemember <jugador>` ← **v6**
- `/claimadmin` — panel administrativo

### Jugador
- *Click derecho con piedra en mano* → coloca claim
- *Click derecho sobre piedra de claim* → menú GUI (solo dueño/OP)

---

## 🔄 Migración desde v5

⚠️ **Migración requerida si tenías la v5 con bloques `claimblocks:claimstone_*`**:

Si ya colocaste piedras de la v5 en tu mundo, esos bloques **dejarán de existir** al actualizar (porque sus IDs custom ya no se registran). Los datos de claims en `claimblocks_data.json` siguen siendo válidos, pero los bloques en sí desaparecerán.

**Opciones**:
- **A**: Antes de actualizar a v6, ejecuta `/claim clear <jugador>` para cada dueño y devuélveles las piedras nuevas con `/claim give`.
- **B**: Acepta que las piedras viejas desaparezcan (los claims persistirán en JSON, pero sin bloque-centro). Después puedes regenerar los bloques con un comando admin (no incluido — fácil de añadir si lo necesitas).
- **C**: Si tu servidor está vacío, simplemente arranca con v6 y empieza limpio.

El JSON de claims de v5 y v6 es compatible: no se pierden datos.

## ⚙️ Build desde fuente

```bash
cd src/v6
./gradlew build
# JAR final: src/v6/build/libs/claimblocks-6.0.0.jar
```

Necesita Java 21 y Gradle 8.10+ (probado con Gradle 8.14.5 + Loom 1.10.5).
