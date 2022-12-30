package com.magomez.androidapps.jctravels.routes.service;

import com.magomez.androidapps.jctravels.routes.converter.RouteConverter;
import com.magomez.androidapps.jctravels.routes.dao.RouteDao;
import com.magomez.androidapps.jctravels.routes.dto.CreateRoute;
import com.magomez.androidapps.jctravels.routes.dto.CreateRouteRequest;
import com.magomez.androidapps.jctravels.routes.dto.Route;
import com.magomez.androidapps.jctravels.routes.dto.RouteDTO;
import com.magomez.androidapps.jctravels.routes.dto.RouteFilter;
import com.magomez.androidapps.jctravels.routes.dto.RouteFilterRequest;
import com.magomez.androidapps.jctravels.routes.dto.UpdateRoute;
import com.magomez.androidapps.jctravels.routes.dto.UpdateRouteRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RouteService {

    private static final String INVALID_PARAMETERS = "Invalid Parameters";
    private final RouteDao routeDao;

    @Autowired
    public RouteService(RouteDao routeDao){
        this.routeDao = routeDao;
    }

    public List<RouteDTO> getRoutes(RouteFilterRequest requestFilter){
        RouteFilter filter = RouteConverter.toFilter(requestFilter);
        List<Route> routes =  routeDao.getAllRoutes(filter);
        return RouteConverter.toDtoList(routes);
    }

    public RouteDTO getRoute(Integer routeId){
        Route route =  routeDao.getRoute(routeId);
        return RouteConverter.toDto(route);
    }

    public void createRoute(CreateRouteRequest request){
        if(request == null || request.name() == null || request.city() == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        CreateRoute route = RouteConverter.toRecord(request);
        routeDao.createRoute(route);
    }

    public void updateRoute(Integer routeId, UpdateRouteRequest request){
        if(request == null || (request.name() == null && request.city() == null)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        UpdateRoute route = RouteConverter.toRecord(request);
        routeDao.updateRoute(routeId,route);
    }
}
