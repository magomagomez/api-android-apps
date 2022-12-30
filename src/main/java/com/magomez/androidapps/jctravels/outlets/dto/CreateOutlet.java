package com.magomez.androidapps.jctravels.outlets.dto;

import java.io.Serial;
import java.io.Serializable;

public record CreateOutlet(
        String name,
        Integer city
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
