# Meat Cooking Timer 🔥

A Progressive Web App (PWA) designed to help you cook meat perfectly by providing timed reminders to flip it every minute.

## Overview

This is a specialized cooking timer application that combines visual, audio, and notification alerts to ensure you never miss the moment to flip your meat while cooking. The app provides minute-by-minute reminders with voice announcements, sound effects, and browser notifications.

## Features

### ⏱️ Timer Functionality
- **Configurable duration**: Set timer from 1 to 120 minutes
- **Multiple input methods**: Number input field and range slider
- **Quick presets**: One-click buttons for common durations (5, 10, 20, 30, 40, 50, 60 minutes)
- **Real-time countdown**: Live display in MM:SS format

### 🔒 Wake Lock (Keep Screen On)
- Optional "Keep screen on" feature to prevent screen lock during timer operation
- Helps reduce background throttling but does NOT guarantee speech synthesis on all devices/browsers
- **Limitations**: Not supported in Safari on iOS; works on Android in Chrome and Edge

### Multi-Modal Alerts
- **Audio beeps**: Web Audio API-generated tones for minute markers and completion
- **Voice announcements**: English or Russian text-to-speech notifications using Speech Synthesis API
  - Start confirmation: "Timer started for X minutes"
  - Minute reminders: "One minute has passed. Time to flip the meat"
  - Completion alert: "Cooking complete! You can remove the meat from heat"
- **Push notifications**: Browser notifications via Service Worker (requires permission)
  - Only ONE notification is shown at a time (new notifications replace the previous one)
  - Minute updates replace the previous notification; final alert requires interaction
  - Notifications are delivered via Service Worker; on first load the app ensures the SW is ready before sending messages
  - Persistent notifications with vibration pattern
  - Works even when tab is in background

### 🎨 Visual Design
- **Dark theme**: Radial gradient background (black to dark gray)
- **Animated flame effect**: CSS-animated flame appears during active cooking
- **Responsive layout**: Mobile-friendly centered design
- **Clean UI**: Minimalist interface with clear controls

### 📱 PWA Capabilities
- **Installable**: Can be installed as a standalone app on mobile and desktop
- **Offline-ready**: Service Worker enables offline functionality
- **Standalone display**: Runs in its own window without browser UI
- **App icons**: 192x192 and 512x512 PNG icons included
- **Theme integration**: Custom theme color (#ff6600 - orange/fire color)

## Technical Stack

- **Pure HTML/CSS/JavaScript**: No frameworks or dependencies
- **Web APIs used**:
  - Service Worker API (for PWA and notifications)
  - Notification API (for push notifications)
  - Web Audio API (for beep sounds)
  - Speech Synthesis API (for voice announcements)
  - Wake Lock API (to keep screen on during timer)
- **PWA manifest**: Configured for standalone installation
- **Service Worker**: Handles notifications and offline capabilities

## File Structure

```
meat-timer/
├── index.html          # Main application file with UI and logic
├── manifest.json       # PWA manifest configuration
├── sw.js              # Service Worker for notifications
├── icon-192.png       # App icon (192x192)
├── icon-512.png       # App icon (512x512)
└── README.md          # This file
```

## How It Works

1. **Set Duration**: Choose cooking time using input field, slider, or preset buttons
2. **Start Timer**: Click "Старт" (Start) to begin countdown
3. **Minute Reminders**: Every minute, the app will:
   - Play a two-tone beep
   - Announce via voice synthesis (in English and Russian)
   - Send a browser notification
   - Remind you to flip the meat
4. **Completion**: When timer reaches zero:
   - Plays a three-tone completion melody
   - Announces cooking is complete
   - Sends final notification
   - Hides the flame animation
5. **Reset**: Click "Сброс" (Reset) to stop and clear the timer

## Usage

### Running Locally
1. Clone or download this repository
2. Open `index.html` in a modern web browser
3. Grant notification permissions when prompted (optional but recommended)

### Installing as PWA
1. Open the app in Chrome, Edge, or other PWA-compatible browser
2. Look for "Install" option in browser menu or address bar
3. Click to install as standalone app
4. Launch from your device's app drawer or desktop

### Configuring for GitHub Pages Deployment

If you fork this repository and deploy on GitHub Pages:

1. Open `manifest.json`
2. Change `start_url` and `scope` according to your repository name:
   ```json
   "start_url": "/your-repo-name/index.html",
   "scope": "/your-repo-name/"
   ```
3. Commit and push the changes

## Browser Compatibility

- **Recommended**: Chrome, Edge, Safari, Firefox (latest versions)
- **Required features**:
  - Service Worker support
  - Web Audio API
  - Speech Synthesis API (for voice announcements)
  - Notification API (for push notifications)

**Known Limitations:**
- Speech synthesis may not work when the screen is locked/off
- Wake Lock helps keep the screen on but does not guarantee speech on all devices
- Wake Lock API is not available on iOS Safari

## Language

The user interface and voice announcements are in **English** and **Russian** (ru-RU), making it ideal for English-speaking  Russian-speaking users. The app title translates to "Timer for Cooking Meat."

## Use Cases

- Grilling steaks or burgers (flip every minute for even cooking)
- Pan-frying meat cutlets
- Cooking kebabs or shashlik
- Any cooking scenario requiring regular flipping intervals

## License

Open source - feel free to use and modify as needed.
