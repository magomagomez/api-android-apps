package com.magomez.androidapps.jctravels.monuments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record MonumentDTO (
        @JsonProperty("id")
        Integer id,
        @JsonProperty("name")
        String name,
        @JsonProperty("image_path")
        String imagePath,
        @JsonProperty("done")
        Boolean done,
        @JsonProperty("city")
        Integer city,
        @JsonProperty("schedule")
        String schedule,
        @JsonProperty("price")
        String price,
        @JsonProperty("route")
        Integer route,
        @JsonProperty("order")
        Integer priority
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

}
