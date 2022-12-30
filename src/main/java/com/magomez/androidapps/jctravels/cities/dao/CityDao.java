package com.magomez.androidapps.jctravels.cities.dao;

import com.magomez.androidapps.jctravels.cities.dto.City;
import com.magomez.androidapps.jctravels.cities.dto.CityFilter;
import com.magomez.androidapps.jctravels.cities.dto.CreateCity;
import com.magomez.androidapps.jctravels.cities.dto.UpdateCity;
import com.magomez.androidapps.jctravels.cities.mapper.CityMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
        String query = SELECT_ALL;
        query = query + FROM + TABLE_CIUDADES + " ";
        query = query + WHERE +" 1=1 ";
        if(filter.travel() != null){
            query = query + AND + COLUMN_TRAVEL + " = " + filter.travel();
        }

        query = query + " order by name asc";
        return  jdbcTemplate.query(query, new CityMapper());
    }

    public City getCity(Integer cityId) {
        String query = SELECT_ALL;
        query = query + FROM + TABLE_CIUDADES + " ";
        query = query + WHERE + COLUMN_ID + "= "+ cityId;
        try {
            return jdbcTemplate.queryForObject(query, new CityMapper());
        }
        catch(Exception e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found");
        }
    }

    public void createCity(CreateCity city) {
        String query = INSERT + TABLE_CIUDADES + " (name,travel) " +
                VALUES + "(?,?)";
        jdbcTemplate.update(query , city.name(), city.travel());
    }

    public void updateCity(Integer cityId, UpdateCity city) {
        String query = UPDATE + TABLE_CIUDADES + SET + COLUMN_ID + " = " + cityId + " ";
        query = getUpdateValues(city, query);
        query = query  + WHERE + COLUMN_ID + " = "+ cityId;
        jdbcTemplate.update(query);
    }

    private String getUpdateValues(UpdateCity city, String query) {
        if(city.name() != null){
            query = query + ", " + COLUMN_NAME +  "= '" + city.name() +"'";
        }
        if(city.travel() != null){
            query = query + ", " + COLUMN_TRAVEL +  "= " +  city.travel();
        }
        return query;
    }

}
