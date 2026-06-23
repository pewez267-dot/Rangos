# HennyEssentials — Fix de respawn con LuckPerms (Forge 1.20.1)

**JAR listo para usar:** `HennyEssentials-forge-1.20.1-1.0.5-respawnfix.jar` (en la raíz del repo)
**Versión base:** Forge 1.20.1 (Forge 47.3.0, Java 17, LuckPerms API 5.4) — confirmado.

## El bug

Cuando un jugador moría y presionaba **respawn**, el servidor recreaba la entidad del
jugador (`ServerPlayer`) y, casi al mismo tiempo, LuckPerms volvía a enganchar su
"capability" (los datos del usuario) a esa entidad nueva.

Durante esa fracción de segundo, los datos del jugador **todavía no estaban listos**.
HennyEssentials consultaba a LuckPerms demasiado rápido y LuckPerms lanzaba:

```
java.lang.IllegalStateException: Capability missing for <UUID>
```

Esa excepción NO se atrapaba dentro de HennyEssentials, así que rompía el proceso de
respawn y el jugador quedaba **congelado en el aire, como un fantasma**.

Es una clásica **condición de carrera**: dos cosas corriendo a la vez cuando una
(la consulta de permisos) depende de que la otra (la carga de datos de LuckPerms) haya
terminado primero.

## La causa exacta en el código

En `LuckPermsIntegration.java`, varios métodos llamaban a:

```java
luckPerms.getContextManager().getQueryOptions(user).orElseThrow();
```

`getQueryOptions(...)` obliga a LuckPerms a recalcular el contexto del jugador leyendo la
capability de la entidad. Si la entidad recién creada (respawn) aún no la tiene,
revienta. Y solo `hasPermission()` tenía un `try/catch`; el resto
(`getPrefix`, `getSuffix`, `getMaxHomeLimit`, `getUserPermissions`, `checkPermission(List)`)
dejaba escapar la excepción.

## La solución

Se blindó por completo `LuckPermsIntegration.java` para que **nunca** pueda romper el
respawn (ni el chat, ni los comandos):

1. **`provider()`** — obtiene la API de LuckPerms de forma segura; si todavía no está
   registrada, devuelve `null` en vez de lanzar.
2. **`loadedUser(uuid)`** — solo devuelve el usuario si LuckPerms confirma que ya está
   cargado (`getUserManager().isLoaded(uuid)`). Durante la ventana del respawn devuelve
   `null`, así que se usa un valor por defecto en lugar de crashear.
3. **`safeQueryOptions(user)`** — nunca usa `orElseThrow()`. Si el contexto dependiente
   del jugador no está listo, cae con elegancia a `getStaticQueryOptions()` (y como último
   recurso a `QueryOptions.nonContextual()`).
4. **Todos los métodos públicos** quedaron envueltos en `try/catch (Throwable)` y
   devuelven un valor seguro (`false` / `""` / `0` / lista vacía) ante cualquier error
   transitorio.

### Resultado

- El `IllegalStateException: Capability missing` ya **no puede propagarse** ni congelar
  al jugador. El respawn termina siempre de forma normal.
- En cuanto LuckPerms termina de cargar al jugador (milisegundos después), los permisos,
  el prefix y el suffix se resuelven con su valor correcto. El único efecto de la ventana
  de carrera es que, por un instante, esos valores son los por defecto.
- No cambia ninguna firma pública: es un reemplazo binario compatible (drop-in).

## Cómo se generó el JAR

El proyecto fuente completo del port traía errores de compilación previos y ajenos a este
bug (el paquete `data/` quedó a medio portar de 1.20.5+ a 1.20.1). Como
`LuckPermsIntegration` **no referencia ninguna clase de Minecraft** (solo `net.luckperms.*`
y `java.*`), la reobfuscación no la toca. Por eso se compiló esa única clase contra
`net.luckperms:api:5.4` (bytecode Java 17, idéntico al original) y se reemplazó dentro del
JAR de producción ya funcional. Así el fix se aplica de forma quirúrgica, sin arrastrar los
errores no relacionados del resto del proyecto.

## Instalación

1. Quitá el JAR viejo de la carpeta `mods/` del servidor.
2. Copiá `HennyEssentials-forge-1.20.1-1.0.5-respawnfix.jar` a `mods/`.
3. Reiniciá el servidor. Forge 1.20.1-47.3.0 + (opcional) LuckPerms para Forge 1.20.1.
