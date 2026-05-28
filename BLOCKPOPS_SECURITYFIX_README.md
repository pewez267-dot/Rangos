# BlockPops 1.5.0 → 1.5.6-securityfix

Patched build of `BlockPops - Fabric - 1.21.1-1.5.0`.

**Download:** [`BlockPops-Fabric-1.21.1-1.5.6-securityfix.jar`](./BlockPops-Fabric-1.21.1-1.5.6-securityfix.jar)

Drop this jar into your server's `mods/` folder **AND every client's `mods/`
folder** in place of the original `1.5.0` jar (or any older `1.5.x-securityfix`
build). Mod id, packet ids and save-data layout are unchanged. `/op` and
`/deop` while connected take effect on the GUI within ~1 second; no reconnect
required.

> **Both sides must update.** The settings gear and admin widgets are drawn
> client-side. Patching only the server jar will keep the visible UI on the
> *clients* unchanged. The server-side packet checks still reject mutations
> from non-OPs, but the clients will still *see* the admin GUI.

---

## Vulnerabilities fixed

### 1. `UnlockCollectionPacket` — missing OP check (CRITICAL)

The C2S packet behind the *"Unlock <collection>"* buttons of the **Cheats**
tab calls `unlockEntireCollection(...)` server-side, which gives the player
one box item per figure in the collection — without spending any tokens.
Every other admin packet uses `if (!player.method_5687(2)) { warn; return; }`.
This one didn't. To a server admin it looked exactly like *"the player has
infinite tickets"* — Funkos kept coming out and the token counter never
moved, because the token system was being bypassed entirely.

**Patch:** the same OP check is now injected at the start of
`lambda$handleServer$0` in `UnlockCollectionPacket`. Non-OPs are rejected
and a warning is written to `logs/latest.log`:

```
Player <name> tried to unlock collection without permission
```

### 2. `DropBoxPacket.verifyAndConsumeToken` — stale cooldown (MEDIUM)

`ServerTickHandler.processRegularTokens` only updates `nextRegularTokenTime`
*when a token is granted*. While a player sits at max tokens the stored
cooldown stays put and ends up far in the past. The next time they spend a
regular token, the next server tick sees `gameTime >= nextRegularTokenTime`
and immediately regenerates one — one effectively-free token every long-idle
cycle.

**Patch:** when a regular token is consumed, if the stored cooldown is
already in the past it is refreshed to `gameTime + cooldownTicks`.

### 3. Settings gear visible to all players + client-side `method_5687(2)` cannot be trusted (HIGH)

The original `CollectionSelectionScreen.init()` adds the `SETTINGS_ICON`
`LinkButton` for **every** player. The intended client-side gate (the
`SettingsScreen` admin widgets) bottoms out in
`field_22787.field_1724.method_5687(2)`. We confirmed at runtime, with
diagnostic logging, that on a real modpack with `luckperms`,
`vanilla-permissions`, `fabric-permissions-api-v0`, `fabric-essentials`,
`essential_commands` and friends installed, that call returns `true` on the
client even for a player who is not OP server-side. The server's
`method_5687(2)` check still works correctly (we have proof in the
`Player <name> tried to ... without permission` rejection log lines), but
the GUI was reading the lying client-side value.

**Patch:** the client-side gating is now driven by a server-stamped flag,
not by `method_5687(2)` on the client.

- `BlockPopsMod` gains a public static volatile boolean `LOCAL_ADMIN`,
  default `false`.
- `SyncServerConfigPacket.sendToPlayer(player)` (server) now appends one
  extra byte to the encoded buffer: `player.method_5687(2)`. This is
  evaluated on the server, where the check is trustworthy and matches
  the server-side packet rejection logic.
- `SyncServerConfigPacket.decode(buf)` (client) reads the trailing byte
  (gated on `class_2540.isReadable()` so unpatched-server packets still
  decode safely) and writes the value into `BlockPopsMod.LOCAL_ADMIN`.
- `ServerTickHandler.onServerTick` already runs once every
  `CHECK_INTERVAL` (= 20 ticks = 1 second) and iterates all online players
  for token regeneration. We additionally call
  `SyncServerConfigPacket.sendToPlayer(player)` for each player on every
  pass, so the trailing admin byte is re-evaluated and resent ~1×/sec.
  This makes `/op` and `/deop` while connected propagate to the client
  GUI within ~1 second without requiring a reconnect.
