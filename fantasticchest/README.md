# Fantastic Chest — Forge 1.20.1

Cofre de almacenamiento masivo comprimido (cantidades **Long**) de la familia visual
FantasticCrates / FantasticSpawners. Dos interfaces totalmente separadas: una de
administración (OP) y una terminal de usuario, ambas con `MenuType` +
`AbstractContainerScreen`.

- **Mod ID:** `fantasticchest` · **Forge** 1.20.1 (47.2.0) · **Java** 17
- **Build:** `./gradlew build`

## Comandos (solo tres, requieren OP nivel 4)

```
/fschest create                 abre la GUI de administracion (creacion)
/fschest delete <id>            elimina un cofre por id
/fschest editor give <jugador>  entrega la Varita del Editor
```

## Las dos interfaces

- **Interfaz 1 — Administración** (OP, con `/fschest create` o la varita sobre un cofre):
  pestañas *Items* (carga masiva por `Long` + items individuales), *General* (id único +
  nombre con color), *Seguridad* (lista de permitidos). En edición: *Items*,
  *Refrescar Stock* (atómico), *Seguridad*.
- **Interfaz 2 — Terminal** (jugador con permiso, clic derecho): nombre, buscador
  client-side, lista con icono · nombre · cantidad formateada (1,000,000 / 1.0M / 2.5B).
  Clic izq = 1, Shift+izq = 64, clic der = elegir item + cantidad manual.

> Clic derecho sobre el cofre = **siempre** la terminal, incluso para OP. La edición solo
> se abre con la varita.

## Rendimiento (reglas arquitectónicas)

- **Cero lógica por tick**: el `ChestBlockEntity` no tiene `tick()`; no hay `TickEvent`.
  Todo es event-driven.
- **BlockEntity puro**: solo datos (`ConcurrentHashMap<Item, Long>`), sin cálculos.
- **Persistencia asíncrona**: `chests.json` se escribe en un hilo dedicado (orden
  garantizado) de forma atómica; *flush* final en `ServerStoppingEvent`.
- **Memoria como fuente de verdad**: `ChestRegistry` se carga una vez al arrancar; nunca
  se lee disco durante el juego; se escribe solo ante cambios reales (flag dirty).
- **Paginación**: la terminal recibe solo `page_size` items por paquete; el scroll pide
  más bajo demanda. Nunca se envía el inventario completo de golpe.
- **Sin entidades adicionales**.

## Inventario comprimido

`ConcurrentHashMap<Item, Long>` con suma saturada a `Long.MAX_VALUE` (sin overflow) y
extracción **atómica** vía `compute`. NBT con `LongTag` y claves `ResourceLocation`.

## Seguridad (server-side)

- UUID siempre del `ServerPlayer` autenticado, nunca del payload.
- Validación de acceso (dueño o permitido) y distancia (`max_interaction_distance`) en
  cada paquete; OP (`hasPermissions(4)`) para toda la Interfaz 1.
- Extracción atómica; si el inventario está lleno, se devuelve el resto al cofre.
- ID único validado server-side; `instanceof` antes de cualquier cast.

## Persistencia y config (`config/fantasticchest/`)

- `chests.json` — definiciones (Gson, `Long` sin truncar, async + atómico).
- `config.toml` — `ForgeConfigSpec`: `default_quantity`, `hide_empty_items`,
  `require_pickup_before_delete`, `max_interaction_distance`, `compact_threshold`,
  `compact_format`, `page_size`, `async_save`.
