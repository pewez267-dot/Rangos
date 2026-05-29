# Revive Mod

Mod **100% server-side** para Minecraft **1.21.1 (Fabric)**, **Java 21**.

Cuando un jugador moriria, en lugar de morir queda **noqueado**: se tumba en el
suelo (pose de arrastre), solo puede arrastrarse lentamente y no puede hacer
nada mas. Aparece una **bossbar con cuenta atras**. Sus companeros pueden
revivirlo, el puede auto-revivirse pagando experiencia, o rendirse.

No se usan mixins ni codigo cliente: los clientes vanilla se conectan sin
instalar nada.

## Caracteristicas

- **Noqueado en el suelo**: pose de arrastre, Slowness para que solo se arrastre.
  No puede romper bloques, usar/poner bloques, usar items, atacar, interactuar
  ni cambiar de slot.
- **Mensaje**: al caer se anuncia `Jugador ha sido noqueado por <entidad/jugador>`.
- **Revivir con click derecho** sobre el jugador noqueado (dentro de 3 bloques y
  mirandolo). **Entre varios es mas rapido**: con 2 jugadores el doble de rapido,
  con 3 el triple, etc.
- **Invencibilidad del que revive**: mientras estas reviviendo a un companero no
  recibes danio; se te quita en cuanto terminas (o dejas de revivir).
- **Auto-revivirse**: el noqueado puede revivirse a si mismo pagando
  **10 niveles de experiencia** (configurable). Boton clickeable en el chat.
- **Rendirse**: el noqueado puede rendirse y morir al instante. Boton clickeable.
- **Sin spam visual al revivir**: solo sonido + particulas, sin titulo verde ni
  mensaje en el chat.
- Sonidos suaves de amatista (chime / hit), nada estridente.
- Efecto Glowing para que los aliados vean al noqueado a traves de paredes.
- A prueba de teletransportes (`/tpa`, `/tp`, ender pearls, dimensiones,
  login/logout) sin desincronizacion.
- Inmune a danio mientras esta noqueado (excepto el vacio).
- Mobs hostiles pierden el target al noquear.

> Nota tecnica: al ser 100% server-side, **los demas jugadores ven al noqueado
> tumbado/arrastrandose**, pero el propio jugador noqueado se ve a si mismo de pie
> en su vista en primera persona (cambiar eso requeriria un mod de cliente).

## Instalacion

1. Coloca `revivemod-1.2.0.jar` en la carpeta `mods/` de tu servidor Fabric.
2. Tener instalado [Fabric API](https://modrinth.com/mod/fabric-api) (>= 0.102.0).
3. Reiniciar el servidor.

Se autogenera `config/revivemod.json`.

## Comandos

Jugador (cualquiera, solo estando noqueado):
```
/revive surrender   - rendirte y morir ahora
/revive self        - auto-revivirte pagando niveles de experiencia
```

Admin (op nivel 2):
```
/revive status                 - lista jugadores noqueados
/revive force <jugador>        - revivir instantaneo
/revive kill <jugador>         - matar al noqueado
/revive down <jugador>         - noquear (test)
/revive set time <segundos>    - duracion del contador
/revive set distance <bloques> - distancia de reanimacion
/revive set channel <ticks>    - duracion base del channel (20 ticks = 1s)
/revive set selfcost <niveles> - costo en niveles del auto-revivir
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
  "clearMobAggroOnDown": true,
  "crawlSlowness": 4,
  "allowSelfRevive": true,
  "selfReviveLevelCost": 10
}
```

## Licencia

MIT.
