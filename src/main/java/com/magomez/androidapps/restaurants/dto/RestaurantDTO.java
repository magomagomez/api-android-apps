package com.magomez.androidapps.restaurants.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record RestaurantDTO(
        Integer id,
        String name,
        String city,
        String cuisine,
        @JsonProperty("price_level")
        Integer priceLevel,
        String website,
        String location,
        @JsonProperty("drive_folder_url")
        String driveFolderUrl,
        @JsonProperty("why_go")
        String whyGo,
        VisitDTO visit
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
