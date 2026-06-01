# Login System 1.8 - Recipe Book Fix

This folder contains the patched source of the main mod class plus the change made
in **loginsystem-1.8.jar** (built for Minecraft 1.20.1 / Forge 47.x).

## What was changed and why

Players reported that **the recipe book opened but showed no recipes** for some
people. After fully decompiling the mod, it does **not** contain any code that
touches recipes, the recipe book, recipe packets or crafting game rules, so it is
not deleting recipes on purpose.

However, the recipe book is synced to the client **during the join sequence**, and
this mod freezes / teleports / clears the player at that exact moment (login gate).
On some sessions that leaves the client's recipe book desynced (empty).

### Fix
A `resyncRecipes(ServerPlayer)` method was added and is called right after a
successful `/login` or `/register`. It:

1. Re-sends the full recipe registry (`ClientboundUpdateRecipesPacket`).
2. Re-sends the player's recipe book (`ServerRecipeBook.sendInitialRecipeBook`).
3. Optionally unlocks every server recipe for the player.

### New config keys (config/loginsystem.properties)
```
# Re-send recipes right after login (fixes the empty recipe book). Default: true
resyncRecipesOnLogin=true
# Force-unlock ALL recipes for the player on login. Default: false
unlockAllRecipesOnLogin=false
```

If recipes still do not show after this, set `unlockAllRecipesOnLogin=true` and
every recipe will appear for everyone on login.

## How the jar was built
Only `LoginSystem.class` was recompiled (official mappings -> reobf to SRG) and
swapped back into the original jar; every other class and bundled library
(MySQL/MariaDB/jBCrypt) and all resources were kept byte-for-byte. To rebuild you
need a `compile-libs.jar` (the original jar with `com/example/loginsystem/LoginSystem.class`
removed) placed next to `build.gradle`, then run `./gradlew build`.
