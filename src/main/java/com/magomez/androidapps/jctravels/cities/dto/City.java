package com.magomez.androidapps.jctravels.cities.dto;

import java.io.Serial;
import java.io.Serializable;

public record City(
        Integer id,
        String name,
        String iconPath,
        Integer travel,
        Boolean hasParks,
        Boolean hasMonuments,
        Boolean hasOutlets
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
