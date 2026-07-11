# Contexto de Mods — Sandbox Rangos

Referencia de todos los mods hechos/trabajados y sus rutas exactas en este sandbox, para pasar contexto a otra IA.

- **Repo:** `pewez267-dot/Rangos`
- **Branch de trabajo:** `fantasticranks-1.1.0-es`
- **Raíz de proyectos de build (sandbox):** `/projects/sandbox/cbwork/`
- **Jars finales entregados (sandbox):** `/projects/sandbox/Rangos/`
- **Link directo a un jar:** `https://github.com/pewez267-dot/Rangos/raw/fantasticranks-1.1.0-es/<archivo>`

---

## Reglas (ÚNICAS instrucciones)

1. **Compilar con las herramientas/entorno local ya cacheado y pushear con las herramientas de git.** No re-clonar ni reconfigurar de cero.
2. **No descargar nada pesado** (ni dependencias externas grandes ni Minecraft/Forge desde fuera): el entorno de ForgeGradle ya está cacheado en el sandbox. Compilar sin descargas externas.

---

## Mods hechos/modificados (última versión)

| Mod | MC / Loader | Proyecto de build (sandbox) | Fuente | Jar final (sandbox) | Jar en repo |
|---|---|---|---|---|---|
| FantasticMobs (nuevo, completo) | Forge 1.20.1 | `/projects/sandbox/cbwork/mobmod/` | `mobmod/src/main/java/com/fsmobs/` | `/projects/sandbox/Rangos/fantasticmobs-1.0.3.jar` | `fantasticmobs-1.0.3.jar` |
| FantasticRecipes (nuevo, completo) | Forge 1.20.1 | `/projects/sandbox/cbwork/recipemod/` | `recipemod/src/main/java/com/fsrecipes/` | `/projects/sandbox/Rangos/fantasticrecipes-1.0.2.jar` | `fantasticrecipes-1.0.2.jar` |
| FantasticRanks (edición incremental) | Forge 1.20.1 | `/projects/sandbox/cbwork/rankswipemod/` | `rankswipemod/src/main/java/com/fantasticranks/` | `/projects/sandbox/Rangos/fantasticranks-1.2.5.jar` | `fantasticranks-1.2.5.jar` |
| FantasticNametags (fix sombra) | Forge 1.20.1 | `/projects/sandbox/cbwork/nametagmod/` | `nametagmod/src/main/java/com/fantasticnametags/` | `/projects/sandbox/Rangos/fantasticnametags-1.0.3.jar` | `fantasticnametags-1.0.3.jar` |
| NoNameTags (mixin nuevo, cliente) | Forge 1.20.1 | `/projects/sandbox/cbwork/nnt/` | `nnt/src/main/java/no/name/tags/mixin/client/` | `/projects/sandbox/Rangos/norendernametags-1.20-1.20.1-forge-1.1.jar` | `norendernametags-1.20-1.20.1-forge-1.1.jar` |
| FantasticPass (fix nametag, revertido) | Forge 1.20.1 | `/projects/sandbox/cbwork/passtag/` | `passtag/src/main/java/com/fantasticpass/nametag/` | (revertido) `fantasticpass-1.3.1.jar` | `fantasticpass-1.3.1.jar` |

## Proyectos de sesiones anteriores (mismo sandbox)

| Mod | Proyecto de build (sandbox) | Paquete fuente |
|---|---|---|
| FantasticRanks (fuente completa) | `/projects/sandbox/cbwork/ranksmod/` | `com/fantasticranks/` |
| FantasticNametags (fuente completa) | `/projects/sandbox/cbwork/ntmod/` | `com/fantasticnametags/` |
| FantasticPass (fuente completa) | `/projects/sandbox/cbwork/passmod/` | `com/fantasticpass/` |
| FantasticPass (sonidos) | `/projects/sandbox/cbwork/fpsndmod/` | `com/fantasticpass/` |
| FantasticPass (GUI admin) | `/projects/sandbox/cbwork/fpmod/` | `com/fantasticpass/` |
| FantasticShop | `/projects/sandbox/cbwork/fshopmod/` | `com/fshop/` |
| FantasticHolograms | `/projects/sandbox/cbwork/holomod/` | `com/fsholo/` |
| FantasticSpawner | `/projects/sandbox/cbwork/spawnermod/` | `com/fspawner/` |
| ClaimBlocks (Fabric 1.21.1) | `/projects/sandbox/cbwork/cbmod/` | `com/claimblocks/` |
| BetterPickaxeTrims | `/projects/sandbox/cbwork/pickmod/` | (paquete del mod) |

## Decompilados de referencia (solo lectura)

- `/projects/sandbox/cbwork/fp-decomp/` — FantasticPass
- `/projects/sandbox/cbwork/fshop-decomp/` — FantasticShop
- `/projects/sandbox/cbwork/spawner-decomp/` — FantasticSpawner
- `/projects/sandbox/cbwork/holo-decomp/` — FantasticHolograms
- `/projects/sandbox/cbwork/norender/` — NoNameTags (clases + jsons de mixin)
- `/projects/sandbox/cbwork/cfr.jar` — decompilador CFR

---

## Notas técnicas por mod (comportamiento, no instrucciones)

- **FantasticMobs 1.0.3:** control de cantidad de mobs por radio (topes por categoría y por mob) + multiplicador de aparición. Bloquea con `MobSpawnEvent.PositionCheck` + `setResult(DENY)` (NO `FinalizeSpawn`, que el NaturalSpawner de 1.20.1 ignora). Tope 0 = deniega siempre; vacío/∞ = salida temprana coste-cero. GUI con pestañas Límites y Estadísticas + panel en pantalla (`/fsmobs stats`). Config JSON, solo OP.
- **FantasticRecipes 1.0.2:** banea/desbanea recetas. Quita del `RecipeManager` las recetas de salida baneada (`replaceRecipes`) y reenvía la lista filtrada a los clientes (`ClientboundUpdateRecipesPacket`); reaplica en cada `/reload` con un reload listener. Sin mixins. Config JSON, solo OP. `/fsrecipes`.
- **FantasticRanks 1.2.5:** rangos por tiempo persistentes vía instantánea `earnedDescriptor`; sobreviven al borrado del paquete; solo `/fsranks wipe` los quita.
- **FantasticNametags 1.0.3:** arregla que la sombra del suelo subiera con el nombre; revierte el offset en `RenderLivingEvent.Post` (después del nombre, antes de la sombra).
- **NoNameTags 1.1:** oculta también el tag del pase; `PlayerRendererMixin` cancela `PlayerRenderer.renderNameTag` en HEAD cuando `hideNameTags` está activo. 100% cliente.
- **Regla del proyecto:** el `mods.toml` de los mods propios NO debe llevar la URL del repo. Ningún mod usa bloques de comando.
