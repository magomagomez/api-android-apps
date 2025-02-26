package com.magomez.androidapps.friki.comics.dto;

import java.io.Serial;
import java.io.Serializable;

public record ComicFilterRequest(
        String name,
        Boolean wish,
        Boolean marvel
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


}
