package id.glitchkid.kotobakeep.server;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntToLongFunction;

/** Each player's answered words. An absent word was never answered; a failed one stays with streak 0, due at once. */
public final class LearningProgress {
    /** due: game tick the word may be asked again. streak: correct answers in a row. */
    public record Entry(long due, int streak) {}
    private static final Entry FAILED = new Entry(0, 0);
    private Map<String, Map<String, Entry>> words = new HashMap<>();

    /** Null when the player never answered this word. */
    public Entry entry(UUID player, String item) {
        Map<String, Entry> items = words.get(player.toString());
        return items == null ? null : items.get(item);
    }

    public boolean isDue(UUID player, String item, long now) {
        Entry entry = entry(player, item);
        return LearningRules.due(entry == null ? null : entry.due(), now);
    }

    /** Correct answers in a row; 0 for a new or failed word. */
    public int streak(UUID player, String item) {
        Entry entry = entry(player, item);
        return entry == null ? 0 : entry.streak();
    }

    /** nextDue maps the new streak to the tick the word may be asked again. */
    public void recordSuccess(UUID player, String item, IntToLongFunction nextDue) {
        int streak = streak(player, item) + 1;
        items(player).put(item, new Entry(nextDue.applyAsLong(streak), streak));
    }

    public void recordFailure(UUID player, String item) { items(player).put(item, FAILED); }

    private Map<String, Entry> items(UUID player) { return words.computeIfAbsent(player.toString(), key -> new HashMap<>()); }

    public String toJson() { return new GsonBuilder().setPrettyPrinting().create().toJson(words); }

    public static LearningProgress fromJson(String json) {
        return of(new GsonBuilder().create().fromJson(json, new TypeToken<Map<String, Map<String, Entry>>>(){}.getType()));
    }

    /** 1.4.0 and older saved only when each learned word is due; each counts as one correct answer. */
    public static LearningProgress fromMasteryJson(String json) {
        Map<String, Map<String, Long>> old = new GsonBuilder().create().fromJson(json, new TypeToken<Map<String, Map<String, Long>>>(){}.getType());
        Map<String, Map<String, Entry>> words = new HashMap<>();
        if (old != null) old.forEach((player, items) -> {
            Map<String, Entry> converted = new HashMap<>();
            if (items != null) items.forEach((item, due) -> { if (due != null) converted.put(item, new Entry(due, 1)); });
            words.put(player, converted);
        });
        return of(words);
    }

    private static LearningProgress of(Map<String, Map<String, Entry>> words) {
        var progress = new LearningProgress();
        if (words != null) progress.words = words;
        return progress;
    }
}
