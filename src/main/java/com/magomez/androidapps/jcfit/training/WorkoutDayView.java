package com.magomez.androidapps.jcfit.training;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** GET /days/{programDayId}?date=: the session fields are null until the day is started. */
public record WorkoutDayView(
        int programDayId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate date,
        int dayOfWeek,
        int weekNumber,
        String name,
        String muscles,
        Integer sessionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant startedAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant finishedAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate lastDate,
        List<DayExercise> exercises
) {
}
