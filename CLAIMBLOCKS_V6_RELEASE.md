# ClaimBlocks v6.0.0 - Release Notes

## ⬇️ Descarga directa

| Archivo | Descripción |
|---|---|
| **[`claimblocks-6.0.0.jar`](./claimblocks-6.0.0.jar)** | El mod listo para soltar en `mods/` del servidor |
| [`claimblocks-6.0.0-sources.jar`](./claimblocks-6.0.0-sources.jar) | Código fuente completo |
| [`src/v6/`](./src/v6/) | Proyecto Gradle completo (para abrir en IntelliJ/VSCode) |

> **SHA-256**: `e3aed93515a12559d36dc18b4179aed4568aa9ec709a775cfcd464585ee6ca23`

## 📋 Requisitos del servidor

| Dependencia | Versión mínima |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.16.0+ |
| Java | 21+ |
| [Fabric API](https://modrinth.com/mod/fabric-api) | 0.102.0+1.21.1 |
| **[Polymer (polymer-bundled)](https://modrinth.com/mod/polymer/version/0.9.12+1.21.1)** | **0.9.12+1.21.1** |

> ⚠️ **Polymer es obligatorio en el servidor** — es lo que permite que clientes vanilla vean los bloques sin tener que instalar el mod.

> ✅ **Los clientes NO necesitan instalar nada**.

---

## 🐛 El bug de "20 registry entries unknown to this client" — RESUELTO

### Causa raíz

```
Received 20 registry entries that are unknown to this client.
namespace: claimblocks
```

`20 = 10 bloques + 10 BlockItems`. Tener `"environment": "server"` en `fabric.mod.json`
**no evita** que los IDs custom se envíen al cliente — solo evita que Fabric Loader cargue el mod en
una instancia de cliente. La sincronización de registries vive en otra capa.

### Solución

v6.0.0 integra **[Polymer](https://github.com/Patbox/polymer)**. Cada `ClaimStoneBlock` implementa
`PolymerBlock` y devuelve un bloque vanilla diferente al cliente:

| Tier | Bloque que ve el cliente |
|---|---|
| 10x10 | Cobblestone |
| 25x25 | Andesite |
| 40x40 | Polished Andesite |
| 64x64 | Smooth Stone |
| 80x80 | Polished Granite |
| 100x100 | Gold Block |
| 150x150 | Copper Block |
| 250x250 | Diamond Block |
| 300x300 | Netherite Block |
| 500x500 | Beacon |

Los items se registran como `PolymerBlockItem(reg, settings, virtualVanillaItem)`,
así que en inventario el cliente vanilla ve el item correspondiente al bloque vanilla disfrazado.

---

## 🔧 Correcciones críticas aplicadas (revisión completa de v5)

### Bugs corregidos

1. **Protección de `ClaimStone` al romperla**: antes el bloque intentaba "deshacer" el break dentro del callback `onBreak` (no funciona); ahora se cancela en `PlayerBlockBreakEvents.BEFORE`.
2. **`ClaimManager.clearClaimsOf`**: ahora solo borra el bloque del centro si todavía es un `ClaimStoneBlock`. Antes borraba cualquier bloque ahí (riesgo de pisar construcciones del dueño).
3. **`PlayerTracker.pushOutOfClaim`**: recalcula `Y` segura usando `Heightmap.MOTION_BLOCKING_NO_LEAVES` y carga el chunk antes. Ya no atasca a jugadores dentro de roca/lava.
4. **`Claim.overlapsWith`**: cambiado `<=` por `<`, permitiendo zonas adyacentes (hombro a hombro) sin conflicto.
5. **`isInteractiveBlock`**: ahora usa tags vanilla (`BlockTags.BUTTONS/DOORS/TRAPDOORS/FENCE_GATES/PRESSURE_PLATES`) en lugar de comparar `translation keys` (frágil ante mods).
6. **`isMatureCrop`**: cobertura ampliada (cualquier bloque con `AGE` al máximo, no solo wheat/carrots/potatoes/beetroot/nether-wart).
7. **`ConcurrentHashMap`** en `pending`, `lastClaim`, `lastAlert`, `bypassPlayers`: era un `HashMap` no thread-safe.
8. **Cleanup en disconnect**: `lastClaim`, `lastAlert`, `bypass` se limpian al desconectar (antes quedaban hasta el reinicio).
9. **`en_us.json`**: ahora está en inglés real (antes era una copia de `es_es.json`).

### Nuevos mixins (protecciones que faltaban)

- **`PistonHandlerMixin`**: cancela `calculatePush()` cuando el push cruza fronteras de claims protegidos (anti pistón cross-claim).
- **`DispenserBlockMixin`**: cancela `dispense()` cuando un dispenser fuera del claim apunta dentro de un claim protegido.

### Mejoras funcionales

- **`/claim transfer <jugador>`**: transfiere la propiedad de la zona donde estás parado.
- **`/claim removemember <jugador>`**: elimina un miembro de la zona.
- **Límite configurable de claims por jugador**: archivo `world/data/claimblocks_config.json` (`maxClaimsPerPlayer`, `0` = sin límite). Se valida al colocar la piedra.
- **Bypass admin con cleanup**: el bypass se desactiva al desconectar.

---

## 📝 Comandos completos (resumen)

### Jugador (cualquiera)
- *colocar piedra* → crear zona
- *click derecho en piedra* → menú GUI (solo dueño/op)

### Operador (`level 2`)
- `/claim give <jugador> <claimstone_*>`
- `/claim clear <jugador>`
- `/claim remove`
- `/claim menu`
- `/claim list`
- `/claim info`
- `/claim ban <jugador>` / `/claim unban <jugador>`
- `/claim transfer <jugador>` ← **v6**
- `/claim removemember <jugador>` ← **v6**
- `/claimadmin` → panel administrativo (sin cambios)

---

## 🔄 Migración desde v5

- **Compatible automáticamente**. El JSON de claims es retro-compatible (mismo formato).
- Las **piedras existentes** en el mundo se renderizarán automáticamente como sus bloques vanilla equivalentes en clientes vanilla. **No hay que romperlas y volver a colocarlas.**
- Si tenías el archivo `claimblocks_data.json`, se cargará tal cual. Solo se creará un nuevo `claimblocks_config.json` con `maxClaimsPerPlayer: 0`.

## ⚙️ Build desde fuente

```bash
cd src/v6
./gradlew build
# JAR final: src/v6/build/libs/claimblocks-6.0.0.jar
```

Necesita Java 21 y Gradle 8.10+ (probado con Gradle 8.14.5 y Loom 1.10.5).
