package net.oupz.bountyboard.client;

public final class ClientRenown {
    private static volatile int TOTAL = 0;

    private ClientRenown() {}

    public static int getTotal() {
        return TOTAL;
    }

    public static void setTotal(int value) {
        TOTAL = Math.max(0, value);
    }
}