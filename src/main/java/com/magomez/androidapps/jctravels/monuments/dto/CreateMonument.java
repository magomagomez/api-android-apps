package com.magomez.androidapps.jctravels.monuments.dto;

import java.io.Serial;
import java.io.Serializable;

public record CreateMonument(
        String name,
        Integer city,
        String schedule,
        String price,
        Integer route) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
