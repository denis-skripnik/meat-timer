# Meat and Breathing Timer 🔥🌬️

A small no-framework PWA with two modes:

- meat cooking timer with minute reminders to flip the meat;
- breathing practice timer with inhale/exhale guidance.

The UI, notifications, and recorded audio prompts support Russian and English.

## Features

### Meat cooking

- Duration from 1 to 120 minutes.
- Number input, range slider, and quick presets: 5, 10, 20, 30, 40, 50, 60 minutes.
- One primary toggle button: `Запуск` / `Пауза` or `Launch` / `Pause`.
- Secondary reset button for clearing the current timer.
- Minute reminders with tones, recorded MP3 voice prompt, and browser notification.
- Completion sound, recorded MP3 prompt, and final notification.
- Optional Wake Lock checkbox to keep the screen on where supported.

### Breathing practice

- Separate tab for breathing practices.
- Practice duration in minutes.
- Configurable inhale and exhale durations in seconds.
- One primary toggle button: launch/pause.
- Calm phase prompts for inhale and exhale.
- Calm minute sound plus recorded minute prompt.
- Completion sound, recorded MP3 prompt, and final notification.

### PWA/offline

- Installable PWA with manifest and icons.
- Service worker caches the app shell and all MP3 assets for offline use after first load.
- Browser notifications are routed through the service worker.

## Audio prompts

The app does **not** depend on Web Speech Synthesis for user-facing voice messages. It plays local MP3 files from:

```text
assets/audio/en/
assets/audio/ru/
```

Current prompt files per language:

- `meat-start.mp3`
- `meat-minute.mp3`
- `meat-finish.mp3`
- `breath-start.mp3`
- `breath-inhale.mp3`
- `breath-exhale.mp3`
- `breath-minute.mp3`
- `breath-finish.mp3`

### Regenerating audio

Install/use `edge-tts`, then run:

```bash
node scripts/generate-audio.js
```

The script writes real MP3 files into `assets/audio/{ru,en}/`. The runtime app only needs the generated MP3 files, not `edge-tts`.

## File structure

```text
meat-timer/
├── assets/audio/en/*.mp3
├── assets/audio/ru/*.mp3
├── scripts/generate-audio.js
├── index.html
├── manifest.json
├── sw.js
├── icon-192.png
├── icon-512.png
├── PLAN.md
└── README.md
```

## Running locally

Because service workers require an HTTP origin, use a local server instead of opening the file directly:

```bash
python3 -m http.server 8080
```

Then open:

```text
http://localhost:8080/
```

## GitHub Pages

If deploying under a different repository path, update `start_url`, `scope`, and icon paths in `manifest.json`.

## Browser notes

- Audio playback requires a user gesture; the launch button provides it.
- Wake Lock is not available in every browser, especially iOS Safari.
- Background timer precision can still be limited by mobile browsers.
- Notifications require browser permission.

## License

Open source - feel free to use and modify as needed.
