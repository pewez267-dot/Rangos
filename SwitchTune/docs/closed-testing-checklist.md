# Closed Testing Checklist — SwitchTune

Personal Google Play developer accounts created after **November 2023** must run
a **closed test with at least 12 testers, opted in for 14 consecutive days**,
before applying for production access. This checklist tracks that requirement.

## A. Account & app setup
- [ ] Google Play Console developer account created and verified.
- [ ] App created in Play Console (package `com.switchtune.app`).
- [ ] App signing by Google Play enabled (upload key configured).
- [ ] Paid-app price set to US$1.95 (Monetization > App pricing). No in-app products.

## B. Store listing readiness
- [ ] Title, short & full description set (see `play-store-listing.md`).
- [ ] App icon (512x512) and feature graphic (1024x500) uploaded.
- [ ] At least 2 phone screenshots uploaded (see `screenshots-plan.md`).
- [ ] Privacy policy URL live and reachable (host `privacy-policy.md`).
- [ ] Data safety form completed ("no data collected").
- [ ] Content rating (IARC) questionnaire completed.
- [ ] Target audience set (not directed at children).

## C. Build
- [ ] `versionCode`/`versionName` bumped.
- [ ] Release signed AAB built (`./gradlew bundleRelease`).
- [ ] `ODESLI_API_KEY` injected for the release build (see README).
- [ ] App opens directly into the main screen (no paywall) and resolves a link.

## D. Closed test track
- [ ] Closed testing track created; AAB uploaded and rolled out.
- [ ] Tester email list (>= 12 real Google accounts) added.
- [ ] Opt-in link shared; confirm **12+ testers actually opt in**.
- [ ] Track has run for **14 consecutive days** with active testers.

## E. Pre-production blockers (owner action — see RELEASE-READINESS.md)
- [ ] Odesli API key obtained AND written confirmation that commercial use in a
      paid app is permitted (developers@song.link).
- [ ] Privacy policy hosted at a stable public URL.
- [ ] Apply for production access after the 14-day closed test.
