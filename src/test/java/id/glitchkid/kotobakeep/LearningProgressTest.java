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
        progress.recordSuccess(player, DIRT, nextDue);
        assertFalse(progress.isDue(player, DIRT, 111));
        assertTrue(progress.isDue(player, DIRT, nextDue));
    }

    @Test void aFailedReviewRequiresPracticeOnTheNextPickupAgain() {
        var progress = new LearningProgress();
        progress.recordSuccess(player, DIRT, 2500);
        assertTrue(progress.isDue(player, DIRT, 2500));
        assertTrue(progress.recordFailure(player, DIRT));
        assertTrue(progress.isDue(player, DIRT, 2501));
        assertTrue(LearningProgress.fromJson(progress.toJson()).isDue(player, DIRT, 2501));
    }

    @Test void learnedCooldownSurvivesReconnectAndIsSeparateForEachWordAndPlayer() {
        var progress = new LearningProgress();
        progress.recordSuccess(player, DIRT, 5000);
        var restored = LearningProgress.fromJson(progress.toJson());
        assertFalse(restored.isDue(player, DIRT, 4999));
        assertTrue(restored.isDue(player, "minecraft:stone", 4999));
        assertTrue(restored.isDue(UUID.randomUUID(), DIRT, 4999));
    }

    @Test void aCancelledAttemptDoesNotTeachAnUnknownWord() {
        var progress = new LearningProgress();
        assertTrue(progress.isDue(player, DIRT, 1));
        var restored = LearningProgress.fromJson(progress.toJson());
        assertTrue(restored.isDue(player, DIRT, 2));
    }
}
