# ClaimBlocksREPARAR-1.0 - Bug Fixes Guide

## 🐛 Bugs Identificados y Soluciones

### Stack del Servidor
- Minecraft 1.21.1 Fabric  
- EasyAuth instalado (autenticación offline/online)  
- Java 21, bytecode version 65.0

---

## BUG 1 — Item fantasma / no se consume visualmente

**Ubicación**: `CBEventHandler.register$lambda$0` línea ~240

**Problema**: El item se consume en el servidor pero no se sincroniza con el cliente, causando un "item fantasma" visual.

**Código Original**:
```java
if (!((class_3222)player).method_7337()) {  // isCreative()
    stackInHand.method_7934(1);              // decrement(1)
}
return class_1269.field_5814;  // devuelve ActionResult
```

**Corrección Necesaria**:
```java
if (!((class_3222)player).method_7337()) {  // isCreative()
    stackInHand.method_7934(1);              // decrement(1)
    ((class_3222)player).method_51282(hand, stackInHand);  // setStackInHand - SYNC CON CLIENTE
}
return class_1269.field_5812;  // ActionResult.SUCCESS (NO field_5814)
```

**Mappings**:
- `method_7337()` = `isCreative()`
- `method_7934(1)` = `decrement(1)`
- `method_51282(hand, stack)` = `setStackInHand(hand, stack)` ← **CRÍTICO: Sincroniza con cliente**

---

## BUG 2 — ActionResult.FAIL hace que Fabric re-dispare el evento

**Ubicación**: `CBEventHandler.register$lambda$0` - 6 ubicaciones

**Problema**: Usar `ActionResult.FAIL` (field_5814) no detiene la cadena de eventos en Fabric 1.21.1, causando re-disparos múltiples.

**Cambios Necesarios** (cambiar TODOS los field_5814 a field_5812):

1. **Error de configuración no encontrada** (~línea 253):
```java
// ANTES:
return class_1269.field_5814;
// DESPUÉS:
return class_1269.field_5812;
```

2. **Error "Inside another zone"** (~línea 262):
```java
// ANTES:
return class_1269.field_5814;
// DESPUÉS:
return class_1269.field_5812;
```

3. **Error "Air placement"** (~línea 268):
```java
// ANTES:
return class_1269.field_5814;
// DESPUÉS:
return class_1269.field_5812;
```

4. **Error "Space blocked"** (~línea 273):
```java
// ANTES:
return class_1269.field_5814;
// DESPUÉS:
return class_1269.field_5812;
```

5. **Error "Overlapping region"** (~línea 305):
```java
// ANTES:
return class_1269.field_5814;
// DESPUÉS:
return class_1269.field_5812;
```

6. **Caso de éxito** (~línea 345):
```java
// ANTES:
return class_1269.field_5814;
// DESPUÉS:
return class_1269.field_5812;
```

**NO CAMBIAR** (mantener como field_5811/PASS):
- Primer return (~línea 236): cuando el jugador no es ServerPlayerEntity
- Segundo return (~línea 243): cuando el item no es un stone type

**Mappings**:
- `field_5814` = `ActionResult.FAIL` ← **INCORRECTO**
- `field_5812` = `ActionResult.SUCCESS` ← **CORRECTO**
- `field_5811` = `ActionResult.PASS` ← **SOLO para los 2 primeros checks**

---

## BUG 3 — isOverlapping bloquea al mismo jugador

**Ubicación**: `CBManager.isOverlapping` líneas ~273-283

**Problema**: Con EasyAuth, el UUID del GameProfile cambia entre sesiones. El código solo compara UUIDs, por lo que el mismo jugador no puede colocar múltiples claim blocks porque sus UUIDs antiguos difieren de los nuevos.

**Código Original**:
```java
} else {
    int existingMinX = Math.min(existingRegion.getPos1().method_10263(), existingRegion.getPos2().method_10263());
    int existingMinZ = Math.min(existingRegion.getPos1().method_10260(), existingRegion.getPos2().method_10260());
    int existingMaxX = Math.max(existingRegion.getPos1().method_10263(), existingRegion.getPos2().method_10263());
    int existingMaxZ = Math.max(existingRegion.getPos1().method_10260(), existingRegion.getPos2().method_10260());
    bl2 = newMinX <= existingMaxX && newMaxX >= existingMinX && newMinZ <= existingMaxZ && newMaxZ >= existingMinZ;
}
```

