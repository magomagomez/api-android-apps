package com.magomez.androidapps.jctravels.travels.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record CreateTravel(
        @JsonProperty("name")
        String name
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
