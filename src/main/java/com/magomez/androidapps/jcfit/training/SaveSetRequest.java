package com.magomez.androidapps.jcfit.training;

import java.math.BigDecimal;

/** Body of PUT /sessions/{id}/sets; completed missing means false. */
public record SaveSetRequest(Integer exerciseId, Integer setNumber, BigDecimal weightKg, Integer reps,
                             Boolean completed) {
}
