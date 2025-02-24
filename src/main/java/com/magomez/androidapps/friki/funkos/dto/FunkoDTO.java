package com.magomez.androidapps.friki.funkos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public record FunkoDTO(
        Integer id,
        String name,
        @JsonProperty("image_path")
        String imagePath,
        @JsonProperty("wish_list")
        Boolean isWishList,
        Integer number,
        List<String> categories
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
