# ClaimBlocks — fuente y build (Forge 1.20.1, Java 17)

Fuente recuperada por decompilacion (CFR) + remapeo SRG->oficial, sobre la que se
desarrollan las features. Mod id `claimblocks`, entrega en `claimblocksActualizar.jar`
(raiz del repo). Descarga directa: la URL raw de `main`.

## Metodo de build usado (surgical, sin recompilar mixins)

Los **mixins** (`PressurePlateMixin`, `DispenserBlockMixin`) y su `claimblocks.refmap.json`
NO se recompilan: se conservan byte-a-byte del jar entregado. Solo se recompilan las
clases de logica/GUI/datos y se **inyectan** en el jar base.

1. Proyecto ForgeGradle 6, mappings `official` 1.20.1, forge `1.20.1-47.2.0`.
2. Las clases del mod que NO se editan se resuelven desde `libs/claimblocks-deps.jar`
   = el jar entregado actual SIN las clases que se recompilan (para evitar duplicados).
   Regenerar con `zip -d`.
3. Las clases editadas viven en `src/main/java/com/claimblocks/...` en **nombres oficiales**.
   Si se parte de codigo decompilado (SRG `m_/f_`), remapear con `remap.py` (usa el tsrg
   `srg_to_official_1.20.1.tsrg` del cache de ForgeGradle).
4. Build:
   ```
   export JAVA_HOME=<java17>
   java -Xmx3G -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain \
        --no-daemon --console=plain assemble
   ```
   Salida reobfuscada (SRG) en `build/libs/cbmod-<version>.jar`.
5. Inyectar `com/claimblocks/**.class` del jar reobf en el jar base + subir `version` en
   `META-INF/mods.toml`.

## Verificacion obligatoria antes de entregar
- `refmap` + `mixins.json` + clases mixin byte-identicas al original (md5).
- Bytecode reobfuscado a SRG (javap: `m_20183_` etc., cero nombres oficiales de MC).
- Version correcta en `mods.toml`. Integridad del zip (`unzip -t`).
