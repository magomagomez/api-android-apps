package com.magomez.androidapps.jctravels.outlets.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record UpdateOutletRequest(
        @JsonProperty("name")
        String name,
        @JsonProperty("city")
        Integer city
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
