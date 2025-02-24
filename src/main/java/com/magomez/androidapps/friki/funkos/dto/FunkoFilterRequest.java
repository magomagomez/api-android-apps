package com.magomez.androidapps.friki.funkos.dto;

import java.io.Serial;
import java.io.Serializable;

public record FunkoFilterRequest(
        String name,
        String category,
        Boolean wish
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


}
