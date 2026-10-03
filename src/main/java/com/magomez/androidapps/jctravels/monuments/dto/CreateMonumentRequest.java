package com.magomez.androidapps.jctravels.monuments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record CreateMonumentRequest(
        @JsonProperty("name")
        String name,
        @JsonProperty("city")
        Integer city,
        @JsonProperty("schedule")
        String schedule,
        @JsonProperty("price")
        String price,
        // Optional: the day route to file it under; the city's default route when absent.
        @JsonProperty("route")
        Integer route
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
