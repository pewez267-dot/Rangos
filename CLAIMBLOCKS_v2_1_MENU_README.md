# Claim Blocks Admin v2.1 MENU - Minecraft 1.21.1 Fabric

5-tier region-protection mod with full GUI menu, 8 protection flags,
OP-only commands, NBT persistence and animated border visualisation.

## Download

[claimblocks-admin-v2_1-MENU.jar](https://github.com/pewez267-dot/Rangos/raw/main/claimblocks-admin-v2_1-MENU.jar)

Drop the file into your `mods/` folder. Requires:
- Minecraft 1.21.1
- Fabric Loader >= 0.16.0
- Fabric API
- Java 21+

## Tier reference

| Tier | Radius | Cube side |
|------|--------|-----------|
| 1    | 10     | 21x21x21  |
| 2    | 20     | 41x41x41  |
| 3    | 30     | 61x61x61  |
| 4    | 40     | 81x81x81  |
| 5    | 50     | 101x101x101 |

The protected area is a cube centred on the placed block.

## Commands (all require OP / permission level 2)

| Command | Description |
|---------|-------------|
| `/claim give <player> <tier>` | Give a tier 1-5 claim block |
| `/claim clear <player>` | Remove every claim owned by the player |
| `/claim remove` | Remove the claim you are standing in (owner / OP) |
| `/claim menu` | Open the GUI of the claim you are standing in |
| `/claim list` | List your claims (or all claims for OPs) |
| `/claim info` | Print details of the claim you are standing in |

Right-click a claim block to also open the menu (owners and members only).

## GUI menu layout (vanilla 9x6 chest)

- Slot 4: claim info (owner, tier, area, members)
- Slots 10-17: 8 flag toggles (lime = ON, gray = OFF)
- Slots 27-35: member heads (right-click a head to remove)
- Slot 47: close
- Slot 49: add member (closes GUI, type a name in chat or `cancel`)
- Slot 53: shift-click to delete the claim and refund the block

## Protection flags

| Flag | What it does (`true` = allow non-members) |
|------|-------------------------------------------|
| CREEPING | place blocks |
| BREAKING | break blocks |
| EXPLOSIONS | explosions inside claim |
| FIRE | fire / lava spread |
| MOBS | mob spawning (default ON) |
| PVP | player vs player |
| MOB_DAMAGE | mobs can damage entities (default ON) |
| TRESPASSER_ALERTS | DM the owner when an unauthorised player walks in |

## Persistence

Claims are stored as compressed NBT in
`<world>/data/claimblocks_data.dat`. Saved automatically:
- on every flag change / member change
- on `/claim` commands
- every 5 minutes (auto-save tick)
- on server stop

## Source

Gradle / Loom project: `claimblocks-mod/`. To rebuild:

```
JAVA_HOME=/path/to/jdk-21 gradle -p claimblocks-mod build
```

Output goes to `claimblocks-mod/build/libs/`.
