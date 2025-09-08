package net.oupz.bountyboard.client;

public final class ClientDailyStatus {
    private static volatile int completed = 0;
    private static volatile long baseSeconds = 0L;
    private static volatile long lastUpdateMs = System.currentTimeMillis();

    private ClientDailyStatus() {}

    public static void update(int completedToday, long secondsToReset) {
        completed = completedToday;
        baseSeconds = Math.max(0L, secondsToReset);
        lastUpdateMs = System.currentTimeMillis();
    }

    public static int completedToday() {
        return completed;
    }

    /** Returns a live countdown in whole seconds, never negative. */
    public static long remainingSeconds() {
        long elapsed = (System.currentTimeMillis() - lastUpdateMs) / 1000L;
        long remain = baseSeconds - elapsed;
        return Math.max(0L, remain);
    }
}
