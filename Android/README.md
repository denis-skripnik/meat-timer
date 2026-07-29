# Cook & Breathe Native Android

Native Android version of the Meat-timer / Cook & Breathe app.

This is intentionally separate from the static PWA. The native app does not use a WebView wrapper; it implements the timer screens, persisted settings, Android notification channel, and exact `AlarmManager` reminders in Android code.

## Why native

Mobile browsers can throttle JavaScript timers and audio when a PWA is hidden or the screen is off. This app schedules minute and finish reminders through Android exact alarms, so alerts are much more reliable in the background.

## Features in this first Android pass

- Meat cooking timer:
  - duration in minutes;
  - quick presets: 5, 10, 20, 30, 40, 50, 60;
  - start, pause, reset;
  - minute reminder notifications and final notification.
- Breathing practice timer:
  - practice duration;
  - inhale and exhale seconds;
  - quick presets;
  - screen phase label;
  - minute reminder notifications and final notification.
- Local settings via `SharedPreferences`.
- Android notification permission request on Android 13+.
- Exact alarm helper button on Android 12+ when the system requires permission.
- Native controls (`TextView`, `EditText`, `Button`) for TalkBack-friendly navigation.

## Build

From this folder:

```bash
./gradlew assembleDebug
```

If the wrapper is not available yet but Gradle is installed:

```bash
gradle wrapper --gradle-version 8.7
./gradlew assembleDebug
```

This environment uses SDK path:

```text
/home/assistent/android-sdk
```

A local `local.properties` file can point Gradle to that SDK:

```properties
sdk.dir=/home/assistent/android-sdk
```

## Install on a connected device

```bash
./gradlew installDebug
```

Then open Cook & Breathe on Android, allow notifications, and if prompted allow exact alarms.

## Android limitations

Native exact alarms are significantly better than PWA background timers, but Android/OEM battery restrictions can still affect aggressively restricted apps. For best reliability, allow notifications and exact alarms, and avoid putting the app into a restricted battery mode.
