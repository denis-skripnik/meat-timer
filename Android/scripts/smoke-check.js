const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const files = {
  manifest: 'app/src/main/AndroidManifest.xml',
  main: 'app/src/main/java/xyz/blinddev/cookbreathe/MainActivity.java',
  scheduler: 'app/src/main/java/xyz/blinddev/cookbreathe/TimerScheduler.java',
  receiver: 'app/src/main/java/xyz/blinddev/cookbreathe/TimerAlarmReceiver.java',
  promptPlayer: 'app/src/main/java/xyz/blinddev/cookbreathe/PromptPlayer.java',
  keeper: 'app/src/main/java/xyz/blinddev/cookbreathe/TimerForegroundService.java',
  math: 'app/src/main/java/xyz/blinddev/cookbreathe/TimerMath.java',
  flame: 'app/src/main/java/xyz/blinddev/cookbreathe/FlameView.java',
  orb: 'app/src/main/java/xyz/blinddev/cookbreathe/BreathOrbView.java',
  styles: 'app/src/main/res/values/styles.xml'
};

function read(rel) { return fs.readFileSync(path.join(root, rel), 'utf8'); }
function assert(condition, message) { if (!condition) throw new Error(message); }
function exists(rel) { return fs.existsSync(path.join(root, rel)); }

const manifest = read(files.manifest);
const main = read(files.main);
const scheduler = read(files.scheduler);
const receiver = read(files.receiver);
const promptPlayer = read(files.promptPlayer);
const keeper = read(files.keeper);
const math = read(files.math);
const flame = read(files.flame);
const orb = read(files.orb);
const styles = read(files.styles);

assert(manifest.includes('android.permission.POST_NOTIFICATIONS'), 'Android 13 notification permission is missing.');
assert(manifest.includes('android.permission.SCHEDULE_EXACT_ALARM'), 'Exact alarm permission is missing.');
assert(manifest.includes('android.permission.WAKE_LOCK'), 'Wake lock permission is missing for locked-screen timer keeping.');
assert(manifest.includes('android.permission.FOREGROUND_SERVICE'), 'Foreground service permission is missing.');
assert(manifest.includes('TimerForegroundService'), 'TimerForegroundService is not registered.');
assert(manifest.includes('foregroundServiceType="mediaPlayback"'), 'Foreground service type for prompt playback is missing.');
assert(manifest.includes('TimerAlarmReceiver'), 'TimerAlarmReceiver is not registered.');

assert(main.includes('requestNotificationPermissionIfNeeded()'), 'Notification runtime permission request is missing.');
assert(main.includes('openExactAlarmSettings()'), 'Exact alarm settings helper is missing.');
assert(main.includes('Готовка мяса'), 'Meat timer section is missing.');
assert(main.includes('Практики дыхания'), 'Breathing timer section is missing.');
assert(main.includes('switchMode(KIND_MEAT)'), 'Native meat tab switching is missing.');
assert(main.includes('switchMode(KIND_BREATH)'), 'Native breath tab switching is missing.');
assert(main.includes('meatPanel.setVisibility'), 'Meat panel should be hidden/shown by tab state.');
assert(main.includes('breathPanel.setVisibility'), 'Breath panel should be hidden/shown by tab state.');
assert(main.includes('collapsiblePresetPanel'), 'Quick presets should be collapsed behind accessible panels.');
assert(main.includes('Показать: ') && main.includes('Скрыть: '), 'Collapsible preset panels should expose show/hide labels.');
assert(main.includes('SharedPreferences'), 'Local settings persistence is missing.');
assert(main.includes('scheduler.scheduleTimer(KIND_MEAT'), 'Meat timer should schedule native alarms.');
assert(main.includes('scheduler.scheduleTimer(KIND_BREATH'), 'Breath timer should schedule native alarms.');
assert(main.includes('setContentDescription'), 'Accessible content descriptions should be present.');
assert(main.includes('Озвучивать подсказки поверх музыки'), 'Voice prompt checkbox copy is missing.');
assert(main.includes('PromptPlayer.playStart'), 'Start prompts should play from the Activity.');
assert(main.includes('PromptPlayer.playPhase'), 'Breathing phase prompts should play while the app is open.');
assert(main.includes('startTimerKeeper') && main.includes('startForegroundService'), 'Active timers should start a foreground keeper service.');
assert(main.includes('updateTimerKeeper'), 'Timer keeper should stop when timers pause/reset.');
assert(main.includes('COLOR_BACKGROUND') && main.includes('setBackgroundColor(COLOR_BACKGROUND)'), 'Dark PWA-like background is missing.');
assert(main.includes('new FlameView') && main.includes('new BreathOrbView'), 'Decorative flame/orb views should be mounted in the tabs.');
assert(main.includes('updateBreathOrb'), 'Breathing orb should update with the current phase.');

