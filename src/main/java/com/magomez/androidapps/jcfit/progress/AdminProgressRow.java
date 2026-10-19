package com.magomez.androidapps.jcfit.progress;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A session joined with one of its completed sets; the set columns are null when it has none. */
record AdminProgressRow(int sessionId, LocalDate date, String dayName, boolean finished, Integer exerciseId,
                        String exerciseName, Integer setNumber, BigDecimal weightKg, Integer reps) {
}
