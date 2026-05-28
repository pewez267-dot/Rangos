# BlockPops 1.5.0 → 1.5.7-securityfix

Patched build of `BlockPops - Fabric - 1.21.1-1.5.0`.

**Download:** [`BlockPops-Fabric-1.21.1-1.5.7-securityfix.jar`](./BlockPops-Fabric-1.21.1-1.5.7-securityfix.jar)

Drop this jar into your server's `mods/` folder **AND every client's `mods/`
folder** in place of any older 1.5.x build. Mod id, packet ids and save-data
layout are unchanged.

## What's new in 1.5.7

Adds a soft outdated-client check on top of all the 1.5.6 hardening:

- The patched client registers a no-op S2C channel `blockpops:version_check`.
- When a player joins, the server asks Architectury whether that channel is
  registered on the player's side. If not, the player is running a
  pre-1.5.7 jar, and the server **kicks them with a friendly message**:

  ```
  Tu mod BlockPops esta desactualizado.

  Descarga la version 1.5.7-securityfix
  y reemplaza el archivo en tu carpeta mods/.

  Tu cliente actual no es compatible con este servidor.
  ```

- The check is wrapped in `try { ... } catch (Throwable t) { warn; }`. If
  *anything* unexpected happens (Architectury API change, weird network
  state, player object in an odd state, etc.) the player joins normally.
  We never block a legitimate join.

So: worst case behaves exactly like 1.5.6. Best case the server rejects
clients with the old jar at the door, no more confusion of "I'm seeing the
admin GUI but nothing works".

## All fixes carried over from 1.5.0..1.5.6

1. **`UnlockCollectionPacket`** — server-side OP gate (was missing in
   1.5.0; let any client redeem an entire collection without spending tokens).
2. **`DropBoxPacket.verifyAndConsumeToken`** — refresh `nextRegularTokenTime`
   on consume so a stale cooldown can't grant a free instant regen.
3. **`SyncServerConfigPacket`** carries a server-stamped admin byte; the
   patched client stores it in `BlockPopsMod.LOCAL_ADMIN`.
4. **`ServerTickHandler.onServerTick`** re-sends `SyncServerConfigPacket`
   every tick interval (~1s/player). `/op` and `/deop` while connected
   propagate to the client GUI within ~1 second; no reconnect needed.
5. Client-side gating for the claw-machine settings gear,
   `openSettingsScreen`, `SettingsScreen.init`, `isAdmin()` and
   `canAccessCheats()` all read `LOCAL_ADMIN` instead of the client-local
   `method_5687(2)`. Immune to interference from `luckperms` /
   `vanilla-permissions` / `fabric-essentials` / `fabric-permissions-api`
   etc., which were causing the gear to render for non-OPs in the user's
   modpack.
6. **NEW (1.5.7):** `BlockPopsFabric` PLAYER_JOIN handler now calls
   `VersionCheckPacket.enforce(player)`, which kicks outdated clients with
   the message above (or no-ops if the check itself fails).

## Wire-format compatibility

| Server | Client | Behaviour |
| --- | --- | --- |
| 1.5.7 | 1.5.7 | All fixes active. |
| 1.5.7 | 1.5.0 | Client gets kicked at join with the "please update" message. |
| 1.5.7 | 1.5.6 | Same as 1.5.0 case (no `version_check` channel). Tell admins to update to 1.5.7 too. |
| 1.5.0 | 1.5.7 | No version_check on the server side either; client joins normally; falls back to vanilla 1.5.0 behaviour. |

## Bandwidth cost

Same as 1.5.6: ~6 KB/min/player from the periodic `SyncServerConfigPacket`.
The version-check itself is a single channel-presence query at join, no
extra packets.
