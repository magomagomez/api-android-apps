package com.magomez.androidapps.movierec.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * One scheduled screening of a film, as published in a festival's public programme.
 * Provider-internal until matched against a recommended title (by
 * {@link com.magomez.androidapps.movierec.schedule.ScheduleService}).
 *
 * @param title         the film's display title (resolved against the festival's films
 *                      catalogue, not necessarily the session's own display name — see
 *                      {@code SitgesScheduleClient}; never blank)
 * @param date          the screening's date
 * @param startTime     the screening's start time
 * @param endTime       the screening's end time, or {@code null} when the source doesn't say
 * @param location      venue/room name, or {@code null} when the source doesn't say
 * @param sessionFilms  every film title playing in this same physical session, in
 *                      programme order (a double bill or marathon lists more than one;
 *                      always includes {@code title} itself)
 * @param originalTitle the film's original-language title, when the catalogue gives one
 *                      different from {@code title} (e.g. a festival markets a film
 *                      internationally under one name but its native title is another —
 *                      a requester may know it by either); {@code null} when there isn't one
 */
public record FilmScreening(
        String title, LocalDate date, LocalTime startTime, LocalTime endTime, String location,
        List<String> sessionFilms, String originalTitle) {

    public FilmScreening {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(startTime, "startTime");
        sessionFilms = (sessionFilms == null || sessionFilms.isEmpty()) ? List.of(title) : List.copyOf(sessionFilms);
    }

    /** Backward-compatible constructor: no separate original-language title. */
    public FilmScreening(String title, LocalDate date, LocalTime startTime, LocalTime endTime, String location,
                          List<String> sessionFilms) {
        this(title, date, startTime, endTime, location, sessionFilms, null);
    }

    /** Backward-compatible constructor for a solo screening (its own single-film session). */
    public FilmScreening(String title, LocalDate date, LocalTime startTime, LocalTime endTime, String location) {
        this(title, date, startTime, endTime, location, List.of(title), null);
    }
}
