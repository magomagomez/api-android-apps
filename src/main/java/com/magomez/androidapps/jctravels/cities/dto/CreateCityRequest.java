package com.magomez.androidapps.jctravels.cities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record CreateCityRequest(
        @JsonProperty("name")
        String name,
        @JsonProperty("travel")
        Integer travel
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
