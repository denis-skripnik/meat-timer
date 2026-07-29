package xyz.blinddev.cookbreathe;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class TimerAlarmReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "cook_breathe_timers";
    public static final String EXTRA_KIND = "kind";
    public static final String EXTRA_EVENT = "event";
    public static final String EXTRA_MINUTE = "minute";
    public static final String PREFS = "cook_breathe_prefs";
    public static final String PREF_VOICE_PROMPTS = "voicePrompts";
    public static final String PREF_LANGUAGE = "language";

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
        } else {
            title = breath ? "Минута дыхания" : "Минута таймера";
            text = breath
                ? "Прошло " + minute + " мин. Продолжайте спокойно дышать."
                : "Прошло " + minute + " мин. Пора перевернуть мясо.";
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
            context, 1, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        android.app.Notification.Builder builder = new android.app.Notification.Builder(context)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new android.app.Notification.BigTextStyle().bigText(text))
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setVibrate(new long[] {0, 220, 120, 220});

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) builder.setChannelId(CHANNEL_ID);
        if (finish) builder.setOngoing(false);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int idBase = breath ? 2000 : 1000;
        manager.notify(idBase + (finish ? 999 : Math.max(1, minute)), builder.build());

        boolean voicePrompts = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PREF_VOICE_PROMPTS, true);
        String language = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(PREF_LANGUAGE, "ru");
        if (voicePrompts) {
            PromptPlayer.playPrompt(context, breath ? "breath" : "meat", finish ? "finish" : "minute", minute, language, pendingResult::finish);
        } else {
            pendingResult.finish();
        }
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID);
        if (existing != null) return;
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID, "Cook & Breathe timers", NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Minute and completion alerts for cooking and breathing timers.");
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
    }
}
