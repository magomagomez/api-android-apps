package com.magomez.androidapps.jctravels.rides.dto;

import java.io.Serial;
import java.io.Serializable;

public record CreateRide(
        String name,
        Integer park
) implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
}
