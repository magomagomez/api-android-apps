package com.magomez.androidapps.jctravels.parks.dao;

import com.magomez.androidapps.jctravels.parks.dto.Park;
import com.magomez.androidapps.jctravels.parks.dto.UpdatePark;
import com.magomez.androidapps.jctravels.parks.mapper.ParkMapper;
import com.magomez.androidapps.jctravels.parks.dto.ParkFilter;
import com.magomez.androidapps.jctravels.parks.dto.CreatePark;
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
public class ParkDao {

    private static final String TABLE_PARQUES = "parks";
    private static final String COLUMN_ID = " id ";
    private static final String COLUMN_CITY = " city ";

    private final JdbcTemplate jdbcTemplate;

    public ParkDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Park> getAllParks(ParkFilter filter) {
        List<Object> args = new ArrayList<>();
        String query = SELECT_ALL + FROM + TABLE_PARQUES + " " + WHERE +" 1=1 ";
        if(filter.city() != null) {
            query = query + AND + COLUMN_CITY + " = ? ";
            args.add(filter.city());
        }
        query = query + " order by name asc";
        return jdbcTemplate.query(query, new ParkMapper(), args.toArray());
    }

    public Park getPark(Integer parkId) {
        String query = SELECT_ALL + FROM + TABLE_PARQUES + " " + WHERE + COLUMN_ID + "= ?";
        try {
            return jdbcTemplate.queryForObject(query, new ParkMapper(), parkId);
        }
        catch(Exception e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Park not found");
        }
    }

    public void createPark(CreatePark park) {
        String query = INSERT + TABLE_PARQUES + " (name,city) " +
                VALUES + "(?,?)";
        jdbcTemplate.update(query , park.name(), park.city());
    }

    public void updatePark(Integer parkId, UpdatePark park) {
        List<Object> args = new ArrayList<>();
        String query = UPDATE + TABLE_PARQUES + SET + COLUMN_ID + " = ? ";
        args.add(parkId);
        query = getUpdateValues(park, query, args);
        query = query + WHERE + COLUMN_ID + " = ?";
        args.add(parkId);
        jdbcTemplate.update(query, args.toArray());
    }

    private String getUpdateValues(UpdatePark park, String query, List<Object> args) {
        if(park.name() != null){
            query = query + ",name = ?";
            args.add(park.name());
        }
        if(park.city() != null){
            query = query + ",city = ?";
            args.add(park.city());
        }
        return query;
    }

}
