package com.magomez.androidapps.jctravels.routes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record CreateRouteRequest(
        @JsonProperty("name")
        String name,
        @JsonProperty("city")
        Integer city
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
