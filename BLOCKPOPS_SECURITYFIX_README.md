# BlockPops 1.5.0 → 1.5.2-securityfix

Patched build of `BlockPops - Fabric - 1.21.1-1.5.0`.

**Download:** [`BlockPops-Fabric-1.21.1-1.5.2-securityfix.jar`](./BlockPops-Fabric-1.21.1-1.5.2-securityfix.jar)

Drop this jar into your server's `mods/` folder **AND every client's `mods/`
folder** in place of the original `1.5.0` jar (or any older `1.5.x-securityfix`
build). Mod id, packet ids and save-data layout are unchanged.

> **IMPORTANT — both sides must update**
>
> The settings gear, the cheats tab and the cooldown sliders all live in the
> client GUI. Patching only the server jar will leave the visible UI on the
> *clients* unchanged. The server-side packet check still rejects mutations
> from non-OPs, but the players will continue to *see* the gear / be able to
> *type* values in the boxes (the value just won't actually take effect on
> the server).
>
> If you are testing the gear visibility, the **player whose screen you are
> looking at** must have this jar installed and must **not be OP** on the
> server they are connected to. In single-player worlds with **Allow Cheats**
> ON, the host is permission level 4 — that counts as OP and the gear *will*
> show by design.

---

## What was broken (and is now fixed)

### 1. `UnlockCollectionPacket` — missing OP check (CRITICAL)

The C2S packet behind the *"Unlock <collection>"* buttons of the **Cheats**
tab calls `unlockEntireCollection(...)` server-side, which gives the player
one box item per figure in the collection — without spending any tokens.
Every other admin packet in the mod uses

```java
if (!player.method_5687(2)) {
    LOGGER.warn("Player {} tried to ... without permission", ...);
    return;
}
```

This one didn't. The Cheats tab is hidden in the GUI for non-OPs, but a
modified or scripted client can send the packet directly to the server, and
the server happily hands out every figure. To a server admin this looks
exactly like *"the player has infinite tickets"* — Funkos keep coming out
and the token counter never moves, because the token system is being
bypassed entirely.

**Patch:** the same OP check is now injected at the start of
`lambda$handleServer$0` in `UnlockCollectionPacket`. Non-OPs are rejected
and a warning is written to `logs/latest.log`:

```
Player <name> tried to unlock collection without permission
```

### 2. `DropBoxPacket.verifyAndConsumeToken` — stale cooldown (MEDIUM)

`ServerTickHandler.processRegularTokens` only updates
`nextRegularTokenTime` *when a token is granted* (`regularTokens < maxTokens`).
While the player sits at max, the stored cooldown never advances and ends
up far in the past. The next time they spend a regular token, the next
server tick sees `gameTime >= nextRegularTokenTime` and immediately
regenerates one — one effectively-free token every long-idle cycle.

**Patch:** when a regular token is consumed, if the stored cooldown is
already in the past it is refreshed to `gameTime + cooldownTicks`:

```java
long gameTime = player.serverLevel().getGameTime();
if (discovery.getNextRegularTokenTime() <= gameTime) {
    long cd = ServerConfig.getInstance().getRegularTokenCooldownHours() * 60L * 60L * 20L;
    discovery.setNextRegularTokenTime(gameTime + cd);
}
```

### 3. Settings gear visible to non-OPs in the claw-machine GUI (MEDIUM)

`CollectionSelectionScreen.method_25426` (the `init()` of the GUI opened by
`blockpops:claw_machine_block`) added the `SETTINGS_ICON` `LinkButton`
unconditionally for every player. Clicking it opens `SettingsScreen`,
where the per-tab admin widgets are gated client-side — but exposing the
entry point at all is a foot-gun and looks broken to non-OP users.

**Patch:** the gear creation+registration is now wrapped in:

```java
if (this.field_22787 != null
    && this.field_22787.field_1724 != null
    && this.field_22787.field_1724.method_5687(2)) {
    this.method_37063(/* settings LinkButton */);
}
```

The Discord button is positioned relative to `settingsButtonX`, which is
still computed on both branches, so the icon row re-flows cleanly when the
gear is hidden.

### 4. `SettingsScreen.isAdmin()` — Architectury dev-mode bypass (HIGH, root-cause of "I can still edit cooldown")

