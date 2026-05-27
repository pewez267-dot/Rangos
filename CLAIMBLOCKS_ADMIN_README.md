# 🛡️ Claim Blocks Mod - ADMIN EDITION
## Minecraft 1.21.1 Fabric - Sistema Administrativo

---

## 📦 **Información del Mod**

**Versión:** 1.0.0 Admin Edition  
**Minecraft:** 1.21.1  
**Loader:** Fabric  
**Fabric API:** 0.107.0+1.21.1 o superior (REQUERIDO)  
**Tipo:** MOD ADMINISTRATIVO (Solo OPs)

---

## ⚠️ **IMPORTANTE: SISTEMA ADMINISTRATIVO**

Este mod está diseñado para **administradores de servidores**.

❌ **NO hay recetas de crafting**  
❌ **Jugadores normales NO pueden obtener bloques claim**  
✅ **Solo OPs (nivel 2+) pueden gestionar claims**  
✅ **Todo se maneja mediante comandos**

---

## 🎯 **Cómo Funciona**

### **Para Administradores (OPs):**

1. **Dar bloques claim a jugadores:**
   ```
   /claim give <jugador> <tier> [cantidad]
   ```

2. **Crear claim en una posición:**
   ```
   /claim create <tier> [posición]
   ```

3. **Eliminar claim:**
   ```
   /claim delete [posición]
   ```

4. **Listar todos los claims:**
   ```
   /claim list
   ```

5. **Ver información de un claim:**
   ```
   /claim info [posición]
   ```

6. **Abrir menú (próximamente):**
   ```
   /claim menu
   ```

---

## 📋 **COMANDOS DETALLADOS**

### **1. `/claim give <jugador> <tier> [cantidad]`**

Da bloques claim a un jugador.

**Ejemplos:**
```
/claim give Steve 1          → Da 1 Claim Block Tier 1 a Steve
/claim give Alex 3 5         → Da 5 Claim Blocks Tier 3 a Alex
/claim give @a 2 1           → Da 1 Claim Block Tier 2 a todos
/claim give @p 5             → Da 1 Claim Block Tier 5 al jugador más cercano
```

**Resultado:**
- El jugador recibe los bloques en su inventario
- Puede colocarlos (si es OP) o solo tenerlos

---

### **2. `/claim create <tier> [posición]`**

Crea un claim y coloca el bloque automáticamente.

**Ejemplos:**
```
/claim create 1              → Crea claim Tier 1 en tu posición actual
/claim create 3 100 64 200   → Crea claim Tier 3 en X:100, Y:64, Z:200
/claim create 5 ~ ~ ~        → Crea claim Tier 5 donde estás parado
```

**Resultado:**
- Se coloca el bloque claim
- Se crea la protección automáticamente
- Se muestra el área protegida

---

### **3. `/claim delete [posición]`**

Elimina un claim existente.

**Ejemplos:**
```
/claim delete                → Elimina el claim donde estás parado
/claim delete 100 64 200     → Elimina el claim en esas coordenadas
/claim delete ~ ~ ~          → Elimina el claim en tu posición
```

**Resultado:**
- El bloque desaparece
- La protección se elimina
- El área queda libre

---

### **4. `/claim list`**

Lista todos los claims en la dimensión actual.

**Ejemplo:**
```
/claim list
```

**Resultado:**
```
📋 Claims en esta dimensión (3):
  • Tier 1 en [100, 64, 200] - Owner: Steve
  • Tier 3 en [500, 70, -300] - Owner: Alex
  • Tier 5 en [-100, 65, 400] - Owner: Herobrine
```

---

### **5. `/claim info [posición]`**

Muestra información detallada de un claim.

**Ejemplos:**
```
/claim info                  → Info del claim donde estás
/claim info 100 64 200       → Info del claim en esas coordenadas
```

**Resultado:**
```
ℹ️ Información del Claim:
  Tier: 3
  Owner: Steve
  Posición: [100, 64, 200]
  Área: 20x20 bloques
```

---

## 🎨 **LOS 5 TIERS**

| Tier | Área Protegida | Radio | Color | Iluminación |
|------|---------------|-------|-------|-------------|
| **1** | 5x5 bloques | 2 | Gris/Plata | Nivel 5 |
| **2** | 10x10 bloques | 5 | Dorado | Nivel 7 |
| **3** | 20x20 bloques | 10 | Azul Turquesa | Nivel 10 |
| **4** | 30x30 bloques | 15 | Verde Esmeralda | Nivel 12 |
| **5** | 50x50 bloques | 25 | Púrpura | Nivel 15 |

---

## 🔒 **SISTEMA DE PROTECCIÓN**

### **¿Qué protege?**
- ❌ Romper bloques
- ❌ Colocar bloques
- ❌ Abrir cofres/puertas/contenedores
- ❌ Usar botones/palancas/pressure plates
- ❌ Interactuar con CUALQUIER cosa

### **¿Quién puede modificar?**
- ✅ El **propietario** del claim (quien lo creó)
- ✅ Los **administradores** (OPs nivel 2+)

