package com.magomez.androidapps.friki.props.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record CreatePropRequest(
        String name,
        @JsonProperty("image_path")
        String imagePath,
        String category
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
