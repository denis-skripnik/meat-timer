package xyz.blinddev.cookbreathe;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class TimerAlarmReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "cook_breathe_timers";
    public static final String KEEPER_CHANNEL_ID = "cook_breathe_timer_keeper";
    public static final String EXTRA_KIND = "kind";
    public static final String EXTRA_EVENT = "event";
    public static final String EXTRA_MINUTE = "minute";
    public static final String PREFS = "cook_breathe_prefs";
    public static final String PREF_VOICE_PROMPTS = "voicePrompts";
    public static final String PREF_LANGUAGE = "language";
    public static final int MEAT_ALERT_NOTIFICATION_ID = 1000;
    public static final int BREATH_ALERT_NOTIFICATION_ID = 2000;

    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult pendingResult = goAsync();
        ensureChannel(context);
        String kind = intent.getStringExtra(EXTRA_KIND);
        String event = intent.getStringExtra(EXTRA_EVENT);
        int minute = intent.getIntExtra(EXTRA_MINUTE, 0);
        boolean breath = "breath".equals(kind);
        boolean finish = "finish".equals(event);

        String title;
        String text;
        if (finish) {
            title = breath ? "Дыхание завершено" : "Таймер завершён";
            text = breath ? "Практика дыхания завершена." : "Готовка завершена. Можно снимать мясо с огня.";
            markTimerFinished(context, breath ? "breath" : "meat");
            Intent keeperIntent = new Intent(context, TimerForegroundService.class).setAction(TimerForegroundService.ACTION_STOP_IF_IDLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(keeperIntent);
            else context.startService(keeperIntent);
        } else {
            title = breath ? "Минута дыхания" : "Минута таймера";
            text = breath
                ? "Прошло " + minute + " мин. Продолжайте спокойно дышать."
                : "Прошло " + minute + " мин. Пора перевернуть мясо.";
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
            context, breath ? 20 : 10, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        android.app.Notification.Builder builder = new android.app.Notification.Builder(context)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new android.app.Notification.BigTextStyle().bigText(text))
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setVibrate(new long[] {0, 220, 120, 220});

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) builder.setChannelId(CHANNEL_ID);
        if (finish) builder.setOngoing(false);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(alertNotificationId(breath), builder.build());

        boolean voicePrompts = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PREF_VOICE_PROMPTS, true);
        String language = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(PREF_LANGUAGE, "ru");
        if (voicePrompts) {
            PromptPlayer.playPrompt(context, breath ? "breath" : "meat", finish ? "finish" : "minute", minute, language, pendingResult::finish);
        } else {
            pendingResult.finish();
        }
    }

    public static int alertNotificationId(boolean breath) {
        return breath ? BREATH_ALERT_NOTIFICATION_ID : MEAT_ALERT_NOTIFICATION_ID;
    }

    private static void markTimerFinished(Context context, String prefix) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putBoolean(prefix + "Running", false)
            .putLong(prefix + "Started", 0L)
            .putLong(prefix + "Duration", 0L)
            .putLong(prefix + "Remaining", 0L)
            .apply();
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Cook & Breathe timer alerts", NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Audible minute and completion alerts for cooking and breathing timers.");
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        if (manager.getNotificationChannel(KEEPER_CHANNEL_ID) != null) return;
        NotificationChannel keeper = new NotificationChannel(
            KEEPER_CHANNEL_ID, "Cook & Breathe active timer", NotificationManager.IMPORTANCE_LOW
        );
        keeper.setDescription("Silent ongoing notification shown while a timer is kept active under the locked screen.");
        keeper.setSound(null, null);
        keeper.enableVibration(false);
        manager.createNotificationChannel(keeper);
    }
}
