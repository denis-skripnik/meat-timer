package xyz.blinddev.cookbreathe;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends android.app.Activity {
    private static final String PREFS = "cook_breathe_prefs";
    private static final String KIND_MEAT = "meat";
    private static final String KIND_BREATH = "breath";

    private SharedPreferences prefs;
    private TimerScheduler scheduler;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private EditText meatMinutesInput;
    private TextView meatDisplay;
    private Button meatToggle;
    private CheckBox voicePromptsCheckbox;
    private String lastSpokenBreathPhase = "";

    private EditText breathMinutesInput;
    private EditText inhaleInput;
    private EditText exhaleInput;
    private TextView breathDisplay;
    private TextView breathPhaseDisplay;
    private Button breathToggle;

    private TimerState meatState = TimerState.idle();
    private TimerState breathState = TimerState.idle();

    private final Runnable uiTicker = new Runnable() {
        @Override public void run() {
            updateRunningDisplays();
            handler.postDelayed(this, 500L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        scheduler = new TimerScheduler(this);
        TimerAlarmReceiver.ensureChannel(this);
        requestNotificationPermissionIfNeeded();
        restoreState();
        setContentView(buildUi());
        updateAllDisplays();
        handler.post(uiTicker);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(uiTicker);
        super.onDestroy();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(32));
        scroll.addView(root);

        TextView title = heading("Cook & Breathe");
        title.setText("Cook & Breathe — таймер мяса и дыхания");
        root.addView(title);
        root.addView(paragraph("Нативная Android-версия использует системные уведомления и точные alarm-события, поэтому лучше работает в фоне, чем PWA в браузере."));

        if (!scheduler.canScheduleExactAlarms()) {
            root.addView(paragraph("Для точных уведомлений разрешите точные будильники для приложения."));
            Button exactButton = button("Разрешить точные уведомления");
            exactButton.setOnClickListener(v -> openExactAlarmSettings());
            root.addView(exactButton);
        }

        voicePromptsCheckbox = new CheckBox(this);
        voicePromptsCheckbox.setText("Озвучивать подсказки поверх музыки");
        voicePromptsCheckbox.setTextSize(18);
        voicePromptsCheckbox.setChecked(prefs.getBoolean(TimerAlarmReceiver.PREF_VOICE_PROMPTS, true));
        voicePromptsCheckbox.setContentDescription("Озвучивать подсказки таймера поверх музыки");
        voicePromptsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean(TimerAlarmReceiver.PREF_VOICE_PROMPTS, isChecked).apply());
        root.addView(voicePromptsCheckbox);
        root.addView(paragraph("Озвучка использует короткий audio focus с ducking: музыка обычно продолжает играть и только слегка приглушается на время фразы."));

        root.addView(sectionTitle("Готовка мяса"));
        meatMinutesInput = numberInput("Длительность готовки в минутах", prefInt("meatMinutes", 10), 1, 120);
        root.addView(labeled("Длительность, минут", meatMinutesInput));
        root.addView(presetRow(new int[]{5, 10, 20, 30, 40, 50, 60}, value -> meatMinutesInput.setText(String.valueOf(value))));
        meatToggle = button("Запуск");
        meatToggle.setOnClickListener(v -> toggleMeat());
        root.addView(meatToggle);
        Button meatReset = button("Сброс готовки");
        meatReset.setOnClickListener(v -> resetMeat());
        root.addView(meatReset);
        meatDisplay = timerText();
        root.addView(meatDisplay);
        root.addView(paragraph("Каждую минуту придёт системное уведомление: пора перевернуть мясо. В конце придёт отдельное уведомление."));

        root.addView(sectionTitle("Практики дыхания"));
        breathMinutesInput = numberInput("Длительность практики в минутах", prefInt("breathMinutes", 5), 1, 120);
        inhaleInput = numberInput("Вдох в секундах", prefInt("inhaleSeconds", 4), 1, 30);
        exhaleInput = numberInput("Выдох в секундах", prefInt("exhaleSeconds", 6), 1, 30);
        root.addView(labeled("Практика, минут", breathMinutesInput));
        root.addView(presetRow(new int[]{1, 2, 3, 5, 10, 15, 20, 30}, value -> breathMinutesInput.setText(String.valueOf(value))));
        root.addView(labeled("Вдох, секунд", inhaleInput));
        root.addView(presetRow(new int[]{1,2,3,4,5,6,7,8,9,10}, value -> inhaleInput.setText(String.valueOf(value))));
        root.addView(labeled("Выдох, секунд", exhaleInput));
        root.addView(presetRow(new int[]{1,2,3,4,5,6,7,8,9,10}, value -> exhaleInput.setText(String.valueOf(value))));
        breathToggle = button("Запуск");
        breathToggle.setOnClickListener(v -> toggleBreath());
        root.addView(breathToggle);
        Button breathReset = button("Сброс дыхания");
        breathReset.setOnClickListener(v -> resetBreath());
        root.addView(breathReset);
        breathDisplay = timerText();
        root.addView(breathDisplay);
        breathPhaseDisplay = paragraph("Готово");
        breathPhaseDisplay.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(breathPhaseDisplay);
        root.addView(paragraph("Фазы вдоха и выдоха показываются на экране. Минутные и финальные напоминания уходят через системные уведомления."));

        return scroll;
    }

    private void toggleMeat() {
        if (meatState.running) pauseMeat(); else startMeat();
    }

    private void startMeat() {
        int minutes = readInput(meatMinutesInput, 1, 120, 10);
        saveSettings();
        if (meatState.remainingMillis <= 0L) meatState = TimerState.start(minutes);
        else meatState = meatState.resume();
        scheduler.scheduleTimer(KIND_MEAT, meatState.startedElapsedMs, (int) Math.ceil(meatState.durationMillis / 60_000.0));
        playStartPrompt(KIND_MEAT);
        persistRuntimeState();
        updateAllDisplays();
    }

    private void pauseMeat() {
        meatState = meatState.pause();
        scheduler.cancelTimer(KIND_MEAT);
        persistRuntimeState();
        updateAllDisplays();
    }

    private void resetMeat() {
        meatState = TimerState.idle();
        scheduler.cancelTimer(KIND_MEAT);
        persistRuntimeState();
        updateAllDisplays();
    }

    private void toggleBreath() {
        if (breathState.running) pauseBreath(); else startBreath();
    }

    private void startBreath() {
        int minutes = readInput(breathMinutesInput, 1, 120, 5);
        saveSettings();
        if (breathState.remainingMillis <= 0L) breathState = TimerState.start(minutes);
        else breathState = breathState.resume();
        scheduler.scheduleTimer(KIND_BREATH, breathState.startedElapsedMs, (int) Math.ceil(breathState.durationMillis / 60_000.0));
        playStartPrompt(KIND_BREATH);
        lastSpokenBreathPhase = "";
        persistRuntimeState();
        updateAllDisplays();
    }

    private void pauseBreath() {
        breathState = breathState.pause();
        scheduler.cancelTimer(KIND_BREATH);
        lastSpokenBreathPhase = "";
        persistRuntimeState();
        updateAllDisplays();
    }

    private void resetBreath() {
        breathState = TimerState.idle();
        scheduler.cancelTimer(KIND_BREATH);
        lastSpokenBreathPhase = "";
        persistRuntimeState();
        updateAllDisplays();
    }

    private void updateRunningDisplays() {
        if (meatState.running) meatState = meatState.refresh();
        if (breathState.running) breathState = breathState.refresh();
        if (meatState.running || breathState.running) persistRuntimeState();
        updateAllDisplays();
    }

    private void updateAllDisplays() {
        if (meatDisplay != null) meatDisplay.setText(TimerMath.format(meatState.remainingMillis));
        if (breathDisplay != null) breathDisplay.setText(TimerMath.format(breathState.remainingMillis));
        if (meatToggle != null) meatToggle.setText(meatState.running ? "Пауза" : "Запуск");
        if (breathToggle != null) breathToggle.setText(breathState.running ? "Пауза" : "Запуск");
        if (breathPhaseDisplay != null) breathPhaseDisplay.setText(currentBreathPhaseText());
        speakBreathPhaseIfNeeded();
        if (meatState.running && meatState.remainingMillis <= 0L) resetMeat();
        if (breathState.running && breathState.remainingMillis <= 0L) resetBreath();
    }

    private String currentBreathPhaseText() {
        if (!breathState.running || breathState.remainingMillis <= 0L) return "Готово";
        long elapsed = Math.max(0L, SystemClock.elapsedRealtime() - breathState.startedElapsedMs);
        TimerMath.BreathPhase phase = TimerMath.breathPhase(elapsed, readInput(inhaleInput, 1, 30, 4), readInput(exhaleInput, 1, 30, 6));
        return ("inhale".equals(phase.phase) ? "Вдох" : "Выдох") + ", осталось " + phase.remainingSeconds + " сек.";
    }

    private void speakBreathPhaseIfNeeded() {
        if (!breathState.running || breathState.remainingMillis <= 0L || !voicePromptsEnabled()) return;
        long elapsed = Math.max(0L, SystemClock.elapsedRealtime() - breathState.startedElapsedMs);
        TimerMath.BreathPhase phase = TimerMath.breathPhase(elapsed, readInput(inhaleInput, 1, 30, 4), readInput(exhaleInput, 1, 30, 6));
        if (!phase.phase.equals(lastSpokenBreathPhase)) {
            lastSpokenBreathPhase = phase.phase;
            PromptPlayer.playPhase(this, phase.phase, currentLanguage());
        }
    }

    private void playStartPrompt(String kind) {
        if (voicePromptsEnabled()) PromptPlayer.playStart(this, kind, currentLanguage());
    }

    private boolean voicePromptsEnabled() {
        return voicePromptsCheckbox == null || voicePromptsCheckbox.isChecked();
    }

    private String currentLanguage() {
        return prefs.getString(TimerAlarmReceiver.PREF_LANGUAGE, "ru");
    }

    private void saveSettings() {
        prefs.edit()
            .putInt("meatMinutes", readInput(meatMinutesInput, 1, 120, 10))
            .putInt("breathMinutes", readInput(breathMinutesInput, 1, 120, 5))
            .putInt("inhaleSeconds", readInput(inhaleInput, 1, 30, 4))
            .putInt("exhaleSeconds", readInput(exhaleInput, 1, 30, 6))
            .putBoolean(TimerAlarmReceiver.PREF_VOICE_PROMPTS, voicePromptsEnabled())
            .putString(TimerAlarmReceiver.PREF_LANGUAGE, currentLanguage())
            .apply();
    }

    private void restoreState() {
        meatState = TimerState.fromPrefs(prefs, "meat");
        breathState = TimerState.fromPrefs(prefs, "breath");
    }

    private void persistRuntimeState() {
        SharedPreferences.Editor editor = prefs.edit();
        meatState.toPrefs(editor, "meat");
        breathState.toPrefs(editor, "breath");
        editor.apply();
    }

    private int prefInt(String key, int fallback) { return prefs.getInt(key, fallback); }

    private int readInput(EditText input, int min, int max, int fallback) {
        try {
            int value = Integer.parseInt(input.getText().toString().trim());
            return Math.max(min, Math.min(max, value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private TextView heading(String text) { TextView v = new TextView(this); v.setText(text); v.setTextSize(26); v.setGravity(Gravity.CENTER_HORIZONTAL); v.setPadding(0, 0, 0, dp(12)); return v; }
    private TextView sectionTitle(String text) { TextView v = new TextView(this); v.setText(text); v.setTextSize(22); v.setPadding(0, dp(24), 0, dp(8)); return v; }
    private TextView paragraph(String text) { TextView v = new TextView(this); v.setText(text); v.setTextSize(16); v.setPadding(0, dp(4), 0, dp(8)); return v; }
    private TextView timerText() { TextView v = new TextView(this); v.setTextSize(42); v.setGravity(Gravity.CENTER_HORIZONTAL); v.setPadding(0, dp(12), 0, dp(12)); return v; }
    private Button button(String text) { Button b = new Button(this); b.setText(text); b.setAllCaps(false); b.setTextSize(18); b.setPadding(dp(8), dp(8), dp(8), dp(8)); return b; }

    private EditText numberInput(String label, int value, int min, int max) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(Math.max(min, Math.min(max, value))));
        input.setHint(label);
        input.setContentDescription(label);
        return input;
    }

    private View labeled(String label, View child) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView l = paragraph(label);
        box.addView(l);
        box.addView(child);
        return box;
    }

    private interface PresetHandler { void apply(int value); }

    private View presetRow(int[] values, PresetHandler handler) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(8));
        for (int value : values) {
            Button b = button(String.valueOf(value));
            b.setTextSize(15);
            b.setContentDescription("Быстрый выбор: " + value);
            b.setOnClickListener(v -> handler.apply(value));
            row.addView(b);
        }
        return row;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private static final class TimerState {
        final boolean running;
        final long startedElapsedMs;
        final long durationMillis;
        final long remainingMillis;

        TimerState(boolean running, long startedElapsedMs, long durationMillis, long remainingMillis) {
            this.running = running;
            this.startedElapsedMs = startedElapsedMs;
            this.durationMillis = durationMillis;
            this.remainingMillis = remainingMillis;
        }

        static TimerState idle() { return new TimerState(false, 0L, 0L, 0L); }
        static TimerState start(int minutes) { long now = SystemClock.elapsedRealtime(); long duration = TimerMath.durationMillis(minutes); return new TimerState(true, now, duration, duration); }
        TimerState refresh() { if (!running) return this; long deadline = startedElapsedMs + durationMillis; return new TimerState(remainingMillis(deadline) > 0L, startedElapsedMs, durationMillis, remainingMillis(deadline)); }
        TimerState pause() { TimerState refreshed = refresh(); return new TimerState(false, 0L, refreshed.durationMillis, refreshed.remainingMillis); }
        TimerState resume() { long now = SystemClock.elapsedRealtime(); long duration = remainingMillis > 0L ? remainingMillis : durationMillis; return new TimerState(true, now, duration, duration); }
        private long remainingMillis(long deadline) { return TimerMath.remainingMillis(deadline, SystemClock.elapsedRealtime()); }

        static TimerState fromPrefs(SharedPreferences prefs, String prefix) {
            boolean running = prefs.getBoolean(prefix + "Running", false);
            long started = prefs.getLong(prefix + "Started", 0L);
            long duration = prefs.getLong(prefix + "Duration", 0L);
            long remaining = prefs.getLong(prefix + "Remaining", 0L);
            TimerState state = new TimerState(running, started, duration, remaining);
            return running ? state.refresh() : state;
        }

        void toPrefs(SharedPreferences.Editor editor, String prefix) {
            editor.putBoolean(prefix + "Running", running)
                .putLong(prefix + "Started", startedElapsedMs)
                .putLong(prefix + "Duration", durationMillis)
                .putLong(prefix + "Remaining", remainingMillis);
        }
    }
}
