package com.magomez.androidapps.restaurants.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A row of gourmet_restaurants. visitedOn and rating are both null until the visit. */
public record Restaurant(
        Integer id,
        String name,
        String city,
        String cuisine,
        Integer priceLevel,
        String website,
        String location,
        String driveFolderUrl,
        String whyGo,
        LocalDate visitedOn,
        BigDecimal rating,
        String comment
) {
}
