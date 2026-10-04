package com.magomez.androidapps.restaurants.controller;

import com.magomez.androidapps.restaurants.config.ApiConfig;
import com.magomez.androidapps.restaurants.dto.RestaurantDTO;
import com.magomez.androidapps.restaurants.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
}
