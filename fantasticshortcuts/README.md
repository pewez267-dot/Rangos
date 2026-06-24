# Fantastic Shortcuts — Forge 1.20.1

Sistema avanzado de reemplazo y simplificación de comandos, de la familia visual de
**FantasticCrates / FantasticSpawners**. Permite a los administradores definir aliases
cortos para cualquier comando registrado, con argumentos dinámicos, integración real con
LuckPerms (sin elevación de privilegios), reemplazo del comando original en el
autocompletado y auditoría completa.

- **Mod ID:** `fantasticshortcuts` · **Forge** 1.20.1 (47.2.0) · **Java** 17
- **Build:** `./gradlew build`

## Comandos (exactamente cuatro)

| Comando | Acción |
|---|---|
| `/fshortcuts` | Abre la GUI de gestión |
| `/fshortcuts create` | Abre el editor para un shortcut nuevo |
| `/fshortcuts edit` | Abre la GUI (selecciona y pulsa Editar) |
| `/fshortcuts delete` | Abre la GUI (selecciona y pulsa Eliminar, con confirmación) |

Todos requieren nivel de permiso `2` (operador) y se validan server-side.

## GUI

`MenuType` + `AbstractContainerMenu` + `AbstractContainerScreen` (que extiende `Screen`,
por lo que conserva el estilo de la familia). Pantalla principal con búsqueda en tiempo
real, lista con scroll y botones Crear / Editar / Eliminar (confirmación). Editor por
pestañas: **General**, **Comando Original** (selector que lee el `CommandDispatcher` real),
**Shortcut** (alias + `{args}`) y **Replace**.

## Ejecución y seguridad (LuckPerms)

Los aliases **no** se registran en el dispatcher del servidor: se interceptan con el
`CommandEvent` de Forge. Cuando un jugador ejecuta un alias:

1. Se resuelve el comando original (sustituyendo `{args}`).
2. Se verifica el permiso **igual que si el jugador hubiera escrito el original**
   (`canUse` del nodo Brigadier contra el `CommandSourceStack` del propio jugador — refleja
   op-level / LuckPerms).
3. Solo si tiene permiso, se ejecuta con `performPrefixedCommand` en el **contexto del
   propio jugador**.

Nunca se ejecuta como consola ni como OP temporal; nunca se eleva, modifica ni se hace
bypass de permisos. Hay además un guard anti-recursión (alias→alias) para no provocar crash.

## Replace Original Command

Por jugador, se reenvía un `ClientboundCommandsPacket` con el árbol filtrado a sus
permisos, **quitando** los originales marcados como `replace` y **añadiendo** los aliases
(con `redirect` al original para heredar sus argumentos/sugerencias cuando se usa `{args}`).
Solo cambia lo que ve el cliente; el servidor conserva su árbol completo. Si el jugador no
tiene permiso para el original, no ve ni el original ni el alias.

> Funciona también para clientes vanilla (sin el mod): el paquete es vanilla y la ejecución
> es server-side. Solo el GUI de administración requiere el mod en el cliente.

## Persistencia, config y auditoría (`config/fantasticshortcuts/`)

- `shortcuts.json` — definiciones (Gson, escritura **asíncrona y atómica**, thread-safe).
- `config.toml` — `ForgeConfigSpec` (`extra_args_behavior`, `show_conflict_warnings`,
  `audit_enabled`, `command_cache_size`, `cache_refresh_on_reload`).
- `audit/audit.log` — eventos con timestamp ISO-8601 UTC (created/edited/deleted/used/
  denied/conflict/invalid), escritura asíncrona.

Caché en memoria `HashMap<alias, Shortcut>` para resolución O(1); se reconstruye al cargar
y en cada CRUD.

## Variables dinámicas

`{args}` en el comando original captura lo que el jugador escriba tras el alias
(`/teleport {args}` + alias `/tp` → `/tp Steve` ejecuta `/teleport Steve`). Sin `{args}`,
los argumentos extra se ignoran o se deniegan según `extra_args_behavior`.
