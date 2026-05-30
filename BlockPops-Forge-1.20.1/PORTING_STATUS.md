# BlockPops — Forge 1.20.1 Port — Status

This folder contains an in-progress port of **BlockPops** (originally by ThePlumTeam,
`com.theplumteam.*`) from **Fabric 1.21.1** to **Forge 1.20.1**.

The source was reconstructed by decompiling `BlockPops-Fabric-1.21.1 Fabric.jar`
(133 classes, ~14,200 lines, ~430 resource files). The complete decompiled reference
is kept under [`reference/decompiled-fabric-1.21.1/`](reference/decompiled-fabric-1.21.1)
so the remaining classes can be finished against the original logic.

> Attribution: original mod authored by **ThePlumTeam**. The `com.theplumteam` package
> namespace and MIT license are preserved.

---

## Target / toolchain

| Item | Value |
|------|-------|
| Minecraft | 1.20.1 |
| Loader | Forge 47.3.x (`javafml`) |
| Java | 17 |
| Mappings | Mojang official 1.20.1 |
| GeckoLib | `geckolib-forge-1.20.1:4.4.9` |
| Architectury API | `architectury-forge:9.2.14` |
| Build | ForgeGradle 6, Gradle wrapper 8.8 |

**Architecture decision:** instead of a multi-loader Architectury project, this is a
single-target **Forge** project that depends on the **Architectury API** as a normal
library. This keeps almost all of the original `dev.architectury.*` code
(`DeferredRegister`, `NetworkManager`, events) unchanged. The only Architectury feature
that required changes was `@ExpectPlatform`, which is inlined into Forge implementations.

---

## Build

```bash
cd BlockPops-Forge-1.20.1
./gradlew build
```

> NOTE: The project does **not compile yet** — the client layer (below) is unfinished and
> several server packets reference client-side handler classes that are still pending. Once
> the client classes are added, `./gradlew build` should produce the jar.

---

## Key 1.21.1 → 1.20.1 API changes applied

These are the systematic differences handled during the port (useful when finishing the rest):

- **Item data: DataComponents → NBT.** 1.21 uses `DataComponents.BLOCK_ENTITY_DATA` +
  `CustomData`. On 1.20.1 this is the `"BlockEntityTag"` NBT compound:
  - read: `BlockItem.getBlockEntityData(stack)`
  - write: `stack.getOrCreateTag().put("BlockEntityTag", tag)` / `BlockItem.setBlockEntityData(...)`
- **Networking: `RegistryFriendlyByteBuf` → `FriendlyByteBuf`.** `new FriendlyByteBuf(Unpooled.buffer())`.
  Architectury 9.x `NetworkManager` has no `registerS2CPayloadType` — use
  `registerReceiver(c2s()/s2c(), id, handler)` + `sendToPlayer/sendToServer`. S2C receivers
  are registered only on the physical client.
- **Block interaction:** 1.21 split `useItemOn`(→`ItemInteractionResult`) + `useWithoutItem`;
  1.20.1 has a single `use(...)` returning `InteractionResult`. The two were merged.
- **`BaseEntityBlock`:** no `codec()`/`MapCodec` (that is 1.20.5+). `playerWillDestroy` returns
  `void` on 1.20.1. `getCloneItemStack(BlockGetter, BlockPos, BlockState)`.
- **BlockEntity:** `saveAdditional/load/getUpdateTag/onDataPacket` take **no** `HolderLookup.Provider`
  on 1.20.1.
- **SavedData:** `save(CompoundTag)` (no provider); `SavedData.Factory<>(Supplier, Function<CompoundTag,T>, null)`.
- **GeckoLib 4.4.x (1.20.1) packages** differ from 4.7+ (1.21):
  `software.bernie.geckolib.core.animation.{AnimationController, RawAnimation, AnimationState, AnimatableManager}`,
  `software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache`,
  `software.bernie.geckolib.core.object.PlayState`.
  (`animatable.GeoBlockEntity`, `cache.GeckoLibCache`, `model.GeoModel`, `renderer.GeoBlockRenderer`,
  `util.GeckoLibUtil` are unchanged.)
- **Skins:** the `PlayerSkin` record (`getSkin()`) is 1.20.2+. On 1.20.1 use
  `PlayerInfo.getSkinLocation()` and `PlayerInfo.getModelName()` ("default"/"slim").
- **Player data:** Fabric stored per-player data via a UUID-keyed `SavedData`. The same
  approach is reused on Forge (`data/forge/StateSaverAndLoader`) — it survives death/dimension
  changes automatically. The Fabric offline-player favorite-color lookup that read each
  player `.dat`'s `cardinal_components` was replaced by reading our `SavedData`.

