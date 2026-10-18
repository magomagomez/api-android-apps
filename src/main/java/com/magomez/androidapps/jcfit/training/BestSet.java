package com.magomez.androidapps.jcfit.training;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The best set ever of an exercise. */
public record BestSet(
        BigDecimal weightKg,
        Integer reps,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate date
) {
}
