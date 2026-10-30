package id.glitchkid.kotobakeep.language;

import com.atilika.kuromoji.ipadic.Tokenizer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

public final class Japanese {
    public record Ruby(String text, String reading) {}
    public record Word(String text, String reading, List<Ruby> ruby) {
        public boolean pronounceable() { return !reading.isBlank() && !hasKanji(reading); }
    }
    private final Tokenizer tokenizer = new Tokenizer();
    private final Map<String, String> translations = new HashMap<>();
    private final Map<String, String> overrides = new TreeMap<>(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()));
    private final Map<String, Word> cache = new HashMap<>();
    public Japanese() {
        readJson("/data/kotobakeep/ja_jp.json").entrySet().forEach(e -> translations.put(e.getKey(), e.getValue().getAsString()));
        readJson("/data/kotobakeep/readings.json").entrySet().forEach(e -> overrides.put(e.getKey(), e.getValue().getAsString()));
    }
    private static JsonObject readJson(String path) {
        var stream = Japanese.class.getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException("Missing Japanese resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) { return JsonParser.parseReader(reader).getAsJsonObject(); }
        catch (Exception e) { throw new IllegalStateException(path, e); }
    }
    public String translation(String key) { return translations.get(key); }
    public Word item(String translationKey) {
        String name = translation(translationKey);
        return name == null ? null : word(name);
    }
    public synchronized Word word(String text) { return cache.computeIfAbsent(text, this::analyze); }
    private Word analyze(String text) {
        List<Ruby> ruby = new ArrayList<>();
        int at = 0;
        while (at < text.length()) {
            String match = overrideAt(text, at);
            if (match != null) {
                ruby.add(new Ruby(match, overrides.get(match))); at += match.length();
            } else {
                int end = at + Character.charCount(text.codePointAt(at));
                while (end < text.length() && overrideAt(text, end) == null) end += Character.charCount(text.codePointAt(end));
                for (var token : tokenizer.tokenize(text.substring(at, end))) {
                    String reading = token.getReading();
                    ruby.add(new Ruby(token.getSurface(), kana(reading == null || reading.equals("*") ? token.getSurface() : reading)));
                }
                at = end;
            }
        }
        return new Word(text, ruby.stream().map(Ruby::reading).reduce("", String::concat), List.copyOf(ruby));
    }
    private String overrideAt(String text, int offset) {
        for (String key : overrides.keySet()) if (text.startsWith(key, offset)) return key;
        return null;
    }
    public static boolean hasKanji(String text) { return text.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN || cp == '々'); }
    public static String kana(String value) {
        StringBuilder result = new StringBuilder();
        Normalizer.normalize(value, Normalizer.Form.NFKC).codePoints().forEach(cp -> result.appendCodePoint(cp >= 0x30A1 && cp <= 0x30F6 ? cp - 0x60 : cp));
        return result.toString();
    }
    public static String normalize(String value) {
        return kana(value).replaceAll("[\\s\\p{P}\\p{Z}・]", "").toLowerCase(Locale.ROOT);
    }
    public static String cleanTranscript(String transcript) {
        if (transcript == null) return "";
        return Normalizer.normalize(transcript, Normalizer.Form.NFKC)
            .replaceAll("(?i)\\[\\s*unk\\s*\\]", " ")
            .replaceAll("\\s+", " ").trim();
    }
    public synchronized boolean matches(String expected, String transcript) {
        if (expected == null || expected.isBlank()) return false;
        String cleaned = cleanTranscript(transcript);
        String actual = normalize(cleaned);
        if (actual.isEmpty()) return false;
        String targetKana = normalize(word(expected).reading());
        if (targetKana.isEmpty()) return false;
        // Not cached: transcripts are unbounded (and client-supplied on the server).
        String actualKana = normalize(analyze(cleaned).reading());
        // Match the complete spoken reading anywhere in the utterance, not a kanji substring:
        // 石炭 (せきたん) contains 石, but it does not say いし.
        return actual.contains(targetKana) || actualKana.contains(targetKana)
            || phonetic(actual).contains(phonetic(targetKana))
            || phonetic(actualKana).contains(phonetic(targetKana));
    }
    // Recognizers commonly spell long vowels differently (オーク / おおく).
    private static String phonetic(String text) {
        text=text.replace("ゔぁ","ば").replace("ゔぃ","び").replace("ゔぇ","べ").replace("ゔぉ","ぼ").replace("ゔ","ぶ");
        StringBuilder out = new StringBuilder();
        char lastVowel = 0;
        for (char c : text.toCharArray()) {
            if (c == 'ー') { if (lastVowel != 0) out.append(lastVowel); continue; }
            char vowel = vowel(c);
            if ((c == 'う' && lastVowel == 'お') || (c == 'い' && lastVowel == 'え')) c = lastVowel;
            out.append(c); lastVowel = vowel != 0 ? vowel : 0;
        }
        return out.toString();
    }
    private static char vowel(char c) {
        if ("あかがさざただなはばぱまゃやらわぁ".indexOf(c) >= 0) return 'あ';
        if ("いきぎしじちぢにひびぴみりぃ".indexOf(c) >= 0) return 'い';
        if ("うくぐすずつづぬふぶぷむゅゆるぅゔ".indexOf(c) >= 0) return 'う';
        if ("えけげせぜてでねへべぺめれぇ".indexOf(c) >= 0) return 'え';
        if ("おこごそぞとどのほぼぽもょよろをぉ".indexOf(c) >= 0) return 'お';
        return 0;
    }
    public Map<String, String> translations() { return Collections.unmodifiableMap(translations); }
}
