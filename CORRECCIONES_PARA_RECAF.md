# 🔧 CORRECCIONES EXACTAS PARA RECAF

## Instrucciones:
1. Abre Recaf
2. Carga `ClaimBlocksREPARAR-1.0.jar`
3. Navega a las clases indicadas
4. Aplica los cambios descritos a continuación
5. Exporta el JAR corregido

---

## ARCHIVO 1: CBEventHandler.class

### CAMBIO 1.1 - BUG 1: Sincronizar item con cliente (línea ~338)

**Busca este código** (alrededor de línea 338):
```java
if (!((class_3222)player).method_7337()) {
    stackInHand.method_7934(1);
}
```

**Reemplaza con**:
```java
if (!((class_3222)player).method_7337()) {
    stackInHand.method_7934(1);
    ((class_3222)player).method_51282(hand, stackInHand);
}
```

---

### CAMBIO 1.2 - BUG 2: Cambiar ActionResult de FAIL a SUCCESS

**Busca y reemplaza TODOS estos returns** (6 instancias en register$lambda$0):

#### Ubicación 1: Error de configuración no encontrada (~línea 253)
```java
// ANTES:
return class_1269.field_5814;
// DESPUÉS:
return class_1269.field_5812;
```

#### Ubicación 2: Error "Inside another zone" (~línea 262)
```java
// ANTES:
return class_1269.field_5814;
// DESPUÉS:
return class_1269.field_5812;
```

#### Ubicación 3: Error "Air placement" (~línea 268)
```java
// ANTES:
return class_1269.field_5814;
// DESPUÉS:
return class_1269.field_5812;
```

#### Ubicación 4: Error "Space blocked" (~línea 273)
```java
// ANTES:
return class_1269.field_5814;
// DESPUÉS:
return class_1269.field_5812;
```

#### Ubicación 5: Error "Overlapping region" (~línea 310)
```java
// ANTES:
return class_1269.field_5814;
// DESPUÉS:
return class_1269.field_5812;
```

#### Ubicación 6: Caso de éxito final (~línea 347)
```java
// ANTES:
return class_1269.field_5814;
// DESPUÉS:
return class_1269.field_5812;
```

**⚠️ NO CAMBIES ESTOS (líneas ~236 y ~243)**:
```java
return class_1269.field_5811;  // ← DEJAR COMO ESTÁ
```

---



## ARCHIVO 2: CBManager.class

### CAMBIO 2.1 - BUG 3: Agregar check de UUID + nombre del jugador

**Busca este bloque de código** (alrededor de líneas 273-283 en el método `isOverlapping`):

```java
} else {
    int existingMinX = Math.min(existingRegion.getPos1().method_10263(), existingRegion.getPos2().method_10263());
    int existingMinZ = Math.min(existingRegion.getPos1().method_10260(), existingRegion.getPos2().method_10260());
    int existingMaxX = Math.max(existingRegion.getPos1().method_10263(), existingRegion.getPos2().method_10263());
    int existingMaxZ = Math.max(existingRegion.getPos1().method_10260(), existingRegion.getPos2().method_10260());
    bl2 = newMinX <= existingMaxX && newMaxX >= existingMinX && newMinZ <= existingMaxZ && newMaxZ >= existingMinZ;
}
```

**Reemplaza con este código completo**:

