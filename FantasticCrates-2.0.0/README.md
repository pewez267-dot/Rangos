# FSCrates — Fantastic Crates for Forge 1.20.1

**Autor:** Pewez
**Versión:** 2.0.0
**Minecraft:** 1.20.1
**Forge:** 47.x

El sistema de crates más avanzado para Forge 1.20.1: crates y llaves con rarezas, GUI editable en juego, recompensas con NBT completo, cooldown por jugador y un motor de animaciones modular e ilimitado.

## Cambios de la 2.0.0

- 🖱️ **Pestaña "Prob." (probabilidades) ahora es una lista con scroll.** Antes, cuando había
  muchas recompensas, los campos se amontonaban y se salían de la pantalla (no se podían ver
  ni editar las de abajo). Ahora:
  - Solo se muestran las filas que caben; las demás se ven **desplazando con la rueda del ratón**.
  - Aparece una **barra de scroll** a la derecha que indica la posición.
  - Cada fila sigue teniendo su campo editable de % (edición en línea, igual que antes).
- ✍️ **Ortografía corregida** en toda la interfaz: acentos, tildes y la "ñ" donde corresponde
  (Animación, Partículas, Tensión, Revelación, tamaño, Común/Épica/Mítica, línea, código, etc.).

> Solo se modificó eso. El resto del mod (recompensas, animaciones, partículas, llaves,
> comandos y guardado) conserva su comportamiento original.

## Compilar

> Este proyecto usa **mappings oficiales de Mojang** (`mappings channel: 'official'`),
> por lo que el código está en nombres legibles de Mojang.

```bash
# Linux/Mac
./gradlew build
# Windows
gradlew.bat build
```

El JAR resultante quedará en `build/libs/fscrates-2.0.0.jar`. Requiere JDK 17.
