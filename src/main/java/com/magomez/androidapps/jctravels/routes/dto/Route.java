package com.magomez.androidapps.jctravels.routes.dto;

import java.io.Serial;
import java.io.Serializable;

public record Route(
        Integer id,
        String name,
        Integer city,
        String day,
        String month,
        Integer sort) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
