package com.magomez.androidapps.restaurants.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The shared visit of a tried restaurant: when, the 1-10 rating (half points) and a comment. */
public record VisitDTO(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate date,
        BigDecimal rating,
        String comment
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
