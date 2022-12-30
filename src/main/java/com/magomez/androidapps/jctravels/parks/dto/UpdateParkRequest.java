package com.magomez.androidapps.jctravels.parks.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record UpdateParkRequest(
        @JsonProperty("name")
        String name,
        @JsonProperty("city")
        Integer city
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
