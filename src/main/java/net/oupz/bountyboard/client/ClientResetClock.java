package net.oupz.bountyboard.client;

public final class ClientResetClock {
    private static volatile long nextEpochSeconds = 0L;
    private ClientResetClock() {}

    public static void setNextEpoch(long epochSeconds) { nextEpochSeconds = Math.max(0L, epochSeconds); }
    public static long getNextEpoch() { return nextEpochSeconds; }

    public static long secondsRemaining() {
        if (nextEpochSeconds <= 0L) return 0L;
        long now = System.currentTimeMillis() / 1000L;
        long rem = nextEpochSeconds - now;
        return rem < 0 ? 0 : rem;
    }
}
