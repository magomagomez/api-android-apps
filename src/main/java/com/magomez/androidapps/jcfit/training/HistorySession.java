package com.magomez.androidapps.jcfit.training;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** One session of {@link ExerciseHistory}, with its completed sets of the exercise only. */
public record HistorySession(
        int sessionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate date,
        String dayName,
        BigDecimal topWeightKg,
        BigDecimal volumeKg,
        int totalReps,
        List<SetEntry> sets
) {
}
