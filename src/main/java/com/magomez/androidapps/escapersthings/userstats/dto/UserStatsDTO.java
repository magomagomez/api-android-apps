package com.magomez.androidapps.escapersthings.userstats.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record UserStatsDTO(
        @JsonProperty("tipo")
        String tipo,
        @JsonProperty("total")
        String total
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
