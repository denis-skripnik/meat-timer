# PLAN

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
- User settings are persisted locally.
- On timer start/resume, native exact alarms are scheduled for minute reminders and completion.
- Notification tap opens the app.
- Android 13+ notification permission is requested from the app.
- Android 12+ exact alarm access has a visible helper action when the system requires it.
- UI uses normal native `TextView`, `Button`, `EditText`, and semantic labels so TalkBack can read the flow.
- Android UI preserves the PWA structure: two accessible mode tabs and collapsible quick-choice panels instead of one long mixed screen.
- Android UI preserves the PWA visual feel: dark background, animated flame for cooking, and animated breathing orb for practice; decorative views are hidden from accessibility services.
- Active timers start a foreground keeper service with a partial wake lock. This avoids Android delaying minute prompts after the screen has been locked for a while; AlarmManager remains as the notification trigger/backstop.
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
