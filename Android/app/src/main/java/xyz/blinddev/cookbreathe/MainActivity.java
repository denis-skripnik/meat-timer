package xyz.blinddev.cookbreathe;

import android.Manifest;
import android.app.AlarmManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends android.app.Activity {
    private static final String PREFS = "cook_breathe_prefs";
    private static final String KIND_MEAT = "meat";
    private static final String KIND_BREATH = "breath";
    private static final int COLOR_BACKGROUND = Color.rgb(5, 5, 7);
    private static final int COLOR_PANEL = Color.rgb(24, 24, 28);
    private static final int COLOR_TEXT = Color.rgb(245, 245, 245);
    private static final int COLOR_HINT = Color.rgb(207, 207, 207);
    private static final int COLOR_ACCENT = Color.rgb(184, 77, 0);

    private SharedPreferences prefs;
    private TimerScheduler scheduler;
    private TextToSpeech guideTts;
    private boolean guideTtsReady = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private EditText meatMinutesInput;
    private TextView meatDisplay;
    private Button meatToggle;
    private Button meatTabButton;
    private Button breathTabButton;
    private LinearLayout meatPanel;
    private LinearLayout breathPanel;
    private FlameView flameView;
    private BreathOrbView breathOrbView;
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
        guideTts = new TextToSpeech(this, status -> {
            guideTtsReady = status == TextToSpeech.SUCCESS;
            if (guideTtsReady) guideTts.setLanguage(new Locale("ru", "RU"));
        });
        restoreState();
        setContentView(buildUi());
        updateAllDisplays();
        handler.post(uiTicker);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(uiTicker);
        if (guideTts != null) {
            guideTts.stop();
            guideTts.shutdown();
        }
        super.onDestroy();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(32));
        root.setBackgroundColor(COLOR_BACKGROUND);
        scroll.setBackgroundColor(COLOR_BACKGROUND);
        scroll.addView(root);

        TextView title = heading("Cook & Breathe");
        title.setText("Cook & Breathe — таймер мяса и дыхания");
        root.addView(title);
        root.addView(paragraph("Нативная Android-версия использует системные уведомления, точные alarm-события и голосовые подсказки поверх музыки."));

        if (!scheduler.canScheduleExactAlarms()) {
            root.addView(paragraph("Для точных уведомлений разрешите точные будильники для приложения."));
            Button exactButton = button("Разрешить точные уведомления");
            exactButton.setOnClickListener(v -> openExactAlarmSettings());
            root.addView(exactButton);
        }

        if (!isIgnoringBatteryOptimizations()) {
            root.addView(paragraph("Если минутные подсказки всё равно опаздывают после блокировки экрана, откройте настройки батареи приложения и выберите режим без ограничений. На разных Android это может называться: Без ограничений, Не оптимизировать, Разрешить работу в фоне."));
            Button batteryButton = button("Открыть настройки батареи приложения");
            batteryButton.setContentDescription("Открыть настройки приложения, чтобы отключить ограничения расхода энергии для Cook & Breathe");
            batteryButton.setOnClickListener(v -> openBatterySettings());
            root.addView(batteryButton);
        }

        voicePromptsCheckbox = new CheckBox(this);
        voicePromptsCheckbox.setText("Озвучивать подсказки поверх музыки");
        voicePromptsCheckbox.setTextSize(18);
        voicePromptsCheckbox.setTextColor(COLOR_TEXT);
        voicePromptsCheckbox.setChecked(prefs.getBoolean(TimerAlarmReceiver.PREF_VOICE_PROMPTS, true));
        voicePromptsCheckbox.setContentDescription("Озвучивать подсказки таймера поверх музыки");
        voicePromptsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean(TimerAlarmReceiver.PREF_VOICE_PROMPTS, isChecked).apply());
        root.addView(voicePromptsCheckbox);
        root.addView(paragraph("Озвучка использует короткий audio focus с ducking: музыка обычно продолжает играть и только слегка приглушается на время фразы."));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER_HORIZONTAL);
        tabs.setPadding(0, dp(12), 0, dp(12));
        meatTabButton = button("Готовка мяса");
        breathTabButton = button("Практики дыхания");
        meatTabButton.setContentDescription("Вкладка: готовка мяса");
        breathTabButton.setContentDescription("Вкладка: практики дыхания");
        meatTabButton.setOnClickListener(v -> switchMode(KIND_MEAT));
        breathTabButton.setOnClickListener(v -> switchMode(KIND_BREATH));
        tabs.addView(meatTabButton);
        tabs.addView(breathTabButton);
        root.addView(tabs);

        meatPanel = new LinearLayout(this);
        meatPanel.setOrientation(LinearLayout.VERTICAL);
        meatPanel.setPadding(dp(12), dp(12), dp(12), dp(12));
        meatPanel.setBackground(panelBackground());
        meatPanel.setContentDescription("Панель готовки мяса");
        buildMeatPanel(meatPanel);
        root.addView(meatPanel);

        breathPanel = new LinearLayout(this);
        breathPanel.setOrientation(LinearLayout.VERTICAL);
        breathPanel.setPadding(dp(12), dp(12), dp(12), dp(12));
        breathPanel.setBackground(panelBackground());
        breathPanel.setContentDescription("Панель практик дыхания");
        buildBreathPanel(breathPanel);
        root.addView(breathPanel);

        switchMode(prefs.getString("activeMode", KIND_MEAT));
        return scroll;
    }

    private void buildMeatPanel(LinearLayout panel) {
        panel.addView(sectionTitle("Готовка мяса"));
        flameView = new FlameView(this);
        panel.addView(flameView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(150)));
        meatMinutesInput = numberInput("Длительность готовки в минутах", prefInt("meatMinutes", 10), 1, 120);
        panel.addView(labeled("Длительность, минут", meatMinutesInput));
        panel.addView(collapsiblePresetPanel("Быстрый выбор длительности готовки", new int[]{5, 10, 20, 30, 40, 50, 60}, value -> meatMinutesInput.setText(String.valueOf(value))));
        meatToggle = button("Запуск");
        meatToggle.setOnClickListener(v -> toggleMeat());
        panel.addView(meatToggle);
        Button meatReset = button("Сброс готовки");
        meatReset.setOnClickListener(v -> resetMeat());
        panel.addView(meatReset);
        meatDisplay = timerText();
        panel.addView(meatDisplay);
        panel.addView(paragraph("Каждую минуту придёт системное уведомление и прозвучит подсказка: пора перевернуть мясо. В конце придёт отдельное уведомление."));
    }

    private void buildBreathPanel(LinearLayout panel) {
        panel.addView(sectionTitle("Практики дыхания"));
        breathOrbView = new BreathOrbView(this);
        panel.addView(breathOrbView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(140)));
        breathMinutesInput = numberInput("Длительность практики в минутах", prefInt("breathMinutes", 5), 1, 120);
        inhaleInput = numberInput("Вдох в секундах", prefInt("inhaleSeconds", 4), 1, 30);
        exhaleInput = numberInput("Выдох в секундах", prefInt("exhaleSeconds", 6), 1, 30);
        panel.addView(labeled("Практика, минут", breathMinutesInput));
        panel.addView(collapsiblePresetPanel("Быстрый выбор длительности практики", new int[]{1, 2, 3, 5, 10, 15, 20, 30}, value -> breathMinutesInput.setText(String.valueOf(value))));
        panel.addView(breathGuidePanel());
        panel.addView(labeled("Вдох, секунд", inhaleInput));
        panel.addView(collapsiblePresetPanel("Быстрый выбор вдоха", new int[]{1,2,3,4,5,6,7,8,9,10}, value -> inhaleInput.setText(String.valueOf(value))));
        panel.addView(labeled("Выдох, секунд", exhaleInput));
        panel.addView(collapsiblePresetPanel("Быстрый выбор выдоха", new int[]{1,2,3,4,5,6,7,8,9,10}, value -> exhaleInput.setText(String.valueOf(value))));
        breathToggle = button("Запуск");
        breathToggle.setOnClickListener(v -> toggleBreath());
        panel.addView(breathToggle);
        Button breathReset = button("Сброс дыхания");
        breathReset.setOnClickListener(v -> resetBreath());
        panel.addView(breathReset);
        breathDisplay = timerText();
        panel.addView(breathDisplay);
        breathPhaseDisplay = paragraph("Готово");
        breathPhaseDisplay.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.addView(breathPhaseDisplay);
        panel.addView(paragraph("Фазы вдоха и выдоха показываются на экране. Минутные и финальные напоминания уходят через системные уведомления и голосовые подсказки."));
    }

    private void switchMode(String mode) {
        boolean meatActive = !KIND_BREATH.equals(mode);
        if (meatPanel != null) meatPanel.setVisibility(meatActive ? View.VISIBLE : View.GONE);
        if (breathPanel != null) breathPanel.setVisibility(meatActive ? View.GONE : View.VISIBLE);
        if (meatTabButton != null) meatTabButton.setEnabled(!meatActive);
        if (breathTabButton != null) breathTabButton.setEnabled(meatActive);
        if (meatTabButton != null) meatTabButton.setBackground(buttonBackground(meatActive));
        if (breathTabButton != null) breathTabButton.setBackground(buttonBackground(!meatActive));
        if (meatTabButton != null) meatTabButton.setContentDescription(meatActive ? "Выбрана вкладка: готовка мяса" : "Вкладка: готовка мяса");
        if (breathTabButton != null) breathTabButton.setContentDescription(meatActive ? "Вкладка: практики дыхания" : "Выбрана вкладка: практики дыхания");
        prefs.edit().putString("activeMode", meatActive ? KIND_MEAT : KIND_BREATH).apply();
    }

    private void toggleMeat() {
        if (meatState.running) pauseMeat(); else startMeat();
    }

    private void startMeat() {
        int minutes = readInput(meatMinutesInput, 1, 120, 10);
        saveSettings();
        if (meatState.remainingMillis <= 0L) meatState = TimerState.start(minutes);
        else meatState = meatState.resume();
        persistRuntimeState();
        startTimerKeeper();
        scheduler.scheduleTimer(KIND_MEAT, meatState.startedElapsedMs, (int) Math.ceil(meatState.durationMillis / 60_000.0));
        playStartPrompt(KIND_MEAT);
        updateAllDisplays();
    }

    private void pauseMeat() {
        meatState = meatState.pause();
        scheduler.cancelTimer(KIND_MEAT);
        persistRuntimeState();
        updateTimerKeeper();
        updateAllDisplays();
    }

    private void resetMeat() {
        meatState = TimerState.idle();
        scheduler.cancelTimer(KIND_MEAT);
        persistRuntimeState();
        updateTimerKeeper();
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
        persistRuntimeState();
        startTimerKeeper();
        scheduler.scheduleTimer(KIND_BREATH, breathState.startedElapsedMs, (int) Math.ceil(breathState.durationMillis / 60_000.0));
        playStartPrompt(KIND_BREATH);
        lastSpokenBreathPhase = "";
        updateAllDisplays();
    }

    private void pauseBreath() {
        breathState = breathState.pause();
        scheduler.cancelTimer(KIND_BREATH);
        lastSpokenBreathPhase = "";
        persistRuntimeState();
        updateTimerKeeper();
        updateAllDisplays();
    }

    private void resetBreath() {
        breathState = TimerState.idle();
        scheduler.cancelTimer(KIND_BREATH);
        lastSpokenBreathPhase = "";
        persistRuntimeState();
        updateTimerKeeper();
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
        updateBreathOrb();
        speakBreathPhaseIfNeeded();
        if (meatState.running && meatState.remainingMillis <= 0L) resetMeat();
        if (breathState.running && breathState.remainingMillis <= 0L) resetBreath();
    }

    private String currentBreathPhaseText() {
        if (!breathState.running || breathState.remainingMillis <= 0L) return "Готово";
        TimerMath.BreathPhase phase = currentBreathPhase();
        return ("inhale".equals(phase.phase) ? "Вдох" : "Выдох") + ", осталось " + phase.remainingSeconds + " сек.";
    }

    private TimerMath.BreathPhase currentBreathPhase() {
        long elapsed = Math.max(0L, SystemClock.elapsedRealtime() - breathState.startedElapsedMs);
        return TimerMath.breathPhase(elapsed, readInput(inhaleInput, 1, 30, 4), readInput(exhaleInput, 1, 30, 6));
    }

    private void updateBreathOrb() {
        if (breathOrbView == null) return;
        if (!breathState.running || breathState.remainingMillis <= 0L) {
            breathOrbView.resetOrb();
            return;
        }
        breathOrbView.setPhase(currentBreathPhase().phase);
    }

    private void speakBreathPhaseIfNeeded() {
        if (!breathState.running || breathState.remainingMillis <= 0L || !voicePromptsEnabled()) return;
        TimerMath.BreathPhase phase = currentBreathPhase();
        if (!phase.phase.equals(lastSpokenBreathPhase)) {
            lastSpokenBreathPhase = phase.phase;
            PromptPlayer.playPhase(this, phase.phase, currentLanguage());
        }
    }

    private void playStartPrompt(String kind) {
        if (voicePromptsEnabled()) PromptPlayer.playStart(this, kind, currentLanguage());
    }

    private View breathGuidePanel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(10), 0, dp(10));
        box.addView(sectionTitle("Три точки для дыхания"));
        box.addView(paragraph("Каждый пункт рассчитан на 10 минут. Используйте только если положение комфортно; при боли или онемении прекратите практику."));
        addBreathGuidePoint(box,
            "Валик под поясницей — сакрум",
            "Это про выдох. Глубокий, животный. Когда крестец мягко опирается на плотную лузгу, поясница получает сигнал: опасности нет, можно расслабить спазм. Почувствуйте, как бедра стекают на пол, а живот становится мягким.");
        addBreathGuidePoint(box,
            "Валик под грудной клеткой — вдоль позвоночника",
            "Мягкий шелест гречки под спиной, руки раскинуты в стороны. Здесь раскрывается грудная клетка. Это мягкая тракция для зажатых межлопаточных мышц. Полежите так 10 минут и заметите, как легче дышится, словно сняли бронежилет.");
        addBreathGuidePoint(box,
            "Валик под шеей",
            "Мешочек под шейный лордоз помогает замедлиться. Затылок удлиняется, подбородок чуть уходит вниз, поток мыслей постепенно становится спокойнее.");
        return box;
    }

    private void addBreathGuidePoint(LinearLayout parent, String title, String text) {
        TextView titleView = sectionTitle(title);
        titleView.setTextSize(18);
        parent.addView(titleView);
        parent.addView(paragraph(text));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_HORIZONTAL);
        Button tenMinutes = button("Поставить 10 минут");
        tenMinutes.setContentDescription("Поставить таймер практики дыхания на 10 минут для пункта: " + title);
        tenMinutes.setOnClickListener(v -> breathMinutesInput.setText("10"));
        Button speak = button("Озвучить");
        speak.setContentDescription("Озвучить описание пункта: " + title);
        speak.setOnClickListener(v -> speakGuide(title + ". " + text));
        actions.addView(tenMinutes);
        actions.addView(speak);
        parent.addView(actions);
    }

    private void speakGuide(String text) {
        if (guideTtsReady && guideTts != null) {
            guideTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "breath-guide");
        }
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

    private boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        return powerManager != null && powerManager.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void openBatterySettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void startTimerKeeper() {
        Intent intent = new Intent(this, TimerForegroundService.class).setAction(TimerForegroundService.ACTION_REFRESH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
    }

    private void updateTimerKeeper() {
        if (meatState.running || breathState.running) {
            startTimerKeeper();
            return;
        }
        stopService(new Intent(this, TimerForegroundService.class).setAction(TimerForegroundService.ACTION_STOP));
    }

    private TextView heading(String text) { TextView v = new TextView(this); v.setText(text); v.setTextSize(26); v.setTextColor(COLOR_TEXT); v.setGravity(Gravity.CENTER_HORIZONTAL); v.setPadding(0, 0, 0, dp(12)); return v; }
    private TextView sectionTitle(String text) { TextView v = new TextView(this); v.setText(text); v.setTextSize(22); v.setTextColor(COLOR_TEXT); v.setPadding(0, dp(24), 0, dp(8)); return v; }
    private TextView paragraph(String text) { TextView v = new TextView(this); v.setText(text); v.setTextSize(16); v.setTextColor(COLOR_HINT); v.setPadding(0, dp(4), 0, dp(8)); return v; }
    private TextView timerText() { TextView v = new TextView(this); v.setTextSize(42); v.setTextColor(COLOR_TEXT); v.setGravity(Gravity.CENTER_HORIZONTAL); v.setPadding(0, dp(12), 0, dp(12)); return v; }
    private Button button(String text) { Button b = new Button(this); b.setText(text); b.setAllCaps(false); b.setTextSize(18); b.setTextColor(COLOR_TEXT); b.setBackground(buttonBackground(false)); b.setPadding(dp(8), dp(8), dp(8), dp(8)); return b; }

    private EditText numberInput(String label, int value, int min, int max) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(Math.max(min, Math.min(max, value))));
        input.setHint(label);
        input.setTextColor(COLOR_TEXT);
        input.setHintTextColor(COLOR_HINT);
        input.setBackground(buttonBackground(false));
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

    private GradientDrawable panelBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(COLOR_PANEL);
        drawable.setStroke(dp(1), Color.rgb(64, 64, 72));
        drawable.setCornerRadius(dp(16));
        return drawable;
    }

    private GradientDrawable buttonBackground(boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(selected ? COLOR_ACCENT : Color.rgb(45, 45, 52));
        drawable.setStroke(dp(1), selected ? Color.rgb(255, 150, 60) : Color.rgb(96, 96, 104));
        drawable.setCornerRadius(dp(10));
        return drawable;
    }

    private interface PresetHandler { void apply(int value); }

    private View collapsiblePresetPanel(String title, int[] values, PresetHandler handler) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        Button summary = button(title);
        summary.setContentDescription(title + ". Нажмите, чтобы раскрыть или скрыть быстрые действия.");
        final LinearLayout[] contentRef = new LinearLayout[1];
        LinearLayout content = presetRow(values, value -> {
            handler.apply(value);
            contentVisibility(contentRef[0], summary, false);
        });
        contentRef[0] = content;
        contentVisibility(content, summary, false);
        summary.setOnClickListener(v -> contentVisibility(content, summary, content.getVisibility() != View.VISIBLE));
        box.addView(summary);
        box.addView(content);
        return box;
    }

    private void contentVisibility(View content, Button summary, boolean visible) {
        content.setVisibility(visible ? View.VISIBLE : View.GONE);
        summary.setText((visible ? "Скрыть: " : "Показать: ") + summary.getText().toString().replace("Показать: ", "").replace("Скрыть: ", ""));
    }

    private LinearLayout presetRow(int[] values, PresetHandler handler) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(8));
        LinearLayout line = null;
        for (int i = 0; i < values.length; i += 1) {
            if (i % 4 == 0) {
                line = new LinearLayout(this);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setGravity(Gravity.CENTER_HORIZONTAL);
                row.addView(line);
            }
            int value = values[i];
            Button b = button(String.valueOf(value));
            b.setTextSize(15);
            b.setContentDescription("Быстрый выбор: " + value);
            b.setOnClickListener(v -> handler.apply(value));
            line.addView(b);
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
