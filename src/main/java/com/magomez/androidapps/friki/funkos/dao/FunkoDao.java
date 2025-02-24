package com.magomez.androidapps.friki.funkos.dao;

import com.magomez.androidapps.friki.funkos.dto.CreateFunkoRequest;
import com.magomez.androidapps.friki.funkos.dto.FunkoFilterRequest;
import com.magomez.androidapps.friki.funkos.dto.Funko;
import com.magomez.androidapps.friki.funkos.mapper.FunkoMapper;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.magomez.androidapps.friki.config.FrikiConfig.AND;
import static com.magomez.androidapps.friki.config.FrikiConfig.FROM;
import static com.magomez.androidapps.friki.config.FrikiConfig.INSERT;
import static com.magomez.androidapps.friki.config.FrikiConfig.SELECT_ALL;
import static com.magomez.androidapps.friki.config.FrikiConfig.VALUES;
import static com.magomez.androidapps.friki.config.FrikiConfig.WHERE;

@Service
public class FunkoDao {

    private static final String TABLE_FUNKOS = " friki_funkos ";
    private static final String COLUMN_NAME = " name ";
    private static final String COLUMN_ID = " id ";
    private static final String COLUMN_WISH = " is_wishList ";

    private final JdbcTemplate jdbcTemplate;

    public FunkoDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Funko> getAllFunkos(FunkoFilterRequest filter) {
        String query = SELECT_ALL;
        query = query + FROM + TABLE_FUNKOS + " ";
        query = query + WHERE +" 1=1 ";
        if( filter.name() != null) {
            query = query + AND + COLUMN_NAME + " = '" + filter.name() + "' ";
        }
        if(BooleanUtils.isTrue(filter.wish())){
            query = query + AND + COLUMN_WISH + " = 1";
        }

        return  jdbcTemplate.query( query, new FunkoMapper());
    }

    public Funko getFunko(Integer funkoId) {
        String query = SELECT_ALL;
        query = query + FROM + TABLE_FUNKOS + " ";
        query = query + WHERE + COLUMN_ID + "= "+ funkoId;
        try {
            return jdbcTemplate.queryForObject(query, new FunkoMapper());
        }
        catch(Exception e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Funko not found");
        }
    }

    public void createFunko(CreateFunkoRequest funko) {
        String query = INSERT + TABLE_FUNKOS + " (name,image_path,number,category,is_wishList) " +
                VALUES + "(?,?,?,?,?,?,?)";
        jdbcTemplate.update(query , funko.name(), funko.imagePath(),
                funko.number(), funko.category(), 1);
    }

}