---

## Status by package

Legend: ✅ ported · ⏳ pending (client layer)

### Common / server — DONE (✅)
- `BlockPopsMod`
- `block/` — `PopBlockColor`, `BoxBlock`, `FigureBlock`, `ClawMachineBlock`
- `blockentity/` — `BoxBlockEntity`, `FigureBlockEntity`, `ClawMachineBlockEntity`
- `item/` — `GeoBlockItem`, `BoxBlockItem`
- `registry/` — `ModBlocks`, `ModItems`, `ModBlockEntities`, `ModCreativeTabs`
- `figure/` — `FigureType`, `FigureDefinition`, `FigureCollection`, `CollectionRegistry`,
  `BuiltInCollections`, `PlayerCollectionHelper`
- `server/` — `ServerTickHandler`, `ServerCollectionLoader`, `config/ServerConfig`, `config/WorldConfig`
- `data/` — `IPlayerDiscovery`, `PlayerDiscovery`, `PlayerDataManager`, `forge/StateSaverAndLoader`
- `network/` — `ModNetworking`, `TokenType`, and all 15 packets (DropBox, FigurePosition,
  SyncToken, SyncServerConfig, SyncDiscovery, SyncDynamicCollections, OpenFavoriteColorScreen,
  UnlockFigure, UnlockCollection, ClawMachineCollection, SetFavoriteColor, ReloadTokens,
  UpdateGuaranteedResetHour, UpdateTokenSettings, UpdateHiddenCollections, UpdateRemoteCollections,
  VersionCheck)
- `command/` — `ModCommands` + 7 subcommands
- `platform/PlatformHelper` (Forge)
- `forge/BlockPopsForge` (`@Mod` entry point)

### Client layer — PENDING (⏳)
These reference heavy MC client rendering / GUI APIs (largest API gap vs 1.20.1) and are not
yet ported. The exact original logic is in `reference/decompiled-fabric-1.21.1/`.

- `client/token/ClientTokenManager`
- `client/discovery/ClientDiscoveryManager`
- `client/config/ClientConfig`, `client/config/ClientServerConfig`
- `client/ClientHelpers`
- `client/remote/` — `RemoteAssetManager`, `RemoteTextureManager`, `RemoteModelManager`, `RemoteAnimationManager`
- `client/model/` — `FigureModel`, `FigureBlockModel`, `BoxBlockModel`, `ClawMachineBlockModel`
- `client/renderer/` — `FigureBlockRenderer`, `BoxBlockRenderer`, `ClawMachineBlockRenderer`,
  `FigureBlockItemRenderer`, `BoxBlockItemRenderer`, `ClawMachineBlockItemRenderer`,
  `FigureWidgetRenderer`, `FigureBoneTextureLayer`
- `client/particle/BlockParticleHelper` *(referenced by `BoxBlock`/`FigureBlock` destroy particles)*
- `client/gui/` — `SettingsScreen`, `FigurePositionScreen`, `CollectionSelectionScreen`,
  `FavoriteColorSelectionScreen`, `StarPatternCache`, `widget/*`, `util/*`
- `util/SkinModelDetector`
- `platform/forge/ClientPlatformHelperImpl` *(opens the screens; referenced by `PlatformHelper`)*
- `forge/BlockPopsForgeClient` *(client setup: register `BlockEntityRenderers`, item renderers via
  `IClientItemExtensions`, etc.)*

### Resources — DONE (✅)
All `assets/` (396 textures, 34 GeckoLib `.geo.json` models, 34 `.animation.json`, blockstates,
block/item models, `lang/en_us.json`) and `data/` (17 collections, recipes in 1.20.1 format,
block/item tags) ported under `src/main/resources/`.

---

## How to finish the client layer

1. Implement the client managers (`ClientTokenManager`, `ClientDiscoveryManager`,
   `ClientServerConfig`, `ClientConfig`, `ClientHelpers`) — these are mostly client-side state
   holders and unblock the `network/` package compilation.
2. Port the GeckoLib `GeoModel`s and `GeoBlockRenderer`s using the 4.4.x package names above.
   Replace the 1.21 skin API (`PlayerInfo.getSkin().texture()`) with `getSkinLocation()`.
3. Port the GUI screens to `net.minecraft.client.gui.GuiGraphics` (1.20.1) — most widget APIs are
   stable; watch for `renderBackground`, `Font.draw*`, and button builders.
4. Add `forge/BlockPopsForgeClient` and register renderers on `FMLClientSetupEvent` /
   `EntityRenderersEvent.RegisterRenderers`, plus item BEWLR via `IClientItemExtensions`.
5. `./gradlew build` and iterate on remaining mapping/API errors.
