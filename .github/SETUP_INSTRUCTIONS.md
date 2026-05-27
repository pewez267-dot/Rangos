# 🚀 Configuración de GitHub Actions para Mods de Minecraft

## ✅ Ya Configurado

Se ha creado el workflow de GitHub Actions en `.github/workflows/build.yml` que:

- ✅ Compila automáticamente tus mods cuando haces push
- ✅ Genera archivos JAR listos para usar
- ✅ Guarda los JAR como artifacts descargables
- ✅ Crea releases automáticas cuando creas tags
- ✅ Usa Java 21 (compatible con Minecraft 1.21.1)

## 📋 Requisitos Previos

Para que el workflow funcione, necesitas agregar el **código fuente** de tus mods al repositorio con esta estructura:

```
Rangos/
├── .github/
│   └── workflows/
│       └── build.yml          ← Ya creado ✅
├── src/
│   └── main/
│       ├── java/
│       │   └── com/tupackage/
│       │       ├── ClaimBlocks.java
│       │       └── ...
│       └── resources/
│           ├── fabric.mod.json
│           ├── assets/
│           └── data/
├── build.gradle               ← NECESARIO
├── gradle.properties          ← NECESARIO
├── settings.gradle            ← NECESARIO
└── gradlew                    ← NECESARIO
```

## 🔧 Cómo Agregar el Código Fuente

### Opción 1: Si ya tienes el código en tu PC

1. Copia toda la carpeta de tu proyecto al repositorio
2. Asegúrate de incluir:
   - `src/` (código fuente)
   - `build.gradle`
   - `gradle.properties`
   - `settings.gradle`
   - `gradlew` y `gradlew.bat`
   - carpeta `gradle/`

### Opción 2: Crear un nuevo proyecto desde cero

Usa el generador de Fabric:
```bash
# Descarga el template de Fabric
git clone https://github.com/FabricMC/fabric-example-mod.git temp
cd temp
# Copia los archivos necesarios a tu repo
```

## 🚀 Uso del Workflow

### Compilación Automática

Cada vez que hagas push, GitHub Actions:
1. Descarga tu código
2. Configura Java 21
3. Ejecuta `./gradlew build`
4. Genera los JAR en `build/libs/`
5. Los guarda como artifacts

### Descargar los JAR compilados

1. Ve a la pestaña **Actions** en GitHub
2. Haz clic en el último workflow ejecutado
3. Baja hasta **Artifacts**
4. Descarga `minecraft-mods.zip`

### Crear un Release

Para crear un release con los JAR:
```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions automáticamente:
- Compilará el código
- Creará un release
- Adjuntará los JAR al release

## 📝 Ejemplo de build.gradle

Si no tienes uno, aquí un ejemplo básico:

```gradle
plugins {
    id 'fabric-loom' version '1.7-SNAPSHOT'
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group

repositories {
    maven { url 'https://maven.fabricmc.net/' }
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}

processResources {
    inputs.property "version", project.version
    
    filesMatching("fabric.mod.json") {
        expand "version": project.version
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

jar {
    from("LICENSE") {
        rename { "${it}_${project.archivesBaseName}"}
    }
}
```

## 📝 Ejemplo de gradle.properties

```properties
# Fabric Properties
minecraft_version=1.21.1
yarn_mappings=1.21.1+build.3
loader_version=0.16.9

# Mod Properties
mod_version=1.0.0
maven_group=com.tupackage
archives_base_name=claimblocks

# Dependencies
fabric_version=0.107.0+1.21.1
```

## 🎯 Para Múltiples Mods

Si tienes varios mods (como claimblocks y claimblocks-admin), hay dos opciones:

### Opción A: Un repositorio por mod
```
claimblocks/        ← Repositorio separado
claimblocks-admin/  ← Otro repositorio
```

### Opción B: Multi-proyecto con Gradle
```
Rangos/
├── claimblocks/
│   ├── src/
│   └── build.gradle
├── claimblocks-admin/
│   ├── src/
│   └── build.gradle
└── settings.gradle  ← include('claimblocks', 'claimblocks-admin')
```

## 🔄 Próximos Pasos

1. **Agrega tu código fuente** al repositorio
2. **Verifica que build.gradle existe** y está configurado
3. **Haz push** a GitHub
4. **Ve a Actions** y observa la compilación
5. **Descarga los JAR** desde los artifacts

## ❓ ¿Necesitas Ayuda?

Si no tienes el código fuente o necesitas ayuda para configurarlo, avísame:
- ¿Dónde está el código fuente?
- ¿Usas Gradle o Maven?
- ¿Es un proyecto existente o necesitas crear uno nuevo?

---

**¡El workflow está listo!** Solo falta agregar el código fuente 🎉
