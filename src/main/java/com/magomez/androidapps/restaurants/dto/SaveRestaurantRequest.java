package com.magomez.androidapps.restaurants.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Body of POST and PUT /restaurants: everything except the visit. */
public record SaveRestaurantRequest(
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
        String whyGo
) {
}