This is the one that actually let non-OPs see the admin widgets. The
original `isAdmin()` decompiled to:

```java
private boolean isAdmin() {
    if (SettingsScreen.isDevelopmentMode()) {       // <-- bypass
        return true;
    }
    if (this.field_22787 != null && this.field_22787.field_1724 != null) {
        return this.field_22787.field_1724.method_5687(2);
    }
    return false;
}

private static boolean isDevelopmentMode() {
    return Platform.isDevelopmentEnvironment();      // Architectury
}
```

`canAccessCheats()` had the exact same shape. `Platform.isDevelopmentEnvironment()`
on Fabric returns `FabricLoader.getInstance().isDevelopmentEnvironment()`,
which is normally `false` in a packaged jar, **but** any of the following
will flip it to `true` and turn every connected client into an admin from
the GUI's point of view:

- launching the game with `-Dfabric.development=true`,
- launching with `-Dfabric.gameJarPath=...` (some custom launchers do this),
- shipping the mod inside a dev/IDE-like environment,
- a buggy launcher that sets the dev flag.

When that flag is `true`:
- `isAdmin()` and `canAccessCheats()` return `true` for any player,
- the admin widgets and cheats tab render normally,
- the **Save** button is enabled,
- clicking Save calls `ClientServerConfig.update(...)` (so the local UI
  shows the new cooldown / max / reset hour),
- `UpdateTokenSettingsPacket` is sent to the server.

The server **does** still reject non-OPs (the server-side check is the same
`method_5687(2)`), so the actual gameplay cooldown doesn't change — but the
client UI looks like it did, and the players think they got away with it
until they realise the server kept the old value. Net effect: enormous
confusion plus the visible cheats-tab buttons that *could* hit
`UnlockCollectionPacket` on an unpatched server (see fix 1).

**Patch:** both `isAdmin()` and `canAccessCheats()` are rewritten to the
strict form, no dev-mode bypass:

```java
return this.field_22787 != null
    && this.field_22787.field_1724 != null
    && this.field_22787.field_1724.method_5687(2);
```

### 5. `SettingsScreen` — close-on-init for non-OPs (defense-in-depth)

`SettingsScreen.method_25426` now also closes itself immediately if the
local player is not OP, even before any widget is created:

```java
if (mc != null && mc.player != null && !mc.player.hasPermissionLevel(2)) {
    this.close();
    return;
}
super.init();
// ...
```

Combined with the gear-hide and the hardened `isAdmin()`, a non-OP cannot
see, open, or interact with the admin GUI under any path.

### 6. `CollectionSelectionScreen.openSettingsScreen()` — no-op for non-OPs

Final belt: even if some other code or a buggy widget calls the private
`openSettingsScreen()` method, it now returns immediately for non-OPs
without opening any screen.

---

## Defense layers, summarised

```
non-OP attempts something                  -> blocked by
-----------------------------------------------------------------------
sees the gear in the claw-machine UI       -> fix 3  (gear creation gated)
clicks the gear via reflection / mod hack  -> fix 6  (openSettingsScreen no-ops)
opens SettingsScreen via another mod       -> fix 5  (init closes the screen)
admin widgets render anyway                -> fix 4  (isAdmin()/canAccessCheats() strict)
sends UpdateTokenSettingsPacket directly   -> server-side method_5687(2) check (already in 1.5.0)
sends UnlockCollectionPacket directly      -> fix 1  (server-side method_5687(2) check)
spams DropBoxPacket while idling at max    -> fix 2  (cooldown refresh on consume)
```

---

## How the patch was applied

The jar is binary-patched with ASM at the bytecode level — only the four
affected classes (`UnlockCollectionPacket`, `DropBoxPacket`,
`CollectionSelectionScreen`, `SettingsScreen`) are touched. All other
classes, assets, mixins, and `fabric.mod.json` deps are byte-identical to
the original 1.5.0 jar. The intermediary mapping namespace, class file
version (Java 21 / classfile major 65) and Fabric manifest are preserved
so the loader treats it the same way as the upstream build.

`fabric.mod.json` `version` is bumped to `1.5.2-securityfix`.
