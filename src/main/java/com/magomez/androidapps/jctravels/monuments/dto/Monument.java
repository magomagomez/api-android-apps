package com.magomez.androidapps.jctravels.monuments.dto;

import java.io.Serial;
import java.io.Serializable;

public record Monument(
        Integer id,
        String name,
        String imagePath,
        Boolean done,
        Integer city,
        String schedule,
        String price,
        Integer route,
        Integer priority) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
