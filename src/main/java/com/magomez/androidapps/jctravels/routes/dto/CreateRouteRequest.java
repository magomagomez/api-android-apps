package com.magomez.androidapps.jctravels.routes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record CreateRouteRequest(
        @JsonProperty("name")
        String name,
        @JsonProperty("city")
        Integer city,
        // Optional date of a day route ("19", "Agost"); absent for named groups like "Central Park".
        @JsonProperty("day")
        String day,
        @JsonProperty("month")
        String month
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
