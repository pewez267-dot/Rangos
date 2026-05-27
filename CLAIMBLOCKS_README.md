# 🛡️ Claim Blocks Mod - Minecraft 1.21.1 Fabric

## 📦 Información del Mod

**Versión:** 1.0.0  
**Minecraft:** 1.21.1  
**Loader:** Fabric  
**Fabric API:** 0.107.0+1.21.1 o superior (REQUERIDO)

---

## ✨ Características

Sistema completo de protección de regiones con **5 tiers progresivos** de bloques claim:

### 🔰 **Tier 1 - Bloque Claim Básico** (Gris/Plata)
- **Área de protección:** 5x5 bloques (radio 2)
- **Receta:** Hierro + Obsidiana + Piedra
- **Iluminación:** Nivel 5
- **Color:** Gris plata con patrón de cruz

### 🥇 **Tier 2 - Bloque Claim de Oro** (Dorado)
- **Área de protección:** 10x10 bloques (radio 5)
- **Receta:** Oro + Diamante + Bloque Claim Tier 1
- **Iluminación:** Nivel 7
- **Color:** Dorado con patrón de diamante

### 💎 **Tier 3 - Bloque Claim de Diamante** (Azul)
- **Área de protección:** 20x20 bloques (radio 10)
- **Receta:** Diamante + Perla de Ender + Bloque Claim Tier 2
- **Iluminación:** Nivel 10
- **Color:** Azul turquesa con patrón de estrella

### 💚 **Tier 4 - Bloque Claim de Esmeralda** (Verde)
- **Área de protección:** 30x30 bloques (radio 15)
- **Receta:** Esmeralda + Estrella del Nether + Bloque Claim Tier 3
- **Iluminación:** Nivel 12
- **Color:** Verde esmeralda con hexágono

### 👑 **Tier 5 - Bloque Claim Supremo** (Púrpura)
- **Área de protección:** 50x50 bloques (radio 25)
- **Receta:** Netherite + Amatista + Bloque Claim Tier 4
- **Iluminación:** Nivel 15 (máxima)
- **Color:** Púrpura con patrón de gema compleja

---

## 🎮 Cómo Usar

### Crear un Claim:
1. Craftea un bloque claim del tier que desees
2. Coloca el bloque en el área que quieres proteger
3. El área protegida se extiende en un radio desde el bloque

### Ver Información del Claim:
- **Click derecho** en el bloque claim para ver:
  - Propietario
  - Tier
  - Tamaño del área protegida

### Romper un Claim:
- Solo el **propietario** puede romper su bloque claim
- Al romperlo, la protección desaparece

---

## 🔒 Sistema de Protección

### ¿Qué está protegido?
- ❌ Romper bloques
- ❌ Colocar bloques
- ❌ Interactuar con bloques (cofres, puertas, botones, etc.)
- ❌ Usar items

### ¿Quién puede modificar?
- ✅ Solo el **propietario** del claim puede modificar su área
- ✅ Los claims **NO se superponen** - no puedes colocar un claim donde haya otro

---

## 🎨 Recetas de Crafting

### Tier 1 - Básico
```
I C I
C O C
I C I
```
- I = Hierro (Iron Ingot)
- C = Piedra (Cobblestone)
- O = Obsidiana

### Tier 2 - Oro
```
G D G
D C D
G D G
```
- G = Oro (Gold Ingot)
- D = Diamante
- C = Bloque Claim Tier 1

### Tier 3 - Diamante
```
D E D
E C E
D E D
```
- D = Diamante
- E = Perla de Ender
- C = Bloque Claim Tier 2

### Tier 4 - Esmeralda
```
E S E
S C S
E S E
```
- E = Esmeralda (Emerald)
- S = Estrella del Nether (Nether Star)
- C = Bloque Claim Tier 3

### Tier 5 - Supremo
```
N A N
A C A
N A N
```
- N = Netherite (Netherite Ingot)
- A = Amatista (Amethyst Shard)
- C = Bloque Claim Tier 4

---

## 💾 Persistencia de Datos

- Los claims se guardan automáticamente en `claimblocks_data.dat`
- Los datos persisten entre reinicios del servidor
- Se guardan por dimensión (Overworld, Nether, End)

---

## 📥 Instalación

1. **Instalar Fabric Loader** para Minecraft 1.21.1
2. **Descargar Fabric API** 0.107.0+1.21.1 o superior
3. **Descargar** `claimblocks-1.0.0.jar`
4. Colocar ambos archivos JAR en la carpeta `mods/`
5. ¡Iniciar Minecraft y jugar!

---

## ⚙️ Características Técnicas

- **Protección en tiempo real** con eventos de Fabric API
- **Sistema de overlapping** - previene colocación de claims superpuestos
- **Persistencia NBT** - datos guardados en formato comprimido
- **Multi-dimensión** - funciona en Overworld, Nether y End
- **Texturas únicas** - cada tier tiene diseño personalizado
- **Iluminación progresiva** - más luz = tier más alto

---

## 🐛 Características de Seguridad

- Bloques claim son **indestructibles** por otros jugadores
- Protección de **50.0 dureza** y **1200.0 resistencia a explosiones**
- Requiere herramientas para minar (pickaxe recomendado)
- Sistema anti-grief completo

---

## 📝 Notas Adicionales

- ✅ Compatible con servidores multijugador
- ✅ Funciona en singleplayer
- ✅ Bajo impacto en performance
- ✅ No requiere configuración
- ✅ Texturas 16x16 vanilla-friendly

---

## 📜 Licencia

MIT License - Libre para usar, modificar y distribuir

---

## 🎯 Ubicación del Archivo

**Archivo compilado:** `/projects/sandbox/claimblocks-1.0.0.jar` (27 KB)

---

¡Disfruta protegiendo tu territorio en Minecraft! 🛡️🏰
