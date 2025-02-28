package com.magomez.androidapps.friki.decks.dto;

import java.io.Serial;
import java.io.Serializable;

public record DeckFilterRequest(
        String name,
        Boolean wish,
        Boolean city
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


}
