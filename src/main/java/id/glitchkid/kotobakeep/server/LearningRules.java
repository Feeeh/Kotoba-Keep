package id.glitchkid.kotobakeep.server;

import java.util.random.RandomGenerator;

public final class LearningRules {
    private static final long TICKS_PER_MINUTE = 1200;
    private LearningRules() {}
    public static long nextDue(long now, int minSeconds, int maxSeconds, RandomGenerator random) {
        return now + random.nextLong(minSeconds * 20L, maxSeconds * 20L + 1);
    }
    /** The nth correct answer in a row waits the nth gap; past the last gap, the last one repeats. */
    public static long growingDue(long now, int streak, int[] gapMinutes) {
        return now + gapMinutes[Math.clamp(streak - 1, 0, gapMinutes.length - 1)] * TICKS_PER_MINUTE;
    }
    public static int penalty(int current, int protectedCount, int acquired) {
        return Math.max(0, Math.min(acquired, current - protectedCount));
    }
    public static boolean due(Long nextDue, long now) { return nextDue == null || now >= nextDue; }
}
