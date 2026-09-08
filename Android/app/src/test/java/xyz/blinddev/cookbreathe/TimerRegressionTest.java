package xyz.blinddev.cookbreathe;

import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowSystemClock;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 31)
@LooperMode(LooperMode.Mode.PAUSED)
public class TimerRegressionTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private AlarmManager alarms;
    private SharedPreferences prefs;

    @Before public void setUp() {
        Context app = RuntimeEnvironment.getApplication();
        Settings.Global.putInt(app.getContentResolver(), Settings.Global.BOOT_COUNT, 10);
        prefs = app.getSharedPreferences(TimerAlarmReceiver.PREFS, Context.MODE_PRIVATE);
        prefs.edit().clear().putBoolean(TimerAlarmReceiver.PREF_VOICE_PROMPTS, false).commit();
        alarms = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        Shadows.shadowOf(alarms).setCanScheduleExactAlarms(true);
        controller = Robolectric.buildActivity(MainActivity.class).setup();
        activity = controller.get();
    }

    @After public void tearDown() {
        PromptPlayer.cancel("meat"); PromptPlayer.cancel("breath");
        if (controller != null) controller.pause().stop().destroy();
    }

    private Object field(String name) throws Exception {
        Field f = MainActivity.class.getDeclaredField(name); f.setAccessible(true); return f.get(activity);
    }
    private void call(String name) throws Exception {
        Method m = MainActivity.class.getDeclaredMethod(name); m.setAccessible(true); m.invoke(activity);
    }
    private EditText input(String name) throws Exception { return (EditText) field(name); }
    private long finishAt() {
        return Shadows.shadowOf(alarms).getScheduledAlarms().stream().mapToLong(a -> a.triggerAtTime).max().orElse(-1L);
    }

    @Test public void resumedDeadlineIsExactAndMinuteNumberContinues() throws Exception {
        input("meatMinutesInput").setText("3"); call("startMeat");
        ShadowSystemClock.advanceBy(Duration.ofMillis(90500)); call("pauseMeat");
        ShadowSystemClock.advanceBy(Duration.ofSeconds(20)); call("startMeat");
        assertEquals(prefs.getLong("meatStarted", 0L) + prefs.getLong("meatDuration", 0L), finishAt());
        assertTrue(Math.abs(finishAt() - SystemClock.elapsedRealtime() - 89500) < 10);
        assertEquals(2, Shadows.shadowOf(alarms).getScheduledAlarms().size());
        long firstAt = Shadows.shadowOf(alarms).getScheduledAlarms().stream().mapToLong(a -> a.triggerAtTime).min().getAsLong();
        assertEquals(finishAt() - 60000, firstAt);
        android.app.PendingIntent first = Shadows.shadowOf(alarms).getScheduledAlarms().stream().filter(a -> a.triggerAtTime == firstAt).findFirst().get().operation;
        assertEquals(2, Shadows.shadowOf(first).getSavedIntent().getIntExtra(TimerAlarmReceiver.EXTRA_MINUTE, -1));
    }

    @Test public void breathingPhaseResumesInsteadOfRestarting() throws Exception {
        call("startBreath"); ShadowSystemClock.advanceBy(Duration.ofMillis(5500)); call("pauseBreath");
        ShadowSystemClock.advanceBy(Duration.ofSeconds(15)); call("startBreath");
        Method m = MainActivity.class.getDeclaredMethod("currentBreathPhase"); m.setAccessible(true);
        TimerMath.BreathPhase phase = (TimerMath.BreathPhase) m.invoke(activity);
        assertEquals("exhale", phase.phase); assertEquals(5, phase.remainingSeconds);
    }

    @Test public void deniedExactAlarmDoesNotCommitRunningState() throws Exception {
        Shadows.shadowOf(alarms).setCanScheduleExactAlarms(false);
        call("startMeat"); call("startBreath");
        assertFalse(prefs.getBoolean("meatRunning", false));
        assertFalse(prefs.getBoolean("breathRunning", false));
        assertTrue(Shadows.shadowOf(alarms).getScheduledAlarms().isEmpty());
    }

    @Test public void rebootInvalidatesRuntimeButPreservesSettings() throws Exception {
        input("meatMinutesInput").setText("23"); call("startMeat");
        Settings.Global.putInt(activity.getContentResolver(), Settings.Global.BOOT_COUNT, 11);
        controller.pause().stop().destroy();
        controller = Robolectric.buildActivity(MainActivity.class).setup(); activity = controller.get();
        assertFalse(prefs.getBoolean("meatRunning", false));
        assertEquals("23", input("meatMinutesInput").getText().toString());
        assertEquals("00:00", ((android.widget.TextView) field("meatDisplay")).getText().toString());
    }

    @Test public void settingsPersistBeforeStartIncludingPresetEquivalent() throws Exception {
        input("inhaleInput").setText("7"); input("breathMinutesInput").setText("10");
        input("exhaleInput").setText(""); // temporary invalid input must not replace the previous valid value
        controller.pause().stop().destroy();
        controller = Robolectric.buildActivity(MainActivity.class).setup(); activity = controller.get();
        assertEquals("7", input("inhaleInput").getText().toString());
        assertEquals("10", input("breathMinutesInput").getText().toString());
        assertEquals("6", input("exhaleInput").getText().toString());
    }

    @Test public void presetAccessibleDescriptionTracksExpansion() {
        Button button = findPreset(activity.getWindow().getDecorView()); assertNotNull(button);
        assertTrue(button.getContentDescription().toString().contains("свёрнуто"));
        button.performClick(); assertTrue(button.getContentDescription().toString().contains("развёрнуто"));
        button.performClick(); assertTrue(button.getContentDescription().toString().contains("свёрнуто"));
    }
    @Test public void sameBootRecreationKeepsRunningAndPausedTimerSurvivesReboot() throws Exception {
        call("startMeat");
        controller.pause().stop().destroy();
        controller = Robolectric.buildActivity(MainActivity.class).setup(); activity = controller.get();
        assertTrue(prefs.getBoolean("meatRunning", false));
        ShadowSystemClock.advanceBy(Duration.ofSeconds(10)); call("pauseMeat");
        long remaining = prefs.getLong("meatRemaining", 0);
        Settings.Global.putInt(activity.getContentResolver(), Settings.Global.BOOT_COUNT, 11);
        controller.pause().stop().destroy();
        controller = Robolectric.buildActivity(MainActivity.class).setup(); activity = controller.get();
        assertEquals(remaining, prefs.getLong("meatRemaining", 0));
        call("startMeat");
        assertTrue(Math.abs(finishAt() - SystemClock.elapsedRealtime() - remaining) < 10);
    }

    @Test public void subMinuteResumeStillGetsExactFinish() throws Exception {
        input("breathMinutesInput").setText("1"); call("startBreath");
        ShadowSystemClock.advanceBy(Duration.ofMillis(59800)); call("pauseBreath");
        ShadowSystemClock.advanceBy(Duration.ofSeconds(7)); call("startBreath");
        assertEquals(1, Shadows.shadowOf(alarms).getScheduledAlarms().size());
        assertTrue(Math.abs(finishAt() - SystemClock.elapsedRealtime() - 200) < 10);
    }

    @Test public void playbackSerializesAndCancellationIsPerTimer() {
        java.util.List<android.media.MediaPlayer> players = new java.util.ArrayList<>();
        org.robolectric.shadows.ShadowMediaPlayer.setMediaInfoProvider(source -> new org.robolectric.shadows.ShadowMediaPlayer.MediaInfo(5000, 0));
        org.robolectric.shadows.ShadowMediaPlayer.setCreateListener((player, shadow) -> players.add(player));
        PromptPlayer.playStart(activity, "meat", "ru");
        assertEquals(1, players.size()); assertTrue(players.get(0).isPlaying());
        PromptPlayer.playStart(activity, "breath", "ru");
        assertEquals(1, players.size());
        assertFalse(PromptPlayer.playPhase(activity, "inhale", "ru"));
        PromptPlayer.cancel("meat");
        assertEquals(org.robolectric.shadows.ShadowMediaPlayer.State.END, Shadows.shadowOf(players.get(0)).getState());
        assertEquals(2, players.size()); assertTrue(players.get(1).isPlaying());
        PromptPlayer.cancel("meat"); assertTrue(players.get(1).isPlaying());
        Shadows.shadowOf(players.get(1)).invokeCompletionListener();
        assertTrue(PromptPlayer.playPhase(activity, "exhale", "ru"));
        assertEquals(3, players.size());
        PromptPlayer.cancel("breath");
        assertEquals(org.robolectric.shadows.ShadowMediaPlayer.State.END, Shadows.shadowOf(players.get(2)).getState());
    }

    @Test public void canceledMinuteSequenceCompletesCallbackExactlyOnce() {
        java.util.List<android.media.MediaPlayer> players = new java.util.ArrayList<>();
        org.robolectric.shadows.ShadowMediaPlayer.setMediaInfoProvider(source -> new org.robolectric.shadows.ShadowMediaPlayer.MediaInfo(5000, 0));
        org.robolectric.shadows.ShadowMediaPlayer.setCreateListener((player, shadow) -> players.add(player));
        java.util.concurrent.atomic.AtomicInteger done = new java.util.concurrent.atomic.AtomicInteger();
        PromptPlayer.playPrompt(activity, "meat", "minute", 2, "ru", done::incrementAndGet);
        assertEquals(1, players.size());
        Shadows.shadowOf(players.get(0)).invokeCompletionListener(); assertEquals(2, players.size());
        PromptPlayer.cancel("meat"); PromptPlayer.cancel("meat");
        assertEquals(1, done.get()); assertEquals(2, players.size());
    }

    @Test public void canceledAlarmCannotReplayOrClearAnotherRun() throws Exception {
        input("meatMinutesInput").setText("1"); call("startMeat");
        android.content.Intent old = new android.content.Intent(Shadows.shadowOf(Shadows.shadowOf(alarms).getScheduledAlarms().get(0).operation).getSavedIntent());
        call("resetMeat"); ShadowSystemClock.advanceBy(Duration.ofSeconds(5)); call("startMeat");
        long deadline = prefs.getLong("meatAlarmDeadline", 0);
        activity.sendBroadcast(old); Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertTrue(prefs.getBoolean("meatRunning", false));
        assertEquals(deadline, prefs.getLong("meatAlarmDeadline", 0));
    }

    @Test public void actualFinishBroadcastClearsStateAndPostsOneAlert() throws Exception {
        input("meatMinutesInput").setText("1"); call("startMeat");
        android.content.Intent finish = Shadows.shadowOf(Shadows.shadowOf(alarms).getScheduledAlarms().get(0).operation).getSavedIntent();
        ShadowSystemClock.advanceBy(Duration.ofSeconds(60));
        activity.sendBroadcast(finish); Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertFalse(prefs.getBoolean("meatRunning", false));
        assertEquals(0L, prefs.getLong("meatAlarmDeadline", -1));
        android.app.NotificationManager manager = (android.app.NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        assertNotNull(Shadows.shadowOf(manager).getNotification(TimerAlarmReceiver.MEAT_ALERT_NOTIFICATION_ID));
    }

    @org.robolectric.annotation.Implements(AlarmManager.class)
    public static class RevokedAlarmManager extends org.robolectric.shadows.ShadowAlarmManager {
        static int attempts;
        @org.robolectric.annotation.Implementation
        protected void setExactAndAllowWhileIdle(int type, long at, android.app.PendingIntent operation) {
            if (++attempts == 2) throw new SecurityException("revoked during scheduling");
            super.setExactAndAllowWhileIdle(type, at, operation);
        }
    }

    @Test @Config(shadows = RevokedAlarmManager.class)
    public void permissionRaceRollsBackPartialAlarmsAndDoesNotStart() throws Exception {
        RevokedAlarmManager.attempts = 0;
        input("meatMinutesInput").setText("2"); call("startMeat");
        assertFalse(prefs.getBoolean("meatRunning", false));
        assertEquals(0L, prefs.getLong("meatAlarmDeadline", 0));
        assertTrue(Shadows.shadowOf(alarms).getScheduledAlarms().isEmpty());
    }

    @Test public void keeperExpiresResumedTimerWithNegativeVirtualStart() throws Exception {
        prefs.edit().putBoolean("meatRunning", true).putLong("meatStarted", -60000L)
            .putLong("meatDuration", 60001L).apply();
        org.robolectric.android.controller.ServiceController<TimerForegroundService> keeper =
            Robolectric.buildService(TimerForegroundService.class).create();
        Method refresh = TimerForegroundService.class.getDeclaredMethod("refreshExpiredTimers");
        refresh.setAccessible(true); refresh.invoke(keeper.get());
        assertFalse(prefs.getBoolean("meatRunning", true));
        keeper.destroy();
    }

    private Button findPreset(View view) {
        if (view instanceof Button && ((Button) view).getText().toString().startsWith("Показать:")) return (Button) view;
        if (view instanceof ViewGroup) for (int i=0; i<((ViewGroup)view).getChildCount(); i++) {
            Button b = findPreset(((ViewGroup)view).getChildAt(i)); if (b != null) return b;
        }
        return null;
    }
}
