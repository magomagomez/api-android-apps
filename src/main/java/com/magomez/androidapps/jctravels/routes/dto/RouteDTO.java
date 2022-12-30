package com.magomez.androidapps.jctravels.routes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record RouteDTO (
        @JsonProperty("id")
        Integer id,
        @JsonProperty("name")
        String name,
        @JsonProperty("city")
        Integer city,
        @JsonProperty("day")
        String day,
        @JsonProperty("month")
        String month
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
