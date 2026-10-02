package com.magomez.androidapps.jctravels.stores.dao;

import com.magomez.androidapps.jctravels.stores.dto.CreateStore;
import com.magomez.androidapps.jctravels.stores.dto.Store;
import com.magomez.androidapps.jctravels.stores.dto.StoreFilter;
import com.magomez.androidapps.jctravels.stores.dto.UpdateStore;
import com.magomez.androidapps.jctravels.stores.mapper.StoreMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
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
public class StoreDao {

    private static final String TABLE_STORES = "stores";
    private static final String COLUMN_ID = " id ";
    private static final String COLUMN_OUTLET = " outlet ";

    private final JdbcTemplate jdbcTemplate;

    public StoreDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Store> getAllOutlets(StoreFilter filter) {
        String query = SELECT_ALL + FROM + TABLE_STORES + " " + WHERE +" " + COLUMN_OUTLET + " = ?";
        query = query + " order by priority asc, name asc";
        return jdbcTemplate.query(query, new StoreMapper(), filter.outlet());
    }

    public Store getStore(Integer storeId) {
        String query = SELECT_ALL + FROM + TABLE_STORES + " " + WHERE + COLUMN_ID + "= ?";
        try {
            return jdbcTemplate.queryForObject(query, new StoreMapper(), storeId);
        }
        catch(Exception e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found");
        }
    }

    public void createStore(CreateStore store) {
        String query = INSERT + TABLE_STORES + " (name,done,outlet) " +
                VALUES + "(?,?,?)";
        jdbcTemplate.update(query , store.name(), 0,
                store.outlet());
    }

    public void updateStore(Integer storeId, UpdateStore store) {
        List<Object> args = new ArrayList<>();
        String query = UPDATE + TABLE_STORES + SET + COLUMN_ID + " = ? ";
        args.add(storeId);
        query = getUpdateValues(store, query, args);
        query = query + WHERE + COLUMN_ID + " = ?";
        args.add(storeId);
        jdbcTemplate.update(query, args.toArray());
    }

    private String getUpdateValues(UpdateStore store, String query, List<Object> args) {
        if(store.done() != null){
            int done = Boolean.TRUE.equals(store.done())?1:0;
            query = query + ",done = ?";
            args.add(done);
        }
        return query;
    }

}
