package com.magomez.androidapps.restaurants.dto;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The shared visit of a tried restaurant: when, the 1-10 rating (half points) and a comment. */
public record VisitDTO(
        LocalDate date,
        BigDecimal rating,
        String comment
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
