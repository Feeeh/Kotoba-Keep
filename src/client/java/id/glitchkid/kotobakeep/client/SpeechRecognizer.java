package id.glitchkid.kotobakeep.client;

import com.google.gson.JsonParser;
import id.glitchkid.kotobakeep.KotobaKeep;
import id.glitchkid.kotobakeep.language.Japanese;
import net.fabricmc.loader.api.FabricLoader;
import id.glitchkid.kotobakeep.client.VoskNative.Model;
import id.glitchkid.kotobakeep.client.VoskNative.Recognizer;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.*;

/** All native recognition work happens on one bounded worker, never on a game/audio thread. */
public final class SpeechRecognizer implements AutoCloseable {
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), runnable -> {
        Thread thread = new Thread(runnable, "KotobaKeep-Speech"); thread.setDaemon(true); return thread;
    });
    private Model model;
    private Recognizer recognizer;
    private SpeechGrammar grammar;
    private volatile UUID challenge;
    private volatile boolean ready;
    private volatile String status = "Loading Japanese speech model...";
    private Consumer<String> transcript = text -> {};
    private String lastText = "";
    public SpeechRecognizer() {
        worker.execute(() -> {
            try {
                Path path = Path.of(KotobaKeep.CONFIG.modelPath);
                if (!path.isAbsolute()) path = FabricLoader.getInstance().getGameDir().resolve(path);
                if (!Files.isRegularFile(path.resolve("am/final.mdl"))) {
                    status = "Japanese model missing — see README"; return;
                }
                model = new Model(path.toString());
                grammar = new SpeechGrammar(path, KotobaKeep.JAPANESE);
                ready = true; status = "Ready";
                KotobaKeep.LOG.info("Offline Japanese speech model loaded from {}", path);
            } catch (Throwable error) { fail(error); }
        });
    }
    public boolean ready() { return ready; }
    public String status() { return status; }
    public void start(UUID id, Japanese.Word word, Consumer<String> onTranscript, Runnable onStarted) {
        challenge = id;
        submit(() -> {
            if (!id.equals(challenge) || !ready) return;
            try {
                if (recognizer != null) recognizer.close();
                recognizer = new Recognizer(model, 48000.0f, grammar.json(word));
                recognizer.setWords(true);
                transcript = onTranscript; lastText = "";
                onStarted.run();
            } catch (Throwable error) { fail(error); }
        });
    }
    public void audio(short[] frame) {
        UUID id = challenge;
        if (!ready || id == null || frame.length == 0 || worker.getQueue().size() > 80) return;
        short[] copy = frame.clone();
        submit(() -> {
            if (!id.equals(challenge) || recognizer == null) return;
            try {
                boolean complete = recognizer.acceptWaveForm(copy, copy.length);
                emit(complete ? recognizer.getResult() : recognizer.getPartialResult(), complete ? "text" : "partial");
            } catch (Throwable error) { fail(error); }
        });
    }
    private void emit(String json, String key) {
        var result = JsonParser.parseString(json).getAsJsonObject();
        if (!result.has(key)) return;
        String text = Japanese.cleanTranscript(result.get(key).getAsString());
        if (!text.isEmpty() && !text.equals(lastText)) { lastText = text; transcript.accept(text); }
    }
    public void flush(UUID id) {
        submit(() -> {
            if (!id.equals(challenge) || recognizer == null) return;
            try { emit(recognizer.getFinalResult(), "text"); }
            catch (Throwable error) { fail(error); }
            finally { if (id.equals(challenge)) challenge = null; }
        });
    }
    public void cancel() { challenge = null; }
    private void submit(Runnable runnable) {
        try { worker.execute(runnable); }
        catch (RejectedExecutionException e) { if (!worker.isShutdown()) { ready = false; status = "Speech worker overloaded; challenges paused"; } }
    }
    private void fail(Throwable error) { ready = false; challenge = null; status = "Speech unavailable — check latest.log"; KotobaKeep.LOG.error(status, error); }
    public void close() {
        challenge = null; ready = false;
        submit(() -> { if (recognizer != null) recognizer.close(); if (model != null) model.close(); });
        worker.shutdown();
    }
}
