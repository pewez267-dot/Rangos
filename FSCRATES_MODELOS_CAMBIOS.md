# FSCrates — Cambio de modelos de crates a los del "Crates and Stuff Model Pack"

Se reemplazaron los modelos/texturas de las crates (generadas con IA) por los modelos 3D
del **Crates and Stuff Model Pack (Update 4)**, distribuidos por rareza según su tamaño.

## Entregable

- **`fscrates-gradle-project-modificado.zip`** → proyecto gradle listo para compilar (tú lo compilas).
- **`fscrates-gradle/`** → mismo proyecto descomprimido, por si quieres revisar los archivos aquí en GitHub.
- **`fscrates-cofres-preview/`** → renders de previsualización de cada cofre + comparativa de tamaños.

> El JAR `fscrates-1.0.0.jar` y el `fscrates-gradle-project.zip` tienen **assets idénticos**
> (mismos md5), así que se trabajó sobre el proyecto gradle, que es el que regenera ese mismo JAR.

## Mapeo rareza → modelo

La asignación del modelo a cada rareza se hizo **por el tamaño natural** del modelo en el pack
(el cofre dorado, que es el de mayor volumen, va a Mítico; el resto en orden de tamaño):

| Rareza (mod) | Modelo del pack      | Color / nota                 |
|--------------|----------------------|------------------------------|
| Común        | `common_crate`       | cajón de madera (el más chico)|
| Raro         | `vote_crate`         | verdoso                      |
| Épico        | `rare_crate`         | azul (con candado)           |
| Legendario   | `cosmetic_crate`     | cofre del tesoro grande      |
| **Mítico**   | **`legendary_crate`**| **dorado, el más grande**    |

Tamaño de render en el mundo (jerarquía clara, mítico el mayor): Común < Raro < Épico < Legendario < Mítico.

## Qué se cambió en el código (se respetó toda la lógica y funciones existentes)

El bloque de la crate usa `RenderShape.ENTITYBLOCK_ANIMATED`, por lo que su aspecto en el mundo
lo dibuja `CrateRenderer` (Java), no el modelo JSON. Por eso fue necesario tocar Java:

1. **Nuevos assets**
   - `assets/fscrates/models/block/crate_{common,rare,epic,legendary,mythic}.json` (convertidos desde los `.bbmodel`).
   - `assets/fscrates/textures/block/crate_{...}.png` (+ `_lock` / `_straw_hat` donde aplica).
2. **`CrateBakedModels.java` (nuevo)** — define las `ResourceLocation` de los 5 modelos y los obtiene del `ModelManager`.
3. **`ClientEvents.java`** — registra los 5 modelos como modelos adicionales (`ModelEvent.RegisterAdditional`).
4. **`CrateRenderer.java`** — en vez de dibujar el cofre genérico tinteado, ahora dibuja el modelo
   horneado correspondiente a la rareza. **Se conservan** todas las animaciones y efectos
   (salto, giro, escala, bamboleo, haz de luz, "ruleta" de premios, hologramas/probabilidades).

No se modificó ninguna otra clase ni la lógica de loot, comandos, red, cooldowns, etc.

## Cómo compilar

Proyecto Forge **1.20.1 (47.2.0)**, Java 17. Dentro de la carpeta del proyecto:

```
./gradlew build      # genera build/libs/fscrates-1.0.0.jar
```

El estilo de los nombres de método del código nuevo es el **mismo** (SRG: `m_xxxxx_`) que el del
resto del proyecto, para que compile con el mismo toolchain que ya usas.

## Notas / limitaciones

- Las texturas del pack son estáticas (no son tiras animadas); la "animación" es el movimiento
  3D del cofre (giro/salto/escala) que ya hace el mod, ahora con los modelos nuevos.
- El **ícono del item** de la crate (en inventario/mano) se dejó como estaba. Si quieres que
  también use el modelo 3D por rareza, se puede hacer (dímelo).
