package com.magomez.androidapps.friki.decks.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record CreateDeckRequest(
        String name,
        @JsonProperty("is_city_deck")
        Integer isCityDeck,
        String category
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
