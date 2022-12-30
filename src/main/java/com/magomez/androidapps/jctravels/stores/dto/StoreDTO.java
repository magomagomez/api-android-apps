package com.magomez.androidapps.jctravels.stores.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record StoreDTO(
        @JsonProperty("id")
        Integer id,
        @JsonProperty("name")
        String name,
        @JsonProperty("image_path")
        String imagePath,
        @JsonProperty("done")
        Boolean done,
        @JsonProperty("outlet")
        Integer outlet
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

}
