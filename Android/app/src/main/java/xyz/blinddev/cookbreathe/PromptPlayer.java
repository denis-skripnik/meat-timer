package xyz.blinddev.cookbreathe;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

public final class PromptPlayer {
    // All entry points and MediaPlayer callbacks run on the main thread.
    private static final Queue<Prompt> PENDING = new ArrayDeque<>();
    private static Prompt current;
    private static MediaPlayer activePlayer;

    private static final class Prompt {
        final Context context;
        final String kind;
        final String language;
        final Queue<String> files = new ArrayDeque<>();
        final Runnable done;
        AudioManager audioManager;
        Object focusRequest;

        Prompt(Context context, String kind, String language, String[] files, Runnable done) {
            this.context = context.getApplicationContext();
            this.kind = kind;
            this.language = isEnglish(language) ? "en" : "ru";
            for (String file : files) if (file != null && !file.isEmpty()) this.files.add(file);
            this.done = done;
        }
    }

    private PromptPlayer() {}

    public static void playStart(Context context, String kind, String language) {
        cancel(kind);
        playFiles(context, kind, language, "breath".equals(kind) ? new String[] {"breath-start.mp3"} : new String[] {"meat-start.mp3"}, null);
    }

    public static boolean playPhase(Context context, String phase, String language) {
        // Retry the current phase from the UI ticker, never queue stale phase words.
        if (current != null || !PENDING.isEmpty()) return false;
        playFiles(context, "breath", language, new String[] {"inhale".equals(phase) ? "breath-inhale.mp3" : "breath-exhale.mp3"}, null);
        return true;
    }

    public static void playPrompt(Context context, String kind, String event, int minute, String language, Runnable done) {
        String safeLang = isEnglish(language) ? "en" : "ru";
        if ("finish".equals(event)) {
            cancel(kind);
            playFiles(context, kind, safeLang, new String[] {"breath".equals(kind) ? "breath-finish.mp3" : "meat-finish.mp3"}, done);
            return;
        }
        playFiles(context, kind, safeLang, minutePromptFiles("breath".equals(kind) ? "breath" : "meat", minute, safeLang), done);
    }

    private static String[] minutePromptFiles(String kind, int minute, String language) {
        int safeMinute = Math.max(1, Math.min(120, minute));
        String action = "breath".equals(kind) ? "breath-minute-action.mp3" : "meat-minute-action.mp3";
        if (isEnglish(language)) {
            return new String[] {"number-" + safeMinute + ".mp3", minuteFormFile(safeMinute, language), elapsedFile(safeMinute, language), action};
        }
        return new String[] {elapsedFile(safeMinute, language), "number-" + safeMinute + ".mp3", minuteFormFile(safeMinute, language), action};
    }

    private static String minuteFormFile(int minutes, String language) {
        if (isEnglish(language)) return minutes == 1 ? "minute-one.mp3" : "minute-many.mp3";
        int lastTwo = minutes % 100;
        int last = minutes % 10;
        if (lastTwo >= 11 && lastTwo <= 14) return "minute-many.mp3";
        if (last == 1) return "minute-one.mp3";
        if (last >= 2 && last <= 4) return "minute-few.mp3";
        return "minute-many.mp3";
    }

    private static String elapsedFile(int minutes, String language) {
        if (isEnglish(language)) return "minute-elapsed.mp3";
        int lastTwo = minutes % 100;
        int last = minutes % 10;
        return last == 1 && lastTwo != 11 ? "minute-elapsed-one.mp3" : "minute-elapsed-many.mp3";
    }

    private static boolean isEnglish(String language) {
        return "en".equals(language);
    }

    private static void playFiles(Context context, String kind, String language, String[] files, Runnable done) {
        PENDING.add(new Prompt(context, kind, language, files, done));
        startNextPrompt();
    }

    private static void startNextPrompt() {
        if (current != null) return;
        current = PENDING.poll();
        if (current == null) return;
        current.audioManager = (AudioManager) current.context.getSystemService(Context.AUDIO_SERVICE);
        current.focusRequest = requestDuckingFocus(current.audioManager);
        playNextFile(current);
    }

    public static void cancel(String kind) {
        java.util.Iterator<Prompt> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            Prompt prompt = iterator.next();
            if (prompt.kind.equals(kind)) {
                iterator.remove();
                if (prompt.done != null) prompt.done.run();
            }
        }
        if (current != null && current.kind.equals(kind)) finishPrompt(current);
    }

    private static void finishPrompt(Prompt prompt) {
        if (current != prompt) return;
        current = null;
        if (activePlayer != null) { release(activePlayer); activePlayer = null; }
        abandonFocus(prompt.audioManager, prompt.focusRequest);
        if (prompt.done != null) prompt.done.run();
        startNextPrompt();
    }

    private static void playNextFile(Prompt prompt) {
        if (current != prompt) return;
        String file = prompt.files.poll();
        if (file == null) { finishPrompt(prompt); return; }
        MediaPlayer player = new MediaPlayer();
        activePlayer = player;
        try (AssetFileDescriptor afd = prompt.context.getAssets().openFd("audio/" + prompt.language + "/" + file)) {
            player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.setOnCompletionListener(mp -> completeFile(prompt, mp));
            player.setOnErrorListener((mp, what, extra) -> { completeFile(prompt, mp); return true; });
            player.prepare();
            player.start();
        } catch (IOException | RuntimeException error) {
            completeFile(prompt, player);
        }
    }

    private static void completeFile(Prompt prompt, MediaPlayer player) {
        if (current != prompt || activePlayer != player) return;
        release(player);
        activePlayer = null;
        playNextFile(prompt);
    }

    private static Object requestDuckingFocus(AudioManager audioManager) {
        if (audioManager == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest request = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                .setOnAudioFocusChangeListener(focusChange -> {})
                .build();
            audioManager.requestAudioFocus(request);
            return request;
        }
        audioManager.requestAudioFocus(null, AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        return Boolean.TRUE;
    }

    private static void abandonFocus(AudioManager audioManager, Object focusRequest) {
        if (audioManager == null || focusRequest == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest instanceof AudioFocusRequest) {
            audioManager.abandonAudioFocusRequest((AudioFocusRequest) focusRequest);
        } else {
            audioManager.abandonAudioFocus(null);
        }
    }

    private static void release(MediaPlayer player) {
        player.setOnCompletionListener(null);
        player.setOnErrorListener(null);
        try { player.release(); } catch (RuntimeException ignored) {}
    }
}
