# ClaimBlocks Mod - Bug Fixes Documentation
## Minecraft 1.21.1 Fabric - Java 21

### Overview
Este documento detalla las correcciones aplicadas al mod ClaimBlocks para solucionar 4 bugs críticos.

---

## BUG 1: Item Fantasma / No se consume visualmente

### Descripción del Bug
Cuando un jugador coloca un claim block, el item se decrementa en memoria pero nunca se sincroniza con el cliente, causando que el jugador siga viendo el item en su mano.

### Ubicación del Bug
**Archivo**: `CBEventHandler.java` (o `.kt`)  
**Método**: `register$lambda$0` (lambda del evento `UseBlockCallback.EVENT`)

### Código Original (BUGGY)
```java
if (!((class_3222)player).method_7337()) {  // isCreative()
    stackInHand.method_7934(1);              // decrement(1)
}
return class_1269.field_5814;  // ActionResult.FAIL
```

### Código Corregido (FIXED)
```java
// BUG 1 FIX: Synchronize item consumption with client
if (!((class_3222)player).method_7337()) {  // if not creative
    stackInHand.method_7934(1);  // decrement count
    
    // Sync the stack back to the player
    if (stackInHand.method_7960()) {  // if stack is now empty (count == 0)
        ((class_3222)player).method_6030(hand, class_1799.field_8037);  // setStackInHand with ItemStack.EMPTY
    } else {
        ((class_3222)player).method_6030(hand, stackInHand);  // setStackInHand with updated stack
    }
}
```

### Explicación
- `method_7934(1)` → `ItemStack.decrement(1)` - Decrementa el contador del stack
- `method_7960()` → `ItemStack.isEmpty()` - Verifica si el stack está vacío
- `method_6030(hand, stack)` → `PlayerEntity.setStackInHand(Hand, ItemStack)` - Sincroniza el stack con el cliente
- `field_8037` → `ItemStack.EMPTY` - Constante para stack vacío

---

## BUG 2: ActionResult.FAIL causa re-disparo del evento

### Descripción del Bug
El callback devuelve `ActionResult.FAIL` (field_5814) en TODOS los casos (errores y éxito). En Fabric 1.21.1, `ActionResult.FAIL` no detiene la cadena de eventos, causando que Fabric re-dispare el callback múltiples veces.

### Ubicación del Bug
**Archivo**: `CBEventHandler.java` (o `.kt`)  
**Método**: `register$lambda$0` - TODOS los `return` excepto los primeros 2

### Código Original (BUGGY)
```java
// En TODOS los returns de error y éxito:
return class_1269.field_5814;  // ActionResult.FAIL
```

### Código Corregido (FIXED)
```java
// CAMBIAR TODOS los returns de field_5814 a field_5812:
return class_1269.field_5812;  // ActionResult.SUCCESS
```

### Ubicaciones específicas de cambios

#### ✅ Mantener PASS (sin cambios):
```java
// Return 1: No es ServerPlayer
if (!(player instanceof class_3222)) {
    return class_1269.field_5811;  // PASS - OK
}

// Return 2: No es un claim block
String stoneType = CBItemManager.INSTANCE.getStoneType(stackInHand);
if (stoneType == null) {
    return class_1269.field_5811;  // PASS - OK
}
```

#### 🔧 Cambiar FAIL → SUCCESS:
```java
// Return 3: Config no encontrada
if (config == null) {
    return class_1269.field_5812;  // ✅ CHANGED: SUCCESS
}

// Return 4: Ya hay un claim en esa posición
if (!((Collection)CBManager.INSTANCE.getAreasAt(placePos, worldKey)).isEmpty()) {
    return class_1269.field_5812;  // ✅ CHANGED: SUCCESS
}

// Return 5: Intento de colocar en el aire
if (world.method_8320(hitResult.method_17777()).method_26215()) {
    return class_1269.field_5812;  // ✅ CHANGED: SUCCESS
}

// Return 6: Espacio bloqueado
if (!world.method_8320(placePos).method_26215() && !world.method_8320(placePos).method_45474()) {
    return class_1269.field_5812;  // ✅ CHANGED: SUCCESS
}

// Return 7: Overlapping detectado
if (CBManager.INSTANCE.isOverlapping(newArea)) {
    // ... código de mensaje ...
    return class_1269.field_5812;  // ✅ CHANGED: SUCCESS
}

// Return 8: Claim creado exitosamente
// ... código de creación ...
return class_1269.field_5812;  // ✅ CHANGED: SUCCESS
```

