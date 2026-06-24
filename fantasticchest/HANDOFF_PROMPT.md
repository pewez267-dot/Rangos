# HANDOFF — Fantastic Chest: Corrección de bugs GUI

## Contexto del proyecto

Mod Forge 1.20.1 (Java 17). El proyecto ya compila con `./gradlew build` limpio.
**No compilar.** Entregar solo código corregido.

## Ruta del proyecto en el repo

```
Rangos/fantasticchest/
```

Rama Git: `feat/fantastic-chest-mod`
Repo: `pewez267-dot/Rangos`

## Bugs a corregir — lista exacta

---

### BUG 1 — GUI Admin: el botón "Añadir item" no funciona visualmente

**Archivo:** `src/main/java/com/fantasticchest/gui/admin/ItemsTab.java`

**Problema:** Cuando el usuario escribe una cantidad en el `EditBox` (`qty`) y hace clic en
"Añadir item", el botón llama a `s.refresh()` que a su vez llama a `rebuildWidgets()`.
Esto destruye y recrea todos los widgets. El `EditBox qty` local capturado en el lambda
del botón apunta al widget **destruido**, así que `qty.getValue()` devuelve string vacío y
`q` queda en 0. El item nunca se añade.

**Fix requerido:** Guardar el valor del campo de cantidad en un campo del screen
(`s.draftItemQty`) antes de que `rebuildWidgets()` lo destruya. El `Responder` del
`EditBox qty` debe escribir a `s.draftItemQty` en cada keystroke. El botón debe leer
`s.draftItemQty`, no `qty.getValue()`.

Lo mismo aplica al campo de búsqueda: guardar en `s.draftItemSearch` para que la búsqueda
no se borre al hacer clic en un item de la lista.

**En `ChestAdminScreen.java`** ya se añadieron los campos:
```java
public long draftItemQty = 0L;
public String draftItemSearch = "";
```

**En `ItemsTab.java`** ya se actualizó el `Responder` del qty y el botón usa `s.draftItemQty`.
Verificar que esté aplicado correctamente y que el `EditBox qty` muestre el valor de
`s.draftItemQty` al reconstruirse.

---

### BUG 2 — GUI Admin: el botón "Añadir todos" no da feedback visual claro

**Archivo:** `src/main/java/com/fantasticchest/gui/admin/ItemsTab.java`

**Problema:** Cuando el usuario hace clic en "Añadir todos", se pone `s.doBulk = true` y
se llama `s.refresh()`. Pero no hay ningún item en la lista derecha (`s.overrides` sigue
vacío), así que parece que no pasó nada. El texto de estado `"§aMasiva activa"` en
`renderLabels` no es suficientemente visible.

**Fix requerido:**
1. Cuando `s.doBulk == true`, mostrar el texto `§aMasiva activa` de forma destacada en el
   área derecha de la pestaña Items (no solo en el label pequeño arriba).
2. Opcionalmente, cambiar el color del botón "Añadir todos" a verde cuando está activo y
   añadir un botón "Cancelar masiva" que ponga `s.doBulk = false`.

---

### BUG 3 — GUI Terminal: no se puede seleccionar un item con clic derecho

**Archivo:** `src/main/java/com/fantasticchest/gui/terminal/ChestTerminalScreen.java`

**Problema raíz:** En `AbstractContainerScreen`, el método `mouseClicked` de la superclase
intercepta los clics y en muchos casos NO llega a la override del screen (especialmente
clic derecho). El código de `ChestTerminalScreen.mouseClicked` nunca se ejecuta para el
clic derecho porque `AbstractContainerScreen` lo consume antes.

**Fix requerido:** Sobreescribir `mouseReleased` en lugar de (o además de) `mouseClicked`
para el clic derecho (button == 1). O bien interceptar en `handleMouseClicked` /
`mouseDragged`. El patrón correcto en Forge 1.20.1 es:

```java
@Override
public boolean mouseClicked(double mouseX, double mouseY, int button) {
    // Handle nuestro clic ANTES de llamar super para que no lo consuma.
    if (/* nuestro área */ && button == 1) {
        // seleccionar item
        return true;
    }
    if (/* nuestro área */ && button == 0) {
        // extraer
        return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
}
```

La clave es retornar `true` ANTES de llamar `super.mouseClicked` cuando es un clic en
nuestra área de lista, de lo contrario `AbstractContainerScreen` lo consume.

---

### BUG 4 — GUI Terminal: el `EditBox` de cantidad no acepta input de teclado

**Archivo:** `src/main/java/com/fantasticchest/gui/terminal/ChestTerminalScreen.java`

