package com.magomez.androidapps.jctravels.cities.dao;

import com.magomez.androidapps.jctravels.cities.dto.City;
import com.magomez.androidapps.jctravels.cities.dto.CityContent;
import com.magomez.androidapps.jctravels.cities.dto.CityFilter;
import com.magomez.androidapps.jctravels.cities.dto.CreateCity;
import com.magomez.androidapps.jctravels.cities.dto.UpdateCity;
import com.magomez.androidapps.jctravels.cities.mapper.CityMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.dao.EmptyResultDataAccessException;
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
public class CityDao {

    private static final String TABLE_CIUDADES = "cities";
    private static final String COLUMN_ID = " id ";
    private static final String COLUMN_NAME = " name ";
    private static final String COLUMN_TRAVEL = " travel ";

    private final JdbcTemplate jdbcTemplate;

    public CityDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<City> getAllCities(CityFilter filter) {
        List<Object> args = new ArrayList<>();
        String query = SELECT_ALL + FROM + TABLE_CIUDADES + " " + WHERE +" 1=1 ";
        if(filter.travel() != null){
            query = query + AND + COLUMN_TRAVEL + " = ?";
            args.add(filter.travel());
        }

        query = query + " order by name asc";
        return jdbcTemplate.query(query, new CityMapper(), args.toArray());
    }

    public City getCity(Integer cityId) {
        String query = SELECT_ALL + FROM + TABLE_CIUDADES + " " + WHERE + COLUMN_ID + "= ?";
        try {
            return jdbcTemplate.queryForObject(query, new CityMapper(), cityId);
        }
        catch(EmptyResultDataAccessException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found");
        }
    }

    public void createCity(CreateCity city) {
        String query = INSERT + TABLE_CIUDADES + " (name,travel,has_monuments,has_parks,has_outlets) " +
                VALUES + "(?,?,?,?,?)";
        jdbcTemplate.update(query, city.name(), city.travel(),
                Boolean.TRUE.equals(city.hasMonuments()), Boolean.TRUE.equals(city.hasParks()), Boolean.TRUE.equals(city.hasOutlets()));
    }

    /** Turns a content flag on once the city gets something for it; flags are never turned off here. */
    public void markHas(Integer cityId, CityContent content) {
        String query = "UPDATE " + TABLE_CIUDADES + " SET " + content.column() + " = true WHERE id = ?";
        jdbcTemplate.update(query, cityId);
    }

    public void updateCity(Integer cityId, UpdateCity city) {
        List<Object> args = new ArrayList<>();
        String query = UPDATE + TABLE_CIUDADES + SET + COLUMN_ID + " = ? ";
        args.add(cityId);
        query = getUpdateValues(city, query, args);
        query = query  + WHERE + COLUMN_ID + " = ?";
        args.add(cityId);
        jdbcTemplate.update(query, args.toArray());
    }

    private String getUpdateValues(UpdateCity city, String query, List<Object> args) {
        if(city.name() != null){
            query = query + ", " + COLUMN_NAME +  "= ?";
            args.add(city.name());
        }
        if(city.travel() != null){
            query = query + ", " + COLUMN_TRAVEL +  "= ?";
            args.add(city.travel());
        }
        return query;
    }

}