```java
} else {
    // NUEVO: Check de mismo owner por UUID o nombre
    UUID existingOwner = existingRegion.getOwner();
    UUID newOwner = newRegion.getOwner();
    
    if (Intrinsics.areEqual((Object)existingOwner, (Object)newOwner)) {
        bl2 = false; // Mismo owner por UUID → permitir overlap
    } else {
        // Fallback: comparar por nombre de jugador (formato: "playername-stonetype-xxxx")
        try {
            String existingPlayerName = existingRegion.getName().split("-")[0];
            String newPlayerName = newRegion.getName().split("-")[0];
            
            if (existingPlayerName.equalsIgnoreCase(newPlayerName)) {
                bl2 = false; // Mismo owner por nombre → permitir overlap
            } else {
                // Diferentes owners → check espacial
                int existingMinX = Math.min(existingRegion.getPos1().method_10263(), existingRegion.getPos2().method_10263());
                int existingMinZ = Math.min(existingRegion.getPos1().method_10260(), existingRegion.getPos2().method_10260());
                int existingMaxX = Math.max(existingRegion.getPos1().method_10263(), existingRegion.getPos2().method_10263());
                int existingMaxZ = Math.max(existingRegion.getPos1().method_10260(), existingRegion.getPos2().method_10260());
                bl2 = newMinX <= existingMaxX && newMaxX >= existingMinX && newMinZ <= existingMaxZ && newMaxZ >= existingMinZ;
            }
        } catch (Exception e) {
            // Fallback si falla el parsing
            int existingMinX = Math.min(existingRegion.getPos1().method_10263(), existingRegion.getPos2().method_10263());
            int existingMinZ = Math.min(existingRegion.getPos1().method_10260(), existingRegion.getPos2().method_10260());
            int existingMaxX = Math.max(existingRegion.getPos1().method_10263(), existingRegion.getPos2().method_10263());
            int existingMaxZ = Math.max(existingRegion.getPos1().method_10260(), existingRegion.getPos2().method_10260());
            bl2 = newMinX <= existingMaxX && newMaxX >= existingMinX && newMinZ <= existingMaxZ && newMaxZ >= existingMinZ;
        }
    }
}
```

---

## ✅ CHECKLIST DE VERIFICACIÓN

Después de aplicar los cambios en Recaf:

- [ ] CBEventHandler.class:
  - [ ] Línea ~340: Agregado `method_51282(hand, stackInHand)`
  - [ ] 6x returns cambiados de `field_5814` a `field_5812`
  - [ ] 2x returns dejados como `field_5811` (líneas ~236 y ~243)

- [ ] CBManager.class:
  - [ ] Agregado bloque de código de check UUID + nombre
  - [ ] Try-catch implementado correctamente
  - [ ] Lógica de fallback presente

- [ ] Exportar JAR:
  - [ ] Nombre sugerido: `claimblocks-1.0.1-FIXED.jar`
  - [ ] Verificar que el tamaño sea similar al original (~28 KB)

---

## 🧪 TESTING

Una vez exportado el JAR, prueba:

1. **Test BUG 1**: Coloca un claim block → item debe desaparecer inmediatamente
2. **Test BUG 2**: Intenta colocar en área inválida → solo 1 mensaje de error
3. **Test BUG 3**: Con EasyAuth, desconecta/reconecta y coloca otro claim → debe funcionar
4. **Test BUG 4**: Clicks rápidos → no spam de mensajes

---

## 💡 TIPS PARA RECAF

### Cómo encontrar las líneas exactas:

1. **Para CBEventHandler**:
   - Busca el método: `register$lambda$0`
   - Usa Ctrl+F para buscar: `field_5814`
   - Verás exactamente dónde están los 6 returns a cambiar

2. **Para CBManager**:
   - Busca el método: `isOverlapping`
   - Busca el bloque que empieza con: `int existingMinX = Math.min`
   - Reemplaza todo ese bloque

### Recomendación:
- En Recaf, usa el modo "Decompile" (CFR o Fernflower)
- Edita el código Java directamente
- Recaf recompilará automáticamente al guardar

---

## 📥 LINK DE DESCARGA DEL JAR ORIGINAL

Si necesitas el JAR original:
```
https://github.com/pewez267-dot/Rangos/raw/main/ClaimBlocksREPARAR-1.0.jar
```

---

**¿Problemas con Recaf?**

Si Recaf no puede recompilar (puede pasar con código Kotlin descompilado):

### Plan B - Usar ASM (bytecode directo):
Te puedo proporcionar un script de bytecode manipulation si lo necesitas.

### Plan C - Solicitar recompilación:
Contacta a F0CUS con este documento completo.

---

**Versión del documento**: 1.0  
**Fecha**: 2025-01-25  
**Archivos a modificar**: 2 (CBEventHandler.class, CBManager.class)  
**Total de cambios**: 8 modificaciones
