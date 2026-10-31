package id.glitchkid.kotobakeep;

import id.glitchkid.kotobakeep.server.LearningProgress;
import id.glitchkid.kotobakeep.server.LearningRules;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LearningProgressTest {
    private final UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String DIRT = "minecraft:dirt";

    @Test void failedNewItemIsAskedOnEveryPickupUntilTheFirstCorrectAnswer() {
        var progress = new LearningProgress();
        for (long pickup = 100; pickup < 110; pickup++) {
            assertTrue(progress.isDue(player, DIRT, pickup));
            progress.recordFailure(player, DIRT);
            progress = LearningProgress.fromJson(progress.toJson());
        }
        long nextDue = LearningRules.nextDue(110, 120, 300, new Random(4));
        progress.recordSuccess(player, DIRT, streak -> nextDue);
        assertFalse(progress.isDue(player, DIRT, 111));
        assertTrue(progress.isDue(player, DIRT, nextDue));
    }

    @Test void aFailedReviewRequiresPracticeOnTheNextPickupAgain() {
        var progress = new LearningProgress();
        progress.recordSuccess(player, DIRT, streak -> 2500);
        assertTrue(progress.isDue(player, DIRT, 2500));
        progress.recordFailure(player, DIRT);
        assertTrue(progress.isDue(player, DIRT, 2501));
        assertTrue(LearningProgress.fromJson(progress.toJson()).isDue(player, DIRT, 2501));
    }

    @Test void learnedCooldownSurvivesReconnectAndIsSeparateForEachWordAndPlayer() {
        var progress = new LearningProgress();
        progress.recordSuccess(player, DIRT, streak -> 5000);
        var restored = LearningProgress.fromJson(progress.toJson());
        assertFalse(restored.isDue(player, DIRT, 4999));
        assertTrue(restored.isDue(player, "minecraft:stone", 4999));
        assertTrue(restored.isDue(UUID.randomUUID(), DIRT, 4999));
    }

    @Test void theStreakCountsCorrectAnswersInARowAndAFailureStartsItOver() {
        var progress = new LearningProgress();
        assertNull(progress.entry(player, DIRT));
        progress.recordSuccess(player, DIRT, streak -> streak * 100L);
        progress.recordSuccess(player, DIRT, streak -> streak * 100L);
        assertEquals(new LearningProgress.Entry(200, 2), LearningProgress.fromJson(progress.toJson()).entry(player, DIRT));
        progress.recordFailure(player, DIRT);
        assertEquals(0, progress.streak(player, DIRT));
        assertTrue(progress.isDue(player, DIRT, 1));
        // A failed word was met: it is no longer a first meeting or a new word.
        assertNotNull(LearningProgress.fromJson(progress.toJson()).entry(player, DIRT));
    }

    @Test void wordsFromTheOldMasteryFileCountAsOneCorrectAnswer() {
        var progress = LearningProgress.fromMasteryJson("{\"" + player + "\":{\"" + DIRT + "\":5000}}");
        assertEquals(new LearningProgress.Entry(5000, 1), progress.entry(player, DIRT));
        assertFalse(progress.isDue(player, DIRT, 4999));
        assertNull(LearningProgress.fromMasteryJson("{}").entry(player, DIRT));
    }

    @Test void aCancelledAttemptDoesNotTeachAnUnknownWord() {
        var progress = new LearningProgress();
        assertTrue(progress.isDue(player, DIRT, 1));
        var restored = LearningProgress.fromJson(progress.toJson());
        assertTrue(restored.isDue(player, DIRT, 2));
        assertNull(restored.entry(player, DIRT));
    }
}
