# Release Readiness — what's done and what's missing

## Done (in this repository)
- Native Kotlin + Jetpack Compose app, single-purpose, no ads, no accounts, no backend.
- Share Sheet (Path A) and clipboard auto-detect (Path B).
- Odesli integration with all required edge cases handled.
- Four required screens: Onboarding, Result/Main, Settings, Empty/Error.
- Google Play Billing 9 — single non-consumable unlock, content gated until purchase.
- Play Store artifacts: privacy policy, listing copy, screenshots plan, closed-test checklist.

## Blockers that require YOU (the founder) — code cannot resolve these
1. **Odesli API key + commercial-use confirmation.**
   - Email developers@song.link. Without a key the public limit (~10 req/min)
     will cause rate-limit (429) errors under real usage.
   - You MUST get written confirmation that commercial use inside a paid app is
     allowed. This is a product-level risk, not a code issue.
   - Once you have the key, add it (see README "Odesli API key").
2. **Google Play Console account** (personal accounts post-Nov-2023 require the
   12-tester / 14-day closed test — see `closed-testing-checklist.md`).
3. **Host the privacy policy** at a stable public URL and update
   `privacy_policy_url` in `strings.xml`.
4. **Create the in-app product** `switchtune_unlock` (US$1.95, non-consumable).

## Engineering follow-ups before production
- [ ] Generate final launcher icon + feature graphic assets.
- [ ] Replace placeholder support/website/privacy URLs with real ones.
- [ ] Add release signing config (keystore) — currently uses default debug signing.
- [ ] Build and verify `bundleRelease` in Android Studio with the Android SDK
      (the build was authored here but NOT compiled — no Android SDK in this env).
- [ ] Optional: add instrumented tests for the share-intent and billing flows.

## Known limitations / risk notes
- Spotify has acted against third-party apps before (SongShift, 2020). SwitchTune
  only parses public URLs via Odesli and does not use Spotify's API, which lowers
  but does not eliminate the risk.
- "App not installed" currently degrades to opening the song on the web, which is
  the elegant fallback required by the spec. A "Get it on Play Store" prompt is
  available in code (`PlatformLauncher.openPlayStore`) and can be surfaced in the
  UI later if desired.
