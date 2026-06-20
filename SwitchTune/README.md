# SwitchTune

A native Android utility (Kotlin + Jetpack Compose) that converts a music link
from one streaming service to your preferred one and opens it with a single tap.
Paid, one-time purchase. No ads, no accounts, no backend.

## How it works
- **Path A (preferred):** share a song to SwitchTune from the Android Share Sheet.
- **Path B:** copy a music link and open SwitchTune — it auto-detects the link
  from the clipboard.

The link is resolved via the public [Odesli](https://odesli.co/) (song.link) API,
then opened in your preferred app: Spotify, YouTube Music, Apple Music, Deezer,
Tidal, or Amazon Music.

## Architecture
- **UI:** Jetpack Compose, Material 3, single-Activity, MVVM + unidirectional state.
- **DI:** Hilt.
- **Network:** Retrofit + OkHttp + kotlinx.serialization (Odesli only).
- **Local storage:** DataStore (preferred platform + onboarding flag). No remote DB.
- **Billing:** Google Play Billing Library 9 — one non-consumable unlock.

```
app/src/main/java/com/switchtune/app/
├── core/linkparser    # extract & validate music URLs
├── core/platform      # MusicPlatform enum + Intent launching
├── data/odesli        # Retrofit API, DTOs, repository
├── data/prefs         # DataStore preferences
├── data/billing       # Play Billing 9 manager
├── domain/model       # Song, ResolvedSong, ResolveResult
├── di                 # Hilt modules
└── ui                 # onboarding, result, settings, paywall, common, theme
```

## Build
Requires Android Studio (Koala+) with the Android SDK (compileSdk 35, minSdk 26).

```
./gradlew assembleDebug      # debug APK
./gradlew bundleRelease      # release AAB for Play
./gradlew testDebugUnitTest  # unit tests (LinkParser)
```

> Note: this project was authored in an environment without the Android SDK, so
> it has not been compiled here. Open it in Android Studio to build/run.

## Odesli API key
For development the public endpoint works without a key (~10 req/min).
For production, request a key from **developers@song.link** and add it to
`local.properties` (do NOT commit it):

```
ODESLI_API_KEY=your_key_here
```

Then wire it into `app/build.gradle.kts` `defaultConfig`:

```kotlin
val odesliKey = (project.findProperty("ODESLI_API_KEY") as? String)
    ?: System.getenv("ODESLI_API_KEY") ?: ""
buildConfigField("String", "ODESLI_API_KEY", "\"$odesliKey\"")
```

## Release checklist
See `docs/RELEASE-READINESS.md` and `docs/closed-testing-checklist.md`.

## Trademarks
SwitchTune is independent and not affiliated with any streaming service. Service
names are used as plain text only; no third-party logos are bundled.
