package com.magomez.androidapps.jctravels.monuments.dto;

import java.io.Serial;
import java.io.Serializable;

public record UpdateMonument(
        Boolean done,
        Integer route
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
