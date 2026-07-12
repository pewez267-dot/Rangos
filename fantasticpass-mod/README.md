# Fantastic Pass — Forge 1.20.1

Premium Battle Pass mod for Forge 1.20.1: 100 tiers, free and premium rewards, a
hermetic anti-AFK time tracker, an admin color editor, and a Mixin-driven rank
nametag rendered above players.

This is the **buildable source project**, reconstructed from the shipped mod and
re-skinned with the "castle" Battle Pass UI.

## What changed in this version

### New castle GUI
The player-facing UI was completely rebuilt on the castle art pack:

- **Hub** (`PassHubScreen`, `battlepass_main`): the castle facade with four
  buttons — **Rewards**, **Premium**, **Quests** and **Info** — each a hover-lit
  hotspot aligned to the artwork.
- **Rewards** (`PassViewScreen`, `battlepass_reward`): the two gold-framed rows
  hold the **FREE** (top) and **PREMIUM** (bottom) reward of each tier, with the
  tier number between them, an XP progress bar and claim / page controls below.
- **Info & Milestones** (`PassInfoScreen`, `battlepass_quest` /
  `battlepass_quest_overview`): pass stats and a scrollable list of rank-reward
  milestones.

All screens share `CastleScreen`, which handles centred/scaled texture blitting,
a fade-in animation and texture→screen coordinate mapping.

### Animations & sounds (written in Java — the art pack ships none)
- Pulsing highlight on claimable tiers, hover glow, page-slide transition and a
  claim success/failure flash.
- Looping background music (`pass_music.ogg`) managed by `PassMusicManager` so it
  keeps playing across hub/sub-screens and stops when the UI closes. UI click and
  level-up sounds on interaction.

### Bug fixes
- **Claim desync:** the client no longer marks a tier claimed optimistically. A
  new server→client `ClaimResultPacket` returns the authoritative player state so
  the screen always matches the server (no more false "claimed" on a full
  inventory).
- **Localization:** the nametag level text and the whole UI now use translation
  keys. Added **Spanish** (`es_es`, `es_mx`) alongside English.

### Cleanup
Removed ~4 MB of unused legacy GUI textures (the old `jeqo/`, `sprites/` and a
3.87 MB `pass_bg.png`).

## Building

Requires JDK 17.

```bash
./gradlew build
```

The mod jar is produced at `build/libs/fantasticpass-1.0.0.jar`.

## How it was reconstructed

No source was available, so the shipped jar (SRG names) was remapped to official
Mojang mappings via ForgeGradle's `fg.deobf`, decompiled with Vineflower into
clean sources, and set up as a standard ForgeGradle + MixinGradle project. The
`PlayerRendererMixin` refmap is regenerated at build time.
