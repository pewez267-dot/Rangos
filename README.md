# Fantastic Shortcuts

Advanced, fully editable **global command shortcuts** for Minecraft **Forge 1.20.1** (Java 17), by
**Pewez777**.

Turn long commands into short custom aliases, e.g. `/gamemode creative` -> `/gc`. This is a **Forge
mod** (not a plugin); it uses only the Forge API, Brigadier and the vanilla command system, with an
optional read-only LuckPerms integration.

- Mod name: **Fantastic Shortcuts**
- Mod id: `fantasticshortcuts`
- Base command: `/fshortcuts`
- Chat prefix: `[F-Shortcuts]`

## Build

Requires JDK 17. The project is a standard ForgeGradle MDK with the Gradle wrapper included.

```bash
./gradlew build
```

Output jar: `build/libs/fantasticshortcuts-1.0.0.jar`.

## Philosophy / Golden Rule of Security

Fantastic Shortcuts is **not** a permission system and never replaces LuckPerms.

- It only intercepts command input and translates shortcuts into real commands.
- The real command runs **as the player** - never as console, never with elevated permissions.
- The alias node also **inherits the permission requirement of the original command**, so a player
  who cannot use `/gamemode creative` neither sees nor can run `/gc`.
- It never creates, grants or modifies permissions. Vanilla / mods / LuckPerms stay fully in control.

Example: `/gc` -> `/gamemode creative`. If the player lacks permission for `/gamemode creative`,
then `/gc` is rejected too.

## Usage

All management is done through the in-game GUI (operator level 4):

| Command | Action |
|---------|--------|
| `/fshortcuts` | Open the editor GUI on the **Lista** tab (browse / edit / delete) |
| `/fshortcuts create` | Open the editor GUI on the **Crear** tab (create a new shortcut) |
| `/fshortcuts reload` | Reload shortcuts from disk |

There are no chat commands for creating/editing/deleting - everything happens in the GUI, exactly
like FantasticCrates / FantasticSpawners. The mod ships with **zero** predefined shortcuts.

After creating a shortcut it is registered live and the command tree is resynced to online players.
Renamed or removed shortcuts fully apply after a vanilla `/reload` or a server restart (Brigadier
cannot remove existing nodes at runtime).

### Dynamic variables

A target command may contain `{args}`, replaced by whatever the player types after the alias:

```
/tpp {args}     ->  typing "/tpp Steve" runs "/tp Steve"
```

If no `{args}` placeholder is present and `allowArguments` is true, the typed text is appended.

## Files

On first run the mod creates `config/fantasticshortcuts/`:

- `config.toml` - behaviour toggles: `enableReplaceMode`, `shortcutPriority`, `auditEnabled`,
  `warnOnConflict`, `luckPermsIntegration`.
- `shortcuts.json` - the list of shortcuts. **It starts empty** - this is a system for you to define
  your own shortcuts through the in-game GUI. Nothing is predefined.
- `audit/audit-YYYY-MM-DD.log` - audit log of CREATE/EDIT/DELETE/EXECUTE, conflicts, denied access
  and injection attempts.

## Security

- Protected commands can never be aliased or targeted: `/fshortcuts`, `/stop`, `/reload`, `/help`,
  `/op`, `/deop`, `/ban`, `/whitelist`, `/kick`, `/execute`, and other critical commands.
- Aliases are validated (single word, safe characters).
- Command injection (newlines, `;`, etc.) is detected and blocked, and logged to the audit trail.
- No permission elevation, no console execution, no OP/LuckPerms bypass.

## Conflicts

If an alias collides with an existing command, the conflict is detected and logged (no crash). The
`shortcutPriority` config decides whether the shortcut or the original command wins.

## LuckPerms integration (optional)

If the `luckperms` mod is installed, Fantastic Shortcuts reads the player's primary group (read-only)
for audit context, via the official LuckPerms API. It never modifies permissions. If LuckPerms is
absent, everything works normally.

## Project structure

```
src/main/java/com/pewez/fantasticshortcuts/
  commands/       - /fshortcuts management command
  client/         - client compatibility marker (no client mod needed)
  server/         - server lifecycle hooks
  shortcuts/      - Shortcut model + ShortcutManager (CRUD)
  brigadier/      - command registration, translation and execution
  integration/luckperms/ - optional read-only LuckPerms integration
  gui/            - server-side chest GUI
  audit/          - audit events + log
  security/       - protected list, validation, injection checks
  config/         - config.toml definition
  storage/        - shortcuts.json persistence
  util/           - chat prefix helpers
```

## Note on "replace mode" (client command-tree hiding)

`replaceOriginal` is fully stored, configurable and audited. Visually hiding the original command
from a client's tab/suggestions (while keeping the alias working) requires per-client command-tree
filtering. This is intentionally implemented as a documented, opt-in flag rather than a forced
behaviour, to avoid ever breaking command execution. The translation/permission core is complete and
works regardless of this flag.

## License

Copyright (c) 2026 Pewez777. All Rights Reserved. See `LICENSE`.
