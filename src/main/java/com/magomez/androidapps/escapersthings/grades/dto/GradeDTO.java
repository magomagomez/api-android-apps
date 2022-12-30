package com.magomez.androidapps.escapersthings.grades.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

public record GradeDTO(
        @JsonProperty("enigma")
        BigDecimal enigma,
        @JsonProperty("game_master")
        BigDecimal gameMaster,
        @JsonProperty("inmersion")
        BigDecimal inmersion,
        @JsonProperty("terror")
        BigDecimal terror,
        @JsonProperty("global")
        BigDecimal global
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
