package com.magomez.androidapps.restaurants.service;

import com.magomez.androidapps.restaurants.converter.RestaurantConverter;
import com.magomez.androidapps.restaurants.dao.RestaurantDao;
import com.magomez.androidapps.restaurants.dto.RestaurantDTO;
import com.magomez.androidapps.restaurants.dto.SaveRestaurantRequest;
import com.magomez.androidapps.restaurants.dto.VisitRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class RestaurantService {

    private static final String NOT_FOUND = "Restaurant not found";
    private static final String INVALID_VISITED = "visited must be 0 or 1";

    private final RestaurantDao restaurantDao;
    private final Clock clock;

    public RestaurantService(RestaurantDao restaurantDao, Clock gourmetClock) {
        this.restaurantDao = restaurantDao;
        this.clock = gourmetClock;
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
                .orElseThrow(RestaurantService::notFound);
    }

    public RestaurantDTO create(SaveRestaurantRequest request) {
        SaveRestaurantRequest clean = RestaurantValidator.restaurant(request);
        int id = restaurantDao.insert(clean);
        return RestaurantConverter.toDto(id, clean);
    }

    public RestaurantDTO update(int id, SaveRestaurantRequest request) {
        SaveRestaurantRequest clean = RestaurantValidator.restaurant(request);
        requireFound(restaurantDao.update(id, clean));
        return get(id);
    }

    public RestaurantDTO saveVisit(int id, VisitRequest request) {
        VisitRequest clean = RestaurantValidator.visit(request, LocalDate.now(clock));
        requireFound(restaurantDao.saveVisit(id, clean));
        return get(id);
    }

    public RestaurantDTO clearVisit(int id) {
        requireFound(restaurantDao.clearVisit(id));
        return get(id);
    }

    public void delete(int id) {
        requireFound(restaurantDao.delete(id));
    }

    private static void requireFound(boolean found) {
        if (!found) {
            throw notFound();
        }
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND);
    }
}
