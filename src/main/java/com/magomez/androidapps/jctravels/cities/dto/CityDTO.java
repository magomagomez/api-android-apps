package com.magomez.androidapps.jctravels.cities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record CityDTO(
        @JsonProperty("id")
        Integer id,
        @JsonProperty("name")
        String name,
        @JsonProperty("icon_path")
        String iconPath,
        @JsonProperty("travel")
        Integer travel,
        @JsonProperty("has_parks")
        Boolean hasParks,
        @JsonProperty("has_monuments")
        Boolean hasMonuments,
        @JsonProperty("has_outlets")
        Boolean hasOutlets
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
