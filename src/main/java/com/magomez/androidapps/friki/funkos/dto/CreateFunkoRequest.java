package com.magomez.androidapps.friki.funkos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record CreateFunkoRequest(
        String name,
        @JsonProperty("image_path")
        String imagePath,
        Integer number,
        String category
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
