package com.magomez.androidapps.jctravels.rides.dto;

import java.io.Serial;
import java.io.Serializable;

public record Ride (
        Integer id,
        String name,
        String imagePath,
        Boolean done,
        Integer park,
        Boolean must,
        String code,
        String locationPath
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
