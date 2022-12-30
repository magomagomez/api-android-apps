package com.magomez.androidapps.escapersthings.escaperooms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

public record EscapeRoomDTO(
        @JsonProperty("id")
        Integer id,
        @JsonProperty("nombre")
        String nombre,
        @JsonProperty("imagen_path")
        String imagenPath,
        @JsonProperty("finalizado")
        Integer finalizado,
        @JsonProperty("ciudad")
        String ciudad,
        @JsonProperty("tipo")
        String tipo,
        @JsonProperty ("nota_total")
        BigDecimal notaTotal,
        @JsonProperty ("terror_total")
        BigDecimal terrorTotal,
        @JsonProperty("finalizado_javi")
        Integer finalizadoJavi,
        @JsonProperty("finalizado_cris")
        Integer finalizadoCris
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
