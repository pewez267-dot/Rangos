# FSCrates — Fantastic Crates for Forge 1.20.1

**Autor:** Pewez  
**Versión:** 1.0.0  
**Minecraft:** 1.20.1  
**Forge:** 47.x

Sistema de crates avanzado con rareza, GUI editable en juego, recompensas NBT completas, cooldown por jugador y motor de animaciones modular.

## Estructura del Proyecto

```
fscrates-gradle/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew           ← Linux/Mac
├── gradlew.bat       ← Windows
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
└── src/main/
    ├── java/com/fscrates/
    │   ├── FSCrates.java              ← Clase principal @Mod
    │   ├── animation/
    │   │   ├── AnimationRegistry.java
    │   │   └── CrateAnimation.java
    │   ├── block/
    │   │   ├── CrateBlock.java
    │   │   └── CrateBlockEntity.java
    │   ├── client/
    │   │   ├── ClientEvents.java
    │   │   ├── ClientPacketHandler.java
    │   │   ├── ClientSetup.java
    │   │   ├── RegistryLists.java
    │   │   ├── render/
    │   │   │   ├── CrateModel.java
    │   │   │   └── CrateRenderer.java
    │   │   ├── screen/
    │   │   │   ├── CrateEditorScreen.java
    │   │   │   └── NbtEditorScreen.java
    │   │   └── widget/
    │   │       └── ScrollSelector.java
    │   ├── command/
    │   │   └── FSCrateCommand.java
    │   ├── config/
    │   │   ├── CrateConfig.java
    │   │   ├── ParticleLayer.java
    │   │   ├── ParticleNames.java
    │   │   ├── Rarity.java
    │   │   └── RewardEntry.java
    │   ├── crate/
    │   │   ├── CooldownData.java
    │   │   ├── CrateOpeningService.java
    │   │   ├── CrateRegistry.java
    │   │   ├── DelayedDelivery.java
    │   │   └── LootEngine.java
    │   ├── item/
    │   │   ├── CrateItems.java
    │   │   └── KeyItem.java
    │   ├── network/
    │   │   ├── FSNetwork.java
    │   │   ├── OpenEditorPacket.java
    │   │   ├── PlayAnimationPacket.java
    │   │   └── SaveCratePacket.java
    │   └── registry/
    │       └── ModRegistry.java
    └── resources/
        ├── pack.mcmeta
        ├── META-INF/
        │   └── mods.toml
        └── assets/fscrates/
            ├── blockstates/
            ├── lang/
            ├── models/
            └── textures/
```

## Compilar

### Requisitos
- Java 17
- Gradle 8.1.1 (incluido via wrapper)
- Internet para descargar Forge MDK y dependencias

### Comandos

```bash
# Compilar y generar el JAR
./gradlew build

# Ejecutar cliente de desarrollo
./gradlew runClient

# Ejecutar servidor de desarrollo
./gradlew runServer

# Generar datos (datagen)
./gradlew runData
```

El JAR compilado estará en `build/libs/fscrates-1.0.0.jar`.

### Primer uso (setup)
```bash
./gradlew genEclipseRuns   # Eclipse
./gradlew genIntellijRuns  # IntelliJ IDEA
```

## Nota sobre el código decompilado

Este proyecto fue generado descompilando el JAR original con Procyon.  
Los métodos obfuscados de Minecraft (ej. `m_49959_`, `f_49792_`) son mappings SRG.  
Para nombres legibles, cambia en `build.gradle`:
```groovy
mappings channel: 'parchment', version: '2023.09.03-1.20.1'
```
y añade el repositorio ParchmentMC (ya incluido en el `build.gradle`).
