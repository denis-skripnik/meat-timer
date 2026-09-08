# AGENTS.md

## Project Context

- **Cook & Breathe / Meat-timer** is one repository with two independent clients: a static no-framework PWA at the repository root and a native Android app under `Android/`.
- The Android app is native Java, not a WebView wrapper. Keep the PWA working independently.
- Product scope is intentionally small: meat-cooking timer and breathing-practice timer. Do not add blockchain, accounts, backend, analytics, recipe databases, or unrelated features.
- Russian is the primary UX language; preserve existing English support where implemented.
- Accessibility for TalkBack and screen readers is a core requirement, not optional polish.

## Current Functionality

### Shared product behavior

- Two separate modes/tabs: `Готовка мяса` and `Практики дыхания`; do not turn them into one long mixed form.
- Meat mode supports 1–120 minute duration, quick presets, start/pause/reset, minute reminders to turn the meat, and completion alert.
- Breathing mode supports practice duration, configurable inhale/exhale seconds, quick presets, start/pause/reset, phase guidance, minute reminders, and completion alert.
- Breathing mode contains the Russian **«Три волшебные точки»** guide: sacrum/lower back, thoracic spine, and neck. Each point sets 10 minutes and can be spoken aloud.
- Preserve the guide text as supplied by Denis; do not rewrite, soften, medicalize, or add warnings unless explicitly requested.
- Visual identity is dark, with a decorative flame for cooking and breathing orb for practice. Decorative visuals must stay out of the accessibility tree.
- Quick choices stay in collapsible sections with explicit accessible expanded/collapsed labels.

### Web/PWA

- `index.html` contains the complete HTML, CSS, localization, timer state, deadline-based timing, browser notifications, Wake Lock handling, audio, and breathing-guide UI.
- Timer display derives from wall-clock deadlines; polling cadence must not become the source of truth.
- `manifest.json` owns install metadata; `sw.js` owns offline caching and notification routing.
- Recorded timer prompts live in `assets/audio/{ru,en}/`; composable minute prompts cover numbers 1–120.
- Regular timer prompts use bundled MP3 files. The three-point guide uses browser `speechSynthesis` for its `Озвучить` buttons.
- Settings persist in `localStorage`.
- PWA background timing is inherently less reliable than native Android when hidden or screen-locked; do not claim otherwise.

### Native Android

- Android sources: `Android/app/src/main/java/xyz/blinddev/cookbreathe/`.
- `MainActivity.java`: programmatic native UI, tabs, collapsible controls, state persistence, guide TextToSpeech, battery/exact-alarm guidance.
- `TimerMath.java`: elapsed-realtime deadline and breathing-phase calculations.
- `TimerScheduler.java`: `AlarmManager` scheduling/cancellation using elapsed-realtime wakeup exact alarms.
- `TimerAlarmReceiver.java`: minute/final alerts, stable notification IDs, completion-state cleanup, voice-prompt dispatch.
- `TimerForegroundService.java`: active-timer keeper with partial wake lock and one low-importance silent ongoing notification.
- `PromptPlayer.java`: bundled MP3 playback with transient may-duck audio focus so other music should usually continue.
- `FlameView.java` and `BreathOrbView.java`: native decorative Canvas animations, hidden from TalkBack.
- Android settings persist in `SharedPreferences`.
- Each timer type updates one stable alert notification instead of accumulating one notification per minute.
- Foreground keeper notifications must remain silent and must not be refreshed every few seconds merely to keep the service alive.
- Finishing, pausing, or resetting timers must clear/cancel the relevant alarms and allow the keeper service to stop when no timer remains.
- Reliability depends on notification permission, exact-alarm access, and OEM battery restrictions; keep the in-app route to application battery settings.

## Run And Validation

### Web/PWA

- Serve locally from repository root: `python3 -m http.server 8080`.
- Open: `http://localhost:8080/`.
- Static validation: `node scripts/smoke-check.js`.
- Regenerate bundled timer MP3 files only when needed: `node scripts/generate-audio.js`.

### Android

- Build from `Android/`: `./gradlew assembleDebug`.
- Android static validation from `Android/`: `node scripts/smoke-check.js`.
- Install on a connected device from `Android/`: `./gradlew installDebug`.
- Debug APK output: `Android/app/build/outputs/apk/debug/app-debug.apk`.
- Local SDK path on this host is `/home/assistent/android-sdk`; `Android/local.properties` is local-only and must not be committed.

### Required final checks

```bash
node scripts/smoke-check.js
cd Android
node scripts/smoke-check.js
./gradlew assembleDebug
```

- For notification, lock-screen, audio-focus, TalkBack, or OEM battery changes, a successful build is not a substitute for a real-device test. Report runtime testing honestly.

## File Ownership And Boundaries

- Root PWA and `Android/` are separate implementations of the same product; parity-sensitive features should be updated in both unless Denis explicitly limits the change to one client.
- Do not replace the Android app with WebView or move native timer reliability back to JavaScript.
- Do not edit generated files under `Android/app/build/` or `Android/.gradle/`.
- Do not commit `Android/local.properties`, build outputs, secrets, signing keys, or credentials.
- User-facing local files sent through Telegram must first be copied under a Hermes media cache.

## Accessibility And Content Rules

- Use semantic native controls on Android and semantic HTML on the web.
- Keep labels and content descriptions explicit; do not rely on decorative emoji or position alone.
- Keep live timer/phase announcements useful without causing excessive repeated TalkBack speech.
- Hide flame/orb decoration from TalkBack and browser accessibility APIs.
- Preserve the compact tab-and-collapsible-controls structure.
- Preserve Denis's supplied breathing-guide wording exactly unless he requests editing.

## Update Coupling

- When `index.html` changes, bump `CACHE_NAME` in `sw.js` and update the expected cache version in `scripts/smoke-check.js` so installed PWAs receive the change.
- When web behavior changes, update `scripts/smoke-check.js` and relevant sections of `README.md`/`PLAN.md`.
- When Android behavior changes, update `Android/scripts/smoke-check.js` and relevant sections of `Android/README.md`/`PLAN.md`.
- When shared functionality or copy changes, inspect and update both clients and both smoke checks.
- When Android permissions, receivers, or services change, update `AndroidManifest.xml` and validate a complete debug build.
- Keep notification roles separate: audible timer alerts on the high-importance alert channel; silent foreground keeper on its low-importance channel.
