package net.oupz.bountyboard.time;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public final class BiweeklyReset {
    private BiweeklyReset() {}

    private static final ZoneId EASTERN = ZoneId.of("America/New_York");
    // Anchor controls which Mondays are “reset Mondays” (every +2 weeks from here).
    private static final LocalDate ANCHOR_MONDAY = LocalDate.of(2025, 1, 6);

    public static ZonedDateTime nextReset() { return nextResetFrom(ZonedDateTime.now(EASTERN)); }

    public static ZonedDateTime nextResetFrom(ZonedDateTime nowAnyZone) {
        ZonedDateTime now = nowAnyZone.withZoneSameInstant(EASTERN);
        ZonedDateTime mon00 = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .truncatedTo(ChronoUnit.DAYS);
        if (now.isAfter(mon00)) mon00 = mon00.plusWeeks(1);
        if (!isResetMonday(mon00.toLocalDate())) mon00 = mon00.plusWeeks(1);
        return mon00;
    }

    public static boolean isResetMonday(LocalDate date) {
        if (date.getDayOfWeek() != DayOfWeek.MONDAY) return false;
        long weeks = ChronoUnit.WEEKS.between(ANCHOR_MONDAY, date);
        return weeks % 2 == 0;
    }

    public static long nextResetEpochSeconds() { return nextReset().toInstant().getEpochSecond(); }
    public static long secondsUntilNextReset() {
        long now = Instant.now().getEpochSecond();
        return Math.max(0L, nextResetEpochSeconds() - now);
    }
}
