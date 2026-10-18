package com.magomez.androidapps.jcfit.training;

import java.math.BigDecimal;

/** A saved set of a session, completed or not; also the answer of PUT /sessions/{id}/sets. */
public record LoggedSet(int setNumber, BigDecimal weightKg, Integer reps, boolean completed) {
}