**Problema:** El `amountBox` (campo de cantidad para extracción manual) no acepta input de
teclado porque `AbstractContainerScreen.keyPressed` consume las teclas antes de que lleguen
al `EditBox`. El override actual en `ChestTerminalScreen.keyPressed` llama a
`super.keyPressed` al final, pero `AbstractContainerScreen` cierra el inventario con teclas
como `e` o captura las letras.

**Fix requerido:** En `keyPressed`, verificar si `amountBox` o `searchBox` están focused y
si es así, pasarles la tecla PRIMERO y retornar `true` sin llamar `super`:

```java
@Override
public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (this.amountBox != null && this.amountBox.isFocused()) {
        if (this.amountBox.keyPressed(keyCode, scanCode, modifiers)) return true;
    }
    if (this.searchBox != null && this.searchBox.isFocused()) {
        if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) return true;
    }
    if (keyCode == 256) { onClose(); return true; } // ESC
    return super.keyPressed(keyCode, scanCode, modifiers);
}
```

También revisar `charTyped`:
```java
@Override
public boolean charTyped(char c, int modifiers) {
    if (this.amountBox != null && this.amountBox.isFocused()) {
        return this.amountBox.charTyped(c, modifiers);
    }
    if (this.searchBox != null && this.searchBox.isFocused()) {
        return this.searchBox.charTyped(c, modifiers);
    }
    return super.charTyped(c, modifiers);
}
```

---

### BUG 5 — Items de operador en la lista del admin

**Archivo:** `src/main/java/com/fantasticchest/gui/admin/ItemsTab.java`

**Problema:** La lista muestra items que solo los operadores del servidor deberían manejar:
`command_block`, `barrier`, `structure_block`, `debug_stick`, `light`, `jigsaw`, etc.

**Fix requerido:** Ya hay un filtro implementado en `allItems()` con un `Set` de items
bloqueados. Verificar que la lista `OPERATOR_ITEMS` incluya al menos:

```java
private static final Set<String> OPERATOR_ITEMS = Set.of(
    "minecraft:command_block", "minecraft:chain_command_block",
    "minecraft:repeating_command_block", "minecraft:command_block_minecart",
    "minecraft:barrier", "minecraft:debug_stick", "minecraft:light",
    "minecraft:structure_block", "minecraft:structure_void", "minecraft:jigsaw",
    "minecraft:spawner", "minecraft:moving_piston", "minecraft:piston_head",
    "minecraft:knowledge_book", "minecraft:filled_map", "minecraft:bundle"
);
```

**Importante:** Los items de MODS instalados NO se deben filtrar — solo los vanilla
listados arriba. La lógica correcta es:
```java
if ("minecraft".equals(rl.getNamespace()) && OPERATOR_ITEMS.contains(rl.toString())) continue;
```

---

### BUG 6 — GUI Admin: `keyPressed` en los `EditBox` del admin no funciona

**Archivo:** `src/main/java/com/fantasticchest/gui/admin/ChestAdminScreen.java`

**Problema:** El mismo problema que el Bug 4. `AbstractContainerScreen` consume las teclas.

**Fix requerido:** Añadir `charTyped` en `ChestAdminScreen`:

```java
@Override
public boolean charTyped(char c, int modifiers) {
    for (final EditBox box : this.editBoxes) {
        if (box.isFocused()) {
            return box.charTyped(c, modifiers);
        }
    }
    return super.charTyped(c, modifiers);
}
```

Y el `keyPressed` existente ya itera `this.editBoxes` — verificar que NO llame a
`super.keyPressed` cuando un EditBox está focused:

```java
@Override
public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) { onClose(); return true; }
    for (final EditBox box : this.editBoxes) {
        if (box.isFocused() && box.canConsumeInput()) {
            if (box.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
}
```

---

## Resumen de archivos a modificar

| Archivo | Bugs | Cambios |
|---|---|---|
| `gui/admin/ChestAdminScreen.java` | 6 | Añadir `charTyped`; corregir `keyPressed` |
| `gui/admin/ItemsTab.java` | 1, 2, 5 | `draftItemQty`/`draftItemSearch`; feedback "Añadir todos"; filtro ya aplicado |
| `gui/terminal/ChestTerminalScreen.java` | 3, 4 | `mouseClicked` antes de super; `keyPressed` + `charTyped` |

## Restricciones absolutas

- **No compilar** el proyecto (`./gradlew build`).
- No añadir comandos nuevos.
- No usar Bukkit/Spigot/Paper.
- No usar APIs que no existan en Forge 1.20.1.
- No inventar imports.
- El fix del `EditBox` es el **patrón estándar** en Forge 1.20.1 con `AbstractContainerScreen`:
  sobreescribir `charTyped` y retornar `true` antes de `super` cuando un campo está focused.

## Cómo compilar (tú, no la IA)

```
cd fantasticchest
./gradlew build
```

El jar queda en `build/libs/fantasticchest-1.0.0.jar`.
