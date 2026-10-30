package id.glitchkid.kotobakeep;

import id.glitchkid.kotobakeep.server.LearningRules;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class LearningRulesTest {
    @Test void familiarItemsWaitBetweenTwoAndFiveMinutes() {
        var random = new Random(91);
        long min=Long.MAX_VALUE,max=0;
        for(int i=0;i<10000;i++) {
            long next=LearningRules.nextDue(12345,120,300,random);
            assertTrue(next>=14745 && next<=18345);
            assertFalse(LearningRules.due(next,next-1)); assertTrue(LearningRules.due(next,next));
            min=Math.min(min,next);max=Math.max(max,next);
        }
        assertTrue(max-min>3500);
        assertTrue(LearningRules.due(null,0));
    }
    @Test void penaltiesProtectTheExistingStock() {
        assertEquals(3,LearningRules.penalty(67,64,3));
        assertEquals(1,LearningRules.penalty(65,64,3));
        assertEquals(0,LearningRules.penalty(63,64,3));
        assertEquals(3,LearningRules.penalty(100,64,3));
        assertEquals(0,LearningRules.penalty(0,0,64));
    }
    @Test void longerReadingsHaveLongerBoundedDeadlines() {
        var config=new Config();
        assertEquals(120,config.durationTicks("つち"));
        assertTrue(config.durationTicks("おーくのげんぼく")>config.durationTicks("つち"));
        assertEquals(360,config.durationTicks("あ".repeat(200)));
    }
}
