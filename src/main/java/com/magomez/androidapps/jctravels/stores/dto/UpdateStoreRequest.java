package com.magomez.androidapps.jctravels.stores.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record UpdateStoreRequest(
        @JsonProperty("name")
        String name,
        @JsonProperty("outlet")
        Integer outlet,
        @JsonProperty("done")
        Boolean done
) implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
}
