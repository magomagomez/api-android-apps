package com.magomez.androidapps.escapersthings.escaperooms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record EscapeRoomFilterRequest(
        @JsonProperty("finalizado")
        Integer finalizado,
        @JsonProperty("valoracion")
        Integer valoracion,
        @JsonProperty("tipo")
        String tipo,
        @JsonProperty("order")
        String order
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

}
