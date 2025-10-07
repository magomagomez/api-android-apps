package com.magomez.androidapps.friki.funkos.dto;

import java.io.Serial;
import java.io.Serializable;

public record CreateFunkoRequest(
        String name,
        Integer number,
        String category
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
