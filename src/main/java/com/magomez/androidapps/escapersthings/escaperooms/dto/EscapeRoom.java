package com.magomez.androidapps.escapersthings.escaperooms.dto;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

public record EscapeRoom(
        Integer id,
        String name,
        String imageUrl,
        Integer done,
        String ubication,
        String type,
        BigDecimal totalNota,
        BigDecimal totalTerror,
        Integer magoDone,
        Integer crisDone
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
