package com.magomez.androidapps.restaurants.converter;

import com.magomez.androidapps.restaurants.dto.Restaurant;
import com.magomez.androidapps.restaurants.dto.RestaurantDTO;
import com.magomez.androidapps.restaurants.dto.VisitDTO;

import java.util.List;

public class RestaurantConverter {

    private RestaurantConverter() {
        throw new UnsupportedOperationException("Cannot instantiate utilities class");
    }

    public static RestaurantDTO toDto(Restaurant restaurant) {
        VisitDTO visit = restaurant.visitedOn() == null ? null
                : new VisitDTO(restaurant.visitedOn(), restaurant.rating(), restaurant.comment());
        return new RestaurantDTO(restaurant.id(), restaurant.name(), restaurant.city(), restaurant.cuisine(),
                restaurant.priceLevel(), restaurant.website(), restaurant.location(), restaurant.driveFolderUrl(),
                restaurant.whyGo(), visit);
    }

    public static List<RestaurantDTO> toDtoList(List<Restaurant> restaurants) {
        return restaurants.stream().map(RestaurantConverter::toDto).toList();
    }
}
