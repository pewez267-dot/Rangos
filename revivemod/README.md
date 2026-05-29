# Revive Mod

Mod **100% server-side** para Minecraft **1.21.1 (Fabric)**, **Java 21**.

Cuando un jugador moriria, en lugar de morir queda **noqueado** con una barra
de jefe (bossbar) que muestra una cuenta atras. Otro jugador puede **revivirlo
agachandose cerca**. Si el contador llega a cero, muere de verdad.

No se usan mixins ni codigo cliente: los clientes vanilla se conectan al servidor
sin necesidad de instalar nada.

## Caracteristicas

- Estado de "noqueado" sin morir, con bossbar de cuenta atras (60s por defecto).
- Revivir agachandose dentro de 3 bloques durante 4 segundos (configurable).
- Efecto Glowing en los noqueados para que sus aliados los vean a traves de paredes.
- Compatible con teletransportes (`/tpa`, `/tp`, ender pearls, viajes entre
  dimensiones, login/logout) sin desincronizacion.
- Inmune a danio mientras esta noqueado (excepto el vacio).
- Mobs hostiles pierden el target cuando el jugador queda noqueado.
- Conserva los efectos de pocion / faro previos al noqueo.
- Comandos `/revive` para administradores.

## Instalacion

1. Coloca `revivemod-1.0.0.jar` en la carpeta `mods/` de tu servidor Fabric.
2. Tener instalado [Fabric API](https://modrinth.com/mod/fabric-api) (>= 0.102.0).
3. Reiniciar el servidor.

Se autogenera `config/revivemod.json` con los valores por defecto.

## Comandos (op level 2)

```
/revive help                   - ayuda
/revive status                 - lista jugadores noqueados
/revive force <jugador>        - revivir al jugador instantaneamente
/revive kill <jugador>         - terminar el contador (muerte real)
/revive down <jugador>         - noquear (para pruebas)
/revive set time <segundos>    - cambiar duracion del contador
/revive set distance <bloques> - cambiar distancia de reanimacion
/revive set channel <ticks>    - cambiar duracion del channel (20 ticks = 1s)
/revive reload                 - recargar config
```

## Configuracion (`config/revivemod.json`)

```json
{
  "downTimeSeconds": 60,
  "reviveDistance": 3.0,
  "reviveTimeTicks": 80,
  "reviveHealth": 6.0,
  "reviveFood": 10,
  "glowingWhileDown": true,
  "clearMobAggroOnDown": true
}
```

## Licencia

MIT.
