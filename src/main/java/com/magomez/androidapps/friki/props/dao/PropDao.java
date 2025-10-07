package com.magomez.androidapps.friki.props.dao;

import com.magomez.androidapps.friki.props.dto.Prop;
import com.magomez.androidapps.friki.props.dto.PropFilterRequest;
import com.magomez.androidapps.friki.props.dto.CreatePropRequest;
import com.magomez.androidapps.friki.props.mapper.PropMapper;
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
public class PropDao {

    private static final String TABLE_PROPS = " friki_props ";
    private static final String COLUMN_NAME = " name ";
    private static final String COLUMN_ID = " id ";
    private static final String COLUMN_WISH = " is_wishList ";

    private final JdbcTemplate jdbcTemplate;

    public PropDao(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Prop> search(PropFilterRequest filter) {
        String query = SELECT_ALL;
        query = query + FROM + TABLE_PROPS + " ";
        query = query + WHERE +" 1=1 ";
        if( filter.name() != null) {
            query = query + AND + COLUMN_NAME + " = '" + filter.name() + "' ";
        }
        if(filter.wish() != null){
            query = query + AND + COLUMN_WISH + " = " + (BooleanUtils.isTrue(filter.wish()) ? 1:0) ;
        }

        return  jdbcTemplate.query(query, new PropMapper());
    }

    public Prop getProp(Integer propId) {
        String query = SELECT_ALL;
        query = query + FROM + TABLE_PROPS + " ";
        query = query + WHERE + COLUMN_ID + "= "+ propId;
        try {
            return jdbcTemplate.queryForObject(query, new PropMapper());
        } catch(Exception e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comic not found");
        }
    }

    public void createProps(CreatePropRequest prop) {
        String query = INSERT + TABLE_PROPS + " (name,category,is_wishList) " +
                VALUES + "(?,?,?)";
        jdbcTemplate.update(query , prop.name(), prop.category(), 1);
    }

}
