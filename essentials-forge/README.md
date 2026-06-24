# Essentials (Forge 1.20.1 port)

A complete Forge **1.20.1** port of [Fabric Essentials](https://modrinth.com/mod/fabric-essentials)
(`1.4.10+1.21.8`) by **Drex**. The original mod is MIT licensed; this port keeps the same license
and reimplements every command using the Forge / vanilla 1.20.1 API (no Fabric-only libraries).

This is a **server-side administrative** mod. It also loads fine on the client, so it can be used in
single player and on clients connecting to a server (it simply registers its commands on the logical
server, like the original).

## Build

Requires JDK 17.

```bash
./gradlew build
```

The finished jar will be in `build/libs/essentials-1.4.10-forge.jar`. Drop it into your server's
(and/or client's) `mods/` folder.

## Features / Commands

All original Fabric Essentials commands were ported:

| Category | Commands |
|----------|----------|
| Homes | `/sethome [name]`, `/home [name]`, `/delhome <name>`, `/homes` |
| Warps | `/setwarp <name>`, `/warp <name>`, `/delwarp <name>`, `/warps` |
| Back | `/back` (returns to your last position or death point) |
| TPA | `/tpa <player>`, `/tpahere <player>`, `/tpaccept [player]`, `/tpdeny [player]`, `/tpall` |
| Virtual menus | `/anvil`, `/cartographytable`, `/craft` (`/workbench`), `/enchanting`, `/grindstone`, `/loom`, `/smithing`, `/stonecutter`, `/enderchest` (`/ec`) |
| Player utilities | `/feed [player]`, `/heal [player]`, `/fly [player]`, `/flyspeed <0-10> [player]`, `/walkspeed <0-10> [player]`, `/glow [player]`, `/invulnerable [player]`, `/hat`, `/repair`, `/ping [player]`, `/whois [player]` |
| Item / sign editing | `/itemedit name <text>` / `name clear` / `lore add` / `lore set <line> <text>` / `lore remove <line>` / `lore clear`, `/signedit <line 1-4> [text]` |
| Admin | `/broadcast <message>` (`/bc`), `/commandspy [on\|off]`, `/essentials reload\|version`, `/mods`, `/msg <player> <message>` (`/tell`, `/w`, `/whisper`), `/reply <message>` (`/r`) |

Teleport commands (home/warp/back/tpa) support a configurable **warmup** (cancelled on movement or
damage) and per-command **cooldowns**.

Text input for `/itemedit`, `/signedit` and messages supports legacy `&` color/format codes
(e.g. `&aGreen &lBold`).

## Configuration

On first run the mod creates `config/essentials/`:

- `config.json` — home limits, teleport warmup, cooldowns, TPA timeout, and the required operator
  permission level for every command node.
- `messages.json` — every player-facing message (with `{placeholders}` and `&` color codes). Edit and
  use `/essentials reload` to apply.

### Permissions

The original used the Fabric permissions API. Because Forge has no universal permission API that works
without extra mods, this port uses **vanilla operator permission levels** (which any permission manager
that adjusts a player's command level will respect). Each command's required level is configurable in
`config.json` under `permissionLevels` (`0` = everyone, `2`/`3`/`4` = operator tiers). By default,
player commands (home, warp, back, tpa, msg, menus...) are available to everyone and administrative
commands require operator.

### Player data

Homes and the "back" location are stored per player, and warps server-wide, inside the world save at
`<world>/essentials/`, so the data travels with your world.

## Porting notes

- Fabric mixins were replaced with Forge events (`RegisterCommandsEvent`, `CommandEvent` for command
  spy, `LivingDeathEvent` for the back-on-death point, `LivingHurtEvent` for warmup cancellation,
  `ServerTickEvent` for warmup/TPA timers).
- The Fabric-only library dependencies (`sgui`, `placeholder-api`, `message-api`, `player-data-api`,
  `fabric-permissions-api`, `common-protection-api`) are **not** required — their functionality is
  reimplemented with vanilla/Forge equivalents, so there are no extra dependencies and no Fabric
  incompatibilities.
- The original's data importers for other Fabric mods (KiloEssentials / EssentialCommands) were not
  ported, since those read Fabric-specific files that do not exist on a Forge server.

## Credits

- Original mod: **Fabric Essentials** by Drex — https://github.com/DrexHD (MIT).
- Forge 1.20.1 port: reimplemented for Forge with the same MIT license.
