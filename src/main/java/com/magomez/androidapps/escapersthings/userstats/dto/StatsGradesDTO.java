package com.magomez.androidapps.escapersthings.userstats.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record StatsGradesDTO(
        @JsonProperty("name")
        String name,
        @JsonProperty("id")
        Integer id,
        @JsonProperty("userEnigma")
        Double userEnigma,
        @JsonProperty("userGM")
        Double userGM,
        @JsonProperty("userInmersion")
        Double userInmersion,
        @JsonProperty("userHorror")
        Double userHorror,
        @JsonProperty("userGlobal")
        Double userGlobal
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
