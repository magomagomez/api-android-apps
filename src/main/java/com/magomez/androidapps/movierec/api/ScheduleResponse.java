package com.magomez.androidapps.movierec.api;

import java.util.List;

/**
 * Response of {@code POST /api/schedule}: a day-by-day calendar of when the requested
 * titles actually screen, built from {@link com.magomez.androidapps.movierec.schedule.ScheduleResult}.
 *
 * @param notScheduled requested titles with no matching screening found
 * @param days         one entry per day that has at least one matched screening, in date order
 */
public record ScheduleResponse(List<String> notScheduled, List<ScheduleDayView> days) {

    public ScheduleResponse {
        notScheduled = notScheduled == null ? List.of() : List.copyOf(notScheduled);
        days = days == null ? List.of() : List.copyOf(days);
    }

    /**
     * @param date    ISO date, e.g. {@code "2026-10-12"}
     * @param weekday the day name in Spanish, e.g. {@code "Lunes"}
     * @param holiday {@code true} for the festival's fixed 12 October public holiday
     * @param sessions this day's matched screenings, ordered by start time
     */
    public record ScheduleDayView(String date, String weekday, boolean holiday, List<SessionView> sessions) {

        public ScheduleDayView {
            sessions = sessions == null ? List.of() : List.copyOf(sessions);
        }
    }

    /**
     * @param title      the requested title
     * @param startTime  {@code "HH:mm"}
     * @param endTime    {@code "HH:mm"}, or {@code null} when the programme doesn't say
     * @param location   venue/room name, or {@code null} when the programme doesn't say
     * @param convenient whether this slot is realistically attendable (see
     *                   {@link com.magomez.androidapps.movierec.schedule.SchedulePriority})
     */
    public record SessionView(String title, String startTime, String endTime, String location, boolean convenient) {
    }
}
