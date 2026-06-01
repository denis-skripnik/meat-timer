# PLAN

## Scope

Extend the existing single-file PWA into a two-tab app:

1. Meat cooking timer.
2. Breathing practice timer.

Keep the current no-framework HTML/CSS/JS approach and update only the files needed for the requested feature set.

## Non-goals

- No backend or external runtime dependency for app users.
- No user accounts, analytics, cloud sync, or expanded cooking algorithms.
- No speculative design-system rewrite.

## Acceptance criteria

- The UI has two accessible tabs: meat cooking and breathing practice.
- The main timer control is a single toggle button that changes between Start/Launch and Pause in RU/EN.
- Reset exists only as a secondary safe action to clear state.
- User-facing voice messages use local MP3 assets instead of Web Speech Synthesis.
- RU and EN are supported for UI text, notifications, and audio prompts.
- Breathing tab includes inhale and exhale duration settings, calm minute sound, and calm inhale/exhale sounds.
- Service worker cache version includes all new assets.
- README documents the new behavior and asset generation.
- Static smoke checks pass and MP3 assets exist.

## Implementation tasks

1. Refactor `index.html` UI into two tab panels while preserving the PWA as a single file.
2. Replace separate Start/Reset primary controls with one primary toggle button per mode and secondary reset buttons.
3. Replace `speechSynthesis` calls with an audio asset playback helper.
4. Add breathing timer state, inhale/exhale phase switching, minute chime, notifications, and language-aware labels.
5. Add `scripts/generate-audio.js` for repeatable MP3 asset generation.
6. Generate actual MP3 assets under `assets/audio/{ru,en}/`.
7. Update `sw.js` with cache versioning and all static/audio assets.
8. Update `README.md`.
9. Run smoke validation: parse/check JS, verify HTML/service worker syntax, verify MP3 files, inspect git diff/status.

## Follow-up: accessible preset controls and persistence

### Scope

- Keep the no-framework single-file app.
- Make TalkBack-friendly preset buttons available beside timer fields without making the page bulky.
- Persist configured values across reloads/return visits.

### Acceptance criteria

- Meat preset buttons are inside an accessible collapsible section (`details`/`summary`) near the meat duration field.
- Breathing practice has accessible collapsible preset sections for practice minutes, inhale seconds, and exhale seconds.
- Inhale and exhale preset buttons provide 1–10 seconds.
- Input changes and preset button presses are saved to `localStorage` and restored on load.
- Range and number inputs stay synchronized.
- Static smoke checks cover the new controls and saved-settings behavior.
- After a preset button is chosen, the currently opened quick-choice section collapses automatically.

## Risks and assumptions

- Audio generation depends on local or reachable tooling. If online TTS is unavailable, the app code path will still support assets, but generation would be blocked. For this pass, use local ffmpeg-generated MP3 prompts if speech TTS tooling is not available.
- Browser autoplay policies require a user click before audio playback; the toggle button provides that user gesture.
- Background tab timer precision can still be limited by browsers; wake lock helps only while supported and enabled.

## Definition of done

- The app works offline after first load with updated service worker cache.
- No `SpeechSynthesisUtterance`/`speechSynthesis` dependency remains in runtime app code.
- The repo contains generated MP3 files and a script to regenerate them.
- Validation commands complete without JavaScript syntax errors.
