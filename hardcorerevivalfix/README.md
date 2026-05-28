# Hardcore Revival Fix (companion mod)

Companion mod for **Hardcore Revival** (`hardcorerevival` by BlayTheNinth) on Minecraft `1.21.1` (Fabric).

It fixes one specific bug and adds a mandatory client version check.

## What it fixes

**Bug:** Right-click revive sometimes fails after the knocked-out player teleports to the rescuer (`/tpa`).

**Why:** The original mod only syncs the knockout state through `ChunkTrackingEvent.Start`, which fires when a *player* starts tracking a *chunk*. When the knocked-out player teleports into chunks the rescuer is already tracking, no new chunk-tracking event fires for the rescuer, so the rescuer's client never receives the `HardcoreRevivalDataMessage`. On his client `PlayerHardcoreRevivalManager.isKnockedOut(target)` returns `false`, the right-click is not interpreted as a rescue, and the rescue silently does nothing.

**Fix:** This companion listens to fabric-api's `EntityTrackingEvents.START_TRACKING`. Whenever any player starts tracking a knocked-out player as an entity, the server immediately re-sends `KnockoutSyncHandler.sendHardcoreRevivalData(...)` so the rescuer's client knows the target is knocked out. It also re-syncs on `ServerEntityEvents.ENTITY_LOAD` to cover dimension changes / respawns.

## Mandatory client check

Modeled on BlockPops' `VersionCheckPacket`:

- Server registers an S2C payload type `hardcorerevivalfix:version_check_v1`.
- Client (with this mod installed) registers a no-op receiver for that ID.
- On `ServerPlayConnectionEvents.JOIN` the server calls `ServerPlayNetworking.canSend(player, ID)`. If the player has not registered the receiver, they are kicked with a Spanish update message.

The packet is never actually sent — its registered-ness is the proof.

To rotate the required client version, change the path of `VersionCheckPayload.ID` (e.g. `version_check_v2`) and rebuild. Old installs of this companion mod will then also be kicked.

## Building

```bash
./gradlew build
```

The built jar lands in `build/libs/hardcorerevivalfix-<version>.jar` and is also copied to the repo root as `hardcorerevivalfix-1.0.0.jar`.

Requires Java 21.

## Install

Drop both jars into `mods/`:

- `hardcorerevival-fabric-1.21.1-21.1.14.jar` (the original mod, untouched)
- `hardcorerevivalfix-1.0.0.jar` (this companion)

Server-only would fix the rescue bug but would kick all clients. Distribute this companion to every player on the server.
