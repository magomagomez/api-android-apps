package com.magomez.androidapps.jctravels.parks.dto;

import java.io.Serial;
import java.io.Serializable;

public record ParkFilter(
        Integer city
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


}
