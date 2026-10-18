package com.magomez.androidapps.jcfit.training;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/** One of the seven days of {@link WeekView}; a REST day has every program field null. */
public record WeekDay(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate date,
        int dayOfWeek,
        DayStatus status,
        Integer programDayId,
        String name,
        String muscles,
        int exerciseCount,
        int completedExercises,
        Integer sessionId
) {
}
