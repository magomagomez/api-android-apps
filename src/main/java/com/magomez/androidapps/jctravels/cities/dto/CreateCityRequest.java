package com.magomez.androidapps.jctravels.cities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record CreateCityRequest(
        @JsonProperty("name")
        String name,
        @JsonProperty("travel")
        Integer travel,
        // Optional: what the city will have; all false when absent (as for the Cordova app).
        @JsonProperty("has_monuments")
        Boolean hasMonuments,
        @JsonProperty("has_parks")
        Boolean hasParks,
        @JsonProperty("has_outlets")
        Boolean hasOutlets
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