assert(scheduler.includes('setExactAndAllowWhileIdle'), 'Scheduler should use exact allow-while-idle alarms.');
assert(scheduler.includes('ELAPSED_REALTIME_WAKEUP'), 'Scheduler should use elapsed realtime wakeup alarms.');
assert(scheduler.includes('cancelTimer'), 'Scheduler should support alarm cancellation.');
assert(scheduler.includes('canScheduleExactAlarms'), 'Scheduler should check exact alarm access.');

assert(receiver.includes('NotificationChannel'), 'Notification channel is missing.');
assert(receiver.includes('IMPORTANCE_HIGH'), 'Timer notifications should be high importance.');
assert(receiver.includes('setContentIntent'), 'Notification tap should open the app.');
assert(receiver.includes('Пора перевернуть мясо'), 'Meat minute notification copy is missing.');
assert(receiver.includes('Практика дыхания завершена'), 'Breath finish notification copy is missing.');
assert(receiver.includes('goAsync()'), 'Receiver should keep broadcast alive while prompts play.');
assert(receiver.includes('PromptPlayer.playPrompt'), 'Receiver should play native voice prompts for alarms.');
assert(receiver.includes('alertNotificationId') && receiver.includes('MEAT_ALERT_NOTIFICATION_ID') && !receiver.includes('idBase +'), 'Timer alerts should update one notification per timer instead of creating one per minute.');
assert(receiver.includes('markTimerFinished'), 'Finish alerts should clear persisted running state.');
assert(receiver.includes('ACTION_STOP_IF_IDLE'), 'Finish alerts should ask the keeper service to stop if no timers remain.');
assert(receiver.includes('PREF_VOICE_PROMPTS'), 'Voice prompts should be user-toggleable.');

assert(promptPlayer.includes('AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK'), 'Prompt playback should request transient may-duck audio focus.');
assert(promptPlayer.includes('USAGE_ASSISTANCE_SONIFICATION'), 'Prompt playback should use sonification audio attributes.');
assert(promptPlayer.includes('CONTENT_TYPE_SPEECH'), 'Prompt playback should mark prompts as speech.');
assert(promptPlayer.includes('minutePromptFiles'), 'Composable minute prompts should be supported.');
assert(keeper.includes('PARTIAL_WAKE_LOCK'), 'Foreground keeper should hold a partial wake lock while a timer runs.');
assert(keeper.includes('startForeground'), 'Foreground keeper should run as a foreground service.');
assert(keeper.includes('START_STICKY'), 'Foreground keeper should be restartable while timers are active.');
assert(keeper.includes('refreshExpiredTimers') && keeper.includes('clearExpiredTimer'), 'Foreground keeper should clear expired timers when the app UI is not open.');
assert(promptPlayer.includes('number-') && promptPlayer.includes('minute-few.mp3'), 'RU/EN composable minute files should be referenced.');
assert(exists('app/src/main/assets/audio/ru/meat-start.mp3'), 'RU start prompt asset is missing.');
assert(exists('app/src/main/assets/audio/ru/number-120.mp3'), 'RU number prompt assets are missing.');
assert(exists('app/src/main/assets/audio/en/breath-finish.mp3'), 'EN prompt assets are missing.');

assert(math.includes('remainingMillis'), 'Wall-clock remaining helper is missing.');
assert(math.includes('breathPhase'), 'Breath phase helper is missing.');
assert(flame.includes('RadialGradient') && flame.includes('ValueAnimator'), 'Animated flame drawing is missing.');
assert(flame.includes('IMPORTANT_FOR_ACCESSIBILITY_NO'), 'Decorative flame should be hidden from accessibility services.');
assert(orb.includes('RadialGradient') && orb.includes('setPhase'), 'Breathing orb drawing/phase update is missing.');
assert(orb.includes('IMPORTANT_FOR_ACCESSIBILITY_NO'), 'Decorative breathing orb should be hidden from accessibility services.');
assert(styles.includes('Theme.Material.NoActionBar') && styles.includes('#050507'), 'Dark native theme is missing.');

console.log('Android smoke checks passed.');
