package com.magomez.androidapps.movierec.schedule;

import java.util.List;

/**
 * The festival calendar for a set of requested titles.
 *
 * @param requestedTitles the titles that were asked for, in the order given
 * @param notScheduled    requested titles with no matching screening found in the
 *                        programme (title didn't match, or it isn't screening)
 * @param days            every day that has at least one matched screening, in date order
 */
public record ScheduleResult(List<String> requestedTitles, List<String> notScheduled, List<ScheduleDay> days) {

    public ScheduleResult {
        requestedTitles = requestedTitles == null ? List.of() : List.copyOf(requestedTitles);
        notScheduled = notScheduled == null ? List.of() : List.copyOf(notScheduled);
        days = days == null ? List.of() : List.copyOf(days);
    }
}
