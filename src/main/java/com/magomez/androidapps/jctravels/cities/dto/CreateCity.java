package com.magomez.androidapps.jctravels.cities.dto;

import java.io.Serial;
import java.io.Serializable;

public record CreateCity(
        String name,
        Integer travel,
        Boolean hasMonuments,
        Boolean hasParks,
        Boolean hasOutlets
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
