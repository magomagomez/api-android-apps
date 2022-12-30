package com.magomez.androidapps.escapersthings.userstats.dto;

import java.io.Serial;
import java.io.Serializable;

public record Stats(
        String total,
        String type
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
