package com.magomez.androidapps.movierec.schedule;

import java.time.LocalDate;
import java.util.List;

/**
 * One calendar day of the festival, with every screening of a requested film that day,
 * ordered by start time.
 */
public record ScheduleDay(LocalDate date, List<ScheduledSession> sessions) {

    public ScheduleDay {
        sessions = sessions == null ? List.of() : List.copyOf(sessions);
    }
}
