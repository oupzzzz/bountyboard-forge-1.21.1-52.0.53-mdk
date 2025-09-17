package net.oupz.bountyboard.client;

public final class ClientRewards {
    private ClientRewards() {}

    // Keep these volatile since they’re read/written from the render thread & netty thread
    private static volatile int pending = 0;
    private static volatile int lastPending = 0;

    /** Current number of unclaimed rewards. */
    public static int pendingCount() {
        return pending;
    }

    /** The previous pending value before the most recent update. */
    public static int lastPendingCount() {
        return lastPending;
    }

    /**
     * Primary updater to use from S2C handlers.
     * Also shifts the "previous" value so UIs can detect decreases and play a sound.
     */
    public static void updateCount(int newCount) {
        lastPending = pending;
        pending = Math.max(0, newCount);
    }

    /**
     * Legacy setter kept for backwards compatibility.
     * Prefer {@link #updateCount(int)} so lastPending is tracked correctly.
     */
    public static void setPending(int n) {
        updateCount(n);
    }
}
