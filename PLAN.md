# PLAN

## Approved audit fixes — 2026-09-08

Outcome: fix all nine reviewed defects, preserving the independent static PWA and native Android app, existing guide wording, tabs, notification roles and MP3 prompts.

Boundaries: root timer JS/service-worker cache, audio generator, Android timer/state/scheduler/playback/UI, focused tests and existing documentation. No redesign, backend, payment/link changes, production deployment, device install or unrelated cleanup.

Functional slices:
- [x] Add regressions for invalid web input, live editing, cancellation/serial speech and English 70–99.
- [x] Web: validate integer ranges; keep valid session phase parameters; serialize prompts and cancel timer-owned speech/delays.
- [x] Android: preserve exact deadline/elapsed time across pause; guard exact-alarm failure before committing running state; discard runtime state from another boot without discarding settings.
- [x] Android: serialize/cancel timer prompts, avoid stale phase queue; persist valid field edits; expose actual expanded/collapsed state.
- [x] Run behavioral checks, existing smoke checks, Android debug build and local browser flows; inspect final diff and commit only task files.

Verification: `node scripts/timer-regression.js`, both existing smoke scripts, Android unit tests/debug build, browser on isolated local server with console checks. Android OS permission, reboot, TalkBack and audio focus require device evidence; report any unavailable coverage explicitly.

Constraints: timer deadlines remain authoritative, minute numbering and breathing elapsed time survive pause. Invalid edits cannot prevent completion. One timer's cancellation must not silence the other. Preserve media/guide content; generator fix is text-only unless existing MP3 defects are demonstrated. Preserve valid settings across recreation; expire old-boot runtime state, not preferences.

Data safety: app SharedPreferences/localStorage and bundled audio must not be erased. No live data/backend in this repository; git initially clean. No reset/clean, no production sync/restart, no user device operations. Runtime-state migration invalidates only legacy running timers lacking a boot marker; paused remaining duration remains resumable. Build outputs remain ignored.

Stop when: secrets, spending/audio generation charges, device install, public release/deploy or an out-of-scope system change is required. Local validation server is bounded to this task, shut down after browser checks. No background agent runs.

Definition of Done: all nine fixes implemented with focused regression evidence, existing checks and build green, browser behavior exercised, remaining device-only coverage stated honestly.

Verified: 8 web regression tests, 14 Robolectric tests, both smoke checks and debug build pass. Browser: invalid input rejected; a one-minute breath practice completes despite an empty inhale field; start/reset works. Diagnostic window error/rejection hooks reported no errors during final start/reset (Camofox does not expose native console logs). Physical-device TalkBack, OEM lock-screen/audio focus and reboot remain untested. Audio and HTML guide markup unchanged. Push withheld: GitHub Pages deploys main automatically.

## Scope

Extend the existing Cook & Breathe / Meat-timer project with a separate native Android app in `Android/`.

The existing static PWA remains in place. The Android app is not a WebView wrapper: it should implement the timer logic natively so minute/final alerts use Android notifications and alarms instead of browser background timers.

## Current PWA scope already delivered

1. Meat cooking timer.
2. Breathing practice timer.
3. Offline PWA with local audio prompts and TalkBack-friendly controls.

## Android follow-up: native timer app

### Scope

- Create a separate Android project under `Android/`.
- Implement the core timer UX natively with accessible controls.
- Use native Android notification channels and exact alarms for reliable minute/final reminders while the app is backgrounded.
- Keep it simple: no blockchain features, no backend, no account system, no payment logic.
- Preserve Russian-first UX with English language support where practical.

### Non-goals

- No WebView-only port of the PWA.
- No server/backend or cloud scheduler.
- No blockchain, wallet, analytics, auth, or monetization features.
- No complex recipe engine or cooking database.
- No publication/signing setup for Play Store in this pass.

### Acceptance criteria

- `Android/` contains a standalone Gradle Android app project.
- App has meat cooking timer controls: duration, quick presets, start/pause/reset.
- App has breathing timer controls: practice duration, inhale seconds, exhale seconds, start/pause/reset.
- Breathing tab includes a three-point 10-minute guide: sacrum/lower back, thoracic spine, and neck; each point can set the timer to 10 minutes and can be spoken with Android TextToSpeech.
- User settings are persisted locally.
- On timer start/resume, native exact alarms are scheduled for minute reminders and completion.
- Notification tap opens the app.
- Android 13+ notification permission is requested from the app.
- Android 12+ exact alarm access has a visible helper action when the system requires it.
- UI uses normal native `TextView`, `Button`, `EditText`, and semantic labels so TalkBack can read the flow.
- Android UI preserves the PWA structure: two accessible mode tabs and collapsible quick-choice panels instead of one long mixed screen.
- Android UI preserves the PWA visual feel: dark background, animated flame for cooking, and animated breathing orb for practice; decorative views are hidden from accessibility services.
- Active timers start a foreground keeper service with a partial wake lock. This avoids Android delaying minute prompts after the screen has been locked for a while; AlarmManager remains as the notification trigger/backstop.
- The foreground keeper notification is silent and low-importance; audible alerts are reserved for minute/final timer events.
- If the system still restricts the app, show user-facing battery guidance and a direct button to app settings so the user can choose unrestricted/no-optimization mode.
- Timer alerts update one stable notification per timer type instead of creating a new notification every minute; finish alerts clear persisted running state for foreground-service shutdown.
- Validation runs at least a static smoke check; if Android SDK/Gradle are available, run a debug build too.

### Implementation tasks

1. Add Android Gradle project files under `Android/`.
2. Add native Java/Kotlin app code with programmatic accessible UI.
3. Add timer state and wall-clock math helpers.
4. Add alarm scheduling/cancellation helpers based on `AlarmManager`.
5. Add `BroadcastReceiver` for timer alarm delivery and notifications.
6. Add Android manifest permissions and notification receiver wiring.
7. Add README instructions for local build/install and Android limitations.
8. Add a static smoke script that verifies important native-alarm wiring.
9. Run smoke/build validation and inspect git status/diff.

### Risks and assumptions

- Exact alarms are still subject to Android's exact-alarm permission/policy on Android 12+; the app should guide the user to allow exact alarms when needed.
- Android 13+ requires runtime notification permission; without it alerts will not appear.
- Some OEM battery managers can still delay or suppress background work if the app is aggressively restricted, but native exact alarms are much more reliable than browser `setInterval` in a hidden PWA.
- Voice prompts are bundled from the PWA MP3 assets under `app/src/main/assets/audio/{ru,en}/`.
- Voice prompts use short transient ducking audio focus, so music apps should usually continue playing with brief volume ducking rather than being stopped.
- Android/OEM audio-focus behavior can vary; some players may still pause instead of ducking.

## Definition of done

- Existing PWA files are not regressed.
- Android native app files are isolated to `Android/` except shared docs/check scripts if needed.
- Static validation passes.
- If local Android tooling is sufficient, `./gradlew assembleDebug` passes.
