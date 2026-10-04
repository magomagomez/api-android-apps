package com.magomez.androidapps.restaurants.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Body of PUT /restaurants/{id}/visit. */
public record VisitRequest(
        LocalDate date,
        BigDecimal rating,
        String comment
) {
}
