# FSpawner — Fantastic Spawner for Forge 1.20.1

**Autor:** Pewez
**Versión:** 2.0.3
**Minecraft:** 1.20.1
**Forge:** 47.x

Sistema avanzado de spawners que extiende el spawner vanilla con GUI completa, integración con Infernal Mobs y soporte universal para entidades e items de cualquier mod.

## Novedades de la 2.0.3

- ❌ **Eliminado** el comando `/fspawner edit` (y el ejecutor base `/fspawner`).
- ✨ **Nuevo** comando `/fspawner editor give` → te entrega la **Varita del Editor de Spawners**.
- 🪄 **Varita del Editor de Spawners**: haz **click derecho** sobre cualquier spawner colocado
  (vanilla o Fantastic Spawner) y se abre el editor de **ese** spawner concreto. Al guardar,
  los cambios se aplican directamente en el bloque.
- La varita tiene textura propia y brillo de encantamiento. Requiere permiso de OP (nivel 2)
  para abrir el editor, igual que el resto de comandos del mod.

Comandos disponibles (todos requieren permiso nivel 2):

| Comando | Acción |
|---|---|
| `/fspawner editor give` | Te entrega la Varita del Editor de Spawners |
| `/fspawner editor give <jugador>` | Entrega la varita a uno o varios jugadores |
| `/fspawner create` | Abre el editor para crear un nuevo Fantastic Spawner (item) |
| `/fspawner pickup` | Recoge el spawner que estás mirando como item |
| `/fspawner save <nombre>` | Guarda la config del spawner en mano como preset |
| `/fspawner load <nombre>` | Carga un preset y te da el item |
| `/fspawner delete <nombre>` | Elimina un preset |

## Compilar

> Este proyecto usa **mappings oficiales de Mojang** (`mappings channel: 'official'`),
> por lo que el código está en nombres legibles de Mojang (`getClickedPos`, `useOn`, …),
> no en SRG.

```bash
# Linux/Mac
./gradlew build

# Windows
gradlew.bat build
```

El JAR resultante quedará en `build/libs/fspawner-2.0.3.jar`.

Requisitos: JDK 17.

### Setup IDE
```bash
./gradlew genEclipseRuns    # Eclipse
./gradlew genIntellijRuns   # IntelliJ IDEA
```

## Archivos nuevos / modificados respecto a la 2.0.2

- `command/FSpawnerCommand.java` — quitado `edit`/ejecutor base; añadido `editor give`.
- `item/FSItems.java` — **nuevo**: registro `DeferredRegister<Item>` con `spawner_wand`.
- `item/SpawnerWandItem.java` — **nuevo**: lógica de la varita (`useOn`).
- `FSpawner.java` — registra `FSItems` en el mod bus.
- `assets/fspawner/models/item/spawner_wand.json` — **nuevo**: modelo del item.
- `assets/fspawner/textures/item/spawner_wand.png` — **nueva**: textura de la varita.
- `assets/fspawner/lang/en_us.json`, `es_es.json` — textos de la varita y el comando.

## Dependencias opcionales

- **Infernal Mobs** — integración opcional, el mod funciona sin ella.