### **Reglas especiales:**
- Solo OPs pueden **colocar** bloques claim manualmente
- Solo OPs pueden **romper** bloques claim
- Los claims **NO se superponen** (protección anti-overlap)
- Cada jugador puede ser dueño de **múltiples claims**

---

## 💡 **CASOS DE USO COMUNES**

### **Escenario 1: Dar claim a un jugador nuevo**
```
1. /claim give NuevoJugador 1 2
2. El jugador recibe 2 bloques Tier 1
3. (Si no es OP, no puede colocarlos)
4. Tú como OP usas: /claim create 1 <posición del jugador>
```

### **Escenario 2: Crear zona protegida de spawn**
```
1. Ve al spawn
2. /claim create 5
3. Claim Tier 5 (50x50) protege todo el spawn
```

### **Escenario 3: Proteger la casa de un jugador**
```
1. Ve a la casa del jugador
2. /claim create 3 ~ ~ ~
3. Claim Tier 3 (20x20) protege la casa
4. El jugador mantiene su casa segura
```

### **Escenario 4: Eliminar claim abandonado**
```
1. Ve al área
2. /claim info (para verificar)
3. /claim delete
4. Área liberada
```

---

## 🔧 **FLUJO DE TRABAJO RECOMENDADO**

### **Para Administradores:**

1. **Explorar claims existentes:**
   ```
   /claim list
   ```

2. **Crear claim para jugador:**
   ```
   /claim create <tier> <posición>
   ```

3. **Verificar claim:**
   ```
   /claim info
   ```

4. **Dar bloques extras (opcional):**
   ```
   /claim give <jugador> <tier> <cantidad>
   ```

5. **Gestionar claims:**
   - Listar periódicamente
   - Eliminar claims abandonados
   - Crear zonas públicas protegidas

---

## ⚙️ **CONFIGURACIÓN DEL SERVIDOR**

### **Permisos necesarios:**

Para usar todos los comandos, necesitas:
- **OP Nivel 2** o superior
- Configurar en `server.properties`: `op-permission-level=2`

### **Comandos para dar OP:**
```
/op <jugador>
/deop <jugador>
```

---

## 📥 **Instalación**

1. **Instalar Fabric Loader** para Minecraft 1.21.1
2. **Descargar Fabric API** 0.107.0+1.21.1 o superior
3. **Descargar** `claimblocks-admin-1.0.0.jar`
4. Colocar ambos archivos JAR en la carpeta `mods/`
5. **Reiniciar el servidor**
6. **Dar OP** a los administradores: `/op <admin>`
7. ¡Usar comandos `/claim`!

---

## 🆚 **DIFERENCIAS CON VERSIÓN NORMAL**

| Característica | Versión Normal | Admin Edition |
|---------------|---------------|---------------|
| Recetas Crafting | ✅ Sí | ❌ No |
| Jugadores pueden obtener | ✅ Sí | ❌ Solo OPs |
| Colocar bloques | ✅ Todos | ❌ Solo OPs |
| Romper bloques | ✅ Dueño | ❌ Solo OPs |
| Comandos | ❌ No | ✅ Sí |
| Control total | ❌ Limitado | ✅ Total |

---

## 🐛 **TROUBLESHOOTING**

### **"Unknown command: claim"**
- Verifica que el mod esté instalado correctamente
- Reinicia el servidor

### **"You don't have permission"**
- Necesitas ser OP nivel 2+
- Usa: `/op <tu_nombre>`

### **"No se puede colocar aquí"**
- Hay otro claim superpuesto
- Usa `/claim list` para ver claims existentes
- Usa `/claim delete` en el claim viejo

### **"No hay claim en esta posición"**
- Asegúrate de estar en el claim
- Usa `/claim list` para ver ubicaciones

---

## 💾 **PERSISTENCIA DE DATOS**

- Los claims se guardan automáticamente en `claimblocks_data.dat`
- Persisten entre reinicios del servidor
- Se organizan por dimensión (Overworld, Nether, End)
- Backup automático al guardar

---

## 📝 **NOTAS IMPORTANTES**

✅ **Compatible con servidores multijugador**  
✅ **Optimizado para bajo impacto en performance**  
✅ **Sistema anti-grief completo**  
✅ **Soporta múltiples dimensiones**  
✅ **Texturas únicas 16x16**  
✅ **Control administrativo total**

⚠️ **Los jugadores NO pueden craftear bloques claim**  
⚠️ **Solo OPs pueden gestionar claims**  
⚠️ **Requiere Fabric API**

---

## 🎯 **UBICACIÓN DEL ARCHIVO**

**Archivo compilado:** `claimblocks-admin-1.0.0.jar` (27 KB)

---

## 📜 **Licencia**

MIT License - Libre para usar, modificar y distribuir

---

## 🚀 **LINK DE DESCARGA**

```
https://github.com/pewez267-dot/Rangos/raw/main/claimblocks-admin-1.0.0.jar
```

---

¡Sistema de protección administrativo completo para tu servidor! 🛡️👑
