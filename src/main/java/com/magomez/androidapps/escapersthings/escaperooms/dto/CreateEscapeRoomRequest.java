package com.magomez.androidapps.escapersthings.escaperooms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record CreateEscapeRoomRequest(
        @JsonProperty("nombre")
        String nombre,
        @JsonProperty("imagen_path")
        String imagenPath,
        @JsonProperty("tipo")
        String tipo
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}