**Corrección Necesaria**:
```java
} else {
    // NUEVA LÓGICA: Comprobar si es el mismo dueño (UUID o nombre)
    UUID existingOwner = existingRegion.getOwner();
    UUID newOwner = newRegion.getOwner();
    
    if (Intrinsics.areEqual((Object)existingOwner, (Object)newOwner)) {
        bl2 = false;  // Mismo owner por UUID → permitir overlap
    } else {
        // Fallback: comparar por nombre de jugador extraído del region name
        // Formato del region name: "playername-stonetype-xxxx"
        try {
            String existingPlayerName = existingRegion.getName().split("-")[0];
            String newPlayerName = newRegion.getName().split("-")[0];
            
            if (existingPlayerName.equalsIgnoreCase(newPlayerName)) {
                bl2 = false;  // Mismo owner por nombre → permitir overlap
            } else {
                // Diferentes owners → comprobar overlap espacial
                int existingMinX = Math.min(existingRegion.getPos1().method_10263(), existingRegion.getPos2().method_10263());
                int existingMinZ = Math.min(existingRegion.getPos1().method_10260(), existingRegion.getPos2().method_10260());
                int existingMaxX = Math.max(existingRegion.getPos1().method_10263(), existingRegion.getPos2().method_10263());
                int existingMaxZ = Math.max(existingRegion.getPos1().method_10260(), existingRegion.getPos2().method_10260());
                bl2 = newMinX <= existingMaxX && newMaxX >= existingMinX && newMinZ <= existingMaxZ && newMaxZ >= existingMinZ;
            }
        } catch (Exception e) {
            // Si falla el parsing del nombre, usar check espacial como fallback
            int existingMinX = Math.min(existingRegion.getPos1().method_10263(), existingRegion.getPos2().method_10263());
            int existingMinZ = Math.min(existingRegion.getPos1().method_10260(), existingRegion.getPos2().method_10260());
            int existingMaxX = Math.max(existingRegion.getPos1().method_10263(), existingRegion.getPos2().method_10263());
            int existingMaxZ = Math.max(existingRegion.getPos1().method_10260(), existingRegion.getPos2().method_10260());
            bl2 = newMinX <= existingMaxX && newMaxX >= existingMinX && newMinZ <= existingMaxZ && newMaxZ >= existingMinZ;
        }
    }
}
```

**Mapings**:
- `method_10263()` = `getX()`
- `method_10260()` = `getZ()`

**Lógica**:
1. Primero intenta comparar UUIDs (funciona cuando no hay EasyAuth o UUID no cambió)
2. Si UUID difiere, extrae el nombre del jugador del nombre de la región (formato: "jugador-tier-xxxx")
3. Compara nombres (case-insensitive)
4. Si nombre coincide → mismo jugador → permitir overlap
5. Si nombre difiere → diferente jugador → hacer check espacial normal

---

## BUG 4 — Spam de mensajes (AUTOMÁTICO)

**Status**: Resuelto automáticamente al corregir BUG 1 y BUG 2.

**Causa**: Los bugs 1 y 2 causaban que el evento se disparara 3-9 veces por cada clic, generando spam de mensajes.

**Solución**: Al corregir la sincronización del item (BUG 1) y el ActionResult (BUG 2), el evento solo se dispara una vez.

---

## 🔧 Herramientas Necesarias para Aplicar Fixes

### Opción 1: Recompilar desde fuente (requiere acceso al código fuente Kotlin original)
```bash
# Si tienes acceso al código fuente:
# 1. Aplicar los cambios en los archivos .kt
# 2. Compilar con Gradle
./gradlew build
```

### Opción 2: Bytecode patching (sin código fuente)
Usar herramientas como:
- **Recaf** (https://github.com/Col-E/Recaf) - Editor gráfico de bytecode
- **ASM** - Librería para modificación de bytecode Java programáticamente
- **Fernflower + Recompile** - Descompilar, modificar, recompilar

### Opción 3: Solicitar al desarrollador original
Contactar a F0CUS con este documento para que aplique los fixes y compile una nueva versión.

---

## 📋 Resumen de Cambios

| Bug # | Archivo | Método | Línea Aprox | Tipo de Cambio |
|-------|---------|--------|-------------|----------------|
| 1 | CBEventHandler.class | register$lambda$0 | ~240 | Agregar `method_51282(hand, stackInHand)` |
| 2 | CBEventHandler.class | register$lambda$0 | 251,259,263,267,305,345 | Cambiar `field_5814` → `field_5812` |
| 3 | CBManager.class | isOverlapping | 273-310 | Agregar check UUID + nombre |
| 4 | - | - | - | Resuelto automáticamente |

---

## ✅ Testing de las Correcciones

### Test 1 - Verificar BUG 1
1. Colocar un claim block
2. **ANTES**: Item permanece visualmente en inventario
3. **DESPUÉS**: Item desaparece inmediatamente

### Test 2 - Verificar BUG 2  
1. Intentar colocar un claim block en área inválida
2. **ANTES**: Aparecen múltiples mensajes de error
3. **DESPUÉS**: Solo un mensaje de error

### Test 3 - Verificar BUG 3 (con EasyAuth)
1. Colocar un claim block
2. Desconectar y reconectar (cambia UUID con EasyAuth)
3. Intentar colocar otro claim block del mismo jugador
4. **ANTES**: Error "Overlapping"
5. **DESPUÉS**: Se coloca exitosamente

### Test 4 - Verificar BUG 4
1. Intentar colocar claim block rápidamente 5 veces
2. **ANTES**: 15-45 mensajes de spam
3. **DESPUÉS**: Solo 5 mensajes (uno por intento)

---

## 📊 Impacto de los Fixes

- **Compatibilidad**: 100% compatible con datos existentes
- **Performance**: Sin impacto negativo
- **Breaking Changes**: Ninguno
- **Nuevas Features**: Ninguna (solo bug fixes)

---

## 🔒 Notas de Seguridad

Estos fixes NO introducen vulnerabilidades:
- No afectan el sistema de protección
- No permiten bypass de claims
- Solo corrigen sincronización cliente-servidor y lógica de ownership

---

## 📞 Soporte

Si necesitas ayuda aplicando estos fixes:
1. Contacta a F0CUS (desarrollador original)
2. Usa herramientas de bytecode editing (Recaf recomendado)
3. O solicita una versión compilada con los fixes aplicados

---

**Versión del documento**: 1.0  
**Fecha**: 2025-01-25  
**Bugs identificados por**: Análisis del stack trace y comportamiento del servidor  
**Soluciones propuestas por**: Análisis de bytecode descompilado
