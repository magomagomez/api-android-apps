package com.magomez.androidapps.jctravels.stores.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record StoreFilterRequest(
        @JsonProperty("outlet")
        Integer outlet
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


}
