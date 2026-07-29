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
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public final class PromptPlayer {
    private static final List<MediaPlayer> ACTIVE_PLAYERS = new ArrayList<>();

    private PromptPlayer() {}

    public static void playStart(Context context, String kind, String language) {
        playFiles(context, language, "breath".equals(kind) ? new String[] {"breath-start.mp3"} : new String[] {"meat-start.mp3"}, null);
    }

    public static void playPhase(Context context, String phase, String language) {
        playFiles(context, language, new String[] {"inhale".equals(phase) ? "breath-inhale.mp3" : "breath-exhale.mp3"}, null);
    }

    public static void playPrompt(Context context, String kind, String event, int minute, String language, Runnable done) {
        String safeLang = isEnglish(language) ? "en" : "ru";
        if ("finish".equals(event)) {
            playFiles(context, safeLang, new String[] {"breath".equals(kind) ? "breath-finish.mp3" : "meat-finish.mp3"}, done);
            return;
        }
        playFiles(context, safeLang, minutePromptFiles("breath".equals(kind) ? "breath" : "meat", minute, safeLang), done);
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

    private static void playFiles(Context context, String language, String[] files, Runnable done) {
        Context appContext = context.getApplicationContext();
        AudioManager audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
        Object focusRequest = requestDuckingFocus(audioManager);
        Queue<String> queue = new ArrayDeque<>();
        for (String file : files) if (file != null && !file.isEmpty()) queue.add(file);
        playNext(appContext, language, queue, audioManager, focusRequest, done);
    }

    private static void playNext(Context context, String language, Queue<String> queue, AudioManager audioManager, Object focusRequest, Runnable done) {
        String file = queue.poll();
        if (file == null) {
            abandonFocus(audioManager, focusRequest);
            if (done != null) done.run();
            return;
        }

        MediaPlayer player = new MediaPlayer();
        synchronized (ACTIVE_PLAYERS) { ACTIVE_PLAYERS.add(player); }
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("audio/" + language + "/" + file);
            player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.setOnCompletionListener(mp -> {
                release(mp);
                playNext(context, language, queue, audioManager, focusRequest, done);
            });
            player.setOnErrorListener((mp, what, extra) -> {
                release(mp);
                playNext(context, language, queue, audioManager, focusRequest, done);
                return true;
            });
            player.prepare();
            player.start();
        } catch (IOException | RuntimeException error) {
            release(player);
            playNext(context, language, queue, audioManager, focusRequest, done);
        }
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
        synchronized (ACTIVE_PLAYERS) { ACTIVE_PLAYERS.remove(player); }
        try { player.release(); } catch (RuntimeException ignored) {}
    }
}
