package id.glitchkid.kotobakeep;

import id.glitchkid.kotobakeep.server.ChallengeTimer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static id.glitchkid.kotobakeep.server.ChallengeTimer.Transition.*;

class ChallengeTimerTest {
    @Test void firstTimeoutOffersEightSecondsBeforeAnInventoryPenalty() {
        var timer=new ChallengeTimer(120,40,160,1);
        timer.start(100);
        assertEquals(NONE,timer.advance(259));
        assertEquals(OFFER_RETRY,timer.advance(260));
        assertTrue(timer.acceptsAnswer(261));
        assertEquals(NONE,timer.advance(419));
        assertEquals(FAIL,timer.advance(420));
        assertFalse(timer.retry(420));
    }
    @Test void retryRestartsAFullAttemptAndDoesNotAllowDuplicateRequests() {
        var timer=new ChallengeTimer(120,40,160,1);
        timer.start(0);assertEquals(OFFER_RETRY,timer.advance(160));
        assertTrue(timer.retry(200));assertEquals(0,timer.retriesLeft());
        assertTrue(timer.waitingForStart());assertFalse(timer.retry(201));
        assertFalse(timer.acceptsAnswer(201));
        timer.start(220);timer.start(300);
        assertTrue(timer.acceptsAnswer(379));
        assertEquals(FAIL,timer.advance(380));
    }
    @Test void playerCanRetryDuringTheFirstAttemptAndSuccessRemainsPossible() {
        var timer=new ChallengeTimer(120,40,160,1);timer.start(100);
        assertTrue(timer.retry(130));timer.start(150);
        assertTrue(timer.acceptsAnswer(300));assertFalse(timer.retry(300));
        assertEquals(FAIL,timer.advance(310));
    }
    @Test void loadingTimeDoesNotConsumeAnAttemptOrTheRetryWindow() {
        var timer=new ChallengeTimer(120,40,160,1);
        assertEquals(NONE,timer.advance(1000));assertFalse(timer.retry(1000));
        timer.start(1000);assertEquals(OFFER_RETRY,timer.advance(1160));
    }
    @Test void retriesCanBeDisabledInServerConfiguration() {
        var timer=new ChallengeTimer(120,40,160,0);timer.start(0);
        assertFalse(timer.retry(10));assertEquals(FAIL,timer.advance(160));
    }
}
