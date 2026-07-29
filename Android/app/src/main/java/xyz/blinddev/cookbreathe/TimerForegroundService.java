package xyz.blinddev.cookbreathe;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

public class TimerForegroundService extends Service {
    public static final String ACTION_REFRESH = "xyz.blinddev.cookbreathe.REFRESH_TIMER_KEEPER";
    public static final String ACTION_STOP = "xyz.blinddev.cookbreathe.STOP_TIMER_KEEPER";
    private static final int NOTIFICATION_ID = 77;
    private static final long CHECK_INTERVAL_MS = 5_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wakeLock;

    private final Runnable checker = new Runnable() {
        @Override public void run() {
            if (!hasRunningTimer()) {
                stopSelf();
                return;
            }
            updateForegroundNotification();
            handler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        TimerAlarmReceiver.ensureChannel(this);
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CookBreathe:TimerWakeLock");
        wakeLock.setReferenceCounted(false);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!hasRunningTimer()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!wakeLock.isHeld()) wakeLock.acquire();
        startForeground(NOTIFICATION_ID, buildNotification());
        handler.removeCallbacks(checker);
        handler.postDelayed(checker, CHECK_INTERVAL_MS);
        return START_STICKY;
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(checker);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private boolean hasRunningTimer() {
        SharedPreferences prefs = getSharedPreferences(TimerAlarmReceiver.PREFS, MODE_PRIVATE);
        return prefs.getBoolean("meatRunning", false) || prefs.getBoolean("breathRunning", false);
    }

    private void updateForegroundNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, buildNotification());
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
            this, 2, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        String text = foregroundText();
        return new Notification.Builder(this, TimerAlarmReceiver.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Cook & Breathe работает")
            .setContentText(text)
            .setStyle(new Notification.BigTextStyle().bigText(text))
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .build();
    }

    private String foregroundText() {
        SharedPreferences prefs = getSharedPreferences(TimerAlarmReceiver.PREFS, MODE_PRIVATE);
        boolean meat = prefs.getBoolean("meatRunning", false);
        boolean breath = prefs.getBoolean("breathRunning", false);
        if (meat && breath) return "Таймеры удерживаются активными для своевременных подсказок при заблокированном экране.";
        if (meat) return "Таймер готовки удерживается активным для своевременных подсказок при заблокированном экране.";
        if (breath) return "Практика дыхания удерживается активной для своевременных подсказок при заблокированном экране.";
        return "Нет активного таймера.";
    }
}
