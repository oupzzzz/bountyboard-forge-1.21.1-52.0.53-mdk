package net.oupz.bountyboard.client;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-side cache of today's bounty status:
 * - how many completed
 * - live countdown (seconds) until daily reset (server authoritative)
 * - which bounty IDs were completed today
 *
 * Updated by DailyStatusS2C; read by the BountyBoardScreen.
 */
public final class ClientDailyStatus {
    private ClientDailyStatus() {}

    private static final Object LOCK = new Object();

    private static volatile int  completed       = 0;
    private static volatile long secondsToReset  = 0L;
    private static volatile long lastSnapshotMs  = System.currentTimeMillis();

    /** Immutable set reference; replaced atomically on update. */
    private static volatile Set<String> completedIds = Set.of();

    /** Legacy path: update count + seconds only (keeps prior completedIds). */
    public static void update(int c, long secs) {
        synchronized (LOCK) {
            completed      = Math.max(0, c);
            secondsToReset = Math.max(0L, secs);
            lastSnapshotMs = System.currentTimeMillis();
            // completedIds unchanged on purpose
        }
    }

    /** Main path: update count + seconds + completed ID set. */
    public static void update(int c, long secs, Set<String> ids) {
        synchronized (LOCK) {
            completed      = Math.max(0, c);
            secondsToReset = Math.max(0L, secs);
            lastSnapshotMs = System.currentTimeMillis();
            completedIds   = (ids == null || ids.isEmpty()) ? Set.of() : Set.copyOf(ids); // immutable
        }
    }

    /** Number completed today (snapshot). */
    public static int completedToday() {
        return completed;
    }

    /**
     * Live, client-side countdown in seconds.
     * Decreases based on elapsed wall time since last server snapshot.
     */
    public static long remainingSeconds() {
        long elapsed = (System.currentTimeMillis() - lastSnapshotMs) / 1000L;
        long rem     = secondsToReset - Math.max(0L, elapsed);
        return Math.max(0L, rem);
    }

    /**
     * True if this bounty was completed today per server snapshot.
     * Accepts fully-qualified ("modid:path") or bare path ("path").
     */
    public static boolean isCompletedToday(String id) {
        if (id == null || id.isEmpty()) return false;

        // Fast path: exact match
        Set<String> ids = completedIds; // local read of volatile
        if (ids.contains(id)) return true;

        // If caller passed a bare path, match any "ns:id" whose path equals this.
        if (id.indexOf(':') < 0) {
            for (String full : ids) {
                int idx = full.indexOf(':');
                if (idx > 0 && idx + 1 < full.length()) {
                    String path = full.substring(idx + 1);
                    if (path.equals(id)) return true;
                }
            }
        }
        return false;
    }

    /** Defensive copy of the completed ID set (optional, for debugging/overlay). */
    public static Set<String> snapshotCompletedIds() {
        synchronized (LOCK) {
            return new HashSet<>(completedIds);
        }
    }
}
