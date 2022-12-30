package com.magomez.androidapps.jctravels.rides.dto;

import java.io.Serial;
import java.io.Serializable;

public record UpdateRide(
        String name,
        Integer park,
        Boolean done
) implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
}
