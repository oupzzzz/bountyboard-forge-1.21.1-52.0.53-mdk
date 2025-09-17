package net.oupz.bountyboard.bounty.renown;

import net.minecraft.resources.ResourceLocation;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

/**
 * Holds base renown values for each bounty and tier multipliers.
 * Adjust these numbers however you like later.
 */
public final class RenownHelper {
    private RenownHelper() {}

    /** Server & client: multiplier stays as-is. */
    public static float getMultiplierForTier(int tier) {
        return switch (tier) {
            case 0 -> 1.0f;    // Tier I
            case 1 -> 1.5f;    // Tier II
            case 2 -> 2.25f;   // Tier III
            default -> 1.0f;
        };
    }

    /** New: base = deterministic 100..200 from (UTC date, player UUID). */
    public static int getBaseRenown(ResourceLocation id, UUID playerId) {
        // Use Eastern time zone (handles EST/EDT automatically).
        LocalDate estDate = LocalDate.now(ZoneId.of("America/New_York"));

        long seed = RenownSeedSource.mix(
                RenownSeedSource.currentGlobalSeed(),
                encodeDate(estDate)
        );

        // bind to player
        seed = RenownSeedSource.mix(seed, playerId.getMostSignificantBits());
        seed = RenownSeedSource.mix(seed, playerId.getLeastSignificantBits());

        // bind to bounty id
        long idHash = id != null ? id.toString().hashCode() : 0;
        seed = RenownSeedSource.mix(seed, idHash);

        long r = splitMix64(seed);
        return (int) java.lang.Long.remainderUnsigned(r, 101) + 100; // 100–200
    }

    private static long encodeDate(LocalDate d) {
        return (long) d.getYear() * 10_000L + (long) d.getMonthValue() * 100L + d.getDayOfMonth();
    }

    private static long splitMix64(long x) {
        long z = x + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
