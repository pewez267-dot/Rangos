# Fantastic Essentials

A complete **server-side administrative essentials** mod for **Minecraft Forge 1.20.1**, by **Pewez777**.

It bundles homes, warps, teleport requests, virtual workstation menus, and a full set of admin
utilities — all configurable, with no external dependencies. It loads on both server and client.

## Build

Requires JDK 17.

```bash
./gradlew build
```

The finished jar will be in `build/libs/fantasticessentials-1.0.0.jar`. Drop it into your server's
(and/or client's) `mods/` folder.

## Features / Commands

| Category | Commands |
|----------|----------|
| Homes | `/sethome [name]`, `/home [name]`, `/delhome <name>`, `/homes` |
| Warps | `/setwarp <name>`, `/warp <name>`, `/delwarp <name>`, `/warps` |
| Back | `/back` (returns to your last position or death point) |
| TPA | `/tpa <player>`, `/tpahere <player>`, `/tpaccept [player]`, `/tpdeny [player]`, `/tpall` |
| Virtual menus | `/anvil`, `/cartographytable`, `/craft` (`/workbench`), `/enchanting`, `/grindstone`, `/loom`, `/smithing`, `/stonecutter`, `/enderchest` (`/ec`) |
| Player utilities | `/feed [player]`, `/heal [player]`, `/fly [player]`, `/flyspeed <0-10> [player]`, `/walkspeed <0-10> [player]`, `/glow [player]`, `/invulnerable [player]`, `/hat`, `/repair`, `/ping [player]`, `/whois [player]` |
| Item / sign editing | `/itemedit name <text>` / `name clear` / `lore add` / `lore set <line> <text>` / `lore remove <line>` / `lore clear`, `/signedit <line 1-4> [text]` |
| Admin | `/broadcast <message>` (`/bc`), `/commandspy [on\|off]`, `/fantasticessentials reload\|version`, `/mods`, `/msg <player> <message>` (`/tell`, `/w`, `/whisper`), `/reply <message>` (`/r`) |

> Note: the admin command is `/fantasticessentials` (the mod id). All other commands use the names above.

Teleport commands (home/warp/back/tpa) support a configurable **warmup** (cancelled on movement or
damage) and per-command **cooldowns**.

Text input for `/itemedit`, `/signedit` and messages supports legacy `&` color/format codes
(e.g. `&aGreen &lBold`).

## Configuration

On first run the mod creates `config/fantasticessentials/`:

- `config.json` — home limits, teleport warmup, cooldowns, TPA timeout, and the required operator
  permission level for every command node.
- `messages.json` — every player-facing message (with `{placeholders}` and `&` color codes). Edit and
  use `/fantasticessentials reload` to apply.

### Permissions

Uses **vanilla operator permission levels** (respected by any permission manager that adjusts a
player's command level). Each command's required level is configurable in `config.json` under
`permissionLevels` (`0` = everyone, `2`/`3`/`4` = operator tiers). By default, player commands are
available to everyone and administrative commands require operator.

### Player data

Homes and the "back" location are stored per player, and warps server-wide, inside the world save at
`<world>/fantasticessentials/`, so the data travels with your world.

## License

Copyright (c) 2026 Pewez777. All Rights Reserved. See `LICENSE`. Third-party attribution is in `NOTICE`.
