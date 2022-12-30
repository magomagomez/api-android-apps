package com.magomez.androidapps.jctravels.routes.controller;

import com.magomez.androidapps.jctravels.config.ApiConfig;
import com.magomez.androidapps.jctravels.routes.service.RouteService;
import com.magomez.androidapps.jctravels.routes.dto.CreateRouteRequest;
import com.magomez.androidapps.jctravels.routes.dto.RouteDTO;
import com.magomez.androidapps.jctravels.routes.dto.RouteFilterRequest;
import com.magomez.androidapps.jctravels.routes.dto.UpdateRouteRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(value = ApiConfig.BASE_URL + "/routes")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class RouteController {

    private final RouteService routeService;

    @Autowired
    public RouteController(RouteService routesService){

        this.routeService = routesService;
    }

    @GetMapping
    public List<RouteDTO> getRoutes(@Valid RouteFilterRequest requestFilter) {

        return routeService.getRoutes(requestFilter);
    }

    @GetMapping("{routeId}")
    public RouteDTO getRoute(@PathVariable Integer routeId) {

        return routeService.getRoute(routeId);
    }

    @PostMapping
    public void createRoute(@RequestBody CreateRouteRequest request) {

        routeService.createRoute(request);
    }

    @PutMapping("{routeId}")
    public void updateRoute(@PathVariable Integer routeId,
                            @RequestBody UpdateRouteRequest request) {

        routeService.updateRoute(routeId, request);
    }

}
