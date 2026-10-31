package id.glitchkid.kotobakeep.client;

import id.glitchkid.kotobakeep.Config;
import id.glitchkid.kotobakeep.KotobaKeep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Kotoba Keep's settings in Minecraft's own options style. Edits go to a copy that is checked, saved and swapped
 * in when the screen closes. On someone else's server, that server's config sets the learning and challenge
 * rules and isn't sent to players, so those sections are replaced by a note.
 */
final class SettingsScreen extends OptionsSubScreen {
    private static final String KEY = "kotobakeep.options.";
    private static final int FIELD_WIDTH = 150, FIELD_HEIGHT = 20, FIELD_MAX_LENGTH = 64, INVALID_COLOR = 0xFFFF6B6B;
    private static final IntFunction<String> SECONDS = value -> value + " s";
    private final Config draft = KotobaKeep.CONFIG.copy();
    private final boolean serverRules;

    SettingsScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.translatable(KEY + "title"));
        var mc = Minecraft.getInstance();
        serverRules = mc.level != null && !mc.hasSingleplayerServer();
    }

    protected void addOptions() {
        addDisplay();
        if (serverRules) {
            list.addHeader(Component.translatable(KEY + "setByServer"));
            return;
        }
        addLearning();
        addChallenges();
    }

    public void removed() {
        super.removed();
        draft.save();
        KotobaKeep.CONFIG = draft;
    }

    private void addDisplay() {
        list.addHeader(Component.translatable(KEY + "display"));
        list.addSmall(toggle("showRomaji", draft.showRomaji, value -> draft.showRomaji = value),
            toggle("fadeHints", draft.fadeHints, value -> draft.fadeHints = value));
        list.addSmall(slider("hideRomajiAfter", Config.HINT_STREAK, draft.hideRomajiAfter, String::valueOf, value -> draft.hideRomajiAfter = value),
            slider("hideFuriganaAfter", Config.HINT_STREAK, draft.hideFuriganaAfter, String::valueOf, value -> draft.hideFuriganaAfter = value));
        list.addSmall(slider("hudTop", Config.HUD_TOP, draft.hudTop, value -> value + " px", value -> draft.hudTop = value));
    }

    private void addLearning() {
        list.addHeader(Component.translatable(KEY + "learning"));
        list.addSmall(toggle("enabled", draft.enabled, value -> draft.enabled = value),
            toggle("gentleFirstMeeting", draft.gentleFirstMeeting, value -> draft.gentleFirstMeeting = value));
        list.addSmall(toggle("limitNewWords", draft.limitNewWords, value -> draft.limitNewWords = value),
            toggle("growingReviewGaps", draft.growingReviewGaps, value -> draft.growingReviewGaps = value));
        number("newWordGapSeconds", Config.NEW_WORD_GAP_SECONDS, draft.newWordGapSeconds, value -> draft.newWordGapSeconds = value);
        field("reviewGapMinutes", joined(draft.reviewGapMinutes), text -> gaps(text).length > 0, text -> draft.reviewGapMinutes = gaps(text));
        number("repeatMinSeconds", Config.REPEAT_SECONDS, draft.repeatMinSeconds, value -> draft.repeatMinSeconds = value);
        number("repeatMaxSeconds", Config.REPEAT_SECONDS, draft.repeatMaxSeconds, value -> draft.repeatMaxSeconds = value);
    }

    private void addChallenges() {
        list.addHeader(Component.translatable(KEY + "challenges"));
        list.addSmall(slider("minimumChallengeSeconds", Config.MINIMUM_CHALLENGE_SECONDS, draft.minimumChallengeSeconds, SECONDS, value -> draft.minimumChallengeSeconds = value),
            slider("maximumChallengeSeconds", Config.CHALLENGE_SECONDS, draft.maximumChallengeSeconds, SECONDS, value -> draft.maximumChallengeSeconds = value));
        int moraSteps = (int) Math.round(draft.secondsPerMora * Config.MORA_STEPS_PER_SECOND);
        list.addSmall(slider("secondsPerMora", Config.MORA_TWENTIETHS, moraSteps,
                value -> String.format(Locale.ROOT, "%.2f s", value / Config.MORA_STEPS_PER_SECOND),
                value -> draft.secondsPerMora = value / Config.MORA_STEPS_PER_SECOND),
            slider("recognitionGraceSeconds", Config.GRACE_SECONDS, draft.recognitionGraceSeconds, SECONDS, value -> draft.recognitionGraceSeconds = value));
        list.addSmall(slider("maxRetries", Config.RETRIES, draft.maxRetries, String::valueOf, value -> draft.maxRetries = value),
            slider("retryWindowSeconds", Config.RETRY_WINDOW_SECONDS, draft.retryWindowSeconds, SECONDS, value -> draft.retryWindowSeconds = value));
        list.addSmall(slider("replayCostSeconds", Config.REPLAY_COST_SECONDS, draft.replayCostSeconds, SECONDS, value -> draft.replayCostSeconds = value));
    }

    private OptionInstance<Boolean> toggle(String name, boolean value, OptionInstance.ValueUpdateListener<Boolean> apply) {
        return OptionInstance.createBoolean(KEY + name, tooltip(name), value, apply);
    }

    private OptionInstance<Integer> slider(String name, Config.Range range, int value, IntFunction<String> label, IntConsumer apply) {
        return new OptionInstance<>(KEY + name, tooltip(name),
            (caption, current) -> Options.genericValueLabel(caption, Component.literal(label.apply(current))),
            new OptionInstance.IntRange(range.min(), range.max()), range.clamp(value), apply::accept);
    }

    private void number(String name, Config.Range range, int value, IntConsumer apply) {
        field(name, Integer.toString(value), text -> inRange(text, range), text -> apply.accept(Integer.parseInt(text)));
    }

    /** A labelled text box. Its value is applied only while it's valid; invalid text turns red. */
    private void field(String name, String value, Predicate<String> valid, Consumer<String> apply) {
        var caption = Component.translatable(KEY + name);
        var label = new StringWidget(FIELD_WIDTH, FIELD_HEIGHT, caption, font);
        var box = new EditBox(font, FIELD_WIDTH, FIELD_HEIGHT, caption);
        box.setMaxLength(FIELD_MAX_LENGTH);
        box.setValue(value);
        box.setResponder(text -> {
            boolean ok = valid.test(text.trim());
            box.setTextColor(ok ? EditBox.DEFAULT_TEXT_COLOR : INVALID_COLOR);
            if (ok) apply.accept(text.trim());
        });
        if (hasTooltip(name)) {
            var tooltip = Tooltip.create(Component.translatable(tooltipKey(name)));
            label.setTooltip(tooltip);
            box.setTooltip(tooltip);
        }
        list.addSmall(label, box);
    }

    private static boolean inRange(String text, Config.Range range) {
        return text.matches("\\d{1,9}") && range.contains(Integer.parseInt(text));
    }

    /** "3, 15, 60" becomes {3, 15, 60}; empty unless every gap is a whole number of minutes in range. */
    private static int[] gaps(String text) {
        String[] parts = text.split("[,\\s]+");
        if (parts.length > Config.MAX_REVIEW_GAPS || !Arrays.stream(parts).allMatch(part -> inRange(part, Config.GAP_MINUTES))) return new int[0];
        return Arrays.stream(parts).mapToInt(Integer::parseInt).toArray();
    }

    private static String joined(int[] gaps) {
        return Arrays.stream(gaps).mapToObj(String::valueOf).collect(Collectors.joining(", "));
    }

    private static String tooltipKey(String name) { return KEY + name + ".tooltip"; }
    private static boolean hasTooltip(String name) { return Language.getInstance().has(tooltipKey(name)); }
    private static <T> OptionInstance.TooltipSupplier<T> tooltip(String name) {
        return hasTooltip(name) ? OptionInstance.cachedConstantTooltip(Component.translatable(tooltipKey(name))) : OptionInstance.noTooltip();
    }
}