### Resumen de Mappings
- `class_1269` → `ActionResult`
- `field_5811` → `ActionResult.PASS` - No manejar, dejar que otros listeners procesen
- `field_5812` → `ActionResult.SUCCESS` - Evento completamente manejado
- `field_5814` → `ActionResult.FAIL` - ⚠️ NO USAR - causa re-trigger en Fabric 1.21.1

---

## BUG 3: isOverlapping bloquea al mismo jugador

### Descripción del Bug
La función `isOverlapping` en `CBManager` compara claims por UUID del GameProfile (`player.method_5667()`). Con EasyAuth instalado, el UUID puede cambiar entre sesiones, causando que el método detecte "overlap" entre claims del mismo jugador.

### Ubicación del Bug
**Archivo**: `CBManager.java` (o `.kt`)  
**Método**: `isOverlapping(@NotNull CBRegion newRegion)`

### Código Original (BUGGY)
```java
public final boolean isOverlapping(@NotNull CBRegion newRegion) {
    // ... código de min/max bounds ...
    
    for (Object element$iv : $this$any$iv) {
        CBRegion existingRegion = (CBRegion)element$iv;
        
        if (Intrinsics.areEqual((Object)existingRegion.getName(), (Object)newRegion.getName())) {
            bl2 = false;  // Skip mismo nombre
        } else if (/* world check */) {
            bl2 = false;
        } else {
            // ❌ BUG: No verifica si es el mismo owner antes del overlap check
            int existingMinX = Math.min(existingRegion.getPos1().method_10263(), ...);
            // ... cálculo de bounds ...
            bl2 = newMinX <= existingMaxX && newMaxX >= existingMinX && 
                  newMinZ <= existingMaxZ && newMaxZ >= existingMinZ;
        }
        if (!bl2) continue;
        return true;  // ❌ Detected overlap even from same owner
    }
    return false;
}
```

### Código Corregido (FIXED)
```java
public final boolean isOverlapping(@NotNull CBRegion newRegion) {
    // ... código de min/max bounds ...
    
    for (Object element$iv : $this$any$iv) {
        CBRegion existingRegion = (CBRegion)element$iv;
        
        if (Intrinsics.areEqual((Object)existingRegion.getName(), (Object)newRegion.getName())) {
            bl2 = false;  // Skip mismo nombre
        }
        // ✅ BUG 3 FIX: Check if same owner before overlap detection
        else if (Intrinsics.areEqual((Object)existingRegion.getOwner(), (Object)newRegion.getOwner())) {
            bl2 = false;  // Skip mismo owner (UUID match)
        }
        else if (/* world check */) {
            bl2 = false;
        } else {
            int existingMinX = Math.min(existingRegion.getPos1().method_10263(), ...);
            // ... cálculo de bounds ...
            bl2 = newMinX <= existingMaxX && newMaxX >= existingMinX && 
                  newMinZ <= existingMaxZ && newMaxZ >= existingMinZ;
        }
        if (!bl2) continue;
        return true;
    }
    return false;
}
```

### Alternativa más robusta (si EasyAuth sigue causando problemas):

Si el UUID sigue cambiando incluso con la fix anterior, usa una comparación adicional por nombre de jugador:

```java
// ✅ ALTERNATIVE FIX: Compare by owner name as fallback
else if (existingRegion.getOwner().equals(newRegion.getOwner()) ||
         existingRegion.getOwnerName().equalsIgnoreCase(newRegion.getOwnerName())) {
    bl2 = false;  // Same owner by UUID or name
}
```

**Nota**: Esta alternativa requiere que `CBRegion` tenga un campo `ownerName` adicional. Si no existe, necesitarás:
1. Agregar `private final String ownerName;` a `CBRegion`
2. Guardar el nombre del jugador al crear el claim:
```java
String ownerName = ((class_3222)player).method_5477().getString();  // getGameProfile().getName()
CBRegion newArea = new CBRegion(areaName, owner, placePos, stoneType, pos1, pos2, 
                                 MapsKt.emptyMap(), null, null, null, worldKey, 
                                 ownerName);  // ADD ownerName parameter
```

### Explicación de Mappings
- `method_5667()` → `Entity.getUuid()` / `GameProfile.getId()`
- `method_5477()` → `PlayerEntity.getGameProfile()`
- `getString()` → `GameProfile.getName()`
- `Intrinsics.areEqual()` → Kotlin's null-safe equality check

---

## BUG 4: Spam de mensajes de overlap

### Descripción del Bug
Este es un **bug sintomático** causado por los BUGs 1 y 2. Cuando el item no desaparece visualmente (BUG 1) y el evento se re-dispara (BUG 2), el jugador ve múltiples mensajes (3-9 veces) por cada intento.

### Solución
✅ **Automáticamente corregido al arreglar BUGs 1 y 2**

