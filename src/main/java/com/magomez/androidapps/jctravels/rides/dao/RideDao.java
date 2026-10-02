package com.magomez.androidapps.jctravels.rides.dao;

import com.magomez.androidapps.jctravels.rides.dto.CreateRide;
import com.magomez.androidapps.jctravels.rides.dto.Ride;
import com.magomez.androidapps.jctravels.rides.dto.RideFilter;
import com.magomez.androidapps.jctravels.rides.dto.UpdateRide;
import com.magomez.androidapps.jctravels.rides.mapper.RideMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.FROM;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.INSERT;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.SELECT_ALL;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.SET;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.UPDATE;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.VALUES;
import static com.magomez.androidapps.jctravels.config.JcTravelsConfig.WHERE;

@Service
public class RideDao {

    private static final String TABLE_RIDES = "rides";
    private static final String COLUMN_ID = " id ";
    private static final String COLUMN_PARK = " park ";

    private final JdbcTemplate jdbcTemplate;

    public RideDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Ride> getAllRides(RideFilter filter) {
        List<Object> args = new ArrayList<>();
        String query = SELECT_ALL + FROM + TABLE_RIDES + " ";
        if(filter.park() != null) {
                query = query + WHERE + COLUMN_PARK + " = ? ";
                args.add(filter.park());
        }

        query = query + "order by done asc, name asc";
        return jdbcTemplate.query(query, new RideMapper(), args.toArray());
    }

    public Ride getRide(Integer rideId) {
        String query = SELECT_ALL + FROM + TABLE_RIDES + " " + WHERE + COLUMN_ID + " = ? ";
        try {
            return jdbcTemplate.queryForObject(query, new RideMapper(), rideId);
        }
        catch(EmptyResultDataAccessException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found");
        }
    }

    public void createRide(CreateRide ride) {
        String query = INSERT + TABLE_RIDES + " (name,park,done) " +
                VALUES + "(?,?,?)";
        jdbcTemplate.update(query , ride.name(),ride.park(),0);
    }

    public void updateRide(Integer rideId, UpdateRide ride) {
        List<Object> args = new ArrayList<>();
        String query = UPDATE + TABLE_RIDES + SET + COLUMN_ID + " = ? ";
        args.add(rideId);
        query = getUpdateValues(ride, query, args);
        query = query + WHERE + COLUMN_ID + " = ?";
        args.add(rideId);
        jdbcTemplate.update(query, args.toArray());
    }

    private String getUpdateValues(UpdateRide ride, String query, List<Object> args) {
        if(ride.name() != null){
            query = query + ",name = ?";
            args.add(ride.name());
        }
        if(ride.done() != null){
            int done = Boolean.TRUE.equals(ride.done())?1:0;
            query = query + ",done = ?";
            args.add(done);
        }
        if(ride.park() != null){
            query = query + ",park = ?";
            args.add(ride.park());
        }
        return query;
    }



}
