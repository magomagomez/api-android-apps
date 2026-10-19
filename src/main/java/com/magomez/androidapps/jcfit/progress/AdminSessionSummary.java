package com.magomez.androidapps.jcfit.progress;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

/** One of Cris's sessions as the backoffice shows it: completed sets only. */
public record AdminSessionSummary(
        int sessionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate date,
        String dayName,
        boolean finished,
        List<AdminSessionExercise> exercises
) {
}
