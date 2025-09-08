package net.oupz.bountyboard.bounty.limits;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

public final class DailyLimit {
    private DailyLimit() {}

    // Tweak if you want a different cap
    public static final int MAX_PER_DAY = 5;

    // Persistent NBT keys
    private static final String ROOT = "bountyboard_daily";
    private static final String KEY_DAY = "epochDayUtc";
    private static final String KEY_COUNT = "count";
    private static final String KEY_SET = "ids"; // string list (comma-joined to keep it simple)

    private static final ZoneId EST = ZoneId.of("America/New_York"); // Handles EST/EDT automatically
    private static final Clock EST_CLOCK = Clock.system(EST);

    /** Ensures the player’s daily bucket is for “today” (UTC). If day changed, reset. */
    private static CompoundTag ensureFresh(ServerPlayer sp) {
        CompoundTag root = sp.getPersistentData();
        if (!root.contains(ROOT)) {
            CompoundTag bb = new CompoundTag();
            root.put(ROOT, bb);
        }
        CompoundTag bb = root.getCompound(ROOT);

        long today = LocalDate.now(EST_CLOCK).toEpochDay();
        long storedDay = bb.getLong(KEY_DAY);

        if (storedDay != today) {
            // New UTC day → reset count and set
            bb.putLong(KEY_DAY, today);
            bb.putInt(KEY_COUNT, 0);
            bb.putString(KEY_SET, "");
        }
        return bb;
    }

    public static boolean hasReachedLimit(ServerPlayer sp) {
        CompoundTag bb = ensureFresh(sp);
        return bb.getInt(KEY_COUNT) >= MAX_PER_DAY;
    }

    public static void consumeOne(ServerPlayer sp) {
        CompoundTag bb = ensureFresh(sp);
        int used = bb.getInt(KEY_COUNT);
        bb.putInt(KEY_COUNT, Math.min(MAX_PER_DAY, used + 1));
    }

    public static int remaining(ServerPlayer sp) {
        CompoundTag bb = ensureFresh(sp);
        int used = bb.getInt(KEY_COUNT);
        int rem = MAX_PER_DAY - used;
        return Math.max(rem, 0);
    }

    public static boolean isCompletedToday(ServerPlayer sp, ResourceLocation id) {
        CompoundTag bb = ensureFresh(sp);
        Set<String> ids = deserializeIdSet(bb.getString(KEY_SET));
        return ids.contains(id.toString());
    }

    /** Mark a bounty as completed (idempotent). Returns true if it actually incremented today’s count. */
    public static boolean markCompleted(ServerPlayer sp, ResourceLocation id) {
        CompoundTag bb = ensureFresh(sp);
        Set<String> ids = deserializeIdSet(bb.getString(KEY_SET));
        if (ids.contains(id.toString())) {
            return false; // already counted today
        }
        ids.add(id.toString());
        bb.putString(KEY_SET, serializeIdSet(ids));

        int used = bb.getInt(KEY_COUNT);
        bb.putInt(KEY_COUNT, Math.min(MAX_PER_DAY, used + 1));
        return true;
    }

    /** Seconds (UTC) until the next midnight. */
    public static long secondsUntilResetUtc() {
        Instant now = Instant.now(EST_CLOCK);
        Instant nextMidnight = LocalDate.now(EST_CLOCK)
                .plusDays(1)
                .atStartOfDay(EST)
                .toInstant();
        return Math.max(0L, ChronoUnit.SECONDS.between(now, nextMidnight));
    }

    // -------- simple string serialization for today’s completed IDs --------
    private static Set<String> deserializeIdSet(String csv) {
        Set<String> out = new HashSet<>();
        if (csv == null || csv.isEmpty()) return out;
        String[] parts = csv.split(",");
        for (String s : parts) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static String serializeIdSet(Set<String> set) {
        if (set.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : set) {
            if (!first) sb.append(',');
            sb.append(s);
            first = false;
        }
        return sb.toString();
    }
}
