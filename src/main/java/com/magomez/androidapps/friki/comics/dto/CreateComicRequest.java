package com.magomez.androidapps.friki.comics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record CreateComicRequest(
        String name,
        @JsonProperty("image_path")
        String imagePath,
        @JsonProperty("is_marvel_ord_dc")
        Integer marvelOrDc,
        String category
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
