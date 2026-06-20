# Screenshots Plan — SwitchTune

Google Play requires a minimum of **2 phone screenshots** (recommended 4–8).
Each: PNG or JPEG, 16:9 or 9:16, min 320 px, max 3840 px on the longest side.

Capture these 5 screens on a phone emulator (Pixel 7, API 35) once the app runs:

| # | Screen | What to show | Suggested caption overlay |
|---|--------|--------------|---------------------------|
| 1 | Paywall | The one-time unlock screen with the $1.95 button | "One-time purchase. No ads, ever." |
| 2 | Onboarding | Platform picker with a service selected | "Pick your music app once." |
| 3 | Result (loaded) | Artwork + title + artist + "Open in …" button | "Open any song link in your app." |
| 4 | Other platforms expanded | The secondary list of services | "Or jump to any other service." |
| 5 | Empty / search fallback | Empty state or the "Search … in …" fallback | "Graceful fallbacks when there's no match." |

Also required:
- **App icon:** 512 x 512 px, 32-bit PNG (export from the adaptive icon).
- **Feature graphic:** 1024 x 500 px (used at the top of the listing).

Notes for compliance:
- Do **not** place third-party streaming-service logos in screenshots. Use the
  in-app plain-text names only.
- Avoid showing real personal data; use a sample song.

Generating screenshots (after a successful build):
```
# From Android Studio: Running Devices > camera icon, or:
adb exec-out screencap -p > screenshot.png
```
