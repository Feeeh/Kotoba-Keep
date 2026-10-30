package id.glitchkid.kotobakeep;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import id.glitchkid.kotobakeep.language.Japanese;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** `gradlew voiceLines` dumps the mod's own readings for tools/generate_voices.py; the test checks the bundled clips. */
class VoiceLines {
    private static final Path ASSETS = Path.of("src/client/resources/assets/kotobakeep");

    /** Item, block and mob names: everything a challenge can ask for. */
    static boolean isChallengeName(String key, String text) {
        return key.matches("(?:item|block|entity)\\.minecraft\\.[^.]+") && !text.contains("%");
    }

    public static void main(String[] args) throws Exception {
        var japanese = new Japanese();
        var lines = new TreeMap<String, Japanese.Word>();
        japanese.translations().forEach((key, text) -> { if (isChallengeName(key, text)) lines.put(key, japanese.word(text)); });
        Files.writeString(Path.of(args[0]), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(lines), StandardCharsets.UTF_8);
    }

    @Test void everyItemAndMobNameHasABundledVoiceClip() throws Exception {
        var sounds = JsonParser.parseString(Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject();
        var missing = new ArrayList<String>();
        for (Map.Entry<String, String> entry : new Japanese().translations().entrySet()) {
            if (!isChallengeName(entry.getKey(), entry.getValue())) continue;
            var event = sounds.getAsJsonObject("say." + entry.getKey());
            String clip = event == null ? "" : event.getAsJsonArray("sounds").get(0).getAsString().replace("kotobakeep:", "");
            if (!Files.isRegularFile(ASSETS.resolve("sounds/" + clip + ".ogg"))) missing.add(entry.getKey());
        }
        assertTrue(missing.isEmpty(), "Run tools/generate_voices.py; missing clips: " + missing);
    }
}
