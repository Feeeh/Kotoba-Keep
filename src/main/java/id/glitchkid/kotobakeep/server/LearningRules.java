package id.glitchkid.kotobakeep.server;

import java.util.random.RandomGenerator;

public final class LearningRules {
    private LearningRules() {}
    public static long nextDue(long now, int minSeconds, int maxSeconds, RandomGenerator random) {
        return now + random.nextLong(minSeconds * 20L, maxSeconds * 20L + 1);
    }
    public static int penalty(int current, int protectedCount, int acquired) {
        return Math.max(0, Math.min(acquired, current - protectedCount));
    }
    public static boolean due(Long nextDue, long now) { return nextDue == null || now >= nextDue; }
}
