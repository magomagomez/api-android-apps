package com.magomez.androidapps.jctravels.travels.dao;

import com.magomez.androidapps.jctravels.travels.dto.CreateTravel;
import com.magomez.androidapps.jctravels.travels.dto.Travel;
import com.magomez.androidapps.jctravels.travels.mapper.TravelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.FROM;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.INSERT;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.SELECT_ALL;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.VALUES;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.WHERE;

@Service
public class TravelDao {

    private static final String TABLE_TRAVELS = "travels";
    private static final String COLUMN_ID = " id ";

    private final JdbcTemplate jdbcTemplate;

    public TravelDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Travel> getAllTravels() {
        String query = SELECT_ALL;
        query = query + FROM + TABLE_TRAVELS + " ";
        query = query + " order by id asc";
        return  jdbcTemplate.query(query, new TravelMapper());
    }

    public Travel getCity(Integer travelId) {
        String query = SELECT_ALL;
        query = query + FROM + TABLE_TRAVELS + " ";
        query = query + WHERE + COLUMN_ID + "= "+ travelId;
        try {
            return jdbcTemplate.queryForObject(query, new TravelMapper());
        }
        catch(Exception e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found");
        }
    }

    public void createTravel(CreateTravel travel) {
        String query = INSERT + TABLE_TRAVELS + " (name) " +
                VALUES + "(?)";
        jdbcTemplate.update(query , travel.name());
    }

}
