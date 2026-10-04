package com.magomez.androidapps.restaurants.controller;

import com.magomez.androidapps.restaurants.config.ApiConfig;
import com.magomez.androidapps.restaurants.dto.RestaurantDTO;
import com.magomez.androidapps.restaurants.dto.SaveRestaurantRequest;
import com.magomez.androidapps.restaurants.dto.VisitRequest;
import com.magomez.androidapps.restaurants.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = ApiConfig.BASE_URL + "/restaurants")
@Tag(name = "JC Gourmet", description = "Restaurantes pendientes y probados")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @Operation(summary = "List restaurants (visited=0 pending, visited=1 tried)")
    @GetMapping
    public List<RestaurantDTO> list(@RequestParam(required = false) Integer visited) {
        return restaurantService.list(visited);
    }

    @Operation(summary = "Get restaurant by id")
    @GetMapping("{restaurantId}")
    public RestaurantDTO get(@PathVariable int restaurantId) {
        return restaurantService.get(restaurantId);
    }

    @Operation(summary = "Create restaurant (X-Api-Key)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantDTO create(@RequestBody SaveRestaurantRequest request) {
        return restaurantService.create(request);
    }

    @Operation(summary = "Update restaurant (X-Api-Key)")
    @PutMapping("{restaurantId}")
    public RestaurantDTO update(@PathVariable int restaurantId, @RequestBody SaveRestaurantRequest request) {
        return restaurantService.update(restaurantId, request);
    }

    @Operation(summary = "Save the visit: date, 1-10 rating in half points, comment (X-Api-Key)")
    @PutMapping("{restaurantId}/visit")
    public RestaurantDTO saveVisit(@PathVariable int restaurantId, @RequestBody VisitRequest request) {
        return restaurantService.saveVisit(restaurantId, request);
    }

    @Operation(summary = "Remove the visit, back to pending (X-Api-Key)")
    @DeleteMapping("{restaurantId}/visit")
    public RestaurantDTO clearVisit(@PathVariable int restaurantId) {
        return restaurantService.clearVisit(restaurantId);
    }

    @Operation(summary = "Delete restaurant (X-Api-Key)")
    @DeleteMapping("{restaurantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int restaurantId) {
        restaurantService.delete(restaurantId);
    }
}
