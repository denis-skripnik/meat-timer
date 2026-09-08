package xyz.blinddev.cookbreathe;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

public class TimerScheduler {
    private static final int MAX_MINUTE_ALARMS = 120;

    private final Context context;
    private final AlarmManager alarmManager;

    public TimerScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        return alarmManager.canScheduleExactAlarms();
    }

    public boolean scheduleTimer(String kind, long startedElapsedMs, long durationMillis) {
        if (!canScheduleExactAlarms()) return false;
        long deadline = startedElapsedMs + durationMillis;
        long nowElapsed = SystemClock.elapsedRealtime();
        if (durationMillis <= 0L || durationMillis > MAX_MINUTE_ALARMS * 60_000L || deadline <= nowElapsed) return false;
        cancelTimer(kind);
        try {
            for (int minute = 1; minute * 60_000L < durationMillis; minute += 1) {
                long trigger = startedElapsedMs + minute * 60_000L;
                if (trigger > nowElapsed) schedule(kind, "minute", minute, trigger, deadline);
            }
            schedule(kind, "finish", (int) ((durationMillis + 59_999L) / 60_000L), deadline, deadline);
            return true;
        } catch (SecurityException denied) {
            cancelTimer(kind); // Access can be revoked between the check and AlarmManager call.
            return false;
        }
    }

    static void discardStaleRunningTimers(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(TimerAlarmReceiver.PREFS, Context.MODE_PRIVATE);
        int boot = android.provider.Settings.Global.getInt(context.getContentResolver(), android.provider.Settings.Global.BOOT_COUNT, -1);
        if (boot >= 0 && prefs.getInt("runtimeBootCount", -2) == boot) return;
        android.content.SharedPreferences.Editor editor = prefs.edit().putInt("runtimeBootCount", boot);
        TimerScheduler scheduler = new TimerScheduler(context);
        for (String prefix : new String[] {"meat", "breath"}) {
            if (!prefs.getBoolean(prefix + "Running", false) && prefs.getLong(prefix + "AlarmDeadline", 0L) == 0L) continue;
            scheduler.cancelTimer(prefix);
            PromptPlayer.cancel(prefix);
            editor.putBoolean(prefix + "Running", false).putLong(prefix + "Started", 0L)
                .putLong(prefix + "Duration", 0L).putLong(prefix + "Remaining", 0L).putLong(prefix + "AlarmDeadline", 0L);
        }
        editor.apply();
    }

    public void cancelTimer(String kind) {
        for (int minute = 1; minute <= MAX_MINUTE_ALARMS; minute += 1) {
            cancel(kind, "minute", minute);
            cancel(kind, "finish", minute);
        }
    }

    private void schedule(String kind, String event, int minute, long triggerElapsedMs, long deadline) {
        PendingIntent pi = pendingIntent(kind, event, minute, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE, deadline);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerElapsedMs, pi);
        } else {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerElapsedMs, pi);
        }
    }

    private void cancel(String kind, String event, int minute) {
        PendingIntent pi = pendingIntent(kind, event, minute, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE, 0L);
        if (pi != null) alarmManager.cancel(pi);
    }

    private PendingIntent pendingIntent(String kind, String event, int minute, int flags, long deadline) {
        Intent intent = new Intent(context, TimerAlarmReceiver.class)
            .putExtra(TimerAlarmReceiver.EXTRA_KIND, kind)
            .putExtra(TimerAlarmReceiver.EXTRA_EVENT, event)
            .putExtra(TimerAlarmReceiver.EXTRA_MINUTE, minute)
            .putExtra("deadline", deadline);
        int requestCode = Math.abs((kind + ":" + event + ":" + minute).hashCode());
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }
}