No requiere cambios adicionales de código. Una vez que:
1. El item se sincroniza correctamente (BUG 1 FIX)
2. El evento devuelve SUCCESS en lugar de FAIL (BUG 2 FIX)

El jugador solo verá UN mensaje por cada acción.

---

## Resumen de Archivos Modificados

### Archivos que DEBEN ser modificados:
1. ✅ `CBEventHandler.java` (o `.kt`)
   - Agregar sincronización de ItemStack (BUG 1)
   - Cambiar todos los `return field_5814` a `field_5812` (BUG 2)

2. ✅ `CBManager.java` (o `.kt`)
   - Agregar verificación de mismo owner en `isOverlapping()` (BUG 3)

3. ⚠️ Opcional: `CBRegion.java` (o `.kt`)
   - Solo si usas la fix alternativa de BUG 3 con `ownerName`

---

## Mappings de Nombres Intermediary → Legibles

### Clases
- `class_3222` → `ServerPlayerEntity`
- `class_1657` → `PlayerEntity`
- `class_1937` → `World`
- `class_1268` → `Hand`
- `class_3965` → `BlockHitResult`
- `class_1269` → `ActionResult`
- `class_1799` → `ItemStack`
- `class_2338` → `BlockPos`
- `class_1747` → `BlockItem`

### Métodos de PlayerEntity
- `method_7337()` → `isCreative()` / `getAbilities().creativeMode`
- `method_5998(hand)` → `getStackInHand(Hand)`
- `method_6030(hand, stack)` → `setStackInHand(Hand, ItemStack)`
- `method_5667()` → `getUuid()`
- `method_5477()` → `getGameProfile()`
- `method_43496(message)` → `sendMessage(Text)`

### Métodos de ItemStack
- `method_7934(amount)` → `decrement(int)`
- `method_7960()` → `isEmpty()`
- `method_7909()` → `getItem()`

### Constants
- `field_5811` → `ActionResult.PASS`
- `field_5812` → `ActionResult.SUCCESS`
- `field_5814` → `ActionResult.FAIL`
- `field_8037` → `ItemStack.EMPTY`

---

## Testing Checklist

Después de aplicar las fixes, verifica:

### Test BUG 1 Fix:
- [ ] Coloca un claim block en modo survival
- [ ] El item debe desaparecer INMEDIATAMENTE del inventario
- [ ] No debe quedar un item "fantasma"

### Test BUG 2 Fix:
- [ ] Intenta colocar un claim en un área inválida (overlap, aire, etc.)
- [ ] Debe aparecer SOLO UN mensaje de error
- [ ] NO debe haber spam de mensajes

### Test BUG 3 Fix:
- [ ] Como el mismo jugador, coloca 2 claims SEPARADOS (sin overlap real)
- [ ] Ambos claims deben colocarse exitosamente
- [ ] NO debe aparecer mensaje de "overlapping" entre tus propios claims
- [ ] Con EasyAuth: Desconecta y reconecta, intenta colocar otro claim
- [ ] Debe funcionar sin detectar overlap con tus claims antiguos

### Test BUG 4 Fix:
- [ ] Realiza cualquier acción (éxito o error)
- [ ] Debe aparecer SOLO UN mensaje
- [ ] NO debe haber duplicación de mensajes

---

## Build Instructions

### Requisitos:
- Java 21 (bytecode version 65.0)
- Gradle 8.x o superior
- Fabric Loom plugin
- Kotlin Gradle Plugin (si usas Kotlin)

### Pasos para recompilar:

```bash
# 1. Clonar/extraer el proyecto
unzip ClaimBlocksREPARAR-1.0.jar -d source/

# 2. Descompilar las clases
java -jar cfr.jar source/com/f0cus/protectionstones/CBEventHandler.class --outputdir src/
java -jar cfr.jar source/com/f0cus/protectionstones/CBManager.class --outputdir src/

# 3. Aplicar las fixes manualmente (editar los archivos)

# 4. Recompilar
./gradlew build

# 5. El JAR estará en build/libs/
```

---

## Notas Finales

- **Compatibilidad**: Estas fixes son específicas para Minecraft 1.21.1 Fabric
- **EasyAuth**: El BUG 3 es específicamente problemático con EasyAuth instalado
- **Testing**: Prueba en un servidor de desarrollo antes de desplegar en producción
- **Backup**: Siempre haz backup del JAR original y de los datos (`claimblocks_data.dat`)

---

**Fecha de documentación**: 2026-05-27  
**Versión del mod**: ClaimBlocksREPARAR-1.0  
**Target**: Minecraft 1.21.1 Fabric + Java 21
