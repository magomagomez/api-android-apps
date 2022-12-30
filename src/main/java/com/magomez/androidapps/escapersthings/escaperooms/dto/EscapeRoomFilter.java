package com.magomez.androidapps.escapersthings.escaperooms.dto;

import java.io.Serial;
import java.io.Serializable;

public record EscapeRoomFilter(
        Integer finalizado,
        Integer valoracion,
        String tipo,
        String order
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

}