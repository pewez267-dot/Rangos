# BlockPops 1.5.0 → 1.5.1-securityfix

Patched build of `BlockPops - Fabric - 1.21.1-1.5.0`.

**Download:** [`BlockPops-Fabric-1.21.1-1.5.1-securityfix.jar`](./BlockPops-Fabric-1.21.1-1.5.1-securityfix.jar)

Drop this jar into your server's `mods/` folder (and clients' `mods/` folder) **in
place of** the original `1.5.0` jar. Mod id, packet ids and save-data layout are
unchanged, so existing worlds keep working with no migration.

---

## What was broken

The user-reported symptom was *"users can change the cooldown of tickets,
making them practically have infinite tickets to get the funkos"*. After
decompiling and reviewing the network/permission layer, the actual root cause
turned out to be a missing permission check on a different packet plus a
secondary cooldown-refresh bug. Both are fixed in this build.

### 1. `UnlockCollectionPacket` — missing OP check (CRITICAL)

`com.theplumteam.network.UnlockCollectionPacket.handleServer` is the C2S
packet behind the *"Unlock <collection>"* buttons in the **Cheats** tab of
the Settings screen. Its server handler immediately calls
`unlockEntireCollection(...)`, which **gives the player one box item for
every figure in the collection** without consuming any tokens.

Every other admin packet in the mod gates itself with
`player.method_5687(2)` (OP level 2):

```java
// ReloadTokensPacket / UpdateTokenSettingsPacket / UpdateHiddenCollectionsPacket / ...
if (!player.method_5687(2)) {
    LOGGER.warn("Player {} tried to ... without permission", player.method_5477().getString());
    return;
}
```

`UnlockCollectionPacket` did not. The Cheats tab is hidden in the GUI for
non-OP players, but a modified or scripted client can still send the packet
directly to the server, and the server will happily hand out the entire
collection. To a server admin this looks exactly like the player has
"infinite tickets" — they keep producing Funkos non-stop without their
token counter going down, because the token system is being skipped
entirely.

**Patch:** the same OP check is now injected at the start of
`lambda$handleServer$0` in `UnlockCollectionPacket`. Non-OP players that try
to send this packet are rejected with a warning in the server log:

```
Player <name> tried to unlock collection without permission
```

### 2. `DropBoxPacket.verifyAndConsumeToken` — stale cooldown (MEDIUM)

`ServerTickHandler.processRegularTokens` only updates
`nextRegularTokenTime` **when a token is granted** (i.e. when
`regularTokens < maxTokens`). While the player sits at max tokens, the
stored cooldown never advances. After enough idle time it ends up far in
the past.

The next time the player spends a regular token, the very next server tick
sees `gameTime >= nextRegularTokenTime` and immediately regenerates one,
giving them one effectively-free token every "long idle" cycle.

**Patch:** when a regular token is successfully consumed, if the stored
`nextRegularTokenTime` is already in the past, it is now refreshed to
`gameTime + cooldownTicks` so a fresh cooldown always starts on consume:

```java
long gameTime = player.serverLevel().getGameTime();
if (discovery.getNextRegularTokenTime() <= gameTime) {
    long cd = ServerConfig.getInstance().getRegularTokenCooldownHours() * 60L * 60L * 20L;
    discovery.setNextRegularTokenTime(gameTime + cd);
}
```

Behaviour for players who are already below max is unchanged (their
cooldown is already in the future, so the `if` is a no-op).

---

## How the patch was applied

The jar is binary-patched with ASM at the bytecode level — only the two
affected methods are touched. Everything else (assets, mixins,
`fabric.mod.json` deps, all other classes) is byte-identical to the
original 1.5.0 jar. The intermediary mapping namespace, class file version
(Java 21 / classfile major 65) and Fabric manifest are preserved so the
loader treats it the same way as the upstream build.

`fabric.mod.json` `version` is bumped to `1.5.1-securityfix` so it shows up
distinctly in the Fabric mod list.
