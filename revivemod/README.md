# Revive Mod

Mod **server-side** (con dos mixins servidor-side, los clientes vanilla siguen funcionando sin instalar nada) para **Minecraft 1.21.1 Fabric, Java 21**.

Cuando un jugador moriria, en lugar de morir queda **noqueado tumbado en el
suelo**. Aparece automaticamente un **menu en pantalla** con dos botones:
**Rendirse** (cama roja) o **Auto-revivir** (botella de XP, paga niveles).
Ademas otros jugadores pueden revivirlo con click derecho.

## Caracteristicas

- **Tumbado de verdad sin jitter**: usa la pose `SLEEPING` (no `SWIMMING`),
  que cliente y servidor comparten via data tracker, asi no hay rebote
  vertical en tercera persona. La hitbox se mantiene normal para que los
  aliados puedan acercarse y mirar el cuerpo.
- **Menu en pantalla** con botones para rendirse o auto-revivir. Si lo cierras
  con ESC se reabre automaticamente al instante (no flicker).
- **Mensaje al noquear**: `Jugador ha sido noqueado por <entidad/jugador>`.
- **Revivir con click derecho** sobre el jugador noqueado (dentro de 3 bloques
  y mirandolo). **Mas rapido entre varios**: 2 jugadores = doble velocidad,
  3 = triple, etc.
- **Invencibilidad del que revive**: mientras estas reviviendo a un companero
  no recibes danio; se quita en cuanto terminas.
- **Auto-revivir**: el noqueado paga XP (default **10 niveles**, configurable).
- **Rendirse**: el noqueado puede rendirse y morir al instante.
- **Efectos al revivir bonitos**: chime + bell undertone + level-up suave +
  particulas en capas (corazones, aldeano feliz, encantamiento, end-rod, glow).
  Sin titulo verde ni mensaje en chat.
- Sonidos suaves de amatista. Glowing en el noqueado para que sus aliados lo
  vean a traves de paredes. A prueba de teletransportes (`/tpa`, `/tp`,
  ender pearls, dimensiones, login/logout). Inmune a danio mientras esta
  noqueado (excepto el vacio). Mobs hostiles pierden el target al noquear.

## Instalacion

1. Coloca `revivemod-1.3.0.jar` en la carpeta `mods/` del servidor Fabric.
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
