package com.magomez.androidapps.restaurants.service;

import com.magomez.androidapps.restaurants.converter.RestaurantConverter;
import com.magomez.androidapps.restaurants.dao.RestaurantDao;
import com.magomez.androidapps.restaurants.dto.RestaurantDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RestaurantService {

    private static final String NOT_FOUND = "Restaurant not found";
    private static final String INVALID_VISITED = "visited must be 0 or 1";

    private final RestaurantDao restaurantDao;

    public RestaurantService(RestaurantDao restaurantDao) {
        this.restaurantDao = restaurantDao;
    }

    /** visited: null for all, 0 for the pending ones, 1 for the tried ones. */
    public List<RestaurantDTO> list(Integer visited) {
        if (visited != null && visited != 0 && visited != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_VISITED);
        }
        Boolean filter = visited == null ? null : visited == 1;
        return RestaurantConverter.toDtoList(restaurantDao.findAll(filter));
    }

    public RestaurantDTO get(int id) {
        return restaurantDao.findById(id)
                .map(RestaurantConverter::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND));
    }
}
