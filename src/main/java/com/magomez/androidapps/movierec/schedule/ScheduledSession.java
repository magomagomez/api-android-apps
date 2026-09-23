package com.magomez.androidapps.movierec.schedule;

import java.time.LocalTime;
import java.util.List;

/**
 * One screening of a requested film on a given {@link ScheduleDay}.
 *
 * @param title        the requested title (as matched, not necessarily the festival's exact wording)
 * @param startTime    screening start time
 * @param endTime      screening end time, or {@code null} when the programme doesn't say
 * @param location     venue/room name, or {@code null} when the programme doesn't say
 * @param convenient   whether this slot is realistically attendable — see {@link SchedulePriority}
 * @param sessionFilms every film playing in this same physical session (a double bill or
 *                     marathon lists more than one; always includes {@code title} itself)
 */
public record ScheduledSession(
        String title, LocalTime startTime, LocalTime endTime, String location, boolean convenient,
        List<String> sessionFilms) {

    public ScheduledSession {
        sessionFilms = (sessionFilms == null || sessionFilms.isEmpty()) ? List.of(title) : List.copyOf(sessionFilms);
    }
}
