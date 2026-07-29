const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const files = {
  manifest: 'app/src/main/AndroidManifest.xml',
  main: 'app/src/main/java/xyz/blinddev/cookbreathe/MainActivity.java',
  scheduler: 'app/src/main/java/xyz/blinddev/cookbreathe/TimerScheduler.java',
  receiver: 'app/src/main/java/xyz/blinddev/cookbreathe/TimerAlarmReceiver.java',
  math: 'app/src/main/java/xyz/blinddev/cookbreathe/TimerMath.java'
};

function read(rel) { return fs.readFileSync(path.join(root, rel), 'utf8'); }
function assert(condition, message) { if (!condition) throw new Error(message); }

const manifest = read(files.manifest);
const main = read(files.main);
const scheduler = read(files.scheduler);
const receiver = read(files.receiver);
const math = read(files.math);

assert(manifest.includes('android.permission.POST_NOTIFICATIONS'), 'Android 13 notification permission is missing.');
assert(manifest.includes('android.permission.SCHEDULE_EXACT_ALARM'), 'Exact alarm permission is missing.');
assert(manifest.includes('TimerAlarmReceiver'), 'TimerAlarmReceiver is not registered.');

assert(main.includes('requestNotificationPermissionIfNeeded()'), 'Notification runtime permission request is missing.');
assert(main.includes('openExactAlarmSettings()'), 'Exact alarm settings helper is missing.');
assert(main.includes('Готовка мяса'), 'Meat timer section is missing.');
assert(main.includes('Практики дыхания'), 'Breathing timer section is missing.');
assert(main.includes('SharedPreferences'), 'Local settings persistence is missing.');
assert(main.includes('scheduler.scheduleTimer(KIND_MEAT'), 'Meat timer should schedule native alarms.');
assert(main.includes('scheduler.scheduleTimer(KIND_BREATH'), 'Breath timer should schedule native alarms.');
assert(main.includes('setContentDescription'), 'Accessible content descriptions should be present.');

assert(scheduler.includes('setExactAndAllowWhileIdle'), 'Scheduler should use exact allow-while-idle alarms.');
assert(scheduler.includes('ELAPSED_REALTIME_WAKEUP'), 'Scheduler should use elapsed realtime wakeup alarms.');
assert(scheduler.includes('cancelTimer'), 'Scheduler should support alarm cancellation.');
assert(scheduler.includes('canScheduleExactAlarms'), 'Scheduler should check exact alarm access.');

assert(receiver.includes('NotificationChannel'), 'Notification channel is missing.');
assert(receiver.includes('IMPORTANCE_HIGH'), 'Timer notifications should be high importance.');
assert(receiver.includes('setContentIntent'), 'Notification tap should open the app.');
assert(receiver.includes('Пора перевернуть мясо'), 'Meat minute notification copy is missing.');
assert(receiver.includes('Практика дыхания завершена'), 'Breath finish notification copy is missing.');

assert(math.includes('remainingMillis'), 'Wall-clock remaining helper is missing.');
assert(math.includes('breathPhase'), 'Breath phase helper is missing.');

console.log('Android smoke checks passed.');
