package com.magomez.androidapps.movierec.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Deterministic, explainable rule for which screenings are actually realistic to attend
 * on a regular Monday-to-Friday work schedule:
 * <ul>
 *   <li>Monday-Friday: only an evening screening ({@value #WEEKDAY_EVENING_FROM_HOUR}:00 or
 *       later) is realistic.</li>
 *   <li>Saturday, Sunday, and the 12th of the month (this festival always runs across
 *       Spain's 12 October public holiday — a weekday that's free all day, every edition):
 *       any time works.</li>
 * </ul>
 *
 * <p>No LLM, no configurable weights here on purpose — this is a fixed personal-schedule
 * fact, not a taste signal.
 */
public final class SchedulePriority {

    static final int WEEKDAY_EVENING_FROM_HOUR = 18;
    private static final LocalTime WEEKDAY_EVENING_FROM = LocalTime.of(WEEKDAY_EVENING_FROM_HOUR, 0);

    private SchedulePriority() {
    }

    /** @return {@code true} when a screening at {@code date}/{@code time} is realistically attendable. */
    public static boolean isConvenient(LocalDate date, LocalTime time) {
        if (date.getDayOfMonth() == 12) {
            return true;
        }
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return true;
        }
        return time != null && !time.isBefore(WEEKDAY_EVENING_FROM);
    }
}
