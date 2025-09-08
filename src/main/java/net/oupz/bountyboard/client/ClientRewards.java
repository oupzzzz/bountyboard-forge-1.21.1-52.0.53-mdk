package net.oupz.bountyboard.client;

public final class ClientRewards {
    private ClientRewards() {}
    private static volatile int PENDING = 0;

    public static int pendingCount() { return PENDING; }
    public static void setPending(int n) { PENDING = Math.max(0, n); }
}
