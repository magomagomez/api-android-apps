package com.magomez.androidapps.jctravels.outlets.dao;

import com.magomez.androidapps.jctravels.outlets.dto.OutletFilter;
import com.magomez.androidapps.jctravels.outlets.dto.UpdateOutlet;
import com.magomez.androidapps.jctravels.outlets.mapper.OutletMapper;
import com.magomez.androidapps.jctravels.outlets.dto.CreateOutlet;
import com.magomez.androidapps.jctravels.outlets.dto.Outlet;
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
public class OutletDao {

    private static final String TABLE_OUTLETS = "outlets";
    private static final String COLUMN_ID = " id ";
    private static final String COLUMN_CITY = " city ";

    private final JdbcTemplate jdbcTemplate;

    public OutletDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Outlet> getAllOutlets(OutletFilter filter) {
        List<Object> args = new ArrayList<>();
        String query = SELECT_ALL + FROM + TABLE_OUTLETS + " " + WHERE +" 1=1 ";
        if(filter.city() != null) {
            query = query + AND + COLUMN_CITY + " = ? ";
            args.add(filter.city());
        }
        query = query + " order by name asc";
        return jdbcTemplate.query(query, new OutletMapper(), args.toArray());
    }

    public Outlet getOutlet(Integer outletId) {
        String query = SELECT_ALL + FROM + TABLE_OUTLETS + " " + WHERE + COLUMN_ID + "= ?";
        try {
            return jdbcTemplate.queryForObject(query, new OutletMapper(), outletId);
        }
        catch(EmptyResultDataAccessException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Outlet not found");
        }
    }

    public void createOutlet(CreateOutlet outlet) {
        String query = INSERT + TABLE_OUTLETS + " (name,city) " +
                VALUES + "(?,?)";
        jdbcTemplate.update(query , outlet.name(), outlet.city());
    }

    public void updateOutlet(Integer outletId, UpdateOutlet outlet) {
        List<Object> args = new ArrayList<>();
        String query = UPDATE + TABLE_OUTLETS + SET + COLUMN_ID + " = ? ";
        args.add(outletId);
        query = getUpdateValues(outlet, query, args);
        query = query + WHERE + COLUMN_ID + " = ?";
        args.add(outletId);
        jdbcTemplate.update(query, args.toArray());
    }

    private String getUpdateValues(UpdateOutlet outlet, String query, List<Object> args) {
        if(outlet.name() != null){
            query = query + ",name = ?";
            args.add(outlet.name());
        }
        if(outlet.city() != null){
            query = query + ",city = ?";
            args.add(outlet.city());
        }
        return query;
    }

}
