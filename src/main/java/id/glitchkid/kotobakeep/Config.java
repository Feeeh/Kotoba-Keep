package id.glitchkid.kotobakeep;

import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    public boolean enabled = true;
    public int repeatMinSeconds = 120;
    public int repeatMaxSeconds = 300;
    public int minimumChallengeSeconds = 6;
    public int maximumChallengeSeconds = 18;
    public double secondsPerMora = 0.55;
    public int recognitionGraceSeconds = 2;
    public int maxRetries = 1;
    public int retryWindowSeconds = 8;
    public String hudAccentColor = "#c888e8";
    public int hudTop = 12;
    public boolean showRomaji = true;
    public String modelPath = "config/kotobakeep/vosk-model-small-ja-0.22";
    public static Config load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("kotobakeep.json");
        var gson = new GsonBuilder().setPrettyPrinting().create();
        Config config = new Config();
        try {
            if (Files.exists(path)) config = gson.fromJson(Files.readString(path), Config.class);
            if (config == null) config = new Config();
            config.repeatMinSeconds = Math.clamp(config.repeatMinSeconds, 1, 86400);
            config.repeatMaxSeconds = Math.clamp(config.repeatMaxSeconds, config.repeatMinSeconds, 86400);
            config.minimumChallengeSeconds = Math.clamp(config.minimumChallengeSeconds, 3, 60);
            config.maximumChallengeSeconds = Math.clamp(config.maximumChallengeSeconds, config.minimumChallengeSeconds, 120);
            config.secondsPerMora = Double.isFinite(config.secondsPerMora) ? Math.clamp(config.secondsPerMora, 0.1, 3.0) : 0.55;
            config.recognitionGraceSeconds = Math.clamp(config.recognitionGraceSeconds, 1, 5);
            config.maxRetries = Math.clamp(config.maxRetries, 0, 5);
            config.retryWindowSeconds = Math.clamp(config.retryWindowSeconds, 3, 30);
            if (config.hudAccentColor == null || !config.hudAccentColor.matches("#[0-9a-fA-F]{6}")) config.hudAccentColor = "#c888e8";
            config.hudTop = Math.clamp(config.hudTop, 0, 200);
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(config));
        } catch (Exception error) { KotobaKeep.LOG.warn("Could not load Kotoba Keep config; using safe defaults", error); }
        return config;
    }
    public int durationTicks(String reading) {
        String moraText = reading.replaceAll("[ゃゅょぁぃぅぇぉ\\s]", "");
        int mora = moraText.codePointCount(0, moraText.length());
        return (int) (20 * Math.clamp(3.0 + mora * secondsPerMora, minimumChallengeSeconds, maximumChallengeSeconds));
    }
    public int accentColor() { return 0xFF000000 | Integer.parseInt(hudAccentColor.substring(1), 16); }
}
