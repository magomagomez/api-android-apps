package com.magomez.androidapps.jctravels.routes.dto;

import java.io.Serial;
import java.io.Serializable;

public record CreateRoute(
        String name,
        Integer city
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
