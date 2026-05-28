# BlockPops 1.5.0 → 1.5.6-securityfix

Patched build of `BlockPops - Fabric - 1.21.1-1.5.0`.

**Download:** [`BlockPops-Fabric-1.21.1-1.5.6-securityfix.jar`](./BlockPops-Fabric-1.21.1-1.5.6-securityfix.jar)

Drop this jar into your server's `mods/` folder **AND every client's `mods/`
folder** in place of the original `1.5.0` jar. Mod id, packet ids and
save-data layout are unchanged. `/op` and `/deop` while connected take
effect on the GUI within ~1 second; no reconnect required.

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
This one didn't.

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
`method_5687(2)` check still works correctly (proven by the
`Player <name> tried to ... without permission` rejection log lines), but
the GUI was reading the lying client-side value.

**Patch:** the client-side gating is driven by a server-stamped flag,
not by `method_5687(2)` on the client.

- `BlockPopsMod` gains a public static volatile boolean `LOCAL_ADMIN`,
  default `false`.
- `SyncServerConfigPacket.sendToPlayer(player)` (server) appends one
  extra byte to the encoded buffer: `player.method_5687(2)`.
- `SyncServerConfigPacket.decode(buf)` (client) reads the trailing byte
  (gated on `class_2540.isReadable()`) and writes it into
  `BlockPopsMod.LOCAL_ADMIN`.
- `ServerTickHandler.onServerTick` resends `SyncServerConfigPacket` once
  per second per online player, so `/op` and `/deop` while connected
  propagate to the client GUI within ~1 second without a reconnect.
- All client-side GUI gating reads `BlockPopsMod.LOCAL_ADMIN` instead of
  `field_22787.field_1724.method_5687(2)`:
  - `CollectionSelectionScreen.method_25426` — gear button creation
  - `CollectionSelectionScreen.openSettingsScreen` — no-op for non-admin
  - `SettingsScreen.method_25426` — close immediately for non-admin
  - `SettingsScreen.isAdmin()` — `return LOCAL_ADMIN;`
  - `SettingsScreen.canAccessCheats()` — `return LOCAL_ADMIN;`

This also closes the `Platform.isDevelopmentEnvironment()` admin bypass
that the original `isAdmin()` and `canAccessCheats()` had at the top.

---

## Wire-format compatibility

| Server | Client | Behaviour |
| --- | --- | --- |
| 1.5.6 | 1.5.6 | All fixes active. |
| 1.5.6 | 1.5.0 | Old client just ignores the trailing byte. Server-side fixes still apply. |
| 1.5.0 | 1.5.6 | `isReadable()` is false on the client, `LOCAL_ADMIN` stays `false`. The gear is hidden for everyone, including OPs. |
| 1.5.0 | 1.5.0 | Original behaviour, all vulnerabilities open. |

---

## How the patches were applied

The jar is binary-patched with ASM at the bytecode level. Classes touched:

- `com.theplumteam.BlockPopsMod` — added `LOCAL_ADMIN` field
- `com.theplumteam.network.UnlockCollectionPacket` — server OP gate
- `com.theplumteam.network.DropBoxPacket` — stale-cooldown refresh
- `com.theplumteam.network.SyncServerConfigPacket` — admin byte stamp/read
- `com.theplumteam.server.ServerTickHandler` — periodic resend
- `com.theplumteam.client.gui.CollectionSelectionScreen` — gear gate
- `com.theplumteam.client.gui.SettingsScreen` — close-on-init + hardened
  `isAdmin()` / `canAccessCheats()`

Everything else is byte-identical to the original 1.5.0 jar.
`fabric.mod.json` `version` is bumped to `1.5.6-securityfix`.
