package id.glitchkid.kotobakeep.server;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Only correct answers earn a cooldown. An absent entry means the word still needs practice. */
public final class LearningProgress {
    private Map<String, Map<String, Long>> learned = new HashMap<>();

    public boolean isDue(UUID player, String item, long now) {
        Map<String, Long> items = learned.get(player.toString());
        return LearningRules.due(items == null ? null : items.get(item), now);
    }

    public void recordSuccess(UUID player, String item, long nextDue) {
        learned.computeIfAbsent(player.toString(), key -> new HashMap<>()).put(item, nextDue);
    }

    public boolean recordFailure(UUID player, String item) {
        Map<String, Long> items = learned.get(player.toString());
        return items != null && items.remove(item) != null;
    }

    public String toJson() { return new GsonBuilder().setPrettyPrinting().create().toJson(learned); }

    public static LearningProgress fromJson(String json) {
        var progress = new LearningProgress();
        Map<String, Map<String, Long>> restored = new GsonBuilder().create().fromJson(json,
            new TypeToken<Map<String, Map<String, Long>>>(){}.getType());
        if (restored != null) progress.learned = restored;
        return progress;
    }
}
