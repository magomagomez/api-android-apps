package com.magomez.androidapps.jctravels.rides.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record CreateRideRequest(
        @JsonProperty("name")
        String name,
        @JsonProperty("park")
        Integer park
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
