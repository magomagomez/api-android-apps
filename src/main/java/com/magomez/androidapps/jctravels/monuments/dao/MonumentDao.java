package com.magomez.androidapps.jctravels.monuments.dao;

import com.magomez.androidapps.jctravels.monuments.dto.CreateMonument;
import com.magomez.androidapps.jctravels.monuments.dto.Monument;
import com.magomez.androidapps.jctravels.monuments.dto.MonumentFilter;
import com.magomez.androidapps.jctravels.monuments.dto.UpdateMonument;
import com.magomez.androidapps.jctravels.monuments.mapper.MonumentMapper;
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
public class MonumentDao {

    private static final String TABLE_MONUMENTS = "monuments";
    private static final String COLUMN_CITY = " city ";
    private static final String COLUMN_ID = " id ";
    private static final String COLUMN_ROUTE = " route ";

    private final JdbcTemplate jdbcTemplate;

    public MonumentDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Monument> getAllMonuments(MonumentFilter filter) {
        List<Object> args = new ArrayList<>();
        String query = SELECT_ALL + FROM + TABLE_MONUMENTS + " " + WHERE +" 1=1 ";
        if( filter.city() != null) {
            query = query + AND + COLUMN_CITY + " = ? ";
            args.add(filter.city());
        }
        if( filter.route() != null){
            query = query + AND + COLUMN_ROUTE + " = ?";
            args.add(filter.route());
        }

        query = query + " order by done asc, priority asc, name asc";
        return jdbcTemplate.query(query, new MonumentMapper(), args.toArray());
    }

    public Monument getMonument(Integer monumentId) {
        String query = SELECT_ALL + FROM + TABLE_MONUMENTS + " " + WHERE + COLUMN_ID + "= ?";
        try {
            return jdbcTemplate.queryForObject(query, new MonumentMapper(), monumentId);
        }
        catch(Exception e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Monument not found");
        }
    }

    public void createMonument(CreateMonument monument, Integer defaultRoute) {
        String query = INSERT + TABLE_MONUMENTS + " (name,city,schedule,price,route,done) " +
                VALUES + "(?,?,?,?,?,?)";
        jdbcTemplate.update(query , monument.name(), monument.city(),
                monument.schedule(), monument.price(),defaultRoute,0);
    }

    public void updateMonument(Integer monumentId, UpdateMonument monument) {
        List<Object> args = new ArrayList<>();
        String query = UPDATE + TABLE_MONUMENTS + SET + COLUMN_ID + " = ? ";
        args.add(monumentId);
        query = getUpdateValues(monument, query, args);
        query = query + WHERE + COLUMN_ID + " = ?";
        args.add(monumentId);
        jdbcTemplate.update(query, args.toArray());
    }

    private String getUpdateValues(UpdateMonument monument, String query, List<Object> args) {
        if(monument.done() != null){
            Integer done = Boolean.TRUE.equals(monument.done())?1:0;
            query = query + ",done = ?";
            args.add(done);
        }
        if(monument.route() != null){
            query = query + ",route = ?";
            args.add(monument.route());
        }
        return query;
    }

}
