package com.magomez.androidapps.jcfit.training;

import java.math.BigDecimal;

/** A completed set as shown in previous and history: weightKg is null for bodyweight sets. */
public record SetEntry(int setNumber, BigDecimal weightKg, Integer reps) {
}