- All client-side GUI gating now reads `BlockPopsMod.LOCAL_ADMIN` instead
  of `field_22787.field_1724.method_5687(2)`:
  - `CollectionSelectionScreen.method_25426` — gear button creation+add
    is wrapped in `if (LOCAL_ADMIN)`.
  - `CollectionSelectionScreen.openSettingsScreen` — no-op for non-admin.
  - `SettingsScreen.method_25426` — calls `method_25419()` (close) and
    returns at the very top if `!LOCAL_ADMIN`.
  - `SettingsScreen.isAdmin()` — `return LOCAL_ADMIN;`
  - `SettingsScreen.canAccessCheats()` — `return LOCAL_ADMIN;`

This also closes the `Platform.isDevelopmentEnvironment()` admin bypass
that the original `isAdmin()` and `canAccessCheats()` had at the top — any
client launched with `-Dfabric.development=true` (some custom launchers do
this) used to be admin from the GUI's point of view; now that's just gone.

---

## Wire-format compatibility

The trailing admin byte is read with `class_2540.isReadable()` so the wire
format remains backwards-compatible:

| Server | Client | Behaviour |
| --- | --- | --- |
| 1.5.6 | 1.5.6 | Full feature, `/op` propagates within ~1s. |
| 1.5.6 | 1.5.0 | Old client just ignores the trailing byte. Server-side fixes still apply (UnlockCollection rejection, stale-cooldown refresh). |
| 1.5.0 | 1.5.6 | `isReadable()` is false, `LOCAL_ADMIN` stays `false`. The gear is hidden for everyone, including OPs (admins must update the server to get GUI access). |
| 1.5.0 | 1.5.0 | Original behaviour, all vulnerabilities open. |

---

## Defense layers, summarised

```
non-OP attempts something                              -> blocked by
-----------------------------------------------------------------------
sees the gear in the claw-machine UI                   -> fix 3   (gear creation gated on LOCAL_ADMIN)
clicks the gear via reflection / mod hack              -> fix 3   (openSettingsScreen no-ops)
opens SettingsScreen via another mod                   -> fix 3   (init closes the screen)
admin widgets render anyway                            -> fix 3   (isAdmin/canAccessCheats return LOCAL_ADMIN)
sends UpdateTokenSettingsPacket directly               -> server-side method_5687(2) check (already in 1.5.0)
sends UpdateGuaranteedResetHourPacket directly         -> server-side method_5687(2) check (already in 1.5.0)
sends UpdateHiddenCollectionsPacket directly           -> server-side method_5687(2) check (already in 1.5.0)
sends UpdateRemoteCollectionsPacket directly           -> server-side method_5687(2) check (already in 1.5.0)
sends ReloadTokensPacket directly                      -> server-side method_5687(2) check (already in 1.5.0)
sends UnlockCollectionPacket directly                  -> fix 1   (server-side method_5687(2) check)
spams DropBoxPacket while idling at max                -> fix 2   (cooldown refresh on consume)
gets /op'd while connected, expects gear to appear     -> fix 3   (LOCAL_ADMIN refreshes within ~1s)
gets /deop'd while connected, expects gear to vanish   -> fix 3   (LOCAL_ADMIN refreshes within ~1s)
```

---

## How the patches were applied

The jar is binary-patched with ASM at the bytecode level. The exact set of
classes touched:

| Class | Change |
| --- | --- |
| `com.theplumteam.BlockPopsMod` | Added `public static volatile boolean LOCAL_ADMIN` |
| `com.theplumteam.network.UnlockCollectionPacket` | OP gate at start of `lambda$handleServer$0` |
| `com.theplumteam.network.DropBoxPacket` | Stale-cooldown refresh in `verifyAndConsumeToken` |
| `com.theplumteam.network.SyncServerConfigPacket` | Stamp + read trailing admin byte |
| `com.theplumteam.server.ServerTickHandler` | Per-player resend of `SyncServerConfigPacket` every interval |
| `com.theplumteam.client.gui.CollectionSelectionScreen` | Gear-add wrapped in `if (LOCAL_ADMIN)`; `openSettingsScreen` no-ops for non-admin |
| `com.theplumteam.client.gui.SettingsScreen` | Close-on-init for non-admin; `isAdmin()` and `canAccessCheats()` rewritten to `return LOCAL_ADMIN;` |

Everything else (assets, mixins, `fabric.mod.json` deps, all other
classes) is byte-identical to the original `1.5.0` jar. The intermediary
mapping namespace, class file version (Java 21 / classfile major 65) and
Fabric manifest are preserved.

`fabric.mod.json` `version` is bumped to `1.5.6-securityfix`.

---

## Bandwidth cost of the periodic resend

`SyncServerConfigPacket` payload is ~30–100 bytes depending on hidden /
remote collection list sizes. At one send per second per online player
that's ~6 KB/min/player. For a server with 100 simultaneous players this
is ~10 KB/s of additional outbound traffic, well below noise floor for any
modded server.
