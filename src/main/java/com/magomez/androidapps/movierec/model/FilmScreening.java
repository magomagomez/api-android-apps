package com.magomez.androidapps.movierec.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * One scheduled screening of a film, as published in a festival's public programme.
 * Provider-internal until matched against a recommended title (by
 * {@link com.magomez.androidapps.movierec.schedule.ScheduleService}).
 *
 * @param title     the session's title exactly as the programme lists it (never blank)
 * @param date      the screening's date
 * @param startTime the screening's start time
 * @param endTime   the screening's end time, or {@code null} when the source doesn't say
 * @param location  venue/room name, or {@code null} when the source doesn't say
 */
public record FilmScreening(String title, LocalDate date, LocalTime startTime, LocalTime endTime, String location) {

    public FilmScreening {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(startTime, "startTime");
    }
}
