package com.magomez.androidapps.jctravels.monuments.dto;

import java.io.Serial;
import java.io.Serializable;

public record MonumentFilter (
        Integer city,
        Integer route
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


}
