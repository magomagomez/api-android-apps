package com.magomez.androidapps.jctravels.routes.converter;

import com.magomez.androidapps.jctravels.routes.dto.CreateRoute;
import com.magomez.androidapps.jctravels.routes.dto.CreateRouteRequest;
import com.magomez.androidapps.jctravels.routes.dto.Route;
import com.magomez.androidapps.jctravels.routes.dto.RouteDTO;
import com.magomez.androidapps.jctravels.routes.dto.RouteFilter;
import com.magomez.androidapps.jctravels.routes.dto.RouteFilterRequest;
import com.magomez.androidapps.jctravels.routes.dto.UpdateRoute;
import com.magomez.androidapps.jctravels.routes.dto.UpdateRouteRequest;

import java.util.List;

public class RouteConverter {

    private RouteConverter(){}

    public static List<RouteDTO> toDtoList(List<Route> routes){
        return routes.stream()
                .map(RouteConverter::toDto)
                .toList();
    }

    public static RouteDTO toDto(Route route) {
        return new RouteDTO(
                route.id(),
                route.name(),
                route.city(),
                route.day(),
                route.month()
        );
    }

    public static CreateRoute toRecord(CreateRouteRequest request) {
        return new CreateRoute(
                request.name(),
                request.city()
        );
    }

    public static UpdateRoute toRecord(UpdateRouteRequest request) {
        return new UpdateRoute(
                request.name(),
                request.city()
        );
    }

    public static RouteFilter toFilter(RouteFilterRequest request){
        return new RouteFilter(
                request.city()
        );
    }
}
