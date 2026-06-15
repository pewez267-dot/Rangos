# Fantastic Kits — Forge 1.20.1

Sistema de kits por rango para Forge 1.20.1, parte de la familia visual de
**FantasticCrates** y **FantasticSpawners**: GUI por pestañas dentro de una única
pantalla, integración con **LuckPerms** en tiempo real, reclamo único por jugador
(UUID), editor de NBT completo desde la interfaz, comandos asociados por grupo y
auditoría.

- **Mod ID:** `fantastickits`
- **Plataforma:** Forge 1.20.1 (`47.2.0`)
- **Java:** 17
- **Build:** `./gradlew build`

## Comandos

Exactamente cinco subcomandos bajo `/fkits` (validación server-side en todos):

| Comando | Quién | Qué hace |
|---|---|---|
| `/fkits create <id>` | admin | Crea un kit y abre el editor |
| `/fkits edit <kit>` | admin | Abre el editor de un kit existente |
| `/fkits delete <kit>` | admin | Elimina un kit |
| `/fkits get <kit>` | jugador | Reclama tu kit (1 vez por jugador, según tu grupo) |
| `/fkits get <jugador> <kit>` | admin | Entrega/repone un kit manualmente (sin gastar el reclamo) |
| `/fkits test <kit>` | admin | Recibe el kit para probarlo, sin registrar reclamo |

> `/fkits get` es dual a propósito: con **un** argumento es el reclamo del propio
> jugador; con **dos** (`<jugador> <kit>`) es la reposición administrativa que pide la
> especificación. `/fkits` sin argumentos muestra la ayuda.

## GUI

Una sola pantalla con navegación por pestañas, al estilo de la familia Fantastic:

- **Editor de kit** — pestañas *Info*, *Grupo*, *Items*, *Comandos*.
  - *Grupo*: lista de grupos de LuckPerms leída en vivo; se asigna exactamente uno.
  - *Items*: buscador de items, lista del kit, cantidad y botón **Editar NBT**.
  - *Comandos*: buscador con filtro por origen (Todos / Vanilla / Mods), selección
    múltiple por toggle y lista de comandos ya asignados al grupo.
- **Editor de NBT** — pestañas *General* (nombre + color, CustomModelData, irrompible,
  daño), *Flags* (cada `HideFlag` vanilla por separado), *Lore* (multilínea con color
  por línea), *Encantamientos* (selector + nivel) y *Atributos* (tipo, cantidad,
  operación, slot), con **vista previa en vivo** del item en un slot dedicado.

> **Nota de arquitectura.** La especificación menciona `MenuType` /
> `AbstractContainerMenu`. La familia de referencia (FantasticCrates /
> FantasticSpawners) construye sus GUIs como `Screen` de cliente abiertas por un paquete
> del servidor, con todo el estado validado y persistido en el servidor. Para mantener
> la *misma familia visual* (requisito vinculante) y garantizar un build limpio, este
> mod sigue ese mismo patrón `Screen` + `SimpleChannel`. El "slot" de vista previa se
> dibuja con `GuiGraphics#renderItem`. El cliente nunca es fuente de verdad.

## Integración con LuckPerms

LuckPerms es una dependencia **opcional** (soft). Con LuckPerms presente:

- Al abrir el editor, el servidor lee `getGroupManager().getLoadedGroups()` y envía la
  lista al cliente (nunca se cachean ni se *hardcodean* grupos).
- El reclamo comprueba la pertenencia con
  `User#getInheritedGroups(QueryOptions)` (respeta herencia).

Sin LuckPerms, el editor sigue funcionando y se puede escribir el grupo manualmente; el
reclamo con grupo se deniega de forma segura (el servidor no puede verificar pertenencia).

## Comandos por grupo

Los kits **no** ejecutan comandos. La pestaña *Comandos* asocia comandos al **grupo** del
kit (`group_commands.json`). En cada ejecución, `CommandGuard` (vía `CommandEvent`):

- Ignora comandos que no estén asociados a ningún grupo.
- Para un comando "gateado", solo lo permite si el jugador pertenece a algún grupo que lo
  incluya; en caso contrario lo cancela. Así un rango inferior nunca usa comandos de uno
  superior. Consola y bloques de comandos no se filtran; opcionalmente los admins tampoco.

## Persistencia (`config/fantastickits/`)

| Archivo | Contenido |
|---|---|
| `kits.json` | Definición de kits (items con NBT como SNBT, grupo asignado) — Gson |
| `players.json` | Reclamos por UUID |
| `group_commands.json` | Grupo → comandos permitidos |
| `audit.log` | Auditoría append-only con timestamp ISO-8601 |
| `config.toml` | Configuración general (Forge Config API) |

Las escrituras JSON son atómicas (archivo temporal + move) y todas las operaciones de
escritura están sincronizadas. El reclamo usa un *check-and-set* atómico
(`PlayerClaimStore#tryClaim`) que evita duplicaciones por *race conditions* o paquetes
repetidos.

## Configuración (`config.toml`)

`general`: `enableAuditLog`, `broadcastOnClaim`, `maxItemsPerKit`.
`security`: `enableCommandGating`, `opsBypassCommandGating`, `allowClaimWithoutGroup`,
`adminPermissionLevel`.

## Auditoría

Se registran: creación, edición y eliminación de kit; reclamo exitoso; reclamo denegado
(motivo: ya reclamado / grupo incorrecto / kit inexistente / sin grupo); uso de comando
permitido; intento de comando bloqueado; entrega administrativa y prueba.
