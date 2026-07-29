package xyz.blinddev.cookbreathe;

public final class TimerMath {
    private TimerMath() {}

    public static long durationMillis(int minutes) {
        return Math.max(1, minutes) * 60_000L;
    }

    public static long remainingMillis(long deadlineElapsedMs, long nowElapsedMs) {
        return Math.max(0L, deadlineElapsedMs - nowElapsedMs);
    }

    public static int elapsedMinute(long startedElapsedMs, long nowElapsedMs) {
        return Math.max(0, (int) ((nowElapsedMs - startedElapsedMs) / 60_000L));
    }

    public static String format(long remainingMillis) {
        long totalSeconds = Math.max(0L, (remainingMillis + 999L) / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    public static BreathPhase breathPhase(long elapsedMs, int inhaleSeconds, int exhaleSeconds) {
        long inhaleMs = Math.max(1, inhaleSeconds) * 1000L;
        long exhaleMs = Math.max(1, exhaleSeconds) * 1000L;
        long cycleMs = inhaleMs + exhaleMs;
        long positionMs = cycleMs == 0L ? 0L : Math.floorMod(elapsedMs, cycleMs);
        boolean inhale = positionMs < inhaleMs;
        long phaseEnd = inhale ? inhaleMs : cycleMs;
        long remaining = Math.max(1L, (phaseEnd - positionMs + 999L) / 1000L);
        return new BreathPhase(inhale ? "inhale" : "exhale", (int) remaining);
    }

    public static final class BreathPhase {
        public final String phase;
        public final int remainingSeconds;

        BreathPhase(String phase, int remainingSeconds) {
            this.phase = phase;
            this.remainingSeconds = remainingSeconds;
        }
    }
}
