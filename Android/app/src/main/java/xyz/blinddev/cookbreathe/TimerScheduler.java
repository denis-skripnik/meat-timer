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

    public void scheduleTimer(String kind, long startedElapsedMs, int durationMinutes) {
        cancelTimer(kind);
        int safeMinutes = Math.max(1, Math.min(MAX_MINUTE_ALARMS, durationMinutes));
        long nowElapsed = SystemClock.elapsedRealtime();
        for (int minute = 1; minute <= safeMinutes; minute += 1) {
            String event = minute == safeMinutes ? "finish" : "minute";
            long triggerElapsedMs = startedElapsedMs + minute * 60_000L;
            if (triggerElapsedMs <= nowElapsed) continue;
            schedule(kind, event, minute, triggerElapsedMs);
        }
    }

    public void cancelTimer(String kind) {
        for (int minute = 1; minute <= MAX_MINUTE_ALARMS; minute += 1) {
            cancel(kind, "minute", minute);
            cancel(kind, "finish", minute);
        }
    }

    private void schedule(String kind, String event, int minute, long triggerElapsedMs) {
        PendingIntent pi = pendingIntent(kind, event, minute, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerElapsedMs, pi);
        } else {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerElapsedMs, pi);
        }
    }

    private void cancel(String kind, String event, int minute) {
        PendingIntent pi = pendingIntent(kind, event, minute, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) alarmManager.cancel(pi);
    }

    private PendingIntent pendingIntent(String kind, String event, int minute, int flags) {
        Intent intent = new Intent(context, TimerAlarmReceiver.class)
            .putExtra(TimerAlarmReceiver.EXTRA_KIND, kind)
            .putExtra(TimerAlarmReceiver.EXTRA_EVENT, event)
            .putExtra(TimerAlarmReceiver.EXTRA_MINUTE, minute);
        int requestCode = Math.abs((kind + ":" + event + ":" + minute).hashCode());
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }
}
