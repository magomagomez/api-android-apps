package com.magomez.androidapps.escapersthings.userstats.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record UserGradesDTO(
        @JsonProperty("all_escapes")
        List<StatsGradesDTO> allEscapes,
        @JsonProperty("top_horror")
        List<StatsGradesDTO> top_horror,
        @JsonProperty("top_escapes")
        List<StatsGradesDTO> top_escapes,
        @JsonProperty("top_inmersion")
        List<StatsGradesDTO> top_inmersion,
        @JsonProperty("top_gm")
        List<StatsGradesDTO> top_gm,
        @JsonProperty("top_worst")
        List<StatsGradesDTO> top_worst
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
