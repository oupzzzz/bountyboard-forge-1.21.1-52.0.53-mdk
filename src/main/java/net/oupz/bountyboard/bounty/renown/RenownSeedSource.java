package net.oupz.bountyboard.bounty.renown;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deterministic per-user, per-day base renown generator.
 * Produces an integer in [100, 200] that is stable for a given (date, playerUUID).
 *
 * You can later change GLOBAL_SEED via setGlobalSeed(...) on the server and (optionally)
 * sync it to clients so everyone sees identical values.
 */
public final class RenownSeedSource {
    private RenownSeedSource() {}

    // Global root seed you can set from the server later (e.g., config or world seed).
    private static final AtomicLong GLOBAL_SEED = new AtomicLong(0x4BBE_9D9AL); // arbitrary default

    /** Optional: set from the server (e.g., on world load or admin command). */
    public static void setGlobalSeed(long seed) {
        GLOBAL_SEED.set(seed);
    }

    /** Returns the deterministic base renown in [100, 200] for (date, playerId). */
    public static int userDailyBaseRenownEst(LocalDate dateUtc, UUID playerId) {
        // 1) Build a stable 64-bit seed from: global seed ⊕ date ⊕ player uuid
        long seed = mix(GLOBAL_SEED.get(), encodeDate(dateUtc));
        seed = mix(seed, playerId.getMostSignificantBits());
        seed = mix(seed, playerId.getLeastSignificantBits());

        // 2) Advance seed through a tiny PRNG (SplitMix64) and map to [100, 200]
        long r = splitMix64(seed);
        // Map unsigned r to 0..100 inclusive, then +100 => 100..200 inclusive
        int span = 101; // inclusive count
        int base = (int) Long.remainderUnsigned(r, span) + 100;
        return base;
    }

    /** Convenience: today in UTC. */
    public static int userDailyBaseRenownEst(UUID playerId) {
        return userDailyBaseRenownEst(LocalDate.now(ZoneOffset.UTC), playerId);
    }

    /** Encode yyyyMMdd as a long; compact & readable. */
    private static long encodeDate(LocalDate d) {
        return (long) d.getYear() * 10_000L + (long) d.getMonthValue() * 100L + d.getDayOfMonth();
    }

    /** Simple 64-bit mixer (xorshift-like). */
    public static long mix(long a, long b) {
        long x = a ^ b;
        x ^= (x << 13);
        x ^= (x >>> 7);
        x ^= (x << 17);
        return x;
    }

    /** SplitMix64 step: good enough for deterministic, uniform mapping. */
    private static long splitMix64(long x) {
        long z = x + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public static long currentGlobalSeed() {
        return GLOBAL_SEED.get();
    }

    public static int dailyUtcSalt() {
        java.time.LocalDate utc = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        return utc.getYear() * 10_000 + utc.getMonthValue() * 100 + utc.getDayOfMonth();
    }
}
