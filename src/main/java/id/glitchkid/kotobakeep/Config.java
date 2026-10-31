package id.glitchkid.kotobakeep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class Config {
    /** An inclusive range of allowed values, shared by the config file checks and the settings screen. */
    public record Range(int min, int max) {
        public int clamp(int value) { return Math.clamp(value, min, max); }
        public boolean contains(int value) { return value >= min && value <= max; }
    }
    public static final Range REPEAT_SECONDS = new Range(1, 86400);
    public static final Range MINIMUM_CHALLENGE_SECONDS = new Range(3, 60);
    public static final Range CHALLENGE_SECONDS = new Range(3, 120);
    /** Seconds per mora is edited in steps of 1/20 s, from 0.1 to 3.0. */
    public static final Range MORA_TWENTIETHS = new Range(2, 60);
    public static final double MORA_STEPS_PER_SECOND = 20.0;
    public static final Range GRACE_SECONDS = new Range(1, 5);
    public static final Range RETRIES = new Range(0, 5);
    public static final Range RETRY_WINDOW_SECONDS = new Range(3, 30);
    public static final Range REPLAY_COST_SECONDS = new Range(0, 10);
    public static final Range GAP_MINUTES = new Range(1, 525600);
    public static final int MAX_REVIEW_GAPS = 20;
    public static final Range HINT_STREAK = new Range(1, 20);
    public static final Range NEW_WORD_GAP_SECONDS = new Range(1, 3600);
    public static final Range HUD_TOP = new Range(0, 200);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public int repeatMinSeconds = 120;
    public int repeatMaxSeconds = 300;
    public int minimumChallengeSeconds = 6;
    public int maximumChallengeSeconds = 18;
    public double secondsPerMora = 0.55;
    public int recognitionGraceSeconds = 2;
    public int maxRetries = 1;
    public int retryWindowSeconds = 8;
    /** Seconds a replay of the spoken name takes off the timer. 0 turns replays off. */
    public int replayCostSeconds = 3;
    /** Each correct answer in a row waits the next gap in reviewGapMinutes instead of repeatMin/MaxSeconds. */
    public boolean growingReviewGaps = false;
    /** Minutes of play time before each review; the last gap repeats. A wrong answer starts over. */
    public int[] reviewGapMinutes = {3, 15, 60, 240, 1440};
    /** Hide a word's romaji, then its furigana, once it's answered right that many times in a row. */
    public boolean fadeHints = true;
    public int hideRomajiAfter = 2;
    public int hideFuriganaAfter = 4;
    /** At most one never-answered word per newWordGapSeconds; other new pickups and kills aren't asked. */
    public boolean limitNewWords = true;
    public int newWordGapSeconds = 60;
    /** Failing a word the first time it's asked takes nothing; penalties start from its second ask. */
    public boolean gentleFirstMeeting = false;
    public int hudTop = 12;
    public boolean showRomaji = true;
    public String modelPath = "config/kotobakeep/vosk-model-small-ja-0.22";

    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("kotobakeep.json"); }

    /** A file that can't be read is left alone for the player to fix, and the defaults are used. */
    public static Config load() {
        Config config;
        try {
            config = Files.exists(path()) ? GSON.fromJson(Files.readString(path()), Config.class) : null;
        } catch (Exception error) {
            KotobaKeep.LOG.warn("Could not load Kotoba Keep config; using safe defaults", error);
            return new Config();
        }
        if (config == null) config = new Config();
        config.save();
        return config;
    }

    /** Brings every value into its allowed range, then writes the file. */
    public void save() {
        validate();
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(this));
        } catch (IOException error) { KotobaKeep.LOG.warn("Could not save Kotoba Keep config", error); }
    }

    public Config copy() { return GSON.fromJson(GSON.toJson(this), Config.class); }

    private void validate() {
        repeatMinSeconds = REPEAT_SECONDS.clamp(repeatMinSeconds);
        repeatMaxSeconds = Math.clamp(repeatMaxSeconds, repeatMinSeconds, REPEAT_SECONDS.max());
        minimumChallengeSeconds = MINIMUM_CHALLENGE_SECONDS.clamp(minimumChallengeSeconds);
        maximumChallengeSeconds = Math.clamp(maximumChallengeSeconds, minimumChallengeSeconds, CHALLENGE_SECONDS.max());
        if (!Double.isFinite(secondsPerMora)) secondsPerMora = new Config().secondsPerMora;
        secondsPerMora = Math.clamp(secondsPerMora, MORA_TWENTIETHS.min() / MORA_STEPS_PER_SECOND, MORA_TWENTIETHS.max() / MORA_STEPS_PER_SECOND);
        recognitionGraceSeconds = GRACE_SECONDS.clamp(recognitionGraceSeconds);
        maxRetries = RETRIES.clamp(maxRetries);
        retryWindowSeconds = RETRY_WINDOW_SECONDS.clamp(retryWindowSeconds);
        replayCostSeconds = REPLAY_COST_SECONDS.clamp(replayCostSeconds);
        if (reviewGapMinutes == null || reviewGapMinutes.length == 0) reviewGapMinutes = new Config().reviewGapMinutes;
        reviewGapMinutes = Arrays.stream(reviewGapMinutes).limit(MAX_REVIEW_GAPS).map(GAP_MINUTES::clamp).toArray();
        hideRomajiAfter = HINT_STREAK.clamp(hideRomajiAfter);
        hideFuriganaAfter = HINT_STREAK.clamp(hideFuriganaAfter);
        newWordGapSeconds = NEW_WORD_GAP_SECONDS.clamp(newWordGapSeconds);
        hudTop = HUD_TOP.clamp(hudTop);
    }

    public int durationTicks(String reading) {
        String moraText = reading.replaceAll("[ゃゅょぁぃぅぇぉ\\s]", "");
        int mora = moraText.codePointCount(0, moraText.length());
        return (int) (20 * Math.clamp(3.0 + mora * secondsPerMora, minimumChallengeSeconds, maximumChallengeSeconds));
    }
}
