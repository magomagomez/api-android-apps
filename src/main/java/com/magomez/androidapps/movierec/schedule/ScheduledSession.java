package com.magomez.androidapps.movierec.schedule;

import java.time.LocalTime;

/**
 * One screening of a requested film on a given {@link ScheduleDay}.
 *
 * @param title      the requested title (as matched, not necessarily the festival's exact wording)
 * @param startTime  screening start time
 * @param endTime    screening end time, or {@code null} when the programme doesn't say
 * @param location   venue/room name, or {@code null} when the programme doesn't say
 * @param convenient whether this slot is realistically attendable — see {@link SchedulePriority}
 */
public record ScheduledSession(String title, LocalTime startTime, LocalTime endTime, String location, boolean convenient) {
}
