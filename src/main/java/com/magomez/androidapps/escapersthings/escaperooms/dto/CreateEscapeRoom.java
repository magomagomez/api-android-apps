package com.magomez.androidapps.escapersthings.escaperooms.dto;

import java.io.Serial;
import java.io.Serializable;

public record CreateEscapeRoom(
        String nombre,
        String imagenPath,
        String tipo
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}