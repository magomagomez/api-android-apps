package com.magomez.androidapps.jctravels.routes.dao;

import com.magomez.androidapps.jctravels.routes.dto.CreateRoute;
import com.magomez.androidapps.jctravels.routes.dto.Route;
import com.magomez.androidapps.jctravels.routes.dto.UpdateRoute;
import com.magomez.androidapps.jctravels.routes.mapper.RouteMapper;
import com.magomez.androidapps.jctravels.routes.dto.RouteFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.AND;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.FROM;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.INSERT;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.SELECT_ALL;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.SET;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.UPDATE;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.VALUES;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.WHERE;

@Service
public class RouteDao {

    private static final String TABLE_ROUTES = " routes ";
    private static final String COLUMN_CITY = " city ";
    private static final String COLUMN_NAME = " name ";
    private static final String COLUMN_SORT = " sort ";
    private static final String COLUMN_ID = " id ";
    private static final String COLUMN_DEF = " def ";

    private final JdbcTemplate jdbcTemplate;

    public RouteDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Route> getAllRoutes(RouteFilter filter) {
        List<Object> args = new ArrayList<>();
        String query = SELECT_ALL + FROM + TABLE_ROUTES;
        if(filter.city() != null) {
                query = query + WHERE + COLUMN_CITY +" = ?";
                args.add(filter.city());
        }
        query = query + " order by "+COLUMN_SORT+" asc";
        return jdbcTemplate.query(query, new RouteMapper(), args.toArray());
    }

    public Route getRoute(Integer routeId) {
        String query = SELECT_ALL + FROM + TABLE_ROUTES + " " + WHERE + COLUMN_ID +" = ?";
        try {
            return jdbcTemplate.queryForObject(query, new RouteMapper(), routeId);
        }
        catch(Exception e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Park not found");
        }
    }

    public Route getDefaultRoute(Integer city) {
        String query = SELECT_ALL + FROM + TABLE_ROUTES + " " + WHERE + COLUMN_CITY + " = ?" + AND +  COLUMN_DEF + "= 1";

        return jdbcTemplate.queryForObject(query, new RouteMapper(), city);
    }

    public void createRoute(CreateRoute route) {
        String query = INSERT + TABLE_ROUTES + " (name,city,def) " + VALUES + "(?,?,?)";
        jdbcTemplate.update(query , route.name(),route.city(),0);
    }

    public void updateRoute(Integer routeId, UpdateRoute route) {
        List<Object> args = new ArrayList<>();
        String query = UPDATE + TABLE_ROUTES  + SET + COLUMN_ID + " = ? ";
        args.add(routeId);
        query = getUpdateValues(route, query, args);
        query = query + WHERE + COLUMN_ID + " = ?";
        args.add(routeId);
        jdbcTemplate.update(query, args.toArray());
    }

    private String getUpdateValues(UpdateRoute route, String query, List<Object> args) {
        if(route.name() != null){
            query = query + ", " + COLUMN_NAME +  "= ?";
            args.add(route.name());
        }
        if(route.city() != null){
            query = query + ", " + COLUMN_CITY +  "= ?";
            args.add(route.city());
        }
        return query;
    }



}
