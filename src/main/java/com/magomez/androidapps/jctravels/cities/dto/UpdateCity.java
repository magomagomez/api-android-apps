package com.magomez.androidapps.jctravels.cities.dto;

import java.io.Serial;
import java.io.Serializable;

public record UpdateCity(
        String name,
        Integer travel
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
