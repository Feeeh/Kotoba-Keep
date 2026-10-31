package id.glitchkid.kotobakeep.server;

/** Server-owned attempt deadlines. A timeout offers a retry before any inventory penalty. */
public final class ChallengeTimer {
    public enum Transition { NONE, OFFER_RETRY, FAIL }
    private enum Phase { PREPARING, RUNNING, RETRY_OFFER, EXPIRED }
    private final int duration, grace, retryWindow, maximumRetries;
    private Phase phase = Phase.PREPARING;
    private long deadline, retryDeadline;
    private int retriesUsed;

    public ChallengeTimer(int duration, int grace, int retryWindow, int maximumRetries) {
        this.duration = duration; this.grace = grace;
        this.retryWindow = retryWindow; this.maximumRetries = maximumRetries;
    }
    public boolean waitingForStart() { return phase == Phase.PREPARING; }
    public int retriesLeft() { return maximumRetries - retriesUsed; }
    public void start(long now) {
        if (phase != Phase.PREPARING) return;
        phase = Phase.RUNNING; deadline = now + duration + grace;
    }
    public boolean acceptsAnswer(long now) {
        return (phase == Phase.RUNNING && now < deadline)
            || (phase == Phase.RETRY_OFFER && now < retryDeadline);
    }
    public boolean retry(long now) {
        if (retriesLeft() == 0 || !(acceptsAnswer(now) || (phase == Phase.RUNNING && now == deadline))) return false;
        retriesUsed++; phase = Phase.PREPARING;
        return true;
    }
    /** Pays for a replay. The client shows the deadline without the grace, so that is the time the cost must fit in. */
    public boolean spend(long now, int cost) {
        if (phase != Phase.RUNNING || cost <= 0 || deadline - grace - now <= cost) return false;
        deadline -= cost;
        return true;
    }
    public Transition advance(long now) {
        if (phase == Phase.RUNNING && now >= deadline) {
            if (retriesLeft() > 0) {
                phase = Phase.RETRY_OFFER; retryDeadline = now + retryWindow;
                return Transition.OFFER_RETRY;
            }
            phase = Phase.EXPIRED; return Transition.FAIL;
        }
        if (phase == Phase.RETRY_OFFER && now >= retryDeadline) {
            phase = Phase.EXPIRED; return Transition.FAIL;
        }
        return Transition.NONE;
    }
}
